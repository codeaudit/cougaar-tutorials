package org.cougaar.demo.mandelbrot.v3;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.FutureResult;
import org.cougaar.demo.mandelbrot.util.Arguments;

/**
 * A blackboard object representing a fractal request.
 */
public final class Job implements UniqueObject {

  private final UID uid;
  private final Arguments args;
  private final FutureResult future;

  /**
   * Create a new Job instance.
   * @param uid the unique job identifier, as generated by the
   *   {@link org.cougaar.core.service.UIDService}
   * @param args the fractal calculation arguments
   * @param future a synchronizer to notify with the calculation
   *   result.
   */
  public Job(UID uid, Arguments args, FutureResult future) {
    this.uid = uid;
    this.args = args;
    this.future = future;

    String s =
      (uid == null ? "uid" :
       args == null ? "args" :
       future == null ? "future" :
       null);
    if (s != null) {
      throw new IllegalArgumentException("null "+s);
    }
  }

  public UID getUID() { return uid; }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException("UID already set");
  }

  public Arguments getArguments() { return args; }

  public FutureResult getFutureResult() { return future; }

  // all blackboard objects require good hashCode/equals methods,
  // since the blackboard is essentially a HashSet.
  public int hashCode() { return uid.hashCode(); }
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Job)) return false;
    return uid.equals(((Job) o).uid);
  }

  public String toString() { return "(job uid="+uid+")"; }
}