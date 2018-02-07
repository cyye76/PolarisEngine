package engine.expression.BooleanExpression;

public class InnerBooleanTreeNode extends BooleanTreeNode {

	private BooleanTreeNode leftChild;
	private BooleanTreeNode rightChild;
	private BooleanOperator op;

	public InnerBooleanTreeNode(BooleanTreeNode leftChild, BooleanTreeNode rightChild,
			                    BooleanOperator op) {
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.op = op;
		setLeafNode(false);
	}
		
	public BooleanOperator getOp() {
		return this.op;
	}

	public BooleanTreeNode getRightChild() {
		return this.rightChild;
	}

	public BooleanTreeNode getLeftChild() {
		return this.leftChild;
	}
	
	public void printTree() {
		
		if(op == BooleanOperator.NOT) {
			System.out.print("!");
			leftChild.printTree();
		} else {

			leftChild.printTree();
	    
			if(op == BooleanOperator.AND)
				System.out.print(" && ");
	    
			if(op == BooleanOperator.OR)
				System.out.print(" || ");
	    
			rightChild.printTree();
		}
	}
}
