package org.cougaar.demo.mandelbrot.v3;

import org.cougaar.demo.mandelbrot.util.FractalMath;

/**
 * This plugin listens for {@link Job} blackboard objects and calculates
 * a Mandelbrot Set result.
 */
public class MandelbrotCalculator extends CalculatorBase {
  protected byte[] calculate(
      int width, int height,
      double min_x, double max_x,
      double min_y, double max_y) {
    return
      FractalMath.mandelbrot(
          width, height,
          min_x, max_x,
          min_y, max_y);
  }
}
