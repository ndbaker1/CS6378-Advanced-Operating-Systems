import java.net.*;
import java.util.*;
import java.io.*;


public class MutualExclusionService {
  final private Map<Integer, Socket> sockets = new HashMap<>();
  final private Map<Integer, ObjectOutputStream> outputStreams = new HashMap<>();

  final PriorityQueue requestQueue = new PriorityQueue();

  final int nodeId;
  final Config config;

  public MutualExclusionService(
    final int nodeId,
    final Config config
  ) throws Exception {
    this.nodeId = nodeId;
    this.config = config;

    // spawn a thread to accept connections to this node
    new Thread(() -> setupListener()).start(); 

    log("sleeping for [3] seconds to allow peer server sockets to setup...");
    Thread.sleep(3000);

    // create a socket connection to each node in the neighbor set
    for (int node = 0; node < config.nodes; node++) {
      final Socket socket = new Socket(
        config.nodeConfigs[node].hostName,
        config.nodeConfigs[node].listenPort
      );

      log("connected to " + node);

      sockets.put(node, socket);
      outputStreams.put(node, new ObjectOutputStream(socket.getOutputStream()));
    }
    
    log("sleeping for [3] seconds to allow peer client sockets to setup...");
    Thread.sleep(3000);
  }

  // Scalar lamport clock
  private int lamportClock = 0;

  /**
   * Critial Section Enter
   *
   * Send request messages to all nodes and wait for replies before allowing
   * entrance into the critical section
   */
  public void csEnter() {

  }

  /**
   * Critical Section Leave
   *
   * Send release messages notifying nodes in the system that the current
   * process has completed executing critical section
   */
  public void csLeave() {

  }

  private void setupListener() {
    try {
      // open a listening socket for the server
      final ServerSocket welcomeSocket = new ServerSocket(config.nodeConfigs[nodeId].listenPort);
      while (true) {
        // accept incoming socket connection requests
        final Socket connectionSocket = welcomeSocket.accept();
        final ObjectInputStream inputStream = new ObjectInputStream(connectionSocket.getInputStream());
        // spawn a new handler for the accepted
        new Thread(() -> {
          while (true) {
            try {
              final Message message = (Message) inputStream.readObject();
              if (message instanceof Message.Request) {
                log("received a request message.");
                handleRequest((Message.Request) message);
              } else if (message instanceof Message.Reply) {
                log("received a reply message.");
                handleReply((Message.Reply) message);
              } else if (message instanceof Message.Release) {
                log("received a release message.");
                handleRelease((Message.Release) message);
              }
            } catch (EOFException e) {
              break; // this is fine to ignore
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleRequest(final Message.Request requestMessage) {
    //
  }

  private void handleReply(final Message.Reply replyMessage) {
    //
  }

  private void handleRelease(final Message.Release releaseMessage) {
    //
  }

  private void err(final String message) {
    System.err.println("[" + nodeId + "-meService] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + nodeId + "-meService] " + message);
  }
}
