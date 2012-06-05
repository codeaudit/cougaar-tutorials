package org.cougaar.demo.mandelbrot.v2;

import org.cougaar.core.component.Service;

/**
 * This service provides access to a calculator implementation.
 * 
 * @see MandelbrotCalculator Mandelbrot Set implementation
 * @see JuliaCalculator Julia Set implementation
 */
public interface FractalService extends Service {
   byte[] calculate(int width, int height, double min_x, double max_x, double min_y, double max_y);
}
