/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package tutorial;

import tutorial.assets.*;
import alp.util.UnaryPredicate;
import alp.cluster.*;
import alp.ldm.plan.*;
import alp.ui.planserver.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;



/**
 * This PSP assigns a vacation month to each ProgrammerAsset.
 * It always looks for the earliest scheduled (to a task) month
 * for the vacation month.  It responds with text describing what it did.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: PSP_ScheduleVacation.java,v 1.1 2000-12-15 20:19:00 mthome Exp $
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

