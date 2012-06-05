package org.cougaar.demo.mandelbrot.v2;

import org.cougaar.demo.mandelbrot.util.FractalMath;

/**
 * This plugin advertises a {@link FractalService} that computes the Mandelbrot
 * Set.
 * 
 * @see JuliaCalculator The JuliaCalculator is a pluggable replacement for this
 *      plugin
 */
public class MandelbrotCalculator
      extends CalculatorBase {
   @Override
   protected byte[] calculate(int width, int height, double min_x, double max_x, double min_y, double max_y) {
      return FractalMath.mandelbrot(width, height, min_x, max_x, min_y, max_y);
   }
}
