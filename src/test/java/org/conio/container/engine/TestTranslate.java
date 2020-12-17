package org.conio.container.engine;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import org.apache.hadoop.yarn.api.records.Resource;
import org.conio.container.engine.util.Translate;
import org.conio.container.k8s.Pod;
import org.junit.Test;

public class TestTranslate {
  @Test
  public void testTranslateResourceRequirements() throws FileNotFoundException {
    Pod pod = Pod.parseFromFile("src/test/resources/sleep_pod.yaml");
    Resource res = Translate.translateResourceRequirements(pod.getSpec().getContainers().get(0));
    assertEquals(200L, res.getMemorySize());
    assertEquals(1, res.getVirtualCores());
  }
}
