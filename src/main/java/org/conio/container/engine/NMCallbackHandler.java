package org.conio.container.engine;

import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.ContainerUpdateType;
import org.apache.hadoop.yarn.api.records.ExecutionType;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.UpdateContainerRequest;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// TODO reimplement this
public class NMCallbackHandler extends NMClientAsync.AbstractCallbackHandler {
    private static final Logger LOG =
            LoggerFactory.getLogger(NMCallbackHandler.class);

    private ConcurrentMap<ContainerId, Container> containers =
            new ConcurrentHashMap<ContainerId, Container>();
    private final ApplicationMaster applicationMaster;

    public NMCallbackHandler(ApplicationMaster applicationMaster) {
        this.applicationMaster = applicationMaster;
    }

    public void addContainer(ContainerId containerId, Container container) {
        containers.putIfAbsent(containerId, container);
    }

    @Override
    public void onContainerStopped(ContainerId containerId) {
        LOG.debug("Succeeded to stop Container {}", containerId);
        containers.remove(containerId);
    }

    @Override
    public void onContainerStatusReceived(ContainerId containerId,
                                          ContainerStatus containerStatus) {
        LOG.debug("Container Status: id={}, status={}", containerId,
                containerStatus);

        // If promote_opportunistic_after_start is set, automatically promote
        // opportunistic containers to guaranteed.
        /*if (autoPromoteContainers) {
            if (containerStatus.getState() == ContainerState.RUNNING) {
                Container container = containers.get(containerId);
                if (container.getExecutionType() == ExecutionType.OPPORTUNISTIC) {
                    // Promote container
                    LOG.info("Promoting container {} to {}", container.getId(),
                            container.getExecutionType());
                    UpdateContainerRequest updateRequest = UpdateContainerRequest
                            .newInstance(container.getVersion(), container.getId(),
                                    ContainerUpdateType.PROMOTE_EXECUTION_TYPE, null,
                                    ExecutionType.GUARANTEED);
                    amRMClient.requestContainerUpdate(container, updateRequest);
                }
            }
        }*/
    }

    @Override
    public void onContainerStarted(ContainerId containerId,
                                   Map<String, ByteBuffer> allServiceResponse) {
        LOG.debug("Succeeded to start Container {}", containerId);
        /*Container container = containers.get(containerId);
        if (container != null) {
            applicationMaster.nmClientAsync.getContainerStatusAsync(
                    containerId, container.getNodeId());
        }
        if (applicationMaster.timelineServiceV2Enabled) {
            long startTime = SystemClock.getInstance().getTime();
            applicationMaster.getContainerStartTimes().put(containerId, startTime);
            applicationMaster.publishContainerStartEventOnTimelineServiceV2(
                    container, startTime);
        }
        if (applicationMaster.timelineServiceV1Enabled) {
            applicationMaster.publishContainerStartEvent(
                    applicationMaster.timelineClient, container,
                    applicationMaster.domainId, applicationMaster.appSubmitterUgi);
        }*/
    }

    @Override
    public void onStartContainerError(ContainerId containerId, Throwable t) {
        LOG.error("Failed to start Container {}", containerId, t);
        /*containers.remove(containerId);
        applicationMaster.numCompletedContainers.incrementAndGet();
        applicationMaster.numFailedContainers.incrementAndGet();
        if (timelineServiceV2Enabled) {
            publishContainerStartFailedEventOnTimelineServiceV2(containerId,
                    t.getMessage());
        }
        if (timelineServiceV1Enabled) {
            publishContainerStartFailedEvent(containerId, t.getMessage());
        }*/
    }

    @Override
    public void onGetContainerStatusError(
            ContainerId containerId, Throwable t) {
        LOG.error("Failed to query the status of Container " + containerId);
    }

    @Override
    public void onStopContainerError(ContainerId containerId, Throwable t) {
        LOG.error("Failed to stop Container " + containerId);
        containers.remove(containerId);
    }

    @Deprecated
    @Override
    public void onIncreaseContainerResourceError(
            ContainerId containerId, Throwable t) {}

    @Deprecated
    @Override
    public void onContainerResourceIncreased(
            ContainerId containerId, Resource resource) {}

    @Override
    public void onUpdateContainerResourceError(
            ContainerId containerId, Throwable t) {
    }

    @Override
    public void onContainerResourceUpdated(ContainerId containerId,
                                           Resource resource) {
    }
}
