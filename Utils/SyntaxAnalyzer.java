package Utils;

import java.util.ArrayList;
import java.util.HashMap;

import engine.InvalidExpressionException;
import engine.expression.Arithmetic.ArithmeticExpression;
import engine.expression.Arithmetic.ArithmeticOperator;
import engine.expression.Arithmetic.LeafArithmeticTreeNode;
import engine.expression.AssignmentExpression.AssignmentExpression;
import engine.expression.AssignmentExpression.InvalidAssignmentExpressionException;
import engine.expression.BooleanExpression.BooleanExpression;
import engine.expression.BooleanExpression.BooleanOperator;
import engine.expression.BooleanExpression.LeafBooleanTreeNode;
import engine.expression.CSExpression.InvalidCSExpressionException;
import engine.expression.PortExpression.InvalidPortExpressionException;
import engine.expression.PortExpression.PortExpression;
import engine.expression.PortExpression.PortVariable;
import engine.expression.PredicateExpression.PredicateExpression;
import engine.expression.PredicateExpression.PredicateOperator;
import engine.expression.ServiceInvocation.InvalidServiceInvocationException;
import engine.expression.ServiceInvocation.Parameter;
import engine.expression.ServiceInvocation.ServiceInvocation;
import engine.expression.TokenExpression.TokenExpression;
import engine.expression.TokenExpression.TokenExpressionException;
import Service.AbstractExpression;

public class SyntaxAnalyzer {
	
    private WordParser parser;
    private String currentWord;
    private HashMap<String, String> variables;
	
    private int vLocIndex = 1;
    private String vLocPrefix;//used to assign each variable a unique location ID
	
	public SyntaxAnalyzer(String expression) {		 
		 parser = new WordParser(expression);
		 currentWord = parser.getNextWord();
	}

	//CSE := BE 
	public AbstractExpression getCSExpression() 
	         throws InvalidExpressionException {
		
		return getBooleanExpression();
	}

	//BE  := BOE
	public AbstractExpression getBooleanExpression() 
	         throws InvalidExpressionException {
		
		return getOrExpression();
	}

	//BOE := BAE | BAE '||' BOE
	private AbstractExpression getOrExpression() 
	       throws InvalidExpressionException {

		AbstractExpression e1 = getAndExpression();
		if(currentWord.equals("||")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getOrExpression();			
			
			return BooleanExpression.constructBooleanExpression(e1, BooleanOperator.OR, e2);
		} 			
			
		return e1;							
	}

	
	//old BAE := BNE | BNE '&&' BAE
	//new BAE := PEE | PEE '&&' BAE
	private AbstractExpression getAndExpression() 
	      throws InvalidExpressionException{
		
		//AbstractExpression e1 = getNOTExpression();
		AbstractExpression e1 = getEEPredicateExpression();
	    if(currentWord.equals("&&")) {
	    	currentWord = parser.getNextWord();
	    	AbstractExpression e2 = getAndExpression();
		    return BooleanExpression.constructBooleanExpression(e1, BooleanOperator.AND, e2);
	    } 
	        
	    return e1;
	}


	//PEE  := PE | PE '==' PEE | PE '!=' PEE
	private AbstractExpression getEEPredicateExpression() 
	        throws InvalidExpressionException {
		
		AbstractExpression e1 = getPredicateExpression();
		
		if(currentWord.equals("==")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getEEPredicateExpression();								
			LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
			leaf.setType(LeafBooleanTreeNode.PREDICATE_TYPE);								
			leaf.setPredicate(new PredicateExpression(e1, e2, PredicateOperator.EQ));
			return new BooleanExpression(leaf);
		} 
		
		if(currentWord.equals("!=")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getEEPredicateExpression();
			LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
			leaf.setType(LeafBooleanTreeNode.PREDICATE_TYPE);
			leaf.setPredicate(new PredicateExpression(e1, e2, PredicateOperator.NEQ));
			return new BooleanExpression(leaf);
		}  
		
		return e1;
	}
	
	
	
