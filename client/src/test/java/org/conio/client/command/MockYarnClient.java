package org.conio.client.command;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewReservationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationDeleteRequest;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationDeleteResponse;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationListRequest;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationListResponse;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationSubmissionRequest;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationSubmissionResponse;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationUpdateRequest;
import org.apache.hadoop.yarn.api.protocolrecords.ReservationUpdateResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAccessType;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptReport;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerReport;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.NodeAttribute;
import org.apache.hadoop.yarn.api.records.NodeAttributeInfo;
import org.apache.hadoop.yarn.api.records.NodeAttributeKey;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.NodeLabel;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.NodeToAttributeValue;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.QueueState;
import org.apache.hadoop.yarn.api.records.QueueStatistics;
import org.apache.hadoop.yarn.api.records.QueueUserACLInfo;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.ResourceTypeInfo;
import org.apache.hadoop.yarn.api.records.ShellContainerCommand;
import org.apache.hadoop.yarn.api.records.SignalContainerCommand;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;

public class MockYarnClient extends YarnClient {
  private ApplicationSubmissionContext latestAppSubmission;

  MockYarnClient() {
    super("mock");
  }

  @Override
  public void start() {
    // do nothing
  }

  @Override
  public YarnClientApplication createApplication() {
    ApplicationId appId = ApplicationId.newInstance(0, 0);
    Resource resource = Resource.newInstance(1000, 1);
    GetNewApplicationResponse newAppResponse = GetNewApplicationResponse.newInstance(
        appId, resource, resource);
    ContainerLaunchContext context = ContainerLaunchContext.newInstance(
        new HashMap<>(), new HashMap<>(), new ArrayList<>(),
        new HashMap<>(), ByteBuffer.wrap("".getBytes()), new HashMap<>());
    ApplicationSubmissionContext appContext = ApplicationSubmissionContext
        .newInstance(appId, "", "", Priority.UNDEFINED, context, false,
            false, 3, resource, "", false, "", "");
    return new YarnClientApplication(newAppResponse, appContext);
  }

  @Override
  public ApplicationId submitApplication(
      ApplicationSubmissionContext appContext) {
    latestAppSubmission = appContext;
    return null;
  }

  ApplicationSubmissionContext getLatestApplication() {
    return latestAppSubmission;
  }

  @Override
  public void failApplicationAttempt(
      ApplicationAttemptId applicationAttemptId) {
  }

  @Override
  public void killApplication(ApplicationId applicationId) {
  }

  @Override
  public void killApplication(
      ApplicationId applicationId, String diagnostics) {
  }

  @Override
  public ApplicationReport getApplicationReport(ApplicationId appId) {
    return null;
  }

  @Override
  public Token<AMRMTokenIdentifier> getAMRMToken(ApplicationId appId) {
    return null;
  }

  @Override
  public List<ApplicationReport> getApplications() {
    return null;
  }

  @Override
  public List<ApplicationReport> getApplications(Set<String> applicationTypes) {
    return null;
  }

  @Override
  public List<ApplicationReport> getApplications(
      EnumSet<YarnApplicationState> applicationStates) {
    return null;
  }

  @Override
  public List<ApplicationReport> getApplications(
      Set<String> applicationTypes, EnumSet<YarnApplicationState> applicationStates) {
    return null;
  }

  @Override
  public List<ApplicationReport> getApplications(
      Set<String> applicationTypes,
      EnumSet<YarnApplicationState> applicationStates,
      Set<String> applicationTags) {
    return null;
  }

  @Override
  public List<ApplicationReport> getApplications(
      Set<String> queues, Set<String> users,
      Set<String> applicationTypes,
      EnumSet<YarnApplicationState> applicationStates) {
    return null;
  }

  @Override
  public YarnClusterMetrics getYarnClusterMetrics() {
    return null;
  }

  @Override
  public List<NodeReport> getNodeReports(NodeState... states) {
    return null;
  }

  @Override
  public org.apache.hadoop.yarn.api.records.Token getRMDelegationToken(
      Text renewer) {
    return null;
  }

  @Override
  public QueueInfo getQueueInfo(String queueName) {
    QueueStatistics stat = QueueStatistics.newInstance(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    return QueueInfo.newInstance(queueName, 1.0f, 1.0f, 1.0f,
        new ArrayList<QueueInfo>(), new ArrayList<ApplicationReport>(),
        QueueState.RUNNING, new HashSet<String>(), "", stat, false);
  }

  @Override
  public List<QueueInfo> getAllQueues() {
    return null;
  }

  @Override
  public List<QueueInfo> getRootQueueInfos() {
    return null;
  }

  @Override
  public List<QueueInfo> getChildQueueInfos(String parent) {
    return null;
  }

  @Override
  public List<QueueUserACLInfo> getQueueAclsInfo() {
    return null;
  }

  @Override
  public ApplicationAttemptReport getApplicationAttemptReport(
      ApplicationAttemptId applicationAttemptId) {
    return null;
  }

  @Override
  public List<ApplicationAttemptReport> getApplicationAttempts(
      ApplicationId applicationId) {
    return null;
  }

  @Override
  public ContainerReport getContainerReport(ContainerId containerId) {
    return null;
  }

  @Override
  public List<ContainerReport> getContainers(
      ApplicationAttemptId applicationAttemptId) {
    return null;
  }

  @Override
  public void moveApplicationAcrossQueues(ApplicationId appId, String queue) {
  }

  @Override
  public GetNewReservationResponse createReservation() {
    return null;
  }

  @Override
  public ReservationSubmissionResponse submitReservation(
      ReservationSubmissionRequest request) {
    return null;
  }

  @Override
  public ReservationUpdateResponse updateReservation(
      ReservationUpdateRequest request) {
    return null;
  }

  @Override
  public ReservationDeleteResponse deleteReservation(
      ReservationDeleteRequest request) {
    return null;
  }

  @Override
  public ReservationListResponse listReservations(
      ReservationListRequest request) {
    return null;
  }

  @Override
  public Map<NodeId, Set<String>> getNodeToLabels() {
    return null;
  }

  @Override
  public Map<String, Set<NodeId>> getLabelsToNodes() {
    return null;
  }

  @Override
  public Map<String, Set<NodeId>> getLabelsToNodes(Set<String> labels) {
    return null;
  }

  @Override
  public List<NodeLabel> getClusterNodeLabels() {
    return null;
  }

  @Override
  public Priority updateApplicationPriority(
      ApplicationId applicationId, Priority priority) {
    return null;
  }

  @Override
  public void signalToContainer(
      ContainerId containerId, SignalContainerCommand command) {
  }

  @Override
  public Map<String, Resource> getResourceProfiles() {
    return null;
  }

  @Override
  public Resource getResourceProfile(String profile) {
    return null;
  }

  @Override
  public List<ResourceTypeInfo> getResourceTypeInfo() {
    return null;
  }

  @Override
  public Set<NodeAttributeInfo> getClusterAttributes() {
    return null;
  }

  @Override
  public Map<NodeAttributeKey, List<NodeToAttributeValue>>
  getAttributesToNodes(Set<NodeAttributeKey> attributes) {
    return null;
  }

  @Override
  public Map<String, Set<NodeAttribute>> getNodeToAttributes(Set<String> hostNames) {
    return null;
  }

  @Override
  public void shellToContainer(
      ContainerId containerId, ShellContainerCommand command) {
  }
}
