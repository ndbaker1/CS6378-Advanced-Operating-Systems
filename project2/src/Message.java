import java.io.Serializable;
import java.util.List;


public abstract class Message implements Serializable {
  private int source;

  private Message(final int source) {
    this.source = source;
  }

  public int getSource() {
    return source;
  }

  static class Request extends Message {
    public Request(final int source) {
      super(source);
    }
  }

  static class Release extends Message {
    public Release(final int source) {
      super(source);
    }
  }

 static class Reply extends Message {
    public Reply(final int source) {
      super(source);
    }
  }
}
