/**
 * WebSYNC Client Copyright 2007, 2008 Dataview Ltd
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * A copy of the GNU General Public License version 3 is included with this 
 * source distribution. Alternatively this licence can be viewed at 
 * <http://www.gnu.org/licenses/>
 */
package nz.dataview.websyncclientgui;

import java.util.*;
import javax.swing.Icon;

import org.apache.log4j.Logger;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;
import java.text.SimpleDateFormat;


/**
 * Performs tasks which involve communicating with the background service.
 * 
 * @author  William Song
 * @version 1.0.2
 */
public class ServiceConnector extends Task {
   /**
    * The message to retrieve from the ResourceMap.
    */
   protected String messageAffix;
   /**
    * The arguments to pass to the message (set to null if none).
    */
   protected Object messageArgs;
   /**
    * The icon to use.
    */
   protected Icon icon;
   /**
    * The large icon to use on the connect panel.
    */
   protected Icon largeIcon;
   /**
    * The string to use on the connect panel overview.
    */
   protected String largeMessage;
   /**
    * The string to use on the connect panel details.
    */
   protected String detailMessage;
   /**
    * Determines whether the background service is running or sleeping.
    */
   protected boolean isRunning;
   /**
    * Determines whether the background service is reachable.
    */
   protected boolean isUp;
   protected boolean isNowRunning;
   protected boolean isNowUp;
   protected String isAlive;
	protected HashMap Status;

   private static Logger logger = Logger.getLogger(ServiceConnector.class);
   
   /**
    * Constructor.
    * 
    * @param   app		 the parent application EDT which calls this Task
    * @param   isAlreadyRunning	 whether the background service is running at the moment or not
    * @param   isUp		 whether the background service is reachable or not
    */
   public ServiceConnector(Application app, boolean isAlreadyRunning, boolean isUp) {
		super(app);
      isRunning = isAlreadyRunning;
      this.isUp = isUp;
   }
   
   /**
    * Executes code in a background thread.
    * Connects to the background service and determines whether it is running or not.
    * 
    * @return  true if service is running, false if service is sleeping (exception thrown if service not detected)
    * @throws  java.lang.Exception  thrown if background service cannot be detected
    */
   @Override protected Boolean doInBackground() throws Exception {
      message("processConnect", new Object[0]);
      setLargeMessage("Detecting WebSYNC...");
      setDetailMessage("");
      setIcon("");
      setLargeIcon("");
      logger.debug("About to lookup");
      ControlFileService service=null;
      service = new ControlFileService();
 
      logger.debug("Finished lookup; about to contact service");
      Boolean ret=false;
      isNowRunning = service.isRunning();
      isAlive = service.isAlive();
		isNowUp= isAlive.equals("alive");
		Status=service.getStatus();
      logger.debug(" Finished service, returned with "+ isNowRunning+","+isAlive);
   
      return ret;
   }
   
   /**
    * This is the called after failed() or succeeded().
    */
   @Override protected void finished() {
//      if (messageAffix != null) {
//	 message(messageAffix, messageArgs);
//      }
   }
   
   /**
    * This is called when doInBackground throws an exception.
	 * Should never happen.
    * 
    * @param   t  the exception thrown
    */
   @Override protected void failed(Throwable t) {
      setIsUp(false);
      setIsRunning(false);
      setIcon("badIcon");
      setLargeIcon("badIconLarge");
      setLargeMessage("Could not detect WebSYNC");
      setDetailMessage("Error while connecting: "+t.getLocalizedMessage());
   }
   
