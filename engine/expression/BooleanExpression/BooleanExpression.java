package engine.expression.BooleanExpression;

import java.util.ArrayList;

import engine.DataField;
import engine.DataType;
import engine.InvalidExpressionException;
import engine.expression.TokenExpression.TokenExpression;
import Service.AbstractExpression;
import Service.Variable;

public class BooleanExpression extends AbstractExpression {

	private BooleanTreeNode booleanExpression;
	
	public BooleanExpression(BooleanTreeNode booleanExpression) {
		this.booleanExpression = booleanExpression;
	}
	
	public Object evaluateExpression(ArrayList<DataField> datafields) 
	               throws InvalidExpressionException {
		return evaluateExpression(datafields, this.booleanExpression);
	}

	private Object evaluateExpression(ArrayList<DataField> datafields,
			BooleanTreeNode expression) throws InvalidExpressionException {

		if(expression.isLeafNode) {
			LeafBooleanTreeNode leaf = (LeafBooleanTreeNode) expression;
			String vLoc = leaf.getVariableLoc();
			int type = leaf.getType();
			
			switch(type) {
			
			case LeafBooleanTreeNode.CONSTANT_TYPE: 
				return leaf.getValue();
			
			case LeafBooleanTreeNode.VARIABLE_TYPE:				
				return getVariableValue(leaf.getVariableName(), datafields, vLoc);
			
			case LeafBooleanTreeNode.PREDICATE_TYPE:				
				 return leaf.getPredicate().evaluateExpression(datafields);				
			
			default:
			    throw new InvalidBooleanExpressionException();  		
			}			
		
		} else {
			InnerBooleanTreeNode inner = (InnerBooleanTreeNode) expression;
			BooleanTreeNode leftChild = inner.getLeftChild();
			BooleanTreeNode rightChild = inner.getRightChild();
			BooleanOperator op = inner.getOp();
			
			Object value1 = evaluateExpression(datafields, leftChild);
			if(!(value1 instanceof Boolean)) 
				 throw new InvalidBooleanExpressionException();  
			
			if(op == BooleanOperator.NOT) {
				return !(Boolean)value1;				
			}
			
			if(op == BooleanOperator.OR) {
				if((Boolean)value1) return true;
				return evaluateExpression(datafields, rightChild);
			} 
			
			if(op == BooleanOperator.AND) {
				if(!((Boolean)value1)) return false;
				return evaluateExpression(datafields, rightChild);				 
			}
			
			throw new InvalidBooleanExpressionException();
		}			
	}

	private boolean getVariableValue(String variableName,
			ArrayList<DataField> datafields, String vLoc) 
	      throws InvalidBooleanExpressionException {	
		
		for(int i=0;i<datafields.size();i++) {
			DataField df = datafields.get(i);
			if(variableName.equals(df.getName())) {
				 if(!df.getType().equals(DataType.BOOLEAN)) 
					 throw new InvalidBooleanExpressionException();
				 
				 if(df instanceof Variable) {
					 return (Boolean)((Variable)df).getValue(vLoc);
				 } else
					 return (Boolean)df.getValue();
			}					
		}
		
		throw new InvalidBooleanExpressionException();
		
	}	
	
	static public AbstractExpression constructBooleanExpression(AbstractExpression e1, BooleanOperator op, AbstractExpression e2) 
	         throws InvalidBooleanExpressionException {

		if((e1 instanceof BooleanExpression) && (e2 instanceof BooleanExpression)) { 
			BooleanTreeNode left_node = ((BooleanExpression)e1).booleanExpression;
			BooleanTreeNode right_node = ((BooleanExpression)e2).booleanExpression;
			
			InnerBooleanTreeNode inner = new InnerBooleanTreeNode(left_node, right_node, op);
			return new BooleanExpression(inner);
		}
		
		if((e1 instanceof BooleanExpression) && (e2 instanceof TokenExpression)) { 
			BooleanTreeNode left_node = ((BooleanExpression)e1).booleanExpression;
			BooleanTreeNode right_node = ((TokenExpression)e2).constructBooleanExpression();
			
			InnerBooleanTreeNode inner = new InnerBooleanTreeNode(left_node, right_node, op);
			return new BooleanExpression(inner);
		}
		
		if((e1 instanceof TokenExpression) && (e2 instanceof BooleanExpression)) { 
			BooleanTreeNode left_node = ((TokenExpression)e1).constructBooleanExpression();
			BooleanTreeNode right_node = ((BooleanExpression)e2).booleanExpression;
			
			InnerBooleanTreeNode inner = new InnerBooleanTreeNode(left_node, right_node, op);
			return new BooleanExpression(inner);
		}
		
		if((e1 instanceof TokenExpression) && (e2 instanceof TokenExpression)) { 
			BooleanTreeNode left_node = ((TokenExpression)e1).constructBooleanExpression();
			BooleanTreeNode right_node = ((TokenExpression)e2).constructBooleanExpression();
			
			InnerBooleanTreeNode inner = new InnerBooleanTreeNode(left_node, right_node, op);
			return new BooleanExpression(inner);
		}
		
		throw new InvalidBooleanExpressionException();
	}
	
	public void print() {
		booleanExpression.printTree();
	}

	public static AbstractExpression constructNotBooleanExpression(
			AbstractExpression e1) 
	           throws InvalidBooleanExpressionException {
		
		if(e1 instanceof BooleanExpression) {
			BooleanTreeNode left_node = ((BooleanExpression)e1).booleanExpression;
			BooleanTreeNode right_node = null;
			
			InnerBooleanTreeNode inner = new InnerBooleanTreeNode(left_node, right_node, BooleanOperator.NOT);
            
			return new BooleanExpression(inner);
		}
		
		if(e1 instanceof TokenExpression) {
			BooleanTreeNode left_node = ((TokenExpression)e1).constructBooleanExpression();
			BooleanTreeNode right_node = null;
			
			InnerBooleanTreeNode inner = new InnerBooleanTreeNode(left_node, right_node, BooleanOperator.NOT);
            
			return new BooleanExpression(inner);
		}
		
		throw new InvalidBooleanExpressionException();
	}

	public BooleanTreeNode getExpression() {
		return booleanExpression;
	}
	
}
