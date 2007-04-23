package org.cougaar.demo.mandelbrot.v1;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.demo.mandelbrot.util.FractalMath;
import org.cougaar.demo.mandelbrot.util.ImageOutput;

/**
 * This servlet is a simple inlined implementation of the Mandelbrot image
 * generator.
 * <p>
 * We'll assume that the {@link org.cougaar.demo.mandelbrot.FrontPageServlet}
 * has set and validated our URL parameters and adjusted the x/y values to
 * preserve our width/height aspect ratio.
 */
public class MandelbrotServlet extends ComponentServlet {
  protected void doGet(
      HttpServletRequest req, HttpServletResponse res
      ) throws ServletException, IOException {

    // parse the URL parameters
    Arguments args = new Arguments(req.getParameterMap());

    // compute the mandelbrot data
    byte[] data = 
      FractalMath.mandelbrot(
          args.getInt("width"), args.getInt("height"),
          args.getDouble("x_min"), args.getDouble("x_max"),
          args.getDouble("y_min"), args.getDouble("y_max"));

    // write the data as a JPEG
    res.setContentType("image/jpeg");
    OutputStream out = res.getOutputStream();
    ImageOutput.writeJPG(
        args.getInt("width"), args.getInt("height"),
        data, out);
    out.close();
  }
}
