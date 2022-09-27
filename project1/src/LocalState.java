import java.io.Serializable;

public class LocalState implements Serializable {
    private int[] applicationClock;
    private State state;
    private int id;

    public LocalState(final int id, final int[] clock, final State state) {
        this.id = id;
        this.state = state;
        this.applicationClock = clock; // do a copy later
    }
}