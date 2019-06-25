/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jenkins.pipeline;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author preety.yadav
 */
public class NetStormResultsPublisher extends Recorder implements SimpleBuildStep  {
     private transient static final Logger logger = Logger.getLogger(NetStormResultsPublisher.class.getName());
    

    
  private static final String DEFAULT_USERNAME = "netstorm";// Default user name for NetStorm
  private static final String DEFAULT_TEST_METRIC = "Average Transaction Response Time (Secs)";// Dafault Test Metric

    public void setUsername(final String username) 
  {
    this.username = username;
  }

  public String getPassword() 
  {
    return password;
  }

  public void setPassword(final String password) 
  {
    this.password = password;
  }

  public String getNetstormUri() 
  {
    return netstormUri;
  }

  public void setNetstormUri(final String netstormUri) 
  {
    this.netstormUri = netstormUri;
  }

  public String getUsername() 
  {
    return username;
  }
  
  public String getHtmlTablePath()
  {
    if(htmlTable != null)
    {
      if(htmlTable.containsKey("htmlTablePath"))
        htmlTablePath = (String)htmlTable.get("htmlTablePath");  
    }
    return htmlTablePath;
  }
    
    
   public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

  //This is used to show post build action item
  @Override
  public String getDisplayName() 
  {
    return LocalMessages.PUBLISHER_DISPLAYNAME.toString();
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
    
 public FormValidation doCheckNetstormUri(@QueryParameter final String netstormUri)
 {
   return  FieldValidator.validateURLConnectionString(netstormUri);
 }
 
 public FormValidation doCheckPassword(@QueryParameter final String password)
 {
   return  FieldValidator.validatePassword(password);
 }
 
 public FormValidation doCheckUsername(@QueryParameter final String username)
 {
   return  FieldValidator.validatePassword(username);
 }
 
 public FormValidation doCheckHtmlTablePath(@QueryParameter final String htmlTablePath)
 {
   return FieldValidator.validateHtmlTablePath(htmlTablePath);
 }
     
 /*
  Need to test connection on given credientials
  */
 public FormValidation doTestNetStormConnection(@QueryParameter("netstormUri") final String netstormRestUri, @QueryParameter("username") final String username, @QueryParameter("password") final String password ) 
 {
   FormValidation validationResult;
   
   StringBuffer errMsg = new StringBuffer();
  
   if (FieldValidator.isEmptyField(netstormRestUri))
   {
     return validationResult = FormValidation.error("URL connection string cannot be empty and should start with http:// or https://");
   } 
   else if (!(netstormRestUri.startsWith("http://") || netstormRestUri.startsWith("https://"))) 
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
   
  NetStormConnectionManager connection = new NetStormConnectionManager(netstormRestUri, username, password);
    
   if (!connection.testNSConnection(errMsg)) 
   { 
     validationResult = FormValidation.warning("Connection to netstorm unsuccessful, due to: "+  errMsg);
   }
  else
     validationResult = FormValidation.ok("Connection successful");
 
   return validationResult;
 }
}

@Extension
public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
 

/**
 * Below fields are configured via the <code>config.jelly</code> page.
 */
 private String netstormUri = "";
 private String username = "";
 private String password = "";
 private JSONObject htmlTable = new JSONObject();
 private String htmlTablePath = null;
  
 @DataBoundConstructor
 public NetStormResultsPublisher(final String netstormUri, final String username,final String password, final JSONObject htmlTable)
 {
   System.out.println("--- getting constructor parmeter== "+netstormUri +", username = "+username);
   setNetstormUri(netstormUri);
   setUsername(username);
   setPassword(password);
   this.htmlTable = htmlTable;
 }
 

 NetStormResultsPublisher(final String netstormUri, final String username,final String password, final String htmlTable) {
     System.out.println("--- getting constructor parmeter== "+netstormUri +", username = "+username);
   setNetstormUri(netstormUri);
   setUsername(username);
   setPassword(password);
 }
 
 public void generateReport() {
     
       NetStormConnectionManager connection = new NetStormConnectionManager(netstormUri, username, password);
    
       logger.log(Level.SEVERE, "Verify connection to NetStorm interface ..."+ connection);
       
          NetStormDataCollector dataCollector = new NetStormDataCollector(connection, null , 4226, "1");
          
            try
    {
      NetStormReport report = dataCollector.createReportFromMeasurements();
    
      NetStormBuildAction buildAction = new NetStormBuildAction(null, report);
     // ((AbstractBuild<?,?>) run).addAction(buildAction);

     
       logger.log(Level.SEVERE,"Ready building NetStorm report");
    //  logger.println("Ready building NetStorm report");
    
     //  mark the build as unstable or failure depending on the outcome.
      List<NetStormReport> previousReportList = new ArrayList<NetStormReport>();;
     //logger.println("Number of old reports located for average: " + previousReportList.size());
     
     logger.log(Level.SEVERE,"previous report list------->>> " + previousReportList);
     
      double averageOverTime = calculateAverageBasedOnPreviousReports(previousReportList);
            logger.log(Level.SEVERE,"Ready building NetStorm data " + averageOverTime);
     

     double currentReportAverage = report.getAverageForMetric(DEFAULT_TEST_METRIC);
      logger.log(Level.SEVERE,"Ready building NetStorm currentReportAverage    " + currentReportAverage);
     
      
      /*
      * Method to execute a perform methods for running a test
      */
      
//      Run r;
//      Launcher lr;
//      TaskListener tr;
//      FilePath fp = new FilePath("C:\\Users\\prateek.jain\\Desktop\\jenkins.txt");
//      perform(, fp, lr, tr);   
 //     logger.println("Metric: " + DEFAULT_TEST_METRIC + "% . Build status is: " + ((AbstractBuild<?,?>) run).getResult());
    }
    catch(Exception e)
    {
         logger.log(Level.SEVERE,"Ready building NetStorm currentReportAverage    " + e);
    //  logger.println("Not able to create netstorm report.may be some configuration issue in running scenario.");
      return ;
    }
 
   
 }
 @Override
 public BuildStepDescriptor<Publisher> getDescriptor()
 {

    return DESCRIPTOR;
 }
 
 
  @Override
 public Action getProjectAction(AbstractProject<?, ?> project) 
 {
   return null;//new NetStormProjectAction(project,  NetStormDataCollector.getAvailableMetricPaths());
 }

