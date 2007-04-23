package org.cougaar.demo.mandelbrot.util;

/**
 * Fractal math routines.
 */
public final class FractalMath {
  
  private FractalMath() {}

  /** @see #compute(double,double,double,double) */
  public static byte mandelbrot(double x0, double y0) {
    return compute(x0, y0, x0, y0);
  }
  /** @see #compute(double,double,double,double) */
  public static byte julia(double x0, double y0, double cx, double cy) {
    return compute(x0, y0, cx, cy);
  }

  /**
   * Compute a single pixel for either the Mandelbrot or Julia Set.
   *
   * @param x0 the initial x value
   * @param y0 the initial y value
   * @param cx for the Mandlebrot Set use x0, otherwise for the Julia Set
   * use a constant (e.g. -0.70176)
   * @param cy for the Mandlebrot Set use y0, otherwise for the Julia Set
   * use a constant (e.g. -0.3842)
   */
  public static byte compute(double x0, double y0, double cx, double cy) {
    double x = x0;
    double y = y0;
    int i;
    for (i = 0; i < 255; i++) {
      double x2 = x * x;
      double y2 = y * y;
      if ((x2 + y2) > 4.0) {
        break;
      }
      double tmp = x * y;
      tmp += tmp;  // tmp *= 2.0
      x = x2 - y2 + cx;
      y = tmp + cy;
    }
    return (byte) i;
  }

  /** @see #compute(double,double,double,double,boolean,double,double) */
  public static byte[] mandelbrot(
      int width, int height,
      double x_min, double x_max,
      double y_min, double y_max) {
    return compute(width, height, x_min, x_max, y_min, y_max, false, 0, 0);
  }
  /** @see #compute(double,double,double,double,boolean,double,double) */
  public static byte[] julia(
      int width, int height,
      double x_min, double x_max,
      double y_min, double y_max,
      double cx, double cy) {
    return compute(width, height, x_min, x_max, y_min, y_max, true, cx, cy);
  }

  /**
   * Compute all pixel values within the given range.
   *
   * @param isJulia if true then use the "cx" and "cy" values to compute the
   * Julia Set, otherwise ignore the "cx" and "cy" values and compute the
   * MandelbrotSet
   * @param cx only used if isJulia is true
   * @param cy only used if isJulia is true
   *
   * @return an array of size (width*height).
   */
  public static byte[] compute(
      int width, int height,
      double x_min, double x_max,
      double y_min, double y_max,
      boolean isJulia, double cx, double cy) {

    validateRange(width, height);

    int data_length = (height * width);
    byte[] data = new byte[data_length];

    compute(
        width, height,
        x_min, x_max,
        y_min, y_max,
        isJulia, cx, cy,
        data);

    return data;
  }

  /** @see #compute(double,double,double,double,boolean,double,double,byte[]) */
  public static void mandelbrot(
      int width, int height,
      double x_min, double x_max,
      double y_min, double y_max,
      byte[] data) {
    compute(width, height, x_min, x_max, y_min, y_max, false, 0, 0, data);
  }
  /** @see #compute(double,double,double,double,boolean,double,double,byte[]) */
  public static void julia(
      int width, int height,
      double x_min, double x_max,
      double y_min, double y_max,
      double cx, double cy,
      byte[] data) {
    compute(width, height, x_min, x_max, y_min, y_max, true, cx, cy, data);
  }

  /**
   * @see #compute(int,int,double,double,double,double,boolean,double,double)
   * Same as the other "compute", except pass in the byte[] instead of
   * allocating and returning one.
   */
  public static void compute(
      int width, int height,
      double x_min, double x_max,
      double y_min, double y_max,
      boolean julia, double cx, double cy,
      byte[] data) {

    // validate
    validateRange(width, height);
    int data_length = (height * width);
    if (data == null || data.length < data_length) {
      throw new IllegalArgumentException(
          "Given "+
          (data == null ? "null" : "small byte["+data.length+"]")+
          " data array, expecting a byte[("+width+"*"+height+")]");
    }

    // compute step size
    double x_step = (x_max - x_min) / width;
    double y_step = (y_max - y_min) / height;

    // compute data
    //
    // note that we use
    //   v += v_step
    // instead of
    //   v = min_v + (i * v_step)
    // the "+=" is faster but introduces (negligible) rounding errors.
    int data_index = 0;
    double y = y_min;
    for (int row = 0; row < height; row++) {
      double x = x_min;
      for (int col = 0; col < width; col++) {
        byte b = compute(x, y, (julia ? cx : x), (julia ? cy : y));
        data[data_index++] = b;
        x += x_step;
      }
      y += y_step;
    }
  }

  private static void validateRange(int width, int height) {
    if (width <= 0) {
      throw new IllegalArgumentException("Invalid width: "+width);
    }
    if (height <= 0) {
      throw new IllegalArgumentException("Invalid height: "+height);
    }
  }
}
