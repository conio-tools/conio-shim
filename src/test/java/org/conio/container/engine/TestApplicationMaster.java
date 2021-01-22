package org.conio.container.engine;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.junit.Test;

public class TestApplicationMaster {
  @Test
  public void testApplicationMaster() throws Exception {
    AMRMClientAsync<AMRMClient.ContainerRequest> amrmClient = mock(AMRMClientAsyncContainerRequest.class);
    RMCallbackHandler rmCallbackHandler = mock(RMCallbackHandler.class);
    NMClientAsync nmClient = mock(NMClientAsync.class);
    MockClientWrapper mockZkClient = new MockClientWrapper();
    MockEnvVarProvider envVars = new MockEnvVarProvider();
    envVars.put(ApplicationConstants.Environment.CONTAINER_ID.name(), "container_1111111111111_0001_01_000001");
    envVars.put(ApplicationConstants.Environment.USER.name(), "test");

    ApplicationMaster am = spy(new ApplicationMaster());
    doReturn(envVars).when(am).getEnvVarProvider();
    doReturn(mockZkClient).when(am).getZkClient();
    am.setAmRMClient(amrmClient);
    am.setRmCallbackHandler(rmCallbackHandler);
    am.setNmClientAsync(nmClient);

    ApplicationMaster.run(am);
  }
}
