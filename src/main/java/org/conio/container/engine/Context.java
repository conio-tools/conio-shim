package org.conio.container.engine;

import org.conio.container.k8s.Pod;

public class Context {
  private Pod pod;

  Context(Pod pod) {
    this.pod = pod;
  }

  Pod getPod() {
    return pod;
  }
}
