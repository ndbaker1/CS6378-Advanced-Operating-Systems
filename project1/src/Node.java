import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.io.EOFException;


public class Node {
  private static final int START_NODE = 0;

  // fields used only in config Parser
  private final int id;
  private final String hostName;
  private final int listenPort;
  private final List<Integer> neighbors;

  private Map<Integer, Socket> sockets = new HashMap<>();
  private Map<Integer, ObjectOutputStream> outputStreams = new HashMap<>();

  private int queuedMessages = 0;
  private int messageLimit = 0;
  private State state = State.PASSIVE;
  private Color markerMode = Color.Blue;
  private Set<Integer> markerLog = new HashSet<>();


  private List<ChannelState> channelStates = new ArrayList<>();
  private LocalState localState;
  private GlobalState globalState;

  private int[] vectorClock;
  private static Config config;

  // resolves to the parent of the node for snapshot messages
  private int forwarder = -1;

  private static String configPath = "";

  public Node(
    final int id,
    final String hostName,
    final int listenPort
  ) {
    this.id = id;
    this.hostName = hostName;
    this.listenPort = listenPort;
    this.neighbors = new ArrayList<>();
  }

  public static void main(String[] args) throws Exception {
    // find out which node this instance is labelled
    final int id = Integer.parseInt(args[1]);
    // read the configuration
    Node.configPath = args[0];
    Node.config = Config.fromFile(new File(configPath));
    // pull the current node's config based on id label
    final Node node = Node.config.nodeConfigs[id];
    // run the node with knowledge from the configuration file
    node.run();
  }

