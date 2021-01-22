package org.conio.container.engine;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.junit.Test;

public class TestApplicationMaster {
  @Test
  public void testApplicationMaster() throws Exception {
    ApplicationMaster am = spy(new ApplicationMaster());
    MockEnvVarProvider envVars = new MockEnvVarProvider();
    envVars.put(ApplicationConstants.Environment.CONTAINER_ID.name(),
        "container_1111111111111_0001_01_000001");
    envVars.put(ApplicationConstants.Environment.USER.name(), "test");
    doReturn(envVars).when(am).getEnvVarProvider();
    MockClientWrapper mockZkClient = new MockClientWrapper();
    doReturn(mockZkClient).when(am).getZkClient();
    AMRMClientAsync<AMRMClient.ContainerRequest> amrmClient =
        mock(AMRMClientAsyncContainerRequest.class);
    am.setAmRMClient(amrmClient);
    RMCallbackHandler rmCallbackHandler = mock(RMCallbackHandler.class);
    am.setRmCallbackHandler(rmCallbackHandler);
    NMClientAsync nmClient = mock(NMClientAsync.class);
    am.setNmClientAsync(nmClient);

    ApplicationMaster.runAppMaster(am);
  }
}
