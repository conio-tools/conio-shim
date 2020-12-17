package org.conio.container.engine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.RejectedSchedulingRequest;
import org.apache.hadoop.yarn.api.records.UpdatedContainer;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.conio.container.k8s.EnvVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMCallbackHandler extends AMRMClientAsync.AbstractCallbackHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RMCallbackHandler.class);

  private final Context context;
  private final NMClientAsync nmClientAsync;

  RMCallbackHandler(Context context, NMClientAsync nmClientAsync) {
    this.context = context;
    this.nmClientAsync = nmClientAsync;
  }

  @Override
  public void onContainersCompleted(List<ContainerStatus> completedContainers) {
    LOG.info("onContainersCompleted called with size=" + completedContainers.size());
    for (ContainerStatus status : completedContainers) {
      context.getContainerTracker().containerCompleted(status);
    }
  }

  @Override
  public void onContainersAllocated(List<Container> allocatedContainers) {
    for (Container allocatedContainer : allocatedContainers) {
      org.conio.container.k8s.Container container =
          context.getContainerTracker().containerAllocated(allocatedContainer);

      ByteBuffer allTokens;
      try {
        Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();
        DataOutputBuffer dob = new DataOutputBuffer();
        credentials.writeTokenStorageToStream(dob);
        allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
      } catch (IOException ioe) {
        throw new RuntimeException("unexpected exception", ioe);
      }

      Map<String, String> env = new HashMap<>();
      env.put("YARN_CONTAINER_RUNTIME_TYPE", "docker");
      env.put("YARN_CONTAINER_RUNTIME_DOCKER_IMAGE", container.getImage());
      env.put("YARN_CONTAINER_RUNTIME_DOCKER_RUN_OVERRIDE_DISABLE", "true");
      // TODO parameterize this
      env.put("YARN_CONTAINER_RUNTIME_DOCKER_DELAYED_REMOVAL", "true");
      fillEnvMapFromPod(container, env);

      LOG.info("Initialized ContainerLaunchContext");
      ContainerLaunchContext ctx =
          ContainerLaunchContext.newInstance(
              new HashMap<>(), env, container.getCommand(),
              null, allTokens.duplicate(), null, null);
      nmClientAsync.startContainerAsync(allocatedContainer, ctx);
    }
  }

  @Override
  public void onContainersUpdated(List<UpdatedContainer> containers) {
    // default impl
    for (UpdatedContainer container : containers) {
      nmClientAsync.updateContainerResourceAsync(container.getContainer());
    }
  }

  @Override
  public void onRequestsRejected(List<RejectedSchedulingRequest> rejectedRequests) {
    for (RejectedSchedulingRequest rejectedRequest : rejectedRequests) {
      context.getContainerTracker().requestRejected(rejectedRequest);
    }
  }

  @Override
  public void onShutdownRequest() {
    LOG.info("shutdown was requested");
    // TODO shut down
  }

  @Override
  public void onNodesUpdated(List<NodeReport> updatedNodes) {
    // let's ignore this now
  }

  @Override
  public float getProgress() {
    return 0;
  }

  @Override
  public void onError(Throwable e) {
    LOG.error("Error in RMCallbackHandler: ", e);
    // TODO should call stop
  }

  private void fillEnvMapFromPod(
      org.conio.container.k8s.Container container, Map<String, String> envs) {
    List<EnvVar> envVarList = container.getEnv();
    for (EnvVar envVar : envVarList) {
      envs.put(envVar.getName(), envVar.getValue());
    }
  }
}
