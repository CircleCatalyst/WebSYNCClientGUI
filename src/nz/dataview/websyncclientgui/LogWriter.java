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

import java.rmi.Naming;

import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 * Performs tasks which involve writing new log entries.
 * 
 * @author  William Song
 * @version 1.0.2
 */
public class LogWriter extends Task {
   
   private boolean isUp;
   private String logMsg;
   private String level;
   
   public LogWriter(Application app, boolean isUp, String logMsg, String level) {
      super(app);
      
      this.isUp = isUp;
      this.logMsg = logMsg;
      this.level = level;
   }

   /**
    * Executes code in a background thread.
    * Connects to the background service and writes a new log entry.
    * 
    * @return  true if the entry was added successfully, false otherwise
    * @throws  java.lang.Exception  thrown if background service cannot be detected
    */
   @Override protected Boolean doInBackground() throws Exception {
      Boolean ret = new Boolean(false);
      if (isUp) { 
	 if (level != null && level.length() > 0 && logMsg != null && logMsg.length() > 0) {
	    ControlFileService service = new ControlFileService();
	    ret = new Boolean(service.writeLog(level, logMsg));
	 }
      }
      
      return ret;
   }
   
   /**
    * This is the called after failed() or succeeded().
    */
   @Override protected void finished() {
      // don't bother...
   }
   
   /**
    * This is called when doInBackground throws an exception.
    * 
    * @param   t  the exception thrown
    */
   @Override protected void failed(Throwable t) {
      // don't bother...
   }
   
   /**
    * This is called when doInBackground is successfully executed.
    * 
    * @param   o  the return value of doInBackground
    */
   @Override protected void succeeded(Object o) {
      // don't bother...
   }
}
