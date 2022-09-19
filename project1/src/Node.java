import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;


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

  public static void main(String[] args) throws Exception {
    // find out which node this instance is labelled
    final int id = Integer.parseInt(args[1]);
    // read the configuration
    final Config config = Config.fromFile(args[0]);
    // pull the current node's config based on id label
    final Node node = config.nodeConfigs[id];
    // run the node with knowledge from the configuration file
    node.run(config);
  }

  private void run(final Config config) throws Exception {

    final Socket[] connections = new Socket[config.nodes];

    // spawn a thread to accept connections to this node
    new Thread(() -> {
      try {
        // open a listening socket for the server
        final ServerSocket welcomeSocket = new ServerSocket(getListenPort());
        while (true) {
          // accept incoming socket connection requests
          final Socket connectionSocket = welcomeSocket.accept();
          // spawn a new handler for the accepted
          new Thread(() -> {
            try {
               final BufferedReader reader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
               // when a message is recieved, buffer it and mark the node as active 
               
            } catch (Exception e) {
              err("failed to spawn handler for connection");
              e.printStackTrace();
            }
          }).start();
        }
      } catch (Exception e) {
        err("failed to start listening on " + getListenPort());
        e.printStackTrace();
      }
    }).start();

    // sleep after creation Server Socket to allow other clients to setup
    Thread.sleep(3000); // 3 seconds

    // create a socket connection to each node in the neighbor set
    for (int neighborIndex : neighbors) {
      try {
        synchronized(connections) {
          connections[neighborIndex] = new Socket(
            config.nodeConfigs[neighborIndex].getHostName(),
            config.nodeConfigs[neighborIndex].getListenPort()
          );
        }
        log("connected to " + neighborIndex);
      } catch (Exception e) {
        err("failed to connect to node " + neighborIndex +
          " at " + config.nodeConfigs[neighborIndex].getHostName() +
          " on port " + config.nodeConfigs[neighborIndex].getListenPort());
        e.printStackTrace();
      }
    }
  }

  private void err(final String message) {
    System.err.println("[" + id + "] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + id + "] " + message);
  }

  private enum State { ACTIVE, PASSIVE }

  private class Message {
    public Message() { }
  }
}
