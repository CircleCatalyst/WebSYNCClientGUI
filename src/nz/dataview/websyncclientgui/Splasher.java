/**
 * This class was sourced from:
 * http://www.randelshofer.ch/oop/javasplash/javasplash.html
 */
package nz.dataview.websyncclientgui;
/*
 * @(#)Splasher.java  2.0  January 31, 2004
 *
 * Copyright (c) 2003-2004 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * This software is in the public domain.
 *
 * @author  werni
 */
public class Splasher {
   /**
    * Shows the splash screen, launches the application and then disposes
    * the splash screen.
    * @param args the command line arguments
    */
   public static void main(String[] args) {
      SplashWindow.splash(Splasher.class.getResource("websync-splash.png"));
      SplashWindow.invokeMain("nz.dataview.websyncclientgui.WebSYNCClientGUIApp", args);
      SplashWindow.disposeSplash();
   }
}