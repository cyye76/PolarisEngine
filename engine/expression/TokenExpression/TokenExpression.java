package engine.expression.TokenExpression;

import java.util.ArrayList;

import engine.DataField;
import engine.DataType;
import engine.InvalidExpressionException;
import engine.expression.Arithmetic.LeafArithmeticTreeNode;
import engine.expression.BooleanExpression.BooleanTreeNode;
import engine.expression.BooleanExpression.InvalidBooleanExpressionException;
import engine.expression.BooleanExpression.LeafBooleanTreeNode;
import Service.AbstractExpression;
import Service.Variable;

public class TokenExpression extends AbstractExpression {
	
	private LeafArithmeticTreeNode node;
	
	public TokenExpression(LeafArithmeticTreeNode node) {
		this.node = node;
	}

	@Override
	public void print() {		
        node.print();
	}

	@Override
	public Object evaluateExpression(ArrayList<DataField> datafields)
			throws InvalidExpressionException {
        int nodeType = node.getType();		
		
		if(nodeType == LeafArithmeticTreeNode.VARIAVLE_TYPE) { //variable
			String name = node.getVariableName();
			String type = node.getVariableType();
			String vLoc = node.getVariableLoc();
			for(DataField df:datafields) {
				if(name.equals(df.getName()) && type.equals(df.getType())) {
					if(df instanceof Variable) {
						return ((Variable)df).getValue(vLoc);
					} else
					    return df.getValue();
				}
			}
						
		}
		
		if(nodeType == LeafArithmeticTreeNode.CONSTANT_TYPE) {//constant
			return node.getConst_value();
		}
		
		return null;
	}

	public void setNode(LeafArithmeticTreeNode node) {
		this.node = node;
	}

	public LeafArithmeticTreeNode getNode() {
		return node;
	}

	public BooleanTreeNode constructBooleanExpression() 
	        throws InvalidBooleanExpressionException{
         
		int nodeType = node.getType();		
		
		if(nodeType == LeafArithmeticTreeNode.VARIAVLE_TYPE) { //variable
            String name = node.getVariableName();
            String type = node.getVariableType();
            String vLoc = node.getVariableLoc();
            if(type.equals(DataType.BOOLEAN)) {
                LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
                leaf.setType(LeafBooleanTreeNode.VARIABLE_TYPE);
                leaf.setVariableName(name);
                leaf.setVariableLoc(vLoc);
                return leaf;
            } 
                
            throw new InvalidBooleanExpressionException();
        } 
            
		if(nodeType == LeafArithmeticTreeNode.CONSTANT_TYPE) {//constant
            Object value = node.getConst_value();
            if("false".equals(value)) {
                LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
                leaf.setType(LeafBooleanTreeNode.CONSTANT_TYPE);
                leaf.setValue(false);
                return leaf;                
            }
            
            if("true".equals(value)) {
                LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
                leaf.setType(LeafBooleanTreeNode.CONSTANT_TYPE);
                leaf.setValue(true);
                return leaf;                
            }
            
            throw new InvalidBooleanExpressionException();
        }
		
		throw new InvalidBooleanExpressionException();
	}
	
}
