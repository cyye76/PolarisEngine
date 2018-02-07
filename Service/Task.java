package Service;

import java.util.ArrayList;
import java.util.HashMap;

import Utils.SyntaxAnalyzer;
import Configuration.Config;
import engine.expression.AssignmentExpression.AssignmentExpression;
import engine.expression.PortExpression.PortExpression;
import engine.expression.ServiceInvocation.ServiceInvocation;

public class Task {

	private String name;
	private boolean isPort;
	private boolean isSend;
	private boolean isService = false;
	private String effect;
	private String instanceID;
	private HashMap<String, String> variable_schema;
	
	private ArrayList<AssignmentExpression> assigns;
	private boolean isParsing =  false;//if true, that means the effect has been parsed into the assigns
	private PortExpression portexp;	
	private ServiceInvocation invocation;
	
	public Task(HashMap<String, String> vschema) {
		//setInstanceID("" + System.currentTimeMillis());
		setInstanceID("");
		variable_schema = vschema;
	}
	

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setPort(boolean isPort) {
		this.isPort = isPort;
	}

	public boolean isPort() {
		return isPort;
	}
	
	public void setIsService(boolean isService) {
		this.isService = isService;
	}
	
	public boolean isService() {
		return isService;
	}

	public void setEffect(String effect) {
		this.effect = effect;
		
		if(Config.getConfig().parsingExpressionInLoading) {			
			parsingEffects();
		}
	}
	
	public void parsingEffects() {
		if(isPort) 
			parsePortTaskEffects();
		else
		
		if(isService)
			parseServiceEffects();
		else
			parsingLocalTaskEffects();
	}

	private void parseServiceEffects() {
		SyntaxAnalyzer analyzer = new SyntaxAnalyzer(effect);
		analyzer.setVariables(variable_schema);
		String vLocPrefix = "Task:" + name + "_";
		analyzer.setVariableLocPrefix(vLocPrefix);
		
		try {
			setInvocation(analyzer.getServiceInvocation());		
		    isParsing = true;
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}


	private void parsePortTaskEffects() {
		SyntaxAnalyzer analyzer = new SyntaxAnalyzer(effect);
		analyzer.setVariables(variable_schema);
		String vLocPrefix = "Task:" + name + "_";
		analyzer.setVariableLocPrefix(vLocPrefix);
		
		try {
		    portexp = analyzer.getPortExpression();		
		    isParsing = true;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


	private void parsingLocalTaskEffects() {		
		SyntaxAnalyzer analyzer = new SyntaxAnalyzer(effect);
		analyzer.setVariables(variable_schema);
		
		String vLocPrefix = "Task:" + name + "_";
		analyzer.setVariableLocPrefix(vLocPrefix);
			
		try {
			ArrayList<AssignmentExpression> ae = analyzer.getAssignmentExpression();					
			
			isParsing = true;
			assigns = ae;
			
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	public String getEffect() {
		return effect;
	}


	private void setInstanceID(String instanceID) {
		this.instanceID = instanceID;
	}


	public String getInstanceID() {
		return instanceID;
	}


	public void setSend(boolean isSend) {
		this.isSend = isSend;
	}


	public boolean isSend() {
		return isSend;
	}


	public void setParsing(boolean isParsing) {
		this.isParsing = isParsing;
	}


	public boolean isParsing() {
		return isParsing;
	}


	public void setAssigns(ArrayList<AssignmentExpression> assigns) {
		this.assigns = assigns;
	}


	public ArrayList<AssignmentExpression> getAssigns() {
		return assigns;
	}
	
	public PortExpression getPortexp() {
		return portexp;
	}


	public ServiceInvocation getInvocation() {
		return invocation;
	}


	public void setInvocation(ServiceInvocation invocation) {
		this.invocation = invocation;
	}
	
		
}