   /**
    * This is called when doInBackground is successfully executed.
    * 
    * @param   o  the return value of doInBackground
    */
   @Override protected void succeeded(Object o) {
      String iconName = "";
      String newLargeMessage = "";
		String NewDetailMessage = "";

      setIsUp(isNowUp);
      setIsRunning(isNowRunning);
		if(isNowUp)
		{
			if (isNowRunning) {
				 newLargeMessage = "WebSYNC is running";
				 iconName = "okIcon";
				 NewDetailMessage = "";
			} else {
				 newLargeMessage = "WebSYNC is sleeping";
				 iconName = "sleepIcon";
				 NewDetailMessage = "";
			}
		} else
		{
			iconName = "badIcon";
			if(isAlive.equals("dead"))
			{
				newLargeMessage="WebSYNC has stopped";
				NewDetailMessage = "The service appears to have stopped";
			} else
			{
				newLargeMessage="WebSYNC has not yet started";
				NewDetailMessage = "Make sure that WebSYNC has been configured to run";
			}
		}
		NewDetailMessage="<html>"+NewDetailMessage;

		Date date=new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
		try{
			String websyncStatus=((HashMap)Status.get("websyncStatus")).get("commentary").toString();
			if(!websyncStatus.equals("")) {
				date.setTime((long)Integer.parseInt(((HashMap)Status.get("websyncStatus")).get("date").toString())*1000);
				if(NewDetailMessage.equals("")) NewDetailMessage+= "<hr>";
				NewDetailMessage+= "<p>"+"Last WebSYNC status summary, as at "+
				formatter.format(date)+
			   "</p><p>"+websyncStatus+"</p>";
			}
		} catch(Exception e){}

		try{
			String knStatus=((HashMap)Status.get("knStatus")).get("commentary").toString();
			if(!knStatus.equals("")) {
				date.setTime((long)Integer.parseInt(((HashMap)Status.get("knStatus")).get("date").toString())*1000);
				NewDetailMessage+= "<hr><p>"+"Last LMS status summary, as at "+
				formatter.format(date)+
			   "</p><p>"+ knStatus+"</p>";
			}
		} catch(Exception e){}
		NewDetailMessage+="</html>";

      setIcon(iconName);
      setLargeIcon(iconName + "Large");
      setLargeMessage(newLargeMessage);
      setDetailMessage(NewDetailMessage);
   }
   
   protected void setIcon(String iconName) {
      ResourceMap resourceMap = getResourceMap();
      Icon newIcon = null;
      if (resourceMap != null) {
	 newIcon = resourceMap.getIcon(resourceName(iconName));
      } 
      Icon oldIcon;
      synchronized(this) {
	 oldIcon = this.icon;
	 this.icon = newIcon;
	 newIcon = this.icon;
      }
      firePropertyChange("icon", oldIcon, newIcon);
   }
   
   protected void setLargeIcon(String iconName) {
      ResourceMap resourceMap = getResourceMap();
      Icon newIcon = null;
      if (resourceMap != null) {
	 newIcon = resourceMap.getIcon(resourceName(iconName));
      } 
      Icon oldIcon;
      synchronized(this) {
	 oldIcon = largeIcon;
	 largeIcon = newIcon;
	 newIcon = largeIcon;
      }
      firePropertyChange("largeIcon", oldIcon, newIcon);
   }
   
   protected void setLargeMessage(String newMessage) {
      String oldMessage;
      synchronized(this) {
		 oldMessage = largeMessage;
		 largeMessage = newMessage;
		 newMessage = largeMessage;
      }
      firePropertyChange("largeMessage", oldMessage, newMessage);
   }
   
   protected void setDetailMessage(String newMessage) {
      String oldMessage;
      synchronized(this) {
			 oldMessage = detailMessage;
			 detailMessage = newMessage;
			 newMessage = detailMessage;
      }
      firePropertyChange("detailMessage", oldMessage, newMessage);
   }
   
   protected void setIsRunning(boolean running) {
      boolean oldRunning, newRunning;
      synchronized(this) {
	 oldRunning = isRunning;
	 isRunning = running;
	 newRunning = isRunning;
      }
      firePropertyChange("isRunning", new Boolean(oldRunning), new Boolean(newRunning));
   }
   
   protected void setIsUp(boolean up) {
      boolean oldUp, newUp;
      synchronized(this) {
	 oldUp = isUp;
	 isUp = up;
	 newUp = isUp;
      }
      firePropertyChange("isUp", new Boolean(oldUp), new Boolean(newUp));
   }
}
