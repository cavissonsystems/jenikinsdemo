package jenkins.pipeline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


import jenkins.pipeline.NetDiagnosticsParamtersForReport;



public class NdConnectionManager {

  private final String URLConnectionString;
  private URLConnection urlConn = null;
  private transient final Logger logger = Logger.getLogger(NdConnectionManager.class.getName());
  private String username = "";
  private String password = "";
  NetDiagnosticsParamtersForReport ndParam;
  private ObjectOutputStream oos = null;
  private ObjectInputStream ois = null;
  private String restUrl = "";
  private String result;
  private boolean isNDE = false;
  private String curStart;
  private String curEnd;
  private JSONObject jkRule = new JSONObject();
  private String critical;
  private String warning;
  private String overall;

   public String getCritical() {
    return critical;
  }

  public void setCritical(String critical) {
    this.critical = critical;
  }

  public String getWarning() {
    return warning;
  }

  public void setWarning(String warning) {
    this.warning = warning;
  }

  public String getOverall() {
    return overall;
  }

  public void setOverall(String overall) {
    this.overall = overall;
  }

  public JSONObject getJkRule() {
    return jkRule;
  }

  public void setJkRule(JSONObject jkRule) {
    this.jkRule = jkRule;
  }

  public String getCurStart() {
    return curStart;
  }

  public void setCurStart(String curStart) {
    this.curStart = curStart;
  }

  public String getCurEnd() {
    return curEnd;
  }

