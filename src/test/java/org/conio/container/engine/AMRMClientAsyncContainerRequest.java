package org.conio.container.engine;

import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;

public abstract class AMRMClientAsyncContainerRequest extends AMRMClientAsync<AMRMClient.ContainerRequest> {
  AMRMClientAsyncContainerRequest(int intervalMs, AMRMClientAsync.AbstractCallbackHandler callbackHandler) {
    super(intervalMs, callbackHandler);
  }
}
