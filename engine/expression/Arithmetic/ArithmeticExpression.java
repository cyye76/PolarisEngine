package engine.expression.Arithmetic;

import java.util.ArrayList;

import engine.DataField;
import engine.InvalidExpressionException;
import engine.expression.TokenExpression.TokenExpression;

import Service.AbstractExpression;
import Service.Variable;

public class ArithmeticExpression extends AbstractExpression{

	private ArithmeticTreeNode expression;
	
	public ArithmeticExpression(ArithmeticTreeNode expression) {
		this.expression = expression;
	}
	
	public ArithmeticTreeNode getExpression() {
		return this.expression;
	}
	
	private Object evaluateExpression(ArithmeticTreeNode expre, ArrayList<DataField> datafields) 
	       throws InvalidExpressionException {
		
		 if(expre.isLeafNode) {
			  LeafArithmeticTreeNode node = (LeafArithmeticTreeNode) expre;
			  if(node.getType()== LeafArithmeticTreeNode.CONSTANT_TYPE){
				  return node.getConst_value();				  
			  } else {				  
				  return getVariableValue(datafields, node.getVariableName(), node.getVariableLoc());
			  }
		 } else {
			 InnerArithmeticTreeNode node = (InnerArithmeticTreeNode) expre;
			 ArithmeticTreeNode leftChild = node.getLeftChild();
			 ArithmeticTreeNode rightChild = node.getRightChild();
			 
			 ArithmeticOperator op = node.getOp();
			 
			 Object value1 = evaluateExpression(leftChild, datafields);
			 Object value2 = evaluateExpression(rightChild, datafields);
			 return manuplicateValue(value1,value2, op);
		 }
		 		 
	}

	private Object manuplicateValue(Object value1, Object value2,
			ArithmeticOperator op) 
	        throws InvalidArithmeticExpressionException {

		int v1 = (Integer)value1;
		int v2 = (Integer)value2;
		
		if(op == ArithmeticOperator.DI)
			return new Integer(v1/v2);
		
		if(op == ArithmeticOperator.MI)
			return new Integer(v1 - v2);
		
		if(op == ArithmeticOperator.PL)
			return new Integer(v1 + v2);
		
		if(op == ArithmeticOperator.TI)
			return new Integer(v1 * v2);
				
		throw new InvalidArithmeticExpressionException();
	}

	

	private Object getVariableValue(ArrayList<DataField> datafields,
			String variableName, String vLoc) 
	        throws InvalidArithmeticExpressionException {		
		
		if (datafields == null) throw new InvalidArithmeticExpressionException();
		for(int i=0; i<datafields.size();i++) {
			DataField df = datafields.get(i);
			if(df.getName().equals(variableName)) {
				if(df instanceof Variable) {
					return ((Variable)df).getValue(vLoc);//expose DataReadEvent
				} else
				    return df.getValue();
			}
		}
		
		throw new InvalidArithmeticExpressionException();
	}

	public Object evaluateExpression(ArrayList<DataField> datafields) 
	       throws InvalidExpressionException {

		return evaluateExpression(this.expression, datafields);
	}
	
	public boolean isVariable() {
	    if(expression.isLeafNode) {
	    	LeafArithmeticTreeNode node = (LeafArithmeticTreeNode)expression;
	    	return LeafArithmeticTreeNode.VARIAVLE_TYPE == node.getType();
	    } else
	    	return false;
	}
	
	public boolean isConstant() {
	    if(expression.isLeafNode) {
	    	LeafArithmeticTreeNode node = (LeafArithmeticTreeNode)expression;
	    	return LeafArithmeticTreeNode.CONSTANT_TYPE == node.getType();
	    } else
	    	return false;
	}

	public String getVariableName() throws InvalidArithmeticExpressionException {

		if(isVariable()) {
			LeafArithmeticTreeNode node = (LeafArithmeticTreeNode)expression;
			return node.getVariableName();
		}
		throw new InvalidArithmeticExpressionException();
	}

	public void print() {
		expression.print();
	}

	public Object getConstantValue() throws InvalidArithmeticExpressionException{

		if(isConstant()) {
			LeafArithmeticTreeNode node = (LeafArithmeticTreeNode)expression;
			return node.getConst_value();
		}
		throw new InvalidArithmeticExpressionException();
	}

	public static AbstractExpression constructArithmeticExpression(
			AbstractExpression e1, AbstractExpression e2, ArithmeticOperator op) 
	   throws InvalidArithmeticExpressionException {

		if((e1 instanceof ArithmeticExpression) && (e2 instanceof ArithmeticExpression)) {
			ArithmeticTreeNode left_node = ((ArithmeticExpression)e1).expression;
			ArithmeticTreeNode right_node = ((ArithmeticExpression)e2).expression;
			InnerArithmeticTreeNode inner = new InnerArithmeticTreeNode(left_node, right_node, op);
			return new ArithmeticExpression(inner);						
		}
		
		if((e1 instanceof ArithmeticExpression) && (e2 instanceof TokenExpression)) {
			ArithmeticTreeNode left_node = ((ArithmeticExpression)e1).expression;
			ArithmeticTreeNode right_node = ((TokenExpression)e2).getNode();
			InnerArithmeticTreeNode inner = new InnerArithmeticTreeNode(left_node, right_node, op);
			return new ArithmeticExpression(inner);
		}
		
		if((e1 instanceof TokenExpression) && (e2 instanceof ArithmeticExpression)) {
			ArithmeticTreeNode left_node = ((TokenExpression)e1).getNode();
			ArithmeticTreeNode right_node = ((ArithmeticExpression)e2).expression;
			InnerArithmeticTreeNode inner = new InnerArithmeticTreeNode(left_node, right_node, op);
			return new ArithmeticExpression(inner);
		}
		
		if((e1 instanceof TokenExpression) && (e2 instanceof TokenExpression)) {
			ArithmeticTreeNode left_node = ((TokenExpression)e1).getNode();
			ArithmeticTreeNode right_node = ((TokenExpression)e2).getNode();
			InnerArithmeticTreeNode inner = new InnerArithmeticTreeNode(left_node, right_node, op);
			return new ArithmeticExpression(inner);
		}
		
		throw new InvalidArithmeticExpressionException();
	}
	
}
