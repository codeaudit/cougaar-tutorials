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
package org.cougaar.tutorial.faststart;

import java.util.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;

/**
 * Plugin to facilitate simple hooking up of agents 
 * based on identities, roles and relationships
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: TutorialHookupPlugin.java,v 1.8 2003-04-08 17:43:27 dmontana Exp $
 */
public class TutorialHookupPlugin extends ComponentPlugin
{

    private DomainService domainService;
    private PlanningFactory ldmf;
    public void setDomainService(DomainService value) {
	domainService=value;
    }
    public DomainService getDomainService() {
	return domainService;
    }

  // At setup time, establish info from parameters
  // Then make copies of all referenced agents in logplan, 
  // and copy self to referenced agents
  public void setupSubscriptions() 
  {
    parseParameters();
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    // Create 'SELF' and put in my log plan
    String my_agent_name = getAgentIdentifier().getAddress();
    Role []my_self_roles = {};
    Organization my_organization = 
      createOrganization(my_agent_name, my_self_roles, Organization.SELF_RELATIONSHIP);
    getBlackboardService().publishAdd(my_organization);

    // Go over all agents specified in command line
    for(int i = 0; i < my_related_agents.size(); i++) {

      // Put each specified agents in my logplan
      String my_related_agent_name = (String)my_related_agents.elementAt(i);
      String my_relationship = (String)my_relationships.elementAt(i);
      Role my_related_role = Role.getRole((String)my_related_roles.elementAt(i));
      Role []my_related_roles = {my_related_role};
      Organization my_related_agent =
         createOrganization(my_related_agent_name, my_related_roles,
			   my_relationship);
      getBlackboardService().publishAdd(my_related_agent);

      // And copy self to all specified related agents
      Organization my_copy =
         createOrganization(my_agent_name, my_related_roles,
			   convertRelationship(my_relationship));
      copyAssetToCluster(my_copy, my_related_agent);
    }
  }

  // Flip sense of symmetric relationship
  // for shipping to other agent
  private String convertRelationship(String relationship) {
    if (relationship.equals(Organization.SUPPLIER_RELATIONSHIP))
      return Organization.CUSTOMER_RELATIONSHIP;
    else if (relationship.equals(Organization.CUSTOMER_RELATIONSHIP))
      return Organization.SUPPLIER_RELATIONSHIP;
    else
      return relationship;
  }

  // This plugin only need setup code
  public void execute()
  {
  }

  /**
   * Create an organization asset with given name,
   * given sets of roles and relationship (to reference organization)
   */
  private Organization createOrganization(String name, Role []roles, String relationship)
  {
    Organization org = (Organization)ldmf
	.createAsset(org.cougaar.tutorial.faststart.Organization.class);
    NewClusterPG cpg = (NewClusterPG)ldmf
	.createPropertyGroup(ClusterPGImpl.class);
    cpg.setMessageAddress(MessageAddress.getMessageAddress(name));
    org.setClusterPG(cpg);

    NewTypeIdentificationPG tipg = (NewTypeIdentificationPG)ldmf
	.createPropertyGroup("TypeIdentificationPG");
    tipg.setTypeIdentification(name);
    org.setTypeIdentificationPG(tipg);

    NewItemIdentificationPG iipg = (NewItemIdentificationPG)ldmf
	.createPropertyGroup("ItemIdentificationPG");
    iipg.setItemIdentification(name);
    org.setItemIdentificationPG(iipg);


    org.setRoles(roles);
    org.setRelationship(relationship);

    // set up this asset's available schedule
    Date start = TutorialUtils.createDate(1990, 1, 1);
    Date end = TutorialUtils.createDate(2010, 1, 1);
    NewSchedule avail = ldmf.newSimpleSchedule(start, end);
    ((NewRoleSchedule)org.getRoleSchedule()).setAvailableSchedule(avail);

    return org;
  }

  /**
   * Copy the given asset to the given agent given by name
   * by creating and publishing an asset transfer
   **/
  private void copyAssetToCluster(Asset the_asset, Asset the_receiving_asset)
  {
    // Why do I need a task and schedule element??
    NewTask task = ldmf.newTask();
    task.setVerb(Verb.get("Dummy"));
    task.setParentTask(task);

    //This makes asset available to the agent from 01/01/1990 tp 
    //01/01/2010
    NewSchedule schedule = 
      ldmf.newSimpleSchedule(TutorialUtils.createDate(1990, 1, 1),
                                TutorialUtils.createDate(2010, 1, 1));

    AspectValue avs[] = new AspectValue[2];
    avs[0] = AspectValue.newAspectValue(AspectType.START_TIME, schedule.getStartTime());
    avs[1] = AspectValue.newAspectValue(AspectType.END_TIME, schedule.getEndTime());
    AllocationResult est_ar = ldmf
	.newAllocationResult(1.0, true, avs);


    AssetTransfer asset_transfer =
      ldmf.createAssetTransfer(ldmf
							  .getRealityPlan(), // plan
                                  task, // task (dummy)
                                  the_asset,
                                  schedule,  // schedule(dummy)
                                  ldmf.cloneInstance(the_receiving_asset), // to_agent
                                  est_ar, // estimated_result
				  Role.AVAILABLE    // role
				  );

    getBlackboardService().publishAdd(task);           // This is necessary or else the asset transfer gets rescinded
    getBlackboardService().publishAdd(asset_transfer);
  }

  /**
   * Parse arguments, related agent , then relationship, then roles
   * All separated by slashes, e.g. BossAgent/Superior/TransporationProvider
   */
  private void parseParameters()
  {
    for(Iterator it= getParameters().iterator();it.hasNext();) {
      String combined = (String)it.next();
      int index1 = combined.indexOf('/');
      int index2 = combined.indexOf('/', index1+1);
      String agent_name = combined.substring(0, index1);
      String relationship = combined.substring(index1+1, index2);
      String role = combined.substring(index2+1);
      my_related_agents.addElement(agent_name);
      my_relationships.addElement(relationship);
      my_related_roles.addElement(role);

      // System.out.println("Agent = " + agent_name +
      //     " relationship = " + relationship + " role = " + role);
    }

  }

  // Private storage for the information about related agents
  private Vector my_related_agents = new Vector();
  private Vector my_relationships = new Vector();
  private Vector my_related_roles = new Vector();



}


