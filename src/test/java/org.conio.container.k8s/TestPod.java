package org.conio.container.k8s;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableList;
import java.io.FileNotFoundException;
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
    assertNull(container.getResources());
  }
}
