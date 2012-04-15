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

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

/**
 * Simple class to send messages to the background WebSyncClient
 */
class MessageSender extends Task {

   private final String message;
   private final String friendlyName;

   public MessageSender(Application app, String message, String friendlyName) {
      super(app);

      this.message = message;
      this.friendlyName = friendlyName;
   }

   @Override
   protected Object doInBackground() throws Exception {
    ControlFileService service = new ControlFileService();
	 service.sendMessage(this.message);

     return null;

   }

      /**
    * This is called when doInBackground throws an exception.
    *
    * @param   t  the exception thrown
    */
   @Override protected void failed(Throwable t) {
      setBadAlertMessage("failed");
   }

   /**
    * This is called when doInBackground is successfully executed.
    *
    * @param   o  the return value of doInBackground
    */
   @Override protected void succeeded(Object o) {
      setOkAlertMessage("succeeded");
   }

   protected void setOkAlertMessage(String message) {
      ResourceMap resourceMap = getResourceMap();
      String newMessage = null;
      if (resourceMap != null) {
      	 newMessage = resourceMap.getString(resourceName(message), this.friendlyName);
      }
      firePropertyChange("okAlertMessage", "", newMessage);
   }

   protected void setBadAlertMessage(String message) {
      ResourceMap resourceMap = getResourceMap();
      String newMessage = null;
      if (resourceMap != null) {
         newMessage = resourceMap.getString(resourceName(message), this.friendlyName);
      }
      firePropertyChange("badAlertMessage", "", newMessage);
   }


}
