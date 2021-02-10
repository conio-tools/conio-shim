package org.conio.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.junit.Test;


public class TestApplicationMonitor {
  private YarnClient createMockedYarnClient(ApplicationId appId) throws IOException, YarnException {
    YarnClient client = mock(YarnClient.class);
    ApplicationReport report =
        ApplicationReport.newInstance(
            appId, ApplicationAttemptId.newInstance(appId, 1), "test-user",
            "test-queue", "test-name", "test-host", 0, null,
            YarnApplicationState.FINISHED, "", "", 0, 0, 0,
            FinalApplicationStatus.SUCCEEDED, null, "", 0, "", null);
    when(client.getApplicationReport(appId)).thenReturn(report);
    return client;
  }

  @Test(timeout = 3000)
  public void testApplicationMonitor() throws IOException, YarnException {
    long millis = System.currentTimeMillis();
    ApplicationId appId = ApplicationId.newInstance(millis, 1);
    YarnClient client = createMockedYarnClient(appId);
    ApplicationMonitor appMonitor = new ApplicationMonitor(client, appId);
    appMonitor.run();
  }
}
