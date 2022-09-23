import java.io.Serializable;


public class Message implements Serializable {
  public Message() { }

  static class Finish extends Message { }
  static class Application extends Message { }
  static class Control extends Message { }
}
