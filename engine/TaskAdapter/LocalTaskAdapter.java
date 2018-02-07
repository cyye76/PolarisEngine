package engine.TaskAdapter;

import java.util.ArrayList;

import engine.DataField;
import engine.DataType;
import engine.InvalidExecutionException;
import engine.InvalidExpressionException;
import engine.expression.AssignmentExpression.AssignmentExpression;
import Service.AbstractExpression;
import Service.AbstractService;
import Service.State;
import Service.Task;
import Service.Variable;

public class LocalTaskAdapter extends TaskAdapter {

	@Override
	public boolean execute(AbstractService service, Task task) throws InvalidExecutionException {		

		//String effect = task.getEffect();		
		//SyntaxAnalyzer analyzer = new SyntaxAnalyzer(effect);
		//analyzer.setVariables(service.getVariableSchema());
		
		try {
			State state = service.getState();
		    ArrayList<AssignmentExpression> ae = getAssignmentExpressions(task);		    
		    updateVariables(state, ae);
		    
		}catch(Exception e) {
			throw new InvalidExecutionException();
		}
		
		return true;
	}

	private ArrayList<AssignmentExpression> getAssignmentExpressions(Task task) {
        if(!task.isParsing())  
        	task.parsingEffects();
        	
        return task.getAssigns(); 				                
	}

	private void updateVariables(State state,
			ArrayList<AssignmentExpression> ae) 
	            throws InvalidExecutionException {		
		
		for(int i=0;i<ae.size();i++) {
			
			AssignmentExpression expression = ae.get(i);
			String vname = expression.getVariable();
			String vLoc = expression.getVariableLoc();
			Variable o_v = state.getVariablebyName(vname);
			if(o_v == null) throw new InvalidExecutionException();
			String type = o_v.getType();
			
			try {
				AbstractExpression cse = expression.getExpression();
				ArrayList<DataField> datafields = new ArrayList<DataField>();
				datafields.addAll(state.getVariables());
				Object result = cse.evaluateExpression(datafields);
				
				if((result instanceof Boolean) && !type.equals(DataType.BOOLEAN))
					throw new InvalidExpressionException();
				
				if((result instanceof String) && !type.equals(DataType.STRING))
					throw new InvalidExpressionException();
				
				if((result instanceof Integer) && !type.equals(DataType.INTEGER))
					throw new InvalidExpressionException();
			
				o_v.setValue(result, vLoc);
				
			} catch(InvalidExpressionException e) {
				e.printStackTrace();
				throw new InvalidExecutionException();
			}
										
		}		
	}		

}
