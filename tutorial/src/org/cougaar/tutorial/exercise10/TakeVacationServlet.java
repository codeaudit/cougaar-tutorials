/*
 * <copyright>
 *  Copyright 2000-2003 BBNT Solutions, LLC
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
package org.cougaar.tutorial.exercise10;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.tutorial.assets.ProgrammerAsset;
import org.cougaar.util.UnaryPredicate;

/**
 * This Servlet assigns a vacation month to each ProgrammerAsset. It always
 * looks for the earliest scheduled (to a task) month for the vacation month. It
 * responds with text describing what it did.
 */
public class TakeVacationServlet
      extends BaseServletComponent
      implements BlackboardClient {
   // The domainService acts as a provider of domain factory services
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

   private BlackboardService blackboard;

   @Override
   protected String getPath() {
      return "/takeVacationServlet";
   }

   @Override
   protected Servlet createServlet() {
      // get the blackboard service
      blackboard = serviceBroker.getService(this, BlackboardService.class, null);
      if (blackboard == null) {
         throw new RuntimeException("Unable to obtain blackboard service");
      }

      // We could inline "MyServlet" here as an anonymous
      // inner-class (like HelloBaseServletComponent does). Instead,
      // we'll move it to a simple inner-class, which will make the
      // code a little easier to read.
      return new MyServlet();
   }

   public void setBlackboardService(BlackboardService blackboard) {
      this.blackboard = blackboard;
   }

   //
   // These are required when implementing a BlackboardClient:
   // A Component must implement BlackboardClient in order
   // to obtain BlackboardService.
   //

   // BlackboardClient method:
   public String getBlackboardClientName() {
      return toString();
   }

   // unused BlackboardClient method:
   public long currentTimeMillis() {
      return new Date().getTime();
   }

   // unused BlackboardClient method:
   public boolean triggerEvent(Object event) {
      return false;
   }

   @Override
   public void unload() {
      super.unload();
      // release the blackboard service
      if (blackboard != null) {
         serviceBroker.releaseService(this, BlackboardService.class, blackboard);
         blackboard = null;
      }
   }

   private class MyServlet
         extends HttpServlet {
      private static final long serialVersionUID = 1L;
      UnaryPredicate pred = new UnaryPredicate() {
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
            return o instanceof ProgrammerAsset;
         }
      };

      @Override
      public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
         execute(req, res);
      }

      @Override
      public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
         execute(req, res);
      }

      /**
       * Iterate over the list of programmers and have each take some vacation.
       * Print the programmers name and vacation month to the Servlet response.
       */
      public void execute(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
         PrintWriter out = res.getWriter();
         out.println("<html><head></head><body>");

         Collection col;
         try {
            ProgrammerAsset pa;
            blackboard.openTransaction();
            col = blackboard.query(pred);
            for (Iterator it = col.iterator(); it.hasNext();) {
               pa = (ProgrammerAsset) it.next();
               makeVacation(pa, out);
            }
         } finally {
            blackboard.closeTransactionDontReset();
            out.println("<BR>Done.</body></html>");
            out.flush();
         }

      }

      /**
       * Find and take a vacation month for this programmer. Print the vacation
       * month to the PrintStream
       */
      private void makeVacation(ProgrammerAsset pa, PrintWriter out) {

         // make a VACATION task
         PlanningFactory factory = (PlanningFactory) getDomainService().getFactory("planning");
         NewTask task = factory.newTask();
         task.setVerb(Verb.get("VACATION"));
         task.setPlan(factory.getRealityPlan());
         task.setDirectObject(pa);
         blackboard.publishAdd(task);

         // allocate it to next availble month of schedule that is not vacation
         Enumeration e = pa.getRoleSchedule().getRoleScheduleElements();
         while (e.hasMoreElements()) {
            Allocation alloc = (Allocation) e.nextElement();
            if (!alloc.getEstimatedResult().isSuccess()) {
               continue;
            }
            if (alloc.getTask().getVerb().toString() == "VACATION") {
               continue;
            }
            long start = alloc.getStartTime();
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(new Date(start));
            cal.add(Calendar.MONTH, 1);
            long end = cal.getTime().getTime();
            AllocationResult ar = new AllocationResult(1.0, true, new AspectValue[] {
               AspectValue.newAspectValue(AspectType.START_TIME, start),
               AspectValue.newAspectValue(AspectType.END_TIME, end)
            });
            Allocation alloc2 = factory.createAllocation(task.getPlan(), task, pa, ar, Role.ASSIGNED);
            blackboard.publishAdd(alloc2);
            System.out.println("Adding VACATION time for " + pa.getItemIdentificationPG().getItemIdentification() + " at time "
                  + new Date(start));
            break;
         }
      }

   }
}
