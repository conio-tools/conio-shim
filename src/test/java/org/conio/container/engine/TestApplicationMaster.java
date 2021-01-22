package org.conio.container.engine;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.junit.Test;

public class TestApplicationMaster {
  @Test(timeout = 20000)
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
    doNothing().when(amrmClient).init(any());
    doNothing().when(amrmClient).start();
    RegisterApplicationMasterResponse resp =
        RegisterApplicationMasterResponse.newInstance(
            null, null, null, null,  null, null, null);
    doReturn(resp).when(amrmClient).registerApplicationMaster(anyString(), anyInt(), anyString(), any());
    doReturn(amrmClient).when(am).getAmRMClient();
    RMCallbackHandler rmCallbackHandler = mock(RMCallbackHandler.class);
    doReturn(rmCallbackHandler).when(am).getRmCallbackHandler();
    NMClientAsync nmClient = mock(NMClientAsync.class);
    doReturn(nmClient).when(am).getNmClientAsync();

    ApplicationMaster.runAppMaster(am);
  }
}
