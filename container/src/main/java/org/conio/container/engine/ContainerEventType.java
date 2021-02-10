package org.conio.container.engine;

public enum ContainerEventType {
  NONE,
  STARTED,
  FAILED,
  SUCCEEDED;

  boolean isTerminated() {
    return this.equals(SUCCEEDED) || this.equals(FAILED);
  }
}
