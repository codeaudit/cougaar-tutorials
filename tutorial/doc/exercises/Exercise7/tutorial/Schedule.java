/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package tutorial;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class is used by ProgrammerAssetAdapter to hold the asset's
 * view of its schedule.
 * Mapping is key=Ingeter(month) value=assignment where assignment is either
 * a string like "Vacation" or a Task object.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: Schedule.java,v 1.3 2003-01-23 19:44:22 mthome Exp $
 */
public class Schedule extends Hashtable implements Serializable {

  /**
   * Create an empty schedule
   */
  public Schedule() {
  }

  /**
   * Assign an activity to a month.
   * @param month the month that the activity is scheduled for
   * @param work the activity for the month
   */
  public Object setWork(int month, Object work) {
    return this.put(new Integer(month), work);
  }

  /**
   * Get the activity scheduled for this month
   * @param month the month that we're interested in
   * @return the work scheduled for this month (or null if none)
   */
  public Object getWork(int month) {
    return this.get(new Integer(month));
  }

  /**
   * Remove the assignment for this month.
   * @param month the month to delete the assignment from
   * @return the previously-scheduled activity
   */
  public Object clearWork(int month) {
    return this.remove(new Integer(month));
  }
}
