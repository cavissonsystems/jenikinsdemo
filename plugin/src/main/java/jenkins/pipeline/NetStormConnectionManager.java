/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jenkins.pipeline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 *
 * @author preety.yadav
 */
public class NetStormConnectionManager {
    
    private final String URLConnectionString;
    private transient final Logger logger = Logger.getLogger(NetStormConnectionManager.class.getName());
  private String servletName = "JenkinsServlet";
  private String username = "";
  private String password = "";
  private String project = "";
  private String subProject = "";
  private String scenario = "";
  private String testMode = "";
  private String duration;
  private String serverHost;
  private String vUsers;
  private String tName;
  private String rampUp;
  private String autoScript;
  private String htmlTablePath;
  private String baselineTR;
  private String result;
  
  
  private HashMap<String,String> slaValueMap =  new HashMap<String,String> ();
  static
  { 
    disableSslVerification();
  }

  public String getHtmlTablePath()
  {
    return htmlTablePath;
  }
  
  public void setHtmlTablePath(String htmlTablePath)
  {
    this.htmlTablePath = htmlTablePath;
  }
  
  public String getAutoScript()
  {
    return autoScript;
  }

  public void setAutoScript(String autoScript)
  {
    this.autoScript = autoScript;
  }
  
  public String gettName() {
    return tName;
  }

  public String getRampUp() {
    return rampUp;
  }

  
  public void settName(String tName) {
    this.tName = tName;
  }

  public void setRampUp(String rampUp) {
    this.rampUp = rampUp;
  }
  
  public String getBaselineTR() {
    return baselineTR;
  }

  public void setBaselineTR(String baselineTR) {
    this.baselineTR = baselineTR;
  }
  
  public void addSLAValue(String key, String value)
  {
    slaValueMap.put(key, value);
  }
  
  public void setDuration(String duration) {
    this.duration = duration;
  }

  public void setServerHost(String serverHost) {
    this.serverHost = serverHost;
  }

  public void setvUsers(String vUsers) {
    this.vUsers = vUsers;
  }

 
  public String getDuration() {
    return duration;
  }

  public String getServerHost() {
    return serverHost;
  }

  public String getvUsers() {
    return vUsers;
  }
  
  public String getResult() {
	    return result;
	  }

	  public void setResult(String result) {
	    this.result = result;
	  }

