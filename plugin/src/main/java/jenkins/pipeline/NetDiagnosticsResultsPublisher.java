package jenkins.pipeline;


import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.BuildStep;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.EnvVars;
import hudson.FilePath;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import jenkins.tasks.SimpleBuildStep;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.*;
import jenkins.pipeline.NdConnectionManager;
import jenkins.pipeline.NetDiagnosticsParamtersForReport;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class NetDiagnosticsResultsPublisher extends Recorder implements SimpleBuildStep  
{

  private static final String DEFAULT_USERNAME = "netstorm";// Default user name for NetStorm
  private static final String DEFAULT_TEST_METRIC = "Average Transaction Response Time (Secs)";// Dafault Test Metric  
  private static final String fileName = "jenkins_check_rule"; 

    @Override
    public void perform(Run<?, ?> run, FilePath fp, Launcher lnchr, TaskListener listener) throws InterruptedException, IOException {
       Map<String, String> env = run instanceof AbstractBuild ? ((AbstractBuild<?,?>) run).getBuildVariables() : Collections.<String, String>emptyMap();    
     PrintStream logger = listener.getLogger();
   StringBuffer errMsg = new StringBuffer();
   
   
   
  // EnvVars env = build.getEnvironment(listener);
   String curStart = "01/30/2018 20:40:54";
   String curEnd = "01/30/2018 20:45:54";
   String path = "";
   String jobName = "";
   String criticalThreshold = "";
   String warningThreshold = "";
   String overallThreshold = "";
   Boolean fileUpload = false;

   Set keyset = env.keySet();
	 
   for(Object key : keyset)
   {
     Object value = env.get(key);
     
     String keyEnv = (String)key;
     
     if(value instanceof String)
     {
       if(keyEnv.startsWith("curStartTime"))
    	 {
    	   curStart =(String) value;
    	   setCurStartTime(curStart);
    	 }
    	 
    	 if(keyEnv.startsWith("curEndTime"))
    	 {
           curEnd =(String) value;
    	   setCurEndTime(curEnd);
    	 }

        if(keyEnv.startsWith("JENKINS_HOME"))
        {
          path = (String) value;
        }

        if(keyEnv.startsWith("JOB_NAME"))
        {
          jobName = (String) value;
        }

        if(keyEnv.equalsIgnoreCase("criticalThreshold"))
        {
          criticalThreshold = (String) value;
        }

        if(keyEnv.equalsIgnoreCase("warningThreshold"))
        {
          warningThreshold = (String) value;
        }

        if(keyEnv.equalsIgnoreCase("overallThreshold"))
        {
          overallThreshold = (String) value;
        }

        if(keyEnv.equalsIgnoreCase(fileName))
        {
          fileUpload = true;
        }

      }
    }

    JSONObject json = null;
    if(fileUpload)
    {
      File file = new File(path+"/workspace/"+ jobName +"/"+fileName);

      if(file.exists())
      {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {

          if(line.contains("GroupName") || line.contains("GraphName") ||line.contains("VectorName") || line.contains("RuleDesc"))
          {
            line = line.trim().replaceAll("\\s", "@@");
          }

          builder.append(line.trim());
        }
        json = (JSONObject) JSONSerializer.toJSON(builder.toString());
      }
    }
   
   //Getting initial duration values
   if(getInitDurationValues() != null)
   {
     String duration = getInitDurationValues();
     String values[] = duration.split("@");
	   
     ndParams.setInitStartTime(values[0]);
     ndParams.setInitEndTime(values[1]);
   }
     
   
     ndParams.setPrevDuration(prevDuration);
   
   NdConnectionManager connection = new NdConnectionManager(netdiagnosticsUri, username, password, ndParams, true);
   connection.setCurStart("01/30/2018 20:35:54");
   connection.setCurEnd("01/30/2018 20:45:54");
   connection.setJkRule(json);
   connection.setCritical(criticalThreshold);
   connection.setWarning(warningThreshold);
   connection.setOverall(overallThreshold);  

 
   logger.println("Verify connection to NetDiagnostics interface ...");
   
   if (!connection.testNDConnection(errMsg, null)) 
   {
     logger.println("Connection to netdiagnostics unsuccessful, cannot to proceed to generate report.");
     logger.println("Error: " + errMsg);
       
     return ;
   }
   logger.println("Connection Done");
   //Need to pass test run number
   
   //For setting duration in case of netdiagnostics
   try{
	SimpleDateFormat trDateFormat = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	Date userDate = trDateFormat.parse(curStartTime); 
	long absoluteDateInMillies = userDate.getTime();	 
	   
	Date userEndDate = trDateFormat.parse(curEndTime); 
	long absoluteEndDateInMillies = userEndDate.getTime();	 
	   
	   
	long durationCal = absoluteEndDateInMillies - absoluteDateInMillies;
	duration = (int) (durationCal / (1000*60));
	    
      } catch(Exception ex){
	   ex.printStackTrace();
    }

   NetStormDataCollector dataCollector = new NetStormDataCollector(connection, run , 50000, "T", true, duration);
   
   
   try
   {
     NetStormReport report = dataCollector.createReportFromMeasurements();
     
     NetStormBuildAction buildAction = new NetStormBuildAction(run, report, true);
     
    run.addAction(buildAction);
   
     //change status of build depending upon the status of report.
     TestReport tstRpt =  report.getTestReport();
      if(tstRpt.getOverAllStatus().equals("FAIL"))
        run.setResult(Result.FAILURE);

     logger.println("Ready building NetDiagnostics report");
  // mark the build as unstable or failure depending on the outcome.
     List<NetStormReport> previousReportList = getListOfPreviousReports(run, report.getTimestamp());
     
     double averageOverTime = calculateAverageBasedOnPreviousReports(previousReportList);
     logger.println("Calculated average from previous reports: " + averageOverTime);

     double currentReportAverage = report.getAverageForMetric(DEFAULT_TEST_METRIC);
     logger.println("Metric: " + DEFAULT_TEST_METRIC + "% . Build status is: " + ((Run<?,?>) run).getResult());
   }
   catch(Exception e)
   {
     logger.println("Not able to create netstorm report.may be some configuration issue in running scenario.");
     return ;
   }
   
   
   return ;
               
 }
    
    public void geterate(AbstractProject job) {
        
       
    }
  
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

  //This is used to show post build action item
  @Override
  public String getDisplayName()
  {
    return LocalMessages.ND_PUBLISHER_DISPLAYNAME.toString();
  }

  @Override
  public boolean isApplicable(Class<? extends AbstractProject> jobType)
  {
    return true;
  }

  public String getDefaultUsername()
  {
      return DEFAULT_USERNAME;
  }

 public String getDefaultTestMetric()
 {
   return DEFAULT_TEST_METRIC;
 }
 public FormValidation doCheckNetdiagnosticsUri(@QueryParameter final String netdiagnosticsUri)
 {
   return  FieldValidator.validateURLConnectionString(netdiagnosticsUri);
 }
 
 public FormValidation doCheckPassword(@QueryParameter final String password)
 {
   return  FieldValidator.validatePassword(password);
 }
 
 public FormValidation doCheckUsername(@QueryParameter final String username)
 {
   return  FieldValidator.validateUsername(username);
 }
 
 public FormValidation doCheckWarThreshold(@QueryParameter final String warThreshold) {
   return FieldValidator.validateThresholdValues(warThreshold);
} 
 
 public FormValidation doCheckCriThreshold(@QueryParameter final String criThreshold) {
   return FieldValidator.validateThresholdValues(criThreshold);
} 
  
 
 public FormValidation doCheckFailThreshold(@QueryParameter final String failThreshold) {
   return FieldValidator.validateThresholdValues(failThreshold);
} 
 
 public FormValidation doCheckBaseStartTime(@QueryParameter final String baseStartTime) throws ParseException {
   return FieldValidator.validateDateTime(baseStartTime);
} 
 
 public FormValidation doCheckBaseEndTime(@QueryParameter final String baseEndTime) throws ParseException {
   return FieldValidator.validateDateTime(baseEndTime);
}  
 
 /*
 Need to test connection on given credientials
 */
public FormValidation doTestNetDiagnosticsConnection(@QueryParameter("netdiagnosticsUri") final String netdiagnosticRestUri, @QueryParameter("username") final String username, @QueryParameter("password") final String password ) 
{
  FormValidation validationResult;
  
  StringBuffer errMsg = new StringBuffer();
 
  if (FieldValidator.isEmptyField(netdiagnosticRestUri))
  {
    return validationResult = FormValidation.error("URL connection string cannot be empty and should start with http:// or https://");
  } 
  else if (!(netdiagnosticRestUri.startsWith("http://") || netdiagnosticRestUri.startsWith("https://"))) 
  {
    return validationResult = FormValidation.error("URL connection st should start with http:// or https://");
  }
  
  if(FieldValidator.isEmptyField(username))
  {
    return validationResult = FormValidation.error("Please enter user name.");
  }

  if(FieldValidator.isEmptyField(password))
  {
    return validationResult = FormValidation.error("Please enter password.");
  }
  
  NdConnectionManager connection = new NdConnectionManager(netdiagnosticRestUri, username, password, true);
 
  String check = netdiagnosticRestUri + "@@" + username +"@@" + password;
  if (!connection.testNDConnection(errMsg, check)) 
  { 
    validationResult = FormValidation.warning("Connection to netdiagnostics unsuccessful, due to: "+  errMsg);
  }
  else
    validationResult = FormValidation.ok("Connection successful");

  return validationResult;
}

 

}
 @Extension
