package engine.expression.Arithmetic;

public class InnerArithmeticTreeNode extends ArithmeticTreeNode {

	public InnerArithmeticTreeNode() {
		setLeafNode(false);
	}
	
	public InnerArithmeticTreeNode(ArithmeticTreeNode leftChild, 
			ArithmeticTreeNode rightChild, ArithmeticOperator op) {
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.op = op;
		setLeafNode(false);
	}
	
	public void setLeftChild(ArithmeticTreeNode leftChild) {
		this.leftChild = leftChild;
	}

	public ArithmeticTreeNode getLeftChild() {
		return leftChild;
	}

	public void setRightChild(ArithmeticTreeNode rightChild) {
		this.rightChild = rightChild;
	}

	public ArithmeticTreeNode getRightChild() {
		return rightChild;
	}

	public void setOp(ArithmeticOperator op) {
		this.op = op;
	}

	public ArithmeticOperator getOp() {
		return op;
	}
	
    public void print() {
    	leftChild.print();
    	
    	if(op == ArithmeticOperator.DI)
    		System.out.print(" / ");
    	
    	if(op == ArithmeticOperator.MI)
    		System.out.print(" - ");
    	
    	if(op == ArithmeticOperator.PL)
    		System.out.print(" + ");
    	
    	if(op == ArithmeticOperator.TI)
    		System.out.print(" * ");
    	
    	rightChild.print();
    }

	private ArithmeticTreeNode leftChild, rightChild;
	
	private ArithmeticOperator op;
}
