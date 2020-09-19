package org.conio.container.engine;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

public class ApplicationMaster {
    private static final Logger LOG = LoggerFactory
            .getLogger(ApplicationMaster.class);

    private final Configuration conf;

    public static void main(String[] args) throws Exception {
        ApplicationMaster appMaster = null;
        try {
            appMaster = new ApplicationMaster();
            appMaster.init(args);
            appMaster.run();
            appMaster.finish();
        } finally {
            if (appMaster != null) {
                appMaster.cleanup();
            }
        }
    }

    private static Options createOptions() {
        // TODO: create a way to handle the same options for the client and the AM
        return new Options();
    }

    // TODO: where should this function be placed?
    public ApplicationMaster() {
        // TODO do we need YarnConfiguration?
        conf = new YarnConfiguration();
    }

    private void init(String[] args) throws ParseException, IOException {
        Options opts = createOptions();

        if (args.length == 0) {
            new HelpFormatter().printHelp("ApplicationMaster", opts);
            throw new IllegalArgumentException(
                    "No args specified for application master to initialize");
        }

        CommandLine cliParser = new GnuParser().parse(opts, args);

        // TODO set up AM based on cliParser arguments

        Map<String, String> envs = System.getenv();

        if (!envs.containsKey(ApplicationConstants.Environment.CONTAINER_ID.name())) {
            throw new RuntimeException("Expected container ID among the environment variables");
        }
        ContainerId containerId = ContainerId.fromString(envs
                .get(ApplicationConstants.Environment.CONTAINER_ID.name()));
        ApplicationAttemptId appAttemptID = containerId.getApplicationAttemptId();
        ApplicationId appId = appAttemptID.getApplicationId();
    }

    private void run() throws IOException, YarnException {
        // TODO token setup
        {
            Credentials credentials =
                    UserGroupInformation.getCurrentUser().getCredentials();
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            // Now remove the AM->RM token so that containers cannot access it.
            Iterator<Token<?>> iter = credentials.getAllTokens().iterator();
            LOG.info("Executing with tokens:");
            while (iter.hasNext()) {
                Token<?> token = iter.next();
                LOG.info(token.toString());
                if (token.getKind().equals(AMRMTokenIdentifier.KIND_NAME)) {
                    iter.remove();
                }
            }
            ByteBuffer allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());

            String appSubmitterUserName =
                    System.getenv(ApplicationConstants.Environment.USER.name());
            UserGroupInformation appSubmitterUgi =
                    UserGroupInformation.createRemoteUser(appSubmitterUserName);
            appSubmitterUgi.addCredentials(credentials);
        }

        AMRMClientAsync.AbstractCallbackHandler allocListener =
                new RMCallbackHandler();
        AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient = AMRMClientAsync.createAMRMClientAsync(1000, allocListener);
        amRMClient.init(conf);
        amRMClient.start();

        NMCallbackHandler containerListener = new NMCallbackHandler(this);
        NMClientAsync nmClientAsync = new NMClientAsyncImpl(containerListener);
        nmClientAsync.init(conf);
        nmClientAsync.start();

        String appMasterHostname = NetUtils.getHostname();

        RegisterApplicationMasterResponse response = amRMClient
                .registerApplicationMaster(appMasterHostname, -1,
                        "", null);

    }

    private void finish() {
        // wait for completion.
        while (!done
                && (numCompletedContainers.get() != numTotalContainers)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
            }
        }

        // Join all launched threads
        // needed for when we time out
        // and we need to release containers
        for (Thread launchThread : launchThreads) {
            try {
                launchThread.join(10000);
            } catch (InterruptedException e) {
                LOG.info("Exception thrown in thread join: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // When the application completes, it should stop all running containers
        LOG.info("Application completed. Stopping running containers");
        nmClientAsync.stop();

        // When the application completes, it should send a finish application
        // signal to the RM
        LOG.info("Application completed. Signalling finished to RM");

        FinalApplicationStatus appStatus;
        boolean success = true;
        String message = null;
        if (numCompletedContainers.get() - numFailedContainers.get()
                >= numTotalContainers) {
            appStatus = FinalApplicationStatus.SUCCEEDED;
        } else {
            appStatus = FinalApplicationStatus.FAILED;
            message = String.format("Application Failure: desired = %d, " +
                            "completed = %d, allocated = %d, failed = %d, " +
                            "diagnostics = %s", numRequestedContainers.get(),
                    numCompletedContainers.get(), numAllocatedContainers.get(),
                    numFailedContainers.get(), diagnostics);
            success = false;
        }
        try {
            amRMClient.unregisterApplicationMaster(appStatus, message, null);
        } catch (YarnException | IOException ex) {
            LOG.error("Failed to unregister application", ex);
        }
        amRMClient.stop();
    }
}
