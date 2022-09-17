import java.net.Socket;
import java.net.ServerSocket;


public class Node {
  private State state;

  public static void main(String[] args) {
    /**
      Pseudocode

      1. read the config file to get the parameters
      2. spawn the TCP or SCTP server connection handler in the background
      3. establish connections to other nodes based on configuration file parameters
      4. handle incoming messages from peers in a state-machine style
     */

    Config config = Config.fromFile("config.txt");

    new Thread(() -> {
      try {
        // open a listening socket for the server
        final ServerSocket welcomeSocket = new ServerSocket(9999);
        while (true) {
          // accept incoming socket connection requests
          final Socket connectionSocket = welcomeSocket.accept();
          // spawn a new handler for the accepted
          new Thread(() -> { }).start();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();

    for (NodeConfig node : config.nodeConfigs) {
      new Thread(() -> {
        try {
          final Socket clientSocket = new Socket(node.getHostName(), node.getListenPort());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }).start();
    }
  }

  public State getState() {
    return this.state;
  }

  public void setState(final State state) {
    this.state = state;
  }

  private enum State { ACTIVE, PASSIVE }
}
