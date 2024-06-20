import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import jakarta.servlet.ServletContext;

public class Context extends TimerTask {

    public enum State {
        INITIALIZING,
        LOADING,
        SORTING,
        CLEANING,
        READY
    }

    private ServletContext _servletContext;
    private Map<String, Session> _sessions;
    private ArrayList<Word> _words;
    private State _state;
    private Timer _timer;
    private int _wordLen;

    public Context(ServletContext servletContext) {
        _servletContext = servletContext;
        _sessions = new HashMap<String, Session>();
        _words = new ArrayList<Word>();
        _wordLen = Integer.MAX_VALUE;
        _state = State.INITIALIZING;
        _timer = new Timer();
        // in 10ms load the database, then every minute cleanup orphaned sessions!
        _timer.schedule(this, 10, 60000);
    }

    public boolean isReady() {
        synchronized(_state) {
            return _state == State.READY;
        }
    }

    public State getState() {
        return _state;
    }

    public Session getSession(String sid) {
        return _sessions.get(sid);
    }

    public boolean isWordValid(String word) {
        return word.length() == _wordLen;
    }

    public List<String> getSessions() {
        List<Session> sortedSessions = new ArrayList<Session>(_sessions.values());
        Collections.sort(sortedSessions);
        List<String> sortedList = new ArrayList<String>();
        for(Session s : sortedSessions) {
            sortedList.add(s.toString());
        }
        return sortedList;
    }

    public Session newSession(String name) {
        int retry = 5;
        int iWord = (int)(_words.size() * Math.random());
        Word secretWord = _words.get(iWord);
        Session session = new Session(name, secretWord);
        while (--retry > 0 && _sessions.containsKey(session.getId())) {
            session = new Session(name, secretWord);
        }
        if (retry == 0) {
            return null;
        }
        _sessions.put(session.getId(), session);
        return session;
    }

    public int sessionCount() {
        return _sessions.size();
    }

    public boolean deleteSession(String sid) {
        return _sessions.remove(sid) != null;
    }

    public void reset() {
        _sessions = new HashMap<String, Session>();
    }

    public void closing() {
        _timer.cancel();
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
            System.out.printf("~~~~ Context state: %s ~~~~\n", _state.name());
        }
        String wordleDbPath = _servletContext.getRealPath("WEB-INF\\classes\\wordle.csv");
        System.out.println(wordleDbPath);
        Scanner wordleDbReader = null;
        int count = 0;
        try {
            wordleDbReader = new Scanner(new File(wordleDbPath));
            while(wordleDbReader.hasNextLine()) {
                String line = wordleDbReader.nextLine();
                count++;
                // skip the header
                if (count == 1) {
                    continue;
                }
                Word word = new Word(line);
                _wordLen = Math.min(_wordLen, word.getWord().length());
                _words.add(word);
            }
        } catch (Exception e) {
            System.out.printf("[csvLine %d] %s\n", count, e.getMessage());
        } finally {
            if (wordleDbReader != null) {
                wordleDbReader.close();
            }
        }
        System.out.printf("Wordle Database loaded ... [%d] words\n", count);
        synchronized(_state) {
            _state = State.SORTING;
            System.out.printf("~~~~ Context state: %s ~~~~\n", _state.name());
        }
        Collections.sort(_words);
        synchronized(_state) {
            _state = State.READY;
            System.out.printf("~~~~ Context state: %s ~~~~\n", _state.name());
        }
    }

    public void runCleanup() {
        synchronized(_state) {
            _state = State.CLEANING;
            System.out.printf("~~~~ Context state: %s ~~~~\n", _state.name());
        }

        Queue<Session> orphanSessions = new LinkedList<Session>();
        Instant now = Instant.now();
        for(Session session : _sessions.values()) {
            if (session.isOrphan(now)) {
                orphanSessions.add(session);
            }
        }
        int count = 0;
        while(!orphanSessions.isEmpty()) {
            Session orphan = orphanSessions.remove();
            _sessions.remove(orphan.getId());
            count++;
        }
        System.out.printf("Sessions cleaned up ... [removed %d][remaining %d] words\n", count, _sessions.size());
        synchronized(_state) {
            _state = State.READY;
            System.out.printf("~~~~ Context state: %s ~~~~\n", _state.name());
        }
    }
}
