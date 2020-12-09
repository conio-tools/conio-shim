package org.conio.container.k8s;

import java.util.List;

public class PodSpec {
  private List<Container> containers;
  private String restartPolicy;

  public PodSpec() {}

  public List<Container> getContainers() {
    return containers;
  }

  public void setContainers(List<Container> containers) {
    this.containers = containers;
  }

  public String getRestartPolicy() {
    return restartPolicy;
  }

  public void setRestartPolicy(String restartPolicy) {
    this.restartPolicy = restartPolicy;
  }
}
