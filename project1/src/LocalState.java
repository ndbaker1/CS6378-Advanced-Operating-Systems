import java.io.Serializable;


public class LocalState implements Serializable {
    private final int[] applicationClock;
    private final Node.State state;
    private final int id;

    public LocalState(final int id, final int[] clock, final Node.State state) {
        this.id = id;
        this.state = state;
        this.applicationClock = clock.clone(); 
    }

    public int[] getApplicationClock() {
        return applicationClock;
    }

    public Node.State getState() {
        return state;
    }

    public int getID() {
        return id;
    }
}
