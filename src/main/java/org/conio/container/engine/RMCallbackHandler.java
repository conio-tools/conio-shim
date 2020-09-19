package org.conio.container.engine;

import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerExitStatus;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.RejectedSchedulingRequest;
import org.apache.hadoop.yarn.api.records.SchedulingRequest;
import org.apache.hadoop.yarn.api.records.UpdatedContainer;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

// TODO reimplement these
public class RMCallbackHandler extends AMRMClientAsync.AbstractCallbackHandler {
    private static final Logger LOG =
            LoggerFactory.getLogger(RMCallbackHandler.class);

    @Override
    public void onContainersCompleted(List<ContainerStatus> completedContainers) {
        LOG.info("Got response from RM for container ask, completedCnt="
                + completedContainers.size());
        for (ContainerStatus containerStatus : completedContainers) {
            String message = appAttemptID + " got container status for containerID="
                    + containerStatus.getContainerId() + ", state="
                    + containerStatus.getState() + ", exitStatus="
                    + containerStatus.getExitStatus() + ", diagnostics="
                    + containerStatus.getDiagnostics();
            if (containerStatus.getExitStatus() != 0) {
                LOG.error(message);
                diagnostics.append(containerStatus.getDiagnostics());
            } else {
                LOG.info(message);
            }

            // non complete containers should not be here
            assert (containerStatus.getState() == ContainerState.COMPLETE);
            // ignore containers we know nothing about - probably from a previous
            // attempt
            if (!launchedContainers.contains(containerStatus.getContainerId())) {
                LOG.info("Ignoring completed status of "
                        + containerStatus.getContainerId()
                        + "; unknown container(probably launched by previous attempt)");
                continue;
            }

            // increment counters for completed/failed containers
            int exitStatus = containerStatus.getExitStatus();
            if (0 != exitStatus) {
                // container failed
                if (ContainerExitStatus.ABORTED != exitStatus) {
                    // shell script failed
                    // counts as completed
                    numCompletedContainers.incrementAndGet();
                    numFailedContainers.incrementAndGet();
                } else {
                    // container was killed by framework, possibly preempted
                    // we should re-try as the container was lost for some reason
                    numAllocatedContainers.decrementAndGet();
                    numRequestedContainers.decrementAndGet();
                    // we do not need to release the container as it would be done
                    // by the RM

                    // Ignore these containers if placementspec is enabled
                    // for the time being.
                    if (placementSpecs != null) {
                        numIgnore.incrementAndGet();
                    }
                }
            } else {
                // nothing to do
                // container completed successfully
                numCompletedContainers.incrementAndGet();
                LOG.info("Container completed successfully." + ", containerId="
                        + containerStatus.getContainerId());
            }
            if (timelineServiceV2Enabled) {
                Long containerStartTime =
                        containerStartTimes.get(containerStatus.getContainerId());
                if (containerStartTime == null) {
                    containerStartTime = SystemClock.getInstance().getTime();
                    containerStartTimes.put(containerStatus.getContainerId(),
                            containerStartTime);
                }
                publishContainerEndEventOnTimelineServiceV2(containerStatus,
                        containerStartTime);
            }
            if (timelineServiceV1Enabled) {
                publishContainerEndEvent(timelineClient, containerStatus, domainId,
                        appSubmitterUgi);
            }
        }

        // ask for more containers if any failed
        int askCount = numTotalContainers - numRequestedContainers.get();
        numRequestedContainers.addAndGet(askCount);

        // Dont bother re-asking if we are using placementSpecs
        if (placementSpecs == null) {
            if (askCount > 0) {
                for (int i = 0; i < askCount; ++i) {
                    AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM();
                    amRMClient.addContainerRequest(containerAsk);
                }
            }
        }

        if (numCompletedContainers.get() + numIgnore.get() >=
                numTotalContainers) {
            done = true;
        }
    }

