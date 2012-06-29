package org.cougaar.demo.mandelbrot;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.demo.mandelbrot.util.Arguments;

/**
 * This servlet writes the HTML page that embeds the generated JPEG image.
 * <p>
 * All the fractal math and drawing are done in the MandelbrotServlet. That
 * servlet listens on the "/image" servlet path.
 * <p>
 * This servlet writes the outer HTML page and "&lt;img src=.../&gt;" tag. The
 * browser then requests the JPEG from the "/image" servlet.
 */

// R. Kurtz 02 Feb 2012 - Existing code apparently makes use of
// some obsolete Javascript features, specifically myImage.x and
// myImage.y; using the .offsetLeft and .offsetTop attributes
// instead appears to work correctly.

public class FrontPageServlet
      extends ComponentServlet {

   private static final long serialVersionUID = 1L;
   // default mandelbrot parameters
   private static final Arguments DEFAULT_SELECTION = new Arguments(new String[] {
      "width=750",
      "height=500",
      "x_min=-2.0",
      "x_max=1.0",
      "y_min=-1.0",
      "y_max=1.0"
   });
   // this is the servlet path of our MandelbrotServlet, which
   // does the real work. It generates the JPEG image.
   private static final String IMAGE_SERVLET_PATH = "/image";
   
   /** This method is called when the agent is created.
    *  It is only here to log a message to let the user know what to browser to
    */
   @Override
   public void start() {
      super.start();
      
      LoggingService log = getService(this, LoggingService.class,  null);
      
      log.warn("Mandelbrot agent has been started");

      log.warn("Browse to http://localhost:8800/$Node1/mandelbrot to interact with Mandelbrot");
   }


   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse res)
         throws ServletException, IOException {

      // parse the URL parameters
      Arguments args = new Arguments(req.getParameterMap(), DEFAULT_SELECTION.keySet(), DEFAULT_SELECTION);

      // fix x/y coordinates to be upright
      if (args.getDouble("x_max") < args.getDouble("x_min")) {
         args.swap("x_max", "x_min");
      }
      if (args.getDouble("y_max") < args.getDouble("y_min")) {
         args.swap("y_max", "y_min");
      }

      // fix x/y aspect ratio to match our width/height ratio
      //
      // this ensures that
      // (width/height) == ((x_max - x_min)/(y_max - y_min));
      int w = Math.max(args.getInt("width"), 1);
      int h = Math.max(args.getInt("height"), 1);
      double wh_ratio = ((double) w / h);
      double x_range = (args.getDouble("x_max") - args.getDouble("x_min"));
      double y_range = (args.getDouble("y_max") - args.getDouble("y_min"));
      double xy_ratio = (x_range / y_range);
      if (xy_ratio < wh_ratio) {
         double x_max = wh_ratio * y_range + args.getDouble("x_min");
         args.setDouble("x_max", x_max);
         x_range = (x_max - args.getDouble("x_min"));
      } else {
         double y_min = args.getDouble("y_max") - x_range / wh_ratio;
         args.setDouble("y_min", y_min);
         y_range = (args.getDouble("y_max") - y_min);
      }

      // figure out the image servlet URI
      String baseURI = req.getRequestURI();
      String ext;
      if ((ext = req.getPathInfo()) != null) {
         baseURI = baseURI.substring(0, baseURI.length() - ext.length());
      }
      if ((ext = req.getServletPath()) != null) {
         baseURI = baseURI.substring(0, baseURI.length() - ext.length());
      }
      String imgURI = baseURI + IMAGE_SERVLET_PATH;

      // write the top-level html page
      res.setContentType("text/html");
      PrintWriter out = res.getWriter();
      out.println(
      // header
      "<html><head><title>Mandelbrot Servlet</title></head><body>" + "<h2>Mandelbrot Servlet</h2>" + // zoom
                                                                                                     // out
            "<form method=\"GET\" action=\""
            + req.getRequestURI()
            + "\">\n"
            + "Click on two points in the image to zoom in, or "
            + DEFAULT_SELECTION.toString("<input name=\"$key\" type=\"hidden\" value=\"$value\">\n")
            + "<input type=\"submit\" value=\"Zoom Out\">\n"
            + "</form>\n"
            + // image
            "<img id=\"myImage"
            + "\" width=\""
            + args.getInt("width")
            + "\" height=\""
            + args.getInt("height")
            + "\" src=\""
            + imgURI
            + "?"
            + args.toString("&$key=$value")
            + "\"/>\n"
            + // selection
            "<form method=\"GET\" name=\"f\" action=\""
            + req.getRequestURI()
            + "\">\n"
            + args.toString(" $key=<input name=\"$key\" type=\"text\" size=\"4\"" + " value=\"$value\">\n")
            + "<input type=\"submit\" value=\"Submit\">\n"
            + // reset
            "<input type=\"button\" name=\"reset\" value=\"Reset\" onClick=\""
            + DEFAULT_SELECTION.toString("document.f.$key.value='$value';")
            + "\">\n"
            + "</form>\n"
            + // javascript
            "<script type=\"text/javascript\">\n"
            + "var myImage = document.getElementById(\"myImage\");\n"
            + "var click_count=0;\n"
            + "function onImageClick(e2) {\n"
            + // R. Kurtz 02 Feb 2012 - Existing code apparently makes use of
              // some obsolete Javascript features, specifically myImage.x and
              // myImage.y; using the .offsetLeft and .offsetTop attributes
              // instead appears to work correctly.
              // "  var w = e.clientX - myImage.x +\n"+
              // "    (document.all ? document.body.scrollLeft : window.scrollX);\n"+
              // "  var h = e.clientY - myImage.y +\n"+
              // "    (document.all ? document.body.scrollTop : window.scrollY);\n"+
            "  var e=window.event || e2;  var w = e.clientX - myImage.offsetLeft +\n" + "    (document.all ? document.body.scrollLeft : window.scrollX);\n"
            + "  var h = e.clientY - myImage.offsetTop +\n" + "    (document.all ? document.body.scrollTop : window.scrollY);\n"
            + "  var x = " + args.getDouble("x_min") + " +\n" + "    (w/" + args.getInt("width") + ")*" + x_range + ";\n"
            + "  var y = " + args.getDouble("y_min") + " +\n" + "    (h/" + args.getInt("height") + ")*" + y_range + ";\n"
            + "  if ((click_count++ % 2) == 0) {\n" + "    document.f.x_max.value=x;\n" + "    document.f.y_max.value=y;\n"
            + "  } else {\n" + "    document.f.x_min.value=x;\n" + "    document.f.y_min.value=y;\n" + "    document.f.submit();\n"
            + // could use myImg.src=...
            "  }\n" + "}\n" + "myImage.onmousedown = onImageClick;\n" + "</script>\n" + // footer
            "</body></html>");
      out.close();
   }
}
