package org.conio.container.engine;

import static org.conio.container.Constants.ENV_YAML_HDFS_PATH;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
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
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ExecutionType;
import org.apache.hadoop.yarn.api.records.ExecutionTypeRequest;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.conio.container.k8s.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationMaster {
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationMaster.class);

  private AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient;
  private RMCallbackHandler allocListener;
  private NMClientAsync nmClientAsync;

  private Pod pod;

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

  private void init(String[] args) throws ParseException {
    Options opts = createOptions();

    if (args.length == 0) {
      new HelpFormatter().printHelp("ApplicationMaster", opts);
      throw new IllegalArgumentException("No args specified for application master to initialize");
    }

    // TODO set up AM based on cliParser arguments
    CommandLine cliParser = new GnuParser().parse(opts, args);
    String appName = cliParser.getOptionValue("appname");
    LOG.info("The application name is {}", appName);

    Map<String, String> envs = System.getenv();

    if (!envs.containsKey(ApplicationConstants.Environment.CONTAINER_ID.name())) {
      throw new RuntimeException("Expected container ID among the environment variables");
    }
    ContainerId containerId =
        ContainerId.fromString(envs.get(ApplicationConstants.Environment.CONTAINER_ID.name()));
    ApplicationAttemptId appAttemptID = containerId.getApplicationAttemptId();
    ApplicationId appId = appAttemptID.getApplicationId();
    LOG.info("The application ID is {}", appId);
  }

  private void run() throws IOException, YarnException {
    // TODO token setup
    tokenSetup();

    // if this needs to be configured, you should also that resource to the AM container context
    Configuration conf = new YarnConfiguration();

    String yamlHdfsPath = System.getenv(ENV_YAML_HDFS_PATH);
    FileSystem fs = FileSystem.get(conf);
    String[] localDirs = StringUtils.getTrimmedStrings(
            System.getenv(ApplicationConstants.Environment.LOCAL_DIRS.key()));
    String containerId = System.getenv(ApplicationConstants.Environment.CONTAINER_ID.key());
    Path path = new Path(localDirs[0], containerId);
    fs.copyToLocalFile(new Path(yamlHdfsPath), path);
    pod = Pod.parseFromFile(path.toString());

    NMCallbackHandler containerListener = new NMCallbackHandler();
    nmClientAsync = new NMClientAsyncImpl(containerListener);
    nmClientAsync.init(conf);
    nmClientAsync.start();

    allocListener = new RMCallbackHandler(this, nmClientAsync);

    amRMClient = AMRMClientAsync.createAMRMClientAsync(1000, allocListener);
    amRMClient.init(conf);
    amRMClient.start();

    String appMasterHostname = NetUtils.getHostname();

    RegisterApplicationMasterResponse response =
        amRMClient.registerApplicationMaster(appMasterHostname, -1, "", null);

    AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM();
    amRMClient.addContainerRequest(containerAsk);
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

    String appSubmitterUserName = System.getenv(ApplicationConstants.Environment.USER.name());
    UserGroupInformation appSubmitterUgi =
        UserGroupInformation.createRemoteUser(appSubmitterUserName);
    appSubmitterUgi.addCredentials(credentials);
  }

  private AMRMClient.ContainerRequest setupContainerAskForRM() {
    Resource resourceCapability = Resource.newInstance(1000, 1);

    // TODO add yaml as resource

    return new AMRMClient.ContainerRequest(
        resourceCapability,
        null,
        null,
        Priority.newInstance(0),
        0,
        true,
        null,
        ExecutionTypeRequest.newInstance(ExecutionType.GUARANTEED, false),
        "");
  }

  private void finish() throws InterruptedException {
    // wait for completion
    while (allocListener.isFinished().get()) {
      Thread.sleep(200);
    }

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

  Pod getPod() {
    return pod;
  }
}
