package org.conio.container.k8s;

public class Pod {
    private String apiVersion;
    private String kind;
    private Metadata metadata;
    private PodSpec spec;

    public Pod(String apiVersion, String kind, Metadata metadata, PodSpec spec) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.spec = spec;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public PodSpec getSpec() {
        return spec;
    }
}
