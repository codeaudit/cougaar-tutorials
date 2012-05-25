package org.cougaar.demo.mandelbrot.v3;

import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.util.FutureResult;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe.ModType;;

/**
 * This plugin is a base class that listens for {@link Job} blackboard objects
 * and calculates the result.
 */
public abstract class CalculatorBase
      extends AnnotatedSubscriptionsPlugin {
   
   abstract byte[] calculate(int width, int height, double min_x, double max_x, double min_y, double max_y);


   @Cougaar.Execute(on = { ModType.CHANGE, ModType.ADD})
   public void runJob(Job job) {

      FutureResult future = job.getFutureResult();
      try {
         Arguments args = job.getArguments();
         byte[] data =
               calculate(args.getInt("width"), args.getInt("height"), args.getDouble("x_min"), args.getDouble("x_max"),
                         args.getDouble("y_min"), args.getDouble("y_max"));
         future.set(data);
      } catch (Exception e) {
         future.setException(e);
      }
   }
}
