package org.conio.container.k8s;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableList;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TestPod {
  @Test
  public void testParseFromFile() throws FileNotFoundException {
    Pod pod = Pod.parseFromFile("src/test/resources/sleep_pod.yaml");

    // object
    Metadata meta = pod.getMetadata();
    assertNull(meta.getAnnotations());
    assertEquals("sleep-pod", meta.getName());
    assertNull(meta.getNamespace());
    assertEquals("default", meta.getExactNamespace());
    Map<String, String> labels = meta.getLabels();
    assertEquals("sleep", labels.get("app"));

    // pod
    PodSpec podSpec = pod.getSpec();
    assertEquals("Never", podSpec.getRestartPolicy());

    // container
    assertEquals(1, podSpec.getContainers().size());
    Container container = podSpec.getContainers().get(0);
    assertNull(container.getEnv());
    assertEquals(ImmutableList.of("sleep", "60"), container.getCommand());
    assertEquals("busybox", container.getImage());
    assertEquals("IfNotPresent", container.getImagePullPolicy());
    assertEquals("sleep-container", container.getName());
    // resources
    Map<String, String> limits = new HashMap<>();
    limits.put("cpu", "1");
    limits.put("memory", "200Mi");
    Map<String, String> requests = new HashMap<>();
    requests.put("cpu", "500m");
    requests.put("memory", "100Mi");
    ResourceRequirements resources = container.getResources();
    assertEquals(resources.getLimits(), limits);
    assertEquals(resources.getRequests(), requests);
  }
}
