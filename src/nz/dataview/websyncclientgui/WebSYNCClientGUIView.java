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
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.io.IOException;

import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.FileAppender;

//import javax.swing.event.ChangeListener;
/**
 * The application's main frame.
 * 
 * @author  William Song, Tim Owens
 * @version 1.1.0
 */
public class WebSYNCClientGUIView extends FrameView implements ActionListener {

   private static boolean providerInstalled = false;

   public WebSYNCClientGUIView(SingleFrameApplication app) {
      super(app);

      // do this before anything else...
      try {
         client = new WebSYNC();
      } catch (java.io.IOException e) {
         // nonono!
      }
      // stuff that needs initialising before the components initialise
      proxyEnabled = client.getProxyEnabled();

      initComponents();

      System.setProperty("java.rmi.server.hostname", "localhost");

      // status bar initialization - message timeout, idle icon and busy animation, etc
      ResourceMap resourceMap = getResourceMap();
      int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
     progressBar.setVisible(false);

      // connecting action tasks to status bar via TaskMonitor
      TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
      taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

         public void propertyChange(java.beans.PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if ("started".equals(propertyName)) {
               progressBar.setVisible(true);
               progressBar.setIndeterminate(true);
            } else if ("done".equals(propertyName)) {
               progressBar.setVisible(false);
               progressBar.setValue(0);
            } else if ("message".equals(propertyName)) {
            } else if ("progress".equals(propertyName)) {
               int value = (Integer) (evt.getNewValue());
               progressBar.setVisible(true);
               progressBar.setIndeterminate(false);
               progressBar.setValue(value);
            } else if ("icon".equals(propertyName)) {
            } else if ("largeIcon".equals(propertyName)) {
               Icon icon = (Icon) evt.getNewValue();
//		    if (mainTabbedPane.getSelectedIndex() == 0) {
               connectPanelCurrentStatusOverviewLabel.setIcon(icon);
//		    }
            } else if ("largeMessage".equals(propertyName)) {
               String text = (String) (evt.getNewValue());
               if (text != null/* && mainTabbedPane.getSelectedIndex() == 0*/) {
                  connectPanelCurrentStatusOverviewLabel.setText(text);
               }
            } else if ("detailMessage".equals(propertyName)) {
               String text = (String) (evt.getNewValue());
               if (text != null &&
							(jScrollPane1.getVerticalScrollBar()==null || !jScrollPane1.getVerticalScrollBar().getValueIsAdjusting()) &&
							(jScrollPane1.getHorizontalScrollBar()==null || !jScrollPane1.getHorizontalScrollBar().getValueIsAdjusting())
						) {
                  connectPanelCurrentStatusDetailsLabel.setText(text);
               }
            } else if ("isRunning".equals(propertyName)) {
               isRunning = ((Boolean) evt.getNewValue()).booleanValue();
               ResourceMap map = getResourceMap();
               String keyName = "runButton.toolTipText";
               if (!isRunning && isUp) {
                  keyName = "runButton.okToolTipText";
               }
               testConnectionButton.setEnabled(isUp && !isRunning);
             } else if ("isUp".equals(propertyName)) {
               isUp = ((Boolean) evt.getNewValue()).booleanValue();
               ResourceMap map = getResourceMap();
               String keyName = "testConnectionButton.toolTipText";
               if (isUp) {
                  // service is reachable (doesn't mean its running)
                  keyName = "testConnectionButton.okToolTipText";
               }
               testConnectionButton.setEnabled(isUp && !isRunning);
               testConnectionButton.setToolTipText(map.getString(keyName, new Object[0]));
//		    viewLogTextPane.setEnabled(isUp);
            } else if ("testIcon".equals(propertyName)) {
               Icon icon = (Icon) evt.getNewValue();
               testConnectionResult.setIcon(icon);
            } else if ("testMessage".equals(propertyName)) {
               String text = (String) evt.getNewValue();
               testConnectionResult.setText(text);
            } else if ("testFinished".equals(propertyName)) {
               testConnectionButton.setEnabled(isUp && !isRunning);
            } else if ("badAlertMessage".equals(propertyName)) {
               String text = (String) evt.getNewValue();
               if (text != null) {
                  showErrorDialog("Error", text);
                  WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
                  LogWriter worker = app.logWriteService(isUp, text, "WARN");
                  app.getContext().getTaskService().execute(worker);
               }
            } else if ("okAlertMessage".equals(propertyName)) {
               String text = (String) evt.getNewValue();
               if (text != null) {
                  showNoticeDialog("Confirmation", text);
                  WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
                  LogWriter worker = app.logWriteService(isUp, text, "INFO");
                  app.getContext().getTaskService().execute(worker);
               }
            } else if ("log".equals(propertyName)) {
               String text = (String) evt.getNewValue();
                  String orig = viewLogTextPane.getText();
                  String log = (StringUtils.isEmpty(text) && (orig.equals(VIEW_LOG_INITIAL) || orig.equals(VIEW_LOG_EMPTY))) ? VIEW_LOG_EMPTY : text;
                  if (log.length() > MAX_LOG_VIEW_SIZE) {
                     int start = log.length() - MAX_LOG_VIEW_SIZE;
                     int end = log.length();
                     log = log.substring(start, end);
                  }
                  viewLogTextPane.setText(log);
            }
         }
      });

      // custom code

      // NetBeans uses the Swing Application Framework
      // Information on this framework is available here:
      // https://appframework.dev.java.net/
      // http://jsourcery.com/api/java.net/appframework/0.21/application/SingleFrameApplication.html
      // http://weblogs.java.net/blog/joconner/archive/2007/06/swing_applicati_1.html
      // https://appframework.dev.java.net/intro/index.html
      // API docs: https://appframework.dev.java.net/nonav/javadoc/AppFramework-1.03/index.html

      // Note: any tweaking of the main frame eg the size etc,
      // should be done in the WebSYNCClientGUIApp.properties file
      // located in src/nz/dataview/websyncclientgui/resources/	
      // (seems overly complex for what I'm trying to achieve, but I guess its convenient)

      initialConfigDone = false;
      initConfig();
      if (!client.isProxyConfigured()) detectProxy();

      viewLogTimer = new Timer(3000, this);
      viewLogTimer.start();

      /*mainTabbedPane.getModel().addChangeListener(
      new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
      javax.swing.DefaultSingleSelectionModel root = (javax.swing.DefaultSingleSelectionModel)e.getSource();
      if (root.getSelectedIndex() == 2) {
      viewLogTimer.start();
      } else {
      viewLogTimer.stop();
      }
      }
      }
      );*/

      statusTimer = new Timer(3*1000, this);
      statusTimer.start();

      browseUploadDirButton.addActionListener(this);
   }

   @Action
   public void showAboutBox() {
      if (aboutBox == null) {
         JFrame mainFrame = WebSYNCClientGUIApp.getApplication().getMainFrame();
         aboutBox = new WebSYNCClientGUIAboutBox(mainFrame);
         aboutBox.setLocationRelativeTo(mainFrame);
      }
      WebSYNCClientGUIApp.getApplication().show(aboutBox);
   }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == viewLogTimer) {
         // always create a new instance of the worker in order for doInBackground to work properly
         WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
         LogReader worker = app.logReadService(isUp, client.getLogFileLocation());
         app.getContext().getTaskService().execute(worker);
      } else if (e.getSource() == statusTimer) {
         // always create a new instance of the worker in order for doInBackground to work properly
         WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
         ServiceConnector worker = app.connectToService(isRunning, isUp);
         app.getContext().getTaskService().execute(worker);
      } else if (e.getSource() == browseUploadDirButton) {
         int ret = dirFileChooser.showOpenDialog(configurePanel);
         switch (ret) {
            case javax.swing.JFileChooser.APPROVE_OPTION:
               // save it
               uploadDirField.setText(dirFileChooser.getSelectedFile().getAbsolutePath());
               configureSaveButton.setEnabled(true);
               break;
            case javax.swing.JFileChooser.CANCEL_OPTION:
               // do nothing
               break;
            case javax.swing.JFileChooser.ERROR_OPTION:
            default:
               // do something
               break;
         }
      }
   }

   private void initConfig() {
      schoolMOENumberField.setText(client.getSchoolMOENo());
      knURLField.setText(client.getKNURL());

      //      useProxyField.setSelected(proxyEnabled);
      proxyHostField.setEnabled(proxyEnabled);
      proxyPortField.setEnabled(proxyEnabled);
      proxyUserField.setEnabled(proxyEnabled);
      proxyPwField.setEnabled(proxyEnabled);
      proxyHostField.setText(client.getProxyHost());
      proxyPortField.setText(client.getProxyPort());
      proxyUserField.setText(client.getProxyUser());
      proxyPwField.setText(client.getProxyPw());

      uploadDirField.setText(client.getUploadDir());

      int uploadCount = new Integer(client.getFailedUploadCount());
      failedUploadSpinner.setValue(uploadCount);
      smtpEnabled = (uploadCount > 0);

      emailTextField.setText(client.getEmail());
      smtpHostTextField.setText(client.getSmtpHost());
      smtpUserTextField.setText(client.getSmtpUser());
      smtpPasswordField.setText(client.getSmtpPassword());

      int selectedProcessTime = client.getProcessUploadIndex();

      javax.swing.ButtonModel model;
      switch (selectedProcessTime) {
         case WebSYNC.PROCESS_TIME_SCHOOL_HOURS:
            model = processUploadSchoolHoursButton.getModel();
            break;
         case WebSYNC.PROCESS_TIME_NON_SCHOOL_HOURS:
            model = processUploadNonSchoolHoursButton.getModel();
            break;
         case WebSYNC.PROCESS_TIME_ALWAYS:
         default:
            model = processUploadAlwaysButton.getModel();
      }
      configProcessUploadGroup.setSelected(model, true);

      authenticationKeyField.setText(client.getAuthenticationKey());
      confAuthKeyField.setText(client.getAuthenticationKey());
//      confAuthKeyField.setEnabled(false);
      advancedSaveButton.setEnabled(false);
      initialConfigDone = true;
   }

   /**
    * Saves all the values of the configuration fields to the appConfig.
    * <strong>Assumes <code>verifyConfigInput()</code> has been called.</strong>
    */
   private void saveConfig() throws java.io.IOException {
      // at this point, we can assume that all fields have been validated
      client.setSchoolMOENo(schoolMOENumberField.getText());
      client.setKNURL(knURLField.getText());
      if (proxyEnabled) {
         // don't bother saving if its not enabled
         client.setProxyHost(proxyHostField.getText());
         client.setProxyPort(Integer.parseInt(proxyPortField.getText()));
         client.setProxyUser(proxyUserField.getText());
         client.setProxyPw(proxyPwField.getPassword());
      }
      client.setProxyEnabled(proxyEnabled);
      client.setEmail(emailTextField.getText());
      client.setSmtpHost(smtpHostTextField.getText());
      client.setSmtpUser(smtpUserTextField.getText());
      client.setSmtpPassword(smtpPasswordField.getPassword());

      client.setFailedUploadCount((Integer) failedUploadSpinner.getValue());

      client.setUploadDir(uploadDirField.getText());
      client.setAuthenticationKey(authenticationKeyField.getPassword());

      int processUploadIndex =
              processUploadAlwaysButton.isSelected() ? WebSYNC.PROCESS_TIME_ALWAYS : (processUploadSchoolHoursButton.isSelected() ? WebSYNC.PROCESS_TIME_SCHOOL_HOURS : (processUploadNonSchoolHoursButton.isSelected() ? WebSYNC.PROCESS_TIME_NON_SCHOOL_HOURS : WebSYNC.PROCESS_TIME_ALWAYS));
      client.setProcessUploadIndex(processUploadIndex);

      client.saveConfig();
   }

   /**
    * Determines whether the source of a KeyEvent is flagged to require a restart of the 
    * background service on change of its value.
    * 
    * @param   evt   the KeyEvent fired
    * @return	     true if event requires a restart if changed, false otherwise
    */
   private boolean fieldChangeRequiresRestart(java.awt.event.KeyEvent evt) {
      Object src = evt.getSource();
      boolean ret = false;

      // add any new fields here
      ret = src == useProxyField || src == proxyHostField || src == proxyPortField || src == proxyUserField || src == proxyPwField;

      return ret;
   }

   /**
    * Performs input validation.
    * 
    * @return  error message if errors exist, or an empty string otherwise
    */
   private String verifyConfigInput() {
      String ret = "";

      // init fields
      String proxyHost = proxyHostField.getText();
      String proxyPort = proxyPortField.getText();
      String proxyUser = proxyUserField.getText();
      char[] proxyPw = proxyPwField.getPassword();
      String schoolMOENo = schoolMOENumberField.getText();
      String knURL = knURLField.getText();
      String uploadDir = uploadDirField.getText();
      char[] authKey = authenticationKeyField.getPassword();
      char[] confAuth = confAuthKeyField.getPassword();

      // mandatory only if enabled
      if (proxyEnabled) {
         if (proxyHost.length() <= 0) {
            ret += "Please enter your Proxy Host\n";
         }
         if (proxyPort.length() <= 0) {
            ret += "Please enter your Proxy Port Number\n";
         }
      }

      // mandatory fields
      if (knURL.length() <= 0) {
         ret += "Please enter your KnowledgeNET URL\n";
      }
      if (uploadDir.length() <= 0) {
         ret += "Please enter your Upload Directory\n";
      }
      if (authKey.length <= 0) {
         ret += "Please enter your Authentication Key\n";
      }
      if (confAuth.length <= 0) {
         ret += "Please confirm your Authentication Key\n";
      }

      if (ret.length() == 0) {
         // begin field specific validation

         Pattern schoolNoPattern = Pattern.compile("^\\d{1,6}$");
         Pattern url = Pattern.compile("^http://[a-zA-Z0-9:._-]{1,256}(/|/[a-zA-Z0-9;&?_=.,/-]{1,512})?$");
         Pattern proxyHostPattern = Pattern.compile("^[a-zA-Z0-9.-_]{1,256}$");
         Pattern proxyPortPattern = Pattern.compile("^\\d{1,5}$");

         if (proxyEnabled) {
            if (!proxyHostPattern.matcher(proxyHost).find()) {
               ret += "\u2022 Proxy Host appears to be invalid or is too short/long.\n";
            }
            if (!proxyPortPattern.matcher(proxyPort).find()) {
               ret += "\u2022 Proxy Port must contain only digits and be between 1 and 5 digits in length.\n";
            }
            if (proxyUser.length() > 0) {
               if (proxyUser.length() > 20) {
                  ret += "\u2022 Proxy User cannot be longer than 20 characters.\n";
               }
            }
            if (proxyPw.length > 0) {
               if (proxyPw.length > 20) {
                  ret += "\u2022 Proxy Password cannot be longer than 20 characters.\n";
               }
            }
         }

         if (schoolMOENo.length() > 0 && !schoolNoPattern.matcher(schoolMOENo).find()) {
            ret += "\u2022 School MoE Number must contain only digits and be between 1 and 6 digits in length.\n";
         }

         if (!url.matcher(knURL).find()) {
            ret += "\u2022 KnowledgeNET URL appears invalid or is too short/long.\n";
         }

         if (uploadDir.length() > 1000) {
            ret += "\u2022 Path to Upload Directory cannot be longer than 1000 characters.\n";
         } else {
            // make sure directory exists and we can read and write to it
            // (since we need to delete the files after uploading)
            File file = new File(uploadDir);
            if (!file.isDirectory()) {
               ret += "\u2022 Upload Directory must be a directory.\n";
            } else if (!file.canRead()) {
               ret += "\u2022 Upload Directory cannot be read.\n";
            } else if (!file.canWrite()) {
               ret += "\u2022 Upload Directory cannot be written to.\n";
            }
         }

         if (authKey.length > 0) {
            if (authKey.length > 255) {
               ret += "\u2022 Authentication Key cannot be longer than 255 characters.\n";
            } else {
               if (authKey.length != confAuth.length) {
                  ret += "\u2022 Authentication Key and its confirmation must be identical.\n";
               } else {
                  boolean bad = false;
                  String full = "";
                  for (int i = 0; i < authKey.length; i++) {
                     try {
                        full += authKey[i];
                        if (authKey[i] != confAuth[i]) {
                           bad = true;
                        }
                     } catch (ArrayIndexOutOfBoundsException e) {
                        bad = true;
                        break;
                     }
                  }
                  if (bad) {
                     ret += "\u2022 Authentication Key and its confirmation must be identical.\n";
                  } /*else if (notAlphanumeric.matcher(full).find()) {
                  ret += "Authentication Key contains invalid characters\n";
                  }*/
                  full = "";
               }
            }

            // clear the authentication key from memory
            for (int i = 0; i < authKey.length; i++) {
               authKey[i] = 0;
            }
            for (int i = 0; i < confAuth.length; i++) {
               confAuth[i] = 0;
            }
         }
      }

      return ret;
   }

   /**
    * Performs input validation.
    * 
    * @return  error message if errors exist, or an empty string otherwise
    */
   private String verifyAdvancedInput() {
      String ret = "";

      // init fields
      String email = emailTextField.getText();
      String smtpHost = smtpHostTextField.getText();
      String smtpUser = smtpUserTextField.getText();
      char[] smtpPw = smtpPasswordField.getPassword();

      // mandatory only if enabled
      if (smtpEnabled) {
         if (email.length() <= 0) {
            ret += "Please enter the email address for reports\n";
         }
         if (smtpHost.length() <= 0) {
            ret += "Please enter your SMTP Host\n";
         }
      }

      if (ret.length() == 0) {
         // begin field specific validation
         Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9.-_']{1,256}@[a-zA-Z0-9.-_']{1,256}$");
         Pattern smtpHostPattern = Pattern.compile("^[a-zA-Z0-9.-_]{1,256}$");
         Pattern smtpUserPattern = Pattern.compile("^[a-zA-Z0-9.-_]{0,256}$");

//         if (smtpEnabled) {
            if (email.length() > 0 && !emailPattern.matcher(email).find()) {
               ret += "\u2022 Email address appears to be invalid or is too short/long.\n";
            }
            if (smtpHost.length() > 0 && !smtpHostPattern.matcher(smtpHost).find()) {
               ret += "\u2022 SMTP Host appears to be invalid or is too short/long.\n";
            }
            if (smtpUser.length() > 0) {
               if (!smtpUserPattern.matcher(smtpUser).find()) {
                  ret += "\u2022 SMTP User  appears to be invalid or is too short/long.\n";
               }
               if (smtpUser.length() > 20) {
                  ret += "\u2022 SMTP User cannot be longer than 20 characters.\n";
               }
            }
            if (smtpPw.length > 0) {
               if (smtpPw.length > 20) {
                  ret += "\u2022 SMTP Password cannot be longer than 20 characters.\n";
               }
            }
//         }

      }

      return ret;
   }

   private void showErrorDialog(String title, String message) {
      javax.swing.JOptionPane.showMessageDialog(mainPanel, message, title, javax.swing.JOptionPane.ERROR_MESSAGE);
   }

   private void showWarningDialog(String title, String message) {
      javax.swing.JOptionPane.showMessageDialog(mainPanel, message, title, javax.swing.JOptionPane.WARNING_MESSAGE);
   }

   private void showNoticeDialog(String title, String message) {
      javax.swing.JOptionPane.showMessageDialog(mainPanel, message, title, javax.swing.JOptionPane.INFORMATION_MESSAGE);
   }

   private int showConfirmDialog(String title, String message) {
      return javax.swing.JOptionPane.showConfirmDialog(mainPanel, message, title, javax.swing.JOptionPane.OK_CANCEL_OPTION);
   }

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      mainPanel = new javax.swing.JPanel();
      mainTabbedPane = new javax.swing.JTabbedPane();
      connectPanel = new javax.swing.JPanel();
      mainConnectLabel = new javax.swing.JLabel();
      connectPanelCurrentStatusLabel = new javax.swing.JLabel();
      connectPanelCurrentStatusOverviewLabel = new javax.swing.JLabel();
      connectSeparator0 = new javax.swing.JSeparator();
      connectPanelTestLabel = new javax.swing.JLabel();
      testConnectionButton = new javax.swing.JButton();
      testConnectionResult = new javax.swing.JLabel();
      progressBar = new javax.swing.JProgressBar();
      jPanel1 = new javax.swing.JPanel();
      jScrollPane1 = new javax.swing.JScrollPane();
      connectPanelCurrentStatusDetailsLabel = new javax.swing.JLabel();
      configurePanel = new javax.swing.JPanel();
      mainConfigureLabel = new javax.swing.JLabel();
      schoolMOENumberLabel = new javax.swing.JLabel();
      uploadDirLabel = new javax.swing.JLabel();
      knURLLabel = new javax.swing.JLabel();
      authenticationKeyLabel = new javax.swing.JLabel();
      confAuthKeyLabel = new javax.swing.JLabel();
      schoolMOENumberField = new javax.swing.JTextField();
      knURLField = new javax.swing.JTextField();
      configSeparator1 = new javax.swing.JSeparator();
      configSeparator2 = new javax.swing.JSeparator();
      uploadDirField = new javax.swing.JTextField();
      configSeparator3 = new javax.swing.JSeparator();
      configureSaveButton = new javax.swing.JButton();
      configureInfoLabel = new javax.swing.JLabel();
      authenticationKeyField = new javax.swing.JPasswordField();
      confAuthKeyField = new javax.swing.JPasswordField();
      browseUploadDirButton = new javax.swing.JButton();
      useProxyField = new javax.swing.JCheckBox();
      proxyPortLabel = new javax.swing.JLabel();
      proxyPortField = new javax.swing.JTextField();
      proxyHostLabel = new javax.swing.JLabel();
      proxyHostField = new javax.swing.JTextField();
      proxyUserLabel = new javax.swing.JLabel();
      proxyUserField = new javax.swing.JTextField();
      proxyPwLabel = new javax.swing.JLabel();
      proxyPwField = new javax.swing.JPasswordField();
      detectProxyButton = new javax.swing.JButton();
      proxyDetectedLabel = new javax.swing.JLabel();
      advancedPanel = new javax.swing.JPanel();
      mainAdvancedlabel = new javax.swing.JLabel();
      configureSeparator1 = new javax.swing.JSeparator();
      advancedSaveButton = new javax.swing.JButton();
      processUploadNonSchoolHoursButton = new javax.swing.JRadioButton();
      processUploadSchoolHoursButton = new javax.swing.JRadioButton();
      processUploadAlwaysButton = new javax.swing.JRadioButton();
      processUploadLabel = new javax.swing.JLabel();
      failedUploadLabel = new javax.swing.JLabel();
      failedUploadSpinner = new javax.swing.JSpinner();
      failedUploadLabel2 = new javax.swing.JLabel();
      emailTextField = new javax.swing.JTextField();
      emailLabel = new javax.swing.JLabel();
      smtpHostLabel = new javax.swing.JLabel();
      smtpHostTextField = new javax.swing.JTextField();
      smtpUserLabel = new javax.swing.JLabel();
      smtpPasswordLabel = new javax.swing.JLabel();
      smtpUserTextField = new javax.swing.JTextField();
      jSeparator1 = new javax.swing.JSeparator();
      jSeparator2 = new javax.swing.JSeparator();
      smtpPasswordField = new javax.swing.JPasswordField();
      viewLogPanel = new javax.swing.JPanel();
      mainViewLogLabel = new javax.swing.JLabel();
      viewLogScrollPane = new javax.swing.JScrollPane();
      viewLogTextPane = new javax.swing.JTextPane();
      viewLogInfoLabel = new javax.swing.JLabel();
      emailLogsButton = new javax.swing.JButton();
      aboutPanel = new javax.swing.JPanel();
      mainAboutLabel = new javax.swing.JLabel();
      aboutScrollPane = new javax.swing.JScrollPane();
      aboutTextPane = new javax.swing.JTextPane();
      dirFileChooser = new javax.swing.JFileChooser();
      configProcessUploadGroup = new javax.swing.ButtonGroup();

      mainPanel.setName("mainPanel"); // NOI18N

      mainTabbedPane.setName("mainTabbedPane"); // NOI18N
      mainTabbedPane.setPreferredSize(new java.awt.Dimension(492, 643));
      mainTabbedPane.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
         public void propertyChange(java.beans.PropertyChangeEvent evt) {
            mainTabbedPanePropertyChange(evt);
         }
      });

      connectPanel.setName("connectPanel"); // NOI18N

      org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(nz.dataview.websyncclientgui.WebSYNCClientGUIApp.class).getContext().getResourceMap(WebSYNCClientGUIView.class);
      mainConnectLabel.setIcon(resourceMap.getIcon("mainConnectLabel.icon")); // NOI18N
      mainConnectLabel.setText(resourceMap.getString("mainConnectLabel.text")); // NOI18N
      mainConnectLabel.setName("mainConnectLabel"); // NOI18N

      connectPanelCurrentStatusLabel.setText(resourceMap.getString("connectPanelCurrentStatusLabel.text")); // NOI18N
      connectPanelCurrentStatusLabel.setName("connectPanelCurrentStatusLabel"); // NOI18N

      connectPanelCurrentStatusOverviewLabel.setText(resourceMap.getString("connectPanelCurrentStatusOverviewLabel.text")); // NOI18N
      connectPanelCurrentStatusOverviewLabel.setName("connectPanelCurrentStatusOverviewLabel"); // NOI18N

      connectSeparator0.setName("connectSeparator0"); // NOI18N

      connectPanelTestLabel.setText(resourceMap.getString("connectPanelTestLabel.text")); // NOI18N
      connectPanelTestLabel.setName("connectPanelTestLabel"); // NOI18N

      javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(nz.dataview.websyncclientgui.WebSYNCClientGUIApp.class).getContext().getActionMap(WebSYNCClientGUIView.class, this);
      testConnectionButton.setAction(actionMap.get("testService")); // NOI18N
      testConnectionButton.setText(resourceMap.getString("testConnectionButton.text")); // NOI18N
      testConnectionButton.setToolTipText(resourceMap.getString("testConnectionButton.toolTipText")); // NOI18N
      testConnectionButton.setName("testConnectionButton"); // NOI18N
      testConnectionButton.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            testConnectionButtonMouseClicked(evt);
         }
      });

      testConnectionResult.setText(resourceMap.getString("testConnectionResult.text")); // NOI18N
      testConnectionResult.setName("testConnectionResult"); // NOI18N

      progressBar.setName("progressBar"); // NOI18N

      jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
      jPanel1.setName("jPanel1"); // NOI18N

      jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
      jScrollPane1.setName("jScrollPane1"); // NOI18N

      connectPanelCurrentStatusDetailsLabel.setText(resourceMap.getString("connectPanelCurrentStatusDetailsLabel.text")); // NOI18N
      connectPanelCurrentStatusDetailsLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
      connectPanelCurrentStatusDetailsLabel.setAutoscrolls(true);
      connectPanelCurrentStatusDetailsLabel.setName("connectPanelCurrentStatusDetailsLabel"); // NOI18N
      jScrollPane1.setViewportView(connectPanelCurrentStatusDetailsLabel);

      org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
      jPanel1.setLayout(jPanel1Layout);
      jPanel1Layout.setHorizontalGroup(
         jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 562, Short.MAX_VALUE)
      );
      jPanel1Layout.setVerticalGroup(
         jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
      );

      org.jdesktop.layout.GroupLayout connectPanelLayout = new org.jdesktop.layout.GroupLayout(connectPanel);
      connectPanel.setLayout(connectPanelLayout);
      connectPanelLayout.setHorizontalGroup(
         connectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(org.jdesktop.layout.GroupLayout.TRAILING, connectPanelLayout.createSequentialGroup()
            .add(connectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
               .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .add(connectPanelLayout.createSequentialGroup()
                  .addContainerGap()
                  .add(connectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, mainConnectLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, connectSeparator0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                     .add(connectPanelLayout.createSequentialGroup()
                        .add(connectPanelCurrentStatusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                     .add(org.jdesktop.layout.GroupLayout.LEADING, connectPanelCurrentStatusOverviewLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 324, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, connectPanelLayout.createSequentialGroup()
                        .add(connectPanelTestLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(testConnectionButton))
                     .add(connectPanelLayout.createSequentialGroup()
                        .add(testConnectionResult, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                        .add(126, 126, 126)))))
            .addContainerGap())
      );
      connectPanelLayout.setVerticalGroup(
         connectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(connectPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainConnectLabel)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(connectSeparator0, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(connectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(connectPanelCurrentStatusLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(connectPanelCurrentStatusOverviewLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(connectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(connectPanelTestLabel)
               .add(testConnectionButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(testConnectionResult, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
      );

      mainTabbedPane.addTab(resourceMap.getString("connectPanel.TabConstraints.tabTitle"), resourceMap.getIcon("connectPanel.TabConstraints.tabIcon"), connectPanel, resourceMap.getString("connectPanel.TabConstraints.tabToolTip")); // NOI18N

      configurePanel.setName("configurePanel"); // NOI18N

      mainConfigureLabel.setIcon(resourceMap.getIcon("mainConfigureLabel.icon")); // NOI18N
      mainConfigureLabel.setText(resourceMap.getString("mainConfigureLabel.text")); // NOI18N
      mainConfigureLabel.setName("mainConfigureLabel"); // NOI18N

      schoolMOENumberLabel.setText(resourceMap.getString("schoolMOENumberLabel.text")); // NOI18N
      schoolMOENumberLabel.setToolTipText(resourceMap.getString("schoolMOENumberLabel.toolTipText")); // NOI18N
      schoolMOENumberLabel.setName("schoolMOENumberLabel"); // NOI18N
      schoolMOENumberLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            schoolMOENumberLabelMouseClicked(evt);
         }
      });

      uploadDirLabel.setText(resourceMap.getString("uploadDirLabel.text")); // NOI18N
      uploadDirLabel.setToolTipText(resourceMap.getString("uploadDirLabel.toolTipText")); // NOI18N
      uploadDirLabel.setName("uploadDirLabel"); // NOI18N
      uploadDirLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            uploadDirLabelMouseClicked(evt);
         }
      });

      knURLLabel.setText(resourceMap.getString("knURLLabel.text")); // NOI18N
      knURLLabel.setToolTipText(resourceMap.getString("knURLLabel.toolTipText")); // NOI18N
      knURLLabel.setName("knURLLabel"); // NOI18N
      knURLLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            knURLLabelMouseClicked(evt);
         }
      });

      authenticationKeyLabel.setText(resourceMap.getString("authenticationKeyLabel.text")); // NOI18N
      authenticationKeyLabel.setToolTipText(resourceMap.getString("authenticationKeyLabel.toolTipText")); // NOI18N
      authenticationKeyLabel.setName("authenticationKeyLabel"); // NOI18N
      authenticationKeyLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            authenticationKeyLabelMouseClicked(evt);
         }
      });

      confAuthKeyLabel.setText(resourceMap.getString("confAuthKeyLabel.text")); // NOI18N
      confAuthKeyLabel.setToolTipText(resourceMap.getString("confAuthKeyLabel.toolTipText")); // NOI18N
      confAuthKeyLabel.setName("confAuthKeyLabel"); // NOI18N
      confAuthKeyLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            confAuthKeyLabelMouseClicked(evt);
         }
      });

      schoolMOENumberField.setText(resourceMap.getString("schoolMOENumberField.text")); // NOI18N
      schoolMOENumberField.setToolTipText(resourceMap.getString("schoolMOENumberField.toolTipText")); // NOI18N
      schoolMOENumberField.setName("schoolMOENumberField"); // NOI18N
      schoolMOENumberField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onConfigFieldChanged(evt);
         }
      });

      knURLField.setText(resourceMap.getString("knURLField.text")); // NOI18N
      knURLField.setToolTipText(resourceMap.getString("knURLField.toolTipText")); // NOI18N
      knURLField.setName("knURLField"); // NOI18N
      knURLField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onConfigFieldChanged(evt);
         }
      });

      configSeparator1.setName("configSeparator1"); // NOI18N

      configSeparator2.setName("configSeparator2"); // NOI18N

      uploadDirField.setText(resourceMap.getString("uploadDirField.text")); // NOI18N
      uploadDirField.setToolTipText(resourceMap.getString("uploadDirField.toolTipText")); // NOI18N
      uploadDirField.setName("uploadDirField"); // NOI18N
      uploadDirField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onConfigFieldChanged(evt);
         }
      });

      configSeparator3.setName("configSeparator3"); // NOI18N

      configureSaveButton.setText(resourceMap.getString("configureSaveButton.text")); // NOI18N
      configureSaveButton.setEnabled(false);
      configureSaveButton.setName("configureSaveButton"); // NOI18N
      configureSaveButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            configureSaveButtonActionPerformed(evt);
         }
      });

      configureInfoLabel.setText(resourceMap.getString("configureInfoLabel.text")); // NOI18N
      configureInfoLabel.setName("configureInfoLabel"); // NOI18N

      authenticationKeyField.setText(resourceMap.getString("authenticationKeyField.text")); // NOI18N
      authenticationKeyField.setToolTipText(resourceMap.getString("authenticationKeyField.toolTipText")); // NOI18N
      authenticationKeyField.setName("authenticationKeyField"); // NOI18N
      authenticationKeyField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onConfigFieldChanged(evt);
         }
      });

      confAuthKeyField.setText(resourceMap.getString("confAuthKeyField.text")); // NOI18N
      confAuthKeyField.setToolTipText(resourceMap.getString("confAuthKeyField.toolTipText")); // NOI18N
      confAuthKeyField.setName("confAuthKeyField"); // NOI18N
      confAuthKeyField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onConfigFieldChanged(evt);
         }
      });

      browseUploadDirButton.setText(resourceMap.getString("browseUploadDirButton.text")); // NOI18N
      browseUploadDirButton.setName("browseUploadDirButton"); // NOI18N

      useProxyField.setText(resourceMap.getString("useProxyField.text")); // NOI18N
      useProxyField.setToolTipText(resourceMap.getString("useProxyField.toolTipText")); // NOI18N
      useProxyField.setName("useProxyField"); // NOI18N
      useProxyField.setSelected(proxyEnabled);
      useProxyField.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            useProxyFieldItemStateChanged(evt);
         }
      });
      useProxyField.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            useProxyFieldActionPerformed(evt);
         }
      });

      proxyPortLabel.setForeground(resourceMap.getColor("proxyPortLabel.foreground")); // NOI18N
      proxyPortLabel.setText(resourceMap.getString("proxyPortLabel.text")); // NOI18N
      proxyPortLabel.setToolTipText(resourceMap.getString("proxyPortLabel.toolTipText")); // NOI18N
      proxyPortLabel.setName("proxyPortLabel"); // NOI18N
      proxyPortLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            proxyPortLabelMouseClicked(evt);
         }
      });

      proxyPortField.setText(resourceMap.getString("proxyPortField.text")); // NOI18N
      proxyPortField.setToolTipText(resourceMap.getString("proxyPortField.toolTipText")); // NOI18N
      proxyPortField.setEnabled(false);
      proxyPortField.setName("proxyPortField"); // NOI18N
      proxyPortField.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            proxyPortFieldMouseClicked(evt);
         }
      });
      proxyPortField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
         public void propertyChange(java.beans.PropertyChangeEvent evt) {
            proxyPortFieldPropertyChange(evt);
         }
      });
      proxyPortField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onAdvancedConfigFieldChanged(evt);
         }
      });

      proxyHostLabel.setForeground(resourceMap.getColor("proxyHostLabel.foreground")); // NOI18N
      proxyHostLabel.setText(resourceMap.getString("proxyHostLabel.text")); // NOI18N
      proxyHostLabel.setToolTipText(resourceMap.getString("proxyHostLabel.toolTipText")); // NOI18N
      proxyHostLabel.setName("proxyHostLabel"); // NOI18N
      proxyHostLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            proxyHostLabelMouseClicked(evt);
         }
      });

      proxyHostField.setText(resourceMap.getString("proxyHostField.text")); // NOI18N
      proxyHostField.setToolTipText(resourceMap.getString("proxyHostField.toolTipText")); // NOI18N
      proxyHostField.setEnabled(false);
      proxyHostField.setName("proxyHostField"); // NOI18N
      proxyHostField.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            proxyHostFieldMouseClicked(evt);
         }
      });
      proxyHostField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onAdvancedConfigFieldChanged(evt);
         }
      });

      proxyUserLabel.setForeground(resourceMap.getColor("proxyUserLabel.foreground")); // NOI18N
      proxyUserLabel.setText(resourceMap.getString("proxyUserLabel.text")); // NOI18N
      proxyUserLabel.setToolTipText(resourceMap.getString("proxyUserLabel.toolTipText")); // NOI18N
      proxyUserLabel.setName("proxyUserLabel"); // NOI18N

      proxyUserField.setToolTipText(resourceMap.getString("proxyUserField.toolTipText")); // NOI18N
      proxyUserField.setEnabled(false);
      proxyUserField.setName("proxyUserField"); // NOI18N
      proxyUserField.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            proxyUserFieldMouseClicked(evt);
         }
      });
      proxyUserField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onAdvancedConfigFieldChanged(evt);
         }
      });

      proxyPwLabel.setForeground(resourceMap.getColor("proxyPwLabel.foreground")); // NOI18N
      proxyPwLabel.setText(resourceMap.getString("proxyPwLabel.text")); // NOI18N
      proxyPwLabel.setToolTipText(resourceMap.getString("proxyPwLabel.toolTipText")); // NOI18N
      proxyPwLabel.setName("proxyPwLabel"); // NOI18N

      proxyPwField.setToolTipText(resourceMap.getString("proxyPwField.toolTipText")); // NOI18N
      proxyPwField.setEnabled(false);
      proxyPwField.setName("proxyPwField"); // NOI18N
      proxyPwField.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            proxyPwFieldMouseClicked(evt);
         }
      });
      proxyPwField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            onAdvancedConfigFieldChanged(evt);
         }
      });

      detectProxyButton.setText(resourceMap.getString("detectProxyButton.text")); // NOI18N
      detectProxyButton.setActionCommand(resourceMap.getString("detectProxyButton.actionCommand")); // NOI18N
      detectProxyButton.setName("detectProxyButton"); // NOI18N
      detectProxyButton.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            detectProxyButtonMouseClicked(evt);
         }
      });

      proxyDetectedLabel.setText(resourceMap.getString("proxyDetectedLabel.text")); // NOI18N
      proxyDetectedLabel.setName("proxyDetectedLabel"); // NOI18N

      org.jdesktop.layout.GroupLayout configurePanelLayout = new org.jdesktop.layout.GroupLayout(configurePanel);
      configurePanel.setLayout(configurePanelLayout);
      configurePanelLayout.setHorizontalGroup(
         configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(configurePanelLayout.createSequentialGroup()
            .add(10, 10, 10)
            .add(mainConfigureLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 566, Short.MAX_VALUE))
         .add(configurePanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
               .add(knURLLabel)
               .add(schoolMOENumberLabel))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(knURLField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 230, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(schoolMOENumberField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(169, Short.MAX_VALUE))
         .add(configurePanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(configurePanelLayout.createSequentialGroup()
                  .add(22, 22, 22)
                  .add(confAuthKeyLabel)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(confAuthKeyField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 145, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
               .add(configurePanelLayout.createSequentialGroup()
                  .add(62, 62, 62)
                  .add(authenticationKeyLabel)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(authenticationKeyField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 145, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
               .add(configSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 465, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(101, Short.MAX_VALUE))
         .add(configurePanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(configurePanelLayout.createSequentialGroup()
                  .add(73, 73, 73)
                  .add(uploadDirLabel)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(uploadDirField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(browseUploadDirButton))
               .add(configurePanelLayout.createSequentialGroup()
                  .add(configSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 465, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
            .add(21, 21, 21))
         .add(configurePanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(configureInfoLabel)
            .addContainerGap(290, Short.MAX_VALUE))
         .add(org.jdesktop.layout.GroupLayout.TRAILING, configurePanelLayout.createSequentialGroup()
            .addContainerGap(402, Short.MAX_VALUE)
            .add(configureSaveButton)
            .add(117, 117, 117))
         .add(configurePanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(configurePanelLayout.createSequentialGroup()
                  .add(useProxyField)
                  .add(10, 10, 10)
                  .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                     .add(proxyPwLabel)
                     .add(proxyUserLabel)
                     .add(proxyHostLabel)
                     .add(proxyPortLabel))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(proxyPortField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(proxyPwField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 146, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(proxyHostField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 230, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(proxyUserField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 146, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(configurePanelLayout.createSequentialGroup()
                        .add(detectProxyButton)
                        .add(10, 10, 10)
                        .add(proxyDetectedLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE))))
               .add(configSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE))
            .addContainerGap())
      );
      configurePanelLayout.setVerticalGroup(
         configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(configurePanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainConfigureLabel)
            .add(18, 18, 18)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(schoolMOENumberField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(schoolMOENumberLabel))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(knURLLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(knURLField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(configSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(12, 12, 12)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(authenticationKeyField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(authenticationKeyLabel))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(confAuthKeyField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(confAuthKeyLabel))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
            .add(configSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(browseUploadDirButton)
               .add(uploadDirField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(uploadDirLabel))
            .add(18, 18, 18)
            .add(configSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(8, 8, 8)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(useProxyField)
               .add(configurePanelLayout.createSequentialGroup()
                  .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(proxyPortField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(proxyPortLabel))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(proxyHostField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(proxyHostLabel))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                  .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(proxyUserLabel)
                     .add(proxyUserField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(proxyPwLabel)
                     .add(proxyPwField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
            .add(18, 18, 18)
            .add(configurePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(detectProxyButton)
               .add(proxyDetectedLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(20, 20, 20)
            .add(configureInfoLabel)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(configureSaveButton)
            .add(136, 136, 136))
      );

      detectProxyButton.getAccessibleContext().setAccessibleName(resourceMap.getString("jButton1.AccessibleContext.accessibleName")); // NOI18N
      proxyDetectedLabel.getAccessibleContext().setAccessibleName(resourceMap.getString("proxyDetectedLabel.AccessibleContext.accessibleName")); // NOI18N

      mainTabbedPane.addTab(resourceMap.getString("configurePanel.TabConstraints.tabTitle"), resourceMap.getIcon("configurePanel.TabConstraints.tabIcon"), configurePanel, resourceMap.getString("configurePanel.TabConstraints.tabToolTip")); // NOI18N

      advancedPanel.setName("advancedPanel"); // NOI18N
      advancedPanel.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            smtpPasswordTextFieldKeyTyped(evt);
         }
      });

      mainAdvancedlabel.setIcon(resourceMap.getIcon("mainAdvancedlabel.icon")); // NOI18N
      mainAdvancedlabel.setText(resourceMap.getString("mainAdvancedlabel.text")); // NOI18N
      mainAdvancedlabel.setName("mainAdvancedlabel"); // NOI18N

      configureSeparator1.setName("configureSeparator1"); // NOI18N

      advancedSaveButton.setText(resourceMap.getString("advancedSaveButton.text")); // NOI18N
      advancedSaveButton.setEnabled(false);
      advancedSaveButton.setName("advancedSaveButton"); // NOI18N
      advancedSaveButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            advancedSaveButtonActionPerformed(evt);
         }
      });

      configProcessUploadGroup.add(processUploadNonSchoolHoursButton);
      processUploadNonSchoolHoursButton.setText(resourceMap.getString("processUploadNonSchoolHoursButton.text")); // NOI18N
      processUploadNonSchoolHoursButton.setToolTipText(resourceMap.getString("processUploadNonSchoolHoursButton.toolTipText")); // NOI18N
      processUploadNonSchoolHoursButton.setName("processUploadNonSchoolHoursButton"); // NOI18N
      processUploadNonSchoolHoursButton.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            processUploadButtonChanged(evt);
         }
      });

      configProcessUploadGroup.add(processUploadSchoolHoursButton);
      processUploadSchoolHoursButton.setText(resourceMap.getString("processUploadSchoolHoursButton.text")); // NOI18N
      processUploadSchoolHoursButton.setToolTipText(resourceMap.getString("processUploadSchoolHoursButton.toolTipText")); // NOI18N
      processUploadSchoolHoursButton.setName("processUploadSchoolHoursButton"); // NOI18N
      processUploadSchoolHoursButton.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            processUploadButtonChanged(evt);
         }
      });

      configProcessUploadGroup.add(processUploadAlwaysButton);
      processUploadAlwaysButton.setSelected(true);
      processUploadAlwaysButton.setText(resourceMap.getString("processUploadAlwaysButton.text")); // NOI18N
      processUploadAlwaysButton.setToolTipText(resourceMap.getString("processUploadAlwaysButton.toolTipText")); // NOI18N
      processUploadAlwaysButton.setName("processUploadAlwaysButton"); // NOI18N
      processUploadAlwaysButton.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            processUploadButtonChanged(evt);
         }
      });

      processUploadLabel.setText(resourceMap.getString("processUploadLabel.text")); // NOI18N
      processUploadLabel.setToolTipText(resourceMap.getString("processUploadLabel.toolTipText")); // NOI18N
      processUploadLabel.setName("processUploadLabel"); // NOI18N

      failedUploadLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
      failedUploadLabel.setText(resourceMap.getString("failedUploadLabel.text")); // NOI18N
      failedUploadLabel.setToolTipText(resourceMap.getString("failedUploadLabel.toolTipText")); // NOI18N
      failedUploadLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
      failedUploadLabel.setName("failedUploadLabel"); // NOI18N
      failedUploadLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            failedUploadLabelMouseClicked(evt);
         }
      });

      failedUploadSpinner.setFont(resourceMap.getFont("failedUploadSpinner.font")); // NOI18N
      failedUploadSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1440, 1));
      failedUploadSpinner.setToolTipText(resourceMap.getString("failedUploadSpinner.toolTipText")); // NOI18N
      failedUploadSpinner.setName("failedUploadSpinner"); // NOI18N
      failedUploadSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
         public void stateChanged(javax.swing.event.ChangeEvent evt) {
            failedUploadSpinnerStateChanged(evt);
         }
      });

      failedUploadLabel2.setText(resourceMap.getString("failedUploadLabel2.text")); // NOI18N
      failedUploadLabel2.setName("failedUploadLabel2"); // NOI18N

      emailTextField.setText(resourceMap.getString("emailTextField.text")); // NOI18N
      emailTextField.setToolTipText(resourceMap.getString("emailTextField.toolTipText")); // NOI18N
      emailTextField.setName("emailTextField"); // NOI18N
      emailTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            emailTextFieldKeyTyped(evt);
         }
      });

      emailLabel.setText(resourceMap.getString("emailLabel.text")); // NOI18N
      emailLabel.setToolTipText(resourceMap.getString("emailLabel.toolTipText")); // NOI18N
      emailLabel.setName("emailLabel"); // NOI18N
      emailLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            emailLabelMouseClicked(evt);
         }
      });

      smtpHostLabel.setText(resourceMap.getString("smtpHostLabel.text")); // NOI18N
      smtpHostLabel.setToolTipText(resourceMap.getString("smtpHostLabel.toolTipText")); // NOI18N
      smtpHostLabel.setName("smtpHostLabel"); // NOI18N
      smtpHostLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            smtpHostLabelMouseClicked(evt);
         }
      });

      smtpHostTextField.setText(resourceMap.getString("smtpHostTextField.text")); // NOI18N
      smtpHostTextField.setToolTipText(resourceMap.getString("smtpHostTextField.toolTipText")); // NOI18N
      smtpHostTextField.setName("smtpHostTextField"); // NOI18N
      smtpHostTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            smtpHostTextFieldKeyTyped(evt);
         }
      });

      smtpUserLabel.setText(resourceMap.getString("smtpUserLabel.text")); // NOI18N
      smtpUserLabel.setToolTipText(resourceMap.getString("smtpUserLabel.toolTipText")); // NOI18N
      smtpUserLabel.setName("smtpUserLabel"); // NOI18N
      smtpUserLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            smtpUserLabelMouseClicked(evt);
         }
      });

      smtpPasswordLabel.setText(resourceMap.getString("smtpPasswordLabel.text")); // NOI18N
      smtpPasswordLabel.setToolTipText(resourceMap.getString("smtpPasswordLabel.toolTipText")); // NOI18N
      smtpPasswordLabel.setName("smtpPasswordLabel"); // NOI18N
      smtpPasswordLabel.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            smtpPasswordLabelMouseClicked(evt);
         }
      });

      smtpUserTextField.setText(resourceMap.getString("smtpUserTextField.text")); // NOI18N
      smtpUserTextField.setName("smtpUserTextField"); // NOI18N
      smtpUserTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            smtpUserTextFieldKeyTyped(evt);
         }
      });

      jSeparator1.setName("jSeparator1"); // NOI18N

      jSeparator2.setName("jSeparator2"); // NOI18N

      smtpPasswordField.setText(resourceMap.getString("smtpPasswordField.text")); // NOI18N
      smtpPasswordField.setToolTipText(resourceMap.getString("smtpPasswordField.toolTipText")); // NOI18N
      smtpPasswordField.setName("smtpPasswordField"); // NOI18N
      smtpPasswordField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyTyped(java.awt.event.KeyEvent evt) {
            smtpPasswordTextFieldKeyTyped(evt);
         }
      });

      org.jdesktop.layout.GroupLayout advancedPanelLayout = new org.jdesktop.layout.GroupLayout(advancedPanel);
      advancedPanel.setLayout(advancedPanelLayout);
      advancedPanelLayout.setHorizontalGroup(
         advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(advancedPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(mainAdvancedlabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 566, Short.MAX_VALUE)
               .add(advancedPanelLayout.createSequentialGroup()
                  .add(configureSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 465, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                  .addContainerGap(101, Short.MAX_VALUE))
               .add(advancedPanelLayout.createSequentialGroup()
                  .add(processUploadLabel)
                  .add(29, 29, 29)
                  .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(processUploadNonSchoolHoursButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                     .add(processUploadSchoolHoursButton)
                     .add(processUploadAlwaysButton))
                  .addContainerGap())
               .add(advancedPanelLayout.createSequentialGroup()
                  .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                     .add(advancedPanelLayout.createSequentialGroup()
                        .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(advancedSaveButton)
                        .add(102, 102, 102))
                     .add(advancedPanelLayout.createSequentialGroup()
                        .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                           .add(jSeparator2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
                           .add(advancedPanelLayout.createSequentialGroup()
                              .add(243, 243, 243)
                              .add(failedUploadLabel2)
                              .add(41, 41, 41)
                              .add(failedUploadSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                           .add(advancedPanelLayout.createSequentialGroup()
                              .add(85, 85, 85)
                              .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                 .add(failedUploadLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                 .add(advancedPanelLayout.createSequentialGroup()
                                    .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                       .add(smtpUserLabel)
                                       .add(smtpPasswordLabel))
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                                 .add(advancedPanelLayout.createSequentialGroup()
                                    .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                       .add(smtpHostLabel)
                                       .add(emailLabel))
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                              .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                 .add(advancedPanelLayout.createSequentialGroup()
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(emailTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))
                                 .add(smtpUserTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                                 .add(org.jdesktop.layout.GroupLayout.TRAILING, smtpHostTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                                 .add(org.jdesktop.layout.GroupLayout.TRAILING, smtpPasswordField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))))
                        .add(35, 35, 35)))
                  .add(33, 33, 33))))
      );
      advancedPanelLayout.setVerticalGroup(
         advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(advancedPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainAdvancedlabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(configureSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(processUploadLabel)
               .add(processUploadAlwaysButton))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(processUploadSchoolHoursButton)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(processUploadNonSchoolHoursButton)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                  .add(failedUploadLabel2)
                  .add(failedUploadSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
               .add(failedUploadLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(emailTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(emailLabel))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(smtpHostTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(smtpHostLabel))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(smtpUserLabel)
               .add(smtpUserTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(smtpPasswordLabel)
               .add(smtpPasswordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(129, 129, 129)
            .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(advancedSaveButton))
            .add(64, 64, 64))
      );

      mainTabbedPane.addTab(resourceMap.getString("advancedPanel.TabConstraints.tabTitle"), resourceMap.getIcon("advancedPanel.TabConstraints.tabIcon"), advancedPanel); // NOI18N

      viewLogPanel.setName("viewLogPanel"); // NOI18N

      mainViewLogLabel.setIcon(resourceMap.getIcon("mainViewLogLabel.icon")); // NOI18N
      mainViewLogLabel.setText(resourceMap.getString("mainViewLogLabel.text")); // NOI18N
      mainViewLogLabel.setName("mainViewLogLabel"); // NOI18N

      viewLogScrollPane.setName("viewLogScrollPane"); // NOI18N

      viewLogTextPane.setName("viewLogTextPane"); // NOI18N
      viewLogScrollPane.setViewportView(viewLogTextPane);
      viewLogTextPane.setText(VIEW_LOG_INITIAL);

      viewLogInfoLabel.setText(resourceMap.getString("viewLogInfoLabel.text")); // NOI18N
      viewLogInfoLabel.setName("viewLogInfoLabel"); // NOI18N

      emailLogsButton.setText(resourceMap.getString("emailLogsButton.text")); // NOI18N
      emailLogsButton.setName("emailLogsButton"); // NOI18N
      emailLogsButton.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            emailLogsMouseClicked(evt);
         }
      });

      org.jdesktop.layout.GroupLayout viewLogPanelLayout = new org.jdesktop.layout.GroupLayout(viewLogPanel);
      viewLogPanel.setLayout(viewLogPanelLayout);
      viewLogPanelLayout.setHorizontalGroup(
         viewLogPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(viewLogPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(viewLogPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(viewLogInfoLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
               .add(mainViewLogLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
               .add(emailLogsButton)
               .add(viewLogScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 547, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
      );
      viewLogPanelLayout.setVerticalGroup(
         viewLogPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(viewLogPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainViewLogLabel)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(viewLogInfoLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(viewLogScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 375, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(26, 26, 26)
            .add(emailLogsButton)
            .addContainerGap(90, Short.MAX_VALUE))
      );

      mainTabbedPane.addTab(resourceMap.getString("viewLogPanel.TabConstraints.tabTitle"), resourceMap.getIcon("viewLogPanel.TabConstraints.tabIcon"), viewLogPanel, resourceMap.getString("viewLogPanel.TabConstraints.tabToolTip")); // NOI18N

      aboutPanel.setName("aboutPanel"); // NOI18N

      mainAboutLabel.setIcon(resourceMap.getIcon("mainAboutLabel.icon")); // NOI18N
      mainAboutLabel.setText(resourceMap.getString("mainAboutLabel.text")); // NOI18N
      mainAboutLabel.setName("mainAboutLabel"); // NOI18N

      aboutScrollPane.setName("aboutScrollPane"); // NOI18N

      aboutTextPane.setContentType(resourceMap.getString("aboutTextPane.contentType")); // NOI18N
      aboutTextPane.setEditable(false);
      aboutTextPane.setText(resourceMap.getString("aboutTextPane.text")); // NOI18N
      aboutTextPane.setMinimumSize(new java.awt.Dimension(532, 9882));
      aboutTextPane.setName("aboutTextPane"); // NOI18N
      aboutScrollPane.setViewportView(aboutTextPane);
      aboutTextPane.getAccessibleContext().setAccessibleDescription(resourceMap.getString("aboutTextPane.AccessibleContext.accessibleDescription")); // NOI18N

      org.jdesktop.layout.GroupLayout aboutPanelLayout = new org.jdesktop.layout.GroupLayout(aboutPanel);
      aboutPanel.setLayout(aboutPanelLayout);
      aboutPanelLayout.setHorizontalGroup(
         aboutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(org.jdesktop.layout.GroupLayout.TRAILING, aboutPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(aboutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
               .add(org.jdesktop.layout.GroupLayout.LEADING, aboutScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
               .add(org.jdesktop.layout.GroupLayout.LEADING, mainAboutLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE))
            .addContainerGap())
      );
      aboutPanelLayout.setVerticalGroup(
         aboutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(aboutPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainAboutLabel)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(aboutScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 540, Short.MAX_VALUE))
      );

      aboutScrollPane.getViewport().setViewPosition(new java.awt.Point(0, 0));

      mainTabbedPane.addTab(resourceMap.getString("aboutPanel.TabConstraints.tabTitle"), resourceMap.getIcon("aboutPanel.TabConstraints.tabIcon"), aboutPanel, resourceMap.getString("aboutPanel.TabConstraints.tabToolTip")); // NOI18N

      org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
      mainPanel.setLayout(mainPanelLayout);
      mainPanelLayout.setHorizontalGroup(
         mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(mainPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 581, Short.MAX_VALUE)
            .addContainerGap())
      );
      mainPanelLayout.setVerticalGroup(
         mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(mainPanelLayout.createSequentialGroup()
            .addContainerGap()
            .add(mainTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 632, Short.MAX_VALUE)
            .addContainerGap(52, Short.MAX_VALUE))
      );

      dirFileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
      dirFileChooser.setName("dirFileChooser"); // NOI18N

      setComponent(mainPanel);
   }// </editor-fold>//GEN-END:initComponents

   private void schoolMOENumberLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_schoolMOENumberLabelMouseClicked
      schoolMOENumberField.grabFocus();
   }//GEN-LAST:event_schoolMOENumberLabelMouseClicked

   private void knURLLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_knURLLabelMouseClicked
      knURLField.grabFocus();
   }//GEN-LAST:event_knURLLabelMouseClicked

   private void useProxyFieldItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_useProxyFieldItemStateChanged
      proxyEnabled = evt.getStateChange() == java.awt.event.ItemEvent.SELECTED;
      proxyFieldsUpdate(proxyEnabled);
   }//GEN-LAST:event_useProxyFieldItemStateChanged

   private void proxyFieldsUpdate(boolean proxyEnabled) {
      java.awt.Color color;
      if (proxyEnabled) {
         color = java.awt.Color.BLACK;
      } else {
         color = proxyHostField.getDisabledTextColor();
      }

      proxyHostField.setEnabled(proxyEnabled);
      proxyPortField.setEnabled(proxyEnabled);
      proxyUserField.setEnabled(proxyEnabled);
      proxyPwField.setEnabled(proxyEnabled);
      proxyHostLabel.setForeground(color);
      proxyPortLabel.setForeground(color);
      proxyUserLabel.setForeground(color);
      proxyPwLabel.setForeground(color);

      if (initialConfigDone) {
         configureSaveButton.setEnabled(true);
      }

      // flag to require restart of background service
      requiresRestart = true;
   }

   private void proxyHostLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proxyHostLabelMouseClicked
      if (proxyEnabled) {
         proxyHostField.grabFocus();
      }
}//GEN-LAST:event_proxyHostLabelMouseClicked

   private void proxyPortLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proxyPortLabelMouseClicked
      if (proxyEnabled) {
         proxyPortField.grabFocus();
      }
   }//GEN-LAST:event_proxyPortLabelMouseClicked

   private void uploadDirLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uploadDirLabelMouseClicked
      uploadDirField.grabFocus();
   }//GEN-LAST:event_uploadDirLabelMouseClicked

   private void authenticationKeyLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_authenticationKeyLabelMouseClicked
      authenticationKeyField.grabFocus();
   }//GEN-LAST:event_authenticationKeyLabelMouseClicked

   private void confAuthKeyLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confAuthKeyLabelMouseClicked
      if (initialConfigDone) {
         confAuthKeyField.grabFocus();
      }
   }//GEN-LAST:event_confAuthKeyLabelMouseClicked

   private void saveConfiguration() {
      WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();

      try {
         saveConfig();
      } catch (java.io.IOException e) {
         showErrorDialog("Invalid Configurations", "Fatal error: could not save configuration file.  Changes were not saved.");
         String level = "WARN";
         String message = "Could not save configuration file: " + e.getMessage() + ", " + e.getStackTrace();
         LogWriter worker = app.logWriteService(isUp, message, level);
         app.getContext().getTaskService().execute(worker);
         return;
      }

      String level = "INFO", message = "Configuration saved via GUI", confirm = "";
      if (requiresRestart) {
         // restart the background service
         ServiceRestarter restarter = app.restartService();
         app.getContext().getTaskService().execute(restarter);

         confirm = ", restart of WebSYNC in progress";
         message += ", restart attempted";
      }

      showNoticeDialog("Configuration saved", "Configurations saved" + confirm);

      configureSaveButton.setEnabled(false);
      advancedSaveButton.setEnabled(false);
      LogWriter worker = app.logWriteService(isUp, message, level);
      app.getContext().getTaskService().execute(worker);
   }
   private void configureSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureSaveButtonActionPerformed
      // verify all input fields
      String configErrors = verifyConfigInput();
      String advancedErrors = verifyAdvancedInput();

      if (configErrors.length() == 0 && advancedErrors.length() == 0) {
         saveConfiguration();
			proxyDetectedLabel.setText("");
      } else {
         if (configErrors.length() != 0) {
            showErrorDialog("Invalid Configurations", configErrors);
         } else {
            showErrorDialog("Invalid Configurations", "There are errors on the advanced tab. Please correct these first:\n\n" + advancedErrors);
         }
      }
   }//GEN-LAST:event_configureSaveButtonActionPerformed

   private void onConfigFieldChanged(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_onConfigFieldChanged
      if (!configureSaveButton.isEnabled() && !evt.isActionKey()) {
         // flag to require restart
         requiresRestart = fieldChangeRequiresRestart(evt);

//	 if (evt.getSource() == authenticationKeyField) {
//	    confAuthKeyField.setEnabled(true);
//	 }
         // a configuration was modified, lets enable the save button
         configureSaveButton.setEnabled(true);
      }
}//GEN-LAST:event_onConfigFieldChanged

   private void processUploadButtonChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_processUploadButtonChanged
      advancedSaveButton.setEnabled(true);
   }//GEN-LAST:event_processUploadButtonChanged

   private void mainTabbedPanePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_mainTabbedPanePropertyChange
      /*if (mainTabbedPane.getSelectedIndex() == 2) {
      viewLogTimer.start();
      } else {
      viewLogTimer.stop();
      }*/
   }//GEN-LAST:event_mainTabbedPanePropertyChange

