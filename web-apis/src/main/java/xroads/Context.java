package xroads;

import java.util.Timer;
import java.util.TimerTask;

import jakarta.servlet.ServletContext;

public class Context extends TimerTask {

    public enum State {
        INITIALIZING,
        LOADING,
        READY
    }

    private ServletContext _servletContext;
    private State _state;
    private Timer _timer;

    public Context(ServletContext servletContext) {
        _servletContext = servletContext;
        _state = State.INITIALIZING;
        _timer = new Timer();
        // in 10ms load the database, then every minute cleanup orphaned sessions!
        _timer.schedule(this, 12, 60000);
    }

    public boolean isReady() {
        synchronized(_state) {
            return _state == State.READY;
        }
    }

    public State getState() {
        return _state;
    }

    @Override
    public void run() {
        switch(_state) {
        case INITIALIZING:
            runInitialize();
            break;
        case READY:
            runCleanup();
            break;
        default:
            // no action on any of the other states!
            break;
        }
    }

    public void runInitialize() {
        synchronized(_state) {
            _state = State.LOADING;
            System.out.printf("~~~~ XRoads Context state: %s ~~~~\n", _state.name());
        }
        String resDirPath = _servletContext.getRealPath("WEB-INF\\classes\\xroads\\res\\");
        System.out.println(resDirPath);

        synchronized(_state) {
            _state = State.READY;
            System.out.printf("~~~~ XRoads Context state: %s ~~~~\n", _state.name());
        }
    }

    public void runCleanup() {

    }

    public void closing() {
    }
}