public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

 

@Override
 public BuildStepDescriptor<Publisher> getDescriptor()
 {
    return DESCRIPTOR;
 }
 
 private String netdiagnosticsUri = "";
 private String username = "";
 private String password = "";
 private boolean prevDuration;
 private JSONObject initDuration = null;
 private  String curStartTime;
 private String curEndTime;
 private String baseStartTime;
 private String baseEndTime;
 private  String checkProfilePath;
 private String initStartTime;
 private String initEndTime;
 private String criThreshold;
 private String warThreshold;
 private String failThreshold;
 int duration;
 
 
public String getCriThreshold() {
	return criThreshold;
}

public void setCriThreshold(String criThreshold) {
	this.criThreshold = criThreshold;
}

public String getWarThreshold() {
	return warThreshold;
}

public void setWarThreshold(String warThreshold) {
	this.warThreshold = warThreshold;
}

public String getFailThreshold() {
	return failThreshold;
}

public void setFailThreshold(String failThreshold) {
	this.failThreshold = failThreshold;
}

public String getInitStartTime() {
	return initStartTime;
}

public void setInitStartTime(String initStartTime) {
	this.initStartTime = initStartTime;
}

public String getInitEndTime() {
	return initEndTime;
}

public void setInitEndTime(String initEndTime) {
	this.initEndTime = initEndTime;
}