private void advancedSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_advancedSaveButtonActionPerformed
   // verify all input fields
   String configErrors = verifyConfigInput();
   String advancedErrors = verifyAdvancedInput();

   if (configErrors.length() == 0 && advancedErrors.length() == 0) {
      saveConfiguration();
   } else {
      if (advancedErrors.length() != 0) {
         showErrorDialog("Invalid Configurations", advancedErrors);
      } else {
         showErrorDialog("Invalid Configurations", "There are errors on the advanced tab. Please correct these first:\n\n" + configErrors);
      }
   }
}//GEN-LAST:event_advancedSaveButtonActionPerformed

private void onAdvancedConfigFieldChanged(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_onAdvancedConfigFieldChanged
   if (!advancedSaveButton.isEnabled() && !evt.isActionKey()) {
      // flag to require restart
      requiresRestart = fieldChangeRequiresRestart(evt);

//	 if (evt.getSource() == authenticationKeyField) {
//	    confAuthKeyField.setEnabled(true);
//	 }
      // a configuration was modified, lets enable the save button
      advancedSaveButton.setEnabled(true);
   }

}//GEN-LAST:event_onAdvancedConfigFieldChanged

private void failedUploadSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_failedUploadSpinnerStateChanged
   smtpEnabled = ((Integer) failedUploadSpinner.getValue() > 0);

   advancedSaveButton.setEnabled(true);
}//GEN-LAST:event_failedUploadSpinnerStateChanged

