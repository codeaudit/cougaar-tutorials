/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.pizza.plugin;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.util.Configuration;


/**
 * Read local agent OWL profile file. Use the listed roles and register this agent with those
 * roles in the YP.
 **/
public class SDRegistrationPlugin extends ComponentPlugin {
  private static int WARNING_SUPPRESSION_INTERVAL = 5;
  private long warningCutoffTime = 0;
  private static final String REGISTRATION_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.pizza.plugin.RegistrationGracePeriod";

  protected static final String OWL_IDENTIFIER = ".profile.owl";

  private Alarm retryAlarm;

  private LoggingService log;
  private RegistrationService registrationService = null;
  private CommunityService communityService = null;

  private YPInfo ypInfo;

  private ProviderDescription provD = null;


  public void setCommunityService(CommunityService cs) { 
    this.communityService = cs; 
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setRegistrationService(RegistrationService rs) {
    registrationService = rs;
  }

  public void suspend() {
    super.suspend();

    if (ypInfo != null) {
      // Remove all community change notifications
      if (log.isInfoEnabled()) {
	log.info(getAgentIdentifier() + " removing community change listeners.");
      }
      
      
      ypInfo.clearCommunity();
    }
  }

  public void unload() {
    if (registrationService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         RegistrationService.class,
                                                         registrationService);
      registrationService = null;
    }

    if ((log != null) && (log != LoggingService.NULL)) {
      getBindingSite().getServiceBroker().releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
  }

  protected void execute () {
    if (isProvider()) {
      if (ypInfo == null) {
	initYPInfo();
	handleYP();
      }

      if (ypInfo.readyToRegister()) {
	if (log.isDebugEnabled()) {
	  log.debug("Registering: " + getAgentIdentifier() + " with " +
		    ypInfo.getCommunity());
	}
	initialRegister();
      }
    }
  }

  private void initialRegister() {
    if (isProvider()) {
      if (!ypInfo.readyToRegister()) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + "Exiting initialRegister early - " +
		    " ypInfo not ready - " + 
		    " community " + ypInfo.getCommunity() +
		    " isRegistered " + ypInfo.getIsRegistered() +
		    " pendingRegistration " + ypInfo.getPendingRegistration() +
		    " isDeleted " + ypInfo.getIsDeleted());
	}
	return;
      }


