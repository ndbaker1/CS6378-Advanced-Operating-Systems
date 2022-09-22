import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class Node {
  private final int id;
  private final String hostName;
  private final int listenPort;
  private final List<Integer> neighbors;

  private Socket[] connections = null;
  private int queuedMessages = 0;
  private int messageLimit = 0;
  
  public List<Integer> getNeighbors() {
    return neighbors;
  }

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
    // not every index here will be filled, this could be replaced with a hashmap or etc.
    connections = new Socket[config.nodes];
    messageLimit = config.maxNumber;

    // spawn a thread to accept connections to this node
    new Thread(() -> {
      try {
        // open a listening socket for the server
        final ServerSocket welcomeSocket = new ServerSocket(listenPort);
        while (true) {
          // accept incoming socket connection requests
          final Socket connectionSocket = welcomeSocket.accept();
          // spawn a new handler for the accepted
          new Thread(() -> {
            try {
              final BufferedReader reader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
              // when a message is recieved, buffer it and mark the node as active 
              String line;
              while ((line = reader.readLine()) != null) {
                log("got a message");
                if (getState() == PASSIVE) {
                  log("becoming active");
                  generateMessages(config.maxPerActive, config.minPerActive);
                  activate(config.minSendDelay);
                }
              }
            } catch (Exception e) {
              err("failed to spawn handler for connection");
              e.printStackTrace();
            }
          }).start();
        }
      } catch (Exception e) {
        err("failed to start listening on " + listenPort);
        e.printStackTrace();
      }
    }).start();

    // sleep after creation Server Socket to allow other clients to setup
    Thread.sleep(3000); // 3 seconds

    // create a socket connection to each node in the neighbor set
    for (int neighborIndex : neighbors) {
      try {
        connections[neighborIndex] = new Socket(
          config.nodeConfigs[neighborIndex].hostName,
          config.nodeConfigs[neighborIndex].listenPort
        );
        log("connected to " + neighborIndex);
      } catch (Exception e) {
        err("failed to connect to node " + neighborIndex +
          " at " + config.nodeConfigs[neighborIndex].hostName +
          " on port " + config.nodeConfigs[neighborIndex].listenPort);
        e.printStackTrace();
      }
    }
    
    Thread.sleep(3000); // 3 seconds
    
    if (id == 0) {
      generateMessages(config.maxPerActive, config.minPerActive);
      activate(config.minSendDelay);
    }
  }

  private synchronized void activate(final int minSendDelay) {
    while (getState() == ACTIVE && messageLimit > 0) {
      final int node = randomNeighbor();
      try {
        log("writing a message to node " + node);
        final OutputStream ostream = connections[node].getOutputStream();
        ostream.write("test\n".getBytes());
        ostream.flush();

        // decriment the message counters
        queuedMessages -= 1;
        messageLimit -= 1;
        // delay the next trasmission
        Thread.sleep(minSendDelay);
      } catch (Exception e) {
        err("failed to send message to node " + node);
        e.printStackTrace();
      }
    }
    log("became passive");
  }

  private int randomNeighbor() {
    return neighbors.get((int) (Math.random() * (neighbors.size() - 1)));
  }

  private void generateMessages(final int max, final int min) {
    queuedMessages += (int) (Math.random() * (max - min)) + min;
  }

  private void err(final String message) {
    System.err.println("[" + id + "] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + id + "] " + message);
  }

  static final int ACTIVE = 1;
  static final int PASSIVE = 0;
  
  /**
   * Returns 0 or 1 indicating if the state of the node is passive or active
   */
  private synchronized int getState() {
    return queuedMessages > 0 ? ACTIVE : PASSIVE;
  }

  private class Message {
    public Message() { }
  }
}