private void emailLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_emailLabelMouseClicked
   emailTextField.grabFocus();
}//GEN-LAST:event_emailLabelMouseClicked

private void smtpHostLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_smtpHostLabelMouseClicked
   smtpHostTextField.grabFocus();
}//GEN-LAST:event_smtpHostLabelMouseClicked

private void smtpUserLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_smtpUserLabelMouseClicked
   smtpUserTextField.grabFocus();
}//GEN-LAST:event_smtpUserLabelMouseClicked

private void smtpPasswordLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_smtpPasswordLabelMouseClicked
   smtpPasswordField.grabFocus();
}//GEN-LAST:event_smtpPasswordLabelMouseClicked

private void failedUploadLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_failedUploadLabelMouseClicked
   failedUploadSpinner.grabFocus();
}//GEN-LAST:event_failedUploadLabelMouseClicked

private void emailTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_emailTextFieldKeyTyped
   advancedSaveButton.setEnabled(true);
}//GEN-LAST:event_emailTextFieldKeyTyped

private void smtpHostTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_smtpHostTextFieldKeyTyped
   advancedSaveButton.setEnabled(true);
}//GEN-LAST:event_smtpHostTextFieldKeyTyped

private void smtpUserTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_smtpUserTextFieldKeyTyped
   advancedSaveButton.setEnabled(true);
}//GEN-LAST:event_smtpUserTextFieldKeyTyped

