package org.conio.client;

import java.io.IOException;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationMonitor.class);

  private static final int SLEEP_TIME = 1000;
  private static final int TIMEOUT = 600000;

  private final YarnClient yarnClient;
  private final ApplicationId appId;

  /**
   * ApplicationMonitor monitors the application, periodically querying application state from YARN.
   */
  public ApplicationMonitor(YarnClient client, ApplicationId appId) {
    this.yarnClient = client;
    this.appId = appId;
  }

  // is a blocking command
  public void run() throws YarnException, IOException {
    monitorApplication();
  }

  private void monitorApplication() throws YarnException, IOException {
    long clientStartTime = System.currentTimeMillis();
    while (true) {
      // Check app status every 1 second.
      try {
        Thread.sleep(SLEEP_TIME);
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
          LOG.warn(
              "Application finished unsuccessfully."
                  + " YarnState="
                  + state.toString()
                  + ", DSFinalStatus="
                  + dsStatus.toString()
                  + ". Breaking monitoring loop");
        }
        return;
      } else if (state == YarnApplicationState.KILLED || state == YarnApplicationState.FAILED) {
        LOG.warn(
            "Application did not finish."
                + " YarnState="
                + state.toString()
                + ", DSFinalStatus="
                + dsStatus.toString()
                + ". Breaking monitoring loop");
        return;
      }

      // The value equal or less than 0 means no timeout
      if (System.currentTimeMillis() > (clientStartTime + TIMEOUT)) {
        LOG.info("Reached client specified timeout for application. " + "Killing application");
        yarnClient.killApplication(appId);
        return;
      }
    }
  }
}
