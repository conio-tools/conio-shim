package org.conio.container.engine;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.conio.container.zookeeper.ClientWrapper;

public class MockClientWrapper extends ClientWrapper {
  public MockClientWrapper() {
    super(null);
  }

  @Override
  public void init(String zkConnectionString) {
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
