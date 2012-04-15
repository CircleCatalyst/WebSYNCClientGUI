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

import java.io.EOFException;

import java.rmi.Naming;
import java.rmi.UnmarshalException;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

/**
 * Performs tasks which involve restarting the background service.
 * 
 * @author  William Song
 * @version 1.0.2
 */
public class ServiceRestarter extends Task {
   /**
    * The message to retrieve from the ResourceMap.
    */
   protected String messageAffix;
   /**
    * The arguments to pass to the message (set to null if none).
    */
   protected Object messageArgs;
   protected String okAlertMessage;
   protected String badAlertMessage;
   
   /**
    * Constructor.
    * 
    * @param   app   the parent application EDT which calls this Task
    */
   public ServiceRestarter(Application app) {
      super(app);
   }

   /**
    * Executes code in a background thread.
    * Connects to the background service and attempts to restart it.
    * 
    * @return  null if restart successful, or exception thrown
    * @throws  java.lang.Exception  thrown if background service cannot be detected or restarted
    */
   @Override protected Object doInBackground() throws Exception {
      message("processRestart", new Object[0]);
      ControlFileService service = new ControlFileService();
      try {
	 service.restart();   
      } catch (UnmarshalException e) {
	 if (e.getCause() instanceof EOFException) {
	    // restart in progress
	 }
      }
      
      WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
      LogWriter worker = app.logWriteService(true, "Attempting to restart WebSYNC", "INFO");	  //assume service is up
      app.getContext().getTaskService().execute(worker);
      
      return null;
   }
   
   /**
    * This is called when doInBackground throws an exception.
    * 
    * @param   t  the exception thrown
    */
   @Override protected void failed(Throwable t) {
      messageArgs = "Could not query WebSYNC";
      messageAffix = "failedRestart";
      setBadAlertMessage(messageAffix, messageArgs);
   }
   
   /**
    * This is called when doInBackground is successfully executed.
    * 
    * @param   o  the return value of doInBackground
    */
   @Override protected void succeeded(Object o) {      
      messageAffix = "succeededRestart";
      setOkAlertMessage(messageAffix);
   }
   
   protected void setOkAlertMessage(String message) {
      ResourceMap resourceMap = getResourceMap();
      String newMessage = null;
      if (resourceMap != null) {
	 newMessage = resourceMap.getString(resourceName(message), new Object[0]);
      }
      String oldMessage;
      synchronized(this) {
	 oldMessage = okAlertMessage;
	 okAlertMessage = newMessage;
	 newMessage = okAlertMessage;
      }
      firePropertyChange("okAlertMessage", oldMessage, newMessage);
   }
   
   protected void setBadAlertMessage(String message, Object reason) {
      ResourceMap resourceMap = getResourceMap();
      String newMessage = null;
      if (resourceMap != null) {
	 newMessage = resourceMap.getString(resourceName(message), reason);
      }
      String oldMessage;
      synchronized(this) {
	 oldMessage = badAlertMessage;
	 badAlertMessage = newMessage;
	 newMessage = badAlertMessage;
      }
      firePropertyChange("badAlertMessage", oldMessage, newMessage);
   }
}
