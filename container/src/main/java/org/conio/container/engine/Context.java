package org.conio.container.engine;

import org.conio.container.k8s.Pod;

public class Context {
  private final Pod pod;
  private final ContainerTracker tracker;

  Context(Pod pod) {
    this.pod = pod;
    this.tracker = new ContainerTracker(
        pod.getSpec().getContainers(), pod.getSpec().getRestartPolicyObject());
  }

  Pod getPod() {
    return pod;
  }

  public ContainerTracker getContainerTracker() {
    return tracker;
  }
}
