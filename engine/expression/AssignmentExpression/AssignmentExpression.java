package engine.expression.AssignmentExpression;

import Service.AbstractExpression;

public class AssignmentExpression extends AbstractExpression{
	
	private String variable;
	private String variableLoc;
	private AbstractExpression expression;
		
	public AssignmentExpression() {
		 
	}
	
	public void setAssignment(String variable, AbstractExpression expression) 
	                  throws InvalidAssignmentExpressionException {		
		    
		this.setExpression(expression);
		this.variable = variable;				
	}
		
	public void setVariable(String variable) {
		this.variable = variable;
	}

	public String getVariable() {
		return variable;
	}

	public void setExpression(AbstractExpression expression) {
		this.expression = expression;
	}

	public AbstractExpression getExpression() {
		return expression;
	}

	public void print() {
		
		System.out.print(variable);
		System.out.print(" := ");
		
		expression.print();		
	}

	public String getVariableLoc() {		
		return variableLoc;
	}
	
	public void setVariableLoc(String loc) {
		variableLoc = loc;
	}
}
