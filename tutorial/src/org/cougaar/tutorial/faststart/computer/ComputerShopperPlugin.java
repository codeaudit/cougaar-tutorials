package org.cougaar.tutorial.faststart.computer;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import java.util.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;

/**
 * Computer shopper plugin - makes requests for computers
 * asking for 'as much CPU and RAM as possible for as cheap as possible
 * and as soon as possible' with different scoring functions and weights.
 * Operates in both estimate and ordering modes
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ComputerShopperPlugin.java,v 1.1 2002-02-12 19:30:41 jwinston Exp $
 */
public class ComputerShopperPlugin  extends ComponentPlugin
{  
  private DomainService domainService = null;

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return domainService;
  }

  // Set up subscription for all plan elements of tasks with verb 'SUPPLY'
  private IncrementalSubscription allSupplyPlanElements;
  private UnaryPredicate allSupplyPlanElementsPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isPlanElementWithVerb(o, ComputerUtils.SUPPLY_VERB);
  }};

  // Set up 'SUPPLY' allocation subscription, 
  // and generate tasks requesting computers
  public void setupSubscriptions() {
    //    System.out.println("ComputerRequesterPlugin::setupSubscriptions");

    // Subscribe to new allocations of 'SUPPLY' tasks
    allSupplyPlanElements =
      (IncrementalSubscription)getBlackboardService().subscribe(allSupplyPlanElementsPredicate);

    // Set up dummy arrays with different mixes of weights
    double []price_weights = {0.25, 1.0, 0.0, 0.0, 0.0, 0.25, 0.25};
    double []ship_weights = {0.25, 0.0, 1.0, 0.0, 0.0, 0.25, 0.25};
    double []cpu_weights = {0.25, 0.0, 0.0, 1.0, 0.0, 0.25, 0.25};
    double []ram_weights = {0.25, 0.0, 0.0, 0.0, 1.0, 0.25, 0.25};

    // Create set of requests for computers based on different weights
    // on preferences between price, shipping time, CPU speed and RAM
    for(int i = 0; i < price_weights.length;i++) {
      Task task = ComputerUtils.createNewTask(price_weights[i],
					      ship_weights[i],
					      cpu_weights[i],
					      ram_weights[i],
					      getDomainService().getFactory());
      getBlackboardService().publishAdd(task);
      //      System.out.println("Publishing task!");
    }
  }

  /**
   * Main execution loop of plugin : respond to any plan element
   *  by printing result
   **/
  public void execute() {
    //    System.out.println("ComputerRequesterPlugin::execute");

    // Look over all NEW PE's and allocation results and
    // respond by printing results
    for(Enumeration e_added = allSupplyPlanElements.getAddedList(); 
	e_added.hasMoreElements();)
      {
	PlanElement pe = (PlanElement)e_added.nextElement();
	//	System.out.println("Got a new PE : " + pe);
	printResults(pe);
      }
  }

  // Print results of planElement - how did we do on the different aspects?
  private void printResults(PlanElement pe)
  {
    AllocationResult result = pe.getEstimatedResult();

    if (result.isSuccess()) {
      double price = result.getValue(ComputerUtils.PRICE_ASPECT);
      double ship = result.getValue(ComputerUtils.SHIP_ASPECT);
      double cpu = result.getValue(ComputerUtils.CPU_ASPECT);
      double ram = result.getValue(ComputerUtils.RAM_ASPECT);

      System.out.println("Got a computer for $" + price + 
			 " shipping in " + (int)ship + " days with " + 
			 (int)cpu + " Mhz and " + 
			 (int)ram + " Mb");
    } else {
      System.out.println("No computer available");
    }
  }
}


