import java.net.*;
import java.util.*;

import java.io.*;

public class MutualExclusionService {
  final private Map<Integer, Socket> sockets = new HashMap<>();
  final private Map<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
  final private Map<Integer, Integer> timestamps = new HashMap<>(); // Latest timestamp received from each process

  final PriorityQueue<Message.Request> requestQueue = new PriorityQueue<Message.Request>();

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
    }
    
    log("sleeping for [3] seconds to allow peer client sockets to setup...");
    Thread.sleep(3000);
  }

  // Scalar lamport clock
  private Integer lamportClock = 0;

  /**
   * Critial Section Enter
   *
   * Send request messages to all nodes and wait for replies before allowing
   * entrance into the critical section
   */
  public void csEnter() {
    Message requestMessage;
    synchronized(lamportClock) {
      requestMessage = new Message.Request(nodeId, lamportClock);
    }

    // Send out request messages to all outgoing channels
    for (int streamIndex : outputStreams.keySet()) {
      sendMessage(streamIndex, requestMessage);
    }

    // wait for reply from all or for timestamp to be lower than all other received times
    // wait for request to be at front of queue
  }

  /**
   * Critical Section Leave
   *
   * Send release messages notifying nodes in the system that the current
   * process has completed executing critical section
   */
  public void csLeave() {
    Message releaseMessage;
    synchronized(lamportClock) {
      releaseMessage = new Message.Release(nodeId, lamportClock);
    }

    // Send out release messages to all outgoing channels
    for (int streamIndex : outputStreams.keySet()) {
      sendMessage(streamIndex, releaseMessage);
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
    requestQueue.removeIf((r) -> r.getSource() == releaseMessage.getSource());
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
}