  private void run() throws Exception {
    vectorClock = new int[config.nodes];
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
                final Message message = (Message) inputStream.readObject();
                if (message instanceof Message.Application) {
                  log("recieved an application message.");
                  handleApplicationMessage((Message.Application) message);
                  tryActivate();
                } else if (message instanceof Message.Marker) {
                  log("recieved a marker message.");
                  handleMarkerMessage((Message.Marker) message);
                } else if (message instanceof Message.Snapshot) {
                  log("recieved a snapshot message.");
                  handleSnapshotMessage((Message.Snapshot) message);
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
    
    if (id == Node.START_NODE) {
      globalState = new GlobalState();
      runSnapshotTimer();
      tryActivate();
    }
  }

  private synchronized void tryActivate() {
    // change the state of the node from PASSIVE to ACTIVE if there are still
    // messages that it can send
    if (getState().equals(State.PASSIVE) && messageLimit > 0) {

      log("becoming active");
      setState(State.ACTIVE);

      new Thread(() -> {
        // generate the normal number of messages for the activation of the node.
        queuedMessages += generateMessages(config.maxPerActive, config.minPerActive);

        // based on the gateway in the synchronized block above, there should not be
        // any other thread executing this loop, so there is no synchronization
        // required.
        while (queuedMessages > 0 && messageLimit > 0) {
          final int node = randomNeighbor();
          try {
            log("writing an application message to node " + node);
            // send Application message to destination node socket
            send(node, new Message.Application(id, vectorClock));
            // decriment the message counters
            queuedMessages--;
            messageLimit--;
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

        // return the node to the PASSIVE state to allow entry into tryActivate again
        log("becoming passive");
        setState(State.PASSIVE);
      }).start();
    }
  }

  /**
   * Handle recieving and/or forwarding snapshot messages to the snapshot initiator
   */
  private void handleSnapshotMessage(final Message.Snapshot snapshotMessage) {
    // only record messages
    // different behavior based on whether the node initiated the snapshot or not
    if (id == Node.START_NODE) {
      log("recieved snapshot for " + snapshotMessage.getSource());
      // add nodes' state to global state and then print snapshot
      globalState.getLocalStates().add(snapshotMessage.getLocalState());
      globalState.getChannelStates().addAll(snapshotMessage.getChannelStates());

      if (globalState.getLocalStates().size() == config.nodes - 1) {
        // add nodes' state to global state and then print snapshot
        globalState.getLocalStates().add(localState);
        globalState.getChannelStates().addAll(channelStates);
        outputSnapshot();
      }
    } else {
      // Note:
      // Only nodes besides the START_NODE should have an forwarder,
      // since it is assigned from the marker message source id while the 
      // initiator node triggers itself
      log("forwarding snapshot message from " + snapshotMessage.getSource() + " to " + forwarder);
      send(forwarder, snapshotMessage);
    }
  }

  /**
   * Handle application messages through vector clock Fidge-Mattern protocol
   */
  private synchronized void handleApplicationMessage(final Message.Application applicationMessage) {
    for (int i = 0; i < vectorClock.length; i++) {
      vectorClock[i] = Math.max(vectorClock[i], applicationMessage.getVectorClock()[i]);
    }
    // increment clock of current process
    vectorClock[id]++;

    // only record channel updates when the node is in RED markerMode,
    // and when the marker response hasnt been seen from the node yet.
    // This is what is making use of FIFO ordering to get all of the events which
    // were in transmission since before the marker had been received.
    if (markerMode.equals(Color.Red) && !markerLog.contains(applicationMessage.getSource())) {
      channelStates.add(new ChannelState(applicationMessage.getSource(), id, applicationMessage.getVectorClock()));
    }
  }

  /**
   * Handle marker messages that are being seen or sent as decribed by the Chandy-Lamport Protocol.
   */
  private synchronized void handleMarkerMessage(final Message.Marker incomingMarker) {
    if (markerMode.equals(Color.Blue)) {
      forwarder = incomingMarker.getSource();
      // handle color change and broadcast
      changeMode();
    }

    // mark the channel as recorded
    markerLog.add(incomingMarker.getSource());
    
    // send snapshot to parent and return to blue if the markers were all replied
    if (markerLog.containsAll(neighbors)) {
      // relax the node back to Blue to allow for more snapshots
      markerMode = Color.Blue;

      // send snapshot to parent since we have recorded all of the
      // connected neighbors if the node is not the snapshot initiator
      if (id != Node.START_NODE) {
        // Note:
        // Only nodes besides the START_NODE should have an forwarder,
        // since it is assigned from the marker message source id while the 
        // initiator node triggers itself.
        log("sending snapshot to " + forwarder);
        send(forwarder, new Message.Snapshot(id, localState, channelStates));
        // once the snapshot is passed on we can reset the protocol
        resetSnapshotProtocol();
      }
    }
  }

  /**
   * Process transition from Blue to Red Chandy-Lamport state
   */
  private void changeMode() {
    // switch the markerMode for Blue to Red
    markerMode = Color.Red;
    // record the node's local state
    localState = new LocalState(id, vectorClock, state);
    // send out the new marker messages to all outgoing channels
    final Message markerMessage = new Message.Marker(id);
    for (int neighborIndex : neighbors) {
      send(neighborIndex, markerMessage);
    }
  }

  private void runSnapshotTimer() {
    new Thread(() -> {
      try {
        Thread.sleep(config.snapshotDelay);
        log("initiating snapshot!");
        changeMode();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }

  private int randomNeighbor() {
    return neighbors.get(new Random().nextInt(neighbors.size()));
  }

  private int generateMessages(final int max, final int min) {
    return new Random().nextInt(max - min + 1) + min;
  }

  public List<Integer> getNeighbors() {
    return neighbors;
  }

  private State getState() {
    return state;
  }

  private void setState(final State state) {
    this.state = state;
  }

  private void outputSnapshot() {
    log("snapshot taken!");
    if (!isSnapshotConsistent()) {
      err("snapshot inconsistent!");
    } else {
      log("consistent snapshot!");
    }

    writeSnapshots();
    resetSnapshotProtocol();
    runSnapshotTimer();
  }

  /**
   * Return where the Global State snapshot is a consistent snapshot
   */
  private boolean isSnapshotConsistent() {
    for (final LocalState e : globalState.getLocalStates()) {
      for (final LocalState f : globalState.getLocalStates()) {
        // For every ordered pair of states we want to verify that:
        //                ¬(e -> f) ∧ ¬(f -> e)
        // We can short circuit the statement if we see any
        // pair which contains a happens-before relation.
        // 
        // e -> f === (e != f) ∧ (V(e)[P_i] < V(f)[P_i])
        if (e.getID() != f.getID()) {
          if (e.getApplicationClock()[e.getID()] <= f.getApplicationClock()[e.getID()]) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private void resetSnapshotProtocol() {
    if (globalState != null) globalState.reset();
    
    localState = null;
    channelStates.clear();
    markerLog.clear();
  }

  /**
   * Send a message to a target node with a provided Message.
   * 
   * Note:
   *  important synchronized resource, only one thread
   *  should be using the output stream at once
   */
  private synchronized void send(final int targetNode, final Message message) {
    try {
      final ObjectOutputStream ostream = outputStreams.get(targetNode);
      ostream.writeObject(message);
      ostream.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void writeSnapshots() {
    try {
      final File configFile = new File(Node.configPath);
      final String configNameWithoutExt = configFile.getName().substring(0, configFile.getName().length() - ".txt".length());
      final String configDirectory = configFile.getAbsoluteFile().getParent();

      for (final LocalState snapshotState : globalState.getLocalStates()) {
        final File outputFile = new File(configDirectory + "/" + configNameWithoutExt + "-" + snapshotState.getID() + ".out");
        final PrintWriter outputWriter = new PrintWriter(new FileOutputStream(outputFile, true));

        for (int clockValue : snapshotState.getApplicationClock()) {
          outputWriter.append(clockValue + " ");
        }
        outputWriter.append("\n");

        outputWriter.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void err(final String message) {
    System.err.println("[" + id + "] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + id + "] " + message);
  }

  private enum Color { Blue, Red }
}
