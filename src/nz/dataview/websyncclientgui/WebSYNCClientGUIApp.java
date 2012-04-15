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

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 * 
 * @author  William Song
 * @version 1.0.2
 */
public class WebSYNCClientGUIApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
//       addExitListener(new ExitListener() {
//            public boolean canExit(java.util.EventObject e) {
//                java.awt.Component source = (java.awt.Component) e.getSource();
//                int option = javax.swing.JOptionPane.showConfirmDialog(
//		  source, 
//		  "Do you really want to exit?", 
//		  "Exit WebSYNC Client",
//		  javax.swing.JOptionPane.YES_NO_OPTION,
//		  javax.swing.JOptionPane.QUESTION_MESSAGE
//		);
//                return option == javax.swing.JOptionPane.YES_OPTION;
//            }
//            public void willExit(java.util.EventObject event) {
//                
//            }
//        });
        show(new WebSYNCClientGUIView(this));
	java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().createImage(WebSYNCClientGUIApp.class.getResource("frame-icon.gif"));
	getMainFrame().setIconImage(img);
//	getMainFrame().setResizable(false);
	
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }
    
    /**
     * Retrieves (and registers) a ServiceConnector task which is used to
     * connect to the background service.
     * 
     * @param  isRunning   whether the background service is running at the moment or not
     * @param  isUp	   whether the background service is reachable or not
     * @return		   the Task which connects to the background service
     */
    @Action public ServiceConnector connectToService(boolean isRunning, boolean isUp) {
	return new ServiceConnector(getApplication(), isRunning, isUp);
    }
    
    /**
     * Retrieves (and registers) a ServiceTester task which is used to
     * test the background service.
     * 
     * @return the Task which tests the background service
     */
    @Action public ServiceTester testService() {
        return new ServiceTester(getApplication());
    }
    
    /**
     * Retrieves (and registers) a ServiceStarter task which is used to
     * start/run the background service.
     * 
     * @return the Task which starts/runs the background service
     */
    @Action public ServiceStarter runService() {
        return new ServiceStarter(getApplication());
    }
    
    /**
     * Retrieves (and registers) a ServiceRestarter task which is used to restart
     * the background service.
     * 
     * @return the Task which restarts the background service
     */
    @Action public ServiceRestarter restartService() {
       return new ServiceRestarter(getApplication());
    }
    
    /**
     * Retrieves (and registers) a LogReader task which is used to
     * retrieve new log entries from the background service.
     * 
     * @param  isUp	   whether the background service is up at the moment or not
     * @param  logFileLoc  the location of the main log file
     * @return		   the Task which retrieves new log entries from the background service
     */
    @Action public LogReader logReadService(boolean isUp, String logFileLoc) {
        return new LogReader(getApplication(), isUp, logFileLoc);
    }
    
    /**
     * Retrieves (and registers) a LogWriter task which is used to
     * write new log entries to the background service.
     * 
     * @param  isUp	whether the background service is up at the moment or not
     * @param  message	the log message
     * * @param  level	the log entry level
     * @return		the Task which writes new log entries to the background service
     */
    @Action public LogWriter logWriteService(boolean isUp, String message, String level) {
        return new LogWriter(getApplication(), isUp, message, level);
    }

    /**
     * get a MessageSender worker, and send the message to WebSyncClient service
     * @param message
     * @return
     */
    @Action public MessageSender sendMessage(String message, String friendlyMessage) {
       return new MessageSender(getApplication(), message, friendlyMessage);
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of WebSYNCClientGUIApp
     */
    public static WebSYNCClientGUIApp getApplication() {
        return Application.getInstance(WebSYNCClientGUIApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(WebSYNCClientGUIApp.class, args);
    }
}
