package org.cougaar.tutorial.faststart.calendar;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import java.awt.event.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.tutorial.faststart.*;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
/**
 * Simple UI plugin to create a task requesting a free day in the 
 * calendar
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarRequesterPlugin.java,v 1.3 2003-01-22 23:09:10 mbarger Exp $
 */
public class CalendarRequesterPlugin extends ComponentPlugin
{
    private DomainService domainService;
    private PlanningFactory ldmf;
    public void setDomainService(DomainService value) {
	domainService=value;
    }
    public DomainService getDomainService() { 
	return domainService; 
    }

  /**
   * Initialize plugin : no subscriptions, but pop up the command UI
   **/
  public void setupSubscriptions() 
  {
    //    System.out.println("CalendarRequesterPlugin::setupSubscriptions()");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        generateTask();
      }
    };
    TutorialUtils.createGUI("Request appointment", "Calendar", listener);
  }

  /// Create and publish a task to log plan requesting scheduling
  // NOTE : This must be done from a PluginAdapter method within Transaction
  // blocks as shown.
  private void generateTask() 
  {
    // System.out.println("Executing new Calendar Request...");
    getBlackboardService().openTransaction();
    Task task = CalendarUtils.createTask(ldmf);
    getBlackboardService().publishAdd(task);
    getBlackboardService().closeTransactionDontReset();
  }

  // No execution for this plugin
  public void execute()
  {
  }

}
