package jenkins.pipeline;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.*;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 */
public class NetStormBuilder extends Builder {

    private final String project;
    private final String subProject;
    private final String scenario;
    private final String URLConnectionString;
    private final String username;
    private final String password;
    private final String testMode ;
    private final String defaultTestMode = "true";
    private transient static final Logger logger = Logger.getLogger(NetStormBuilder.class.getName());
    private final String baselineTR;
    public static String testRunNumber = "5279";

    @DataBoundConstructor
    public NetStormBuilder(String URLConnectionString, String username, String password, String project,
            String subProject, String scenario, String testMode, String baselineTR) {
       logger.log(Level.INFO, "inside a constructor..............");
        
        this.project = project;
        this.subProject = subProject;
        this.scenario = scenario;
        this.URLConnectionString = URLConnectionString;
        this.username = username;
        this.password = password;
        this.testMode = testMode;
        this.baselineTR = baselineTR;
    }

    public String getProject() 
    {
      return project;
    }
    
    public String getDefaultTestMode()
    {
      return defaultTestMode;
    }

    public String getSubProject() {
        return subProject;
    }

    public String getScenario() {
        return scenario;
    }

    public String getURLConnectionString() {
        return URLConnectionString;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getTestMode() 
    {
      return testMode;
    }
    
    public String getBaselineTR()
    {
      return baselineTR;
    }
    @SuppressWarnings("deprecation")
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) 
    {   
      logger.log(Level.INFO, "Calling perform method()");  
      PrintStream logger = listener.getLogger();
      NetStormConnectionManager netstormConnectionManger = new NetStormConnectionManager(URLConnectionString, username, password, project, subProject, scenario, testMode, baselineTR);      
      StringBuffer errMsg = new StringBuffer();
      @SuppressWarnings({ "deprecation", "rawtypes" })
       Map  envVarMap = build.getEnvVars();
      
      @SuppressWarnings("rawtypes")
      Set keyset = envVarMap.keySet();
      
      //Duration Time/Sessions=NS_SESSION_12:00:05
      //Total Virtual Users=NS_NUM_USERS_213
      //Transaction Failure=NS_SLA_CHANGE_45
      //Server/Host=NS_SESSION_10.10.10.65
      String path = "";
      for(Object key : keyset)
      {
        Object value = envVarMap.get(key);
       	
       	if(key.equals("JENKINS_HOME"))
 	 path = (String)envVarMap.get(key);
       			
       			
        if(value instanceof String)
        {
           String envValue = (String) value;
 
           if(envValue.startsWith("NS_SESSION"))
           {
             String temp [] = envValue.split("_");
             if(temp.length > 2)
             {
                netstormConnectionManger.setDuration(temp[2]);
             }
           }
           else if(envValue.startsWith("NS_NUM_USERS"))
           {
             String temp [] = envValue.split("_");
             if(temp.length > 3)
                netstormConnectionManger.setvUsers(temp[3]);
           }  
           else if(envValue.startsWith("NS_SERVER_HOST"))
           {
             String temp [] = envValue.split("_");
             if(temp.length > 3)
                netstormConnectionManger.setServerHost(temp[3]);
           }  
           else if(envValue.startsWith("NS_SLA_CHANGE"))
           {
             String temp [] = envValue.split("_");
             if(temp.length > 3)
                netstormConnectionManger.addSLAValue(key.toString() , temp [3] );
           }
           else if(envValue.startsWith("NS_RAMP_UP_SEC") || envValue.startsWith("NS_RAMP_UP_MIN") || envValue.startsWith("NS_RAMP_UP_HR"))
           {
             String temp [] = envValue.split("_");
             if(temp.length > 4)
                netstormConnectionManger.setRampUp(temp[4] + "_" + temp[3]);
           }
           else if(envValue.startsWith("NS_TNAME"))
           {
             String tName = getSubString(envValue, 2, "_");
             if(!tName.equals(""))
               netstormConnectionManger.settName(tName);
           }
           else if(envValue.startsWith("NS_AUTOSCRIPT"))
           {
             String temp [] = envValue.split("_", 3);
             if(temp.length > 2)
                netstormConnectionManger.setAutoScript(temp[2]);
           }
           
           
        }
      }
     
      if(testMode == null)
      {
        logger.println("Please verify configured buit step, test profile mode is not selected.");
        return false;
      }
      
      if(getTestMode().equals("N"))
        logger.println("Starting test with scenario(" + project + "/" + subProject + "/" + scenario + ")");
      else
        logger.println("Starting test with test suite(" + project + "/" + subProject + "/" + scenario + ")");
      
      logger.println("NetStorm URI: " + URLConnectionString );
      
  
        
      HashMap result = netstormConnectionManger.startNetstormTest(errMsg ,logger );
      
      boolean status = (Boolean )result.get("STATUS");
      
      logger.println("Test Run Status - " + status);
      
      if(result.get("TESTRUN") != null && !result.get("TESTRUN").toString().trim().equals(""))
      {
        try
        {
          logger.println("Test Run  - " + result.get("TESTRUN"));
          
          build.setDisplayName((String)result.get("TESTRUN"));
          
          /*set a test run number in static refrence*/
          testRunNumber = (String)result.get("TESTRUN");
          
          if(result.get("ENV_NAME") != null && !result.get("ENV_NAME").toString().trim().equals(""))
            build.setDescription((String)result.get("ENV_NAME")); 
         
          
          //To set the host and user name in a file for using in other publisher.
          logger.println("path  - " + path);
          File dir = new File(path.trim()+"/Property");
          if (!dir.exists()) {
              if (dir.mkdir()) {
                  System.out.println("Directory is created!");
              } else {
                  System.out.println("Failed to create directory!");
              }
          }
          
          File file = new File(path.trim()+"/Property/" +((String)result.get("TESTRUN")).trim()+"_CavEnv.property");
          
          if(file.exists())
           file.delete();
          else
          {
            file.createNewFile();
            
            try
            {
              FileWriter fw = new FileWriter(file, true);
              BufferedWriter bw = new BufferedWriter(fw);
              bw.write("HostName="+URLConnectionString);
              bw.write("\n");
              bw.write("UserName="+username);
              bw.close();
            }
            catch (Exception e){
            	 System.out.println("Exception in writing in file - "+e);
            }
         }
          
          build.setResult(Result.SUCCESS);
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }
      }
      else
       build.setResult(Result.FAILURE);
      
      return status;
    }
    
    
     /*
      *  Method which is used to start a test 
      * it makes a connection with the m/c and authenticate
      *
     */
  public String  startTest() {
      try {
          StringBuffer errBuf = new  StringBuffer();
          
          FileOutputStream fout=new FileOutputStream("C:\\Users\\prateek.jain\\Desktop\\jenkins.txt");  
          PrintStream pout=new PrintStream(fout); 
          
          NetStormConnectionManager netstormConnectionManger = new NetStormConnectionManager(URLConnectionString, username, password,
          project, subProject, scenario, testMode, baselineTR);
          
          HashMap result =   netstormConnectionManger.startNetstormTest(errBuf , pout);
          
      
        if(result.get("TESTRUN") != null && !result.get("TESTRUN").toString().trim().equals(""))
        {
          /*set a test run number in static refrence*/
          testRunNumber = (String)result.get("TESTRUN");
          
          return netstormConnectionManger.getResult();
        }
      
        return result.toString();
      }catch(Exception e) {
          System.out.println("Error in startin a test"+ e);
          return "Error in starting a test";
      }
  } 
    
   
    /**      
	 * @param OrgString
	 * @param startIndex
	 * @param seperator
	 * @return
	 * ex.--  OrgString = NS_TNAME_FIRST_TEST ,startIndex = 2 ,seperator = "_" .
	 * 
	 *      ("NS_TNAME_FIRST_TEST", 2 , "_")   method returns FIRST_TEST.
	 *      
	 */
    public String getSubString(String OrgString, int startIndex, String seperator)
    {
      String f[] = OrgString.split(seperator);
      String result = "";
      if(startIndex <= f.length-1)
      {
        for(int i = startIndex ; i < f.length; i++)
	{
          if(i == startIndex)
	    result  = result + f[i] ;
          else
            result  = result + "_" + f[i]  ;
	}
      }
      return result;
    }
  
    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> 
    {
        public Descriptor() 
        {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            
        		  save();
            return true;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException
        {
          return super.newInstance(req, formData);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public String getDisplayName() {
            return Messages.NetStormBuilder_Task();
        }
        
        /**
         * 
         * @param password
         * @return 
         */
        public FormValidation doCheckPassword(@QueryParameter final String password) {
          
            return FieldValidator.validatePassword(password);
        }
        
        /**
         * 
         * @param username
         * @return 
         */
        public FormValidation doCheckUsername(@QueryParameter final String username) {
            return FieldValidator.validateUsername(username);
        }
        
        /**
         * 
         * @param URLConnectionString
         * @return 
         */
        public FormValidation doCheckURLConnectionString(@QueryParameter final String URLConnectionString)
        {
            return FieldValidator.validateURLConnectionString(URLConnectionString);
        }
        
        /**
         * 
         * @param project
         * @return 
         */
        //public FormValidation doCheckProject(@QueryParameter final String project)
        //{
        //  return FieldValidator.validateProject(project);
        //}
        
        /**
         * 
         * @param subProject
         * @return 
         */ 
        //public FormValidation doCheckSubProject(@QueryParameter final String subProject)
        //{
        //  return FieldValidator.validateSubProjectName(subProject);
        //}
        
        /**
         * 
         * @param scenario
         * @return 
         */
        //public FormValidation doCheckScenario(@QueryParameter final String scenario)
        //{
        //    return FieldValidator.validateScenario(scenario);
        //}

        /**
         *
         * @param URLConnectionString
         * @param username
         * @param password
         * @return
         */
        public FormValidation doTestNetstormConnection(@QueryParameter("URLConnectionString") final String URLConnectionString, @QueryParameter("username") final String username, @QueryParameter("password") final String password) {

            FormValidation validationResult;
            NetStormConnectionManager netstormConnectionManger = new NetStormConnectionManager(URLConnectionString, username, password);
            
            StringBuffer errMsg = new StringBuffer();
            
            if(netstormConnectionManger.testNSConnection(errMsg))
              validationResult = FormValidation.ok("Successfully Connected");
            else
             validationResult = FormValidation.warning("Cannot Connect to NetStorm due to :" + errMsg);
                       
            return validationResult;
        }
           
        public synchronized ListBoxModel doFillProjectItems(@QueryParameter("URLConnectionString") final String URLConnectionString, @QueryParameter("username") final String username, @QueryParameter("password") final String password)
        {
          ListBoxModel models = new ListBoxModel();
          StringBuffer errMsg = new StringBuffer();
          
          //IF creadentials are null or blank
          if(URLConnectionString == null || URLConnectionString.trim().equals("") || username == null || username.trim().equals("") || password == null || password.trim().equals(""))
          {
            models.add("---Select Project ---");   
            return models;
          }  
          
          //Making connection server to get project list
          NetStormConnectionManager objProject = new NetStormConnectionManager(URLConnectionString, username, password);
         
          ArrayList<String> projectList = objProject.getProjectList(errMsg);
          
          //IF project list is found null
          if(projectList == null || projectList.size() == 0)
          {
            models.add("---Select Project ---");   
            return models;
          }
          
          for(String project : projectList)
            models.add(project);
          
          return models;
        }
       
        public synchronized ListBoxModel doFillSubProjectItems(@QueryParameter("URLConnectionString") final String URLConnectionString, @QueryParameter("username") final String username, @QueryParameter("password") final String password, @QueryParameter("project") final String project )
        {
          ListBoxModel models = new ListBoxModel();
          
          if(URLConnectionString == null || URLConnectionString.trim().equals("") || username == null || username.trim().equals("") || password == null || password.trim().equals("") || project == null || project.trim().equals(""))
          {
            models.add("---Select SubProject ---");   
            return models;
          }  
            
          if(project.trim().equals("---Select Project ---"))
          {
            models.add("---Select SubProject ---");   
            return models;
          } 
          
          NetStormConnectionManager connection = new NetStormConnectionManager(URLConnectionString, username, password);
          StringBuffer errMsg = new StringBuffer();
          ArrayList<String> subProjectList = connection.getSubProjectList(errMsg, project);
          
          if(subProjectList == null || subProjectList.size() == 0)
          {
            models.add("---Select SubProject ---");   
            return models;
          }
          
          for(String subProject : subProjectList)
          {
            models.add(subProject);
          }
             
          return models;
        }
        
        public synchronized ListBoxModel doFillScenarioItems(@QueryParameter("URLConnectionString") final String URLConnectionString, @QueryParameter("username") final String username, @QueryParameter("password") final String password, @QueryParameter("project") final String project, @QueryParameter("subProject") final String subProject , @QueryParameter("testMode") final String testMode )
        {
          ListBoxModel models = new ListBoxModel();
          
          if(URLConnectionString == null || URLConnectionString.trim().equals("") || username == null || username.trim().equals("") || password == null || password.trim().equals("") || project == null || project.trim().equals("") || subProject == null || subProject.trim().equals(""))
          {
            models.add("---Select Profile ---");   
            return models;
          }  
          
          if(project.trim().equals("---Select Project ---") || subProject.trim().equals("---Select SubProject ---"))
          {
            models.add("---Select SubProject ---");   
            return models;
          } 
          
          NetStormConnectionManager connection = new NetStormConnectionManager(URLConnectionString, username, password);
          StringBuffer errMsg = new StringBuffer();
          ArrayList<String> scenariosList = connection.getScenarioList(errMsg, project, subProject, testMode);
          
          if(scenariosList == null || scenariosList.size() == 0)
          {
            models.add("---Select Scenarios ---");   
            return models;
          }
          
          for(String scenarios : scenariosList)
          {
            models.add(scenarios);
          }
          
          return models;
        }
        
        public ListBoxModel doFillTestModeItems()
        {
           ListBoxModel model = new ListBoxModel();
           model.add("Scenario", "N");
           model.add("Test Suite" , "T");
           
           return model;
        }
    }
}
