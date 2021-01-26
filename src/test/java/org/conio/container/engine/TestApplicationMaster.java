package org.conio.container.engine;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.conio.container.engine.mock.MockAMRMImpl;
import org.conio.container.engine.mock.MockClientWrapper;
import org.conio.container.engine.mock.MockEnvVarProvider;
import org.conio.container.engine.mock.MockNMClientAsync;
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
    MockNMClientAsync nmClient = new MockNMClientAsync();
    doReturn(nmClient).when(am).getNmClientAsync();

    // Run the application master
    am.init();

    RMCallbackHandler rmCallbackHandler = new RMCallbackHandler(am.getContext(), nmClient);
    nmClient.setRMCallbackHandler(rmCallbackHandler);
    AMRMClientAsync<AMRMClient.ContainerRequest> amrmClient = new MockAMRMImpl(rmCallbackHandler);
    doReturn(amrmClient).when(am).getAmRMClient();

    am.run();
    am.finish();
    am.cleanup();
  }
}
