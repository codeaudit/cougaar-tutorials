/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import tutorial.assets.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.lib.planserver.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;



/**
 * This PSP assigns a vacation month to each ProgrammerAsset.
 * It always looks for the earliest scheduled (to a task) month
 * for the vacation month.  It responds with text describing what it did.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: PSP_ScheduleVacation.java,v 1.2 2001-08-22 20:30:48 mthome Exp $
 */
public class PSP_ScheduleVacation extends PSP_BaseAdapter implements PlanServiceProvider, UISubscriber
{
  /** A zero-argument constructor is required for dynamically loaded PSPs,
   *         required by Class.newInstance()
   **/
  public PSP_ScheduleVacation()
  {
    super();
  }

  /**
   * This constructor includes the URL path as arguments
   */
  public PSP_ScheduleVacation( String pkg, String id ) throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  /**
   * Some PSPs can respond to queries -- URLs that start with "?"
   * I don't respond to queries
   */
  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }


  /**
   * Called when a HTTP request is made of this PSP.
   * @param out data stream back to the caller.
   * @param query_parameters tell me what to do.
   * @param psc information about the caller.
   * @param psu unused.
   */
  public void execute( PrintStream out,
                       HttpInput query_parameters,
                       PlanServiceContext psc,
                       PlanServiceUtilities psu ) throws Exception
  {
    Collection programmers = null;
    try {
      System.out.println("PSP_ScheduleVacation called from " + psc.getSessionAddress());

      programmers = psc.getServerPlugInSupport().queryForSubscriber(
                      new UnaryPredicate() {
                        public boolean execute(Object o) {
                          return o instanceof ProgrammerAsset;
                        }});

      Iterator iter = programmers.iterator();
      while (iter.hasNext()) {
        ProgrammerAsset pa = (ProgrammerAsset)iter.next();
        if (makeVacation(pa, out)) {
          psc.getServerPlugInSupport().publishChangeForSubscriber(pa);
        }
      }

    } catch (Exception ex) {
      out.println(ex.getMessage());
      ex.printStackTrace(out);
      System.out.println(ex);
      out.flush();
    }
  }

  /**
   * Find and take a vacation month for this programmer.  Print the vacation
   * month to the PrintStream
   */
  private boolean makeVacation(ProgrammerAsset pa, PrintStream out) {
    boolean vacation = false;
    int min_month = Integer.MAX_VALUE;
    Schedule s = pa.getSchedule();
    Enumeration months = s.keys();
    // find the earliest month to force the most replanning
    while (months.hasMoreElements()) {
      int month = ((Integer)months.nextElement()).intValue();
      if ((month < min_month) && (s.getWork(month) instanceof Task))
        min_month = month;
    }
    if (min_month != Integer.MAX_VALUE) {
      s.setWork(min_month, "Vacation");
      out.println("<p>Vacation time for "+pa.getItemIdentificationPG().getItemIdentification() +
                  " month: "+min_month+"</p>");
      vacation = true;
    }
    return vacation;
  }

  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return true;
  }

  /**  Any PlanServiceProvider must be able to provide DTD of its
   *  output IFF it is an XML PSP... ie.  returnsXML() == true;
   *  or return null
   **/
  public String getDTD()  {
    return null;
  }

  /**
   * The UISubscriber interface. (not needed)
   */
  public void subscriptionChanged(Subscription subscription) {
  }

}

