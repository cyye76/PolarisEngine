package ServiceTesting.EventInterface;

import java.util.ArrayList;
import java.util.HashMap;


import Service.AbstractService;
import Service.Task;
import Service.Transition;
import ServiceTesting.ConstraintSolver.MyConstraint;
import Utils.WordParser;

public class PathCoverageEventInterface extends EventInterface {
	
	private HashMap<String, ArrayList<String>> orders = new HashMap<String, ArrayList<String>>();
	private String startTask;

	/*
	 * source_task, sink_task, conditions (or)
	 */
	private HashMap<String, HashMap<String, ArrayList<MyConstraint>>> causality = new HashMap<String, HashMap<String, ArrayList<MyConstraint>>>();

	@Override
	public void deriveEventInterface(AbstractService service) {		
		
		setServiceName(service.getName());
		
		String taskname = service.getStartTask();
		orders.put(taskname, null);
		this.setStartTask(taskname);
		
		ArrayList<String> visited = new ArrayList<String>();
		visited.add(taskname);
		
		Task t1 = service.getTaskbyName(taskname);
		String tc = t1.getEffect();	
				
		ArrayList<MyConstraint> conditions = addConstraints(tc, service.getName());
		addConditions("START", taskname, conditions);
		conditions = new ArrayList<MyConstraint>();
		
		traverseService(service, taskname, taskname, visited, conditions);
	}

	private ArrayList<MyConstraint> addConstraints(String tc, String serviceName) {
		
		ArrayList<MyConstraint> result = new ArrayList<MyConstraint>();
		
		WordParser parser = new WordParser(tc);
		String[] exps = parser.getExpression();
		if(exps!=null) {
			ArrayList<String> idc_list = new ArrayList<String>();
			
			if(exps.length>1 && exps[1].equals(":=")) { //assignment
				String ss = tc;
				int index = ss.indexOf(';');
				while(index>=0) {
					String si = ss.substring(0, index+1);
					idc_list.add(si);
					ss = ss.substring(index+1);
					index = ss.indexOf(';');
				}
				
			} else 
				idc_list.add(tc);
			
			for(String si: idc_list) {
				MyConstraint mc = new MyConstraint();
				mc.condition = si;
				mc.serviceName = serviceName;
				result.add(mc);
			}
		}
		
		return result;
	}

	private void traverseService(AbstractService service, String taskname, String preceding,
			ArrayList<String> visited, ArrayList<MyConstraint> conditions) {		
		ArrayList<Transition> trans = service.getAvailableNextTransitions(taskname);
		String nextpreceding = preceding;
		
		if(trans!=null) {
			for(Transition tr: trans) {
				
				ArrayList<MyConstraint> nc_list = new ArrayList<MyConstraint>();
				nc_list.addAll(conditions);
				
				String sink = tr.getSink();
				String gc = tr.getGuard().getGuard();
				MyConstraint mc = new MyConstraint();
				mc.condition = gc;
				mc.serviceName = service.getName();
				nc_list.add(mc);
				
				Task t1 = service.getTaskbyName(sink);
				String tc = t1.getEffect();				
				nc_list.addAll(addConstraints(tc, service.getName()));
												
				if(trans.size()>1 || hasMultipleIncoming(service, sink) || isPortTask(service, sink)) {
					addOrder(preceding, sink);
					addConditions(preceding, sink, nc_list);
					nextpreceding = sink;
					nc_list = new ArrayList<MyConstraint>();
				} 				
				
				if(!visited.contains(sink)) {
					visited.add(sink);
					traverseService(service, sink, nextpreceding, visited, nc_list);
				}
			}						
		}
	}

	private void addConditions(String preceding, String sink, ArrayList<MyConstraint> conditions) {
		HashMap<String, ArrayList<MyConstraint>> c1 = causality.get(preceding);
		if(c1==null) {
			c1 = new HashMap<String, ArrayList<MyConstraint>>();
			causality.put(preceding, c1);
		}				
		
		c1.put(sink, conditions);			
	}

	private boolean isPortTask(AbstractService service, String sink) {
		Task task = service.getTaskbyName(sink);
		assert(task!=null);
		return task.isPort();
	}

	private boolean hasMultipleIncoming(AbstractService service, String sink) {

		ArrayList<Transition> trans = service.getTransitions();
		int count = 0;
		for(Transition tr: trans) {
			if(sink.equals(tr.getSink())) count++;
		}
		
		return count>1;
	}

	private void addOrder(String preceding, String sink) {		
		ArrayList<String> out = orders.get(preceding);
		if(out==null) {
			out = new ArrayList<String>();
			orders.put(preceding, out);
		}
		
		if(!out.contains(sink))
			out.add(sink);
	}

	public HashMap<String, ArrayList<String>> getOrders() {
		return orders;
	}

	public void setStartTask(String startTask) {
		this.startTask = startTask;
	}

	public String getStartTask() {
		return startTask;
	}
	
	public HashMap<String, HashMap<String, ArrayList<MyConstraint>>> getCausality() {
		return causality;
	}
	
	public ArrayList<MyConstraint> getCausalityCondition(String preceding, String next) {
		
		HashMap<String, ArrayList<MyConstraint>> tm = causality.get(preceding);
		if(tm==null) return null;
		
		return tm.get(next);
	}
}
