package org.cougaar.tutorial.faststart;

import java.util.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.core.cluster.ClusterIdentifier;

/**
 * Plugin to facilitate simple hooking up of clusters 
 * based on identities, roles and relationships
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: TutorialHookupPlugIn.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 */
public class TutorialHookupPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{

  // At setup time, establish info from parameters
  // Then make copies of all referenced clusters in logplan, 
  // and copy self to referenced clusters
  public void setupSubscriptions() 
  {
    parseParameters();

    // Create 'SELF' and put in my log plan
    String my_cluster_name = getClusterIdentifier().getAddress();
    Role []my_self_roles = {};
    Organization my_organization = 
      createOrganization(my_cluster_name, my_self_roles, Organization.SELF_RELATIONSHIP);
    publishAdd(my_organization);

    // Go over all clusters specified in command line
    for(int i = 0; i < my_related_clusters.size(); i++) {

      // Put each specified clusters in my logplan
      String my_related_cluster_name = (String)my_related_clusters.elementAt(i);
      String my_relationship = (String)my_relationships.elementAt(i);
      Role my_related_role = Role.getRole((String)my_related_roles.elementAt(i));
      Role []my_related_roles = {my_related_role};
      Organization my_related_cluster =
         createOrganization(my_related_cluster_name, my_related_roles,
			   my_relationship);
      publishAdd(my_related_cluster);

      // And copy self to all specified related clusters
      Organization my_copy =
         createOrganization(my_cluster_name, my_related_roles,
			   convertRelationship(my_relationship));
      copyAssetToCluster(my_copy, my_related_cluster);
    }
  }

  // Flip sense of symmetric relationship
  // for shipping to other cluster
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
    Organization org = (Organization)theLDMF.createAsset(org.cougaar.tutorial.faststart.Organization.class);
    NewClusterPG cpg = (NewClusterPG)theLDMF.createPropertyGroup(ClusterPGImpl.class);
    cpg.setClusterIdentifier(new ClusterIdentifier(name));
    org.setClusterPG(cpg);

    NewTypeIdentificationPG tipg = (NewTypeIdentificationPG)theLDMF.createPropertyGroup("TypeIdentificationPG");
    tipg.setTypeIdentification(name);
    org.setTypeIdentificationPG(tipg);

    NewItemIdentificationPG iipg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
    iipg.setItemIdentification(name);
    org.setItemIdentificationPG(iipg);


    org.setRoles(roles);
    org.setRelationship(relationship);

    // set up this asset's available schedule
    Date start = TutorialUtils.createDate(1990, 1, 1);
    Date end = TutorialUtils.createDate(2010, 1, 1);
    NewSchedule avail = theLDMF.newSimpleSchedule(start, end);
    ((NewRoleSchedule)org.getRoleSchedule()).setAvailableSchedule(avail);

    return org;
  }

  /**
   * Copy the given asset to the given cluster given by name
   * by creating and publishing an asset transfer
   **/
  private void copyAssetToCluster(Asset the_asset, Asset the_receiving_asset)
  {
    // Why do I need a task and schedule element??
    NewTask task = theLDMF.newTask();
    task.setVerb(new Verb("Dummy"));
    task.setParentTask(task);

    //This makes asset available to the cluster from 01/01/1990 tp 
    //01/01/2010
    NewSchedule schedule = 
      theLDMF.newSimpleSchedule(TutorialUtils.createDate(1990, 1, 1),
                                TutorialUtils.createDate(2010, 1, 1));

    int [] aspects = {AspectType.START_TIME, AspectType.END_TIME};
    double [] results = {schedule.getStartTime(), schedule.getEndTime()};
    AllocationResult est_ar = theLDMF.newAllocationResult(1.0, true, aspects, results);


    AssetTransfer asset_transfer =
      theLDMF.createAssetTransfer(theLDMF.getRealityPlan(), // plan
                                  task, // task (dummy)
                                  the_asset,
                                  schedule,  // schedule(dummy)
                                  the_receiving_asset, // to_cluster
                                  est_ar, // estimated_result
				  Role.AVAILABLE    // role
				  );

    publishAdd(task);           // This is necessary or else the asset transfer gets rescinded
    publishAdd(asset_transfer);
  }

  /**
   * Parse arguments, related cluster , then relationship, then roles
   * All separated by slashes, e.g. BossCluster/Superior/TransporationProvider
   */
  private void parseParameters()
  {
    for(Enumeration e = getParameters().elements();e.hasMoreElements();) {
      String combined = (String)e.nextElement();
      int index1 = combined.indexOf('/');
      int index2 = combined.indexOf('/', index1+1);
      String cluster_name = combined.substring(0, index1);
      String relationship = combined.substring(index1+1, index2);
      String role = combined.substring(index2+1);
      my_related_clusters.addElement(cluster_name);
      my_relationships.addElement(relationship);
      my_related_roles.addElement(role);

      // System.out.println("Cluster = " + cluster_name +
      //     " relationship = " + relationship + " role = " + role);
    }

  }

  // Private storage for the information about related clusters
  private Vector my_related_clusters = new Vector();
  private Vector my_relationships = new Vector();
  private Vector my_related_roles = new Vector();



}