	//PE  := AE | AE '<' AE | AE '<=' AE | AE '>' AE | AE '>=' AE  
	private AbstractExpression getPredicateExpression() 
	         throws InvalidExpressionException {
		
		AbstractExpression e1 = getArithmeticExpression();							
		
		if(currentWord.equals(">=")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getArithmeticExpression();
			LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
			leaf.setType(LeafBooleanTreeNode.PREDICATE_TYPE);
			leaf.setPredicate(new PredicateExpression(e1, e2, PredicateOperator.GEQ));
			return new BooleanExpression(leaf);
		} 	
		
		if(currentWord.equals(">")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getArithmeticExpression();
			LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
			leaf.setType(LeafBooleanTreeNode.PREDICATE_TYPE);
			leaf.setPredicate(new PredicateExpression(e1, e2, PredicateOperator.GT));
			return new BooleanExpression(leaf);
		} 
		
		if(currentWord.equals("<=")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getArithmeticExpression();
			LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
			leaf.setType(LeafBooleanTreeNode.PREDICATE_TYPE);
			leaf.setPredicate(new PredicateExpression(e1, e2, PredicateOperator.LEQ));
			return new BooleanExpression(leaf);
		}  
	
		if(currentWord.equals("<")) {
			currentWord = parser.getNextWord();
			AbstractExpression e2 = getArithmeticExpression();
			LeafBooleanTreeNode leaf = new LeafBooleanTreeNode();
			leaf.setType(LeafBooleanTreeNode.PREDICATE_TYPE);
			leaf.setPredicate(new PredicateExpression(e1, e2, PredicateOperator.LT));
			return new BooleanExpression(leaf);
		} 				
				
	    return e1;									
	}
	

	//AE  := ATDE | ATDE '+' AE | ATDE '-' AE
	private AbstractExpression getArithmeticExpression() 
	           throws InvalidExpressionException {		
		
		AbstractExpression e1 = getTDArithmeticExpression();		
		
		do {
			if(currentWord.equals("+")) {
				currentWord = parser.getNextWord();
				AbstractExpression e2 = getTDArithmeticExpression();						
				e1 = ArithmeticExpression.constructArithmeticExpression(e1, e2, ArithmeticOperator.PL);			
			} 
			
			if(currentWord.equals("-")) {
				currentWord = parser.getNextWord();
				AbstractExpression e2 = getTDArithmeticExpression();			
				e1 = ArithmeticExpression.constructArithmeticExpression(e1, e2, ArithmeticOperator.MI);			
			}
		} while(currentWord.equals("+") || currentWord.equals("-"));
		 
		return e1;										
	}

	//ATDE:= NSSE | NSSE '*' ATDE | NSSE '/' ATDE
	private AbstractExpression getTDArithmeticExpression() 
	       throws InvalidExpressionException {
		
		AbstractExpression e1 = getNSSExpression();
		
		do {
			if(currentWord.equals("*")) {
				currentWord = parser.getNextWord();
				AbstractExpression e2 = getNSSExpression();
				e1 = ArithmeticExpression.constructArithmeticExpression(e1, e2, ArithmeticOperator.TI);
			}  
			
			if(currentWord.equals("/")) {
				currentWord = parser.getNextWord();
				AbstractExpression e2 = getNSSExpression();
				e1 = ArithmeticExpression.constructArithmeticExpression(e1, e2, ArithmeticOperator.DI);			
			}	 
		}while(currentWord.equals("*") || currentWord.equals("/"));
			
		return e1;		
	}
	
	//NSSE := SSE | '!' SSE 
	private AbstractExpression getNSSExpression() 
    	throws InvalidExpressionException {
		
		if(currentWord.equals("!")) {
			currentWord = parser.getNextWord();			
			AbstractExpression e1 = getSSExpression();
			return BooleanExpression.constructNotBooleanExpression(e1);
		}  
            					
		return getSSExpression();        				
	}

	//SSE := VSE | CTE | '(' CSE ')'
	private AbstractExpression getSSExpression() 
	            throws InvalidExpressionException {
		
		if(currentWord.equals("(")) {
			currentWord = parser.getNextWord();
			AbstractExpression e1 = getCSExpression();
			if(currentWord.equals(")")) {
				currentWord = parser.getNextWord();
				return e1;
			}
			
			throw new InvalidCSExpressionException();
		}
		
		return getTokenExpression();
		
	}

	//VSE := variable
	//CTE := constant
	private AbstractExpression getTokenExpression() 
	         throws InvalidExpressionException {

		if(currentWord!=null) {
			LeafArithmeticTreeNode node = new LeafArithmeticTreeNode();
			if(variables.containsKey(currentWord)) {
				node.setType(LeafArithmeticTreeNode.VARIAVLE_TYPE);
				node.setVariableName(currentWord);
				node.setVariableType(variables.get(currentWord));
				
				//set variable location ID
				node.setVariableLoc(getVariableLOC());
			
			} else {
				node.setType(LeafArithmeticTreeNode.CONSTANT_TYPE);				
				node.setConst_value(transfer(currentWord));
			}
			currentWord = parser.getNextWord();
			return new TokenExpression(node);
		} 
		
		throw new TokenExpressionException();
	}

