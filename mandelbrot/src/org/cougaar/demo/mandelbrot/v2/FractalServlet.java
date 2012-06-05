package org.cougaar.demo.mandelbrot.v2;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.demo.mandelbrot.util.ImageOutput;
import org.cougaar.util.annotations.Cougaar;

/**
 * This servlet uses the {@link FractalService} to calculate the image data.
 */
public class FractalServlet
      extends ComponentServlet {

   private static final long serialVersionUID = 1L;
   
   @Cougaar.ObtainService()
   public FractalService svc;

   @Override
   public void unload() {
      // shutting down, release the FractalService
      if (svc != null) {
         releaseService(this, FractalService.class, svc);
         svc = null;
      }

      super.unload();
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse res)
         throws ServletException, IOException {

      // parse the URL parameters
      Arguments args = new Arguments(req.getParameterMap());

      // calculate the image data
      byte[] data =
            svc.calculate(args.getInt("width"), args.getInt("height"), args.getDouble("x_min"), args.getDouble("x_max"),
                          args.getDouble("y_min"), args.getDouble("y_max"));

      // write the data as a JPEG
      res.setContentType("image/jpeg");
      OutputStream out = res.getOutputStream();
      ImageOutput.writeJPG(args.getInt("width"), args.getInt("height"), data, out);
      out.close();
   }
}
