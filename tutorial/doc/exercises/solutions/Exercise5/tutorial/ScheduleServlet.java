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



public class ScheduleServlet extends HttpServlet
{
	private SimpleServletSupport support;

	public void setSimpleServletSupport(SimpleServletSupport support)
	{
		this.support = support;
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

		PrintWriter out = response.getWriter();

		try
		{
  		  System.out.println("Servlet called." );

                  Collection programmers =  support.queryBlackboard(new ProgrammersPredicate());
		  Iterator iter = programmers.iterator();
		  while (iter.hasNext()) {
		    ProgrammerAsset pa = (ProgrammerAsset)iter.next();
		    dumpProgrammerSchedule(pa, out);
		  }
		}
		catch (Exception ex)
		{
			out.println("Error processing servlet:"+ex.getMessage());
			ex.printStackTrace(out);
			System.out.println(ex);
			out.flush();
		}

	}


  /**
   * Print an HTML table of this programmer's schedule to the PrintStream
   */
  private void dumpProgrammerSchedule(ProgrammerAsset pa, PrintWriter out) {
      // dump classnames and count to output stream
      out.println("<b>Programmer: "+pa.getItemIdentificationPG().getItemIdentification()+"<b><br>");
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
          out.print(task.getVerb());
          out.print(" " + task.getDirectObject().getItemIdentificationPG().getItemIdentification());
        } else {
          out.print(o);
        }
        out.println("<td>"+key+"</tr>");
      }
      out.println("</table>");
      out.flush();
  }

}

/**
 * This predicate matches all Programmer asset objects
 */
class ProgrammersPredicate implements UnaryPredicate {
  public boolean execute(Object o) {
    return o instanceof ProgrammerAsset;
  }
}


