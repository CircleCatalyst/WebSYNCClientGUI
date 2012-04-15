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

import java.io.IOException;

import java.net.UnknownHostException;

import java.rmi.RemoteException;

import javax.swing.Icon;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

/**
 * Performs tasks which involve testing the background service.
 * 
 * @author  William Song
 * @version 1.0.2
 */
public class ServiceTester extends Task {
   
   protected Icon testIcon;
   protected Icon icon;
   protected String messageAffix;
   protected Object messageArgs;
   protected String testMessage;
   protected String okAlertMessage;
   protected String badAlertMessage;
   protected boolean isOk;
   
   public ServiceTester(Application app) {
      super(app);
   }

   /**
    * Executes code in a background thread.
    * Connects to the background service and performs a test of its WebSYNC connection.
    * 
    * @return  true if service connection is ok, false otherwise
    * @throws  java.lang.Exception  thrown if background service cannot be detected
    */
   @Override protected Boolean doInBackground() throws Exception {
      message("processTest", new Object[0]);
      setIcon("");
      setTestIcon("");
      setTestMessage("processTest", new Object[0]);
      ControlFileService service = new ControlFileService();
      Boolean ret = new Boolean(service.testConnection());
      
      WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
      LogWriter worker = app.logWriteService(true, "Testing service via GUI", "INFO");	  //assume service is up
      app.getContext().getTaskService().execute(worker);
      
      return ret;
   }
   
   /**
    * This is the called after failed() or succeeded().
    */
   @Override protected void finished() {
      if (messageAffix != null) {
	 message(messageAffix, messageArgs);
	 setTestMessage(messageAffix, messageArgs);

     firePropertyChange("testFinished", null, null);
     // needs to be before dialog boxes below, otherwise event goes to the dialog boxes

     if (isOk) {
	    setOkAlertMessage(messageAffix);
	 } else {
	    setBadAlertMessage(messageAffix, messageArgs);
	 }
      }
   }
   
   /**
    * This is called when doInBackground throws an exception.
    * 
    * @param   t  the exception thrown
    */
   @Override protected void failed(Throwable t) {
      if (t instanceof UnknownHostException) {
	 messageArgs = "DNS lookup failed";
      } else if (t instanceof IOException) {
	 messageArgs = "Network error: " + t.getMessage();
      } else if (t instanceof RemoteException) {
	 messageArgs = "Could not query background service";
      }
      messageAffix = "failedTest";
      setTestIcon("badIcon");
      setIcon("badIconSmall");
      isOk = false;
//      setBadAlertMessage(messageAffix, messageArgs);
   }
   
   /**
    * This is called when doInBackground is successfully executed.
    * 
    * @param   o  the return value of doInBackground
    */
   @Override protected void succeeded(Object o) {
      boolean testOk = ((Boolean)o).booleanValue();
      if (testOk) {
	 messageAffix = "succeededTest";
	 setTestIcon("okIcon");
	 setIcon("okIconSmall");
	 isOk = true;
//	 setOkAlertMessage(messageAffix);
      } else {
	 // means the InetAddress.isReachable() failed
	 messageAffix = "failedTest";
	 messageArgs = "Network error";
	 setTestIcon("badIcon");
	 setIcon("badIconSmall");
	 isOk = false;
//	 setBadAlertMessage(messageAffix, messageArgs);
      }
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
   
   protected void setTestIcon(String iconName) {
      ResourceMap resourceMap = getResourceMap();
      Icon newIcon = null;
      if (resourceMap != null) {
	 newIcon = resourceMap.getIcon(resourceName(iconName));
      } 
      Icon oldIcon;
      synchronized(this) {
	 oldIcon = testIcon;
	 testIcon = newIcon;
	 newIcon = testIcon;
      }      
      firePropertyChange("testIcon", oldIcon, newIcon);
   }
   
   protected void setTestMessage(String message, Object arg) {
      ResourceMap resourceMap = getResourceMap();
      String newMessage = null;
      if (resourceMap != null) {
	 newMessage = resourceMap.getString(resourceName(message), arg);
      } 
      String oldMessage;
      synchronized(this) {
	 oldMessage = testMessage;
	 testMessage = newMessage;
	 newMessage = testMessage;
      }  
      firePropertyChange("testMessage", oldMessage, newMessage);
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
