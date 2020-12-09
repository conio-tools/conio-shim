package org.conio.container.engine;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RMCallbackHandler extends AMRMClientAsync.AbstractCallbackHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RMCallbackHandler.class);

  private final NMClientAsync nmClientAsync;
  private final AtomicBoolean done;

  RMCallbackHandler(NMClientAsync nmClientAsync) {
    this.nmClientAsync = nmClientAsync;
    this.done = new AtomicBoolean(false);
  }

  @Override
  public void onContainersCompleted(List<ContainerStatus> completedContainers) {
    LOG.info("onContainersCompleted called with size=" + completedContainers.size());
    this.done.set(true);
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
    Container container = allocatedContainers.get(0);

    ByteBuffer allTokens;
    try {
      Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();
      DataOutputBuffer dob = new DataOutputBuffer();
      credentials.writeTokenStorageToStream(dob);
      allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
    } catch (IOException ioe) {
      throw new RuntimeException("unexpected exception", ioe);
    }

    // TODO parameterize these
    List<String> commands = Arrays.asList("sleep", "60");
    String image = "library/ubuntu:latest";

    Map<String, String> env = new HashMap<>();
    env.put("YARN_CONTAINER_RUNTIME_TYPE", "docker");
    env.put("YARN_CONTAINER_RUNTIME_DOCKER_IMAGE", image);
    env.put("YARN_CONTAINER_RUNTIME_DOCKER_RUN_OVERRIDE_DISABLE", "true");
    env.put("YARN_CONTAINER_RUNTIME_DOCKER_DELAYED_REMOVAL", "true");

    ContainerLaunchContext ctx =
        ContainerLaunchContext.newInstance(
            new HashMap<>(), env, commands, null, allTokens.duplicate(), null, null);
    nmClientAsync.startContainerAsync(container, ctx);
  }

  @Override
  public void onContainersUpdated(List<UpdatedContainer> containers) {
    // default impl
    for (UpdatedContainer container : containers) {
      nmClientAsync.updateContainerResourceAsync(container.getContainer());
    }
  }

  @Override
  public void onRequestsRejected(List<RejectedSchedulingRequest> rejReqs) {
    LOG.info("scheduling request rejected");
    done.set(true);
  }

  @Override
  public void onShutdownRequest() {
    LOG.info("shutdown was requested");
    done.set(true);
  }

  @Override
  public void onNodesUpdated(List<NodeReport> updatedNodes) {}

  @Override
  public float getProgress() {
    return 0;
  }

  @Override
  public void onError(Throwable e) {
    LOG.error("Error in RMCallbackHandler: ", e);
    done.set(true);
  }

  public AtomicBoolean isFinished() {
    return done;
  }
}
