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
import org.conio.container.k8s.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMCallbackHandler extends AMRMClientAsync.AbstractCallbackHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RMCallbackHandler.class);

  private final Context context;
  private final NMClientAsync nmClientAsync;

  private final AtomicInteger containerAsks;
  private final AtomicInteger runningContainers;

  RMCallbackHandler(Context context, NMClientAsync nmClientAsync) {
    this.context = context;
    this.nmClientAsync = nmClientAsync;
    this.containerAsks = new AtomicInteger(0);
    this.runningContainers = new AtomicInteger(0);
  }

  @Override
  public void onContainersCompleted(List<ContainerStatus> completedContainers) {
    LOG.info("onContainersCompleted called with size=" + completedContainers.size());
    runningContainers.getAndDecrement();
  }

  @Override
  public void onContainersAllocated(List<Container> allocatedContainers) {
    if (allocatedContainers.size() != 1) {
      LOG.warn("Expected exactly one container");
      if (allocatedContainers.size() == 0) {
        LOG.error("AllocatedContainer size is zero! Nothing to do.");
        return;
      }
    }

    ByteBuffer allTokens;
    try {
      Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();
      DataOutputBuffer dob = new DataOutputBuffer();
      credentials.writeTokenStorageToStream(dob);
      allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
    } catch (IOException ioe) {
      throw new RuntimeException("unexpected exception", ioe);
    }

    Pod pod = context.getPod();
    Map<String, String> env = new HashMap<>();
    env.put("YARN_CONTAINER_RUNTIME_TYPE", "docker");
    env.put("YARN_CONTAINER_RUNTIME_DOCKER_IMAGE",
        pod.getSpec().getContainers().get(0).getImage());
    env.put("YARN_CONTAINER_RUNTIME_DOCKER_RUN_OVERRIDE_DISABLE", "true");
    // TODO parameterize this
    env.put("YARN_CONTAINER_RUNTIME_DOCKER_DELAYED_REMOVAL", "true");
    fillEnvMapFromPod(pod, env);

    LOG.info("Initialized ContainerLaunchContext");
    ContainerLaunchContext ctx =
        ContainerLaunchContext.newInstance(
            new HashMap<>(), env, pod.getSpec().getContainers().get(0).getCommand(),
            null, allTokens.duplicate(), null, null);
    nmClientAsync.startContainerAsync(allocatedContainers.get(0), ctx);
    runningContainers.getAndIncrement();
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
    for (RejectedSchedulingRequest rsr : rejectedRequests) {
      LOG.error("Scheduling request rejected. Reason: {}", rsr.getReason());
      containerAsks.getAndDecrement();
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

  private void fillEnvMapFromPod(Pod pod, Map<String, String> envs) {
    List<EnvVar> envVarList = pod.getSpec().getContainers().get(0).getEnv();
    for (EnvVar envVar : envVarList) {
      envs.put(envVar.getName(), envVar.getValue());
    }
  }

  public void incrementContainerAsks() {
    containerAsks.getAndIncrement();
  }

  public int getContainerAsks() {
    return containerAsks.get();
  }

  public int getRunningContainers() {
    return runningContainers.get();
  }
}
