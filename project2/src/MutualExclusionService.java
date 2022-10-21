import java.util.concurrent.Semaphore;
import java.util.*;
import java.net.*;
import java.io.*;

public class MutualExclusionService {
  private final Map<Integer, Socket> sockets = new HashMap<>();
  private final Map<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
  private final Map<Integer, Integer> timestamps = new HashMap<>(); // Latest timestamp received from each process
  private final Map<Integer, Boolean> finished_map = new HashMap<>(); // Track if each process has finished 

  // wait for reply from all or for timestamp to be lower than all other received times
  // wait for request to be at front of queue
  private final Semaphore csLock = new Semaphore(1, true);

  private final PriorityQueue<Message.Request> requestQueue = new PriorityQueue<Message.Request>();

  private final int nodeId;
  private final Config config;
  private final FileWriter output_writer;

  // Scalar lamport clock
  private Integer lamportClock = 0;

  public MutualExclusionService(
    final int nodeId,
    final Config config
  ) throws Exception {
    this.nodeId = nodeId;
    this.config = config;

    // Create logs directory in the project directory and write CS activity
    new File(config.project_path, "logs").mkdir();
    output_writer = new FileWriter(config.project_path + "/logs/output-" + nodeId + ".out");
    
    // spawn a thread to accept connections to this node
    new Thread(() -> setupListener()).start(); 

    log("sleeping for [3] seconds to allow peer server sockets to setup...");
    Thread.sleep(3000);

    finished_map.put(nodeId, false); // Need entry to track if self has finished
    
    // create a socket connection to each node
    for (int node = 0; node < config.nodes; node++) {
      if(node == nodeId) // Don't connect to self
        continue;
        
      final Socket socket = new Socket(
        config.nodeConfigs[node].hostName,
        config.nodeConfigs[node].listenPort
      );

      log("connected to " + node);

      sockets.put(node, socket);
      outputStreams.put(node, new ObjectOutputStream(socket.getOutputStream()));

      timestamps.put(node, 0);
      finished_map.put(node, false);
    }
    
    log("sleeping for [3] seconds to allow peer client sockets to setup...");
    Thread.sleep(3000);
  }

  /**
   * Critial Section Enter
   *
   * Send request messages to all nodes and wait for replies before allowing
   * entrance into the critical section
   */
  public void csEnter() {
    Message.Request requestMessage;
    synchronized(lamportClock) {
      lamportClock += 1;
      requestMessage = new Message.Request(nodeId, lamportClock);
    }
    synchronized(requestQueue) {
      requestQueue.add(requestMessage);
    }

    // Send out request messages to all outgoing channels
    for (int streamIndex : outputStreams.keySet()) {
      sendMessage(streamIndex, requestMessage);
    }

    // block until permission given
    try {
      csLock.acquire();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      output_writer.append(nodeId+" enter at " + lamportClock + "\n");
    } catch (Exception e) {
      e.printStackTrace();
		}
  }

  /**
   * Critical Section Leave
   *
   * Send release messages notifying nodes in the system that the current
   * process has completed executing critical section
   */
  public void csLeave(boolean finished) {
    try {
      output_writer.append(nodeId + " leave at " + lamportClock + "\n");
    } catch (Exception e) {
      e.printStackTrace();
		}

    Message releaseMessage;
    synchronized(lamportClock) {
      lamportClock += 1;
      releaseMessage = new Message.Release(nodeId, lamportClock, finished);
    }

    // Remove own request from the queue
    synchronized(requestQueue) {
      requestQueue.poll();
    }
    
    // Send out release messages to all outgoing channels
    for (int streamIndex : outputStreams.keySet()) {
      sendMessage(streamIndex, releaseMessage);
    }

    if (finished) {
      check_finished(nodeId);
    }
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
    synchronized(requestQueue) {
      requestQueue.add(requestMessage);
    }

    // Update scalar clock and send reply
    synchronized(lamportClock) {
      lamportClock = Integer.max(lamportClock, requestMessage.getTime()) + 1; 
      sendMessage(requestMessage.getSource(), new Message.Reply(nodeId, lamportClock));
    }
    
    // log("start queue");
    // for (Message.Request request : requestQueue) {
    //   log(request.getSource()+" "+request.getTime());
    // }
    // log("front of queue "+requestQueue.peek().getSource());
  }

  private void handleReply(final Message.Reply replyMessage) {
    int source = replyMessage.getSource();
    int time = replyMessage.getTime();

    // Update scalar clock
    synchronized(lamportClock) {
      lamportClock = Integer.max(lamportClock, time) + 1; 
    }

    // Update the timestamp of a source. Don't need to synchronize since each source is only accessed by a single thread
    timestamps.put(source, time);
  }

  private void handleRelease(final Message.Release releaseMessage) {
    // Update scalar clock
    synchronized(lamportClock) {
      lamportClock = Integer.max(lamportClock, releaseMessage.getTime()) + 1; 
    }

    // Remove source's request from the queue
    synchronized(requestQueue) {
      requestQueue.removeIf((r) -> r.getSource() == releaseMessage.getSource());

      if ( // provide access to the critical section when request is at the front and all peering timestamps are lower
        requestQueue.peek().getSource() == nodeId &&
        timestamps.entrySet().stream().allMatch(t -> t.getValue() < requestQueue.peek().getTime())
      ) {
        csLock.release();
      }
    }

    if (releaseMessage.getFinished()) {
      check_finished(releaseMessage.getSource());
    }
  }
  
  /**
   * Send a message to a target node with a provided Message.
   * 
   * Note:
   *  important synchronized resource, only one thread
   *  should be using the output stream at once
   */
  private synchronized void sendMessage(final int targetNode, final Message message) {
    try {
      final ObjectOutputStream ostream = outputStreams.get(targetNode);
      ostream.writeObject(message);
      ostream.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void err(final String message) {
    System.err.println("[" + nodeId + "-meService] " + message);
  }

  private void log(final String message) {
    System.out.println("[" + nodeId + "-meService] " + message);
  }

  // Update node_id to be finished. If all nodes are finished, terminate
  private void check_finished(int node_id) {
    finished_map.put(node_id, true);
    if(!finished_map.containsValue(false)) {
      log("terminating");
      try {
        output_writer.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.exit(0);
    }
  }
}
