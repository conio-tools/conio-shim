package org.conio.container.engine.mock;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.conio.container.engine.RMCallbackHandler;

import java.util.Collections;
import java.util.List;

import static org.conio.container.Constants.LOOP_TIME;

public class MockNMClientAsync extends NMClientAsync {
  private RMCallbackHandler rmCallbackHandler;

  public MockNMClientAsync() {
    super(null);
  }

  public void setRMCallbackHandler(RMCallbackHandler rmCallbackHandler) {
    this.rmCallbackHandler = rmCallbackHandler;
  }

  @Override
  public void startContainerAsync(Container container, ContainerLaunchContext containerLaunchContext) {
    Thread t = new Thread(() -> {
      try {
        Thread.sleep(LOOP_TIME);
      } catch (InterruptedException e) {
        return;
      }
      ContainerStatus status = ContainerStatus.newInstance(
          container.getId(), ContainerState.COMPLETE, "", 0);
      List<ContainerStatus> completedContainers = Collections.singletonList(status);
      rmCallbackHandler.onContainersCompleted(completedContainers);
    });
    t.start();
  }

  @Override
  public void increaseContainerResourceAsync(Container container) {

  }

  @Override
  public void updateContainerResourceAsync(Container container) {

  }

  @Override
  public void reInitializeContainerAsync(ContainerId containerId, ContainerLaunchContext containerLaunchContext, boolean b) {

  }

  @Override
  public void restartContainerAsync(ContainerId containerId) {

  }

  @Override
  public void rollbackLastReInitializationAsync(ContainerId containerId) {

  }

  @Override
  public void commitLastReInitializationAsync(ContainerId containerId) {

  }

  @Override
  public void stopContainerAsync(ContainerId containerId, NodeId nodeId) {

  }

  @Override
  public void getContainerStatusAsync(ContainerId containerId, NodeId nodeId) {

  }
}
