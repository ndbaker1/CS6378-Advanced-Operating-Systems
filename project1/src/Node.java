import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import java.util.ArrayList;


public class Node {
  private final int id;
  private final String hostName;
  private final int listenPort;
  private final List<Integer> neighbors;

  public Node(
    final int id,
    final String hostName,
    final int listenPort
  ) {
    this.id = id;
    this.hostName = hostName;
    this.listenPort = listenPort;
    this.neighbors = new ArrayList<Integer>();
  }

  public int getId() {
    return id;
  }

  public String getHostName() {
    return hostName;
  }

  public int getListenPort() {
    return listenPort;
  }

  public List<Integer> getNeighbors() {
    return neighbors;
  }

  public static void main(String[] args) {
    /**
      Pseudocode

      1. read the config file to get the parameters
      2. spawn the TCP or SCTP server connection handler in the background
      3. establish connections to other nodes based on configuration file parameters
      4. handle incoming messages from peers in a state-machine style
     */

    // find out which node this instance is labelled
    final int id = Integer.parseInt(args[2]);
    // read the configuration
    Config config = Config.fromFile("config.txt");
    // pull the current node's config based on id label
    final Node node = config.nodeConfigs[id];

    // spawn a thread to accept connections to this node
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

    // create a socket connection to each node in the neighbor set
    for (int neighborIndex : node.neighbors) {
      new Thread(() -> {
        try {
          final Socket clientSocket = new Socket(node.getHostName(), node.getListenPort());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }).start();
    }
  }

  private enum State { ACTIVE, PASSIVE }
}
