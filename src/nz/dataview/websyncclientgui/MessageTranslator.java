package nz.dataview.websyncclientgui;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.commons.lang.StringUtils;

/**
 * Simple code that does replace substring with keys on a properties file
 */
class MessageTranslator {

   private static String configBundle = "nz.dataview.websyncclientgui.resources.messagetranslator";
   private static final String START_DELIM = "{";
   private static final String END_DELIM = "}";

   /**
    * search for message keys in the message param, replace
    * them as needed
    * 
    * @param message
    * @return
    */
   public static String translate(String message) {
      String messageKey = StringUtils.substringBetween(message, START_DELIM, END_DELIM);
      if (StringUtils.isEmpty(messageKey)) return message;

      String before = StringUtils.substringBefore(message, START_DELIM);
      String after = StringUtils.substringAfter(message, END_DELIM);
      String messageTranslated = getString(messageKey);
      if (StringUtils.isNotEmpty(messageTranslated))
              return before + messageTranslated + after;
      else
         return message;
   }



  /**
   * Get the config resource bundle with forced reload.
   *
   * @return The reloaded resource bundle
   *
   */
  private static ResourceBundle getBundle() {
    return ResourceBundle.getBundle(configBundle);
  }

  /**
   * Accessor method to retrieve Properties values
   *
   * @param parameter
   *          the name of the parameter as a String
   * @return the value of the parameter in the Properties bundle
   * @throws DataException on any error
   */
  private static String getParameter(String parameter) throws MissingResourceException {

    String val;
    try {
      val = getBundle().getString(parameter).trim();
    }
    catch (MissingResourceException ex) {
      String errStr = "Error in getParameter. Missing resource: " + ex.getKey();
      throw new MissingResourceException(errStr, "config", parameter);
    }
    return val;
  }

  /**
   * Accessor method to retrieve Properties values as string
   *
   * @param name The name of the parameter
   *
   * @return the value of the parameter in the Properties bundle
   * @throws Throwable
   */
  private static String getString(String name) throws MissingResourceException  {
      return getParameter(name);
  }


}
