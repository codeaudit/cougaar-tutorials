package org.cougaar.tutorial.faststart.calendar;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import java.awt.event.*;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.tutorial.faststart.*;

/**
 * Simple UI plugin to create a task requesting a free day in the 
 * calendar
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarRequesterPlugIn.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 */
public class CalendarRequesterPlugIn extends org.cougaar.core.plugin.SimplePlugIn 
{

  /**
   * Initialize plugin : no subscriptions, but pop up the command UI
   **/
  public void setupSubscriptions() 
  {
    //    System.out.println("CalendarRequesterPlugIn::setupSubscriptions()");

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        generateTask();
      }
    };
    TutorialUtils.createGUI("Request appointment", "Calendar", listener);
  }

  /// Create and publish a task to log plan requesting scheduling
  // NOTE : This must be done from a PlugInAdapter method within Transaction
  // blocks as shown.
  private void generateTask() 
  {
    // System.out.println("Executing new Calendar Request...");
    openTransaction();
    Task task = CalendarUtils.createTask(theLDMF);
    publishAdd(task);
    closeTransaction(false);
  }

  // No execution for this plugin
  public void execute()
  {
  }

}