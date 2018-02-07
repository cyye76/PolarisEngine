package ServiceTesting.ConstraintSolver;

import java.util.ArrayList;
import java.util.HashMap;


public class ConstraintTransformer {
	
	private static void adjustConstraintOrders(ArrayList<MyConstraint> cs_list, 
    		HashMap<String, HashMap<String, String>> schema) {
		
		int pointer = cs_list.size()-1;
		ArrayList<MyConstraint> moved = new ArrayList<MyConstraint>();
		while(pointer >=0) {
			MyConstraint mc = cs_list.get(pointer);
    		if(moved.contains(mc)) {//already moved
    			pointer--;
    			continue;
    		}
    		
    		if(mc.isAssignment()) {
    			postponeAssignmentConstraints(cs_list, mc, moved, schema, pointer+1);    				
    		}     		
    		
    		if(mc.isBooleanExpression()) {
    		    postponeBooleanConstraints(cs_list, mc, moved, schema, pointer+1);
    		}	
    		
    		pointer--;
		}
	}

    private static boolean postponeBooleanConstraints(
			ArrayList<MyConstraint> cs_list, MyConstraint mc,
			ArrayList<MyConstraint> moved,
			HashMap<String, HashMap<String, String>> schema, int next) {
    	
    	HashMap<String, String> map = schema.get(mc.serviceName);
    	ArrayList<String> rv_list = mc.getExpressionVariables(map, false);
		
    	int pt = next;
    	while(pt<cs_list.size()) {
    		MyConstraint n_c = cs_list.get(pt);
    		if(n_c.serviceName.equals(mc.serviceName)) {    		    		    
    		    /*
    		    if(n_c.isReceive()) {
    		    	ArrayList<String> pvs = n_c.getPortVariables();    		    	
    		    	
    		    	boolean existing = false;
    		    	for(String rv:rv_list)
    		    		if(pvs.contains(rv)) {
    		    			existing = true;
    		    			break;
    		    		}
    		    		
    		    	if(existing) break;
    		    }    		        		    
    		    
    		    if(n_c.isAssignment()) {    		    	
    		    	String ban = n_c.getAssignmentVariable();    		    	    		    	
    		    	if(rv_list.contains(ban)) break;
    		    }*/
    		}
    		
    		pt++;
    	}
    	
    	if(pt>next) {//adjust the order
    		ArrayList<MyConstraint> buffer = new ArrayList<MyConstraint>();
    		for(int i=0;i<next-1;i++)
    			buffer.add(cs_list.get(i));
    		
    		for(int i=next;i<pt;i++)
    			buffer.add(cs_list.get(i));
    		
    		buffer.add(mc);
    		
    		for(int i=pt;i<cs_list.size();i++) 
    			buffer.add(cs_list.get(i));
    		
    		cs_list.clear();
    		cs_list.addAll(buffer);
    		
    		moved.add(mc);
    		
    		return true;
    	}

    	return false;
	}

