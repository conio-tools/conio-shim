package org.conio.client.command.option;

import java.util.Collection;

public interface CLIOptionProvider {
  Collection<CLIOption> collectCLIOptions();
}
