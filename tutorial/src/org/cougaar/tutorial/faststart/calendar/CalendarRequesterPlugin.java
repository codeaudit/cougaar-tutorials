package org.cougaar.tutorial.faststart.calendar;

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
 * @version $Id: CalendarRequesterPlugin.java,v 1.4 2003-01-23 22:12:54 mbarger Exp $
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
