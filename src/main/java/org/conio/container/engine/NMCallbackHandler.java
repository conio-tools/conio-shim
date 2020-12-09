package org.conio.container.engine;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMCallbackHandler extends NMClientAsync.AbstractCallbackHandler {
  private static final Logger LOG = LoggerFactory.getLogger(NMCallbackHandler.class);

  private final ConcurrentMap<ContainerId, Container> containers = new ConcurrentHashMap<>();

  public NMCallbackHandler() {
  }

  @Override
  public void onContainerStopped(ContainerId containerId) {
    LOG.debug("Container {} stopped", containerId);
    containers.remove(containerId);
  }

  @Override
  public void onContainerStatusReceived(ContainerId containerId, ContainerStatus containerStatus) {
    LOG.debug("Container Status: id={}, status={}", containerId, containerStatus);
  }

  @Override
  public void onContainerStarted(
      ContainerId containerId, Map<String, ByteBuffer> allServiceResponse) {
    LOG.debug("Started container {}", containerId);
  }

  @Override
  public void onStartContainerError(ContainerId containerId, Throwable t) {
    LOG.error("Failed to start Container {}", containerId, t);
  }

  @Override
  public void onGetContainerStatusError(ContainerId containerId, Throwable t) {
    LOG.error("Failed to query the status of Container " + containerId);
  }

  @Override
  public void onStopContainerError(ContainerId containerId, Throwable t) {
    LOG.error("Failed to stop Container " + containerId);
    containers.remove(containerId);
  }

  @Deprecated
  @Override
  public void onIncreaseContainerResourceError(ContainerId containerId, Throwable t) {
  }

  @Deprecated
  @Override
  public void onContainerResourceIncreased(ContainerId containerId, Resource resource) {
  }

  @Override
  public void onUpdateContainerResourceError(ContainerId containerId, Throwable t) {
  }

  @Override
  public void onContainerResourceUpdated(ContainerId containerId, Resource resource) {
  }
}
