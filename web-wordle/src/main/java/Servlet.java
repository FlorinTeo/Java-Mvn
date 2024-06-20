import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api")
public class Servlet extends HttpServlet{
    private static final long serialVersionUID = 3L;
    Context _context;

     /**
     * On initialization retrieve and retain _serverContext 
     */
    public void init() throws ServletException {
        _context = (Context) getServletContext().getAttribute("context");
    }

    private static void checkTrue(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("##Err##: " + message);
        }
    }

    private static List<String> passThroughPwdHash = Arrays.asList(
        "4f5dcabf99ab7c6f545e0dfa4c0477db",
        "d52b71110c77496d304995ec1a8a57b6",
        "ad177c7653b82ecee0c648569214b909",
        "59a8add95efb4a6974aad6f7580a13f9"
    );

    private static void checkPwd(String pwd) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5Digest = md.digest(pwd.getBytes(StandardCharsets.UTF_8));
        String pwdHash = new BigInteger(1, md5Digest).toString(16);
        checkTrue(passThroughPwdHash.contains(pwdHash), "Invalid password for restricted access!");
    }

    /**
     * Parse a "http://.../web-wordle/api?" request
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String[]> params = request.getParameterMap();
        String sid = "?";
        Session session = null;
        Answer answer = new Answer();

        try {
            checkTrue(params.containsKey("cmd"),"Missing 'cmd' parameter!");
            String cmd = params.get("cmd")[0];
            switch(cmd.toLowerCase()) {
                case "new": // http://localhost:8080/web-wordle/api?cmd=new[&name=<name>]
                    checkTrue(_context.isReady(), "Server not ready!");
                    String name = params.containsKey("name") ? params.get("name")[0] : "~anonymous~";
                    session = _context.newSession(name);
                    checkTrue(session != null, "Too many sessions!");
                    sid = session.getId();
                    answer = answer.new Msg(sid, "Session created!");
                    break;
                case "close": // http://localhost:8080/web-wordle/api?sid=8C537D99&cmd=close
                    checkTrue(params.containsKey("sid"), "Missing 'sid' parameter!");
                    sid = params.get("sid")[0];
                    boolean closed = _context.deleteSession(sid);
                    checkTrue(closed, "Invalid session!");
                    answer = answer.new Msg(sid, "Session closed!");
                    break;
                case "check": // http://localhost:8080/web-wordle/api?sid=8C537D99&cmd=check&word=ABCDE --> HTTP203 if not solved! 
                    checkTrue(params.containsKey("sid"), "Missing 'sid' parameter!");
                    sid = params.get("sid")[0];
                    session = _context.getSession(sid);
                    session.touch();
                    checkTrue(session != null, "Invalid session!");
                    checkTrue(params.containsKey("word"), "Missing 'word' parameter!");
                    String word = params.get("word")[0].toUpperCase();
                    checkTrue(_context.isWordValid(word), "Invalid word!");
                    boolean success = session.checkWord(word);
                    answer = answer.new Msg(sid, session.getLastHint(), word);
                    if (!success) {
                        response.setStatus(203);
                    }
                    break;
                case "reveal": // http://localhost:8080/web-wordle/api?sid=8C537D99&cmd=reveal
                    checkTrue(params.containsKey("sid"), "Missing 'sid' parameter!");
                    sid = params.get("sid")[0];
                    session = _context.getSession(sid);
                    session.touch();
                    checkTrue(session != null, "Invalid session!");
                    answer = answer.new Msg(sid, "Secret revealed!", session.getSecret().toString());
                    break;
                case "reset": // http://localhost:8080/web-wordle/api?sid=8C537D99&cmd=reset&pwd=<password>
                    checkTrue(params.containsKey("pwd"), "Missing 'pwd' parameter!");
                    checkPwd(params.get("pwd")[0]);
                    _context.reset();
                case "stats": // http://localhost:8080/web-wordle/api?cmd=stats[&sid=<session>]
                    if (params.containsKey("sid")) {
                        sid = params.get("sid")[0];
                        session = _context.getSession(sid);
                        session.touch();
                        checkTrue(session != null, "Invalid session!");
                        answer = answer.new Stats(session);
                    } else {
                        answer = answer.new Stats(_context);
                    }
                    break;
                default:
                    answer = answer.new Err("Unsupported 'cmd' parameter!");
            }
        } catch(RuntimeException | NoSuchAlgorithmException e) {
            answer = answer.new Err(e.getMessage());
        }

        String jsonAnswer = answer.toString();
        if (answer instanceof Answer.Err) {
            response.setStatus(400);
        }
        response.setContentType("application/json");
        response.getOutputStream().print(jsonAnswer);
    }
}
