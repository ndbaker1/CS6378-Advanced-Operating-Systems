import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;


public class Node {
  private final int id;
  private final String hostName;
  private final int listenPort;
  private final List<Integer> neighbors;

  private HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
  private HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<Integer, ObjectOutputStream>();
  private Socket[] connections = null;
  private int queuedMessages = 0;
  private int messageLimit = 0;
  private State state = State.PASSIVE;
  
  private static Config config = null;

  public List<Integer> getNeighbors() {
    return neighbors;
  }

  private State getState() {
    return state;
  }

  private void setState(final State state) {
    if (state == State.ACTIVE) {
      log("becoming active");
    } else {
      log("becoming passive");
    } 
    this.state = state;
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
    Node.config = config;
    // pull the current node's config based on id label
    final Node node = config.nodeConfigs[id];
    // run the node with knowledge from the configuration file
    node.run();
  }

  private void run() throws Exception {
    messageLimit = config.maxNumber;

    // spawn a thread to accept connections to this node
    new Thread(() -> {
      try {
        // open a listening socket for the server
        final ServerSocket welcomeSocket = new ServerSocket(listenPort);
        while (true) {
          // accept incoming socket connection requests
          final Socket connectionSocket = welcomeSocket.accept();
          final ObjectInputStream inputStream = new ObjectInputStream(connectionSocket.getInputStream());
          // spawn a new handler for the accepted
          new Thread(() -> {
            while (true) {
              try {
                // when a message is recieved, buffer it and mark the node as active 
                final Message message = (Message) inputStream.readObject();
                log("got a message");

                if (message instanceof Message.Application) {
                  try_activate();
                } else if (message instanceof Message.Control) {

                }
              } catch (EOFException e) {
                break;
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }).start();
        }
      } catch (Exception e) {
        err("failed to start listening on " + listenPort);
        e.printStackTrace();
      }
    }).start();

    log("sleeping for [3] seconds to allow peer server sockets to setup...");
    Thread.sleep(3000); // 3 seconds

    // create a socket connection to each node in the neighbor set
    for (int neighborIndex : neighbors) {
      try {
        final Socket socket = new Socket(
          config.nodeConfigs[neighborIndex].hostName,
          config.nodeConfigs[neighborIndex].listenPort
        );

        log("connected to " + neighborIndex);

        sockets.put(neighborIndex, socket);
        outputStreams.put(neighborIndex, new ObjectOutputStream(socket.getOutputStream()));
      } catch (Exception e) {
        err("failed to connect to node " + neighborIndex +
          " at " + config.nodeConfigs[neighborIndex].hostName +
          " on port " + config.nodeConfigs[neighborIndex].listenPort);
        e.printStackTrace();
      }
    }
    
    log("sleeping for [3] seconds to allow peer client sockets to setup...");
    Thread.sleep(3000); // 3 seconds
    
    // make node 0 the start node for at least one node at the start
    if (id == 0) {
      runSnapshotTimer();
      try_activate();
    }
  }

  private void try_activate() {

    synchronized(this) {
      // change the state of the node from PASSIVE to ACTIVE if there are still
      // messages that it can send
      if (getState() == State.PASSIVE && messageLimit > 0) setState(State.ACTIVE);
      else return;
    }

    // generate the normal number of messages for the activation of the node.
    queuedMessages += generateMessages(config.maxPerActive, config.minPerActive);

    // based on the gateway in the synchronized block above, there should not be
    // any other thread executing this loop, so there is no synchronization
    // required.
    while (queuedMessages > 0 && messageLimit > 0) {
      final int node = randomNeighbor();
      try {
        log("writing an application message to node " + node);
        final ObjectOutputStream ostream = outputStreams.get(node);
        ostream.writeObject(new Message.Application());
        ostream.flush();

        // decriment the message counters
        queuedMessages -= 1;
        messageLimit -= 1;
        // delay the next trasmission
        Thread.sleep(config.minSendDelay);
      } catch (Exception e) {
        err("failed to send message to node " + node);
        e.printStackTrace();
      }
    }

    if (messageLimit == 0) {
      log("sent max number of messages.");
    }

    // return the node to the PASSIVE state
    setState(State.PASSIVE);
  }

  private void runSnapshotTimer() {
    new Thread(() -> {
      try {
        Thread.sleep(config.snapshotDelay);
        takeSnapshot();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }

  private void takeSnapshot() {

  }

  private int randomNeighbor() {
    return neighbors.get(new Random().nextInt(neighbors.size()));
  }

  private int generateMessages(final int max, final int min) {
    return new Random().nextInt(max - min + 1) + min;
  }

  private void err(final String message) {
    System.err.println("[" + id + "] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + id + "] " + message);
  }
}