  public void setCurEnd(String curEnd) {
    this.curEnd = curEnd;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  static
  { 
    disableSslVerification();
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

  public NdConnectionManager(String uRLConnectionString, String username, String password, boolean isNDE) {

    URLConnectionString = uRLConnectionString;
    this.username = username;
    this.password = password;
    this.restUrl = uRLConnectionString;
    this.isNDE = isNDE;
  }

  public NdConnectionManager(String uRLConnectionString, String username, String password, NetDiagnosticsParamtersForReport ndParam, boolean isNDE) {

    URLConnectionString = uRLConnectionString;
    this.username = username;
    this.password = password;
    this.ndParam = ndParam;
    this.restUrl = uRLConnectionString;
    this.isNDE = isNDE;
  }

  /**
   * This Method makes connection to netdiagnostics.
   *
   * @param errMsg
   * @return true , if Successfully connected and authenticated false ,
   * otherwise
   */
  public boolean testNDConnection(StringBuffer errMsg, String test) 
  {
    logger.log(Level.INFO, "testNDConnection() method is  called. rest url -"+ restUrl);
    if(checkAndMakeConnection(URLConnectionString, restUrl, errMsg, test))
    {
      logger.log(Level.INFO, "After check connection method.");

      if((getResult() == null))
      {
	logger.log(Level.INFO, "Connection failure, please check whether Connection URI is specified correctly");
	errMsg.append("Connection failure, please check whether Connection URI is specified correctly");
	return false;
      }
      else 
      {
	logger.log(Level.INFO, "Successfully Authenticated.");   
	return true;
      }

    } 
    else 
    {
      logger.log(Level.INFO, "Connection failure, please check whether Connection URI is specified correctly");
      errMsg.append("Connection failure, please check whether Connection URI is specified correctly");
      return false;
    }

  }

  private boolean checkAndMakeConnection(String urlString, String restUrl, StringBuffer errMsg)
  {
    return checkAndMakeConnection(urlString, restUrl,errMsg, null);
  }

  /**
   * This method checks connection with netstorm
   *
   * @param urlString
   * @param servletPath
   * @param errMsg
   * @param aa 
   * @return true if connection successfully made false, otherwise
   */
  private boolean checkAndMakeConnection(String urlString, String restUrl, StringBuffer errMsg, String test)
  {
    logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments restUrl : ", new Object[]{restUrl});
    try
    {
      if(ndParam != null)
      {
	logger.log(Level.INFO, "checkAndMakeConnection method called. with ndParam "+  ndParam.toString());
	ndParam.setCurStartTime(this.getCurStart().replace(" ", "@"));
	ndParam.setCurEndTime(this.getCurEnd().replace(" ", "@"));
	ndParam.setBaseStartTime(ndParam.getBaseStartTime().replace(" ", "@"));
	ndParam.setBaseEndTime(ndParam.getBaseEndTime().replace(" ", "@"));
	if(ndParam.getInitStartTime() != null)
	  ndParam.setInitStartTime(ndParam.getInitStartTime().replace(" ", "@"));

	if(ndParam.getInitEndTime() != null)
	  ndParam.setInitEndTime(ndParam.getInitEndTime().replace(" ", "@"));
      }

      if(this.getCritical() != null && this.getCritical() != "")
        ndParam.setCritiThreshold(this.getCritical().replace(" ", "@"));

      if(this.getWarning() != null && this.getWarning() != "")
        ndParam.setWarThreshold(this.getWarning().replace(" ", "@"));

      if(this.getOverall() != null && this.getOverall() != "")
        ndParam.setFailThreshold(this.getOverall().replace(" ", "@"));

      URL url ;

      if(test == null || test.equals(null))
	url = new URL(restUrl+"ProductUI/productSummary/jenkinsService/reportData?&reportParam="+ndParam.toString()+"&status=false"+"&chkRule="+this.getJkRule());
      else
	url = new URL(restUrl+"ProductUI/productSummary/jenkinsService/reportData?&reportParam="+test+"&status=true"); //for only test connection

      logger.log(Level.INFO, "checkAndMakeConnection method called. with arguments url"+  url);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");
      if (conn.getResponseCode() != 200) {
	throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
      setResult(br.readLine());
      logger.log(Level.INFO, "RESPONSE -> "+getResult());
      
      JSONObject resonseObj = null;
      resonseObj =  (JSONObject) JSONSerializer.toJSON(this.result);
      String check = resonseObj.get("status").toString();
      System.out.println("check -- "+check);
      if(check.equals("true"))
        return true;
      else
	return false;
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

  public MetricDataContainer fetchMetricData(String metrics[], int durationInMinutes, int groupIds[], int graphIds[], int testRun, String testMode)
  {
    logger.log(Level.INFO, "fetchMetricData() called. get resulet"+ getResult());
    JSONObject resonseObj = null;
    resonseObj =  (JSONObject) JSONSerializer.toJSON(this.result);
    if(resonseObj == null)
    {
      logger.log(Level.SEVERE, "Not able to get response form server due to: ");
      return null;
    }

    return parseJSONData(resonseObj, testMode);
  }

  private MetricDataContainer parseJSONData(JSONObject resonseObj, String testMode)
  {
    logger.log(Level.INFO, "parseJSONData() called.");

    MetricDataContainer metricDataContainer = new MetricDataContainer();
    logger.log(Level.INFO,"Metric Data:" + metricDataContainer );
    logger.log(Level.INFO,"Recived response from : " + resonseObj );
    System.out.println("Recived response from : " + resonseObj);

    try{
      ArrayList<MetricData> dataList = new ArrayList<MetricData>();
      JSONObject jsonGraphs = (JSONObject)resonseObj.get("graphs");
      logger.log(Level.INFO,"Recived response from graphs : " + jsonGraphs );
      int freq = ((Integer)resonseObj.get("frequency"))/1000; 
      logger.log(Level.INFO,"Recived response from : frq = " + freq );
      metricDataContainer.setFrequency(freq);

      if(resonseObj.containsKey("customHTMLReport"))
	metricDataContainer.setCustomHTMLReport((String)resonseObj.get("customHTMLReport"));

      TestReport testReport = new TestReport();
      if("T".equals(testMode))
      {
	logger.log(Level.INFO,"Recived response inside test mode : "  );
	testReport = new TestReport();
	JSONObject jsonTestReportWholeObj = resonseObj.getJSONObject("testReport");
	logger.log(Level.INFO,"Recived response from whole report : " + jsonTestReportWholeObj );
	JSONObject jsonTestReport = jsonTestReportWholeObj.getJSONObject("members");

	logger.log(Level.INFO,"Recived response from test report : " + jsonTestReport );
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
	try{
	  currentDateTime = jsonTestReport.getString("Current Date Time");
	  previousDescription = jsonTestReport.getString("Previous Description"); 
	  baselineDescription = jsonTestReport.getString("Baseline Description");
	  currentDescription =  jsonTestReport.getString("Current Description");
	  initialDescription = jsonTestReport.getString("Initial Description");
	}
	catch(Exception ex) {
	  logger.log(Level.SEVERE, "Error in parsing Test Report Data:" + ex);
	  logger.log(Level.SEVERE, "---" + ex.getMessage());
	}

	ArrayList<TestMetrics> testMetricsList = new ArrayList<TestMetrics>(metricsUnderTest.size());
	for(Object jsonData : metricsUnderTest)
	{  
	  JSONObject jsonObject = (JSONObject)jsonData;
	  String prevTestValue = jsonObject.getString("Prev Test Value ");
	  String baseLineValue = jsonObject.getString("Baseline Value ");
	  String initialValue = jsonObject.getString("Initial Value ");
	  String currValue = jsonObject.getString("Value");
	  String edLink = jsonObject.getString("link");
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
	  testMetric.setEdLink(edLink);
	  testMetric.setOperator(operator);
	  testMetric.setPrevTestRunValue(prevTestValue);
	  testMetric.setInitialValue(initialValue);
	  testMetric.setSLA(sla);
	  testMetric.setLinkForTrend(trendLink);
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

	if(!isNDE)
	{
	  for(int i = 0 ; i < transObj; i++)
	  {
	    JSONObject transactionJson = null;

	    if(i == 1)
	      transactionJson = jsonTestReport.getJSONObject("BASETOT");
	    else 
	    {  
	      if(jsonTestReport.has("TOT"))
		transactionJson = jsonTestReport.getJSONObject("TOT");
	      else
		transactionJson = jsonTestReport.getJSONObject("CTOT"); 
	    }

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
	      transactionStats.setTransTestRun("BASETOT");
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
	}
	else
	  testReport.setShowHideTransaction(true); 


	if(isNDE)
	{
	  String[] startDateTime = ndParam.getBaseStartTime().split("@");         
	  String[] endDateTime = ndParam.getBaseEndTime().split("@");

	  System.out.println("start time == "+startDateTime[0]);
	  System.out.println("end 9time == "+endDateTime[0]);
	  if(startDateTime[0].equals(endDateTime[0]))
	    baseLineDateTime = ndParam.getBaseStartTime().replace("@", " ") + " to "+ endDateTime[1];
	  //baseLineDateTime = startDateTime[0] + " to "+ endDateTime[1];
	  else
	    baseLineDateTime = ndParam.getBaseStartTime().replace("@", " ") + " to "+ ndParam.getBaseEndTime().replace("@", " ");


	  startDateTime = ndParam.getCurStartTime().split("@");
	  endDateTime = ndParam.getCurEndTime().split("@");
	  if(startDateTime[0].equals(endDateTime[0]))
	    currentDateTime = ndParam.getCurStartTime().replace("@", " ") + " to "+ endDateTime[1];
	  else
	    currentDateTime =  ndParam.getCurStartTime().replace("@", " ") + " to "+ ndParam.getCurEndTime().replace("@", " ");

	  if(ndParam.getInitEndTime() != null)
	  {
	    startDateTime = ndParam.getInitStartTime().split("@");	
	    endDateTime = ndParam.getInitEndTime().split("@");
	    if(startDateTime[0].equals(endDateTime[0]))
	      initialDateTime = ndParam.getInitStartTime().replace("@", " ") + " to "+ endDateTime[1];
	    else
	      initialDateTime = ndParam.getInitStartTime().replace("@", " ") + " to "+ ndParam.getInitEndTime().replace("@", " ");
	  } 

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
	testReport.setIpPortLabel(productName);
	testReport.setPreviousTestRun(previousTestRun);
	testReport.setTestRun(testRun);
	testReport.setNormalThreshold(normalThreshold);
	testReport.setCriticalThreshold(criticalThreshold);
	testReport.setCurrentDateTime(currentDateTime);
	testReport.setPreviousDescription(previousDescription);
	testReport.setBaselineDescription(baselineDescription);
	testReport.setInitialDescription(initialDescription);
	testReport.setCurrentDescription(currentDescription);
	metricDataContainer.setTestReport(testReport);
      }

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


}
