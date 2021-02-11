package org.conio.client.command.option;

import static org.conio.client.command.option.CLIOption.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.cli.Option;
import org.junit.Test;

public class TestCLIOption {
  @Test
  public void testQueue() {
    assertEquals("test", TEST.option());
    assertTestOption(TEST.toOption());
  }

  static void assertTestOption(Option opt) {
    assertEquals("t", opt.getOpt());
    assertEquals("test", opt.getLongOpt());
    assertTrue(opt.hasArg());
    assertNotNull(opt.getDescription());
  }
}
