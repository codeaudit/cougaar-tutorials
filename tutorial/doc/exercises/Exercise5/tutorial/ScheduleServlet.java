package tutorial;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import org.cougaar.core.servlet.*;

import tutorial.assets.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.*;
import org.cougaar.glm.ldm.asset.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.TaskImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.servlet.SimpleServletComponent;



  /**
   * todo:  Create Predicate matching all ProgrammerAssets
   */


 // todo:  add code to make this a servlet subclass
public class ScheduleServlet
{
	private Properties properties = null;
	private PrintWriter out;
	private SimpleServletSupport support;

	public ScheduleServlet(SimpleServletSupport mySupport)
	{
		super();
		support = mySupport;
		properties = new Properties();
	}

	public void doGet(
	        HttpServletRequest request,
	        HttpServletResponse response) throws IOException, ServletException
	{
		execute(request, response);
	}

	public void doPost(
	        HttpServletRequest request,
	        HttpServletResponse response) throws IOException, ServletException
	{
		execute(request, response);
	}

	private void execute(
	        HttpServletRequest request,
	        HttpServletResponse response) throws IOException, ServletException
	{

		// todo:  get the PrintWriter which sends data to HTTP
	    try {
		System.out.println("ScheduleServlet called from agent: " + support.getEncodedAgentName());

		// todo: query the Blackboard for a Collection of ProgrammerAssets
           }
		catch (Exception ex)
		{
			out.println(ex.getMessage());
			ex.printStackTrace(out);
			System.out.println(ex);
			out.flush();
		}

	}

  /**
   * Print an HTML table of this programmer's schedule to the PrintStream
   */
  private void dumpProgrammerSchedule(ProgrammerAsset pa, PrintWriter out) {

      // todo: print programmer's name and a line break
      out.println("<table border=1>");
      Schedule s = pa.getSchedule();

      TreeSet ts = new TreeSet(s.keySet());
      Iterator iter = ts.iterator();

      out.println("<tr><td>Task<td>Verb<td>Month</tr>");
      int i = 0;
      while (iter.hasNext()) {
        Object key = iter.next();
        Object o = s.get(key);

        out.print("<tr><td>"+i+++"<td>");
        if (o instanceof Task) {
          Task task = (Task)o;
          // todo:  print the verb and the item to be coded
        } else {
          out.print(o);
        }
        out.println("<td>"+key+"</tr>");
      }
      out.println("</table>");
      out.flush();
  }

}


