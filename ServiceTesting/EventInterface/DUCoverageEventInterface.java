package ServiceTesting.EventInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import engine.expression.Arithmetic.ArithmeticExpression;
import engine.expression.Arithmetic.ArithmeticTreeNode;
import engine.expression.Arithmetic.InnerArithmeticTreeNode;
import engine.expression.Arithmetic.LeafArithmeticTreeNode;
import engine.expression.AssignmentExpression.AssignmentExpression;
import engine.expression.BooleanExpression.BooleanExpression;
import engine.expression.BooleanExpression.BooleanTreeNode;
import engine.expression.BooleanExpression.InnerBooleanTreeNode;
import engine.expression.BooleanExpression.LeafBooleanTreeNode;
import engine.expression.PortExpression.PortExpression;
import engine.expression.PortExpression.PortVariable;
import engine.expression.PredicateExpression.PredicateExpression;
import engine.expression.ServiceInvocation.Parameter;
import engine.expression.ServiceInvocation.ServiceInvocation;
import engine.expression.TokenExpression.TokenExpression;

import Service.AbstractExpression;
import Service.AbstractService;
import Service.Guard;
import Service.Task;
import Service.Transition;

public class DUCoverageEventInterface extends EventInterface {
	
	private ArrayList<DUPair> du_list = new ArrayList<DUPair>();

	@Override
	public void deriveEventInterface(AbstractService service) {		
		
		setServiceName(service.getName());
		
		String startTask = service.getStartTask();
		HashMap<String, String> recentDef = new HashMap<String, String>();
		ArrayList<String> visitedTasks = new ArrayList<String>();//for loop
		
		deriveEventInterface(service, startTask, recentDef, visitedTasks, "");
				
	}

	private void deriveEventInterface(AbstractService service,
			String taskName, HashMap<String, String> recentDef, 
			ArrayList<String> visitedTasks,
			String conditions) {		
		
		Task task = service.getTaskbyName(taskName);
		assert(task!=null);
		HashMap<String, String> newDef;
		
		String newconditions = conditions + task.getEffect();
		if(task.isPort() || task.isService()) newconditions += ";";	
		
		if(task.isPort())
			newDef = traversePortTaskExpression(task, recentDef, newconditions);
		else if(task.isService())
			newDef = traverseServiceInvocation(task, recentDef, newconditions);
		else
			newDef = traverseLocalTaskExpression(task, recentDef, newconditions);
		
		if(!visitedTasks.contains(taskName)) {
		     visitedTasks.add(taskName);
		
		     ArrayList<Transition> transitions = service.getAvailableNextTransitions(taskName);
		     if(transitions!=null) {
		    	 for(Transition tr: transitions) {
		    		 deriveEventInterface(service, tr, newDef, visitedTasks, newconditions);
		    	 }
		     }
		}
	}

	private HashMap<String, String> traverseServiceInvocation(Task task,
			HashMap<String, String> recentDef, String conditions) {
		
		ServiceInvocation invocation = task.getInvocation();
		ArrayList<Parameter> inputparameters = invocation.getInputParameters();
		ArrayList<Parameter> outputparameters = invocation.getOutputParameters();
			
		for(Parameter pv: inputparameters) {
			String vName = pv.getName();
			String vLoc = pv.getvLoc();
			DUPair du = new DUPair(recentDef.get(vName), vLoc);
			du.addConditions(conditions);
			addNewGenerationDU(du);
		}

		HashMap<String, String> newDef = copyHashMap(recentDef);
		for(Parameter pv: outputparameters) {
				String vName = pv.getName();
				String vLoc = pv.getvLoc();
				newDef.put(vName, vLoc);
		}
			
		return newDef;
	}

	private void deriveEventInterface(AbstractService service, Transition tr,
			HashMap<String, String> newDef, ArrayList<String> visitedTasks, String conditions) {		
		
		Guard guard = tr.getGuard();
		traverseGuardExpression(guard, newDef, conditions);
		String taskName = tr.getSink();
		if(!visitedTasks.contains(taskName)) {
			String newconditions = conditions + guard.getGuard() + ";";
		    deriveEventInterface(service, taskName, newDef, visitedTasks, newconditions);
		}
	}

	private void traverseGuardExpression(Guard guard,
			HashMap<String, String> newDef, String conditions) {
		
		AbstractExpression exp = guard.getGuardexp();
		traverseExpression(exp, newDef, conditions);
	}

	private void traverseExpression(AbstractExpression exp,
			HashMap<String, String> result, String conditions) {
		
		if(exp instanceof TokenExpression) {
			TokenExpression te = (TokenExpression) exp;			
			LeafArithmeticTreeNode node = te.getNode();
			
			traverseLeafArithmeticTreeNode(node, result, conditions);			
		
		} else
			
		if(exp instanceof ArithmeticExpression)	{			
			ArithmeticExpression ae = (ArithmeticExpression) exp;
			ArithmeticTreeNode node = ae.getExpression(); 
			
			traverseArithmeticTreeNode(node, result, conditions);			
			
		} else
			
		if(exp instanceof BooleanExpression) {
			BooleanExpression be = (BooleanExpression) exp;
			BooleanTreeNode node = be.getExpression();
			
			traverseBooleanTreeNode(node, result, conditions);
		}		
	}

	private void traverseBooleanTreeNode(BooleanTreeNode node,
			HashMap<String, String> result, String conditions) {
		
		if(node instanceof LeafBooleanTreeNode) {
			LeafBooleanTreeNode leafnode = (LeafBooleanTreeNode)node;
			traverseLeafBooleanTreeNode(leafnode, result, conditions);
		} else
			
		if(node instanceof InnerBooleanTreeNode) {
			InnerBooleanTreeNode innernode = (InnerBooleanTreeNode)node;
			traverseInnerBooleanTreeNode(innernode, result, conditions);
		}
		
	}

