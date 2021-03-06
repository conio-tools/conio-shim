package org.conio.container.k8s;

import java.util.Map;

public class Metadata {
  private static final String DEFAULT_NAMESPACE = "default";

  private String name;
  private String namespace;
  private Map<String, String> annotations;
  private Map<String, String> labels;

  public Metadata() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  /**
   * Get the non-null namespace of this object.
   */
  public String getExactNamespace() {
    String namespace = getNamespace();
    if (namespace == null) {
      return DEFAULT_NAMESPACE;
    } else {
      return namespace;
    }
  }

  public Map<String, String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
}
