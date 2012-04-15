package nz.dataview.websyncclientgui;

import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import java.io.*;
import java.util.*;
import net.iharder.base64.Base64;

public class ControlFileService {

   private static Logger logger = Logger.getLogger(ControlFileService.class);

   private static String controlDirPath = "";
   private static final int IS_UP_STALE_MINUTES = -1;
   private static final int LAST_RUN_STILL_FRESH_SECS = -15;
   private static final int NO_RETRIES_TEST_CONN = 20;
   private static final int SECS_RETRIES_TEST_CONN = 2;


   public ControlFileService() throws IOException {
      // WebSYNC config is reread on each creation, lets just do this once
      // We don't want to pass the WebSYNC object and edit the method signatures
      // this seems to be lowest impact on terms of code change to achieve this
      if (controlDirPath.isEmpty()) {
         WebSYNC webSync = new WebSYNC();
         controlDirPath = webSync.getControlDir();
      }
   }

	public boolean isRunning() {
      File isRunningFile = new File(controlDirPath + "/is_running");
      return isRunningFile.exists();
	}

	public String isAlive()
	{
      // check if its up
      File isUpFile = new File(controlDirPath + "/is_up");
      if (isUpFile.exists()) {
         Calendar isUpFileDate = Calendar.getInstance();
         isUpFileDate.setTimeInMillis(isUpFile.lastModified());

         Calendar staleTime = Calendar.getInstance();
         staleTime.add(Calendar.MINUTE, IS_UP_STALE_MINUTES);
         if (isUpFileDate.before(staleTime))
            return "dead";
			return "alive";
      } else {
         return "missing";
      }
	}

   public HashMap getStatus() {
	   String batchNumber;
      String status = "";
      String websyncStatusString = "";
      String knStatusString = "";
		Object knStatus=null;
		Object websyncStatus=null;
		long lastRunFileTime=0;
		HashMap response=new HashMap();

		try {
         FileInputStream fis = new FileInputStream(controlDirPath + File.separator + "websync_status.txt");
         BufferedReader br = new BufferedReader(new InputStreamReader(fis));

         String line = "";
         line = br.readLine();
         status = line.trim();
         line = br.readLine();
         batchNumber = line.trim();
         line = br.readLine();
         websyncStatusString = line.trim();
         line = br.readLine();
         knStatusString = line.trim();

			if(knStatusString!=null)
			{
				try{
					knStatus = PHPSerializer.unserialize(Base64.decode(knStatusString));
				} catch(UnSerializeException e)
				{
					logger.error("Could not unserialize LMS status : " + e.toString());
				}
			}
			if(websyncStatusString!=null)
			{
				try{
					websyncStatus = PHPSerializer.unserialize(Base64.decode(websyncStatusString));
				} catch(UnSerializeException e)
				{
					logger.error("Could not unserialize client status : " + e.toString());
				}
			}

         fis.close();
      } catch (FileNotFoundException e) {
         //No need for an error.
      } catch (IOException e) {
         logger.error("Could not read status file" + e.getMessage());
      }

		// check if the last run was just recent, its possible is_running
      // has been deleted before it was detected.  so if the last_run was
      // just recent then is_running must be true for a short while
      File lastRunFile = new File(controlDirPath + "/last_run");
      if (lastRunFile.exists()) {
         lastRunFileTime=lastRunFile.lastModified();
      }
		response.put("knStatus",knStatus);
		response.put("websyncStatus",websyncStatus);
		response.put("lastRunFileTime",lastRunFileTime);
      
      return response;
   }

   public void startRun() throws IOException {
      File controlFile = new File(controlDirPath + "/init_upload");
      controlFile.createNewFile();
   }

   public boolean testConnection() throws UnknownHostException, IOException {
      boolean testOk = false;
      File initTest = new File(controlDirPath + "/init_test");
      File successTest = new File(controlDirPath + "/success_test");
      File failTest = new File(controlDirPath + "/fail_test");

      try {
         initTest.createNewFile();

         int retries = 0;
         do {
            try {
               if (successTest.exists()) {
                  testOk = true;
                  break;
               } else if (failTest.exists()) {
                  testOk = false;
                  checkException(failTest);
                  break;
               }
               Thread.sleep(1000 * SECS_RETRIES_TEST_CONN);
            } catch (InterruptedException ex) {
               throw new IOException(ex);
            }
            retries++;
         } while (retries <= NO_RETRIES_TEST_CONN);
         if (retries >= NO_RETRIES_TEST_CONN)
            throw new IOException("Service not responding");
      } finally {
         successTest.delete();
         failTest.delete();
      }
      
      return testOk;

   }

   /**
    * read the control file, see what particular error
    * occured, it would then throw a new appropriate exception
    */
   private void checkException(File failTest) throws UnknownHostException, IOException {
      BufferedReader reader = new BufferedReader(new FileReader(failTest));
      try {
         String line = "";
         while (((line = reader.readLine()) != null)) {
            if ("UnknownHostException".equals(line))
               throw new UnknownHostException();
            if ("IOException".equals(line))
               throw new IOException();
         }
      } finally {
         reader.close();
      }
   }

   public String getLatestLog() throws IOException {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public boolean writeLog(String level, String message) throws IOException {

     // code taken from Client.writeLog
     if (level != null && level.length() > 0) {
         if (level.equalsIgnoreCase("trace") && logger.isTraceEnabled()) {
            logger.trace(message);
         } else if (level.equalsIgnoreCase("debug") && logger.isDebugEnabled()) {
            logger.debug(message);
         } else if (level.equalsIgnoreCase("info")) {
            logger.info(message);
         } else if (level.equalsIgnoreCase("warn")) {
            logger.warn(message);
         } else if (level.equalsIgnoreCase("error")) {
            logger.error(message);
         } else if (level.equalsIgnoreCase("fatal")) {
            logger.fatal(message);
         }
      }

      return true; // always return true now, as logging is not remote anymore
   }

   public void restart() throws IOException {
      File controlFile = new File(controlDirPath + "/restart");
      controlFile.createNewFile();
   }

   public void sendMessage(String message) throws IOException {
      File controlFile = new File(controlDirPath + "/email_logs");
      controlFile.createNewFile();
   }

}
