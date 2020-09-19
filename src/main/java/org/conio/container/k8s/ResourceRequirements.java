package org.conio.container.k8s;

import java.util.Map;

public class ResourceRequirements {
    private Map<String, String> limits;
    private Map<String, String> requests;

    public ResourceRequirements() {
    }
}
