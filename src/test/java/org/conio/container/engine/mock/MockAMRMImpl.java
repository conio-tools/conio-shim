package org.conio.container.engine.mock;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.UpdateContainerRequest;
import org.apache.hadoop.yarn.api.resource.PlacementConstraint;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.conio.container.engine.RMCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.conio.container.Constants.LOOP_TIME;

public class MockAMRMImpl extends AMRMClientAsync<AMRMClient.ContainerRequest> {
  private static final Logger LOG = LoggerFactory.getLogger(MockAMRMImpl.class);

  private final RMCallbackHandler rmCallbackHandler;

  public MockAMRMImpl(RMCallbackHandler rmCallbackHandler) {
    super(0, null);
    this.rmCallbackHandler = rmCallbackHandler;
  }

  @Override
  public List<? extends Collection<AMRMClient.ContainerRequest>> getMatchingRequests(Priority priority, String s, Resource resource) {
    return null;
  }

  @Override
  public RegisterApplicationMasterResponse registerApplicationMaster(String s, int i, String s1) throws YarnException, IOException {
    return null;
  }

  @Override
  public RegisterApplicationMasterResponse registerApplicationMaster(String appHostName, int appHostPort, String appTrackingUrl, Map<Set<String>, PlacementConstraint> placementConstraints) throws YarnException, IOException {
    return RegisterApplicationMasterResponse.newInstance(
        null, null, null, null,  null, null, null);
  }

  @Override
  public void unregisterApplicationMaster(FinalApplicationStatus finalApplicationStatus, String s, String s1) throws YarnException, IOException {

  }

  @Override
  public void addContainerRequest(AMRMClient.ContainerRequest containerRequest) {
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(LOOP_TIME);
      } catch (InterruptedException e) {
        return;
      }
      ApplicationId appId = ApplicationId.newInstance(1111111111, 1);
      ApplicationAttemptId attemptId = ApplicationAttemptId.newInstance(appId, 1);
      ContainerId cId = ContainerId.newContainerId(attemptId, 1);
      NodeId nodeId = NodeId.newInstance("localhost", 1010);
      Resource resource = Resource.newInstance(1, 1000);
      Priority priority = Priority.newInstance(0);
      Container container = Container.newInstance(cId, nodeId, "unknown", resource, priority, null);
      container.setAllocationRequestId(containerRequest.getAllocationRequestId());
      List<Container> allocatedContainers = Collections.singletonList(container);
      rmCallbackHandler.onContainersAllocated(allocatedContainers);
    });
    LOG.info("Adding container request");
    thread.start();
  }

  @Override
  public void removeContainerRequest(AMRMClient.ContainerRequest containerRequest) {

  }

  @Override
  public void requestContainerUpdate(Container container, UpdateContainerRequest updateContainerRequest) {

  }

  @Override
  public void releaseAssignedContainer(ContainerId containerId) {

  }

  @Override
  public Resource getAvailableResources() {
    return null;
  }

  @Override
  public int getClusterNodeCount() {
    return 0;
  }

  @Override
  public void updateBlacklist(List<String> list, List<String> list1) {

  }

  @Override
  public void init(Configuration conf) {

  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }
}
