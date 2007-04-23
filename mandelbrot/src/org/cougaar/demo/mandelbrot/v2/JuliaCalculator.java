package org.cougaar.demo.mandelbrot.v2;

import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.demo.mandelbrot.util.FractalMath;

/**
 * This plugin advertises a {@link FractalService} that computes the
 * Julia Set.
 * <p>
 * Two optional plugin parameters are supported, where the defaults are:<pre>
 *   &lt;argument name="cx" value="-0.70176"/&gt;
 *   &lt;argument name="cy" value="-0.3842"/&gt;
 * </pre>
 *
 * @see MandelbrotCalculator The MandelbrotCalculator is a pluggable
 * replacement for this plugin
 */
public class JuliaCalculator extends CalculatorBase {

  /**
   * This default is from the Wikipedia "Julia_set" page.
   * <p>
   * A couple interesting configurations from that page are:<ul>
   *   <li>cx=-0.70176, cy=-0.3842</li>
   *   <li>cx= 0.285,   cy= 0.01</li>
   *   <li>cx=-0.382,   cy= 0.618</li>
   * </ul>
   */
  private static final Arguments DEFAULT_ARGS = 
    new Arguments(new String[] { "cx=-0.70176", "cy=-0.3842" });

  private Arguments args = DEFAULT_ARGS;

  public void setParameter(Object o) {
    args = new Arguments(o, DEFAULT_ARGS);
  }

  protected byte[] calculate(
      int width, int height,
      double min_x, double max_x,
      double min_y, double max_y) {
    return
      FractalMath.julia(
          width, height,
          min_x, max_x,
          min_y, max_y,
          args.getDouble("cx"), args.getDouble("cy"));
  }
}
