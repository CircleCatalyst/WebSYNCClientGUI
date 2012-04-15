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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.rmi.RemoteException;

import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 * Performs tasks which involve retrieving logs from the background service.
 * 
 * @author  William Song
 * @version 1.0.2
 */
public class LogReader extends Task {
   
   private boolean isUp;
   private String log;
   private String badAlertCall;
   private String logFileLoc;
   /**
    * Indicates the number of characters read by the Task in this session,
    * i.e. the number of characters to skip for each instance.
    */
   private static long numCharsRead;
   
   /**
    * Constructor...
    * 
    * @param app	the main desktop application which calls this worker
    * @param isUp	whether the service is up or not
    * @param logFileLoc	the location of the main log file
    */
   public LogReader(Application app, boolean isUp, String logFileLoc) {
      super(app);
      
      this.isUp = isUp;
      this.logFileLoc = logFileLoc;
   }

   /**
    * Executes code in a background thread.
    * Connects to the background service and performs a test of its WebSYNC connection.
    * 
    * @return  the log lines
    * @throws  java.lang.Exception  thrown if background service cannot be detected
    */
   @Override
	protected String doInBackground() throws Exception {
		File logfile = new File(logFileLoc);
		// do we have any new entries in the log that we haven't read yet?
		BufferedReader br = new BufferedReader(new FileReader(logfile));
		StringBuffer ret = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			ret.append(line).append("\n");
		}
		br.close();

		return ret.toString();
	}
   
   /**
    * This is the called after failed() or succeeded().
    */
   @Override protected void finished() {
      if (badAlertCall != null) {
	 badAlertCall = null;
      }
   }
   
   /**
    * This is called when doInBackground throws an exception.
    * 
    * @param   t  the exception thrown
    */
   @Override protected void failed(Throwable t) {
      if (t instanceof RemoteException) {
	 badAlertCall = "Could not detect background service";
      } else {
	 badAlertCall = t.getMessage();
      }
   }
   
   /**
    * This is called when doInBackground is successfully executed.
    * 
    * @param   o  the return value of doInBackground
    */
   @Override protected void succeeded(Object o) {
      String s = (String)o;     
	   setLog(s);
   }
   
   protected void setLog(String newLog) {
      String oldLog;
      synchronized(this) {
	 oldLog = this.log;
	 this.log = newLog;
	 newLog = this.log;
      }
      firePropertyChange("log", oldLog, newLog);
      this.log = "";
   }
}
