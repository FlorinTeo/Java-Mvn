import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Session implements Comparable<Session> {
    // A Session which have not been touched the _LIFECHECK duration
    // is considered orphaned and subjected to removal.
    private static final Duration _LIFECHECK = Duration.ofHours(12);

    private String _sessionId;
    private String _name;
    private Word _secret;
    private ArrayList<String[]> _guesses;
    private int _solved;
    private boolean _spoiled;
    private Instant _heartbeat;

    public Session(String name, Word secret) {
        UUID uuid = UUID.randomUUID();
        _sessionId = uuid.toString().substring(0, 8).toUpperCase();
        _name = name;
        _secret = secret;
        _guesses = new ArrayList<String[]>();
        _solved = 0;
        _spoiled = false;
        _heartbeat = Instant.now();
    }

    public String getId() {
        return _sessionId;
    }

    public String getName() {
        return _name;
    }

    public Word getSecret() {
        // mark the session as spoiled only if it was not solved already
        _spoiled = (_solved == 0);
        return _secret;
    }

    public List<String> getGuesses() {
        List<String> guesses = new ArrayList<String>();
        for(String[] guess : _guesses) {
            guesses.add(String.format("%s > %s", guess[0], guess[1]));
        }
        return guesses;
    }

    public boolean checkWord(String word) {
        String hints = _secret.getHints(word);
        String[] guess = new String[] {word, hints};
        _guesses.add(guess);
        if (_solved == 0 && hints.equals(word)) {
            _solved = _guesses.size();
        }
        return hints.equals(word);
    }

    public String getLastHint() {
        return _guesses.size() > 0 ? _guesses.get(_guesses.size()-1)[1] : null;
    }

    public String getState() {
        if (_guesses.size() == 0) {
            return "NOT Started!";
        } else if (_solved > 0) {
            if (_guesses.size() > _solved) {
                return String.format("Already %sSOLVED in %d guesses. Still guessing?", _spoiled ? "SPOILED/": "", _solved);
            } else {
                return String.format("%sSOLVED in %d guesses!", _spoiled ? "SPOILED/": "", _solved);
            }
        } else if(_guesses.size() < 4) {
            return "GUESSING";
        } else if (_guesses.size() < 6) {
            return "GUESSING Hard!";
        } else if (_guesses.size() < 10) {
            return "GUESSING Too Hard!";
        } else {
            return "GUESSING Randomly?";
        }
    }

    public void touch() {
        _heartbeat = Instant.now();
    }

    public boolean isOrphan(Instant now) {
        Duration lifetime = Duration.between(_heartbeat, now);
        return (lifetime.compareTo(_LIFECHECK) >= 0);
    }

    public boolean isSpoiled() {
        return _spoiled;
    }

    @Override
    public int compareTo(Session o) {
        return _name.compareTo(o._name);
    }

    @Override
    public String toString() {
        return String.format("(%s %c %s) %s", _sessionId, _spoiled ? '!' : ':', _name, getState());
    }
}
