package org.conio.container;

import org.conio.container.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class which runs the client.
 */
public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  /**
   * Entrypoint of the program.
   */
  public static void main(String[] args) {
    Client client = new Client();
    try {
      client.init(args);
      client.run();
    } catch (Exception e) {
      LOG.error("Fatal exception, exiting.", e);
      System.exit(1);
    } finally {
      client.cleanup();
    }
  }
}
