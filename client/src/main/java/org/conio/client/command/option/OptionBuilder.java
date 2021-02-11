package org.conio.client.command.option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.Options;

public class OptionBuilder {
  private final List<CLIOption> cliOptions = new ArrayList<>();

  public OptionBuilder() {
  }

  public static OptionBuilder builder() {
    return new OptionBuilder();
  }

  public OptionBuilder withCLIOption(CLIOption option) {
    cliOptions.add(option);
    return this;
  }

  public OptionBuilder withCLIOptions(CLIOption... options) {
    cliOptions.addAll(Arrays.asList(options));
    return this;
  }

  public OptionBuilder withCLIOptionProvider(CLIOptionProvider provider) {
    cliOptions.addAll(provider.collectCLIOptions());
    return this;
  }

  /**
   * Builds the {@code Options} object.
   */
  public Options build() {
    Options opts = new Options();
    for (CLIOption cliOption : cliOptions) {
      opts.addOption(cliOption.toOption());
    }
    return opts;
  }
}
