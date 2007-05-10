package org.cougaar.demo.mandelbrot.v2;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

/**
 * This component is a base class that advertises the {@link FractalService}.
 */
public abstract class CalculatorBase extends ComponentSupport {

  private ServiceProvider sp;

  public void load() {
    super.load();

    // wrap our "calculate" method as a service
    FractalService svc = new FractalService() {
      public byte[] calculate(
          int width, int height,
          double min_x, double max_x,
          double min_y, double max_y) {
        return CalculatorBase.this.calculate(
              width, height,
              min_x, max_x,
              min_y, max_y);
      }
    };

    // advertise our service
    sp = new TrivialServiceProvider(svc);
    sb.addService(FractalService.class, sp);
  }

  public void unload() {
    // shutting down, revoke our service
    if (sp != null) {
      sb.revokeService(FractalService.class, sp);
      sp = null;
    }

    super.unload();
  }

  /** Implement this method. */
  protected abstract byte[] calculate(
      int width, int height,
      double min_x, double max_x,
      double min_y, double max_y);

  private static final class TrivialServiceProvider implements ServiceProvider {
    private final Object svc;
    public TrivialServiceProvider(Object svc) {
      this.svc = svc;
    }
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      return svc;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service) {
    }
  }
}
