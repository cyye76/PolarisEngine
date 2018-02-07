package ServiceTesting.Monitoring;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import engine.DataField;
import engine.expression.BooleanExpression.InvalidBooleanExpressionException;

import scripts.OracleScript.ConstraintDefinition;
import scripts.OracleScript.ConstraintsType;
import scripts.OracleScript.OracleDefinition;
import scripts.OracleScript.VariableMappingDefinition;
import scripts.OracleScript.VariableMappingsType;

import Service.AbstractExpression;
import Service.AbstractService;
import Service.Variable;
import Utils.SyntaxAnalyzer;
import Utils.XMLProcessing;

public class Oracle {
	
	private OracleDefinition m_oracle = null;
	
	public Oracle(String filename) {
		try {
			FileInputStream input = new FileInputStream(filename);
			m_oracle = XMLProcessing.unmarshal(OracleDefinition.class, input);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
		
    public boolean checkOracle(ArrayList<AbstractService> services) {
    	if(m_oracle==null) return false;
    	
    	//construct the data fields and variable schema 
    	VariableMappingsType mappings = m_oracle.getVariableMappings();
    	assert(mappings!=null);
    	
    	ArrayList<DataField> datafields = new ArrayList<DataField>();
    	HashMap<String, String> variableSchema = new HashMap<String, String>();
    	
    	List<VariableMappingDefinition> vmap_list = mappings.getVariableMapping();
    	for(VariableMappingDefinition vmap: vmap_list) {
    		String serviceName = vmap.getServiceName();
    		String varName = vmap.getVarName();
    		Variable v = getServiceVariable(services, serviceName, varName);
    		assert(v!=null);
    		
    		String vType = v.getType();
    		Object value = v.getValue();
    		
    		String oracleVName = vmap.getOracleVariable();
    		
    		//update variableSchema
    		variableSchema.put(oracleVName, vType);
    		
    		//update DataField
    		DataField df = new DataField();
    		df.setName(oracleVName);
    		df.setType(vType);
    		df.setValue(value);
    		datafields.add(df);    		
    	}
    	
    	//verify all the constraints
    	ConstraintsType ct = m_oracle.getConstraints();
    	assert(ct!=null);
    	
    	List<ConstraintDefinition> constraint_list = ct.getConstraint();
    	for(ConstraintDefinition cd: constraint_list) {
    		String contents = cd.getContents();
    		boolean evaluate_result = evaluateConstraint(contents, datafields, variableSchema);
    		if(evaluate_result == false) 
    			return false;
    	}
    	
    	return true;
	}

	private boolean evaluateConstraint(String contents,
			ArrayList<DataField> datafields,
			HashMap<String, String> variableSchema) {
		
		SyntaxAnalyzer analyzer = new SyntaxAnalyzer(contents);
		analyzer.setVariables(variableSchema);
		
		try{
			AbstractExpression be = analyzer.getCSExpression();		    
		    Object result = be.evaluateExpression(datafields);
		    if(!(result instanceof Boolean)) 
		    	throw new InvalidBooleanExpressionException();
		    
		    return (Boolean)result;
		    
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private Variable getServiceVariable(ArrayList<AbstractService> services,
			String serviceName, String varName) {
		
		for(AbstractService sv: services) {
			String name = sv.getName();
			if(serviceName.equals(name))
				return sv.getState().getVariablebyName(varName);
		}
		
		return null;
	}
		
	
}
