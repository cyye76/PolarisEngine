package engine.expression.Arithmetic;

public class LeafArithmeticTreeNode extends ArithmeticTreeNode {

	public LeafArithmeticTreeNode() {
		setLeafNode(true);
	}
	
	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setConst_value(Object const_value) {
		this.const_value = const_value;
	}

	public Object getConst_value() {
		return const_value;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}
	
	public void print() {
	    switch(type) {
	    case CONSTANT_TYPE: 
	    	System.out.print(" " + const_value);
	    	break;
	    case VARIAVLE_TYPE:
	    	System.out.print(" " + variableName);
	    	break;
	    default:
	    }
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}

	public String getVariableType() {
		return variableType;
	}

	public void setVariableLoc(String variableLoc) {
		this.variableLoc = variableLoc;
	}

	public String getVariableLoc() {
		return variableLoc;
	}

	//type =0: constant
	//type = 1: variable
	public final static int CONSTANT_TYPE =0;
	public final static int VARIAVLE_TYPE =1;
	private int type;
	
	private Object const_value;
	
	private String variableName;
	private String variableType;
	private String variableLoc;
}
