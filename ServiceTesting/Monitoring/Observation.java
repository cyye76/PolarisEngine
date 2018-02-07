package ServiceTesting.Monitoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import engine.Event.Event;
import engine.Event.TaskCompleteEvent;
import engine.expression.PortExpression.PortExpression;
import engine.expression.PortExpression.PortVariable;
import Configuration.Config;
import Service.AbstractService;
import Service.Task;
import ServiceTesting.ConstraintSolver.MyConstraint;
import ServiceTesting.EventInterface.ActivityCoverageEventInterface;
import ServiceTesting.EventInterface.BranchCoverageEventInterface;
import ServiceTesting.EventInterface.DUCoverageEventInterface;
import ServiceTesting.EventInterface.DUPair;
import ServiceTesting.EventInterface.PathCoverageEventInterface;
import Utils.WordParser;

public abstract class Observation implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8135036177941148604L;

	//all the non-observations
	protected ArrayList<ObservationUnit> non_observations 
	           = new ArrayList<ObservationUnit>();
	
	//all the already observations
	protected ArrayList<ObservationUnit> my_observations 
               = new ArrayList<ObservationUnit>();
	
	//this sets the flag to calculate the coverage, which is
	//local to a service or global to a service composition
	//private boolean local=false;
	//public void setLocalFlag(boolean flag) {
	//	local = flag;
	//}	
	
	
	/*
	 * add the observed event into the observation
	 * return true if the added event helps to improve the 
	 * coverage; otherwise, return false.
	 */
	public boolean addObservedEvent(ArrayList<Event> events) {
		ArrayList<Event> buffer = new ArrayList<Event>();
		buffer.addAll(events);
		
		
		//ArrayList<String> exposedEvents = null;
		for(Event event: buffer) {
			for(ObservationUnit unit: non_observations) {
				unit.feedEvent(event);
				//if(exposedEvents == null && unit instanceof EventSequenceObservationUnit) exposedEvents = ((EventSequenceObservationUnit)unit).getExposedEvents();
			}
		}
		
		//for debug
		
		
		//outputEventSequence(events, exposedEvents);
		//outputObservations();		
		
		
		ArrayList<ObservationUnit> covered = new ArrayList<ObservationUnit>();
		for(ObservationUnit unit: non_observations) {
			if(unit.isObserved()) {
				covered.add(unit);
				
				//for debugging
				//System.out.println("matched");
				//unit.rollback();
			}else {
				unit.rollback();//reset the matching state of the unmatched unit
			}
		}
		
		non_observations.removeAll(covered);
		my_observations.addAll(covered);
		
		return covered.size()>0;
	}
	
	private void outputObservations() {
		for(ObservationUnit unit: non_observations) {
			unit.print();
			System.out.println("------------------------------");
		}
		
	}

	private void outputEventSequence(ArrayList<Event> events, ArrayList<String> exposedEvents) {
		HashMap<String, ArrayList<String>> maps = new HashMap<String, ArrayList<String>>();
		for(Event event: events) {
			if(event instanceof TaskCompleteEvent) {
				String sn = event.getServiceName();		
				//if(!sn.equals("Buyer1") && !sn.equals("Saler")) continue;
				String tn = ((TaskCompleteEvent) event).getTaskName();
				if(!exposedEvents.contains(sn+":"+tn)) continue;
				
				ArrayList<String> list = maps.get(sn);
				if(list==null) {
					list = new ArrayList<String>();
					maps.put(sn, list);
				}
				
				list.add(tn);
			}
		}
		
		Set<String> keys = maps.keySet();
		for(String key: keys) {
			ArrayList<String> list = maps.get(key);
			System.out.print(key+":");
			for(String tn: list) 
				System.out.print(tn + " ");
			System.out.println();
		}
		
		System.out.println("========================================");
		System.out.println();
	}

	/*
	 * Report the coverage
	 */
	abstract public double getCoverage();				
	
	public static Observation constructActivityCoverageObservation(ArrayList<ActivityCoverageEventInterface> eventinterfaces) {
		LocalObservation observation = new LocalObservation();
		HashMap<ObservationUnit, String> mapping = new HashMap<ObservationUnit, String>();
		
		for(ActivityCoverageEventInterface acei: eventinterfaces) {
			
			String serviceName = acei.getServiceName();
			ArrayList<String> aclist = acei.getActivityList();
			
			for(String acname: aclist) {
		
				ActivityObservationUnit unit = new ActivityObservationUnit();
				unit.setServiceName(serviceName);
				unit.setTaskName(acname);
				mapping.put(unit, serviceName);
				
				observation.addObservationUnit(unit);
			}
		}
		
		observation.setMapping(mapping);
		return observation;
	}
	
	public void addObservationUnit(ObservationUnit unit) {
		boolean exist = false;
		for(ObservationUnit eu: non_observations) {
			if(eu.equals(unit)) {
				exist = true;
				break;
			}				
		}
		
		if(!exist) non_observations.add(unit);
	}

	public static Observation constructBranchCoverageObservation(ArrayList<BranchCoverageEventInterface> eventinterfaces) {
		LocalObservation observation = new LocalObservation();
		HashMap<ObservationUnit, String> mapping = new HashMap<ObservationUnit, String>();
		
		for(BranchCoverageEventInterface bcei: eventinterfaces) {
			
			String serviceName = bcei.getServiceName();
			ArrayList<String> trlist = bcei.getTransitionList();
			
			for(String trname: trlist) {
		
				BranchObservationUnit unit = new BranchObservationUnit();
				unit.setServiceName(serviceName);
				unit.setTransitionName(trname);
				mapping.put(unit, serviceName);
				
				observation.addObservationUnit(unit);
			}
		}
		
		observation.setMapping(mapping);
		return observation;
	}
	
	public static Observation constructLocalDUCoverageObservation(ArrayList<DUCoverageEventInterface> eventinterfaces) {
		LocalObservation observation = new LocalObservation();
		HashMap<ObservationUnit, String> mapping = new HashMap<ObservationUnit, String>();
		
		for(DUCoverageEventInterface duei: eventinterfaces) {
			String serviceName = duei.getServiceName();
			ArrayList<DUPair> dupairs = duei.getDUPairs();
			
			for(DUPair du: dupairs) {
				DUObservationUnit unit = new DUObservationUnit(serviceName, du.getDef(), serviceName, du.getUse());
				mapping.put(unit, serviceName);
				observation.addObservationUnit(unit);
			}
		}
		
		observation.setMapping(mapping);
		return observation;
	}
	
	public static Observation constructGlobalDUCoverageObservation(ArrayList<DUCoverageEventInterface> eventinterfaces, 
			                 ArrayList<AbstractService> services) {
		
		GlobalObservation observation = new GlobalObservation();
		
		HashMap<String, ArrayList<DUPair>> link_source = new HashMap<String, ArrayList<DUPair>>();
		HashMap<String, ArrayList<DUPair>> link_sink = new HashMap<String, ArrayList<DUPair>>();
		
		for(DUCoverageEventInterface duei: eventinterfaces) {
			String serviceName = duei.getServiceName();
			ArrayList<DUPair> dupairs = duei.getDUPairs();		
			
			AbstractService service = getServiceByName(services, serviceName);
			ArrayList<Task> porttask = getPortTask(service);
			
			for(DUPair du: dupairs) {
				
				String vname = getPortVariableName(porttask, du.getDef());
				if(vname!=null) cacheDUPair(vname, du, link_sink, serviceName);//receive
				
				else {
					vname = getPortVariableName(porttask, du.getUse());
					if(vname!=null) cacheDUPair(vname, du, link_source, serviceName);//send
				
					else {
						DUObservationUnit unit = new DUObservationUnit(serviceName, du.getDef(), serviceName, du.getUse());
						ArrayList<String> conditions  = du.getConditions();
						for(String cd: conditions) {
						    ArrayList<MyConstraint> mc_list = constructConstraints(cd, du.getServiceName());	
						    unit.addConstraint(mc_list);
						}
						observation.addObservationUnit(unit);
					}
				}
			}									
		}
		
		Set<String> portVNs =  link_source.keySet();
		for(String pvn: portVNs) {
			ArrayList<DUPair> source = link_source.get(pvn);
			ArrayList<DUPair> sink = link_sink.get(pvn);
			
			if(source!=null && sink!=null) {
				for(DUPair sourcedu: source)
					for(DUPair sinkdu: sink) {//combine two DUs by port variable
						DUObservationUnit unit = new DUObservationUnit(sourcedu, sinkdu);
						observation.addObservationUnit(unit);
						
						ArrayList<String> conditions1  = sourcedu.getConditions();
						ArrayList<String> conditions2  = sinkdu.getConditions();
						for(String cd1: conditions1) 
						   for(String cd2: conditions2){
						      ArrayList<MyConstraint> mc_list1 = constructConstraints(cd1, sourcedu.getServiceName());						      						     
						      ArrayList<MyConstraint> mc_list2 = constructConstraints(cd2, sinkdu.getServiceName());
						      mc_list1.addAll(mc_list2);
						      unit.addConstraint(mc_list1);
						   }
					}
			}
			
			if(source==null && sink!=null) {
				for(DUPair sinkdu: sink) {
					DUObservationUnit unit = new DUObservationUnit(sinkdu.getServiceName(), sinkdu.getDef(), sinkdu.getServiceName(), sinkdu.getUse());
					observation.addObservationUnit(unit);
					
					ArrayList<String> conditions  = sinkdu.getConditions();
					for(String cd: conditions) {
					    ArrayList<MyConstraint> mc_list = constructConstraints(cd, sinkdu.getServiceName());	
					    unit.addConstraint(mc_list);
					}
				}
			}
			
			if(source!=null && sink==null) {
				for(DUPair sourcedu: source) {
					DUObservationUnit unit = new DUObservationUnit(sourcedu.getServiceName(), sourcedu.getDef(), sourcedu.getServiceName(), sourcedu.getUse());
					observation.addObservationUnit(unit);
					
					ArrayList<String> conditions  = sourcedu.getConditions();
					for(String cd: conditions) {
					    ArrayList<MyConstraint> mc_list = constructConstraints(cd, sourcedu.getServiceName());	
					    unit.addConstraint(mc_list);
					}
				}
			}
		}
				
		return observation;
	}
	
	private static ArrayList<MyConstraint> constructConstraints(String cd,
			String serviceName) {
		
		ArrayList<MyConstraint> result = new ArrayList<MyConstraint>();
		
		String buffer = cd;
		int index = buffer.indexOf(';');
		while(index>=0) {
			String item = buffer.substring(0, index);
			WordParser parser = new WordParser(item);
			String[] exps = parser.getExpression();
			if(exps!=null && exps.length > 1 && exps[1].equals(":=")) 
				item = item + ";";
			MyConstraint mc = new MyConstraint();
			mc.serviceName = serviceName;
			mc.condition = item;
			result.add(mc);
			
			buffer = buffer.substring(index+1);
			index = buffer.indexOf(';');
		}
		
		return result;
	}

	private static void cacheDUPair(String vname, DUPair du,
			HashMap<String, ArrayList<DUPair>> link, String serviceName) {
		
		ArrayList<DUPair> pair = link.get(vname);
		if(pair==null) {
			pair = new ArrayList<DUPair>();
			link.put(vname, pair);
		}
		
		du.setServiceName(serviceName);
		pair.add(du);
	}

	private static ArrayList<Task> getPortTask(AbstractService service) {
		
		ArrayList<Task> porttask = new ArrayList<Task>();
		
		ArrayList<Task> allTasks = service.getTasks();
		for(Task task: allTasks)
			if(task.isPort()) porttask.add(task);
		
		return porttask;
	}

	private static AbstractService getServiceByName(
			ArrayList<AbstractService> services, String serviceName) {
		for(AbstractService service: services)
			if(serviceName.equals(service.getName())) return service;
		return null;
	}

	

	private static String getPortVariableName(ArrayList<Task> porttask, String Lov) {
		
		String taskName = getTaskName(Lov);
		if(taskName==null) return null;				
		
		for(Task task: porttask) {
			String name = task.getName();
			if(!name.equals(taskName)) continue;
			
			PortExpression exp = task.getPortexp();
			ArrayList<PortVariable> portvariables = exp.getPortVariables();
			for(PortVariable pv: portvariables) 
				if(Lov.equals(pv.getvLoc())) return pv.getName();
			
		}
			
		return null;
	}

	private static String getTaskName(String lov) {		
		
		if(lov!=null && lov.startsWith("Task:")) {
			int index = lov.indexOf('_');
			return lov.substring(5,index);
		}
		return null;
	}

	public static Observation constructPathCoverageObservation(ArrayList<PathCoverageEventInterface> eventinterfaces, HashMap<String, ArrayList<String>> cho, HashMap<String,ArrayList<Task>> porttasks) {
		GlobalObservation observation = new GlobalObservation();
		
		HashMap<String, ArrayList<String>> history = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> queue = new HashMap<String, ArrayList<String>>();
		ArrayList<MyConstraint> constraint = new ArrayList<MyConstraint>();
		for(PathCoverageEventInterface pcei: eventinterfaces) {
			String serviceName = pcei.getServiceName();
			history.put(serviceName, new ArrayList<String>());
			queue.put(serviceName, new ArrayList<String>());
		}
		
		constructPaths(observation, eventinterfaces, history, queue, cho, porttasks, constraint);
		
		//for debugging
		//observation.outputObservations();		
		
		return observation;
	}
	
	private static boolean verifyPath(
	          HashMap<String, ArrayList<String>> history,  
	          HashMap<String, ArrayList<String>> cho, 
	          HashMap<String, ArrayList<Task>> porttasks) {
	    
	    HashMap<String, ArrayList<String>> queue = new HashMap<String, ArrayList<String>>();
	    
	    ArrayList<VCTask> availabletasks = new ArrayList<VCTask>();
	    Set<String> snlist = history.keySet();
        for(String sn: snlist) {
             queue.put(sn, new ArrayList<String>());
             
             ArrayList<String> path = history.get(sn);
             if(!path.isEmpty()) {
            	 String tkID = path.get(0);
                 Task tk = getPortTask(sn, tkID, porttasks);
                 VCTask tct = new VCTask();
                 tct.serviceName = sn;
                 tct.taskID = tkID;
                 tct.task = tk;
                 tct.cp = 0;
        
                 availabletasks.add(tct);
             }
        } 

        int notfw = 0;  
        ArrayList<String> gh = new ArrayList<String>();
        while(!availabletasks.isEmpty()) {
        	VCTask vct = availabletasks.remove(0);
        	boolean ret = invokeOneTask(vct.taskID, vct.serviceName,
        			gh, queue, vct.task, cho);
        	
        	if(ret) {
        		vct.cp++;
        		ArrayList<String> path = history.get(vct.serviceName);
        		if(path!=null && vct.cp< path.size()) {
        			String tkID = path.get(vct.cp);
                    Task tk = getPortTask(vct.serviceName, tkID, porttasks);
                    vct.taskID = tkID;
                    vct.task = tk;
                    availabletasks.add(vct);
        		} 
        		
                notfw = 0;
        	} else {//put it back to the queue
        		availabletasks.add(vct);
        		notfw++;
        	}
        	
        	if(notfw > availabletasks.size()) break;
        }
	    
	    return availabletasks.isEmpty();	    		      	    
	}
	
	private static void constructPaths(Observation observation,
			ArrayList<PathCoverageEventInterface> eventinterfaces,
			HashMap<String, ArrayList<String>> history, 
			HashMap<String, ArrayList<String>> queue, 
			HashMap<String, ArrayList<String>> cho, 
			HashMap<String, ArrayList<Task>> porttasks,
			ArrayList<MyConstraint> constraint) {
		
		//ArrayList<SearchTask> nextavailabletasks = getNextAvailableTasks(history, queue, eventinterfaces, cho, porttasks);
		ArrayList<SearchTask> nextavailabletasks = getNextAvailableTasks4CompletePath(history, queue, eventinterfaces, cho, porttasks);
		
		if(nextavailabletasks.isEmpty()) {
		
			if(isCompletePath(history, eventinterfaces)) {
			
				EventSequenceObservationUnit unit = new EventSequenceObservationUnit();
				unit.setSequence(history);
				unit.setConstraint(constraint);
			
				ArrayList<String> exposedEvent = getExposedEvents(eventinterfaces);
				unit.setExposedEvents(exposedEvent);
			
				observation.addObservationUnit(unit);			
			}
			
		} else {
		
			boolean forward = false;
			
			for(SearchTask st: nextavailabletasks) {
				String serviceName = st.serviceName;
				String taskID = st.taskID;
        	
				ArrayList<MyConstraint> mc_list = new ArrayList<MyConstraint>();
				AddConstraints(mc_list,constraint);
				
				ArrayList<String> seq = history.get(serviceName);				
												        	
				Task task = getPortTask(serviceName, taskID, porttasks);
				boolean iret = invokeOneTask(taskID, serviceName, seq, queue, task, cho);				
 
				if(iret) {
					forward = true;
					
					PathCoverageEventInterface pcei = st.pcei;
					int length = seq.size();
					ArrayList<MyConstraint> tc = null;
					if(length>1) {
						String preceding = seq.get(length-2);//the last two
						tc = pcei.getCausalityCondition(preceding, taskID);
						AddConstraints(mc_list,tc);
						//mc_list.addAll(tc);
					}
					
					if(length == 1) {//start task
						String preceding = "START";
						tc = pcei.getCausalityCondition(preceding, taskID);
						//mc_list.addAll(tc);
						AddConstraints(mc_list,tc);
					}
										
				    constructPaths(observation, eventinterfaces, history, queue, cho, porttasks, mc_list);
				    
				    //backtracking
				    //1. remove task from seq
				    seq.remove(seq.size()-1);				    
				    
				    //2. if the execution of the task consumes messages, then restoring them
				    if(task!=null && !task.isSend()) { //receiving messages
				    	
				    	PortExpression pexp = task.getPortexp();
				    	ArrayList<PortVariable> pvs = pexp.getPortVariables();
				    	ArrayList<String> que = queue.get(serviceName);
				    	
				    	for(PortVariable pv: pvs) {
				    		que.add(pv.getName());
				    	}
				    }
				    
				    //3. if the execution of the task produces messages, then remove them
				    if(task!=null && task.isSend()) { //sending messages
				    	
				    	PortExpression pexp = task.getPortexp();
				    	ArrayList<PortVariable> pvs = pexp.getPortVariables();
				    	
				    	for(int i=pvs.size()-1;i>=0;i--) {//remove the message
				    		PortVariable pv = pvs.get(i);
				    		String vname = pv.getName();
				    		ArrayList<String> sn_list = getReceiver(cho, vname, serviceName);
							
				    		for(String sn: sn_list) {
								ArrayList<String> que = queue.get(sn);
								if(que!=null) {
									int index = que.lastIndexOf(vname);
									que.remove(index);
								}
							}
				    	}				    	
				    }
				    
				}
        									
			}
			
			if(!forward) {//deadlock or all the remaining tasks are not executable.
				if(isCompletePath(history, eventinterfaces)) {
					EventSequenceObservationUnit unit = new EventSequenceObservationUnit();
					unit.setSequence(history);
					unit.setConstraint(constraint);
				
					ArrayList<String> exposedEvent = getExposedEvents(eventinterfaces);
					unit.setExposedEvents(exposedEvent);
				
					observation.addObservationUnit(unit);	
				}
			}
		}				
	}

	private static boolean isCompletePath(
			HashMap<String, ArrayList<String>> history,
			ArrayList<PathCoverageEventInterface> eventinterfaces) {

		Set<String> snlist = history.keySet();
		for(String sn: snlist) {
			ArrayList<String> path = history.get(sn);

			if(path!=null && !path.isEmpty()) {
				String ltn = path.get(path.size()-1);
                if(!isEndTask(ltn, eventinterfaces, sn)) return false;				
			}
		}
		
		return true;
	}

	private static boolean isEndTask(String ltn,
			ArrayList<PathCoverageEventInterface> eventinterfaces, String sn) {

		for(PathCoverageEventInterface ei: eventinterfaces) {
			if(sn.equals(ei.getServiceName())) {
				HashMap<String, ArrayList<String>> orders = ei.getOrders();
				if(orders!=null) {
					ArrayList<String> endtasks = getEndTasks(orders);
					if(endtasks.contains(ltn)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

	private static void AddConstraints(ArrayList<MyConstraint> mc_list,
			ArrayList<MyConstraint> constraint) {
		
		for(MyConstraint mc: constraint) {
			mc_list.add(mc.backup());
		}
		
	}

	public static ArrayList<String> getExposedEvents(
			ArrayList<PathCoverageEventInterface> eventinterfaces) {
		
		ArrayList<String> result = new ArrayList<String>();
		for(PathCoverageEventInterface pcei: eventinterfaces) {
			String sn = pcei.getServiceName();
			String stn = pcei.getStartTask();
			result.add(sn+":"+stn);
			
			HashMap<String, ArrayList<String>> orders = pcei.getOrders();
			Set<String> keys = orders.keySet();
			for(String key: keys) {
				String ep = sn+":"+key;
				if(!result.contains(ep)) result.add(ep);
				ArrayList<String> list = orders.get(key);
				for(String tn: list) {
					ep = sn + ":" + tn;
					if(!result.contains(ep)) result.add(ep);
				}
			}			
		}
		
		return result;
	}

	/*
	 * Symbolic execution of one task
	 */
	private static boolean invokeOneTask(String taskID, 
			String serviceName, 
			ArrayList<String> seq,
			HashMap<String, ArrayList<String>> queue, 
			Task task, 
			HashMap<String, ArrayList<String>> cho) {
		
		if(task==null) { //non-port action
			seq.add(taskID);			
		    return true; 
		} 
		
		PortExpression pe = task.getPortexp();
		ArrayList<PortVariable> pvs = pe.getPortVariables();
			
		if(task.isSend()) {//send
				
			for(PortVariable pv: pvs) {
				String vname = pv.getName();
				ArrayList<String> sn_list = getReceiver(cho, vname, serviceName);
				for(String sn: sn_list) {
					ArrayList<String> que = queue.get(sn);
					if(que!=null) que.add(vname);
				}
			}
				
			seq.add(taskID);
			return true;
		
		} 
		
		//receive
		//check whether the required variables are available
		ArrayList<String> que = queue.get(serviceName);
		if(available(que,pvs)) { 
			seq.add(taskID);
			updateQueue(que, pvs);
			return true;
		}			
			
		return false;		
	}

	private static void updateQueue(ArrayList<String> que,
			ArrayList<PortVariable> pvs) {		
		for(PortVariable pv: pvs) {
			String vname = pv.getName();
			
			for(int i = 0; i<que.size();i++ ) {
				if(vname.equals(que.get(i))) {
					que.remove(i);
					break;
				}
			}
		}
	}

	private static boolean available(ArrayList<String> que,
			ArrayList<PortVariable> pvs) {
		
		ArrayList<String> bk = new ArrayList<String>();
		bk.addAll(que);
		
		for(PortVariable pv: pvs) {
			String vn = pv.getName();
			boolean found = false; 
			for(int i=0;i<bk.size();i++) {
				if(vn.equals(bk.get(i))) {
					found = true;
					bk.remove(i);
					break;
				}
			}
			
			if(!found) return false;						
		}
		
		return true;		
	}

	private static ArrayList<String> getReceiver(HashMap<String, ArrayList<String>> cho,
			String vname, String serviceName) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		Set<String> sn_list = cho.keySet();
		for(String sn: sn_list) {
		   if(serviceName.equals(sn)) continue;
		   ArrayList<String> pl = cho.get(sn);
		   if(pl.contains(vname)) result.add(sn);
		}
		
		return result;
	}

	private static Task getPortTask(String serviceName, String taskID,
			HashMap<String, ArrayList<Task>> porttasks) {
        ArrayList<Task> pts = porttasks.get(serviceName);
        if(pts==null) return null;
		
		for(Task task: pts) {			
			String tn = task.getName();
			if(taskID.equals(tn))
				return task;
		}
		
		return null;
	}

	private static ArrayList<SearchTask> getNextAvailableTasks(
			HashMap<String, ArrayList<String>> history,
			HashMap<String, ArrayList<String>> queue, 
			ArrayList<PathCoverageEventInterface> eventinterfaces, 
			HashMap<String, ArrayList<String>> cho, 
			HashMap<String, ArrayList<Task>> porttasks) {
		
		ArrayList<SearchTask> result = new ArrayList<SearchTask>();
		
		if(exceedMaxPathLength(history)) 
			return result;
		
		for(PathCoverageEventInterface pcei: eventinterfaces) {
			String serviceName = pcei.getServiceName();
			ArrayList<String> his = history.get(serviceName);						
			
			ArrayList<SearchTask> st = getNextAvailableTask(pcei, his);
			result.addAll(st);			
		}
		
		return result;
	}

	private static ArrayList<SearchTask> getNextAvailableTasks4CompletePath(
			HashMap<String, ArrayList<String>> history,
			HashMap<String, ArrayList<String>> queue, 
			ArrayList<PathCoverageEventInterface> eventinterfaces, 
			HashMap<String, ArrayList<String>> cho, 
			HashMap<String, ArrayList<Task>> porttasks) {
		
		ArrayList<SearchTask> result = new ArrayList<SearchTask>();
		
		boolean exceededMaxPathLength = exceedMaxPathLength(history); 
		
		for(PathCoverageEventInterface pcei: eventinterfaces) {
			String serviceName = pcei.getServiceName();
			ArrayList<String> his = history.get(serviceName);						
			
			ArrayList<SearchTask> st = getNextAvailableTask4CompletePath(pcei, his, exceededMaxPathLength);
			result.addAll(st);			
		}
		
		return result;
	}
	
	private static ArrayList<SearchTask> getNextAvailableTask4CompletePath(
			PathCoverageEventInterface pcei, ArrayList<String> his, boolean exceeded) {
		
		ArrayList<SearchTask> result = new ArrayList<SearchTask>();
		String serviceName = pcei.getServiceName();
		
		
		if(his.isEmpty()) { 
			String tID = pcei.getStartTask();
			SearchTask st = new SearchTask();
		    st.serviceName = serviceName;
		    st.taskID = tID;
		    st.pcei = pcei;
		    result.add(st);
		
		} else {
		
			ArrayList<String> tasks;
			HashMap<String, ArrayList<String>> orders = pcei.getOrders();
			ArrayList<String> tasks_1 = getNextofShortestPath(serviceName, his.get(his.size()-1), orders);
			ArrayList<String> tasks_2 = orders.get(his.get(his.size()-1));
			if(exceeded) 
				tasks = tasks_1;
			else
				tasks = tasks_2;
			
			
			if(tasks!=null) {
		
				for(String tID: tasks) {
					SearchTask st = new SearchTask();
					st.serviceName = serviceName;
					st.taskID = tID;
					st.pcei = pcei;
					result.add(st);
				}
			}
		}
		
		return result;
	}
	
	private static HashMap<String, HashMap<String, ArrayList<String>>> spbuffer = new HashMap<String, HashMap<String, ArrayList<String>>>();
	private static ArrayList<String> getNextofShortestPath(String sn, String tn,
			HashMap<String, ArrayList<String>> orders) {
		
		HashMap<String, ArrayList<String>> next = spbuffer.get(sn);
		if(next==null) {
			next = new HashMap<String, ArrayList<String>>();
			spbuffer.put(sn, next);
			
			getNextAvailableTSP(next, orders);
		}
		
		return next.get(tn);
	}

	private static void getNextAvailableTSP(
			HashMap<String, ArrayList<String>> next,
			HashMap<String, ArrayList<String>> orders) {
	
		ArrayList<String> endtasks = getEndTasks(orders);
		
		for(String et: endtasks) {
		   ArrayList<ArrayList<String>> sps = getShortestPaths(orders, et);
		   
		   for(ArrayList<String> path: sps) {
			   for(int i=path.size()-1;i>0;i--) {
				   String tks = path.get(i);
				   String tkn = path.get(i-1);
			   
				   ArrayList<String> nts = next.get(tks);
				   if(nts==null) {
					   nts = new ArrayList<String>();
					   next.put(tks, nts);
				   }
				   
				   if(!nts.contains(tkn)) nts.add(tkn);
			   }
		   }
		   
		   next.put(et, new ArrayList<String>());
		}
	}

	private static ArrayList<String> getEndTasks(
			HashMap<String, ArrayList<String>> orders) {
		
		Set<String> keys = orders.keySet();
		ArrayList<String> etasks = new ArrayList<String>();		
		
		for(String tn:keys) {
			ArrayList<String> ot = orders.get(tn);
			for(String item: ot) {
				if(!etasks.contains(item)) etasks.add(item);
			}
		}
		
		etasks.removeAll(keys);
		
		return etasks;
	}

	private static ArrayList<ArrayList<String>> getShortestPaths(
			HashMap<String, ArrayList<String>> orders, String et) {
		
		HashMap<String, ArrayList<String>> reverseOrders = getReverseOrders(orders);
		
		HashMap<String, ArrayList<ArrayList<String>>> buf = new HashMap<String, ArrayList<ArrayList<String>>>();
		
		ArrayList<ArrayList<String>> paths = new ArrayList<ArrayList<String>>(); 
		
		ArrayList<ArrayList<String>> queue = new ArrayList<ArrayList<String>>();
		ArrayList<String> visited = new ArrayList<String>();
		ArrayList<String> initpath = new ArrayList<String>();
		initpath.add(et);
		visited.add(et);
		queue.add(initpath);
		while(!queue.isEmpty()) {
			ArrayList<String> cp = queue.remove(0);
			
			String tl = cp.get(cp.size()-1);
			ArrayList<String> ts = reverseOrders.get(tl);
			
			if(ts==null || ts.isEmpty()) {
				paths.add(cp);
				
				cachePath(tl, cp, buf);
				continue;
			}
			

			boolean hasterminated = true;
			for(String tst:ts) {

				ArrayList<String> np = new ArrayList<String>();
			    np.addAll(cp);
			    np.add(tst);

				
				if(!visited.contains(tst)) {	
					
				    visited.add(tst);
				    queue.add(np);		
				    
				    hasterminated = false;
				} else 
					if(!cp.contains(tst) && hasNoLoop(tst, cp, buf)) {
						paths.add(np);
						cachePath(tst, np, buf);
						hasterminated = false;
					}
					
			}
			
			if(hasterminated) paths.add(cp);

		}
				
		return paths;
	}

	private static boolean hasNoLoop(String tst, ArrayList<String> cp,
			HashMap<String, ArrayList<ArrayList<String>>> buf) {
		if(cp.isEmpty()) return true;
		
		for(int i=cp.size()-1;i>=0;i--) {
			String tn = cp.get(i);
			
			ArrayList<ArrayList<String>> paths = buf.get(tn);
			if(paths!=null) {
				for(ArrayList<String> path: paths)
					if(path.contains(tst)) return false;
			}
		}
		
		return true;
	}

	private static void cachePath(String tl, ArrayList<String> cp,
			HashMap<String, ArrayList<ArrayList<String>>> buf) {
		
		ArrayList<ArrayList<String>> paths = buf.get(tl);
		if(paths==null) {
			paths = new ArrayList<ArrayList<String>>();
			buf.put(tl, paths);
		}
		
		paths.add(cp);
	}

	private static HashMap<String, ArrayList<String>> getReverseOrders(
			HashMap<String, ArrayList<String>> orders) {
		
		HashMap<String, ArrayList<String>> reverseOrders = new HashMap<String, ArrayList<String>>();
		
		Set<String> keys = orders.keySet();
		for(String key:keys) {
			ArrayList<String> nts = orders.get(key);
			
			if(nts!=null) {
				for(String nt: nts) {
					ArrayList<String> ro = reverseOrders.get(nt);
					if(ro==null) {
						ro = new ArrayList<String>();
						reverseOrders.put(nt, ro);
					}
					
					ro.add(key);
				}
			}
		}
		
		return reverseOrders;
	}

	private static ArrayList<SearchTask> getNextAvailableTask(
			PathCoverageEventInterface pcei, ArrayList<String> his) {
		
		ArrayList<SearchTask> result = new ArrayList<SearchTask>();
		String serviceName = pcei.getServiceName();
		
		
		if(his.isEmpty()) { 
			String tID = pcei.getStartTask();
			SearchTask st = new SearchTask();
		    st.serviceName = serviceName;
		    st.taskID = tID;
		    st.pcei = pcei;
		    result.add(st);
		
		} else {
		
			HashMap<String, ArrayList<String>> orders = pcei.getOrders();
			ArrayList<String> tasks = orders.get(his.get(his.size()-1));
			
			if(tasks!=null) {
		
				for(String tID: tasks) {
					SearchTask st = new SearchTask();
					st.serviceName = serviceName;
					st.taskID = tID;
					st.pcei = pcei;
					result.add(st);
				}
			}
		}
		
		return result;
	}
	
	private static boolean exceedMaxPathLength(
			HashMap<String, ArrayList<String>> history) {

		int pathlength = 0;
		Set<String> keys = history.keySet();
		for(String key: keys) {
			ArrayList<String> qe = history.get(key);
			if(qe!=null)
				pathlength += qe.size();
		}				
		
		return pathlength >= Config.getConfig().maxPathLength;
	}
	
	public ArrayList<ObservationUnit> getObservationUnit() {
		return this.non_observations;
	}
	
	public ArrayList<ObservationUnit> getCoveredObservationUnit() {
		return this.my_observations;
	}
	
	public static Observation constructStaticGlobalPathCoverageObservation(
			ArrayList<PathCoverageEventInterface> eventInterfaces, HashMap<String, ArrayList<String>> cho, HashMap<String,ArrayList<Task>> porttasks) {
		GlobalObservation observation = new GlobalObservation();
		ArrayList<String> exposedEvent = getExposedEvents(eventInterfaces);
		
		HashMap<String, ArrayList<ArrayList<String>>> mapping = new HashMap<String, ArrayList<ArrayList<String>>>(); 
		
		for(PathCoverageEventInterface ei: eventInterfaces) {
			
			ArrayList<ArrayList<String>> paths = generateLocalPaths(ei);
			String sn = ei.getServiceName();
			//if(sn.equals("client")) continue;
			
			if(sn.equals("EuropAssist")) {
				for(ArrayList<String> path: paths) {
					if(path.size()<=3) {
						HashMap<String, ArrayList<String>> history = new HashMap<String, ArrayList<String>>();
						history.put(sn, path);
						
						EventSequenceObservationUnit unit = new EventSequenceObservationUnit();
						unit.setSequence(history);								
						unit.setExposedEvents(exposedEvent);
						observation.addObservationUnit(unit);
						
						paths.remove(path);
						
						break;
					}
				}
			}
			
			mapping.put(sn, paths);
		}
		
		
		HashMap<String, ArrayList<String>> history = new HashMap<String, ArrayList<String>>();
		constructCombinationPaths(mapping, 0, observation, history, exposedEvent);
		
		ArrayList<ObservationUnit> invalidUnit = new ArrayList<ObservationUnit>();
		for(ObservationUnit unit: observation.non_observations) {
			HashMap<String, ArrayList<String>> sequence = ((EventSequenceObservationUnit)unit).getSequence();
		    if(!verifyPath(sequence, cho, porttasks)) invalidUnit.add(unit);
		}
		observation.non_observations.removeAll(invalidUnit);
		
		
		return observation;
	}
		
	
	private static void constructCombinationPaths(HashMap<String, ArrayList<ArrayList<String>>> mapping,
			int index, GlobalObservation observation, HashMap<String, ArrayList<String>> history,
			ArrayList<String> exposedEvent) {
		
		Set<String> keys = mapping.keySet();
		ArrayList<String> sn_list = new ArrayList<String>();
		sn_list.addAll(keys);
		
		if(index< keys.size()) {
		    String sn = sn_list.get(index);
		    ArrayList<ArrayList<String>> paths = mapping.get(sn);
			
			for(ArrayList<String> path: paths) {
				 history.put(sn, path);				 
				 constructCombinationPaths(mapping, index+1, observation, history, exposedEvent);
				 history.remove(sn);
			}
			
		} else {
			EventSequenceObservationUnit unit = new EventSequenceObservationUnit();
			unit.setSequence(history);								
			unit.setExposedEvents(exposedEvent);
			observation.addObservationUnit(unit);
			
		}
		
	}	

	public static Observation constructLocalPathCoverageObservation(
			ArrayList<PathCoverageEventInterface> eventInterfaces) {

		LocalObservation observation = new LocalObservation();
		HashMap<ObservationUnit, String> mapping = new HashMap<ObservationUnit, String>();
		
		ArrayList<String> exposedEvent = getExposedEvents(eventInterfaces);
		
		for(PathCoverageEventInterface ei: eventInterfaces) {
			
			ArrayList<ArrayList<String>> paths = generateLocalPaths(ei);
			String sn = ei.getServiceName();
			
			for(ArrayList<String> ph: paths) {
				HashMap<String, ArrayList<String>> history = new HashMap<String, ArrayList<String>>();
				history.put(sn, ph);
				
				EventSequenceObservationUnit unit = new EventSequenceObservationUnit();
				unit.setSequence(history);								
				unit.setExposedEvents(exposedEvent);
				
                mapping.put(unit, sn);
				observation.addObservationUnit(unit);
			}
		}
		
		observation.setMapping(mapping);
		return observation;
	}

	public static ArrayList<ArrayList<String>> generateLocalPaths(
			PathCoverageEventInterface ei) {

		String start_task = ei.getStartTask();
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ArrayList<String> path = new ArrayList<String>();
		path.add(start_task);
		generateLocalPaths(start_task, path, ei, result);
		return result;
	}

	private static void generateLocalPaths(
			String start_task, 
			ArrayList<String> path, 
			PathCoverageEventInterface ei, 
			ArrayList<ArrayList<String>> result) {
		
		//return if the length of the path has exceeded the maximum length
		if(path.size() >= Config.getConfig().maxPathLength) {
			ArrayList<String> newpath = new ArrayList<String>(path);			
			result.add(newpath);		
		
		} else {
			
			ArrayList<String> tasks = getNextLocalAvailableTasks(start_task, ei);
			if(tasks==null || tasks.size() == 0) {//no available next tasks
				ArrayList<String> newpath = new ArrayList<String>(path);				
				result.add(newpath);
		
			} else {
				//construct paths for every next task
				for(String tk: tasks) {										
					path.add(tk);
					generateLocalPaths(tk, path, ei, result);
					
					//backtrack
					int index = path.size()-1;
					path.remove(index);
				}
			}
		
		}						
	}

	private static ArrayList<String> getNextLocalAvailableTasks(
			String tn, PathCoverageEventInterface ei) {
		
		HashMap<String, ArrayList<String>> orders = ei.getOrders();		
		
		return orders.get(tn);
	}

	public void setExposedPercentage(double exposedPercentage) {
		
		int size = non_observations.size();
		int num = (int)(size * exposedPercentage);
		int index = 0;	
		//Random rd = new Random(System.currentTimeMillis());
		
		int previous = 0;
		int current = 0;
		
		for(ObservationUnit unit: non_observations) {
			unit.setExposedPercentage(exposedPercentage);
			index++;
			current = (int)(index * exposedPercentage);
			//index = rd.nextInt(size); 
			if(unit instanceof DUObservationUnit && index >= num)
				((DUObservationUnit)unit).setExposed(false);
			/*if(unit instanceof DUObservationUnit) {
				if(current >= previous) {
				    ((DUObservationUnit)unit).setExposed(true);
				    previous++;
			    } else
				    ((DUObservationUnit)unit).setExposed(false);
			}*/
		}
				
	}
}

class SearchTask {
	public PathCoverageEventInterface pcei;
	public String serviceName;
	public String taskID;
}

//used to cache the task information for verifying paths
class VCTask {
	public String serviceName;
	public String taskID;
	public Task task;
	public int cp; 
}