public String getCurStartTime() {
	return curStartTime;
}

public void setCurStartTime(String curStartTime) {
	this.curStartTime = curStartTime;
}

public String getCurEndTime() {
	return curEndTime;
}

public void setCurEndTime(String curEndTime) {
	this.curEndTime = curEndTime;
}

public String getBaseStartTime() {
	return baseStartTime;
}

public void setBaseStartTime(String baseStartTime) {
	this.baseStartTime = baseStartTime;
}

public String getBaseEndTime() {
	return baseEndTime;
}

public void setBaseEndTime(String baseEndTime) {
	this.baseEndTime = baseEndTime;
}

public String getCheckProfilePath() {
	return checkProfilePath;
}

public void setCheckProfilePath(String checkProfilePath) {
	this.checkProfilePath = checkProfilePath;
}

public String getNetdiagnosticsUri() {
	return netdiagnosticsUri;
}

public void setNetdiagnosticsUri(String netdiagnosticsUri) {
	this.netdiagnosticsUri = netdiagnosticsUri;
}

public String getUsername() {
	return username;
}

public void setUsername(String username) {
	this.username = username;
}

public String getPassword() {
	return password;
}

public void setPassword(String password) {
	this.password = password;
}

NetDiagnosticsParamtersForReport  ndParams = new NetDiagnosticsParamtersForReport();

