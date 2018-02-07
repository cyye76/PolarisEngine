package ServiceTesting.ConstraintSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import engine.DataField;
import engine.expression.AssignmentExpression.AssignmentExpression;
import engine.expression.BooleanExpression.BooleanExpression;
import engine.expression.PortExpression.PortExpression;
import engine.expression.PortExpression.PortVariable;

import Service.AbstractExpression;
import Utils.SyntaxAnalyzer;

public class ConstraintSolver {

	/*
	 * ServiceName, VariableName, CSLVariable
	 */
	private HashMap<String, HashMap<String, CSLVariable>> variables = 
		                    new HashMap<String, HashMap<String, CSLVariable>>();
	
	private HashMap<String, HashMap<String, String>> variable_schema = new HashMap<String, HashMap<String, String>>();
	
	private ArrayList<MyConstraint> constraint_chain = new ArrayList<MyConstraint>();
	
	private ArrayList<CSLVariable> currentVariables = new ArrayList<CSLVariable>();	
	
	private HashMap<String, ArrayList<CSLVariable>> queue = new HashMap<String, ArrayList<CSLVariable>>();
	
	public void addConstraint(MyConstraint ct) {
		//constraint_chain.add(ct);
		constraint_chain.add(ct.backup());
	}	
	
	public void setVariableSchema(String serviceName, HashMap<String, String> schema) {
		variable_schema.put(serviceName, schema);
		queue.put(serviceName, new ArrayList<CSLVariable>());
	}
	
	public void initializeVariables(String serviceName, HashMap<String, String> schema) {
		HashMap<String, CSLVariable> vm = new HashMap<String, CSLVariable>();
		
		Set<String> vns = schema.keySet();
		for(String vn: vns) {
			String type = schema.get(vns);
			CSLVariable cv = CSLVariable.initialize(vn, type, serviceName);
			vm.put(vn, cv);
		}
		
		variables.put(serviceName, vm);
	}
	
	private int pointer = 0;//pointing to current constraint in the constraint chain
	
	private boolean isfirst = true;
	private boolean nobackward = false;
	
	public boolean generateNextResultWithoutBackward() {
		nobackward = true;		
		return generateNextResult();
	}
	
	public boolean generateNextResult() {
		boolean forward = isfirst;
		if(isfirst) {
			isfirst = false;
			ConstraintTransformer.transformConstraints(constraint_chain, variable_schema);
		}
        
		return generateNextResult(forward);		

	}
	
	public boolean generateNextResult(boolean forward) {
		
		//boolean forward = true;
		
		while(pointer<constraint_chain.size() && pointer >=0) {
			MyConstraint mc = constraint_chain.get(pointer);
              				
			if(forward)  
				forward = moveForward(mc);		
			
			//quickly terminate for hybrid verification of a solution
			//if(nobackward && !forward) return false;
			
			if(!forward)
				forward = backtrack(mc);
			
			if(forward) 
				pointer++;
			else 
				pointer--;		
													
		}
		
		if (pointer>=constraint_chain.size()) {
			pointer--;
			return true;
		} else
			return false;
	}

	private boolean moveForward(MyConstraint mc) {
		if(mc.isSend()) return handleSendExpression(mc);
		
		if(mc.isReceive()) return handleReceiveExpression(mc);
		
		if(mc.isAssignment()) return handleAssignment(mc);
		
		if(mc.isBooleanExpression()) return handleBooleanExpression(mc);
		
		return false;
	}

	private boolean backtrack(MyConstraint mc) {		
        
		if(mc.isSend()) return backtrackSendExpression(mc);
		
		if(mc.isReceive()) return backtrackReceiveExpression(mc);
		
		if(mc.isAssignment()) return backtrackAssignment(mc);
		
		if(mc.isBooleanExpression()) return backtrackBooleanExpression(mc);
		
		return false;
	}

