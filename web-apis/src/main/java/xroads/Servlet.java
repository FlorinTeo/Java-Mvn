package xroads;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/xroads")
public class Servlet extends HttpServlet{
    private static final long serialVersionUID = 3L;

    private Context _context;

     /**
     * On initialization retrieve and retain _serverContext 
     */
    public void init() throws ServletException {
        _context = (Context) getServletContext().getAttribute("context-xroads");
    }

    private static void checkTrue(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("##Err##: " + message);
        }
    }

    /**
     * Parse a "http://.../web-apis/xroads?" request
     */
    @SuppressWarnings("null")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String[]> params = request.getParameterMap();
        _context.runInitialize();
        response.setContentType("text/plain");
        response.getOutputStream().print("Not implemented yet!");
    }
}