  private static void disableSslVerification() 
  {
    try
    {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() 
      {            
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {                
          return null;            
        }
        
        public void checkClientTrusted(X509Certificate[] certs, String authType) 
        { 
        }            
       
        public void checkServerTrusted(X509Certificate[] certs, String authType)
        {            
        }        
      }        
    };
    // Install the all-trusting trust manager       
    SSLContext sc = SSLContext.getInstance("SSL");       
    sc.init(null, trustAllCerts, new java.security.SecureRandom());   
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());  
    // Create all-trusting host name verifier    
    HostnameVerifier allHostsValid = new HostnameVerifier() 
    {  
      public boolean verify(String hostname, SSLSession session) 
      { 
        return true;            
      }         
    };        
    // Install the all-trusting host verifier        
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);    
   }
   catch (NoSuchAlgorithmException e) 
   {     
     e.printStackTrace();    
   } 
   catch (KeyManagementException e)
   {       
     e.printStackTrace(); 
   }
  }  
  
  private static enum JSONKeys {

	 URLCONNECTION("URLConnectionString"),USERNAME("username"), PASSWORD("password"), PROJECT("project"), SUBPROJECT("subproject"), OPERATION_TYPE("Operation"),
    SCENARIO("scenario"), STATUS("Status"), TEST_RUN("TESTRUN"),
    TESTMODE("testmode"), GETPROJECT("PROJECTLIST") , GETSUBPROJECT("SUBPROJECTLIST"), GETSCENARIOS("SCENARIOLIST"), BASELINE_TR("baselineTR"), REPORT_STATUS("reportStatus");

    private final String value;

    JSONKeys(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static enum OperationType
  {
    START_TEST, AUTHENTICATE_USER, GETDATA, GETPROJECT, GETSUBPROJECT, GETSCENARIOS
  };

  public NetStormConnectionManager(String URLConnectionString, String username, String password)
  {
    logger.log(Level.INFO, "NetstormConnectionManger constructor called with parameters with username:{0}, password:{1}", new Object[]{username, password});

    this.URLConnectionString = URLConnectionString;
    this.username = username;
    this.password = password;
  }

  public NetStormConnectionManager(String URLConnectionString, String username, String password, String project, String subProject, String scenario, String testMode, String baselineTR)
  {
    logger.log(Level.INFO, "NetstormConnectionManger constructor called with parameters with username:{0}, password:{1}, project:{2}, subProject:{3}, scenario:{4}, baselineTR:{5}", new Object[]{username, password, project, subProject, scenario, baselineTR});

    this.URLConnectionString = URLConnectionString;
    this.username = username;
    this.password = password;
    this.project = project;
    this.subProject = subProject;
    this.scenario = scenario;
    this.testMode = testMode;
    this.baselineTR = baselineTR;
  }
  
  /**
   * This method checks connection with netstorm
   *
   * @param urlString
   * @param servletPath
   * @param errMsg
   * @return true if connection successfully made false, otherwise
   */
  private boolean checkAndMakeConnection(String urlString, String servletPath, StringBuffer errMsg)
  {
    logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments restUrl : ", new Object[]{urlString});
    try
      {
	 JSONObject reqObj = new JSONObject();
	 reqObj.put("username", this.username);
	 reqObj.put("password" ,this.password);
	    	
	  URL url ;
	  String str =   URLConnectionString.substring(0,URLConnectionString.lastIndexOf("/"));
	  url = new URL(str+"/ProductUI/productSummary/jenkinsService/validateUser");
	     
	  logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments url"+  url);
	  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	  conn.setRequestMethod("POST");
	      
	  conn.setRequestProperty("Accept", "application/json");
	  conn.setDoOutput(true);
	  String json =reqObj.toString();
	  OutputStream os = conn.getOutputStream();
	  os.write(json.getBytes());
	  os.flush();

	   if (conn.getResponseCode() != 200) {
   	        throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
	   }
	   else 
	   {
	       BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
	       setResult(br.readLine());
	       logger.log(Level.INFO, "RESPONSE -> "+getResult());
	       return true;
	   }
	      
	} catch (MalformedURLException e) {
	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. MalformedURLException -", e);
	      e.printStackTrace();
	      return false;
	} catch (IOException e) {
	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. IOException -", e);
	      e.printStackTrace();
	      return false;
	} catch (Exception e) {
	      logger.log(Level.SEVERE, "Unknown exception in establishing connection.", e);
	      return (false);
	}
  
  }
    
  private void setDefaultSSLProperties(URLConnection urlConnection,StringBuffer errMsg)
  {
    try 
    {
      /*
       * For normal HTTP connection there is no need to set SSL properties.
       */
      if (urlConnection instanceof HttpsURLConnection)
      {
        /* We are not checking host name at time of SSL handshake. */
        HttpsURLConnection con = (HttpsURLConnection) urlConnection;
        con.setHostnameVerifier(new HostnameVerifier()
        {
          @Override
          public boolean verify(String arg0, SSLSession arg1) 
          {
            return true;
          }
        });
      }
    } 
    catch (Exception e) 
    {
    }
  }
  
  public JSONObject makeRequestObject(String type)
  {
    JSONObject jsonRequest = new JSONObject();
    
    if(type.equals("START_TEST")) 
    {
      jsonRequest.put(JSONKeys.USERNAME.getValue(), username);
      jsonRequest.put(JSONKeys.PASSWORD.getValue(), password); 
      jsonRequest.put(JSONKeys.URLCONNECTION.getValue(), URLConnectionString);
      jsonRequest.put(JSONKeys.OPERATION_TYPE.getValue(), OperationType.START_TEST.toString());
      jsonRequest.put(JSONKeys.PROJECT.getValue(), project);
      jsonRequest.put(JSONKeys.SUBPROJECT.getValue(), subProject);
      jsonRequest.put(JSONKeys.SCENARIO.getValue(), scenario);
      jsonRequest.put(JSONKeys.STATUS.getValue(), Boolean.FALSE);
      jsonRequest.put(JSONKeys.TESTMODE.getValue(), testMode);
      jsonRequest.put(JSONKeys.REPORT_STATUS.getValue(), ""); 
     

      if(getBaselineTR() != null && !getBaselineTR().trim().equals(""))
       {
         String baseline = getBaselineTR();
         if(baseline.startsWith("TR"))
	  baseline = baseline.substring(2, baseline.length());
	  
         jsonRequest.put("BASELINE_TR", baseline);
       } 
      else
       jsonRequest.put("BASELINE_TR", "-1");
       

      if(getDuration() != null && !getDuration().trim().equals(""))
      {
        jsonRequest.put("DURATION", getDuration());
      }
      
      if(getServerHost() != null && !getServerHost().trim().equals(""))
      {
        jsonRequest.put("SERVER_HOST", getServerHost());
      }
      
      if(getvUsers() != null && !getvUsers().trim().equals(""))
      {
        jsonRequest.put("VUSERS", getvUsers());
      }
      
      if(getRampUp() != null && !getRampUp().trim().equals(""))
      {
        jsonRequest.put("RAMP_UP", getRampUp());
      }
      
      if(gettName()!= null && !gettName().trim().equals(""))
      {
        jsonRequest.put("TNAME", gettName());
      }
      if(getAutoScript()!= null && !getAutoScript().trim().equals(""))
      {
        jsonRequest.put("AUTOSCRIPT", getAutoScript());
      }
      
      if(slaValueMap.size() > 0)
      {
        JSONArray  jsonArray = new JSONArray();
        Set<String> keyset = slaValueMap.keySet();
        
        for(String rule : keyset)
        {
          JSONObject jsonRule = new  JSONObject();
          jsonRule.put(rule, slaValueMap.get(rule));
          jsonArray.add(jsonRule);
        }
        
        jsonRequest.put("SLA_CHANGES", jsonArray);
      }
    }
    else if(type.equals("TEST_CONNECTION"))
    {
      jsonRequest.put(JSONKeys.USERNAME.getValue(), username);
      jsonRequest.put(JSONKeys.PASSWORD.getValue(), password);
      jsonRequest.put(JSONKeys.OPERATION_TYPE.getValue(), OperationType.AUTHENTICATE_USER.toString());
      jsonRequest.put(JSONKeys.STATUS.getValue(), Boolean.FALSE);
    }
    else if(type.equals("GET_DATA"))
    {
      jsonRequest.put(JSONKeys.USERNAME.getValue(), username);
      jsonRequest.put(JSONKeys.PASSWORD.getValue(), password);
      jsonRequest.put(JSONKeys.OPERATION_TYPE.getValue(), OperationType.GETDATA.toString());
      jsonRequest.put(JSONKeys.STATUS.getValue(), Boolean.FALSE); 
      
      //This is used get html report netstorm side.
      if(getHtmlTablePath() != null && !"".equals(getHtmlTablePath()))
        jsonRequest.put("REPORT_PATH", getHtmlTablePath());
      
    }
    else if(type.equals("GET_PROJECT"))
    {
      jsonRequest.put(JSONKeys.USERNAME.getValue(), username);
      jsonRequest.put(JSONKeys.PASSWORD.getValue(), password);
      jsonRequest.put(JSONKeys.OPERATION_TYPE.getValue(), OperationType.GETPROJECT.toString());
      jsonRequest.put(JSONKeys.STATUS.getValue(), Boolean.FALSE);
    }
    else if(type.equals("GET_SUBPROJECT"))
    {
      jsonRequest.put(JSONKeys.USERNAME.getValue(), username);
      jsonRequest.put(JSONKeys.PASSWORD.getValue(), password);
      jsonRequest.put(JSONKeys.PROJECT.getValue(), project);
      jsonRequest.put(JSONKeys.OPERATION_TYPE.getValue(), OperationType.GETSUBPROJECT.toString());
      jsonRequest.put(JSONKeys.STATUS.getValue(), Boolean.FALSE);
    }
    else if(type.equals("GET_SCENARIOS"))
    {
      jsonRequest.put(JSONKeys.USERNAME.getValue(), username);
      jsonRequest.put(JSONKeys.PASSWORD.getValue(), password);
      jsonRequest.put(JSONKeys.PROJECT.getValue(), project);
      jsonRequest.put(JSONKeys.SUBPROJECT.getValue(), subProject);
      jsonRequest.put(JSONKeys.TESTMODE.getValue(), testMode);
     
      jsonRequest.put(JSONKeys.OPERATION_TYPE.getValue(), OperationType.GETSCENARIOS.toString());
      jsonRequest.put(JSONKeys.STATUS.getValue(), Boolean.FALSE);
    }
    return jsonRequest;
  }
  

  /**
   * This Method makes connection to netstorm.
   *
   * @param errMsg
   * @return true , if Successfully connected and authenticated false ,
   * otherwise
   */
  public boolean testNSConnection(StringBuffer errMsg) 
  {
    logger.log(Level.INFO, "testNSConnection() called.");

    if(checkAndMakeConnection(URLConnectionString, servletName, errMsg))
    {
      
    	JSONObject jsonResponse  =  (JSONObject) JSONSerializer.toJSON(getResult());
      
      if((jsonResponse == null))
      { 
        logger.log(Level.INFO, "Connection failure, please check whether Connection URI is specified correctly");
        errMsg.append("Connection failure, please check whether Connection URI is specified correctly");
        return false;
      }
      
      boolean status = (Boolean)jsonResponse.get(JSONKeys.STATUS.getValue());
     
      if (status)
      { 
        logger.log(Level.INFO, "Successfully Authenticated.");
        return true;
      }
      else
      { 
        logger.log(Level.INFO, "Authentication failure, please check whether username and password given correctly");
        errMsg.append("Authentication failure, please check whether username and password given correctly");
      }
    }
    else
    { 
      logger.log(Level.INFO, "Connection failure, please check whether Connection URI is specified correctly");
      errMsg.append("Connection failure, please check whether Connection URI is specified correctly");
    }
      return false;
  }
   
   
  
  public ArrayList<String> getProjectList(StringBuffer errMsg)
  {
    logger.log(Level.INFO, "getProjectList method called.");
    
    try
    {
      logger.log(Level.INFO, "Making connection to Netstorm with following request uri- " + URLConnectionString);
      logger.log(Level.INFO, "Sending requets to get project list - " + URLConnectionString);
      JSONObject jsonResponse  = null;
      if(checkAndMakeConnection(URLConnectionString, servletName, errMsg))
      {   
    	JSONObject jsonRequest =    makeRequestObject("GET_PROJECT");
    	        
        try {
              URL url ;
              String str =   URLConnectionString.substring(0,URLConnectionString.lastIndexOf("/"));
    	      url = new URL(str+"/ProductUI/productSummary/jenkinsService/getProject");
    	     
    	      logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments for metric  url"+  url);
    	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	      conn.setRequestMethod("POST");
    	      
    	      conn.setRequestProperty("Accept", "application/json");
    	      String json =jsonRequest.toString();
    	      conn.setDoOutput(true);
    	      OutputStream os = conn.getOutputStream();
    	      os.write(json.getBytes());
    	      os.flush();
    	      
    	      
    	      if (conn.getResponseCode() != 200) {
    		  throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
    	      }

    	      BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
    	      setResult(br.readLine());
    	      logger.log(Level.INFO, "RESPONSE for metric getProjectList  -> "+getResult());
    	      
    	      
    	       jsonResponse  =  (JSONObject) JSONSerializer.toJSON(this.result);
        	
          }
          catch (MalformedURLException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. MalformedURLException -", e);
    	      e.printStackTrace();
    	      
    	 } catch (IOException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. IOException -", e);
    	      e.printStackTrace();
    	    
    	 } catch (Exception e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection.", e);
    	 }
           
        if(jsonResponse != null)
        {
          if(jsonResponse.get(JSONKeys.STATUS.getValue()) != null && jsonResponse.get(JSONKeys.GETPROJECT.getValue()) !=null)
          {
            boolean status = (Boolean)jsonResponse.get(JSONKeys.STATUS.getValue());
            JSONArray projectJsonArray= (JSONArray)(jsonResponse.get(JSONKeys.GETPROJECT.getValue()));
           
            if(status == true)
            {
              ArrayList<String> projectList = new ArrayList<String>();
             
              for(int i = 0 ; i < projectJsonArray.size() ; i++)
              {
                 projectList.add((String)projectJsonArray.get(i));
              }
            
              return projectList;
            }
            else
            {
              logger.log(Level.INFO, "Not able to fetch project list from - " + URLConnectionString);
            }
          }
        }     
      }
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception in getting project list ", ex);
    }
    
    return null;
  }

  public ArrayList<String> getSubProjectList(StringBuffer errMsg , String project)
  {
    logger.log(Level.INFO, "getSubProjectList method called.");
    
    try
    {
      logger.log(Level.INFO, "Making connection to Netstorm with following request uri- " + URLConnectionString);

      this.project =  project;
      JSONObject jsonResponse  = null;
      if (checkAndMakeConnection(URLConnectionString, servletName, errMsg))
      {
    	JSONObject jsonRequest =    makeRequestObject("GET_SUBPROJECT");
     
        try {
              URL url ;
              String str =   URLConnectionString.substring(0,URLConnectionString.lastIndexOf("/"));
    	      url = new URL(str+"/ProductUI/productSummary/jenkinsService/getSubProject");
    	     
    	      logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments for metric  url"+  url);
    	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	      conn.setRequestMethod("POST");
    	      
    	      conn.setRequestProperty("Accept", "application/json");
    	      
    	      String json =jsonRequest.toString();
    	      conn.setDoOutput(true);
    	      OutputStream os = conn.getOutputStream();
    	      os.write(json.getBytes());
    	      os.flush();
    	      
    	      if (conn.getResponseCode() != 200) {
    		  throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
    	      }

    	      BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
    	      setResult(br.readLine());
    	      logger.log(Level.INFO, "RESPONSE for metric getSubProjectList  -> "+getResult());
    	      
    	      
    	       jsonResponse  =  (JSONObject) JSONSerializer.toJSON(this.result);
        	
          }
             catch (MalformedURLException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. MalformedURLException -", e);
    	      e.printStackTrace();
    	      
    	    } catch (IOException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. IOException -", e);
    	      e.printStackTrace();
    	    
    	    } catch (Exception e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection.", e);
    	    
    	 }
        
        if(jsonResponse != null)
        {
          if(jsonResponse.get(JSONKeys.STATUS.getValue()) != null && jsonResponse.get(JSONKeys.GETSUBPROJECT.getValue()) !=null)
          {
            boolean status = (Boolean)jsonResponse.get(JSONKeys.STATUS.getValue());
            JSONArray subProjectJSONArray= (JSONArray)(jsonResponse.get(JSONKeys.GETSUBPROJECT.getValue()));
            if(status == true)
            {
              ArrayList<String> subProjectList = new ArrayList<String>();
              for(int i = 0 ; i < subProjectJSONArray.size() ; i++)
              {
                 subProjectList.add((String)subProjectJSONArray.get(i));
              }
             
              return subProjectList;
            }
            else
            {
              logger.log(Level.SEVERE, "Not able to get sub project from - " + URLConnectionString);
            }
          }
       }
     }
   }
   catch (Exception ex)
   {
     logger.log(Level.SEVERE, "Exception in getting getSubProjectList.", ex);
   }   
 
   return null;
 }  
  
  public ArrayList<String> getScenarioList(StringBuffer errMsg , String project, String subProject, String mode)
  {
    logger.log(Level.INFO, "getScenarioList method called.");
    try
    {
      logger.log(Level.INFO, "Making connection to Netstorm with following request uri- " + URLConnectionString);
      this.project = project;
      this.subProject = subProject;
      this.testMode = mode;
   
      JSONObject jsonResponse  = null;
      if (checkAndMakeConnection(URLConnectionString, servletName, errMsg))
      {
    	JSONObject jsonRequest =    makeRequestObject("GET_SCENARIOS");
        
        try {
              URL url ;
              String str =   URLConnectionString.substring(0,URLConnectionString.lastIndexOf("/"));
    	      url = new URL(str+"/ProductUI/productSummary/jenkinsService/getScenario");
    	     
    	      logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments for metric  url"+  url);
    	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	      conn.setRequestMethod("POST");
    	      
    	      conn.setRequestProperty("Accept", "application/json");
    	
    	      String json =jsonRequest.toString();
    	      conn.setDoOutput(true);
    	      OutputStream os = conn.getOutputStream();
    	      os.write(json.getBytes());
    	      os.flush();
    	            
    	      if (conn.getResponseCode() != 200) {
    		  throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
    	      }

    	      BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
    	      setResult(br.readLine());
    	      logger.log(Level.INFO, "RESPONSE for metric getScenarioList  -> "+getResult());
    	      
    	      
    	       jsonResponse  =  (JSONObject) JSONSerializer.toJSON(this.result);	
            }
             catch (MalformedURLException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. MalformedURLException -", e);
    	      e.printStackTrace();
    	      
    	    } catch (IOException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. IOException -", e);
    	      e.printStackTrace();
    	    
    	    } catch (Exception e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection.", e);
    	 } 
    	  
       if(jsonResponse != null)
       {
         if(jsonResponse.get(JSONKeys.STATUS.getValue()) != null && jsonResponse.get(JSONKeys.GETSCENARIOS.getValue()) !=null)
         {
           
           boolean status = (Boolean)jsonResponse.get(JSONKeys.STATUS.getValue());
           JSONArray scenarioJSONArray= (JSONArray)(jsonResponse.get(JSONKeys.GETSCENARIOS.getValue()));
           
           if(status == true)
            {
              ArrayList<String> scenarioList = new ArrayList<String>();
              for(int i = 0 ; i < scenarioJSONArray.size() ; i++)
              {
                 scenarioList.add((String)scenarioJSONArray.get(i));
              }
             
              return scenarioList;
            }
            else
            {
              logger.log(Level.SEVERE, "Not able to get scenarios from - " + URLConnectionString);
            }
         }
        }
      }
    }
    catch (Exception ex)
   {
     logger.log(Level.SEVERE, "Exception in getting getScenario.", ex);
   }   
  
   return null;
  }

  public HashMap startNetstormTest(StringBuffer errMsg , PrintStream consoleLogger)
  {
    logger.log(Level.INFO, "startNetstormTest() called.");
   
    HashMap resultMap = new HashMap(); 
    resultMap.put("STATUS", false);
    
    try 
    {
      logger.log(Level.INFO, "Making connection to Netstorm with following request uri- " + URLConnectionString);
      consoleLogger.println("Making connection to Netstorm with following request uri- " + URLConnectionString);
      JSONObject jsonResponse =null;
      if (checkAndMakeConnection(URLConnectionString, servletName, errMsg))
      { 
    	JSONObject jsonRequest =    makeRequestObject("START_TEST");
        consoleLogger.println("Starting Test ... ");
        
        try {
              URL url;
              String str =   URLConnectionString.substring(0,URLConnectionString.lastIndexOf("/"));
    	      url = new URL(str+"/ProductUI/productSummary/jenkinsService/startTest");
    	     
    	      logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments for metric  url"+  url);
    	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	      conn.setRequestMethod("POST");
    	      
    	      conn.setRequestProperty("Accept", "application/json");
    	      
    	      String json =jsonRequest.toString();
    	      conn.setDoOutput(true);
    	      OutputStream os = conn.getOutputStream();
    	      os.write(json.getBytes());
    	      os.flush();
    	      
    	      if (conn.getResponseCode() != 200) {
    		  throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
    	      }

    	      BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
    	      setResult(br.readLine());
    	      logger.log(Level.INFO, "RESPONSE for metric startTest  -> "+getResult());
    	      
    	       jsonResponse  =  (JSONObject) JSONSerializer.toJSON(this.result);	
            }
            catch (MalformedURLException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. MalformedURLException -", e);
    	      e.printStackTrace();
    	    } catch (IOException e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. IOException -", e);
    	      e.printStackTrace();
    	    } catch (Exception e) {
    	      logger.log(Level.SEVERE, "Unknown exception in establishing connection.", e);
    	  }
              
        if(jsonResponse != null)
        {
          boolean status = false;
          if(jsonResponse.get(JSONKeys.STATUS.getValue()) != null)
          {
            status = (Boolean)jsonResponse.get(JSONKeys.STATUS.getValue()); 
            if(!status)
            {
              consoleLogger.println("Test is aborted."); 
              return resultMap;
            }
          }

          //Changes for showing shell output on jenkins console.
          if(jsonResponse.get(JSONKeys.REPORT_STATUS.getValue()) != null) {
            
            String exeCmd = (String)(jsonResponse.get(JSONKeys.REPORT_STATUS.getValue()));
            exeCmd =  exeCmd.substring(exeCmd.lastIndexOf("[")+1,exeCmd.lastIndexOf("]"));
            String[] var = exeCmd.split(",");
            for(int j = 0;j<var.length;j++)
              consoleLogger.println(var[j]); 
          }

          if(jsonResponse.get(JSONKeys.TEST_RUN.getValue()) !=null)
          {
            String testRun= (String)(jsonResponse.get(JSONKeys.TEST_RUN.getValue()));
            resultMap.put("STATUS", status);
            resultMap.put("TESTRUN",testRun);
         
            if(jsonResponse.containsKey("ENV_NAME"))
            { 
              String envNames = "";
              JSONArray envArr = (JSONArray)jsonResponse.get("ENV_NAME");
               
              for(int i = 0 ; i < envArr.size() ; i++)
              { 
                if( i == 0)
                  envNames = (String)envArr.get(i);
                else
                  envNames = envNames + "," + (String)envArr.get(i);
              }
               
              resultMap.put("ENV_NAME", envNames);
            }       
            
            consoleLogger.println("Test is executed successfully.");
           
          }
          logger.log(Level.SEVERE, "Data in the result map>>>>>>>>>>>>>>>>>>>>>>>>..."+ resultMap);
          return resultMap;
        }
      }
     }
    catch (Exception ex) 
    {
      logger.log(Level.SEVERE, "Exception in closing connection.", ex);
      return resultMap;
    }
    
    logger.log(Level.INFO, "Start Test Status is fail with request uri- " + URLConnectionString);
    consoleLogger.println("Test is not started, Please verify given scenario...");
    
    return resultMap;
  }

  public MetricDataContainer fetchMetricData(String metrics[], int durationInMinutes, int groupIds[], int graphIds[], 
          int testRun, String testMode)
  {
    logger.log(Level.INFO, "fetchMetricData() called.");
    
  
    JSONObject jsonRequest = makeRequestObject("GET_DATA");
    
     logger.log(Level.INFO, "json request----->",jsonRequest);
    jsonRequest.put("TESTRUN", String.valueOf(testRun));
    
    logger.log(Level.INFO, "Test Run Values--->"+String.valueOf(testRun));
    jsonRequest.put(JSONKeys.TESTMODE.getValue(), testMode);
    
    JSONArray jSONArray = new JSONArray();
     
    for(int i = 0 ; i < metrics.length ; i++)
    {
      jSONArray.add(groupIds[i] + "." + graphIds[i]);
    }
   
    jsonRequest.put("Metric", jSONArray);
     logger.log(Level.INFO, "json fabfdksbksdjbvkjd-->"+jSONArray);
    
    logger.log(Level.INFO, "Metrix report-->"+String.valueOf(testRun));
   
    StringBuffer errMsg = new StringBuffer();
    JSONObject resonseObj = null;
    
    if(checkAndMakeConnection(URLConnectionString, servletName, errMsg))
    {
      try {
    	    URL url ;
    	    String str =   URLConnectionString.substring(0,URLConnectionString.lastIndexOf("/"));
	    url = new URL(str+"/ProductUI/productSummary/jenkinsService/jsonData");
	     
	    logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments for metric  url"+  url);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("POST");
	      
	    conn.setRequestProperty("Accept", "application/json");
	      
	    String json =jsonRequest.toString();
	    conn.setDoOutput(true);
	    OutputStream os = conn.getOutputStream();
	    os.write(json.getBytes());
	    os.flush();
	      
	    if (conn.getResponseCode() != 200) {
	      throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
	    }

	    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
	    setResult(br.readLine());
	    logger.log(Level.INFO, "RESPONSE for metric  -> "+getResult());
	      
	    resonseObj =  (JSONObject) JSONSerializer.toJSON(this.result);
            
            logger.log(Level.INFO, "Data in the response object========++++++ -> "+resonseObj);
    	 }
         catch (MalformedURLException e) {
	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. MalformedURLException -", e);
	      e.printStackTrace();
	 } catch (IOException e) {
	      logger.log(Level.SEVERE, "Unknown exception in establishing connection. IOException -", e);
	      e.printStackTrace();
	 } catch (Exception e) {
	      logger.log(Level.SEVERE, "Unknown exception in establishing connection.", e);
	 }
    }
    else
    {
      logger.log(Level.INFO, "Connection failure, please check whether Connection URI is specified correctly");
      errMsg.append("Connection failure, please check whether Connection URI is specified correctly");
      return null;
    }
   
    if(resonseObj == null)
    {
      logger.log(Level.SEVERE, "Not able to get response form server due to: " + errMsg);
      return null;
    }
    
     logger.log(Level.SEVERE, "response objectkln========."+ resonseObj);
     
     logger.log(Level.SEVERE, "test mode========."+ testMode);
    
    return parseJSONData(resonseObj, testMode);
  }

  private MetricDataContainer parseJSONData(JSONObject resonseObj, String testMode)
  {
    logger.log(Level.INFO, "parseJSONData() called.");
    
    
    MetricDataContainer metricDataContainer = new MetricDataContainer();
    logger.log(Level.INFO,"Metric Data:" + metricDataContainer );
    logger.log(Level.INFO,"Recived response from : " + resonseObj );
    System.out.println("Recived response from : " + resonseObj);
    
    try
    {
      ArrayList<MetricData> dataList = new ArrayList<MetricData>();
      
      JSONObject jsonGraphs = (JSONObject)resonseObj.get("graphs");
      int freq = ((Integer)resonseObj.get("frequency"))/1000; 
      metricDataContainer.setFrequency(freq);
      
      if(resonseObj.containsKey("customHTMLReport"))
        metricDataContainer.setCustomHTMLReport((String)resonseObj.get("customHTMLReport"));
      
      TestReport testReport = new TestReport();
      if("T".equals(testMode))
      {
        logger.log(Level.INFO,"Inside a equals methods.........");  
        testReport = new TestReport();
        testReport.setUserName(username);
        JSONObject jsonTestReportWholeObj = resonseObj.getJSONObject("testReport");
        JSONObject jsonTestReport = jsonTestReportWholeObj.getJSONObject("members");
        
        String overAllStatus =  jsonTestReport.getString("Overall Status");
        String date = jsonTestReport.getString("Date");
        String overAllFailCriteria = jsonTestReport.getString("Overall Fail Criteria (greater than red) %");
        JSONArray metricsUnderTest = jsonTestReport.getJSONArray("Metrics Under Test");
        String serverName = jsonTestReport.getString("IP");
	String productName = jsonTestReport.getString("ProductName");
        String previousTestRun = jsonTestReport.getString("Previous Test Run");
        String baseLineTestRun = jsonTestReport.getString("Baseline Test Run");
        String initialTestRun = jsonTestReport.getString("Initial Test Run");
        String baseLineDateTime = jsonTestReport.getString("Baseline Date Time");
        String previousDateTime = jsonTestReport.getString("Previous Date Time");
        String initialDateTime = jsonTestReport.getString("Initial Date Time");
        String testRun = jsonTestReport.getString("Test Run");
        String normalThreshold = jsonTestReport.getString("Normal Threshold");
        String criticalThreshold = jsonTestReport.getString("Critical Threshold");
        String currentDateTime = "", previousDescription = "", baselineDescription = "", currentDescription = "", initialDescription = "";
        try
        {
          currentDateTime = jsonTestReport.getString("Current Date Time");
          previousDescription = jsonTestReport.getString("Previous Description"); 
          baselineDescription = jsonTestReport.getString("Baseline Description");
          currentDescription =  jsonTestReport.getString("Current Description");
          initialDescription = jsonTestReport.getString("Initial Description");
        }
        catch(Exception ex)
        {
          logger.log(Level.SEVERE, "Error in parsing Test Report Data:" + ex);
          logger.log(Level.SEVERE, "---" + ex.getMessage());
        }

        ArrayList<TestMetrics> testMetricsList = new ArrayList<TestMetrics>(metricsUnderTest.size());
        
        String str = "";
        int index = 0; 
        
        for(Object jsonData : metricsUnderTest)
        {  
          JSONObject jsonObject = (JSONObject)jsonData;
          
          String prevTestValue = jsonObject.getString("Prev Test Value ");
          String baseLineValue = jsonObject.getString("Baseline Value ");
          String initialValue = jsonObject.getString("Initial Value ");
          String edLink = jsonObject.getString("link");
          String currValue = jsonObject.getString("Value");
          String metric = jsonObject.getString("Metric");
          String metricRule = jsonObject.getString("MetricRule");
          String operator = jsonObject.getString("Operator");
          String sla = jsonObject.getString("SLA");
          if(sla.indexOf(">") != -1 || sla.indexOf(">") > 0)
	    sla = sla.substring(sla.lastIndexOf(">")+1, sla.length());

          String transactiontStatus = jsonObject.getString("Transaction Status");
          String transactionBgcolor = jsonObject.getString("Transaction BGcolor");
          String transactionTooltip = jsonObject.getString("Transaction Tooltip"); 
          String trendLink = jsonObject.getString("trendLink");
          TestMetrics testMetric = new TestMetrics();
          
          testMetric.setBaseLineValue(baseLineValue);
          testMetric.setCurrValue(currValue);
          if(edLink != null)
            testMetric.setEdLink(edLink);
          else
            testMetric.setEdLink("NA");
          testMetric.setOperator(operator);
          testMetric.setPrevTestRunValue(prevTestValue);
          testMetric.setInitialValue(initialValue);
          testMetric.setSLA(sla);
          if(trendLink != null)
           testMetric.setLinkForTrend(trendLink);
          else
            testMetric.setLinkForTrend("NA");
            
          String headerName = "";
          String displayName = metric;
          if (index == 0)
          {
            str = displayName;
            if(displayName.contains("- All"))
            {
               headerName = displayName.substring(0, str.lastIndexOf("-")+5);
               displayName = displayName.substring(displayName.lastIndexOf("-")+6,displayName.length()-1);
            }
            else if(displayName.contains(" - "))
            {
              headerName = displayName.substring(0, str.lastIndexOf("-")+1);
              displayName = displayName.substring(displayName.lastIndexOf("-")+1,displayName.length()-1);
            }
            else
            {
              headerName = "Other";
            }
              index++;
           }
           else
           {
             if (displayName.contains(" - "))
             {
               String metricName = displayName.substring(0, displayName.lastIndexOf("-"));

               if (metricName.equals(str.substring(0, str.lastIndexOf("-"))))
               {
                  headerName = "";
                 if (displayName.contains("- All"))
                 {
                   displayName = displayName.substring(displayName.lastIndexOf("-")+6,displayName.length()-1);
                 }
                 else
                   displayName = displayName.substring(displayName.lastIndexOf("-")+1,displayName.length());
                }
               else
               {
                 str = displayName;
                 if (displayName.contains("- All"))
                 {
                   headerName = displayName.substring(0, displayName.lastIndexOf("-")+5);
                   displayName = displayName.substring(displayName.lastIndexOf("-")+6,displayName.length()-1);
                 }
                 else if(displayName.contains(" - "))
                 {
                   headerName = displayName.substring(0, displayName.lastIndexOf("-"));
                   displayName = displayName.substring(displayName.lastIndexOf("-")+1,displayName.length());
                 }
                 else
                 {
                   headerName = "Other";
                 }
                 
               }
             }
             else
             {
              headerName = "Other";
             }
           }
         
          testMetric.setNewReport("NewReport");
          testMetric.setDisplayName(displayName);
          testMetric.setHeaderName(headerName);               
          testMetric.setMetricName(metric);
          testMetric.setMetricRuleName(metricRule);
          testMetric.setTransactiontStatus(transactiontStatus);
          testMetric.setStatusColour(transactionBgcolor);
          testMetric.setTransactionTooltip(transactionTooltip);

          testMetricsList.add(testMetric);
          testReport.setOperator(operator);
        }
        
        int transObj = 1;
        
        //Check is used for if Base transaction exist in json report.
        if(jsonTestReport.has("BASETOT"))
           transObj = 2;        
 
        for(int i = 0 ; i < transObj; i++)
        {
          JSONObject transactionJson = null;
          
          if(i == 1)
          {
            transactionJson = jsonTestReport.getJSONObject("BASETOT");
          }
          else 
          {  
            if(jsonTestReport.has("TOT"))
              transactionJson = jsonTestReport.getJSONObject("TOT");
            else
              transactionJson = jsonTestReport.getJSONObject("CTOT"); 
          }

          logger.log(Level.INFO, "transactionJson ="+transactionJson);
   
          String complete = "NA";
          if(transactionJson.getString("complete") != null)
            complete = transactionJson.getString("complete");
        
          String totalTimeOut = "NA";
          if(transactionJson.getString("Time Out") != null)
            totalTimeOut = transactionJson.getString("Time Out");
        
          String t4xx = "NA";
          if(transactionJson.getString("4xx") != null)
            t4xx = transactionJson.getString("4xx");
        
          String t5xx = "NA";
          if(transactionJson.getString("5xx") != null)
            t5xx = transactionJson.getString("5xx");
        
          String conFail = "NA";
          if(transactionJson.getString("ConFail") != null)
            conFail = transactionJson.getString("ConFail");
        
          String cvFail = "NA";
          if(transactionJson.getString("C.V Fail") != null)
            cvFail = transactionJson.getString("C.V Fail");
 
          String success = "NA";
          if(transactionJson.getString("success") != null)
            success = transactionJson.getString("success");
        
          String warVersionTrans = "NA";
          if(transactionJson.has("warVersion"))
            warVersionTrans = transactionJson.getString("warVersion");
          
          String releaseVersionTrans = "NA";
          if(transactionJson.has("releaseVersion"))
            releaseVersionTrans = transactionJson.getString("releaseVersion");

          //Create Transaction Stats Object to save Base Test and Current Test Run transaction details
          TransactionStats transactionStats = new TransactionStats();
        
          if(i == 1)
          {
            transactionStats.setTransTestRun("BASETOT");
          }
          else
          {
            if(jsonTestReport.has("TOT"))
              transactionStats.setTransTestRun("TOT");
            else
              transactionStats.setTransTestRun("CTOT");
          }
          
          

          transactionStats.setComplete(complete);
          transactionStats.setConFail(conFail);
          transactionStats.setCvFail(cvFail);
          transactionStats.setSuccess(success);
          transactionStats.setT4xx(t4xx);
          transactionStats.setT5xx(t5xx);
          transactionStats.setTotalTimeOut(totalTimeOut);
          transactionStats.setWarVersion(warVersionTrans);
          transactionStats.setReleaseVersion(releaseVersionTrans);        
 
          testReport.getTransStatsList().add(transactionStats);
        }
        
        testReport.setBaseLineTestRun(baseLineTestRun);
        testReport.setInitialTestRun(initialTestRun);
        testReport.setBaselineDateTime(baseLineDateTime);
        testReport.setPreviousDateTime(previousDateTime);
        testReport.setInitialDateTime(initialDateTime);
        testReport.setOverAllFailCriteria(overAllFailCriteria);
        testReport.setDate(date);
        testReport.setTestMetrics(testMetricsList);
        testReport.setOverAllStatus(overAllStatus);
        testReport.setServerName(serverName);
        testReport.setPreviousTestRun(previousTestRun);
        testReport.setTestRun(testRun);
        testReport.setNormalThreshold(normalThreshold);
        testReport.setCriticalThreshold(criticalThreshold);
        testReport.setCurrentDateTime(currentDateTime);
        testReport.setPreviousDescription(previousDescription);
        testReport.setBaselineDescription(baselineDescription);
        testReport.setIpPortLabel(productName);
        testReport.setInitialDescription(initialDescription);
        testReport.setCurrentDescription(currentDescription);
        metricDataContainer.setTestReport(testReport);
      }
      if(jsonGraphs != null)
      {
      Set keySet  = jsonGraphs.keySet();
      Iterator itr = keySet.iterator();
    
      while(itr.hasNext())
      {
        String key = (String)itr.next();
        MetricData metricData = new MetricData();
      
        JSONObject graphJsonObj = (JSONObject)jsonGraphs.get(key);
        String graphName = (String)graphJsonObj.get("graphMetricPath");
    
        metricData.setMetricPath(graphName.substring(graphName.indexOf("|") + 1));
        metricData.setFrequency(String.valueOf(freq));
    
        JSONArray jsonArray = (JSONArray)graphJsonObj.get("graphMetricValues");
        
        ArrayList<MetricValues> list = new ArrayList<MetricValues>();
     
        for (Object jsonArray1 : jsonArray)
        {
          MetricValues values =  new MetricValues();
          JSONObject graphValues = (JSONObject) jsonArray1;
          String currVal = String.valueOf(graphValues.get("currentValue"));
          String maxVal  = String.valueOf(graphValues.get("maxValue"));
          String minVal = String.valueOf(graphValues.get("minValue"));
          String avg  = String.valueOf(graphValues.get("avgValue"));
          long timeStamp  = (Long)graphValues.get("timeStamp");
          values.setValue((Double)graphValues.get("currentValue"));
          values.setMax((Double)graphValues.get("maxValue"));
          values.setMin(getMinForMetric((Double)graphValues.get("minValue")));
          values.setStartTimeInMillis(timeStamp);
          list.add(values);
          
        }   
        metricData.setMetricValues(list);
        dataList.add(metricData); 
        metricDataContainer.setMetricDataList(dataList);
      }
      }
      
      //Now checking in response for baseline and previous test data
      if(testMode.equals("T"))
      {
        if(resonseObj.get("previousTestDataMap") != null)
        {
          JSONObject jsonGraphObj = (JSONObject)resonseObj.get("previousTestDataMap"); 
        
          ArrayList<MetricData> prevMetricDataList =  parsePreviousAndBaseLineData(jsonGraphObj , freq , "Previous Test Run");
        
          if((prevMetricDataList != null))
          {
            logger.log(Level.INFO, "Setting previous test data in metric container = "  + prevMetricDataList);
            metricDataContainer.setMetricPreviousDataList(prevMetricDataList);
          }
        }
      
        if(resonseObj.get("baseLineTestDataMap") != null)
        {
          JSONObject jsonGraphObj = (JSONObject)resonseObj.get("baseLineTestDataMap"); 
          ArrayList<MetricData> baseLineMetricDataList =  parsePreviousAndBaseLineData(jsonGraphObj, freq , "Base Line Test Run");
       
          if((baseLineMetricDataList != null))
          {
            logger.log(Level.INFO, "Setting baseline test data in metric container = " + baseLineMetricDataList);
            metricDataContainer.setMetricBaseLineDataList(baseLineMetricDataList);
          }
        }
      }
    }
    catch(Exception e)
    {
      logger.log(Level.SEVERE, "Error in parsing metrics stats" );
      logger.log(Level.SEVERE, "Metric Data:" + e);
      e.printStackTrace();
      logger.log(Level.SEVERE, "---" + e.getMessage());
      return null;
    }
    
    logger.log(Level.INFO,"Metric Data:" + metricDataContainer );

          
    return metricDataContainer;
  }
  
  private ArrayList<MetricData> parsePreviousAndBaseLineData(JSONObject jsonGraphData, int freq , String type)
  {
    try
    {
      logger.log(Level.INFO, "method called for type = " + type);
      
      ArrayList<MetricData> listData = new ArrayList<MetricData>();
      
      Set keySet  = jsonGraphData.keySet();
      
      if(keySet.size() < 1)
      {
        logger.log(Level.INFO, "Graph Metrics is not available for " + type);
        return null;
      }
      
      Iterator itrTest = keySet.iterator();

      while(itrTest.hasNext())
      {
        Object keyValue  = itrTest.next();   
        
        if(jsonGraphData.get(keyValue) == null)
          return null;
        
        JSONObject graphWithDataJson = (JSONObject)jsonGraphData.get(keyValue);
           
        Set keys  = graphWithDataJson.keySet();
        Iterator itr = keys.iterator();
           
        while(itr.hasNext())
        {
          String key = (String)itr.next();
          MetricData metricData = new MetricData();
      
          JSONObject graphJsonObj = (JSONObject)graphWithDataJson.get(key);
          String graphName = (String)graphJsonObj.get("graphMetricPath");
        
          metricData.setMetricPath(graphName.substring(graphName.indexOf("|") + 1));
          metricData.setFrequency(String.valueOf(freq));
    
          JSONArray jsonArray = (JSONArray)graphJsonObj.get("graphMetricValues");
      
          ArrayList<MetricValues> list = new ArrayList<MetricValues>();
      
          for (Object jsonArray1 : jsonArray)
          {
            MetricValues values =  new MetricValues();
            JSONObject graphValues = (JSONObject) jsonArray1;
            String currVal = String.valueOf(graphValues.get("currentValue"));
            String maxVal  = String.valueOf(graphValues.get("maxValue"));
            String minVal = String.valueOf(graphValues.get("minValue"));
            String avg  = String.valueOf(graphValues.get("avgValue"));
            long timeStamp  = (Long)graphValues.get("timeStamp");
            values.setValue((Double)graphValues.get("currentValue"));
            values.setMax((Double)graphValues.get("maxValue"));
            values.setMin(getMinForMetric((Double)graphValues.get("minValue")));
            values.setStartTimeInMillis(timeStamp);
            list.add(values);
           }     
           
           metricData.setMetricValues(list);
           listData.add(metricData);
        }
      }
      return listData;
    }
    catch(Exception ex)
    {
      logger.log(Level.SEVERE, "Error in parsing previous or baseline metrics stats" );
      logger.log(Level.SEVERE, "---" + ex.getMessage());
      return null;
    }
  }
  
  private double getMinForMetric(double metricValue) 
  {
    if(metricValue == Double.MAX_VALUE)
      return 0.0;
    else
      return metricValue;
  }
  
  public static void main(String args[]) 
  {
    String[] METRIC_PATHS =  new String[]{"Transactions Started/Second", "Transactions Completed/Second", "Transactions Successful/Second", "Average Transaction Response Time (Secs)", "Transactions Completed","Transactions Success" };
    int graphId [] = new int[]{7,8,9,3,5,6};
    int groupId [] = new int[]{6,6,6,6,6,6};
   // NetStormConnectionManager ns = new NetStormConnectionManager("http://localhost:8080", "netstorm", "netsrm");
  
   // ns.testNSConnection(new StringBuffer());
   // ns.fetchMetricData(METRIC_PATHS, 0, groupId, graphId,3334, "N");
  }
    
}