private void smtpPasswordTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_smtpPasswordTextFieldKeyTyped
   advancedSaveButton.setEnabled(true);
}//GEN-LAST:event_smtpPasswordTextFieldKeyTyped

private void testConnectionButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_testConnectionButtonMouseClicked
   testConnectionButton.setEnabled(false);
}//GEN-LAST:event_testConnectionButtonMouseClicked

private void detectProxyButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_detectProxyButtonMouseClicked
   detectProxy();

}//GEN-LAST:event_detectProxyButtonMouseClicked

private void emailLogsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_emailLogsMouseClicked
   //WebSYNCClientGUIApp app = WebSYNCClientGUIApp.getApplication();
   //MessageSender worker = app.sendMessage("email_logs", "Emailing logs");
   //app.getContext().getTaskService().execute(worker);
	emailLogs();
}//GEN-LAST:event_emailLogsMouseClicked
   private void emailLogs() {
		if(showConfirmDialog("Are You Sure?","This will send all logs and config files")==javax.swing.JOptionPane.OK_OPTION)
		try {
			File root = null;
			File[] logs = null;
			File[] configs = null;
			root = new File("../logs");
			if (root.isDirectory() && root.canRead()) {
				logs = root.listFiles();
			}
			root = new File("../config");
			if (root.isDirectory() && root.canRead()) {
				configs = root.listFiles();
			}
			File[] files = null;
			if(logs!=null && configs!=null)
			{
				files=new File[logs.length+configs.length];
				int j=0;
				for(int i=0;i<logs.length;i++)
				{
					files[j++]=logs[i];
				}
				for(int i=0;i<configs.length;i++)
				{
					files[j++]=configs[i];
				}
			}
         String content = "Log files and configuration of websync attached.\n";
         String subject = "Log files and configuration of websync";
         File zipFile = EmailUtils.zipFiles(files);
         MimeMessage msg = EmailUtils.createEmailWithAttachment(client.getEmail(), "websynclient@dataview.co.nz", subject, content, zipFile);
         EmailUtils.sendEmail(msg, client.getSmtpHost(), client.getSmtpUser(), client.getSmtpPassword());
         zipFile.delete();
			showNoticeDialog("Confirmation", "Logs emailed");
      } catch (AddressException ex) {
			showErrorDialog("Error", "Could not send email. Have you set up the SMTP settings in the Advanced tab?");
      } catch (MessagingException ex) {
			showErrorDialog("Error", "Could not send email. Have you set up the SMTP settings in the Advanced tab?");
      } catch (IOException ex) {
			showErrorDialog("Error", "Could not read logs and config files.");
      }
   }


