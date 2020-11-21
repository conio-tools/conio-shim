package org.conio.container.k8s;

import java.util.Map;

public class Metadata {
    private final String name;
    private final String namespace;
    private final Map<String, String> annotations;
    private final Map<String, String> labels;

    public Metadata(String name, String namespace, Map<String, String> annotations, Map<String, String> labels) {
        this.name = name;
        this.namespace = namespace;
        this.annotations = annotations;
        this.labels = labels;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public Map<String, String> getLabels() {
        return labels;
    }
}
