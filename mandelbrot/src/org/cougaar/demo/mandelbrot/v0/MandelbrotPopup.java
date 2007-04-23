package org.cougaar.demo.mandelbrot.v0;

import java.io.FileOutputStream;
import java.io.OutputStream;
import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.demo.mandelbrot.util.FractalMath;
import org.cougaar.demo.mandelbrot.util.ImageOutput;

/**
 * A minimal non-Cougaar application that computes the Mandelbrot Set image
 * data and either writes it to a file or pops up a UI.
 * <p>
 * To write to a file, specify a "file=<i>String</i>" argument.  If no file
 * is specified then the image will be displayed in a popup Swing UI.
 * <p>
 * Usage is:<pre>
 *   java \
 *     -classpath lib/mandelbrot.jar \
 *     org.cougaar.demo.mandelbrot.v0.MandelbrotPopup \
 *     [ARGS]
 * </pre>
 * Supported arguments are:<ul>
 *   <li>width=<i>integer</i></li>
 *   <li>height=<i>integer</i></li>
 *   <li>x_min=<i>double</i></li>
 *   <li>x_max=<i>double</i></li>
 *   <li>y_min=<i>double</i></li>
 *   <li>y_max=<i>double</i></li>
 *   <li>file=<i>String</i></li>
 * </ul>
 */
public class MandelbrotPopup {

  // default mandelbrot parameters
  private static final Arguments DEFAULT_SELECTION = 
    new Arguments(new String[] {
      "width=750",
      "height=500",
      "x_min=-2.0",
      "x_max=1.0",
      "y_min=-1.0",
      "y_max=1.0"});

  public static void main(String[] sa) {
    // parse our arguments
    Arguments args = new Arguments(sa, DEFAULT_SELECTION);

    // compute the mandelbrot data
    byte[] data = 
      FractalMath.mandelbrot(
          args.getInt("width"), args.getInt("height"),
          args.getDouble("x_min"), args.getDouble("x_max"),
          args.getDouble("y_min"), args.getDouble("y_max"));

    // optionally write to file
    String filename = args.getString("file");
    if (filename != null) {
      try {
        OutputStream out = new FileOutputStream(filename);
        ImageOutput.writeJPG(
            args.getInt("width"), args.getInt("height"),
            data, out);
        out.close();
      } catch (Exception e) {
        throw new RuntimeException("Unable to write to "+filename, e);
      }
      System.out.println("Wrote JPEG to "+filename);
      return;
    }

    // display the data as a Swing popup UI
    //
    // Ideally our popup would support interactive navigation, but for now
    // it's a trivial image display.
    //
    // In contrast, our servlet version of this UI uses HTML forms and
    // JavaScript to support interactive navigation.  See ../FrontPageServlet.
    ImageOutput.displayImage(
        args.getInt("width"), args.getInt("height"), data);

    // the popup "close" button will call "System.exit"
  }
}
