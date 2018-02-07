package engine.expression.PredicateExpression;

import java.util.ArrayList;

import engine.DataField;
import engine.InvalidExpressionException;
import Service.AbstractExpression;

public class PredicateExpression extends AbstractExpression{
	
	
	public PredicateExpression(AbstractExpression leftExpression, 
			                   AbstractExpression rightExpression,
			                   PredicateOperator op) {
		this.leftExpression = leftExpression;
		this.rightExpression = rightExpression;
		this.op = op;
	}
		
	public Object evaluateExpression(ArrayList<DataField> datafields) 
	       throws InvalidPredicateExpressionException{

		try {
		     Object value1 = leftExpression.evaluateExpression(datafields);
		     Object value2 = rightExpression.evaluateExpression(datafields);
		     return evaluatePredicateExpression(value1, op, value2);
		} catch(InvalidExpressionException e) {
			e.printStackTrace();
			throw new InvalidPredicateExpressionException();
		}		
	}
	
	private boolean evaluatePredicateExpression(Object value1,
			PredicateOperator op, Object value2)
	        throws InvalidPredicateExpressionException{

		if((value1 instanceof Integer) && (value2 instanceof Integer))
			return compareInteger(value1, op, value2);
		else
		
		if((value1 instanceof String) && (value2 instanceof String))
			return compareString(value1, op, value2);		
		else
			
		if((value1 instanceof Boolean) && (value2 instanceof Boolean))	
		    return compareBoolean(value1, op, value2);
		else
			throw new InvalidPredicateExpressionException();
		
	}

	private boolean compareBoolean(Object value1, PredicateOperator op2,
			Object value2) throws InvalidPredicateExpressionException {

		if(op.equals(PredicateOperator.EQ))
			return value1.equals(value2);
		else
		
		if(op.equals(PredicateOperator.NEQ))
			return !value1.equals(value2);
		else
			
		throw new InvalidPredicateExpressionException();
	}

	private boolean compareString(Object value1, PredicateOperator op2,
			Object value2)throws InvalidPredicateExpressionException {

		if(op.equals(PredicateOperator.EQ))
			return value1.equals(value2);
		else
		
		if(op.equals(PredicateOperator.NEQ))
			return !value1.equals(value2);
		else
			
		throw new InvalidPredicateExpressionException();
	}

	private boolean compareInteger(Object value1, PredicateOperator op2,
			Object value2) throws InvalidPredicateExpressionException {
		
		int v1 = (Integer)value1;
		int v2 = (Integer)value2;
		
		if(op.equals(PredicateOperator.EQ))
			return v1 == v2;
		
		if(op.equals(PredicateOperator.GEQ))
			return v1 >= v2;
			
		if(op.equals(PredicateOperator.GT))
			return v1 > v2;
			
		if(op.equals(PredicateOperator.LEQ))
			return v1 <= v2;
		
		if(op.equals(PredicateOperator.LT))
			return v1 < v2;
		
		if(op.equals(PredicateOperator.NEQ))
			return v1 != v2;
		
		throw new InvalidPredicateExpressionException();
		
	}

	private AbstractExpression leftExpression, rightExpression;	
	private PredicateOperator op;
	
	public void print() {
		leftExpression.print();
		
		if(op.equals(PredicateOperator.EQ))
			System.out.print(" == ");
		
		if(op.equals(PredicateOperator.GEQ))
			System.out.print(" >= ");
			
		if(op.equals(PredicateOperator.GT))
			System.out.print(" > ");
			
		if(op.equals(PredicateOperator.LEQ))
			System.out.print(" <= ");
		
		if(op.equals(PredicateOperator.LT))
			System.out.print(" < ");
		
		if(op.equals(PredicateOperator.NEQ))
			System.out.print(" != ");
		
		rightExpression.print();
	}	

	public AbstractExpression getLeftExpression() {
		return leftExpression;
	}
	
	public AbstractExpression getRightExpression() {
		return rightExpression;
	}
}
