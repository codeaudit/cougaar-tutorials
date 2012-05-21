package org.cougaar.demo.mandelbrot.v3;

import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.demo.mandelbrot.util.Arguments;
import org.cougaar.util.FutureResult;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin is a base class that listens for {@link Job} blackboard objects
 * and calculates the result.
 */
public abstract class CalculatorBase
      extends ComponentPlugin {

   private static final UnaryPredicate JOB_PRED = new UnaryPredicate() {
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
         return (o instanceof Job);
      }
   };

   private IncrementalSubscription sub;

   @Override
   protected void setupSubscriptions() {
      sub = (IncrementalSubscription) blackboard.subscribe(JOB_PRED);
   }

   @Override
   protected void execute() {
      if (!sub.hasChanged()) {
         return;
      }
      for (Iterator iter = sub.getAddedCollection().iterator(); iter.hasNext();) {
         Job job = (Job) iter.next();
         Arguments args = job.getArguments();
         FutureResult future = job.getFutureResult();

         try {
            byte[] data =
                  calculate(args.getInt("width"), args.getInt("height"), args.getDouble("x_min"), args.getDouble("x_max"),
                            args.getDouble("y_min"), args.getDouble("y_max"));
            future.set(data);
         } catch (Exception e) {
            future.setException(e);
         }
      }
   }

   /** Implement this method. */
   protected abstract byte[] calculate(int width, int height, double min_x, double max_x, double min_y, double max_y);
}
