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

/**
 * This class can be published to the PLAN and subscribed to by PlugIns
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: Job.java,v 1.2 2001-04-05 19:28:38 mthome Exp $
 **/
public class Job implements java.io.Serializable {

  String what;
  public Job(String what) {
    this.what = what;
  }

  public String toString() {
    return what;
  }
} 