package org.conio.container.engine.mock;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.conio.zookeeper.ClientWrapper;

public class MockClientWrapper extends ClientWrapper {
  public MockClientWrapper() {
    super();
  }

  @Override
  public void init(String zkRoot, String zkConnectionString) {
  }

  @Override
  public void start() {
  }

  @Override
  public byte[] downloadPod(String namespace, String name) throws Exception {
    File file = new File("src/test/resources/sleep_pod.yaml");
    return FileUtils.readFileToByteArray(file);
  }
}
