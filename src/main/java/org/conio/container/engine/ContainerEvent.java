package org.conio.container.engine;

public class ContainerEvent {
  private final long timestamp;
  private final ContainerEventType type;

  ContainerEvent(ContainerEventType type) {
    this.type = type;
    this.timestamp = System.currentTimeMillis();
  }

  public ContainerEventType getType() {
    return type;
  }
}
