package org.cougaar.tutorial.faststart.binary;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import java.util.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

/**
 * Binary search plugin
 * For the binary search, we will establish a task with VERB = MANAGE
 * with preferences START_TIME and END_TIME marking the bounds of the search
 * The response will succeed if the solution value is within the given bounds, 
 * and fail if not.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: BinaryIteratorPlugIn.java,v 1.2 2001-12-27 23:53:14 bdepass Exp $
 */
public class BinaryIteratorPlugIn extends org.cougaar.core.plugin.SimplePlugIn 
{

  // Set up subscription for all plan of tasks with verb 'MANAGE'
  private IncrementalSubscription allManageAllocations;
  private UnaryPredicate allManageAllocationsPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isAllocationWithVerb(o, BinaryUtils.MANAGE_VERB);
    }
  };

  // Set up 'MANAGE' allocation subscription, and set initial 'guess' task in motion
  public void setupSubscriptions() {
    //    System.out.println("BinaryIteratorPlugIn::setupSubscriptions");

    // Subscribe to allocations of 'MANAGE' tasks (CHANGED and NEW)
    allManageAllocations = 
      (IncrementalSubscription)subscribe(allManageAllocationsPredicate);

    // Create initial task to search the given range
    Task task = BinaryUtils.createNewTask(theLDMF);
    publishAdd(task);
    System.out.println("Publishing task!");
  }

  /**
   * Main execution loop of plugin : respond to any allocation (new or change)
   *  by changing preferences to try to close in on solution
   **/
  public void execute() {
    //    System.out.println("BinaryIteratorPlugIn::execute");

    // Look over all NEW allocations and allocation results and respond by changing
    // preferences
    for(Enumeration e_added = allManageAllocations.getAddedList(); 
	e_added.hasMoreElements();)
    {
      Allocation alloc = (Allocation)e_added.nextElement();
      //	System.out.println("Got a new allocation : " + alloc);
      respondToResults(alloc);
    }

    // Ditto for CHANGED allocations:respond by changing preferences
    for(Enumeration e_changed = allManageAllocations.getChangedList(); 
	e_changed.hasMoreElements();)
    {
      Allocation alloc = (Allocation)e_changed.nextElement();
      //	System.out.println("Got a changed allocation : " + alloc);
      respondToResults(alloc);
    }
  }

  // Respond to latest allocation results by changing preferences
  private void respondToResults(Allocation alloc) 
  {
    Task task = alloc.getTask();

    // Pull bounds out of task preferences
    double []bounds = 
      TutorialUtils.getPreferredValueBounds
      (task, BinaryUtils.BINARY_BOUNDS_ASPECT,-1.0);
    double low_bounds = bounds[0];
    double high_bounds = bounds[1];

    // System.out.println("respondToResults : " + low_bounds + " " + high_bounds);

    Allocation allocation = (Allocation)task.getPlanElement();
    AllocationResult ar_est = allocation.getEstimatedResult();
    AllocationResult ar_rep = allocation.getReportedResult();

    if (ar_est != null)
    {
      if ((ar_rep == null) || (ar_est == ar_rep)) {

        // The gap between high and low is returned in bounds aspect
        // So we can see if the result matches the task preferences
        // If not, we were woken up unnecessarily, and don't modify preferences
        double gap = ar_est.getValue(BinaryUtils.BINARY_BOUNDS_ASPECT);
        if ((high_bounds - low_bounds) == gap) {

          if (ar_est.isSuccess()) {

            if (high_bounds - low_bounds < 1.0) {
              // We've converged on a set of preferences 
              // that exactly match the solution
              System.out.println("BinaryIteratorPlugin : Converged to solution " + 
                Math.floor(high_bounds));
            } 
            else {
              // break space in half [always try lower half] and try again
              // System.out.println("Iterating on successful previous guess");
              BinaryUtils.UpdatePreferences
                (task, low_bounds, 
              low_bounds + (high_bounds - low_bounds)/2.0,
              theLDMF);
              publishChange(task);
            }
          } 
          else {
            // We picked the wrong side of the space [lower] last time, so we know
            // we're in the other half [ upper]. Break that in half and try again.
            // Original bounds were low_bounds, and low_bounds + 2*(high_bounds-low_bounds)
            // So we break space between current high bounds and original high bounds
            // System.out.println("Iterating on unsuccessful previous guess");
            double orig_high_bounds = low_bounds + 2.0*(high_bounds-low_bounds);
            BinaryUtils.UpdatePreferences
              (task, high_bounds, 
            high_bounds + (orig_high_bounds - high_bounds)/2.0,
            theLDMF);
            publishChange(task);
          }
        }
      }
    }
  }
}