   @Override
 public BuildStepMonitor getRequiredMonitorService() 
 {
   // No synchronization necessary between builds
   return BuildStepMonitor.NONE;
 }
 
 
 
      @Override
    public void perform(Run<?, ?> run, FilePath fp, Launcher lnchr, TaskListener taskListener) throws InterruptedException, IOException {
     Map<String, String> envs = run instanceof AbstractBuild ? ((AbstractBuild<?,?>) run).getBuildVariables() : Collections.<String, String>emptyMap();   
    PrintStream logger = taskListener.getLogger();
    StringBuffer errMsg = new StringBuffer();
    
     NetStormConnectionManager connection = new NetStormConnectionManager(netstormUri, username, password);
    
    logger.println("Verify connection to NetStorm interface ...");
    
    if (!connection.testNSConnection(errMsg)) 
    {
      logger.println("Connection to netstorm unsuccessful, cannot to proceed to generate report.");
      logger.println("Error: " + errMsg);
      
      if (run.getResult().isBetterOrEqualTo(Result.UNSTABLE))
        run.setResult(Result.FAILURE);
      
      return ;
    }
    
    
     if (run.getResult() == Result.FAILURE || run.getResult() == Result.ABORTED) {
          return ;
        }
     
    
    
//    if (run.getResult().isWorseOrEqualTo(Result.UNSTABLE))
//    {
//      run.setResult(Result.FAILURE);
//      return ;
//    }
    
    //This is used to set html table path. 
    if(getHtmlTablePath() != null)
    {
      //connection.setHtmlTablePath(getHtmlTablePath());
      logger.println("Html Report Path set as - " + htmlTablePath);
    }
    
      
//    Project buildProject = (Project) ((AbstractBuild<?,?>) run).getProject();   
//    List<Builder> blist = buildProject.getBuilders();
//    String testMode = "N";
//        
//    for(Builder currentBuilder : blist)
//    {
//      if(currentBuilder instanceof NetStormBuilder)
//      {
//         testMode = ((NetStormBuilder)currentBuilder).getTestMode();
//         break;
//      }
//    }
//    
    logger.println("Connection successful, continue to fetch measurements from netstorm Controller ...");
 //  run.doBuildNumber(NetStormBuilder.testRunNumber);
    NetStormDataCollector dataCollector = new NetStormDataCollector(connection,  run , Integer.parseInt(NetStormBuilder.testRunNumber), "T");
    
    try
    {
      NetStormReport report = dataCollector.createReportFromMeasurements();
    
      NetStormBuildAction buildAction = new NetStormBuildAction(run, report);
       run.addAction(buildAction);

      logger.println("Ready building NetStorm report");
    
     //  mark the build as unstable or failure depending on the outcome.
    List<NetStormReport> previousReportList = getListOfPreviousReports( run, report.getTimestamp());
    // logger.println("Number of old reports located for average: " + previousReportList.size());
//      double averageOverTime = calculateAverageBasedOnPreviousReports(previousReportList);

    // logger.println("Calculated average from previous reports: " + averageOverTime);

     double currentReportAverage = report.getAverageForMetric(DEFAULT_TEST_METRIC);
      logger.println("Current report average: " + currentReportAverage);
    
     // logger.println("Metric: " + DEFAULT_TEST_METRIC + "% . Build status is: " + ((AbstractBuild<?,?>) run).getResult());
    }
    catch(Exception e)
    {
      logger.println("Not able to create netstorm report.may be some configuration issue in running scenario.");
      return ;
    }
    return ;
     
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
   
   
  private List<NetStormReport> getListOfPreviousReports(final Run build, final long currentTimestamp) 
  {
    final List<NetStormReport> previousReports = new ArrayList<NetStormReport>();

   // final List<? extends AbstractBuild<?, ?>> builds = build.getProject().getBuilds();
    final NetStormBuildAction performanceBuildAction = build.getAction(NetStormBuildAction.class);
    
        logger.log(Level.INFO, "data in the performence build action = "+ performanceBuildAction);
    
    previousReports.add(performanceBuildAction.getBuildActionResultsDisplay().getNetStormReport());
    
    logger.log(Level.INFO, "data in the get result display = "+ performanceBuildAction.getBuildActionResultsDisplay().getNetStormReport());
//    for (AbstractBuild<?, ?> currentBuild : builds) 
//    {
//      final NetStormBuildAction performanceBuildAction = currentBuild.getAction(NetStormBuildAction.class);
//      if (performanceBuildAction == null) 
//      {
//        continue;
//      }
//      
//      final NetStormReport report = performanceBuildAction.getBuildActionResultsDisplay().getNetStormReport();
//      
//      if (report != null && (report.getTimestamp() != currentTimestamp || builds.size() == 1)) 
//      {
//        previousReports.add(report);
//      }
//    }

    return previousReports;
  }

 
  public boolean isImportSelected()
  {
    if(getHtmlTablePath() == null)
      return false;
    else
      return true;
  }
}