	private static boolean postponeAssignmentConstraints(
			ArrayList<MyConstraint> cs_list, MyConstraint mc,
			ArrayList<MyConstraint> moved,
			HashMap<String, HashMap<String, String>> schema, int next) {
		
    	String avn = mc.getAssignmentVariable();
    	HashMap<String, String> map = schema.get(mc.serviceName);
    	ArrayList<String> rv_list = mc.getExpressionVariables(map, true);
		
    	int pt = next;
    	while(pt<cs_list.size()) {
    		MyConstraint n_c = cs_list.get(pt);
    		if(n_c.serviceName.equals(mc.serviceName)) {
    			/*
    		    if(n_c.isSend()) {
                    ArrayList<String> pvs = n_c.getPortVariables();
                    if(pvs.contains(avn)) break;
    		    }
    		    
    		    if(n_c.isReceive()) {
    		    	ArrayList<String> pvs = n_c.getPortVariables();
    		    	if(pvs.contains(avn)) break;
    		    	
    		    	boolean existing = false;
    		    	for(String rv:rv_list)
    		    		if(pvs.contains(rv)) {
    		    			existing = true;
    		    			break;
    		    		}
    		    		
    		    	if(existing) break;
    		    }
    		    
    		    if(n_c.isBooleanExpression()) {
    		    	HashMap<String, String> bmap = schema.get(n_c.serviceName);
    		    	ArrayList<String> bvs = n_c.getExpressionVariables(bmap, false);
    		    	if(bvs.contains(avn)) break;
    		    }
    		    
    		    if(n_c.isAssignment()) {
    		    	HashMap<String, String> bmap = schema.get(n_c.serviceName);
    		    	ArrayList<String> bvs = n_c.getExpressionVariables(bmap, true);
    		    	String ban = n_c.getAssignmentVariable();
    		    	
    		    	if(bvs.contains(avn)) break;
    		    	if(ban.equals(avn)) break;
    		    	if(rv_list.contains(ban)) break;
    		    }*/
    		}
    		
    		pt++;
    	}
    	
    	if(pt>next) {//adjust the order
    		ArrayList<MyConstraint> buffer = new ArrayList<MyConstraint>();
    		for(int i=0;i<next-1;i++)
    			buffer.add(cs_list.get(i));
    		
    		for(int i=next;i<pt;i++)
    			buffer.add(cs_list.get(i));
    		
    		buffer.add(mc);
    		
    		for(int i=pt;i<cs_list.size();i++) 
    			buffer.add(cs_list.get(i));
    		
    		cs_list.clear();
    		cs_list.addAll(buffer);
    		
    		moved.add(mc);
    		
    		return true;
    	}
    	
    	return false;
    	
	}

	private static void transformReceiveConstraints(ArrayList<MyConstraint> cs_list, 
    		HashMap<String, HashMap<String, String>> schema) {
    
    	int pointer = 0;
    	ArrayList<MyConstraint> moved = new ArrayList<MyConstraint>();
    	while(pointer < cs_list.size()) {
    		MyConstraint mc = cs_list.get(pointer);
    		if(moved.contains(mc)) {//already moved
    			pointer++;
    			continue;
    		}
    		
    		if(mc.isReceive()) {
    			
    			ArrayList<String> variables = mc.getPortVariables();
    			ArrayList<String> postVariables = new ArrayList<String>();
    			
    			HashMap<String, String> vsm = schema.get(mc.serviceName);
    			
    			for(String vn: variables) {
    				if(postponeReceiveVariable(vn, vsm, mc.serviceName, pointer+1, cs_list, moved)) postVariables.add(vn);
    			}   
    			
    			variables.removeAll(postVariables);
    			if(variables.isEmpty())  
    				cs_list.remove(mc);
    			else {
    				mc.condition = "receive";
    				for(String vn: variables)
    					mc.condition += " " + vn;
    				
    				pointer++;
    			}
    		
    		} else    		
    		    pointer++;
    	}    
    	
    	filterReceive(cs_list);
    }
		
	private static boolean postponeReceiveVariable(String vn, HashMap<String, String> schema,
			String serviceName, int location, ArrayList<MyConstraint> cs_list, ArrayList<MyConstraint> moved) {
		
		int pointer = location;
		while(pointer < cs_list.size()) {
		    MyConstraint mc = cs_list.get(pointer);
		    if(!mc.serviceName.equals(serviceName)) {
		    	pointer++;
		    	continue;
		    }
		    
		    if(mc.isReceive()) {
		    	pointer++;		    	
		    }
		    
		    if(mc.isSend()) {
		    	ArrayList<String> variables = mc.getPortVariables();
		    	if(variables.contains(vn)) 
		    		break;
		    	else
		    		pointer++;
		    }
		    
		    if(mc.isAssignment() || mc.isBooleanExpression()) {
		    	ArrayList<String> variables = mc.getExpressionVariables(schema, false);
		    	if(variables.contains(vn)) 
		    		break;
		    	else
		    		pointer++;
		    }
		    	
		}
		
		if(pointer > location) {
			if(pointer < cs_list.size()) {
				ArrayList<MyConstraint> buffer = new ArrayList<MyConstraint>();
				for(int i=0;i<pointer;i++)
					buffer.add(cs_list.get(i));
			
				MyConstraint mc = new MyConstraint();
				mc.serviceName = serviceName;
				mc.condition = "receive " + vn;
			
				buffer.add(mc);
				moved.add(mc);			
			
				for(int i=pointer;i<cs_list.size();i++)
					buffer.add(cs_list.get(i));
			
				cs_list.clear();
				cs_list.addAll(buffer);
			}
			
			return true;
		}
		
		return false;
	}
	