      try {
        final ProviderDescription pd = getPD();

	if (pd == null) {
	  ypInfo.setIsRegistered(false);	
          ypInfo.setPendingRegistration(false); // okay to try again
	  
	  retryErrorLog("Problem getting ProviderDescription." + 
			" Unable to add registration to " +
			ypInfo.getCommunity().getName() + 
			", try again later.");

	  return;
	}

	ypInfo.setPendingRegistration(true);

	RegistrationService.Callback cb =
	  new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    boolean success = ((Boolean) o).booleanValue();
	    if (log.isInfoEnabled()) {
	      log.info(pd.getProviderName()+ " initialRegister success = " + 
		       success + " with " + ypInfo.getCommunity().getName());
	    }
	    
            ypInfo.setIsRegistered(true);
            ypInfo.setPendingRegistration(false);
	    ypInfo.clearCommunity();


	    retryAlarm = null;

	    org.cougaar.core.service.BlackboardService bbs = 
	      getBlackboardService();
	    if (bbs != null) { 
	      bbs.signalClientActivity();
	    }
	  }
          public void handle(Exception e) {
            ypInfo.setPendingRegistration(false); // okay to try again
            ypInfo.setIsRegistered(false);

	    retryErrorLog("Problem adding ProviderDescription to " + 
			  ypInfo.getCommunity().getName() + 
			  ", try again later: " +
			  getAgentIdentifier(), e);
          }
        };
	
	// actually submit the request.
        registrationService.addProviderDescription(ypInfo.getCommunity(),
						   pd,
						   cb);
      } catch (RuntimeException e) {
	  ypInfo.setIsRegistered(false);	
          ypInfo.setPendingRegistration(false); // okay to try again
	  
	  retryErrorLog("Problem adding ProviderDescription to " + 
			ypInfo.getCommunity().getName() + 
			", try again later: " +
			getAgentIdentifier(), e);
      }
    }
  }

  private boolean registrationComplete() {
    if (!isProvider()) {
      return true;
    } else {
      return ypInfo.getIsRegistered();
    }
  }

  private void handleYP() {
    Community ypCommunity = 
      communityService.getCommunity(getYPCommunityName(ypInfo.getAgentName()),
				    new YPCommunityResponseListener(ypInfo));
    
    if (ypCommunity != null) {
      ypInfo.setCommunity(ypCommunity);
      if (log.isDebugEnabled()) {
	log.debug("Registering: " + getAgentIdentifier() + " with " +
		  ypInfo.getCommunity());
      }
      initialRegister();
    } else if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + " waiting on community info " +
		getYPCommunityName(ypInfo.getAgentName()));
    }
  }

  private void initYPInfo() {
    Collection params = getParameters();
    
    if (params.isEmpty()) {
      IllegalArgumentException iae = new IllegalArgumentException();
      log.error(getAgentIdentifier() + 
		"SDRegistrationPlugin: no YP agent parameter" +
		" - unable to register.", iae);
    } else {
      ypInfo = new YPInfo((String) params.iterator().next(),
			    null, false, false, false);
    }

    System.out.println(getAgentIdentifier() + ": ypInfo = " + ypInfo);
  }

  private String getYPCommunityName(String ypAgentName) {
    // For now assume every YP represented by a YPCommunity called
    // <yp agent name>-YPCOMMUNITY
    return ypAgentName + "-YPCOMMUNITY";
  }
 
  
  /* ProviderDescription is big - release resources if we don't need it
   * anymore.
   */
  private  void clearPD() {
    if ((provD != null) && 
	(log.isDebugEnabled())) {
      log.debug(getAgentIdentifier() + ": clearPD()");
    }
    
    
    provD = null;
  }

  /* Returns null if unable to parse the provider description */
  private  ProviderDescription getPD() {
    if (provD == null) {
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + ": getPD() parsing OWL.");
      }
      
      ProviderDescription pd = new ProviderDescriptionImpl();
      try {
	URL serviceProfileURL = 
	  Configuration.urlify(Constants.getDataPath() + File.separator + 
			       "serviceprofiles");
	boolean ok = pd.parseOWL(serviceProfileURL, 
				 getAgentIdentifier() + OWL_IDENTIFIER);
	
	if (ok && (pd.getProviderName() != null)) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      ": getPD() successfully parsed OWL.");
	  }
	  
	  provD = pd;
	} else {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      ": getPD() unable to parse OWL." +
		      " ok = " + ok);
	  }
	}
      } catch (java.util.ConcurrentModificationException cme) {
	// Jena can do a concurrent mod exception. See bug 3052
	// Leave provD uninitialized
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    ": getPD() ConcurrentModificationException - " +
		    cme);
	}
      } catch (java.net.MalformedURLException mue) {
	log.error(getAgentIdentifier() +
		  ": getPD() unable to construct a URL from " + 
		  Constants.getDataPath() + File.separator + 
		  "serviceprofiles", mue);
      }
    }
    return provD;
  }
  
  private long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(REGISTRATION_GRACE_PERIOD_PROPERTY,
							WARNING_SUPPRESSION_INTERVAL).intValue();
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }
    
    return warningCutoffTime;
  }
  
  
  private void resetWarningCutoffTime() {
    warningCutoffTime = -1;
  }

  private void retryErrorLog(String message) {
    retryErrorLog(message, null);
  }
  
  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  private void retryErrorLog(String message, Throwable e) {
    
    long absTime = getAlarmService().currentTimeMillis()+ 
      (int)(Math.random()*10000) + 1000;
    
    retryAlarm = new RetryAlarm(absTime);
    getAlarmService().addAlarm(retryAlarm);
    
    if(System.currentTimeMillis() > getWarningCutOffTime()) {
      if (e == null)
	log.error(getAgentIdentifier() + message);
      else
	log.error(getAgentIdentifier() + message, e);
    } else if (log.isDebugEnabled()) {
      if (e == null)
	log.debug(getAgentIdentifier() + message);
      else
	log.debug(getAgentIdentifier() + message, e);
    }
  }

  // Is this Agent a service provider?
  private boolean isProvider() {
    return getProviderFile().exists();
  }
  
  // Get the OWL service provider file
  private File getProviderFile() {
    String owlFileName = getAgentIdentifier().toString() + OWL_IDENTIFIER;
    return new File(getServiceProfileURL().getFile() +
		    owlFileName);
  }

  private URL getServiceProfileURL() {
    try {
      return new URL(Configuration.getInstallURL(), 
		     "pizza/data/serviceprofiles/");
    } catch (java.net.MalformedURLException mue) {
      log.error("Exception constructing service profile URL: " , mue);
      return null;
    }
  }
  
  private class RetryAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;

    public RetryAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }

    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<RetryAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for SDCommunityBasedRegistrationPlugin at " + 
	getAgentIdentifier() + ">";
    }
  }   

  private class YPInfo {
    private String myYPAgentName;
    private Community myYPCommunity;
    private YPCommunityChangeListener myCommunityListener;
    private boolean myIsRegistered;
    private boolean myIsDeleted;
    private boolean myPendingRegistration;
    
    public YPInfo(String ypAgentName, Community ypCommunity, 
		  boolean isRegistered, boolean pendingRegistration, 
		  boolean isDeleted) {
      myYPAgentName = ypAgentName;
      myYPCommunity = ypCommunity;
      myIsRegistered = isRegistered;
      myPendingRegistration = pendingRegistration;
      myIsDeleted = isDeleted;
    }

    public String getAgentName(){
      return myYPAgentName;
    }

    public void setAgentName(String ypAgentName){
      if (ypAgentName == null) {
	clearCommunity();
      } else {
	myYPAgentName = ypAgentName;
      }
    }

    public Community getCommunity(){
      return myYPCommunity;
    }

    public void setCommunity(Community ypCommunity){
      if (ypCommunity == null) {
	clearCommunity();
      } else {
	if (myYPCommunity == null) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " adding listener for " + 
		      ypCommunity);
	  }
	  myYPCommunity = ypCommunity;
	
	  // First time so set up change listener
	  myCommunityListener = new YPCommunityChangeListener(this);
	  communityService.addListener(myCommunityListener);
	} else {
	  myYPCommunity = ypCommunity;
	}
      }
    }


    public void clearCommunity() {
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " removing listener for " + myYPCommunity);
      }
      myYPCommunity = null;
      if (myCommunityListener != null) {
	communityService.removeListener(myCommunityListener);
      }
    }

    public boolean getIsRegistered() {
      return myIsRegistered;
    }

    public void setIsRegistered(boolean isRegistered){ 
      if ((myIsRegistered) && (!isRegistered) &&
	  (log.isDebugEnabled())) {
	RuntimeException re  = new RuntimeException();
	log.debug(getAgentIdentifier() + 
		  " setIsRegistered() going from true to false.", re);
      }
      myIsRegistered = isRegistered;
    }

    public boolean getPendingRegistration() {
      return myPendingRegistration;
    }

    public void setPendingRegistration(boolean pendingRegistration){ 
      myPendingRegistration = pendingRegistration;
    }

    public boolean getIsDeleted() {
      return myIsDeleted;
    }

    public void setIsDeleted(boolean isDeleted){ 
      myIsDeleted = isDeleted;
    }

    public boolean readyToRegister() {
      return ((getCommunity() != null) &&
	      (!getIsRegistered()) &&
	      (!getPendingRegistration()) &&
              (!getIsDeleted()));
    }
  }

  private class YPCommunityResponseListener 
  implements CommunityResponseListener {
    private YPInfo ypInfo;

    public YPCommunityResponseListener(YPInfo info) {
      ypInfo = info;
    }

    public void getResponse(CommunityResponse resp){
      System.out.println(getAgentIdentifier() + ": got community info for " +
			 (Community) resp.getContent());
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " got Community info for " +
		  (Community) resp.getContent());
      }

      Community ypCommunity = (Community) resp.getContent();

      ypInfo.setCommunity(ypCommunity);
      {
        org.cougaar.core.service.BlackboardService bbs = getBlackboardService();
        if (bbs != null) bbs.signalClientActivity();
      }
    }
  }

  private class YPCommunityChangeListener 
  implements CommunityChangeListener {
    private YPInfo ypInfo;
    String communityName;

    public YPCommunityChangeListener(YPInfo info) {
      ypInfo = info;
      communityName = ypInfo.getCommunity().getName();
    }

    public void communityChanged(CommunityChangeEvent event){
      Community ypCommunity = event.getCommunity();

      // Paranoia code - bug in community code seems to lead to
      // notifications with null communities.
      if (ypCommunity == null) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    " received Community change info for a null community");
	}
	return;
      }

      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " got Community change info for " +
		  ypCommunity);
      }

      if (ypCommunity == null) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    " received Community change info for a null community");
	}
	return;
      }

      if (ypCommunity.getName().equals(getCommunityName())) {
	ypInfo.setCommunity(ypCommunity);

	if (ypInfo.readyToRegister()) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " signalClientActivity for " + 
		      ypCommunity);
	  }
	  
	  if (getBlackboardService() == null) {
	    log.warn(getAgentIdentifier() + " ignoring change notification " +
		     " - getBlackboardService() returned null");
	    ypInfo.clearCommunity();
	  } else {
            org.cougaar.core.service.BlackboardService bbs = getBlackboardService();
            if (bbs != null) bbs.signalClientActivity();
	  }
	}
      } else if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + 
		  " ignoring CommunityChangeEvent  for " + 
		  ypCommunity.getName() + 
		  " - listening for - " + getCommunityName());
      }

    }

    public String getCommunityName() {
      return communityName;
    }
  }
}


