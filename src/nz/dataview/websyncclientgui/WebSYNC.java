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

import java.util.Properties;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;

/**
 * A simple class used to hold the WebSYNC configurations.
 * 
 * @author  William Song, Tim Owens
 * @version 1.1.0
 */
public class WebSYNC {
   /**
    * The system configurations.  <strong>MUST</strong> be initialised before anything else.
    */
   private Properties systemConfig;
   /**
    * Application configurations contain the settings which can be changed via the GUI.
    */
   private Properties appConfig;
   /**
    * The path at which the application configuration file lies.
    */
   private String appConfigFilePath;
   /**
    * The path at which the system configuration file lies.
    */
   private String systemConfigFilePath;
   
   public static final int SCHEDULE_UPLOAD_10_MIN = 0;
   public static final int SCHEDULE_UPLOAD_30_MIN = 1;
   public static final int SCHEDULE_UPLOAD_1_HR = 2;
   public static final int SCHEDULE_UPLOAD_2_HR = 3;
   public static final int SCHEDULE_UPLOAD_4_HR = 4;
   public static final int SCHEDULE_UPLOAD_1_DAY = 5;
   public static final int SCHEDULE_UPLOAD_NEVER = 6;
   
   public static final int PROCESS_TIME_ALWAYS = 0;
   public static final int PROCESS_TIME_SCHOOL_HOURS = 1;
   public static final int PROCESS_TIME_NON_SCHOOL_HOURS = 2;
   
   public WebSYNC() throws IOException {
      systemConfigFilePath = System.getProperty("nz.dataview.websyncclientgui.sysconf_file");
      initialiseConfig();
   }
   
   /**
    * Initialises the application configurations into the <code>Properties</code> object.
    * <strong>This should be the first method call in the constructor.</strong>
    * All methods assume that this is called first before anything else.
    * 
    * @throws  java.io.IOException  thrown if IO error occurred while opening the configuration file
    */
   private void initialiseConfig() throws IOException {
      if (systemConfig == null)
	 systemConfig = new Properties();
      if (appConfig == null)
	 appConfig = new Properties();
      
      int count = 0;
      boolean error;
      do {
	 error = false;
	 try {
	    systemConfig.loadFromXML(new FileInputStream(systemConfigFilePath));
	 } catch (IOException e) {
	    error = true;
	    // possibly file lock from the service, back off and try again
	    try {
	       // sleep for a random number of milliseconds, max 3000
	       Thread.sleep((long)(Math.random() * 3000));	       
	    } catch (InterruptedException ex) {}
	 }
	 count++;
      } while (error && count < 3);
      
      // something bad happened
      if (error && count >= 3)
	 throw new IOException();
      
      appConfigFilePath = systemConfig.getProperty("nz.dataview.websyncclient.appconfig_file");
      
      count = 0;
      do {
	 error = false;
	 try {
	    appConfig.loadFromXML(new FileInputStream(appConfigFilePath));
	 } catch (IOException e) {
	    error = true;
	    try {
	       Thread.sleep((long)(Math.random() * 3000));
	    } catch (InterruptedException ex) {}
	    error = false;
	 }
	 count++;
      } while (error && count < 3);
      
      if (error && count >= 3)
	 throw new IOException();
   }
   
   /**
    * Saves the current application configurations to the config file.
    * Ensure that all configs are up to date - this will overwrite the 
    * existing configuration files!
    * 
    * @throws java.io.IOException   thrown if IO error occurred while opening/closing the configuration file
    */
   public synchronized void saveConfig() throws IOException {
      if (systemConfig != null) {
	 int count = 0;
	 boolean error;
	 do {
	    error = false;
	    try {
	       systemConfig.storeToXML(new FileOutputStream(systemConfigFilePath), null);
	    } catch (IOException e) {
	       error = true;
	       // possibly file lock from the service, back off and try again
	       try {
		  // sleep for a random number of milliseconds, max 3000
		  Thread.sleep((long)(Math.random() * 3000));
	       } catch (InterruptedException ex) {}
	    }
	    count++;
	 } while (error && count < 3);
	 
	 if (error && count >= 3)
	    throw new IOException();
      }
      
      if (appConfig != null) {
	 int count = 0;
	 boolean error;
	 do {
	    error = false;
	    try {
	       appConfig.storeToXML(new FileOutputStream(appConfigFilePath), null);
	    } catch (IOException e) {
	       error = true;
	       try {
		  Thread.sleep((long)(Math.random() * 3000));
	       } catch (InterruptedException ex) {}
	    }
	    count++;
	 } while (error && count < 3);	 
	 
	 if (error && count >= 3)
	    throw new IOException();
      }
   }
   
   public String getUploadDir() {
      return appConfig.getProperty("nz.dataview.websyncclient.upload_dir");
   }
   
   public synchronized void setUploadDir(String dir) {
      appConfig.setProperty("nz.dataview.websyncclient.upload_dir", dir);
   }
   
   public String getControlDir() {
      String ret=appConfig.getProperty(
              "nz.dataview.websyncclient.control_dir");
		if(ret!=null)
		{
			return ret;
		} else
		{
			return "../Control";
		}
   }

