package org.conio.container.client;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ApplicationMonitor {
    private static final Logger LOG =
            LoggerFactory.getLogger(ApplicationMonitor.class);

    private static final int TIMEOUT = 600000;

    private final YarnClient yarnClient;
    private final ApplicationId appId;
    private final long clientStartTime;

    public ApplicationMonitor(YarnClient client, ApplicationId appId, long clientStartTime) {
        this.yarnClient = client;
        this.appId = appId;
        this.clientStartTime = clientStartTime;
    }

    // is a blocking command
    public void run() throws YarnException, IOException {
        monitorApplication();
    }

    private void monitorApplication()
            throws YarnException, IOException {
        while (true) {
            // Check app status every 1 second.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.debug("Thread sleep in monitoring loop interrupted");
                break;
            }

            // Get application report for the appId we are interested in
            ApplicationReport report = yarnClient.getApplicationReport(appId);

            YarnApplicationState state = report.getYarnApplicationState();
            FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
            if (state == YarnApplicationState.FINISHED) {
                if (dsStatus == FinalApplicationStatus.SUCCEEDED) {
                    LOG.info("Application has completed successfully. Breaking monitoring loop");
                } else {
                    LOG.warn("Application finished unsuccessfully."
                            + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                            + ". Breaking monitoring loop");
                }
                return;
            }
            else if (YarnApplicationState.KILLED == state
                    || YarnApplicationState.FAILED == state) {
                LOG.warn("Application did not finish."
                        + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                        + ". Breaking monitoring loop");
                return;
            }

            // The value equal or less than 0 means no timeout
            if (System.currentTimeMillis() > (clientStartTime + TIMEOUT)) {
                LOG.info("Reached client specified timeout for application. " +
                        "Killing application");
                yarnClient.killApplication(appId);
                return;
            }
        }

    }
}
