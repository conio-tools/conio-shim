package org.conio.client;

import org.conio.client.command.Command;
import org.conio.client.command.Create;
import org.conio.client.command.Delete;
import org.conio.client.command.Get;
import org.conio.client.command.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Client {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  public Client() {
  }

  /**
   * Initializes the client by parsing the input arguments.
   */
  public void run(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("No args specified for client to initialize");
      // TODO display options?
    }

    // TODO handle args[0].equals("help"); case

    Command command;
    String commandString = args[0];
    switch (commandString) {
      case "create":
        command = new Create();
        break;
      case "get":
        command = new Get();
        break;
      case "apply":
        command = new Update();
      case "delete":
        command = new Delete();
        break;
      default:
        LOG.error("Could not find command: {}", commandString);
        throw new IllegalArgumentException("Could not find command: " + commandString);
    }

    String[] cutArgs = Arrays.copyOfRange(args, 1, args.length);

    try {
      command.init(cutArgs);
      command.run();
    } catch (Exception ignore) {
      command.cleanup();
    }
  }

  public static void main(String[] args) {
    Client client = new Client();
    try {
      client.run(args);
    } catch (Exception e) {
      LOG.error("Fatal exception, exiting.", e);
      System.exit(1);
    }
  }
}
