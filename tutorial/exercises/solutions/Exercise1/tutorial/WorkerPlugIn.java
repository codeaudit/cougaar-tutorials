/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package tutorial;

import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.cluster.IncrementalSubscription;
import java.util.Enumeration;
import org.cougaar.util.UnaryPredicate;

/**
 * This UnaryPredicate matches all Job objects
 */
class myPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    return (o instanceof Job);
  }
}

/**
 * This COUGAAR PlugIn subscribes to Job objects and prints them out.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: WorkerPlugIn.java,v 1.4 2001-04-05 19:28:45 mthome Exp $
 **/
public class WorkerPlugIn extends SimplePlugIn {
  // holds my subscription for Job objects (matching predicate above)
  private IncrementalSubscription jobs;

  /**
   * Called when the PlugIn is loaded.  Establish the subscription for
   * Job objects
   */
protected void setupSubscriptions() {
  jobs = (IncrementalSubscription)subscribe(new myPredicate());
  System.out.println("WorkerPlugIn");
}

/**
 * Called when there is a change on my subscription(s).
 * This plugin just prints all new jobs to stdout
 */
protected void execute () {
  Enumeration new_jobs = jobs.getAddedList();
  while (new_jobs.hasMoreElements()) {
    Job job = (Job)new_jobs.nextElement();
    System.out.println("WorkerPlugIn got a new Job: " + job);
  }
}

}