   public synchronized void setControlDir(String dir) {
      appConfig.setProperty("nz.dataview.websyncclient.control_dir", dir);
   }

   public boolean getProxyEnabled() {
      return appConfig.getProperty("nz.dataview.websyncclient.use_proxy").equalsIgnoreCase("y");
   }
   
   public synchronized void setProxyEnabled(boolean enabled) {
      String val = enabled ? "y" : "n";
      appConfig.setProperty("nz.dataview.websyncclient.use_proxy", val);
   }

   public boolean isProxyConfigured() {
      return StringUtils.isNotEmpty(appConfig.getProperty("nz.dataview.websyncclient.use_proxy"));
   }

   public String getProxyHost() {
      return appConfig.getProperty("nz.dataview.websyncclient.proxy_host");
   }
   
   public String getProxyUser() {
      return appConfig.getProperty("nz.dataview.websyncclient.proxy_user");
   }
   
   public synchronized void setProxyUser(String user) {
      appConfig.setProperty("nz.dataview.websyncclient.proxy_user", user);
   }
   
   public synchronized void setProxyPw(char[] pw) {
      String stringPw = "";
      for (int i = 0; i < pw.length; i++) {
	 stringPw += pw[i];
      }
      appConfig.setProperty("nz.dataview.websyncclient.proxy_pw", simpleEncrypt(stringPw));
   }
   
   public String getProxyPw() {
      return simpleDecrypt(appConfig.getProperty("nz.dataview.websyncclient.proxy_pw"));
   }
   
   public synchronized void setProxyHost(String host) {
      appConfig.setProperty("nz.dataview.websyncclient.proxy_host", host);
   }
   
   public String getProxyPort() {
      return appConfig.getProperty("nz.dataview.websyncclient.proxy_port");
   }
   
   public synchronized void setProxyPort(int port) {
      appConfig.setProperty("nz.dataview.websyncclient.proxy_port", "" + port);
   }
   
   public String getSchoolMOENo() {
      return appConfig.getProperty("nz.dataview.websyncclient.school_moe_no");
   }
   
   public synchronized void setSchoolMOENo(String num) {
      appConfig.setProperty("nz.dataview.websyncclient.school_moe_no", num);
   }
   
   public String getAuthenticationKey() {
      return simpleDecrypt(appConfig.getProperty("nz.dataview.websyncclient.authentication_key"));
   }
   
   public synchronized void setAuthenticationKey(char[] key) {
      // md5 this?
      String stringKey = "";
      for (int i = 0; i < key.length; i++) {
	 stringKey += key[i];
      }
      appConfig.setProperty("nz.dataview.websyncclient.authentication_key", simpleEncrypt(stringKey));
   }
   
   public int getProcessUploadIndex() {
      int ret = Integer.parseInt(appConfig.getProperty("nz.dataview.websyncclient.process_time", "" + PROCESS_TIME_ALWAYS));
      return ret;
   }
   
   public synchronized void setProcessUploadIndex(int index) {
      appConfig.setProperty("nz.dataview.websyncclient.process_time", "" + index);
   }
   
   public String getKNURL() {
      return appConfig.getProperty("nz.dataview.websyncclient.kn_web_service_url");
   }
   
   public synchronized void setKNURL(String url) {
      appConfig.setProperty("nz.dataview.websyncclient.kn_web_service_url", url);
   }
   
   public String getBlockSize() {
      return systemConfig.getProperty("nz.dataview.websyncclient.upload_byte_limit");
   }
   
   public synchronized void setBlockSize(Integer size) {
      systemConfig.setProperty("nz.dataview.websyncclient.upload_byte_limit", size.toString());
   }

   public Integer getResponseTime()
   {
       String time = systemConfig.getProperty("nz.dataview.websyncclient.response_check_time");
       Integer responseCheckTime = 10;
       if(time!=null)
       {
           try
           {
               responseCheckTime=Integer.parseInt(time);
           } catch(NumberFormatException e)
           {
               //Just set it to default;
           }
       }
       return responseCheckTime;
   }
   public synchronized void setResponseTime(Integer time) {
      appConfig.setProperty("nz.dataview.websyncclient.response_check_time", time.toString());
   }
   public Integer getFailedUploadCount()
   {
       String count = systemConfig.getProperty("nz.dataview.websyncclient.failed_upload_count");
       Integer failedUploadCount = 0;
       if(count!=null)
       {
           try
           {
               failedUploadCount=Integer.parseInt(count);
           } catch(NumberFormatException e)
           {
               //Just set it to default;
           }
       }
       return failedUploadCount;
   }
   public synchronized void setFailedUploadCount(Integer count) {
      appConfig.setProperty("nz.dataview.websyncclient.failed_upload_count", count.toString());
   }
   public String getEmail()
   {
       String text=appConfig.getProperty("nz.dataview.websyncclient.reports_email");
       if(text!=null)
       {
           return text;
       } else
       {
           return "";
       }
   }
   public synchronized void setEmail(String email) {
      appConfig.setProperty("nz.dataview.websyncclient.reports_email", email);
   }
   public String getSmtpHost()
   {
       String text=appConfig.getProperty("nz.dataview.websyncclient.smtp_host");
       if(text!=null)
       {
           return text;
       } else
       {
           return "";
       }
   }
   public synchronized void setSmtpHost(String text) {
      appConfig.setProperty("nz.dataview.websyncclient.smtp_host", text);
   }
   public String getSmtpUser()
   {
       String text=appConfig.getProperty("nz.dataview.websyncclient.smtp_user");
       if(text!=null)
       {
           return text;
       } else
       {
           return "";
       }
   }
   public synchronized void setSmtpUser(String text) {
      appConfig.setProperty("nz.dataview.websyncclient.smtp_user", text);
   }
   public String getSmtpPassword()
   {
       String text=appConfig.getProperty("nz.dataview.websyncclient.smtp_password");
       if(text!=null)
       {
           return simpleDecrypt(text);
       } else
       {
           return "";
       }
   }
   public synchronized void setSmtpPassword(char[] pw) {
      String stringPw = "";
      for (int i = 0; i < pw.length; i++) {
	 stringPw += pw[i];
      }
      appConfig.setProperty("nz.dataview.websyncclient.smtp_password", simpleEncrypt(stringPw));
   }
   
