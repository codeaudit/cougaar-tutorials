package org.cougaar.demo.mandelbrot.v3;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.plugin.ServletPlugin;
import org.cougaar.core.service.UIDService;
import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.demo.mandelbrot.util.ImageOutput;
import org.cougaar.util.FutureResult;

/**
 * This servlet publishes a {@link Job} and waits for a result notification.
 */
public class FractalServlet extends ServletPlugin {

  private static final long DEFAULT_TIMEOUT = 60000;

  private UIDService uidService;

  public void load() {
    // get the required unique identifier service
    uidService = (UIDService) getServiceBroker().getService(
        this, UIDService.class, null);
    if (uidService == null) {
      throw new RuntimeException("Unable to obtain the UIDService");
    }

    super.load();
  }

  public void unload() {
    super.unload();

    if (uidService != null) {
      getServiceBroker().releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
  }

  /**
   * Override "isTransactional" to false.
   * <p>
   * By default, our "publishAdd" will be buffered until the end of our "doGet"
   * call.  We want to publish it immediately, so the calculator plugin can see
   * it and notify us of the result.  Therefore, we don't want our "doGet" to
   * run in the "execute()" method's buffered blackboard transaction.
   */
  protected boolean isTransactional() { 
    return false;
  }

  protected void doGet(
      HttpServletRequest req, HttpServletResponse res
      ) throws IOException {	// djw: removed ServletException from the throws list

    // parse the URL parameters
    Arguments args = new Arguments(req.getParameterMap());
    long timeout = args.getLong("timeout", DEFAULT_TIMEOUT);
    if (timeout < 0) {
      timeout = Long.MAX_VALUE;
    }

    // create a synchronized result holder 
    FutureResult future = new FutureResult();

    // publish the request
    Job job = new Job(uidService.nextUID(), args, future);
    publishAdd(job);

    // wait for the result
    byte[] data = null;
    Exception e = null;
    try {
      data = (byte[]) future.timedGet(timeout);
    } catch (Exception ex) {
      // either a timeout or an invalid argument
      e = ex;
    }

    // cleanup the blackboard
    publishRemove(job);

    if (e != null) {
      // write error
      res.setContentType("text/plain");
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      PrintWriter out = res.getWriter();
      e.printStackTrace(out);
      return;
    }

    // write the data as a JPEG
    res.setContentType("image/jpeg");
    OutputStream out = res.getOutputStream();
    ImageOutput.writeJPG(
        args.getInt("width"), args.getInt("height"),
        data, out);
    out.close();
  }
}
