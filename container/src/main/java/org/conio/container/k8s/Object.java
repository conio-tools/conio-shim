package org.conio.container.k8s;

public class Object {
  private String apiVersion;
  private String kind;
  private Metadata metadata;

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  /**
   * Returns the metadata of the pod.
   * Throws RuntimeException if the metadata is null.
   */
  public Metadata getMetadata() {
    if (metadata == null) {
      throw new RuntimeException("Metadata field is null");
    }
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }
}
