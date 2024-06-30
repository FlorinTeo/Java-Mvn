import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class Server implements ServletContextListener {
     /**
     * Code executed when the server is starting, before all servlets are activated.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // create a custom-made context instance and attach it to the
        // platform's servlet context, under the "context" attribute name.
        wordle.Context ctxWordle = new wordle.Context(event.getServletContext());
        event.getServletContext().setAttribute("context-wordle", ctxWordle);
        xroads.Context ctxXRoads = new xroads.Context(event.getServletContext());
        event.getServletContext().setAttribute("context-xroads", ctxXRoads);
    }
    
    /**
     * Code executed when the server is shutting down, after all servlets are deactivated.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        wordle.Context ctxWordle = (wordle.Context)event.getServletContext().getAttribute("context-wordle");
        ctxWordle.closing();
        xroads.Context ctxXRoads = (xroads.Context)event.getServletContext().getAttribute("context-xroads");
        ctxXRoads.closing();
    }
}
