
package jenkins.pipeline;
        
import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class NetstormTestParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = 1L;
    private String keyword;
   
    @DataBoundConstructor
    public NetstormTestParameterDefinition(String name, String keyword, String description)
    {
        super(name, description);
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getRootUrl() {
        return Hudson.getInstance().getRootUrl();
    }

    /*
    @Override
    public NetstormStringParameterValue getDefaultParameterValue()
    {
        NetstormStringParameterValue v = new NetstormStringParameterValue(getName(), keyword, getDescription());
        return v;
    }*/

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return "NetStorm Test Parameter";
        }

        public FormValidation doCheckName(@QueryParameter final String name) {
            return FieldValidator.validateUsername(name);
        }
        /**
         * Called to validate the passed user entered value against the configured regular expression.
         */
        public FormValidation doValidate(@QueryParameter("keyword") String keyword, @QueryParameter("value") final String value) 
        {
          try
          {
            
            if(keyword != null && !keyword.equals(""))
            {
              if(keyword.equals("NS_NUM_USERS"))
              {
                try
                {
                  int i = Integer.parseInt(value);
                
                  if(i < 1)
                  {
                    return FormValidation.error("Users should be more then zero.");
                  }
                
                  return FormValidation.ok();
                }
                catch(Exception ex)
                {
                  return FormValidation.error("Please give users as number");
                }
              }
              
              if(keyword.equals("NS_SLA_CHANGE"))
              {
                 try
                 {
                   double d = Double.parseDouble(value);
                 }
                 catch(Exception e)
                 {
                    return FormValidation.error("SLA value should be numeric");
                 }
              }
            }
            
            
            return FormValidation.ok();
          }
          catch (PatternSyntaxException pse) 
          {
             return FormValidation.error("Invalid Keyword values expression [" + keyword + "]: " + pse.getDescription());
          }
        }
        
        public ListBoxModel doFillKeywordItems()
        {
           ListBoxModel model = new ListBoxModel();
           model.add("Total Users", "NS_NUM_USERS");
           model.add("Ramp up per second" , "NS_RAMP_UP_SEC");
           model.add("Ramp up per minute" , "NS_RAMP_UP_MIN");
           model.add("Ramp up per hour" , "NS_RAMP_UP_HR");
           model.add("Duration" , "NS_SESSION");
           model.add("Server/host" , "NS_SERVER_HOST");
           model.add("SLA" , "NS_SLA_CHANGE");
           model.add("Test Name" , "NS_TNAME");
           model.add("Automate Script Path" , "NS_AUTOSCRIPT");
           
           return model;
        }
       
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) 
    {
      final JSONObject paramModel = new JSONObject(false);
      String val = (String)jo.get("value");
      paramModel.put("value", keyword + "_" + val);
      paramModel.put("name", jo.get("name"));
      NetstormStringParameterValue value = req.bindJSON(NetstormStringParameterValue.class, paramModel);
      value.setDescription(getDescription());
      value.setKeyword(keyword);
        
      return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] value = req.getParameterValues(getName());
        if (value == null || value.length < 1) {
            return getDefaultParameterValue();
        } else {
            return new NetstormStringParameterValue(getName(), value[0] , keyword, getDescription());
        }
    }
}