    @Override
    public void onContainersAllocated(List<Container> allocatedContainers) {
        LOG.info("Got response from RM for container ask, allocatedCnt="
                + allocatedContainers.size());
        for (Container allocatedContainer : allocatedContainers) {
            if (numAllocatedContainers.get() == numTotalContainers) {
                LOG.info("The requested number of containers have been allocated."
                        + " Releasing the extra container allocation from the RM.");
                amRMClient.releaseAssignedContainer(allocatedContainer.getId());
            } else {
                numAllocatedContainers.addAndGet(1);
                String yarnShellId = Integer.toString(yarnShellIdCounter);
                yarnShellIdCounter++;
                LOG.info(
                        "Launching shell command on a new container."
                                + ", containerId=" + allocatedContainer.getId()
                                + ", yarnShellId=" + yarnShellId
                                + ", containerNode="
                                + allocatedContainer.getNodeId().getHost()
                                + ":" + allocatedContainer.getNodeId().getPort()
                                + ", containerNodeURI="
                                + allocatedContainer.getNodeHttpAddress()
                                + ", containerResourceMemory"
                                + allocatedContainer.getResource().getMemorySize()
                                + ", containerResourceVirtualCores"
                                + allocatedContainer.getResource().getVirtualCores());

                Thread launchThread =
                        createLaunchContainerThread(allocatedContainer, yarnShellId);

                // launch and start the container on a separate thread to keep
                // the main thread unblocked
                // as all containers may not be allocated at one go.
                launchThreads.add(launchThread);
                launchedContainers.add(allocatedContainer.getId());
                launchThread.start();

                // Remove the corresponding request
                Collection<AMRMClient.ContainerRequest> requests =
                        amRMClient.getMatchingRequests(
                                allocatedContainer.getAllocationRequestId());
                if (requests.iterator().hasNext()) {
                    AMRMClient.ContainerRequest request = requests.iterator().next();
                    amRMClient.removeContainerRequest(request);
                }
            }
        }
    }

    @Override
    public void onContainersUpdated(
            List<UpdatedContainer> containers) {
        for (UpdatedContainer container : containers) {
            LOG.info("Container {} updated, updateType={}, resource={}, "
                            + "execType={}",
                    container.getContainer().getId(),
                    container.getUpdateType().toString(),
                    container.getContainer().getResource().toString(),
                    container.getContainer().getExecutionType());

            // TODO Remove this line with finalized updateContainer API.
            // Currently nm client needs to notify the NM to update container
            // execution type via NMClient#updateContainerResource() or
            // NMClientAsync#updateContainerResourceAsync() when
            // auto-update.containers is disabled, but this API is
            // under evolving and will need to be replaced by a proper new API.
            nmClientAsync.updateContainerResourceAsync(container.getContainer());
        }
    }

    @Override
    public void onRequestsRejected(List<RejectedSchedulingRequest> rejReqs) {
        List<SchedulingRequest> reqsToRetry = new ArrayList<>();
        for (RejectedSchedulingRequest rejReq : rejReqs) {
            LOG.info("Scheduling Request {} has been rejected. Reason {}",
                    rejReq.getRequest(), rejReq.getReason());
            reqsToRetry.add(rejReq.getRequest());
        }
        totalRetries.addAndGet(-1 * reqsToRetry.size());
        if (totalRetries.get() <= 0) {
            LOG.info("Exiting, since retries are exhausted !!");
            done = true;
        } else {
            amRMClient.addSchedulingRequests(reqsToRetry);
        }
    }

    @Override public void onShutdownRequest() {
        if (keepContainersAcrossAttempts) {
            LOG.info("Shutdown request received. Ignoring since "
                    + "keep_containers_across_application_attempts is enabled");
        } else{
            LOG.info("Shutdown request received. Processing since "
                    + "keep_containers_across_application_attempts is disabled");
            done = true;
        }
    }

    @Override
    public void onNodesUpdated(List<NodeReport> updatedNodes) {}

    @Override
    public float getProgress() {
        // set progress to deliver to RM on next heartbeat
        float progress = (float) numCompletedContainers.get()
                / numTotalContainers;
        return progress;
    }

    @Override
    public void onError(Throwable e) {
        LOG.error("Error in RMCallbackHandler: ", e);
        done = true;
    }
}