private void useProxyFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useProxyFieldActionPerformed
	proxyDetectedLabel.setText("");
}//GEN-LAST:event_useProxyFieldActionPerformed

private void proxyPortFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proxyPortFieldMouseClicked
	proxyDetectedLabel.setText("");
}//GEN-LAST:event_proxyPortFieldMouseClicked

private void proxyPortFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_proxyPortFieldPropertyChange
	// TODO add your handling code here:
}//GEN-LAST:event_proxyPortFieldPropertyChange

private void proxyHostFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proxyHostFieldMouseClicked
	proxyDetectedLabel.setText("");
}//GEN-LAST:event_proxyHostFieldMouseClicked

private void proxyUserFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proxyUserFieldMouseClicked
	proxyDetectedLabel.setText("");
}//GEN-LAST:event_proxyUserFieldMouseClicked

private void proxyPwFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proxyPwFieldMouseClicked
	proxyDetectedLabel.setText("");
}//GEN-LAST:event_proxyPwFieldMouseClicked

/**
 * try to detect if a proxy exists, and update the UI
 * 
 * @return
 */
private void detectProxy() {

   try {
         proxyDetectedLabel.setText("");
			List l = ProxySelector.getDefault().select(
                     new URI(knURLField.getText()));

         for (Iterator iter = l.iterator(); iter.hasNext(); ) {
            Proxy proxy = (Proxy) iter.next();
            if ("DIRECT".equals(proxy.type().toString())) {
					proxyEnabled = false;
               useProxyField.setSelected(proxyEnabled);
					proxyFieldsUpdate(proxyEnabled);
				} else
				{
               InetSocketAddress addr = (InetSocketAddress)
                    proxy.address();
               if(addr != null) {
                   // if we have a proxy set the correct fields and state of UI
                   proxyEnabled = true;
                   useProxyField.setSelected(proxyEnabled);
                   proxyHostField.setText(addr.getHostName());
                   proxyPortField.setText(Integer.toString(addr.getPort()));
                   proxyFieldsUpdate(proxyEnabled);

               }
            }
				String text=(String)"Proxy settings detected and updated";
				proxyDetectedLabel.setText(text);
         }
   } catch (Exception e) {
         showErrorDialog("Detect Proxy", "Problem in detecting proxy, please set proxy settings manually");
   } 
   
}
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JPanel aboutPanel;
   private javax.swing.JScrollPane aboutScrollPane;
   private javax.swing.JTextPane aboutTextPane;
   private javax.swing.JPanel advancedPanel;
   private javax.swing.JButton advancedSaveButton;
   private javax.swing.JPasswordField authenticationKeyField;
   private javax.swing.JLabel authenticationKeyLabel;
   private javax.swing.JButton browseUploadDirButton;
   private javax.swing.JPasswordField confAuthKeyField;
   private javax.swing.JLabel confAuthKeyLabel;
   private javax.swing.ButtonGroup configProcessUploadGroup;
   private javax.swing.JSeparator configSeparator1;
   private javax.swing.JSeparator configSeparator2;
   private javax.swing.JSeparator configSeparator3;
   private javax.swing.JLabel configureInfoLabel;
   private javax.swing.JPanel configurePanel;
   private javax.swing.JButton configureSaveButton;
   private javax.swing.JSeparator configureSeparator1;
   private javax.swing.JPanel connectPanel;
   private javax.swing.JLabel connectPanelCurrentStatusDetailsLabel;
   private javax.swing.JLabel connectPanelCurrentStatusLabel;
   private javax.swing.JLabel connectPanelCurrentStatusOverviewLabel;
   private javax.swing.JLabel connectPanelTestLabel;
   private javax.swing.JSeparator connectSeparator0;
   private javax.swing.JButton detectProxyButton;
   private javax.swing.JFileChooser dirFileChooser;
   private javax.swing.JLabel emailLabel;
   private javax.swing.JButton emailLogsButton;
   private javax.swing.JTextField emailTextField;
   private javax.swing.JLabel failedUploadLabel;
   private javax.swing.JLabel failedUploadLabel2;
   private javax.swing.JSpinner failedUploadSpinner;
   private javax.swing.JPanel jPanel1;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JSeparator jSeparator1;
   private javax.swing.JSeparator jSeparator2;
   private javax.swing.JTextField knURLField;
   private javax.swing.JLabel knURLLabel;
   private javax.swing.JLabel mainAboutLabel;
   private javax.swing.JLabel mainAdvancedlabel;
   private javax.swing.JLabel mainConfigureLabel;
   private javax.swing.JLabel mainConnectLabel;
   private javax.swing.JPanel mainPanel;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JLabel mainViewLogLabel;
   private javax.swing.JRadioButton processUploadAlwaysButton;
   private javax.swing.JLabel processUploadLabel;
   private javax.swing.JRadioButton processUploadNonSchoolHoursButton;
   private javax.swing.JRadioButton processUploadSchoolHoursButton;
   private javax.swing.JProgressBar progressBar;
   private javax.swing.JLabel proxyDetectedLabel;
   private javax.swing.JTextField proxyHostField;
   private javax.swing.JLabel proxyHostLabel;
   private javax.swing.JTextField proxyPortField;
   private javax.swing.JLabel proxyPortLabel;
   private javax.swing.JPasswordField proxyPwField;
   private javax.swing.JLabel proxyPwLabel;
   private javax.swing.JTextField proxyUserField;
   private javax.swing.JLabel proxyUserLabel;
   private javax.swing.JTextField schoolMOENumberField;
   private javax.swing.JLabel schoolMOENumberLabel;
   private javax.swing.JLabel smtpHostLabel;
   private javax.swing.JTextField smtpHostTextField;
   private javax.swing.JPasswordField smtpPasswordField;
   private javax.swing.JLabel smtpPasswordLabel;
   private javax.swing.JLabel smtpUserLabel;
   private javax.swing.JTextField smtpUserTextField;
   private javax.swing.JButton testConnectionButton;
   private javax.swing.JLabel testConnectionResult;
   private javax.swing.JTextField uploadDirField;
   private javax.swing.JLabel uploadDirLabel;
   private javax.swing.JCheckBox useProxyField;
   private javax.swing.JLabel viewLogInfoLabel;
   private javax.swing.JPanel viewLogPanel;
   private javax.swing.JScrollPane viewLogScrollPane;
   private javax.swing.JTextPane viewLogTextPane;
   // End of variables declaration//GEN-END:variables
   private JDialog aboutBox;   // custom vars
   private boolean proxyEnabled;
   private boolean smtpEnabled;
   private WebSYNC client;
   private boolean initialConfigDone;
   private Timer viewLogTimer;
   private Timer statusTimer;
   private boolean isRunning = true;
   private boolean isUp;
   private boolean requiresRestart;
   private static final int MAX_LOG_VIEW_SIZE = 50000;
   private static final String VIEW_LOG_INITIAL = "Loading...";
   private static final String VIEW_LOG_EMPTY = "No new log entries since last view!";
}
