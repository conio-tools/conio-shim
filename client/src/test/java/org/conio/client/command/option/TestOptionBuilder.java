package org.conio.client.command.option;

import static org.conio.client.command.option.CLIOption.TEST;
import static org.conio.client.command.option.CLIOption.TEST2;
import static org.conio.client.command.option.TestCLIOption.assertTestOption;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

public class TestOptionBuilder {
  @Test
  public void testEmpty() {
    Options options = OptionBuilder.builder().build();
    assertEquals(0, options.getOptions().size());
  }

  @Test
  public void testWithCLIOption() {
    Options options = OptionBuilder.builder()
        .withCLIOption(TEST)
        .build();
    assertEquals(1, options.getOptions().size());
    Option option = options.getOption(TEST.option());
    assertNotNull(option);
    assertTestOption(option);
  }

  @Test
  public void testWithCLIOptions() {
    Options options = OptionBuilder.builder()
        .withCLIOptions(TEST, TEST2)
        .build();
    assertEquals(2, options.getOptions().size());
    Option option = options.getOption(TEST.option());
    assertNotNull(option);
    assertTestOption(option);
    Option option2 = options.getOption(TEST2.option());
    assertNotNull(option2);
  }

  @Test
  public void testWithCLIOptionProvider() {
    TestCLIOptionProvider provider = new TestCLIOptionProvider();
    Options options = OptionBuilder.builder()
        .withCLIOptionProvider(provider)
        .build();
    assertEquals(1, options.getOptions().size());
    Option option = options.getOption(TEST.option());
    assertNotNull(option);
    assertTestOption(option);
  }

  static class TestCLIOptionProvider implements CLIOptionProvider {
    @Override
    public Collection<CLIOption> collectCLIOptions() {
      return Collections.singletonList(TEST);
    }
  }
}