   public String getLogFileLocation() {
       String ret = systemConfig.getProperty("nz.dataview.websyncclient.main_log_file");
      return ret;
   }
   /**
    * Retrieves the successful DNS cache TTL for the JVM from the config.
    * 
    * @return  the number of seconds for the TTL
    */
    public int getDnsCacheTtl() {
        int ttl = 0;
      
        try {
            ttl = Integer.parseInt(systemConfig.getProperty("nz.dataview.websyncclient.dnscachettl", "900"));
        } catch (NumberFormatException e) {
            ttl = 900;
        }
      
        return ttl;
    }
    /**
     * Retrieves the negative (unsuccessful) DNS cache TTL for the JVM from the config.
     * 
     * @return  the number of seconds for the TLL
     */
    public int getNegativeDnsCacheTtl() {
        int ttl = 0;

        try {
            ttl = Integer.parseInt(systemConfig.getProperty("nz.dataview.websyncclient.negativednscachettl", "10"));
        } catch (NumberFormatException e) {
            ttl = 10;
        }
        return ttl;
    }

   /**
    * @return xor of arrays a and b
    */
   public static char[] xor ( char[] a, char[] b )
      {
      int length = Math.min (a.length, b.length );
      char[] result = new char[length];
      for ( int i=0; i<length ; i++ )
         {
         // ^ is the xor operator
         // see http://mindprod.com/jgloss/xor.html
         result[i] = (char) ( a[i] ^ b[i] );
         }
      return result;
      }

   /**
    * A simple obscuring method using XOR. Works both ways.
    *
    * The keystring is base64 - it needs to be a strict multiple of 3 characters
    * (padded with =) and only containing characters valid in base64.
    *
    * If you change it, you need to work out a way to convert all existing
    * passwords too...
    *
    * The key string needs to be 1/3 longer than the longest string to encode.
    * @param cleartext
    * @return
    */
   private static String xorencode(String cleartext,boolean toBase64) {
      String ret;
      char[] encoded;

      String keystring="GoTheD4t4V13wMassive/MaryHadALittleLambTheDoctorWasSuprisedWhenOldMcDonaldHadAFarmTheDoctorNearlyDied=";
      byte[] keybyte = net.iharder.base64.Base64.decode(keystring);
      char[] key= new char[keybyte.length];
      for(int i=0; i<keybyte.length;i++) key[i]=(char)keybyte[i];
          
      encoded = xor( cleartext.toCharArray(), key );

      //Convert to base64 for storing in the config if required
      if(toBase64)
      {
          byte[] encodedbyte = new byte[encoded.length];
          for(int i=0; i<encoded.length;i++) encodedbyte[i]=(byte)encoded[i];
          ret=net.iharder.base64.Base64.encodeBytes(encodedbyte);
      } else
      {
          ret=new String(encoded);
      }
      return ret;
   }

   /**
    * Encode a string for storing in the config. The @# is just a little
    * marker to tell us it's encoded.
    *
    * @param cleartext
    * @return
    */
   private static String simpleEncrypt(String cleartext) {
       return "@#"+xorencode(cleartext,true);
   }

   /**
    * Decode a string. However, it could be a plain text version from an old
    * config, so check for the @# first.
    * 
    * @param ciphertext
    * @return
    */
   private static String simpleDecrypt(String ciphertext) {
       //Need to cater for existing unencrypted strings
      if(ciphertext.length() >= 2 && ciphertext.substring(0,2).equals("@#"))
      {
          byte[] encodedbyte=net.iharder.base64.Base64.decode(ciphertext.substring(2));
          char[] encodedchar= new char[encodedbyte.length];
          for(int i=0; i<encodedbyte.length;i++) encodedchar[i]=(char)encodedbyte[i];
          ciphertext= new String(encodedchar);
          return xorencode(ciphertext,false);
      } else 
      {
          return ciphertext;
      }
   }   

}