@DataBoundConstructor
 public NetDiagnosticsResultsPublisher(final String netdiagnosticsUri, final String username,
         final String password, final String baseStartTime, final String baseEndTime, 
         final boolean prevDuration, final JSONObject initDuration, final String initEndTime,
         final String initStartTime,final String checkProfilePath, final String criThreshold,
         final String warThreshold, final String failThreshold)
 {

   /*creating json for sending the paramters to get the response json. */
   setNetdiagnosticsUri(netdiagnosticsUri);
   setUsername(username);
   setPassword(password);
   setBaseStartTime(baseStartTime);
   setBaseEndTime(baseEndTime);
   setCurEndTime(this.getCurEndTime());
   setCurStartTime(this.getCurStartTime());
   setCheckProfilePath(checkProfilePath);
   setCriThreshold(criThreshold);
   setWarThreshold(warThreshold);
   setFailThreshold(failThreshold);
   this.initDuration = initDuration;
   this.prevDuration = prevDuration;
   ndParams.setBaseEndTime(baseEndTime);
   ndParams.setBaseStartTime(baseStartTime);
   ndParams.setCheckProfilePath(checkProfilePath);

   if(this.getCurEndTime() != "")
      ndParams.setCurEndTime(this.getCurEndTime());
    else
      ndParams.setCurEndTime(curEndTime);
    
    if(this.getCurStartTime() != "")
      ndParams.setCurStartTime(this.getCurStartTime());
    else
      ndParams.setCurStartTime(curStartTime);
    
    if(this.getCriThreshold() != "")
      ndParams.setCritiThreshold(this.getCriThreshold());
    else
      ndParams.setCritiThreshold(criThreshold);

    if(this.getWarThreshold() != "")
      ndParams.setWarThreshold(this.getWarThreshold());
    else
      ndParams.setWarThreshold(warThreshold);

    if(this.getFailThreshold() != "")
      ndParams.setFailThreshold(this.getFailThreshold());
    else
      ndParams.setFailThreshold(failThreshold);

 }

 public BuildStepMonitor getRequiredMonitorService()
 {
   // No synchronization necessary between builds
   return BuildStepMonitor.NONE;
 }
  
   private double calculateAverageBasedOnPreviousReports(final List<NetStormReport> reports)
   {
     double calculatedSum = 0;
     int numberOfMeasurements = 0;
     for (NetStormReport report : reports) 
     {
       double value = report.getAverageForMetric(DEFAULT_TEST_METRIC);
     
       if (value >= 0)
       {
         calculatedSum += value;
         numberOfMeasurements++;
       }
     }

     double result = -1;
     if (numberOfMeasurements > 0)
     {
       result = calculatedSum / numberOfMeasurements;
     }

     return result;
   }

   
   private List<NetStormReport> getListOfPreviousReports(final Run<?, ?> build, final long currentTimestamp) 
   {
     final List<NetStormReport> previousReports = new ArrayList<NetStormReport>();
     
     final NetStormBuildAction performanceBuildAction = build.getAction(NetStormBuildAction.class);
     
     
     /*
      * Adding current report object in to list.
      */
     previousReports.add(performanceBuildAction.getBuildActionResultsDisplay().getNetStormReport());

//     final List<? extends Run<?, ?>> builds = build.getProject().getBuilds();

    
//     for (Run<?, ?> currentBuild : builds) 
//     {
//       final NetStormBuildAction performanceBuildAction = currentBuild.getAction(NetStormBuildAction.class);
//       if (performanceBuildAction == null) 
//       {
//         continue;
//       }
//       
//       final NetStormReport report = performanceBuildAction.getBuildActionResultsDisplay().getNetStormReport();
//       
//       if (report != null && (report.getTimestamp() != currentTimestamp || builds.size() == 1)) 
//       {
//         previousReports.add(report);
//       }
//     }

     return previousReports;
   }

   public boolean isInitDuration()
   {
     if(getInitDurationValues() == null)
       return false;
     else
       return true;
   }
   
   public boolean isPrevDuration()
   {
	return prevDuration;
   }
   
   
   public String getInitDurationValues()
   {
     if(initDuration != null)
     {
       if(initDuration.containsKey("initStartTime"))
       {
         initStartTime = (String)initDuration.get("initStartTime");
         setInitStartTime(initStartTime);
       }
       
       if(initDuration.containsKey("initEndTime"))
       {
           initEndTime = (String)initDuration.get("initEndTime");
           setInitEndTime(initEndTime);
       }   
         
     }
     
     if(initStartTime != null && initEndTime != null)
       return initStartTime+"@"+initEndTime;
     else
    	return null;
   }
}
