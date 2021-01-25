package org.conio.container.engine;

import static org.conio.container.Constants.ENV_NAMESPACE;
import static org.conio.container.Constants.ENV_POD_NAME;
import static org.conio.container.Constants.ENV_ZK_ADDRESS;
import static org.conio.container.Constants.ENV_ZK_ROOT_NODE;
import static org.conio.container.Constants.LOOP_TIME;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
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
import org.conio.container.engine.util.EnvVarProvider;
import org.conio.container.engine.util.Translate;
import org.conio.container.k8s.Container;
import org.conio.container.k8s.Pod;
import org.conio.container.zookeeper.ClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationMaster {
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationMaster.class);

  private EnvVarProvider envVars;
  private AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient;
  private RMCallbackHandler rmCallbackHandler;
  private NMClientAsync nmClientAsync;
  private Configuration conf;
  private Context context;
  private ClientWrapper zkClient;

  /**
   * Main entrypoint of the Application Master class.
   */
  public static void main(String[] args) throws Exception {
    ApplicationMaster appMaster = new ApplicationMaster();
    runAppMaster(appMaster);
  }

  static void runAppMaster(ApplicationMaster appMaster) throws Exception {
    try {
      appMaster.init();
      appMaster.run();
      appMaster.finish();
    } finally {
      if (appMaster != null) {
        appMaster.cleanup();
      }
    }
  }

  void init() throws Exception {
    this.envVars = getEnvVarProvider();

    String containerIdStr = envVars.get(ApplicationConstants.Environment.CONTAINER_ID.name());
    if (containerIdStr.isEmpty()) {
      throw new RuntimeException("Expected container ID among the environment variables");
    }
    ContainerId amContainerId = ContainerId.fromString(containerIdStr);
    LOG.info("Application ID: {}", amContainerId.getApplicationAttemptId().getApplicationId());

    // if this needs to be configured, resources should be added to the AM container context
    conf = new YarnConfiguration();

    zkClient = getZkClient();
    zkClient.start();
    LOG.info("ZK client started");
    Pod pod = Pod.parseFromBytes(
        zkClient.downloadPod(envVars.get(ENV_NAMESPACE), envVars.get(ENV_POD_NAME)));
    context = new Context(pod);
    LOG.info("Pod loaded successfully");
  }

  void run() throws IOException, YarnException {
    tokenSetup();

    nmClientAsync = getNmClientAsync();
    nmClientAsync.init(conf);
    nmClientAsync.start();
    // TODO check that callback handlers are active

    rmCallbackHandler = getRmCallbackHandler();

    amRMClient = getAmRMClient();
    amRMClient.init(conf);
    amRMClient.start();

    String appMasterHostname = NetUtils.getHostname();

    amRMClient.registerApplicationMaster(appMasterHostname, -1, "", null);

    controlLoop();
  }

  private void addContainerRequest(Container container) {
    AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM(container);
    amRMClient.addContainerRequest(containerAsk);
  }

  private void controlLoop() {
    ContainerTracker containerTracker = context.getContainerTracker();
    while (true) {
      List<Container> unlaunchedContainers = containerTracker.getUnlaunchedContainers();
      for (Container unlaunchedContainer : unlaunchedContainers) {
        addContainerRequest(unlaunchedContainer);
      }
      if (containerTracker.hasFinished()) {
        break;
      }
      try {
        Thread.sleep(LOOP_TIME);
      } catch (InterruptedException ie) {
        return;
      }
    }
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

    String appSubmitterUserName = envVars.get(ApplicationConstants.Environment.USER.name());
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

  void finish() {
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

  void cleanup() {
  }

  // For testing we need to mock these out

  @VisibleForTesting
  AMRMClientAsync<AMRMClient.ContainerRequest> getAmRMClient() {
    return AMRMClientAsync.createAMRMClientAsync(1000, rmCallbackHandler);
  }

  @VisibleForTesting
  RMCallbackHandler getRmCallbackHandler() {
    return new RMCallbackHandler(context, nmClientAsync);
  }

  @VisibleForTesting
  NMClientAsync getNmClientAsync() {
    return new NMClientAsyncImpl(new NMCallbackHandler());
  }

  @VisibleForTesting
  ClientWrapper getZkClient() {
    ClientWrapper zkClient = new ClientWrapper(envVars.get(ENV_ZK_ROOT_NODE));
    zkClient.init(envVars.get(ENV_ZK_ADDRESS));
    return zkClient;
  }

  @VisibleForTesting
  EnvVarProvider getEnvVarProvider() {
    return new EnvVarProvider();
  }

  @VisibleForTesting
  Context getContext() {
    return context;
  }
}
