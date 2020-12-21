package org.conio.container.engine;

import static org.conio.container.Constants.ENV_YAML_HDFS_PATH;
import static org.conio.container.engine.util.Util.secureGetEnv;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ExecutionType;
import org.apache.hadoop.yarn.api.records.ExecutionTypeRequest;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.conio.container.engine.util.Translate;
import org.conio.container.k8s.Container;
import org.conio.container.k8s.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationMaster {
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationMaster.class);

  private AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient;
  private RMCallbackHandler rmCallbackHandler;
  private NMClientAsync nmClientAsync;
  private Configuration conf;
  private Context context;
  private ContainerId amContainerId;

  private int requestId = 0;

  /**
   * Main entrypoint of the Application Master class.
   */
  public static void main(String[] args) throws Exception {
    ApplicationMaster appMaster = null;
    try {
      appMaster = new ApplicationMaster();
      appMaster.init(args);
      appMaster.run();
      appMaster.finish();
    } finally {
      if (appMaster != null) {
        appMaster.cleanup();
      }
    }
  }

  private static Options createOptions() {
    // TODO: create a way to handle the same options for the client and the AM
    Options opts = new Options();
    opts.addOption("appname", true, "the name of the application");
    return opts;
  }

  private void init(String[] args) throws ParseException, IOException {
    Options opts = createOptions();

    if (args.length == 0) {
      new HelpFormatter().printHelp("ApplicationMaster", opts);
      throw new IllegalArgumentException("No args specified for application master to initialize");
    }

    CommandLine cliParser = new GnuParser().parse(opts, args);
    String appName = cliParser.getOptionValue("appname");
    LOG.info("Parsed application name: {}", appName);

    String containerIdStr = secureGetEnv(ApplicationConstants.Environment.CONTAINER_ID.name());
    if (containerIdStr.isEmpty()) {
      throw new RuntimeException("Expected container ID among the environment variables");
    }
    amContainerId = ContainerId.fromString(containerIdStr);
    LOG.info("Application ID: {}", amContainerId.getApplicationAttemptId().getApplicationId());

    // if this needs to be configured, resources should be added to the AM container context
    conf = new YarnConfiguration();
    Pod pod = Pod.parseFromFile(getYamlPath(conf).toString());
    context = new Context(pod);
    LOG.info("Loaded pod {}", pod.getMetadata().getName());
  }

  private void run() throws IOException, YarnException {
    tokenSetup();

    NMCallbackHandler nmCallbackHandler = new NMCallbackHandler();
    nmClientAsync = new NMClientAsyncImpl(nmCallbackHandler);
    nmClientAsync.init(conf);
    nmClientAsync.start();
    // TODO check that callback handlers are active

    rmCallbackHandler = new RMCallbackHandler(context, nmClientAsync);

    amRMClient = AMRMClientAsync.createAMRMClientAsync(1000, rmCallbackHandler);
    amRMClient.init(conf);
    amRMClient.start();

    String appMasterHostname = NetUtils.getHostname();

    RegisterApplicationMasterResponse response =
        amRMClient.registerApplicationMaster(appMasterHostname, -1, "", null);

    controlLoop();
  }

  private void addContainerRequest(Container container) {
    AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM(container);
    amRMClient.addContainerRequest(containerAsk);
  }

  private void controlLoop() {
    while (true) {
      List<Container> unlaunchedContainers =
          context.getContainerTracker().getUnlaunchedContainers();
      for (Container unlaunchedContainer : unlaunchedContainers) {
        addContainerRequest(unlaunchedContainer);
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException ie) {
        return;
      }
    }
  }

  private Path getYamlPath(Configuration conf) throws IOException {
    String yamlHdfsPath = secureGetEnv(ENV_YAML_HDFS_PATH);
    FileSystem fs = FileSystem.get(conf);
    String[] localDirs = StringUtils.getTrimmedStrings(
        secureGetEnv(ApplicationConstants.Environment.LOCAL_DIRS.key()));

    Path path = new Path(new Path(localDirs[0], amContainerId.toString()), "pod.yaml");
    fs.copyToLocalFile(new Path(yamlHdfsPath), path);
    return path;
  }

  private void tokenSetup() throws IOException {
    Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();
    DataOutputBuffer dob = new DataOutputBuffer();
    credentials.writeTokenStorageToStream(dob);
    // Now remove the AM->RM token so that containers cannot access it.
    Iterator<Token<?>> iter = credentials.getAllTokens().iterator();
    LOG.info("Executing with tokens:");
    while (iter.hasNext()) {
      Token<?> token = iter.next();
      LOG.info(token.toString());
      if (token.getKind().equals(AMRMTokenIdentifier.KIND_NAME)) {
        iter.remove();
      }
    }
    // ByteBuffer allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());

    String appSubmitterUserName = secureGetEnv(ApplicationConstants.Environment.USER.name());
    UserGroupInformation appSubmitterUgi =
        UserGroupInformation.createRemoteUser(appSubmitterUserName);
    appSubmitterUgi.addCredentials(credentials);

    LOG.debug("Tokens has been set up successfully.");
  }

  private AMRMClient.ContainerRequest setupContainerAskForRM(Container container) {
    return new AMRMClient.ContainerRequest(
        Translate.translateResourceRequirements(container),
        null,
        null,
        Priority.newInstance(0),
        context.getContainerTracker().getNextRequestIdForContainer(container),
        true,
        null,
        ExecutionTypeRequest.newInstance(ExecutionType.GUARANTEED, false),
        "");
  }

  private void finish() {
    LOG.info("Application completed, cleaning up");

    // When the application completes, it should stop all running containers
    LOG.info("Stopping running containers");
    nmClientAsync.stop();

    LOG.info("Unregistering AM");
    try {
      amRMClient.unregisterApplicationMaster(
          FinalApplicationStatus.SUCCEEDED, "containers completed", null);
    } catch (Exception ex) {
      LOG.error("Failed to unregister application", ex);
    }
    amRMClient.stop();
  }

  private void cleanup() {
  }
}
