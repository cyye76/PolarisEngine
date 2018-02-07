package Service;

import java.util.HashMap;

import Utils.SyntaxAnalyzer;

import Configuration.Config;


public class Guard {

	private String guard = "true";//default
	
	private AbstractExpression guardexp;
	private boolean isParsed = false; //if true, the condition has been parsed into guardexp;

	private HashMap<String, String> variable_schema;
	private String transitionName;
	
	public Guard(HashMap<String, String> vschema, String tName) {
		variable_schema = vschema;
		transitionName = tName;
	}

	public void setGuard(String guard) {
				
		if(!guard.isEmpty())
			this.guard = guard;
		
		if(Config.getConfig().parsingExpressionInLoading) {					
			parseCondition();
		}
				
	}

	public String getGuard() {
		return guard;
	}	

	public AbstractExpression getGuardexp() {
		return guardexp;
	}

	public void setParsed(boolean isParsed) {
		this.isParsed = isParsed;
	}

	public boolean isParsed() {
		return isParsed;
	}

	public void parseCondition() {		
		
		SyntaxAnalyzer analyzer = new SyntaxAnalyzer(guard);
		analyzer.setVariables(variable_schema);
		
		String vLocPrefix = "Transition:" + transitionName + "_";
		analyzer.setVariableLocPrefix(vLocPrefix);
		
		try{			
			guardexp = analyzer.getCSExpression();
			isParsed = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