	private Object transfer(String value) {

		try {
		    return Integer.parseInt(value);
		}catch (Exception e) {
			if ("true".equals(value)) return true;
			if("false".equals(value)) return false;
			return value;
		}		
	}

	public void setVariables(HashMap<String, String> variables) {
		this.variables = variables;
	}

	public HashMap<String, String> getVariables() {
		return this.variables;
	}
	
	
	public ArrayList<AssignmentExpression> getAssignmentExpression() 
	            throws InvalidExpressionException {
		
		ArrayList<AssignmentExpression> assigns = new ArrayList<AssignmentExpression>();
		boolean finished = false;
		while(!finished){
		    AssignmentExpression ae = getOneAssignment();
		    assigns.add(ae);
		    if(currentWord.equals("")) 		    	
		    	finished = true;
		}		
		
		return assigns;
	}

	//ASE := VSE ':=' CSE
	private AssignmentExpression getOneAssignment() throws InvalidExpressionException {

		if(currentWord == null) throw new InvalidAssignmentExpressionException();
		
		if(!variables.containsKey(currentWord)) throw new InvalidAssignmentExpressionException();
		
		String variable = currentWord;
		
		currentWord = parser.getNextWord();
        if(!currentWord.equals(":=")) throw new InvalidAssignmentExpressionException();        
        
        currentWord = parser.getNextWord();        
        AbstractExpression ae = getCSExpression();            
        
        if(!currentWord.equals(";")) throw new InvalidAssignmentExpressionException();        	
	    currentWord = parser.getNextWord();
        
	    AssignmentExpression assign = new AssignmentExpression();
        assign.setAssignment(variable, ae);
        
        //set Loc for the assigned variable
        assign.setVariableLoc(getVariableLOC());        
        
        return assign;       					
	}
		
	private String getVariableLOC() {
		String vLOC = vLocPrefix+ vLocIndex;
		vLocIndex++;
		return vLOC;
	}
	
	public PortExpression getPortExpression() throws InvalidPortExpressionException {
		String operator = currentWord;
		if(!"send".equals(operator) && !"receive".equals(operator)) 
			throw new InvalidPortExpressionException();
		
		currentWord = parser.getNextWord();
		ArrayList<PortVariable> portList = new ArrayList<PortVariable>();
		while((currentWord!=null) && (currentWord.length()>0)) {
			if(variables.get(currentWord) == null) 
				throw new InvalidPortExpressionException();
			
			PortVariable pv = new PortVariable(currentWord, getVariableLOC());
			portList.add(pv);
			
			currentWord = parser.getNextWord();
		}
		
		return new PortExpression(operator, portList);
	}

	public void setVariableLocPrefix(String vLocPrefix) {
		this.vLocPrefix = vLocPrefix;
	}

	//syntax: op1, op2,..., opm := servicename(ip1, ip2, ..., ipn) 
	public ServiceInvocation getServiceInvocation() throws InvalidServiceInvocationException {
		
		ArrayList<Parameter> outputParameters = extractParameter();
		if(!currentWord.equals(":=")) throw new InvalidServiceInvocationException();
		currentWord = parser.getNextWord();
		
		String invokedServiceName = currentWord;
		currentWord = parser.getNextWord();
		
		if(!currentWord.equals("(")) throw new InvalidServiceInvocationException();
		currentWord = parser.getNextWord();
		
		ArrayList<Parameter> inputParameters = extractParameter();
		
		if(!currentWord.equals(")")) throw new InvalidServiceInvocationException();
		currentWord = parser.getNextWord();
		
		
		return new ServiceInvocation(invokedServiceName, inputParameters, outputParameters);
	}

	private ArrayList<Parameter> extractParameter() throws InvalidServiceInvocationException {

		ArrayList<Parameter> plist = new ArrayList<Parameter>();
		while((currentWord!=null) && (currentWord.length()>0)) {
			if(variables.get(currentWord) == null) 
				throw new InvalidServiceInvocationException();
			
			Parameter pv = new Parameter(currentWord, getVariableLOC());
			plist.add(pv);
			
			currentWord = parser.getNextWord();
			if(!currentWord.equals(",")) break;
			currentWord = parser.getNextWord();
		}
		
		return plist;
	}
}	
