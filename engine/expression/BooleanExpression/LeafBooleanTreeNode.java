package engine.expression.BooleanExpression;

import engine.expression.PredicateExpression.PredicateExpression;

public class LeafBooleanTreeNode extends BooleanTreeNode {

	public LeafBooleanTreeNode() {
		setLeafNode(true);
	}
	
	public void setValue(boolean value) {
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	public void setPredicate(PredicateExpression predicate) {
		this.predicate = predicate;
	}

	public PredicateExpression getPredicate() {
		return predicate;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}
	
	public void printTree() {

		switch(type) {
		
		case CONSTANT_TYPE: 
			System.out.print(" " + value);
			break;
		
		case VARIABLE_TYPE:
			System.out.print(" " + variableName);
			break;
		
		case PREDICATE_TYPE:
			predicate.print();
			break;
		
		default:
				
		}
	}

	private boolean value;
	private PredicateExpression predicate;
	private String variableName;
	private int type;
	private String vLoc;
	
	public final static int CONSTANT_TYPE = 0;
	public final static int PREDICATE_TYPE =1;
	public final static int VARIABLE_TYPE = 2;
	
	
	public void setVariableLoc(String vLoc) {
		this.vLoc = vLoc;		
	}
	
	public String getVariableLoc() {
		return this.vLoc;
	}
}
