package org.conio.container.k8s;

public enum RestartPolicy {
  ALWAYS("Always"),
  ON_FAILURE("OnFailure"),
  NEVER("Never");

  private String value;

  RestartPolicy(String value) {
    this.value = value;
  }

  static RestartPolicy fromString(String text) {
    for (RestartPolicy restartPolicy : RestartPolicy.values()) {
      if (restartPolicy.value.equalsIgnoreCase(text)) {
        return restartPolicy;
      }
    }
    return null;
  }
}