	private void traverseInnerBooleanTreeNode(InnerBooleanTreeNode innernode, HashMap<String, String> result, String conditions) {
		BooleanTreeNode leftnode = innernode.getLeftChild();
		BooleanTreeNode rightnode = innernode.getRightChild();
		
		traverseBooleanTreeNode(leftnode, result, conditions);
		
		if(rightnode!=null) //rightnode ==null if it is a "! BE"
			traverseBooleanTreeNode(rightnode, result, conditions);
	}

	private void traverseLeafBooleanTreeNode(LeafBooleanTreeNode leafnode,
			HashMap<String, String> result, String conditions) {
		int type = leafnode.getType();
		
		if(type == LeafBooleanTreeNode.VARIABLE_TYPE) {
			String vName = leafnode.getVariableName();
			String vLoc = leafnode.getVariableLoc();
			DUPair du = new DUPair(result.get(vName), vLoc);
			du.addConditions(conditions);
			addNewGenerationDU(du);
			
		} else 
			
		if(type == LeafBooleanTreeNode.PREDICATE_TYPE) {
			PredicateExpression pexp = leafnode.getPredicate();
			traversePredicateExpression(pexp, result, conditions);
		}
	}

	private void traversePredicateExpression(PredicateExpression pexp,
			HashMap<String, String> result, String conditions) {
        
		AbstractExpression leftExpression = pexp.getLeftExpression();
		AbstractExpression rightExpression = pexp.getRightExpression();
		
		traverseExpression(leftExpression, result, conditions);
		traverseExpression(rightExpression, result, conditions);
	}

	private void traverseArithmeticTreeNode(ArithmeticTreeNode node, HashMap<String, String> result, String conditions) {
		
		if(node instanceof LeafArithmeticTreeNode) {
			LeafArithmeticTreeNode leafnode = (LeafArithmeticTreeNode)node;
			traverseLeafArithmeticTreeNode(leafnode, result, conditions);
		} else
			
		if(node instanceof InnerArithmeticTreeNode) {
			InnerArithmeticTreeNode innernode = (InnerArithmeticTreeNode)node;
			traverseInnerArithmeticTreeNode(innernode, result, conditions);
		}
		
	}

	private void traverseInnerArithmeticTreeNode(
			InnerArithmeticTreeNode innernode, HashMap<String, String> result, String conditions) {		
		ArithmeticTreeNode leftnode = innernode.getLeftChild();
		ArithmeticTreeNode rightnode = innernode.getRightChild();
		
		traverseArithmeticTreeNode(leftnode, result, conditions);
		traverseArithmeticTreeNode(rightnode, result, conditions);
		
	}

	private void traverseLeafArithmeticTreeNode(LeafArithmeticTreeNode node,
			HashMap<String, String> result, String conditions) {
		
		int nodeType = node.getType();
		if(nodeType == LeafArithmeticTreeNode.VARIAVLE_TYPE) {
			String vName = node.getVariableName();
			String vLoc = node.getVariableLoc();
			DUPair du = new DUPair(result.get(vName), vLoc);
			du.addConditions(conditions);
			addNewGenerationDU(du);
		}
		
	}

	private void addNewGenerationDU(DUPair du) {
		Boolean existing = false;
		du.setServiceName(getServiceName());
		
		for(DUPair item: du_list) {
			if(item.equals(du)) {
				existing = true;
				ArrayList<String> conditions = du.getConditions();
				for(String cd: conditions)
					item.addConditions(cd);
				break;
			}
		}
		
		if(!existing) {			
			du_list.add(du);
		}
	}

	private HashMap<String, String> copyHashMap(HashMap<String, String> newDef) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		Set<String> keys = newDef.keySet();
		for(String key: keys)
			result.put(key, newDef.get(key));
		
		return result;
	}

	private HashMap<String, String> traverseLocalTaskExpression(Task task, HashMap<String, String> recentDef, String conditions) {
		
		HashMap<String, String> newDef = copyHashMap(recentDef);
		ArrayList<AssignmentExpression> ase_list = task.getAssigns();
		for(AssignmentExpression ae: ase_list)
			traverseAssignmentExpression(ae, newDef, conditions);
		
		return newDef;
	}

	private void traverseAssignmentExpression(AssignmentExpression ae,
			HashMap<String, String> newDef, String conditions) {
		
		AbstractExpression exp = ae.getExpression();
		traverseExpression(exp, newDef, conditions);
		
		String vName = ae.getVariable();
		String vLoc = ae.getVariableLoc();
		
		//update the definition of vName
		newDef.put(vName, vLoc);
	}

	private HashMap<String, String> traversePortTaskExpression(Task task, HashMap<String, String> recentDef, String conditions) {

		PortExpression port_exp = task.getPortexp();
		ArrayList<PortVariable> port_variables = port_exp.getPortVariables();
		
		if(task.isSend()) {
			
			for(PortVariable pv: port_variables) {
				String vName = pv.getName();
				String vLoc = pv.getvLoc();
				DUPair du = new DUPair(recentDef.get(vName), vLoc);
				du.addConditions(conditions);
				addNewGenerationDU(du);
			}
		
			return recentDef;
		
		} else {
			
			HashMap<String, String> newDef = copyHashMap(recentDef);
			for(PortVariable pv: port_variables) {
				String vName = pv.getName();
				String vLoc = pv.getvLoc();
				newDef.put(vName, vLoc);
			}
			
			return newDef;
		}
	}

	public ArrayList<DUPair> getDUPairs() {
		return du_list;
	}
}