	private static void transformSendConstraints(ArrayList<MyConstraint> cs_list, 
			HashMap<String, HashMap<String, String>> schema) {    	
    
    	int pointer = 0;
    	ArrayList<MyConstraint> moved = new ArrayList<MyConstraint>();
    	while(pointer < cs_list.size()) {
    		MyConstraint mc = cs_list.get(pointer);
    		if(moved.contains(mc)) {
    			pointer++;
    			continue;
    		}
    		
    		if(mc.isSend()) {
    			
    			ArrayList<String> variables = mc.getPortVariables();
    			ArrayList<String> postVariables = new ArrayList<String>();
    			
    			HashMap<String, String> vsm = schema.get(mc.serviceName); 
    			for(String vn: variables) {
    				if(postponeSendVariable(vn, vsm, mc.serviceName, pointer+1, cs_list, moved)) postVariables.add(vn);
    			}   
    			
    			variables.removeAll(postVariables);
    			if(variables.isEmpty()) 
    				cs_list.remove(mc);
    			else {
    				mc.condition = "send";
    				for(String vn: variables)
    					mc.condition += " " + vn;
    				
    				pointer++;
    			}
    		} else    		
    		    pointer++;
    	}  
    	
    	filterSend(cs_list);
    }
	
	private static void filterReceive(ArrayList<MyConstraint> cs_list) {
		int pointer=cs_list.size()-1;
		while(pointer>=0) {
			MyConstraint mc = cs_list.get(pointer);
			if(mc.isReceive())
				cs_list.remove(mc);
			else
				break;
			
			pointer--;
		}
	}
	
	private static void filterSend(ArrayList<MyConstraint> cs_list) {
		int pointer=cs_list.size()-1;
		while(pointer>=0) {
			MyConstraint mc = cs_list.get(pointer);
			if(mc.isSend())
				cs_list.remove(mc);
			else
				break;
			
			pointer--;
		}
	}

	private static boolean postponeSendVariable(String vn,
			HashMap<String, String> schema, String serviceName, int location,
			ArrayList<MyConstraint> cs_list, ArrayList<MyConstraint> moved) {
		
		int pointer = location;
		while(pointer < cs_list.size()) {
		    MyConstraint mc = cs_list.get(pointer);		    		    
		    if(mc.isSend() || mc.isBooleanExpression()) pointer++;
		    
		    if(mc.isReceive()) {
		    	ArrayList<String> variables = mc.getPortVariables();
		    	if(variables.contains(vn)) 
		    		break;
		    	else
		    		pointer++;
		    }
		    
		    if(mc.isAssignment()) {
		    	if(serviceName.equals(mc.serviceName)) {
		    		String assign = mc.getAssignmentVariable();
		    		if(vn.equals(assign)) 
		    			break;
		    		else
		    			pointer++;
		    	}else
		    		pointer++;
		    }
		}
		
		if(pointer > location) {
			if(pointer < cs_list.size()) {
				ArrayList<MyConstraint> buffer = new ArrayList<MyConstraint>();
				for(int i=0;i<pointer;i++)
					buffer.add(cs_list.get(i));
			
				MyConstraint mc = new MyConstraint();
				mc.serviceName = serviceName;
				mc.condition = "send " + vn;
			
				buffer.add(mc);
				moved.add(mc);
			
				for(int i=pointer;i<cs_list.size();i++)
					buffer.add(cs_list.get(i));
			
				cs_list.clear();
				cs_list.addAll(buffer);
			}
			
			return true;
		}
		
		return false;
	}

	public static void transformConstraints(ArrayList<MyConstraint> cs_list, 
			HashMap<String, HashMap<String, String>> schema) {		
		transformReceiveConstraints(cs_list, schema);
		transformSendConstraints(cs_list, schema);
		adjustConstraintOrders(cs_list, schema);
		//transformReceiveConstraints(cs_list, schema);
		//transformSendConstraints(cs_list, schema);
	}
	
