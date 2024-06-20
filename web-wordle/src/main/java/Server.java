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
        Context wwCtxt = new Context(event.getServletContext());
        event.getServletContext().setAttribute("context", wwCtxt);
    }
    
    /**
     * Code executed when the server is shutting down, after all servlets are deactivated.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Context wwCtxt = (Context)event.getServletContext().getAttribute("context");
        wwCtxt.closing();
    }
}
