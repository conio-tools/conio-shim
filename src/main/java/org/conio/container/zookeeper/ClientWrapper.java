package org.conio.container.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

public class ClientWrapper {
  private CuratorFramework zkClient;

  public ClientWrapper(String zkConnectionString) {
    RetryPolicy retryPolicy = new RetryNTimes(10, 1000);
    zkClient = CuratorFrameworkFactory.newClient(zkConnectionString, retryPolicy);
  }

  public void start() {
    zkClient.start();
  }

  public void close() {
    zkClient.close();
  }

  public void createZkPath(String path) {

  }

  public boolean zkPathExists(String path) {

  }

  public String getZkPathData(String path) {

  }
}
