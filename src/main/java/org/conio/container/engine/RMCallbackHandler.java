package org.conio.container.engine;

import io.netty.buffer.ByteBuf;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.RejectedSchedulingRequest;
import org.apache.hadoop.yarn.api.records.UpdatedContainer;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RMCallbackHandler extends AMRMClientAsync.AbstractCallbackHandler {
    private static final Logger LOG =
            LoggerFactory.getLogger(RMCallbackHandler.class);

    private final NMClientAsync nmClientAsync;
    private AtomicBoolean done;

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
        LOG.info("Got response from RM for container ask, allocatedCnt="
                + allocatedContainers.size());

        ByteBuffer allTokens;
        try {
            Credentials credentials =
                    UserGroupInformation.getCurrentUser().getCredentials();
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
        } catch (IOException ioe) {
            throw new RuntimeException("unexpected exception", ioe);
        }

        for (Container container : allocatedContainers) {
            // TODO parameterize these
            List<String> commands = Arrays.asList("sleep", "60");
            String image = "library/ubuntu:latest";

            Map<String, String> env = new HashMap<String, String>();
            env.put("YARN_CONTAINER_RUNTIME_TYPE", "docker");
            env.put("YARN_CONTAINER_RUNTIME_DOCKER_IMAGE", image);
            env.put("YARN_CONTAINER_RUNTIME_DOCKER_RUN_OVERRIDE_DISABLE", "true");
            env.put("YARN_CONTAINER_RUNTIME_DOCKER_DELAYED_REMOVAL", "true");

            ContainerLaunchContext ctx = ContainerLaunchContext.newInstance(
                    new HashMap<String, LocalResource>(), env, commands, null, allTokens.duplicate(),
                    null, null);
            nmClientAsync.startContainerAsync(container, ctx);
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
        //List<SchedulingRequest> reqsToRetry = new ArrayList<>();
        for (RejectedSchedulingRequest rejReq : rejReqs) {
            LOG.info("Scheduling Request {} has been rejected. Reason {}",
                    rejReq.getRequest(), rejReq.getReason());
            //reqsToRetry.add(rejReq.getRequest());
        }
        /*totalRetries.addAndGet(-1 * reqsToRetry.size());
        if (totalRetries.get() <= 0) {
            LOG.info("Exiting, since retries are exhausted !!");
            done = true;
        } else {
            amRMClient.addSchedulingRequests(reqsToRetry);
        }*/
    }

    @Override
    public void onShutdownRequest() {
    }

    @Override
    public void onNodesUpdated(List<NodeReport> updatedNodes) {
    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public void onError(Throwable e) {
        LOG.error("Error in RMCallbackHandler: ", e);
        // done = true;
    }

    public AtomicBoolean isFinished() {
        return done;
    }
}
