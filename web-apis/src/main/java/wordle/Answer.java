package wordle;

import java.util.List;

import com.google.gson.Gson;

public class Answer {

    public class Stats extends Answer {
        public String _state;
        public int _count;
        public List<String> _sessions;

        public Stats(Context context) {
            _state = context.getState().name();
            _sessions = context.getSessions();
            _count = _sessions.size();
        }

        public Stats(Session session) {
            _state = String.format("(%s %c %s) %s", session.getId(), session.isSpoiled()? '!' : ':', session.getName(), session.getState());
            _sessions = session.getGuesses();
            _count = _sessions.size();
        }
    }

    public class Err extends Answer {
        public String _error;

        public Err(String error) {
            _error = error;
        }
    }

    public class Msg extends Answer {
        public String _sid;
        public String _word;
        public String _message;


        public Msg(String sid, String message) {
            this(sid, message, null);
        }

        public Msg(String sid, String message, String word) {
            _sid = sid;
            _message = message;
            _word = word;
        }
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