	public static void filterInvalidConstraints(ArrayList<MyConstraint> cs_list) {
		HashMap<String, ArrayList<String>> queue = new HashMap<String, ArrayList<String>>();
		
		ArrayList<MyConstraint> filter = new ArrayList<MyConstraint>();		
		ArrayList<String> sn_list = new ArrayList<String>();
		
		for(MyConstraint mc: cs_list)
			if(!sn_list.contains(mc.serviceName)) sn_list.add(mc.serviceName);
		
		for(MyConstraint mc: cs_list) {
			if(mc.isSend()) {
				ArrayList<String> pvs = mc.getPortVariables();				
				for(String sn: sn_list) {
					if(sn.equals(mc.serviceName)) continue;
					
					ArrayList<String> que = queue.get(sn);
					if(que == null) {
						que = new ArrayList<String>();
						queue.put(sn, que);
					}
					que.addAll(pvs);					
				}
			}
			
			if(mc.isReceive()) {
				ArrayList<String> que = queue.get(mc.serviceName);
				ArrayList<String> pvs = mc.getPortVariables();
				if(que==null){ 					
					filter.add(mc);
				} else {
					if(!que.containsAll(pvs)) {
						ArrayList<String> extra = new ArrayList<String>();
						extra.addAll(pvs);
						extra.removeAll(que);
						pvs.removeAll(extra);
						if(pvs.isEmpty()) 
							filter.add(mc);
						else {
							mc.condition = "receive";
							for(String vn: pvs)
								mc.condition += " " + vn;
						}
					}
					
					que.removeAll(pvs);
				}
			}
		}
		
		cs_list.removeAll(filter);
	}

	public static HashMap<ArrayList<MyConstraint>, Integer> removeRedundantConstraints(
			ArrayList<ArrayList<MyConstraint>> constraint_list) {
		HashMap<ArrayList<MyConstraint>, Integer> ws = new HashMap<ArrayList<MyConstraint>, Integer>();
		
		ArrayList<ArrayList<MyConstraint>> filter = new ArrayList<ArrayList<MyConstraint>>(); 
		
		for(int i=0;i<constraint_list.size();i++) {
			ArrayList<MyConstraint> cs_list = constraint_list.get(i);
			
			for(int j=0;j<constraint_list.size();j++) {
				if(i==j) continue;
				ArrayList<MyConstraint> item_list = constraint_list.get(j);
				
				if(isSubConstraintList(cs_list, item_list)) {
                    filter.add(cs_list);
                    addWeight(ws, item_list, cs_list);
                    break;
				}
			}
		}
		
		constraint_list.removeAll(filter);
		
		return ws;
		
	}

	private static void addWeight(HashMap<ArrayList<MyConstraint>, Integer> ws, ArrayList<MyConstraint> item_list,
			ArrayList<MyConstraint> cs_list) {
		
		Integer w1 = ws.get(cs_list);
		if(w1==null) w1 = 1;
		
		Integer w2 = ws.get(item_list);
		if(w2==null)
			w2 = 1;
		
		w2 += w1;
		ws.put(item_list, w2);
		
	}

	private static boolean isSubConstraintList(ArrayList<MyConstraint> cs_list,
			ArrayList<MyConstraint> item_list) {
		
		if(cs_list.size()> item_list.size()) return false;
		
		for(int i=0;i<cs_list.size();i++) {
			MyConstraint mc1 = cs_list.get(i);
			MyConstraint mc2 = item_list.get(i);
			
			if(!mc1.isSame(mc2)) return false;
		}
		
		return true;
	}

	public static void removeTrueConstraints(
			ArrayList<ArrayList<MyConstraint>> constraint_list) {
		
		ArrayList<MyConstraint> filter = new ArrayList<MyConstraint>();
		ArrayList<ArrayList<MyConstraint>> buffer = new ArrayList<ArrayList<MyConstraint>>();
		
		for(ArrayList<MyConstraint> mc_list: constraint_list) {
			filter.clear();
			for(MyConstraint mc: mc_list) {
				if(mc.condition.equals("true"))
					filter.add(mc);
			}
			
			mc_list.removeAll(filter);
			if(mc_list.isEmpty()) buffer.add(mc_list);
		}
		
		constraint_list.removeAll(buffer);
	}
	
}