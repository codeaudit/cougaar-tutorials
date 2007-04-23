package org.cougaar.demo.mandelbrot.v2;

/**
 * This service provides access to a calculator implementation.
 *
 * @see MandelbrotCalculator Mandelbrot Set implementation
 * @see JuliaCalculator Julia Set implementation
 */
public interface FractalService {
  byte[] calculate(
      int width, int height,
      double min_x, double max_x,
      double min_y, double max_y);
}