	private boolean handleBooleanExpression(MyConstraint mc) {
		
		if(mc.m_expression==null) {
			SyntaxAnalyzer analyzer = new SyntaxAnalyzer(mc.condition);
			analyzer.setVariables(variable_schema.get(mc.serviceName));
			try {
			     mc.m_expression = analyzer.getBooleanExpression();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		AbstractExpression be = (AbstractExpression) mc.m_expression;
		
		initializeDataField(mc, false);
		ArrayList<CSLVariable> search_variable = mc.searchVariables;
		ArrayList<DataField> datafields = mc.datafields;
		
		try {		    
			boolean er = (Boolean)be.evaluateExpression(datafields);
			while(!er) {
				boolean hasNextDataField = nextDataField(search_variable);
				if(hasNextDataField) 
					er = (Boolean)be.evaluateExpression(datafields);
				else
					return false;
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		//evaluation to be true
		//add search_variables to bind variables
		currentVariables.addAll(search_variable);		
		
		return true;
	}
	
    private void initializeDataField(MyConstraint mc, boolean isAssignment) {
    	//ArrayList<CSLVariable> search_variable = mc.searchVariables;
    	//ArrayList<DataField> datafields = mc.datafields;
    	
    	//if(datafields == null || search_variable == null) {    	
    	    ArrayList<CSLVariable> search_variable = new ArrayList<CSLVariable>();
    	    ArrayList<DataField> datafields = new ArrayList<DataField>();
		    mc.datafields = datafields;
		    mc.searchVariables = search_variable;

    		HashMap<String, String> schema = variable_schema.get(mc.serviceName);
    		ArrayList<String> vns = mc.getExpressionVariables(schema, isAssignment);

    		for(String vn: vns) {    			
    			
    			CSLVariable csl = getVariableByName(currentVariables, mc.serviceName, vn);    			
    			if(csl==null) {
    				String type = schema.get(vn);
    				csl = CSLVariable.initialize(vn, type, mc.serviceName);		    	    				
    				search_variable.add(csl);
    			}		        			
    			
    			datafields.add(csl);
    		}
    		
//    	} 
//        else {
//    		//initialize all the search variables
//    		for(CSLVariable csl: search_variable)
//    			csl.init();
//    	}
	}

	private boolean backtrackBooleanExpression(MyConstraint mc) {
			
		ArrayList<CSLVariable> search_variable = mc.searchVariables;
		boolean hasNextDataField = nextDataField(search_variable);
		if(!hasNextDataField) {
			currentVariables.removeAll(search_variable);//remove search variables from the bind variable list
			return false;
		}

		BooleanExpression be = (BooleanExpression) mc.m_expression;		
		ArrayList<DataField> datafields = mc.datafields;
		
		try {		    
			boolean er = (Boolean)be.evaluateExpression(datafields);
			while(!er) {
				hasNextDataField = nextDataField(search_variable);
				if(hasNextDataField) 
					er = (Boolean)be.evaluateExpression(datafields);
				else {
					currentVariables.removeAll(search_variable);//remove search variables from the bind variable list
					return false;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}				
		
		return true;
	}

	private boolean nextDataField(ArrayList<CSLVariable> search_variable) {
		if(search_variable==null || search_variable.isEmpty()) return false;
		int cp = search_variable.size()-1;//search from the last one
		
		while(cp>=0) {
			CSLVariable csl = search_variable.get(cp);
			if(csl.hasNext()) {
				csl.next();
				for(int i=cp+1;i<search_variable.size();i++) {
					CSLVariable cn = search_variable.get(i);
					cn.init();
				}
				return true;
			} else 
				cp--;//backtrack
		}
		
		return false;
	}

	private CSLVariable getVariableByName(ArrayList<CSLVariable> vars, String sn, String vn) {

		if(vars==null) return null;
		
		for(CSLVariable csl: vars) 
			if(vn.equals(csl.getName()) && sn.equals(csl.getServiceName())) return csl;
		
		return null;
	}
	
	private CSLVariable getVariableByName(String vn, ArrayList<CSLVariable> vars) {

		if(vars==null) return null;
		
		for(CSLVariable csl: vars) 
			if(vn.equals(csl.getName())) return csl;
		
		return null;
	}

	private boolean handleAssignment(MyConstraint mc) {
		
		if(mc.m_expression==null) {
			SyntaxAnalyzer analyzer = new SyntaxAnalyzer(mc.condition);
			analyzer.setVariables(variable_schema.get(mc.serviceName));
			try {
				 ArrayList<AssignmentExpression> ae_list = analyzer.getAssignmentExpression();
				 if(ae_list == null) return false;
			     mc.m_expression = ae_list.get(0);
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		AssignmentExpression ae = (AssignmentExpression) mc.m_expression;		
		String avn = ae.getVariable();
		
		initializeDataField(mc, true);
		ArrayList<CSLVariable> search_variable = mc.searchVariables;
		ArrayList<DataField> datafields = mc.datafields;
		//add search_variables to bind variables
		currentVariables.addAll(search_variable);
	 	
		CSLVariable assignVariable = getVariableByName(currentVariables, mc.serviceName, avn);
		if(assignVariable==null) {
			assignVariable = new CSLVariable();
			assignVariable.setName(avn);
			HashMap<String, String> schema = variable_schema.get(mc.serviceName);
			assignVariable.setType(schema.get(avn));
			assignVariable.setServiceName(mc.serviceName);
			
			//add assignVariable to bind variable
			currentVariables.add(assignVariable);
			
		} else { //backup the original value
			mc.rstV = assignVariable.backup();
		}
		
		try {		
			AbstractExpression cse = ae.getExpression();
			Object value = cse.evaluateExpression(datafields);
			assignVariable.setValue(value);
			
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
								
		return true;		
	}
	
    private boolean backtrackAssignment(MyConstraint mc) {			
		
		AssignmentExpression ae = (AssignmentExpression) mc.m_expression;		
		String avn = ae.getVariable();
				
		ArrayList<CSLVariable> search_variable = mc.searchVariables;
		CSLVariable assignVariable = getVariableByName(currentVariables, mc.serviceName, avn);
		
		if(mc.rstV!=null) {
			assignVariable.restore(mc.rstV);
			mc.rstV = null;
		} else
			currentVariables.remove(assignVariable);
		
		boolean hasNextDataField = nextDataField(search_variable);
		if(!hasNextDataField) {																			
			currentVariables.removeAll(search_variable);			
			return false;		
		} else {			
			
			ArrayList<DataField> datafields = mc.datafields;
			if(currentVariables.contains(assignVariable)) 
				mc.rstV = assignVariable.backup();
			
			try {		    
				AbstractExpression cse = ae.getExpression();
				Object value = cse.evaluateExpression(datafields);
				assignVariable.setValue(value);
				
				if(!currentVariables.contains(assignVariable))
				    currentVariables.add(assignVariable);
				
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}									
			return true;
		}								
	}

	private boolean handleReceiveExpression(MyConstraint mc) {
		
		if(mc.m_expression==null) {
			SyntaxAnalyzer analyzer = new SyntaxAnalyzer(mc.condition);
			analyzer.setVariables(variable_schema.get(mc.serviceName));
			try {				  
			     mc.m_expression = analyzer.getPortExpression();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		PortExpression pe = (PortExpression) mc.m_expression;
		ArrayList<PortVariable> pvars = pe.getPortVariables();
				
		return initializeReceiveData(mc, pvars);		
	}
	
	private boolean backtrackReceiveExpression(MyConstraint mc) {				
		
		//restore queue
		ArrayList<CSLVariable> receiveQueue = queue.get(mc.serviceName);
		receiveQueue.addAll(mc.recvVariables);
		
		//restore bindVariable
		currentVariables.removeAll(mc.recvFree);
		
		//restore original values
		for(CSLVariable csl: mc.recvRestore) {
			String vn = csl.getName();
			CSLVariable ov = getVariableByName(currentVariables, mc.serviceName, vn);
			if(ov!=null) ov.restore(csl);
		}
		
		return false;		
	}

	private boolean initializeReceiveData(MyConstraint mc,
			ArrayList<PortVariable> pvars) {
		
		ArrayList<CSLVariable> receiveQueue = queue.get(mc.serviceName);
		ArrayList<CSLVariable> recvVariables = new ArrayList<CSLVariable>();
		for(PortVariable pv: pvars) {
			String vn = pv.getName();
			CSLVariable csl = getVariableByName(vn, receiveQueue);
			if(csl==null) return false;
			recvVariables.add(csl);
		}
		
		receiveQueue.removeAll(recvVariables);
		ArrayList<CSLVariable> recvRestore = new ArrayList<CSLVariable>();
		ArrayList<CSLVariable> recvFree = new ArrayList<CSLVariable>();
		
		for(CSLVariable rv: recvVariables) {
			String vn = rv.getName();
			CSLVariable csl = getVariableByName(currentVariables, mc.serviceName, vn);
			if(csl==null) {
				csl = rv.backup();
				csl.setServiceName(mc.serviceName);
				recvFree.add(csl);
				currentVariables.add(csl);
			} else {
				CSLVariable rst = csl.backup();
				recvRestore.add(rst);
			}
		}
		
		mc.recvFree  = recvFree;
		mc.recvRestore = recvRestore;
		mc.recvVariables = recvVariables;
		
		return true;
	}

	private boolean handleSendExpression(MyConstraint mc) {
		
		if(mc.m_expression==null) {
			SyntaxAnalyzer analyzer = new SyntaxAnalyzer(mc.condition);
			analyzer.setVariables(variable_schema.get(mc.serviceName));
			try {				  
			     mc.m_expression = analyzer.getPortExpression();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		PortExpression pe = (PortExpression) mc.m_expression;
		ArrayList<PortVariable> pvars = pe.getPortVariables();		
		initializeSendData(mc, pvars);
		
		//send the variable to queue
		ArrayList<CSLVariable> sendVariables = mc.sendVariables;
		Set<String> sn_list = queue.keySet();		
		for(String sn: sn_list) {
			if(sn.equals(mc.serviceName)) continue;			
			ArrayList<CSLVariable> varlist = queue.get(sn);				
			varlist.addAll(sendVariables);
		}		
				
		return true;
	}
	
	private void initializeSendData(MyConstraint mc, ArrayList<PortVariable> pvars) {
		
		//if(mc.sendVariables==null || mc.searchVariables==null) {
			ArrayList<CSLVariable> search_variable = new ArrayList<CSLVariable>();
			ArrayList<CSLVariable> sendVariables = new ArrayList<CSLVariable>();
			mc.sendVariables = sendVariables;
			mc.searchVariables = search_variable;
		
			for(PortVariable pv: pvars) {
				String vn = pv.getName();			
				CSLVariable csl = getVariableByName(currentVariables, mc.serviceName, vn);
				
				if(csl==null) {														
					HashMap<String, String> schema = variable_schema.get(mc.serviceName);					
					csl = CSLVariable.initialize(vn, schema.get(vn), mc.serviceName);
					
					//add assignVariable to bind variable
					currentVariables.add(csl);
					search_variable.add(csl);
				}	
						
			    //backup all the sent variables
			    sendVariables.add(csl.backup());
			}				
		
//		} else
//			for(CSLVariable csl: mc.searchVariables) {
//				csl.init();
//			}
	}

	private boolean backtrackSendExpression(MyConstraint mc) {
		ArrayList<CSLVariable> search_variable = mc.searchVariables;
		ArrayList<CSLVariable> sendVariables = mc.sendVariables;				
		
		boolean hasNextDataField = nextDataField(search_variable);
		if(!hasNextDataField) {																			
			currentVariables.removeAll(search_variable);
			
			//remove all send variables from other queues 
			Set<String> sn_list = queue.keySet();		
			for(String sn: sn_list) {
				if(sn.equals(mc.serviceName)) continue;			
				ArrayList<CSLVariable> varlist = queue.get(sn);				
				varlist.removeAll(sendVariables);
			}		
			
			return false;				
		} 		
				
		//re-send the variable to queue (update the value)
		for(CSLVariable ssl: search_variable) {
			for(CSLVariable csl: sendVariables) {
				
				if(csl.getName().equals(ssl.getName()) &&
					csl.getServiceName().equals(ssl.getServiceName()) &&
					csl.getType().equals(ssl.getType())) {
					
					csl.setValue(ssl.getValue());
					break;
				}
					
			}
		}
		
		return true;
	}	
	
	public ArrayList<CSLVariable> getSolution() {
		return this.currentVariables;
	}

	public void setBindVariables(ArrayList<CSLVariable> bindVariables) {
		currentVariables.clear();
		currentVariables.addAll(bindVariables);		
	}
}
