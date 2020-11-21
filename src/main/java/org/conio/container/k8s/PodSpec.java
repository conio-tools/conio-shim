package org.conio.container.k8s;

import java.util.List;

public class PodSpec {
    private List<Container> containers;
    private String restartPolicy;

    public PodSpec(List<Container> containers, String restartPolicy) {
        this.containers = containers;
        this.restartPolicy = restartPolicy;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public String getRestartPolicy() {
        return restartPolicy;
    }
}
