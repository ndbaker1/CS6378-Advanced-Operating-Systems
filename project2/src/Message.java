import java.io.Serializable;
import java.util.List;

public abstract class Message implements Serializable {
  private int source;
  private int time;

  private Message(final int source, final int time) {
    this.source = source;
    this.time = time;
  }

  public int getSource() {
    return source;
  }

  public int getTime() {
    return time;
  }

  static class Request extends Message implements Comparable<Message.Request> {
    public Request(final int source, final int time) {
      super(source, time);
    }

    // Comparator for priority queue ordering
    @Override
    public int compareTo(Message.Request anotherRequest) {
      final int thisTime = this.getTime();
      final int anotherTime = anotherRequest.getTime();
      if(thisTime < anotherTime)
        return -1;
      else if(thisTime > anotherTime)
        return 1;
        
      // If equal timestamp, order by node ID
      final int thisSource = this.getSource();
      final int anotherSource = anotherRequest.getSource();
      if(thisSource < anotherSource)
        return -1;
      else if(thisSource > anotherSource)
        return 1;
      else 
        return 0; // Shouldn't happen since only one request can exist per node
    }
  }

  static class Release extends Message {
    private boolean finished;

    public Release(final int source, final int time, final boolean finished) {
      super(source, time);
      this.finished = finished;
    }

    public boolean getFinished() {
      return finished;
    }
  }

 static class Reply extends Message {
    public Reply(final int source, final int time) {
      super(source, time);
    }
  }
}
