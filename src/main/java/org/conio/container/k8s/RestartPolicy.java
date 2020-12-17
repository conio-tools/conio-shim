package org.conio.container.k8s;

public enum RestartPolicy {
  ALWAYS("Always"),
  ON_FAILURE("OnFailure"),
  NEVER("Never");

  private String value;

  RestartPolicy(String value) {
    this.value = value;
  }
}
