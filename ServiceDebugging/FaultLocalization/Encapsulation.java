package ServiceDebugging.FaultLocalization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import engine.DataField;
import engine.DataType;
import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;
import engine.Event.EventType;
import engine.Event.TaskCompleteEvent;
import engine.Event.TransitionFiringEvent;
import engine.Event.TransitionNotFiringEvent;
import engine.Event.VariableEvent;
import engine.Queue.Message;
import Jama.Matrix;
import Service.AbstractService;
import Service.Task;
import Service.Transition;
import Service.Variable;
import ServiceDebugging.ServiceDebugging;
import Utils.MathUtils;

public class Encapsulation implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4243910902492856545L;

	//location of seeded faults
	//servicename:taskname/servicename:transitionname
	private ArrayList<String> mutationLocations = new ArrayList<String>();

	private ArrayList<ExecutionRecord> instances = new ArrayList<ExecutionRecord>();
	
	/*
	 * Cache the control flow information of each task for event encapsulation
	 * key: servicename_taskname
	 */
	private HashMap<String, String> controlFlowInfo = new HashMap<String, String>();
	private HashMap<String, String> dominateInfo = new HashMap<String, String>();
	
	//used to cache the port tasks of the services
	private HashMap<String, ArrayList<String>> port_tasks = null;
	
	//used to cache the distance between every two tasks/transitions in a service
	//key: servicename:Task:taskname/Transition:transitionname, 
	private HashMap<String, Integer> distances = null;
	public int maxGlobalPath = 0;
	
	private FaultLocalizationSolution m_solution;

	//used to cache the previous events of one event for SD
	private HashMap<String, ArrayList<String>> prebuf = new HashMap<String, ArrayList<String>>();
	
	//used to cache event prob 
	//private HashMap<Integer, HashMap<Integer, Double>> eventprobs = null;
	//private HashMap<String, Integer> eventcontents = null;
	
	public void snapshotServiceInstance(ArrayList<AbstractService> service_list) {
				
		
		if(distances == null && port_tasks == null) {
						
			distances = new HashMap<String, Integer>();
			port_tasks = new HashMap<String, ArrayList<String>>();
			
			for(AbstractService srv: service_list) {
				String sn = srv.getName();
				ArrayList<String> ptasks = getServicePortTasks(srv);
				port_tasks.put(sn, ptasks);
				
				maxGlobalPath += calculateTaskDistances(srv, distances);
				
				cacheControlFlowInformationNew(controlFlowInfo, srv);
				//cacheDominateInformation(dominateInfo, srv);
				cacheDominateNew(dominateInfo, srv);
			}			
			
		}				
	}	

	private void cacheDominateNew(HashMap<String, String> dmInfo, AbstractService srv) {
		ArrayList<Task> tasks = srv.getTasks();
		ArrayList<Transition> transitions = srv.getTransitions();
		String[] tokens = new String[tasks.size() + transitions.size()];
		
		//reverse postorder traversal of the service
		tokens[0] = srv.getStartTask();
		for(int i=1;i<tokens.length;i++) tokens[i]=null;
		reversePostOrderNew(srv, tokens, 0);
		
		int size = tokens.length;
		boolean[][] graph = new boolean[size][size];
		for(int i=0;i<size;i++)
			for(int j=0;j<size;j++)
				graph[i][j] = false;
		
		for(Transition tr:transitions) {
			String source = tr.getSource();
			String sink = tr.getSink();
			String trname = tr.getName();
			int sourceindex = getTaskIndex(tokens, source);
			int sinkindex = getTaskIndex(tokens, sink);
			int trindex = getTaskIndex(tokens, trname);
			graph[sourceindex][trindex] = true;
			graph[trindex][sinkindex] = true;
		}
		
		int[] doms = new int[tokens.length];
		for(int i=0;i<doms.length;i++) doms[i] = -1;
		doms[0] = 0;
		boolean changed = true;
		while(changed) {
			changed = false;
			for(int i=1;i<doms.length;i++) {				
				int tmpdom = -1;
				for(int j=0;j<tokens.length;j++) {
					 if(graph[j][i] && doms[j]>=0) {					 
						     if(tmpdom<0) 
							    tmpdom = j;
						     else 
							    tmpdom = intersect(tmpdom, j, doms);
					 }
				}
				
				if(doms[i]!=tmpdom) {
					doms[i] = tmpdom;
					changed = true;
				}
			}
		}
		
		//cache the data
		String sn = srv.getName();
		for(int i=0;i<tokens.length;i++) {
			String key = sn+"_"+tokens[i];
			String value = tokens[doms[i]];
			dmInfo.put(key, value);
		}
	}
	
	private void cacheDominate(HashMap<String, String> dmInfo, AbstractService srv) {
		ArrayList<Task> tasks = srv.getTasks();
		String[] taskIndexs = new String[tasks.size()];
		//reverse postorder traversal of the service
		taskIndexs[0] = srv.getStartTask();
		for(int i=1;i<taskIndexs.length;i++) taskIndexs[i]=null;
		reversePostOrder(srv, taskIndexs, 0);
		
		int[] doms = new int[taskIndexs.length];
		for(int i=0;i<doms.length;i++) doms[i] = -1;
		doms[0] = 0;
		boolean changed = true;
		while(changed) {
			changed = false;
			for(int i=1;i<doms.length;i++) {
				String cn = taskIndexs[i];
				ArrayList<String> preds = getIncomingTasks(srv, cn);
				int tmpdom = -1;
				for(String pn: preds) {
					 int pnIndex = getTaskIndex(taskIndexs, pn);
					 if(doms[pnIndex]>=0)
						 if(tmpdom<0) 
							 tmpdom = pnIndex;
						 else 
							 tmpdom = intersect(tmpdom, pnIndex, doms);
				}
				
				if(doms[i]!=tmpdom) {
					doms[i] = tmpdom;
					changed = true;
				}
			}
		}
		
		//cache the data
		String sn = srv.getName();
		for(int i=0;i<taskIndexs.length;i++) {
			String key = sn+"_"+taskIndexs[i];
			String value = taskIndexs[doms[i]];
			dmInfo.put(key, value);
		}
	}

	private int intersect(int tmpdom, int i, int[] doms) {
		if(tmpdom<0) return i;
		if(i<0) return tmpdom;
		while(tmpdom!=i) {
			while(tmpdom>i) 
				tmpdom = doms[tmpdom];
			while(i>tmpdom)
				i = doms[i];
		}
			
		return i;
	}

	private int getTaskIndex(String[] taskIndexs, String pn) {
		for(int i=0;i<taskIndexs.length;i++)
			if(pn.equals(taskIndexs[i])) return i;
		return -1;
	}
	
	private int reversePostOrderNew(AbstractService srv, String[] taskIndexs,
			int i) {
		
		String currentTask = taskIndexs[i];
		ArrayList<Transition> transitions = srv.getAvailableNextTransitions(currentTask);
		int num = transitions.size();
		for(int index=num-1;index>=0;index--) {
			Transition tr = transitions.get(index);
			i++;
			taskIndexs[i] = tr.getName();
			String tn = tr.getSink();
			if(!IsVisitedTask(tn, taskIndexs, i)) {
				i++;
				taskIndexs[i] = tn;
				i = reversePostOrderNew(srv, taskIndexs, i);
			}
		}
		
		return i;
	}
	
	private int reversePostOrder(AbstractService srv, String[] taskIndexs,
			int i) {
		
		String currentTask = taskIndexs[i];
		ArrayList<Transition> transitions = srv.getAvailableNextTransitions(currentTask);
		int num = transitions.size();
		for(int index=num-1;index>=0;index--) {
			Transition tr = transitions.get(index);
			String tn = tr.getSink();
			if(!IsVisitedTask(tn, taskIndexs, i)) {
				i++;
				taskIndexs[i] = tn;
				i = reversePostOrder(srv, taskIndexs, i);
			}
		}
		
		return i;
	}

	private boolean IsVisitedTask(String tn, String[] taskIndexs, int i) {
		for(int index=0;index<=i;index++)
			if(tn.equals(taskIndexs[index])) return true;
		
		return false;
	}


	private ArrayList<String> getIncomingTasks(AbstractService srv, String tn) {
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<Transition> transitions = srv.getTransitions();
		for(Transition tr: transitions) {
			String sink = tr.getSink();
			if(tn.equals(sink)) {
				String source = tr.getSource();
				if(!result.contains(source)) result.add(source);
			}
		}
		return result;
	}
	
	private void cacheControlFlowInformationNew(
			HashMap<String, String> idoms, AbstractService srv) {
		HashMap<String, Integer> cfmap = new HashMap<String, Integer>();
		String srvname = srv.getName();
		ArrayList<Transition> trs = srv.getTransitions();
		for(Transition tr: trs) {
			String source = tr.getSource();
			String sink = tr.getSink();
			String trname = tr.getName();
			
			String key = srvname + "_" + trname; 
			cfmap.put(key, 4);
			
			key = srvname + "_"  + source;
			Integer value = cfmap.get(key);
			if(value==null) 
				value = 3;
			else
				if(value<=5) value+=3;
			cfmap.put(key, value);
			
			key = srvname + "_" + sink;
			value = cfmap.get(key);
			if(value==null) 
				value = 1;
			else {
				int tmp = value - value/3 * 3;
				if(tmp<=1) tmp++;
				value = value/3 * 3 + tmp;
			}
			cfmap.put(key, value);
		}
		
		for(Transition tr: trs) {
			String source = tr.getSource();
			String sink = tr.getSink();
			String trname = tr.getName();
			
			String key = srvname + "_" + source; 
			int value = cfmap.get(key);
			if(value<=5) //single out
				idoms.put(srvname+"_"+trname, source);
			
			key=srvname+"_"+sink;
			value=cfmap.get(key);
			value=value - value/3*3;
			if(value<=1) //single in
				idoms.put(srvname+"_"+sink, trname);
		}
	}


	public void setDistances(HashMap<String, Integer> dists) {
		distances = dists;
	}
	
	public void addMutation(String mutationToken) {
		mutationLocations.add(mutationToken);
	}
	
	private int calculateTaskDistances(AbstractService srv,
			HashMap<String, Integer> dist) {
         
		String sn = srv.getName();
		
		HashMap<String, Integer> tnIndex = new HashMap<String, Integer>();
		ArrayList<String> tnIndex_reverse = new ArrayList<String>();
		
		ArrayList<Transition> tr_list = srv.getTransitions();		
		int tokenIndex=0;
		for(Transition tr:tr_list) {
			String tn = "Transition:"+tr.getName();
			tnIndex.put(tn, tokenIndex);
			tnIndex_reverse.add(tn);
			tokenIndex++;
		}
		
		
		ArrayList<Task> tasks = srv.getTasks();		
		for(Task tk: tasks) {		
			String tn = "Task:"+tk.getName();
			tnIndex.put(tn, tokenIndex);
			tnIndex_reverse.add(tn);
			tokenIndex++;
		}
				
		
		int[][] buffer = new int[tokenIndex][tokenIndex];
		for(int i=0;i<buffer.length;i++)
			buffer[i][i] = 0;
		
		for(int i=0;i<buffer.length;i++)
			for(int j=0;j<buffer.length;j++)
				if(i!=j) buffer[i][j] = 10000;
		
		for(Transition tr: tr_list) {
		    String source = tr.getSource();
		    String sink = tr.getSink();
		    
		    int sourceIndex = tnIndex.get("Task:"+source);
		    int sinkIndex = tnIndex.get("Task:"+sink);
		    int tr_Index = tnIndex.get("Transition:" + tr.getName());
		    
		    buffer[sourceIndex][sinkIndex] = 2;
		    buffer[sinkIndex][sourceIndex] = 2;
		    buffer[sourceIndex][tr_Index]=1;
		    buffer[tr_Index][sourceIndex]=1;
		    buffer[sinkIndex][tr_Index]=1;
		    buffer[tr_Index][sinkIndex]=1;
		}
		
		//calculate the distance based on data-flow dependency
		
		
		//Floyd algorithm
		for(int count = 0;count<buffer.length;count++)
		for(int i=0;i<buffer.length;i++)
			for(int j=0;j<buffer.length;j++)
				for(int k=0;k<buffer.length;k++) { 
					if(k==i || k == j) continue;
				    int tmp = buffer[i][k] + buffer[k][j];
				    if(tmp < buffer[i][j]) 
					   buffer[i][j] =  tmp;
				}						
		
		
		//generate output
		int max=-1;
		for(int i=0;i<buffer.length;i++)
			for(int j=0;j<buffer.length;j++) {
				String key = sn + ":" + tnIndex_reverse.get(i) + ":" + tnIndex_reverse.get(j);
				dist.put(key, buffer[i][j]);
				
				if(max<buffer[i][j] && buffer[i][j]<10000) max = buffer[i][j];
			}
				
		
		return max;
	}

	private ArrayList<String> getServicePortTasks(AbstractService srv) {

		ArrayList<String> result = new ArrayList<String>();
		
		ArrayList<Task> tk_list = srv.getTasks();
		for(Task tk: tk_list) {
			if(tk.isPort()) result.add(tk.getName());
		}
		
		return result;
	}

	private int numPassed = 0;
	private int numFailed = 0;
	
	public int getNumofPassed() {
		return numPassed;
	}
	
	public int getNumofFailed() {
		return numFailed;
	}
	
	public void addInstanceRecord(ExecutionRecord record) {				
		instances.add(record);
		
		if(record.isPassed()) 
			numPassed++;
		else
			numFailed++;
	}
	
	/*
	 * Policy: 0, 3 expose all
	 *         1 port only
	 *         2 encapsulation
	 */
	public ProbeEvent[] exposeEvents(ExecutionRecord record) {
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
						
		
		ArrayList<String> sn_list = record.getAllServiceNames();
		for(String sn: sn_list) {
			//filter out the client events
			if(sn.endsWith("Client")) continue;

                        //filter out the buyer2-10 events due to the memory limit
                        //if(sn.equals("Buyer2")||
            if(sn.equals("Buyer2")||sn.equals("Buyer3")||sn.equals("Buyer4")||sn.equals("Buyer5")||
               sn.equals("Buyer6")||sn.equals("Buyer7")||sn.equals("Buyer8")||sn.equals("Buyer9")||sn.equals("Buyer10")) continue;
			
			//filter data_read events in the transitions that are not fired
			//filterNotFiredTranstionDataReadEvents(sn, record);
			
			ArrayList<ProbeEvent> te =exposeEvents4Service(sn, record); 
		    if(te!=null) result.addAll(te);
		}				
		
		//to calculate information leakage 
		//if(m_solution.exposePolicy == FaultLocalizationSolution.EXPOSEALL || m_solution.exposePolicy == FaultLocalizationSolution.EXPOSEALLWITHSD) {
		//	calculateEventProbAdvanced(result);
		//}
		
		ProbeEvent[] ret = new ProbeEvent[result.size()];
		result.toArray(ret);
		return ret;
	}

	private void filterNotFiredTranstionDataReadEvents(String sn,
			ExecutionRecord record) {

		ArrayList<Event> eventlist = record.getServiceEvents(sn);
		
		ArrayList<Event> filter = new ArrayList<Event>();
		
		for(Event evt: eventlist) {
			if(evt instanceof DataReadEvent) {
				 String loc = ((DataReadEvent) evt).getVariableLoc();
				 if(loc.startsWith("Transition:")) {
					 int start = loc.indexOf(':')+1;
					 int end = loc.indexOf('_');
					 String trn = loc.substring(start, end);
					 
					 if(transitionNotFired(trn, eventlist)) 
						 filter.add(evt);
				 }
				 
			}
		}
		
		eventlist.removeAll(filter);
		
	}

	private boolean transitionNotFired(String trn, ArrayList<Event> eventlist) {
		
		for(Event evt: eventlist) {
			if(evt instanceof TransitionFiringEvent) {
				 String name = ((TransitionFiringEvent) evt).getTransitionName();
				 if(name.equals(trn)) return false;
			}
		}
		
		return true;
	}

	private ArrayList<ProbeEvent> exposeEvents4Service(
			String serv, ExecutionRecord record) {
		
		switch(m_solution.exposePolicy) {
		case FaultLocalizationSolution.EXPOSEALL: //expose all events
			return exposeAllEvents4Service(serv, record);
		case FaultLocalizationSolution.EXPOSEPORTONLY: //expose port events only
			return exposePortEvents4Service(serv, record);
		case FaultLocalizationSolution.EXPOSEENCAPSULATION: //expose encapsulated events, at the beginning, 
			    //all events between two port actions are encapsulated
			return exposeEncapsulateEvents4Service(serv, record);
		case FaultLocalizationSolution.EXPOSEENCAPSULATIONNEW: //expose encapsulated events, at the beginning, 
		    //all events between two port actions are encapsulated
		     return exposeEncapsulateEvents4ServiceNew(serv, record,0);	
		case FaultLocalizationSolution.EXPOSEENCAPSULATIONFOLD: //expose encapsulated events, at the beginning, 
		    //all events between two port actions are encapsulated,data-flow folding
		     return exposeEncapsulateEvents4ServiceNew(serv, record,1);	     
		case FaultLocalizationSolution.EXPOSEALLWITHSD:	
			return exposeAllEvents4ServiceSD(serv, record);
		}
		
		return null;
 
	}

	private String getEventToken(Event evt) {
		
		if(evt instanceof TaskCompleteEvent) 
			return "Task:"+((TaskCompleteEvent)evt).getTaskName();
		
		if(evt instanceof TransitionFiringEvent)
			return "Transition:"+((TransitionFiringEvent)evt).getTransitionName();
		
		if(evt instanceof TransitionNotFiringEvent)
			return "Transition:"+((TransitionNotFiringEvent)evt).getTransitionName();
		
		if(evt instanceof VariableEvent) {
			String vLoc = ((VariableEvent)evt).getVariableLoc();
			int start = 0;
			int end = vLoc.indexOf('_');
			return vLoc.substring(start, end);
		}
		
		return null;
	}
	
	private ArrayList<ProbeEvent> exposeEncapsulateEvents4ServiceNew(String serv,
			ExecutionRecord record, int datafield_option) {

		ArrayList<ProbeEvent> pe_list = new ArrayList<ProbeEvent>();
		ArrayList<Event> event_list = record.getServiceEvents(serv);
		
		//cluster the events based on task or transition, 
		//these clusters are then used in the encapsulation
		HashMap<String, ArrayList<Event>> buffer = new HashMap<String, ArrayList<Event>>();
		ArrayList<String> tokens = new ArrayList<String>();
		for(Event evt: event_list) {
			String uID = getEventToken(evt);
			ArrayList<Event> elist = buffer.get(uID);
			if(elist==null) {
				elist = new ArrayList<Event>();
				buffer.put(uID, elist);
				tokens.add(uID);
			}			
			elist.add(evt);
		}
		
		ArrayList<String> needEncapsulate = new ArrayList<String>();
		//boolean metPort = false;
		for(String uID: tokens) {
/*			
			ArrayList<Event> elist = buffer.get(uID);
			Event evt = elist.get(0);
			if(isPortEvent(evt)) {
				metPort = true;
				needEncapsulate.add(uID);
			} else {
				if(metPort) {			
					pe_list.addAll(EncapsulateProbeEventNew.encapsulateEvents(dominateInfo, needEncapsulate, serv, record.getInstanceID(), m_solution.refinementPolicy, buffer));
					needEncapsulate.clear();
					metPort = false;
				} 
				needEncapsulate.add(uID);
			}	*/
			
			ArrayList<Event> elist = buffer.get(uID);
			Event evt = elist.get(0);
			if(isPortEvent(evt)) {
			    if(!needEncapsulate.isEmpty()) {										
				    pe_list.addAll(EncapsulateProbeEventNew.encapsulateEvents(dominateInfo, needEncapsulate, serv, record.getInstanceID(), m_solution.refinementPolicy, buffer, datafield_option));
				    needEncapsulate.clear();
				}
				
			    for(Event et: elist) {
		    	    ProbeEvent pe = new ProbeEvent(et, record.getInstanceID());
		    	    pe_list.add(pe);
		    	}	
				
			} else 				
				needEncapsulate.add(uID);
		}

		//encapsulate the rest of events
		if(!needEncapsulate.isEmpty()) {
			pe_list.addAll(EncapsulateProbeEventNew.encapsulateEvents(dominateInfo, needEncapsulate, serv, record.getInstanceID(), m_solution.refinementPolicy, buffer,datafield_option));
		}					
		
		return pe_list;
	}
	
	private ArrayList<ProbeEvent> exposeEncapsulateEvents4Service(String serv,
			ExecutionRecord record) {

		ArrayList<ProbeEvent> pe_list = new ArrayList<ProbeEvent>();
		ArrayList<Event> event_list = record.getServiceEvents(serv);
		
		//cluster the events based on task or transition, 
		//these clusters are then used in the encapsulation
		HashMap<String, ArrayList<Event>> buffer = new HashMap<String, ArrayList<Event>>();
		ArrayList<String> tokens = new ArrayList<String>();
		for(Event evt: event_list) {
			String uID = getEventToken(evt);
			ArrayList<Event> elist = buffer.get(uID);
			if(elist==null) {
				elist = new ArrayList<Event>();
				buffer.put(uID, elist);
				tokens.add(uID);
			}
			
			elist.add(evt);
		}
		
		ArrayList<String> needEncapsulate = new ArrayList<String>();
		//String sourcePort = "BEGIN";//for the beginning
		//String sinkPort = "BEGIN";
		for(String uID: tokens) {
			ArrayList<Event> elist = buffer.get(uID);
			Event evt = elist.get(0);
			if(isPortEvent(evt)) {
				//sourcePort = sinkPort;
				//sinkPort = uID;
				
				//expose the events related to the port action
				for(Event et: elist) {
					ProbeEvent pe = new ProbeEvent(et, record.getInstanceID());
					pe_list.add(pe);
				}
				
				//encapsulate the events
				if(!needEncapsulate.isEmpty()) {					
					pe_list.addAll(EncapsulateProbeEvent.generateEncapsulateEvents(controlFlowInfo, serv, needEncapsulate, record.getInstanceID(), buffer, m_solution.needFinedEncapsulation, m_solution.refinementPolicy, m_solution.improvedEncapsulation));
					needEncapsulate.clear();
				}
			
			} else {		
				needEncapsulate.add(uID);
			}
				
		}

		//encapsulate the rest of events
		if(!needEncapsulate.isEmpty()) {
			pe_list.addAll(EncapsulateProbeEvent.generateEncapsulateEvents(controlFlowInfo, serv, needEncapsulate, record.getInstanceID(), buffer, m_solution.needFinedEncapsulation, m_solution.refinementPolicy, m_solution.improvedEncapsulation));
		}					
		
		return pe_list;
	}

	private ArrayList<ProbeEvent> exposePortEvents4Service(String serv,
			ExecutionRecord record) {

		ArrayList<ProbeEvent> pe_list = new ArrayList<ProbeEvent>();

		ArrayList<Event> event_list = record.getServiceEvents(serv);
		for(Event evt: event_list) {		
			if(isPortEvent(evt)) {
				//double dfd = calculateDFDistance(evt, event_list, serv);
				ProbeEvent pe = new ProbeEvent(evt, record.getInstanceID());
				//pe.setDistance(dfd);
				pe_list.add(pe);
			}
		}
		
		return pe_list;
	}
	

	private ArrayList<ProbeEvent> exposeAllEvents4Service(String serv,
			ExecutionRecord record) {
		
		ArrayList<ProbeEvent> pe_list = new ArrayList<ProbeEvent>();

		ArrayList<Event> event_list = record.getServiceEvents(serv);
				
		for(Event evt: event_list) {						
			
			ArrayList<Event> drelist = null;
			//(comment at 15.12.05) if(evt instanceof TransitionFiringEvent) drelist = findTransitionDataReadEventList(evt, event_list); 
			ProbeEvent pe = new ProbeEvent(evt, record.getInstanceID(), drelist); 
			String eID = pe.getEventID();
			if(eID==null) continue;
						
			/*
			double dfd = calculateDFDistance(evt, event_list, serv);
			if(drelist!=null) {				
				for(Event e: drelist) {
					 double td = calculateDFDistance(e, event_list, serv);
					 if(dfd > td) dfd = td;
				}				
			}
			pe.setDistance(dfd);*/
			
			pe_list.add(pe);			
		}
		
		return pe_list;
	}
	
	/**
	 * Expose events for statistical debugging
	 * Need to generate events for the fundamental predicates in SD
	 * @param serv
	 * @param record
	 * @return
	 */
	private ArrayList<ProbeEvent> exposeAllEvents4ServiceSD(String serv,
			ExecutionRecord record) {		
		
		ArrayList<ProbeEvent> pe_list = new ArrayList<ProbeEvent>();
		ArrayList<Event> event_list = record.getServiceEvents(serv);
		
		//the map to save the latest value of variables for generating scalar pair events
		HashMap<String, Variable> vmap = new HashMap<String, Variable>();
		
		for(Event evt: event_list) {						
			
			//assignment event or receive event, generate all scalar pair events
			if(evt instanceof DataModificationEvent) {
				String vname = ((DataModificationEvent) evt).getVariableName();
				String vtype = ((DataModificationEvent) evt).getVariableType();
				Object vvalue = ((DataModificationEvent) evt).getVariableUpdateNewValue();
				Variable vb = vmap.get(vname);
				Variable vb_old=null;
				if(vb==null) {
					vb = new Variable();
					vb.setName(vname);
					vb.setType(vtype);					
					vmap.put(vname, vb);
				} else {//added on 2016.03.06
					vb_old = new Variable();
					vb_old.setName(vname);
					vb_old.setType(vtype);
					vb_old.setValue(((DataModificationEvent) evt).getVariableUpdateOldValue());
				}
				vb.setValue(vvalue);
				
				//construct all the scalar pair events
				Set<String> keys = vmap.keySet();
				for(String key: keys) {
					if(!key.equals(vname)) {
						Variable cp_vb = vmap.get(key);
						String cp_vtype = cp_vb.getType();
						if(cp_vtype.equals(vtype)) {
							ArrayList<ProbeEvent> cp_elist = generateScalarPairPB(evt, vb, cp_vb); 
							if(cp_elist!=null) {
								pe_list.addAll(cp_elist);
							}
						}
					} else { //compare its old value and new value (2016.03.06)
						if(vb_old!=null) {
							ArrayList<ProbeEvent> cp_elist = generateScalarPairPB(evt, vb, vb_old); 
							if(cp_elist!=null) {
								pe_list.addAll(cp_elist);
							}
						}
					}
				}
				
			} else
				
			if(evt instanceof DataReadEvent) {//filter all data read events	
			   //update the variable maps if the variable is first read
				String vname = ((DataReadEvent) evt).getVariableName();
				String vtype = ((DataReadEvent) evt).getVariableType();
				Object vvalue = ((DataReadEvent) evt).getVariableReadValue();
				Variable vb = vmap.get(vname);
				if(vb==null) {
					vb = new Variable();
					vb.setName(vname);
					vb.setType(vtype);					
					vmap.put(vname, vb);
				} 				
				vb.setValue(vvalue);
				
			} else 
			
			if(evt instanceof TransitionFiringEvent) { //transition firing events
				ProbeEvent pe = new ProbeEvent();
				String transitionName = ((TransitionFiringEvent) evt).getTransitionName();
				pe.serviceName = evt.getServiceName();
				pe.eventToken = "Transition:" + transitionName;				
				pe.eventID = evt.getServiceName() + "_transition_" + transitionName;
				pe.instanceID = evt.getInstanceID();
				
				DataField df = new DataField();
				df.setName("predicate");
				df.setType(DataType.BOOLEAN);
				df.setValue(true);
				pe.fields.add(df);
				
				//double dfd = calculateDFDistance(evt, event_list, serv);
				//pe.setDistance(dfd);	
				pe_list.add(pe);
				
			} else
				
			if(evt instanceof TransitionNotFiringEvent) { //transition not firing event
				ProbeEvent pe = new ProbeEvent();
				String transitionName = ((TransitionNotFiringEvent) evt).getTransitionName();
				pe.serviceName = evt.getServiceName();
				pe.eventToken = "Transition:" + transitionName;				
				pe.eventID = evt.getServiceName() + "_transition_" + transitionName;
				pe.instanceID = evt.getInstanceID();
				
				DataField df = new DataField();
				df.setName("predicate");
				df.setType(DataType.BOOLEAN);
				df.setValue(false);
				pe.fields.add(df);
				
				//double dfd = calculateDFDistance(evt, event_list, serv);
				//pe.setDistance(dfd);	
				pe_list.add(pe);	
				
			} else
				
			if(evt instanceof TaskCompleteEvent) { //taskComplete events 
			
				ProbeEvent pe = new ProbeEvent(); 			
				String taskName = ((TaskCompleteEvent)evt).getTaskName();
				pe.serviceName = evt.getServiceName();
				pe.eventToken = "Task:" + taskName;			
				pe.eventID = evt.getServiceName() + "_task_" + taskName;
				pe.instanceID = evt.getInstanceID();
				
				DataField df = new DataField();
				df.setName("predicate");
				df.setType(DataType.BOOLEAN);
				df.setValue(true);
				pe.fields.add(df);
				
				//double dfd = calculateDFDistance(evt, event_list, serv);
			    //pe.setDistance(dfd);			
			    pe_list.add(pe);
			
			}
		}
		
		return pe_list;
	}	
	
	private ArrayList<ProbeEvent> generateScalarPairPB(Event evt, Variable vb,
			Variable cp_vb) {
		
		String type = vb.getType();
		if(type.equals(DataType.BOOLEAN)) return generateDSValueScalarPairPB(evt, vb, cp_vb);
		
		if(type.equals(DataType.INTEGER)) return generateDecimalScalarPairPB(evt, vb, cp_vb);
		
		if(type.equals(DataType.STRING)) return generateDSValueScalarPairPB(evt, vb, cp_vb);
		
		return null;
	}

	private ArrayList<ProbeEvent> generateDecimalScalarPairPB(Event evt,
			Variable vb, Variable cp_vb) {

		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		
		Object value1 = vb.getValue();
		Object value2 = cp_vb.getValue();
			
		if(value1==null && value2==null) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<", false);
			result.add(pe);			
			pe = generateSPEvent(evt, vb, cp_vb, "!=", false);
			result.add(pe);			
			return result;
		}
		
		if((value1==null && value2!=null) ||
		   (value1!=null && value2==null)) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "!=", true);
			result.add(pe);		
			pe = generateSPEvent(evt, vb, cp_vb, "=", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">=", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<=", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<", false);
			result.add(pe);
			return result;
		}
		
		if(value1.equals(value2)) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<", false);
			result.add(pe);			
			pe = generateSPEvent(evt, vb, cp_vb, "!=", false);
			result.add(pe);			
			return result;
		}
		
		Integer iv1 = (Integer) value1;
		Integer iv2 = (Integer) value2;
		if(iv1>iv2) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, ">", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "!=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<=", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "=", false);
			result.add(pe);
			return result;
		}
			
		if(iv1<iv2) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "<", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "<=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "!=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, ">=", false);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "=", false);
			result.add(pe);
			return result;
		}
		
		return result;
	}

	private ProbeEvent generateSPEvent(Event evt, Variable vb, Variable cp_vb,
			String op, boolean value) {

		String vname1 = vb.getName();
		String vname2 = cp_vb.getName();
		String name =  vname1 + op + vname2;
		
		ProbeEvent pe = new ProbeEvent();
		
		pe.serviceName = evt.getServiceName();
		pe.instanceID = evt.getInstanceID();		
		String vloc = ((DataModificationEvent) evt).getVariableLoc();	
		//for debugging
		if(vloc.equals("Task:t")) {
			System.out.println("ServiceInstance:" + pe.instanceID);
			System.out.println("ServiceName:" + pe.serviceName);
			System.out.println("vname:" + name);
			evt.printMessage();
		}
		
		pe.eventToken = extractLoc(vloc);		
		pe.eventID = pe.serviceName + "_" + vloc + "_scalarpair_" + name;	 				
						
		DataField df = new DataField();
		df.setName("predicate");
		df.setType(DataType.BOOLEAN);
		df.setValue(value);
		pe.fields.add(df);
		
		return pe;
	}

	private ArrayList<ProbeEvent> generateDSValueScalarPairPB(Event evt,
			Variable vb, Variable cp_vb) {
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		
		Object value1 = vb.getValue();
		Object value2 = cp_vb.getValue();
			
		if(value1==null && value2==null) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "!=", false);
			result.add(pe);
			return result;
		}
		
		if((value1==null && value2!=null) ||
		   (value1!=null && value2==null)) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "!=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "=", false);
			result.add(pe);
			return result;
		}
		
		if(value1.equals(value2)) {
			ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "=", true);
			result.add(pe);
			pe = generateSPEvent(evt, vb, cp_vb, "!=", false);
			result.add(pe);
			return result;
		}

		ProbeEvent pe = generateSPEvent(evt, vb, cp_vb, "!=", true);
		result.add(pe);		
		pe = generateSPEvent(evt, vb, cp_vb, "=", false);
		result.add(pe);
		return result;
	}

	private ArrayList<Event> findTransitionDataReadEventList(Event ee,
			ArrayList<Event> elist) {

		String transitionName = ((TransitionFiringEvent) ee).getTransitionName();
		
		ArrayList<Event> result = new ArrayList<Event>();
		
		for(Event evt: elist) {
			if(evt instanceof DataReadEvent) {
				 String loc = ((DataReadEvent) evt).getVariableLoc();
				 if(loc.startsWith("Transition:")) {
					 int start = loc.indexOf(':')+1;
					 int end = loc.indexOf('_');
					 String trn = loc.substring(start, end);
					 
					 if(transitionName.equals(trn)) result.add(evt);
				 }				 
			}
		}

		return result;
	}


	public double calculateDFDistance(Event evt, ArrayList<Event> event_list,
			String serv) {
		
		String variableName = "================================="; //this can not be matched with any variable name
		if(evt instanceof VariableEvent) variableName = ((VariableEvent) evt).getVariableName();

		int tln = getTotalLocNum(event_list);

		ArrayList<String> mLoc_list = getMutationTokenByServicename(serv);
		if(mLoc_list==null||mLoc_list.isEmpty()) return tln;//not exist a fault, the distance is all the locs

		
		ArrayList<String> pre = new ArrayList<String>();
		ArrayList<String> preCF = new ArrayList<String>();
		ArrayList<String> pos = new ArrayList<String>();
		ArrayList<String> posCF = new ArrayList<String>();
		
		String loc = getEventLoc(evt);
		pre.add(loc);
		preCF.add(loc);
		pos.add(loc);
		posCF.add(loc);
		
		for(int i=0;i<event_list.size();i++) {
			
			Event e = event_list.get(i);
			
			if(e.equals(evt)) {								

				for(int j=i-1;j>=0;j--) { 
				    Event pe = event_list.get(j);
				    String eloc = getEventLoc(pe);
				    if(!preCF.contains(eloc)) preCF.add(eloc);
				    
				    if(pe instanceof VariableEvent) {
				    	String vn = ((VariableEvent) pe).getVariableName();
				    	if (vn.equals(variableName) && !pre.contains(eloc)) pre.add(eloc);				    	
				    }
				}
				
				for(int j=i+1;j<event_list.size();j++) {
				    Event pe = event_list.get(j);
				    String eloc = getEventLoc(pe);
				    if(!posCF.contains(eloc)) posCF.add(eloc);
				    
				    if(pe instanceof VariableEvent) {
				    	String vn = ((VariableEvent) pe).getVariableName();
				    	if (vn.equals(variableName) && !pos.contains(eloc)) pos.add(eloc);
				    }
				}
				
				break;	
			}
		}				
		
		int distance = tln;
		for(String mloc: mLoc_list) {
			int prl1 = matchMLoc(mloc, pre, serv);
			int prl2 = matchMLoc(mloc, preCF, serv);
			int pol1 = matchMLoc(mloc, pos, serv);
			int pol2 = matchMLoc(mloc, posCF, serv);
			
			if(prl1 >=0 && distance > prl1) distance = prl1;
			if(pol1 >=0 && distance > pol1) distance = pol1;
			if(prl2 >=0 && distance > prl2) distance = prl2;
			if(pol2 >=0 && distance > pol2) distance = pol2;				
		}
		
		return distance;
	}		

	private int getTotalLocNum(ArrayList<Event> event_list) {
		ArrayList<String> buffer = new ArrayList<String>();
		for(Event evt: event_list) {
			String loc = getEventLoc(evt);
			if(!buffer.contains(loc)) buffer.add(loc);
		}
		return buffer.size();
	}

	private int matchMLoc(String mloc, ArrayList<String> pre, String srv) {
				
		for(int i=0;i<pre.size();i++)
			if(mloc.equals(srv+ ":"+ pre.get(i))) return i;
		
		return -1;
	}

	private String getEventLoc(Event evt) {
		
		if(evt instanceof TaskCompleteEvent) {
			
			String taskName = ((TaskCompleteEvent)evt).getTaskName();
			return "Task:" + taskName;			
		}
		
		if(evt instanceof TransitionNotFiringEvent) {
			
			String transitionName = ((TransitionNotFiringEvent) evt).getTransitionName();
			return "Transition:" + transitionName;			
		}
		
		if(evt instanceof TransitionFiringEvent) {
						
			String transitionName = ((TransitionFiringEvent) evt).getTransitionName();
			return "Transition:" + transitionName;
		}
		
		if(evt instanceof DataReadEvent) {
			
			String vloc = ((DataReadEvent) evt).getVariableLoc();									
			return extractLoc(vloc);
						
		}
		
		if(evt instanceof DataModificationEvent) {
			
			String vloc = ((DataModificationEvent) evt).getVariableLoc();									
			return extractLoc(vloc);
		}

		return null;
	}
	
    private String extractLoc(String loc) {
		
		int sI = 0;//loc.indexOf(':')+1;
		int eI = loc.indexOf('_');
		
		//for debugging 
		if(eI<0) {
		   System.out.println("loc: " + loc + ", eI=" + eI);
		}
		if(loc.equals("Task:t"))  
			System.out.println("found one invalid loc!");
		
		return loc.substring(sI, eI);
	}

	private boolean isPortEvent(Event evt) {
		
		if(evt instanceof TransitionFiringEvent) return false;
		
		String serviceName = evt.getServiceName();		
		
		if(evt instanceof TaskCompleteEvent) {
			String taskName = ((TaskCompleteEvent)evt).getTaskName();
			
			return isPortTask(serviceName, taskName);			
		}
		
		if(evt instanceof VariableEvent) {
			String vLoc = ((VariableEvent)evt).getVariableLoc();
			if(vLoc.startsWith("Task")) {
	        	int start = vLoc.indexOf(':') + 1;
	        	int end = vLoc.indexOf('_');
	        	String taskname = vLoc.substring(start, end);
	        	
	        	return isPortTask(serviceName, taskname);
			}
		}
				
		return false;
	}
		
	
	private boolean isPortTask(String serviceName, String taskName) {
		ArrayList<String> ports = port_tasks.get(serviceName);
		if(ports!=null) {
			for(String pt: ports) 
				if(pt.equals(taskName)) return true;
		}
		
		return false;
	}

	public SuspiciousEventRank markSuspiciousEvents() throws FLTimeoutException {		
		SuspiciousEventRank rank = new SuspiciousEventRank();
		
		HashMap<String, ArrayList<ProbeEvent>> success_map = new HashMap<String, ArrayList<ProbeEvent>>();
		HashMap<String, ArrayList<ProbeEvent>> failure_map = new HashMap<String, ArrayList<ProbeEvent>>();				
				
		//1. expose events
		for(ExecutionRecord rd: instances) {			
			//the exposed events for 
			ProbeEvent[] events = exposeEvents(rd);
			
			if(rd.isPassed())
				statisEvents(success_map, events);
			else 
				statisEvents(failure_map, events);							
		}		
		rank.failureEvents = failure_map;
		rank.successEvents = success_map;
		
		if(m_solution.exposePolicy==FaultLocalizationSolution.EXPOSEENCAPSULATION ||
				m_solution.exposePolicy==FaultLocalizationSolution.EXPOSEENCAPSULATIONNEW||
				m_solution.exposePolicy==FaultLocalizationSolution.EXPOSEENCAPSULATIONFOLD)
		    furtherRefineEvents(rank);
		
		markSuspiciousEvents(rank);
		
		return rank;
	}
	
	public void markSuspiciousEvents(SuspiciousEventRank rank) throws FLTimeoutException {	
		if(m_solution.clustercompositionPolicy == FaultLocalizationSolution.CLUSTERLOGICAND)
			rankSuspiciousEventsInstanceLevel(rank);
		
		if(m_solution.clustercompositionPolicy == FaultLocalizationSolution.CLUSTERJOINDIST)
			rankSuspiciousEventsInstanceLevelCPE(rank);
		
		if(m_solution.clustercompositionPolicy == FaultLocalizationSolution.NOCLUSTER)
			rankSuspicousEventsOverall(rank);
		
	}
	
	private void rankSuspicousEventsOverall(SuspiciousEventRank rank) {
		int rt = m_solution.ranktype;		
		switch(rt) {
		case 0: rankSuspiciousEventsSD(rank);break;
		case 1: 
		case 2: 
		case 3: 
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
		case 11:
		case 12: rankSuspiciousEventsPC(rank);break;
		}
		
	}

	/**
	 * This function ranks suspicious events using probabilistic causality metrics
	 * @param rank
	 * @param rt
	 */
	private void rankSuspiciousEventsPC(SuspiciousEventRank rank) {
		HashMap<String, ArrayList<ProbeEvent>> failureMap = rank.failureEvents;
		HashMap<String, ArrayList<ProbeEvent>> successMap = rank.successEvents;
				
		Set<String> eIDS = failureMap.keySet();						
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.addAll(eIDS);
		
		Set<String> seID_list = successMap.keySet();
		for(String id: seID_list)
			if(!keyset.contains(id)) keyset.add(id);
		
		rank.totalEventsNum = keyset.size();
		int tn = instances.size();
		ArrayList<RankRecord> rdlist = new ArrayList<RankRecord>();
		
		for(String eID: keyset) {
			ArrayList<ProbeEvent> flist = failureMap.get(eID);
			ArrayList<ProbeEvent> slist = successMap.get(eID);
			
			int fnum = (flist!=null)?flist.size():0;
			int snum = (slist!=null)?slist.size():0;
			
			RankRecord rd = new RankRecord();
			rd.eventIDs = new String[1];
			rd.eventIDs[0] = eID;
			rd.recordID = eID;
			
			//debugging
			rd.fn = fnum;
			rd.sn = snum;
			
			rankEventWithType(tn, fnum, snum, rd);
			rdlist.add(rd);
		}
		
		//sort the final rank list
		rank.ranklist = SuspiciousEventRank.sortRank(rdlist);
		
	}

    public void reRankSuspiciousEvents(SuspiciousEventRank rank, ArrayList<String> refinedEventIDs) throws FLTimeoutException{
		
		HashMap<String, ArrayList<ProbeEvent>> failureMap = rank.failureEvents;
		HashMap<String, ArrayList<ProbeEvent>> successMap = rank.successEvents;
				
		Set<String> eIDS = failureMap.keySet();						
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.addAll(eIDS);
		
		Set<String> seID_list = successMap.keySet();
		for(String id: seID_list)
			if(!keyset.contains(id)) keyset.add(id);
		
		rank.totalEventsNum = keyset.size();

		//Used for caching rank record with Event ID to ease construction of composite events
		HashMap<String, RankRecord> rkcache = new HashMap<String, RankRecord>();
		ArrayList<String> eIDlist = new ArrayList<String>();
		for(RankRecord rd: rank.ranklist) {
			rkcache.put(rd.recordID, rd);
			if(rd.eventIDs.length<=1) {//single event			
				eIDlist.add(rd.recordID);
			}
		}
		
		
		ArrayList<String> generatedCE = new ArrayList<String>();
		ArrayList<RankRecord> rdlist = new ArrayList<RankRecord>();
		for(String eID: refinedEventIDs) {			
			ArrayList<RankRecord> rks = rankSingleEvent(eID, failureMap, successMap);		
			rdlist.addAll(rks);		
			for(RankRecord rd: rks) {
				rkcache.put(rd.recordID, rd);		
				generatedCE.add(rd.recordID);
			}			
		}	
		
		eIDlist.addAll(generatedCE);
		
		//set the threshold for filtering composite events
		//setTopKRankValueThreshold(rdlist);
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			 			
		//handle the combination of events
		if (m_solution.needCompositeEvent) 
			//RankCompositeEvents(rdlist, rkcache, failureMap, successMap);
			RankCompositeEvents(rdlist, rkcache, failureMap, successMap, eIDlist, generatedCE, -1);
		
		//sort the final rank list
		RankRecord[] list = SuspiciousEventRank.sortRank(rdlist);
		rank.ranklist = SuspiciousEventRank.mertSort(rank.ranklist, list);
	}

	/**
	 * This function ranks suspicious events with composite event support 
	 * @param rank
	 */
	
    private void rankSuspiciousEventsInstanceLevelCPE(SuspiciousEventRank rank) throws FLTimeoutException{
		
		HashMap<String, ArrayList<ProbeEvent>> failureMap = rank.failureEvents;
		HashMap<String, ArrayList<ProbeEvent>> successMap = rank.successEvents;
				
		Set<String> eIDS = failureMap.keySet();						
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.addAll(eIDS);
		
		Set<String> seID_list = successMap.keySet();
		for(String id: seID_list)
			if(!keyset.contains(id)) keyset.add(id);
		
		rank.totalEventsNum = keyset.size();
		
		//Used for caching rank record with Event ID to ease construction of composite events
		HashMap<String, RankRecord> rkcache = new HashMap<String, RankRecord>();
		
		ArrayList<RankRecord> rdlist = new ArrayList<RankRecord>();
		for(String eID: keyset) {	
		
			ArrayList<RankRecord> rks = rankSingleEvent(eID, failureMap, successMap);
			
			rdlist.addAll(rks);		
			for(RankRecord rd: rks) rkcache.put(rd.recordID, rd);			
			
		}	
		
		//set the threshold for filtering composite events
		//setTopKRankValueThreshold(rdlist);
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			 			
		//handle the combination of events
		if (m_solution.needCompositeEvent) 
			RankCompositeEvents(rdlist, rkcache, failureMap, successMap);
		
		//sort the final rank list
		rank.ranklist = SuspiciousEventRank.sortRank(rdlist);
	}
	
    private ArrayList<RankRecord> rankSingleEvent(String eID,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {

    	ArrayList<RankRecord> result = new ArrayList<RankRecord>();
    	
    	EventCluster[] clusters = eventClustering(eID, failureMap, successMap);
    	
    	int groupnum = 1;
		
		for(EventCluster clt: clusters) {
			ProbeEvent[] sub = clt.sub;
			if(sub.length==0) continue;
			
			RankRecord rd = new RankRecord();
			rd.eventIDs = new String[1];
			rd.eventIDs[0] = eID;
			
			String groupID = eID + "_g"+ groupnum;
			rd.groupIDs = new String[1];
			rd.groupIDs[0] = groupID;
			rd.recordID = groupID;
			
			rankSingleEvent(eID, clt, failureMap, successMap, rd);
			result.add(rd);
			groupnum++;
			
			//get unique values and cache them in the rankrecord 
			//added on 2016.11.10
			rd.events = clt.sub;
			
		}						

		return result;
	}

	private void rankSingleEvent(String eID, EventCluster clt,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, RankRecord rd) {
		    	
    	int tn = instances.size();
		double fnum = statisticsEventNumber(eID, clt, failureMap);
		double snum = statisticsEventNumber(eID, clt, successMap);
		
		//debugging
		rd.fn = fnum;
		rd.sn = snum;
		
		rankEventWithType(tn, fnum, snum, rd);
    }
    
    /**
     * Rank events based on different causality strength formulas
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankEventWithType(int tn, double fnum, double snum, RankRecord rd){
    	switch(m_solution.ranktype) {
    	case 0:
    	case 1:	
    	case 2: rankSingleEventT0(tn, fnum, snum, rd);
    	   break;
    	case 3: rankSingleEventT3(tn, fnum, snum, rd);   
    	   break;
    	case 4: rankSingleEventT4(tn, fnum, snum, rd);
    	   break;
    	case 5: rankSingleEventT5(tn, fnum, snum, rd);
    	   break;
    	case 6: rankSingleEventT6(tn, fnum, snum, rd);
    	   break;
    	case 7: rankSingleEventT7(tn, fnum, snum, rd);
    	   break;
    	case 8: rankSingleEventT8(tn, fnum, snum, rd);
    	   break;
    	case 9: rankSingleEventT9(tn, fnum, snum, rd);
 	       break;
    	case 10: rankSingleEventT10(tn, fnum, snum, rd);
    	   break;
    	case 11: rankSingleEventT11(tn, fnum, snum, rd);
    	   break;
    	case 12: rankSingleEventT12(tn, fnum, snum, rd);
    	   break;
    	default:
    	   rankSingleEventT0(tn, fnum, snum, rd);	
    	}		
	}


    /**
     * Ranking based on Good causality strength ij2 (CS =((P(~E|~C)-P(~E|C))/P(~E|~C))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT12(int tn, double fnum, double snum, RankRecord rd) {
    	
		double p_S_C = (fnum + snum>0)?(snum*1.0)/(fnum + snum):0;
		double p_S_NOTC = (tn - fnum - snum>0)?((numPassed - snum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_S_NOTC > 0? (p_S_NOTC - p_S_C)/p_S_NOTC:0;		
		rd.prob = 1- p_S_C;								
		
	}

	/**
     * Ranking based on Good causality strength ij1 (CS =((P(~E|~C)-P(~E|C))/(P(~E|~C)+P(~E|C)))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT11(int tn, double fnum, double snum, RankRecord rd) {

    	
		double p_S_C = (fnum + snum>0)?(snum*1.0)/(fnum + snum):0;
		double p_S_NOTC = (tn - fnum - snum>0)?((numPassed - snum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_S_C + p_S_NOTC > 0? ( p_S_NOTC - p_S_C)/(p_S_C + p_S_NOTC):0;		
		rd.prob = 1- p_S_C;								
		
	}

	/**
     * Ranking based on Good causality strength ij (CS =(P(~E|~C)/P(~E|C))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT10(int tn, double fnum, double snum, RankRecord rd) {

		double p_S_C = (fnum + snum>0)?(snum*1.0)/(fnum + snum):0;
		double p_S_NOTC = (tn - fnum - snum>0)?((numPassed - snum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_S_NOTC > 0? p_S_C/p_S_NOTC :0;		
		rd.prob = 1- p_S_C;								
		
	}

	/**
     * Ranking based on Lewis ratio causality strength lr2 (CS =((P(E|C)- P(E|~C))/P(E|C))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT9(int tn, double fnum, double snum, RankRecord rd) {

		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F_NOTC = (tn - fnum - snum>0)?((numFailed - fnum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_F_C > 0? (p_F_C - p_F_NOTC)/p_F_C :0;		
		rd.prob = p_F_C;						
		
	}

	/**
     * Ranking based on Lewis ratio causality strength lr1 (CS =((P(E|C)- P(E|~C))/(P(E|C) + P(E|~C)))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT8(int tn, double fnum, double snum, RankRecord rd) {
    	
		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F_NOTC = (tn - fnum - snum>0)?((numFailed - fnum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_F_NOTC + p_F_C > 0? (p_F_C - p_F_NOTC)/(p_F_C + p_F_NOTC):0;		
		rd.prob = p_F_C;			
		
	}

	/**
     * Ranking based on Lewis ratio causality strength lr (CS =(P(E|C)/P(E|~C)))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private double maxratiorankvalue = 100000000;
    private void rankSingleEventT7(int tn, double fnum, double snum, RankRecord rd) {

		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F_NOTC = (tn - fnum - snum>0)?((numFailed - fnum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_F_NOTC > 0? p_F_C / p_F_NOTC:maxratiorankvalue;
		if(rd.rankValue>maxratiorankvalue) rd.rankValue = maxratiorankvalue; 
		rd.prob = p_F_C;				
		
	}

	/**
     * Ranking based on Cheng causality strength (CS =(P(E|C)-P(E|~C))/P(~E/~C))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT6(int tn, double fnum, double snum, RankRecord rd) {
		
		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F_NOTC = (tn - fnum - snum>0)?((numFailed - fnum)*1.0)/(tn - fnum - snum):0;
		double p_NOTF_NOTC = 1 - p_F_NOTC;
		rd.rankValue = p_NOTF_NOTC > 0? (p_F_C - p_F_NOTC)/ p_NOTF_NOTC:0;
		rd.prob = p_F_C;				
		
	}

	/**
     * Ranking based on Galton causality strength (CS =4P(C)*P(~C)*(P(E|C)-P(E|~C)))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT5(int tn, double fnum, double snum, RankRecord rd) {

		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F_NOTC = (tn - fnum - snum>0)?((numFailed - fnum)*1.0)/(tn - fnum - snum):0;
		double p_C = ((fnum + snum)*1.0)/tn;
		double p_NOTC = 1 - p_C;
		rd.rankValue = 4 * p_C * p_NOTC * (p_F_C - p_F_NOTC);
		rd.prob = p_F_C;				
	}

	/**
     * Ranking based on Suppes causality strength (CS = P(E|C)-P(E))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT4(int tn, double fnum, double snum, RankRecord rd) {
    	
		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F = (numFailed *1.0)/tn;
		rd.rankValue = p_F_C - p_F;
		rd.prob = p_F_C;
		
	}

	/**
     * Ranking based on Eells causality strength (CS = P(E|C)-P(E|~C))
     * @param tn
     * @param fnum
     * @param snum
     * @param rd
     */
    private void rankSingleEventT3(int tn, double fnum, double snum, RankRecord rd) {
    	
		double p_F_C = (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
		double p_F_NOTC = (tn - fnum - snum>0)?((numFailed - fnum)*1.0)/(tn - fnum - snum):0;
		rd.rankValue = p_F_C - p_F_NOTC;
		rd.prob = p_F_C;
	}

	/**
     * RankType = 0, rank events based on fn(eg)-sn(eg), no composite events
     * RankType = 1, rank events based on fn(eg)-sn(eg), has composite events (- dependency)
     * RankType = 2, rank events based on fn(eg)-sn(eg), has composite events (* (1-dependency))    
     * @param fnum
     * @param snum
     * @param rd
     */
	private void rankSingleEventT0(int tn, double fnum, double snum, RankRecord rd) {
				
		rd.rankValue = fnum - snum;
		
		if(fnum + snum <=0) {
			rd.rankValue -= 100000;
			rd.prob = 0;
		} else		
		    rd.prob = (fnum *1.0) /(fnum + snum);				
		
		//optimize the ranking by make those with fnum=0 to smaller than those with those fnum>0		
		if(fnum<=0) rd.rankValue -= 1000;
		
	}

	private double topKThreshold = 0;
	private void setTopKRankValueThreshold(ArrayList<RankRecord> rdlist) {
		Double[] rankvalues = new Double[rdlist.size()];
		
		int rindex = 0;
		for(RankRecord rd: rdlist) {
			rankvalues[rindex] = rd.rankValue;
			rindex++;
		}
		
		Arrays.sort(rankvalues);		
		if(topK > rankvalues.length) 
			rindex = 0;
		else
			rindex = rankvalues.length-topK;
		
		topKThreshold = rankvalues[rindex];
	}

	private void RankCompositeEvents(ArrayList<RankRecord> rdlist,
			HashMap<String, RankRecord> rkcache,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) throws FLTimeoutException {
		
		Set<String> keys = rkcache.keySet();
		ArrayList<String> eIDList = new ArrayList<String>();
		eIDList.addAll(keys);
		
		//used to store generated composite events
		//at the beginning, store all single events for easy implementation
		ArrayList<String> generatedCE = new ArrayList<String>();
		generatedCE.addAll(eIDList);
		
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		
		//recursively rank the composite events
		//RankCompositeEvents(rdlist, rkcache, failureMap, successMap, eIDList, generatedCE, 1);
		//RankCompositeEvents(rdlist, rkcache, failureMap, successMap, eIDList, generatedCE, 6);
		RankCompositeEvents(rdlist, rkcache, failureMap, successMap, eIDList, generatedCE, -1);
		
	}

	private void RankCompositeEvents(ArrayList<RankRecord> rdlist,
			HashMap<String, RankRecord> rkcache,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap,
			ArrayList<String> eIDList, ArrayList<String> generatedCE, int round) throws FLTimeoutException {
		
		
		//double maxrankalue = getMaxRankValue(rdlist);
		
		//used to store the new generated composite event IDs
		ArrayList<String> ngCElist = new ArrayList<String>();
		
		//used to store the removed composite and their corresponding rank record IDs				
		ArrayList<String> removedRecordIDs = new ArrayList<String>();
		
		HashMap<RankRecord, Double> buffer = new HashMap<RankRecord, Double>();
		
		for(String seID: eIDList) {
			RankRecord serd = rkcache.get(seID);
			
			for(String ceID: generatedCE) {
				RankRecord cerd = rkcache.get(ceID);
				if(cerd!=null && !partOfCompositeEventGID(seID, cerd)) {
					
					 //The eIDlist of the new composite event  
					 ArrayList<RankRecord> sortedlist = sortRankRecord(serd, cerd, rkcache);
					 
					 String[] eIDs = new String[sortedlist.size()];
					 String[] gIDs = new String[sortedlist.size()];
					 for(int i=0;i<sortedlist.size();i++) {
						 RankRecord brd = sortedlist.get(i);
						 eIDs[i] = brd.eventIDs[0];
						 gIDs[i] = brd.groupIDs[0];
					 }
					
				/*	 if(serd.recordID.contains("Manufacturer_transition_t2"))
						 if(cerd.recordID.contains("Retailer_dataread_Task:task5_7")) {
							 System.out.println("Stop here!");
						 }
					 
					 if(serd.recordID.contains("Retailer_dataread_Task:task5_7"))
						 if(cerd.recordID.contains("Manufacturer_transition_t2")) {
							 System.out.println("Stop here!");
						 }*/
					 
                     String recordID = generateRecordID(gIDs);	
                     //calculate conditional ranking 
					 //double rv1 = rankEventConditional(serd, cerd, failureMap, successMap);
					 //double rv2 = rankEventConditional(cerd, serd, failureMap, successMap);
					 
					 double rv1_NF = rankEventConditionalNoFuzzy(serd, cerd, failureMap, successMap);
					 double rv2_NF = rankEventConditionalNoFuzzy(cerd, serd, failureMap, successMap);
					 
					 
					 //check whether the corresponding composite event exists or not
					 RankRecord nerd = rkcache.get(recordID);
					 if(nerd!=null) {//already exists
						 
						 //check whether the sub event has a better rank value
						 if(!removedRecordIDs.contains(nerd.recordID) 
								 && existingBetterSubCompositeEvents(rkcache, nerd)) 
							 removedRecordIDs.add(nerd.recordID);
						 
						 continue; 
					 }
					 
					 nerd = new RankRecord();
					 nerd.eventIDs = eIDs;
					 					 					
					 nerd.groupIDs = gIDs;
					 nerd.recordID = recordID;
					 				 
					 //calculate the rank value of this composite event
					 boolean success = RankACompositeEvent(nerd, failureMap, successMap, serd, cerd);
					 if(!success) continue;
					 double nerd_NF = RankACompositeEventNOFuzzy(nerd, failureMap, successMap, serd, cerd);
					 
					 //calculateDependencyPenalty(nerd, serd, cerd, failureMap, successMap);
					 
					 //double incremental = nerd.rankValue * 2 - rv2 - rv1;
					 double incremental = nerd_NF * 2 - rv2_NF - rv1_NF;
					 
					 boolean failed = false;
					 failed = failed || serd.prob > nerd.prob || cerd.prob > nerd.prob;
					 //failed = failed || (isEncapsulated(serd, failureMap, successMap)? rv1_NF > nerd_NF:rv1_NF>= nerd_NF);
					 failed = failed || (isEncapsulated(serd, failureMap, successMap)? false:rv1_NF>= nerd_NF);
					 //failed = failed || (isEncapsulated(cerd, failureMap, successMap)? rv2_NF > nerd_NF:rv2_NF>= nerd_NF);
					 failed = failed || rv2_NF>= nerd_NF;
					 //if(!(rv1_NF>= nerd_NF || rv2_NF >= nerd_NF) && //added on 15.12.02
					      //serd.prob <= nerd.prob && cerd.prob <= nerd.prob) { 
					 if(!failed) {	 
						 if(hasNoMoreLargeSubFuzzyConditional(serd.eventIDs[0], cerd.eventIDs, nerd_NF, failureMap, successMap))						 
						      buffer.put(nerd, incremental);
					 }
				}
			}
		}
		
		//filter the composite events and keep only top k 
		//filterCompositeEvents(rdlist, rkcache, ngCElist);
		
		ArrayList<RankRecord> selected = selectTopkElements(buffer);
		System.out.println("Filter " + buffer.size() + " composite events!");
		
		//Set<RankRecord> selected = buffer.keySet();
		for(RankRecord record: selected) {
			
			if(!existingBetterSubCompositeEvents(rkcache, record)) {
			
			    rdlist.add(record);
			 
			    //cache the new generated rankrecord
			    rkcache.put(record.recordID, record);
			 
			    //put the new generated eventID in the list
			    ngCElist.add(record.recordID);
			}
		}
		
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		
		for(String recordID: removedRecordIDs) {
			RankRecord record = rkcache.remove(recordID);
			rdlist.remove(record);
		}
		
		round --;
		if(!ngCElist.isEmpty() && round!=0) 
			RankCompositeEvents(rdlist, rkcache, failureMap, successMap, eIDList, ngCElist, round);
		
	}
	
	private boolean isEncapsulated(RankRecord serd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		return isEncapsulated(serd.eventIDs, failureMap, successMap);
	}

	private boolean isEncapsulated(String[] eventIDs,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		for(String eID: eventIDs) {
			if(isEncapsulated(eID, failureMap, successMap)) return true;
		}
				
		return false;
	}
	
	public boolean isEncapsulated(String eID, 
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
	    
		ProbeEvent pe = null;
		ArrayList<ProbeEvent> flist = failureMap.get(eID);
		ArrayList<ProbeEvent> slist = successMap.get(eID);
		
		if(pe==null && flist!=null && !flist.isEmpty()) pe = flist.get(0);
		if(pe==null && slist!=null && !slist.isEmpty()) pe = slist.get(0);
		if(pe!=null && pe.isEncapsulated) return true;
		
		return false;
	}

	private boolean hasNoMoreLargeSubFuzzyConditional(String seID,
			String[] eventIDs, double nerd_NF,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		if(eventIDs.length<=1) return true;
		
		String recordID = "NOFuzzy"+seID;//just a string ID, no matter what the content is
		ArrayList<String[][]> eventIDPairs = generateEventIDPairs(seID, eventIDs);
		for(String[][] eIDpair: eventIDPairs) {
			double NF_v1 = rankEventConditionalNoFuzzy(recordID, eIDpair[0], eIDpair[1], failureMap, successMap);
			//boolean failed = isEncapsulated(eIDpair[0], failureMap, successMap)? NF_v1 > nerd_NF:NF_v1 >= nerd_NF;
			if(NF_v1 >= nerd_NF) return false;
			//if(failed) return false;
			double NF_v2 = rankEventConditionalNoFuzzy(recordID, eIDpair[1], eIDpair[0], failureMap, successMap);
			//failed = isEncapsulated(eIDpair[1], failureMap, successMap)? NF_v2 > nerd_NF:NF_v2 >= nerd_NF;
			if(NF_v2 >= nerd_NF) return false;
			//if(failed) return false;
		}
		
		return true;
	}

	private ArrayList<String[][]> generateEventIDPairs(String seID, String[] eventIDs) {
		int length = eventIDs.length;
		ArrayList<String[][]> result = new ArrayList<String[][]>();
		int num = 1;
		for(int i=0;i<length;i++) num*=2;
		for(int i=1;i<num-1;i++) {
			boolean[] indexs = getSelectedIDIndex(i, length);
			String[][] tmp = new String[2][];
			int first = getFirstNum(indexs);
			int second = length - first;
			tmp[0] = new String[first+1];
			tmp[1] = new String[second];
			first=0;second=0;
			for(int k=0;k<indexs.length;k++) {
				boolean sel = indexs[k];
				if(sel) {//first 
				    tmp[0][first] = eventIDs[k];
				    first++;
				} else {//second
					tmp[1][second] = eventIDs[k];
					second++;
				}
		    }
			tmp[0][first] = seID;
			result.add(tmp);
		}
		
		return result;
	}

	private int getFirstNum(boolean[] indexs) {
		int count = 0;
		for(boolean bv: indexs) 
			if(bv) count++;
		return count;
	}

	private boolean[] getSelectedIDIndex(int num, int length) {
		boolean[] result = new boolean[length];
		for(int i=0;i<result.length;i++) {
			int d = num - num/2 * 2;
			num = num/2;
			if(d>0) 
				result[i] = true;
			else
				result[i] = false;
		}	
		
		return result;
	}

	private double rankEventConditionalNoFuzzy(RankRecord serd, RankRecord cerd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		return rankEventConditionalNoFuzzy(serd.recordID, serd.eventIDs, cerd.eventIDs,failureMap, successMap);
	}
		
	private double rankEventConditionalNoFuzzy(String recordID, String[] sEventIDs, String[] cEventIDs,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {	
		HashMap<String, ArrayList<ProbeEvent>> cfmap = constructConditionalEventMap(sEventIDs, failureMap, recordID, cEventIDs);
		HashMap<String, ArrayList<ProbeEvent>> csmap = constructConditionalEventMap(sEventIDs, successMap, recordID, cEventIDs);
		
		ProbeEvent[] buffer = extractUniqueEventContentsbyID(recordID, cfmap, csmap);
 		ArrayList<ProbeEvent> flist = cfmap.get(recordID);
		ArrayList<ProbeEvent> slist = csmap.get(recordID);
		EventCluster[] clusters = nearestNeighbourClusteringMH(flist, slist, buffer);
		//EventCluster[] clusters = eventClustering(serd.recordID, cfmap, csmap);
		RankRecord rd = new RankRecord();
		rd.rankValue = -100000;
		
		for(EventCluster clt: clusters) {
			ProbeEvent[] sub = clt.sub;
			
			if(sub.length==0) continue;				
			
			rankCompositeEvent(recordID, clt, cfmap, csmap, rd);
		}														
				
		return rd.fn - rd.sn;
		
	}

	private boolean existingBetterSubCompositeEvents(HashMap<String, RankRecord> rkcache, RankRecord nerd) {
		
		ArrayList<String> potentialSubrecords = generatePotentialSubRecordIDs(nerd);
		
		Set<String> keys = rkcache.keySet(); 
		for(String ID: keys) {
			if(potentialSubrecords.contains(ID)) {
				RankRecord rd = rkcache.get(ID);
				if(rd.rankValue>=nerd.rankValue) return true;
			}
		}
		
		return false;
	}

	private ArrayList<String> generatePotentialSubRecordIDs(RankRecord nerd) {
		
		String[] groupIDs = nerd.groupIDs.clone();		
		Arrays.sort(groupIDs);
        
		ArrayList<String> result = generatePotentialSubRecordIDs(groupIDs, 0);
		
		return result;
	}

	private ArrayList<String> generatePotentialSubRecordIDs(String[] groupIDs,
			int i) {
		ArrayList<String> result = new ArrayList<String>();
		if(i < groupIDs.length) { 
		    ArrayList<String> buffer = generatePotentialSubRecordIDs(groupIDs, i+1);
		    result.add(groupIDs[i]);
		    for(String id: buffer) {
		    	result.add(id);
		    	result.add(groupIDs[i] + id);
		    }
		}
		
		return result;
	}

	/*
	 * Revised on 15.12.02 to implement a better filter for composite events
	 * Select top K elements from the candidate composite events
	 * 1) top K maximal incremental values first
	 * 2) top K maximal rank values from the rest
	 */
	private ArrayList<RankRecord> selectTopkElements(
			HashMap<RankRecord, Double> buffer) {
		int num = 0;
		ArrayList<RankRecord> selected = new ArrayList<RankRecord>();	
		
		//select top K with maximal incremental values
		while(num <topK && !buffer.isEmpty()) {
			RankRecord tprecord = extractTopRecord(buffer, true);
			selected.add(tprecord);
			num++;
		}
		
		num=0;
		//select top K with maximal rank values
		while(num <topK && !buffer.isEmpty()) {
			RankRecord tprecord = extractTopRecord(buffer, false);
			selected.add(tprecord);
			num++;
		}
		
		return selected;
	}

	private RankRecord extractTopRecord(HashMap<RankRecord, Double> buffer, boolean isValue) {

		double top = -100000;
		RankRecord selected = null;
		Set<RankRecord> keys = buffer.keySet();
		for(RankRecord key: keys) {
			double value = isValue? buffer.get(key): key.rankValue; 
			if(value>top) {
				selected = key;
				top = value;
			}
		}
		
		buffer.remove(selected);
		return selected;
	}

	private double getMaxRankValue(ArrayList<RankRecord> rdlist) {

		if(rdlist==null) return 0;
		
		double max=-1000;
		
		for(RankRecord rd: rdlist)
			if(rd.rankValue>max) max = rd.rankValue;		
		
		return max;
	}

	private double rankEventConditional(RankRecord serd, RankRecord cerd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
				
		HashMap<String, ArrayList<ProbeEvent>> cfmap = constructConditionalEventMap(serd.eventIDs, failureMap, serd.recordID, cerd.eventIDs);
		HashMap<String, ArrayList<ProbeEvent>> csmap = constructConditionalEventMap(serd.eventIDs, successMap, serd.recordID, cerd.eventIDs);
		
		EventCluster[] clusters = eventClustering(serd.recordID, cfmap, csmap);
		RankRecord rd = new RankRecord();
		rd.rankValue = -100000;
		
		for(EventCluster clt: clusters) {
			ProbeEvent[] sub = clt.sub;
			
			if(sub.length==0) continue;				
			
			rankCompositeEvent(serd.recordID, clt, cfmap, csmap, rd);
		}														
				
		return rd.rankValue;
	}

	private HashMap<String, ArrayList<ProbeEvent>> constructConditionalEventMap(
			String[] eventIDs,
			HashMap<String, ArrayList<ProbeEvent>> inputMap, String recordID, String[] ceIDs) {

		HashMap<String, ArrayList<ProbeEvent>> outputMap = new HashMap<String, ArrayList<ProbeEvent>>();
		
		ArrayList<ProbeEvent> feasible = new ArrayList<ProbeEvent>();
		
		//get all shared instances of conditional event IDs
		if(ceIDs==null || ceIDs.length == 0) return outputMap;
		
		ArrayList<String> sharedinsts = getAllInstances(ceIDs[0], inputMap);
		for(int i=1; i < ceIDs.length; i++) {
			String eID = ceIDs[i]; 
		    ArrayList<String> allinsts = getAllInstances(eID, inputMap);
		    ArrayList<String> tmp = new ArrayList<String>();
		    tmp.addAll(sharedinsts);
		    tmp.removeAll(allinsts);
		    sharedinsts.removeAll(tmp);
		}
		
		//for each instance, get all the sub events
		for(String inst: sharedinsts) {
			ArrayList<ArrayList<ProbeEvent>> subevents = getSubEventsWithSameInstance(eventIDs, inputMap, inst, false);
			if(subevents!=null) {
				for(ArrayList<ProbeEvent> sl: subevents) {
				
					if(sl.size()>1) {
				        CompositeProbeEvent cpevent = new CompositeProbeEvent(sl, recordID);
				        feasible.add(cpevent);
				    } 
				
				    if(sl.size()==1) 
					    feasible.add(sl.get(0));
				    }
			}				
		}						
		
		outputMap.put(recordID, feasible);
		
		return outputMap;
	}

	private String generateRecordID(String[] gIDs) {
		 Arrays.sort(gIDs);
		 
		 String recordID = "";
		 for(String gID: gIDs) recordID +=gID;
		 
		 return recordID;
	}

	private String[] extractGroupIDs(RankRecord serd, RankRecord cerd) {
		String[] egIDs = new String[serd.eventIDs.length + cerd.eventIDs.length];				
		
		//Copy group IDs
		int egIDIndex = 0;
		for(String egID: serd.groupIDs) {
			egIDs[egIDIndex] = egID;				    				    
			egIDIndex++;
		}

		for(String egID: cerd.groupIDs) {
			egIDs[egIDIndex] = egID;
			egIDIndex++;
		}
		
		return egIDs;
	}
	
	private String[] extractEventIDs(RankRecord serd, RankRecord cerd) {
		String[] evtIDs = new String[serd.eventIDs.length + cerd.eventIDs.length];								
		
		//Copy event IDs
		int eIDIndex = 0;
		for(String eID: serd.eventIDs) {
			evtIDs[eIDIndex] = eID;				    				    
			eIDIndex++;
		}

		for(String eID: cerd.eventIDs) {
			evtIDs[eIDIndex] = eID;
			eIDIndex++;
		}
		
		return evtIDs;
	}

	private ArrayList<RankRecord> sortRankRecord(RankRecord serd,
			RankRecord cerd, HashMap<String, RankRecord> rkcache) {
		
		ArrayList<RankRecord> result = new ArrayList<RankRecord>();
		ArrayList<RankRecord> tmp = new ArrayList<RankRecord>();
		
		for(String gID: cerd.groupIDs) {
			RankRecord rcd = rkcache.get(gID);
			if(rcd!=null && !tmp.contains(rcd)) tmp.add(rcd);
		}
		
		for(String gID: serd.groupIDs) {
			RankRecord rcd = rkcache.get(gID);
			if(rcd!=null && !tmp.contains(rcd)) tmp.add(rcd);
		}				
		
		RankRecord rd = null;
		double maxvalue = -1000000;
		while(!tmp.isEmpty()) {
			maxvalue = -1000000;	
			for(int i=0;i<tmp.size();i++) {
			    RankRecord rrdt = tmp.get(i);
			    if(rrdt.rankValue> maxvalue) {
			    	maxvalue = rrdt.rankValue;
			    	rd = rrdt;
			    }
			}
			
			if(rd!=null) {
				result.add(rd);
				tmp.remove(rd);
			}
		}
		
		return result;
	}

/*
	private void calculateDependencyPenalty(RankRecord nerd, RankRecord serd,
			RankRecord cerd, HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		int tn = calculateTotalInstanceNum(failureMap, successMap);
		
		String eID = nerd.eventIDs[0]; 		
		ArrayList<String> allinsts = getAllInstances(eID, failureMap);
		
		HashMap<ProbeEvent, ProbeEvent> failureEvents = new HashMap<ProbeEvent, ProbeEvent>();
		HashMap<ProbeEvent, ProbeEvent> successEvents = new HashMap<ProbeEvent, ProbeEvent>();
		
		//for each instance, get all the sub events
		for(String inst: allinsts) {
			ArrayList<ProbeEvent> subevents = getSubEventsWithSameInstance(nerd.eventIDs, failureMap, inst,false);
			if(subevents!=null) {
				ProbeEvent spe = constructProbeEvent(serd.eventIDs, subevents);
				ProbeEvent cpe = constructProbeEvent(cerd.eventIDs, subevents);
			
				failureEvents.put(spe, cpe);
			}
		}
		
		allinsts = getAllInstances(eID, successMap);
		//for each instance, get all the sub events
		for(String inst: allinsts) {
			ArrayList<ProbeEvent> subevents = getSubEventsWithSameInstance(nerd.eventIDs, successMap, inst,false);
			if(subevents!=null) {
				ProbeEvent spe = constructProbeEvent(serd.eventIDs, subevents);
				ProbeEvent cpe = constructProbeEvent(cerd.eventIDs, subevents);
					
				successEvents.put(spe, cpe);
			}
		}
		
		ArrayList<ProbeEvent> tmp1 = new ArrayList<ProbeEvent>();
		tmp1.addAll(failureEvents.keySet());
		tmp1.addAll(successEvents.keySet());
		ArrayList<ProbeEvent> uc1 = getUniContents(tmp1);
		//tmp1.addAll(successEvents.keySet());
		
		ArrayList<ProbeEvent> tmp2 = new ArrayList<ProbeEvent>();
		tmp2.addAll(failureEvents.values());
		tmp2.addAll(successEvents.values());
		ArrayList<ProbeEvent> uc2 = getUniContents(tmp2);
		//tmp2.addAll(successEvents.values());
		
		double dependency = 0;
		for(ProbeEvent pe1: uc1) {
			int en1 = statisticsEventNumWithSameContents(tmp1, pe1);
			for(ProbeEvent pe2: uc2) {
				int en2 = statisticsEventNumWithSameContents(tmp2, pe2);
				int jen = statisticsJointEventWithSameContents(failureEvents, successEvents, pe1, pe2);
				
				//double dp = (en1*en2>0)? (jen*jen*1.0)/(en1*en2):0;
				
				double dp = ((tn*jen*1.0)/(en1*en2)>0)?(jen*1.0/tn)*Math.log((tn*jen*1.0)/(en1*en2)):0; //mutual information
				dependency +=dp;
			}
		}
		
		if(m_solution.penaltyType==FaultLocalizationSolution.PENALTYMINUS)
		   nerd.rankValue -= dependency; 

        if(m_solution.penaltyType==FaultLocalizationSolution.PENALTYPERCANTAGE)    
		   nerd.rankValue = nerd.rankValue * (1 - dependency);   		
	}
*/
	private int calculateTotalInstanceNum(
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		ArrayList<String> instances = new ArrayList<String>();
		
		Set<String> keys = failureMap.keySet();
		for(String eID: keys) {
			ArrayList<ProbeEvent> flist = failureMap.get(eID);
			if(flist!=null) {
				for(ProbeEvent pe: flist)
					if(!instances.contains(pe.instanceID)) instances.add(pe.instanceID);
			}
		}

		keys = successMap.keySet();
		for(String eID: keys) {
			ArrayList<ProbeEvent> slist = successMap.get(eID);
			if(slist!=null) {
				for(ProbeEvent pe: slist)
					if(!instances.contains(pe.instanceID)) instances.add(pe.instanceID);
			}
		}

		return instances.size();
	}

	private int statisticsJointEventWithSameContents(
			HashMap<ProbeEvent, ProbeEvent> failureEvents,
			HashMap<ProbeEvent, ProbeEvent> successEvents, ProbeEvent pe1,
			ProbeEvent pe2) {

		int count = 0;
		Set<ProbeEvent> keys = failureEvents.keySet();
		for(ProbeEvent key: keys) {
			ProbeEvent value = failureEvents.get(key);
			if(key.isSameValue(pe1) && value.isSameValue(pe2))
				count++;
		}
		
		keys = successEvents.keySet();
		for(ProbeEvent key: keys) {
			ProbeEvent value = successEvents.get(key);
			if(key.isSameValue(pe1) && value.isSameValue(pe2))
				count++;
		}
		
		return count;
	}

	private int statisticsEventNumWithSameContents(ArrayList<ProbeEvent> list,
			ProbeEvent pe) {
		
		int count = 0;
		for(ProbeEvent event: list)
			if(event.isSameValue(pe)) count++;
		
		return count;
	}

	private ArrayList<ProbeEvent> getUniContents(ArrayList<ProbeEvent> list) {

		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		
		for(ProbeEvent pe: list)
			if(notExistProbeEventWithSameContents(pe, result))
				result.add(pe);
		
		return result;
	}

	private boolean notExistProbeEventWithSameContents(ProbeEvent pe,
			ArrayList<ProbeEvent> result) {
		
		for(ProbeEvent event: result)
		   if(event.isSameValue(pe)) return false;
		
		return true;
	}

	private ProbeEvent constructProbeEvent(String[] eventIDs,
			ArrayList<ProbeEvent> subevents) {

		if(eventIDs.length==1) {
			String eID = eventIDs[0];
			ProbeEvent pe = getEventWithbyEID(subevents, eID);
			return pe;
		}
		
		ArrayList<ProbeEvent> tl = new ArrayList<ProbeEvent>();
		String cpeID = "";
		for(String eID: eventIDs) {
			ProbeEvent pe = getEventWithbyEID(subevents, eID);
			tl.add(pe);
			cpeID+=eID;
		}				
		
		CompositeProbeEvent cpe = new CompositeProbeEvent(tl, cpeID);
		
		return cpe;
	}

	private ProbeEvent getEventWithbyEID(ArrayList<ProbeEvent> subevents,
			String eID) {
		
		for(ProbeEvent pe: subevents)
		    if(pe.eventID.equals(eID)) return pe;
		
		return null;
	}

	private boolean increaseProbInstanceLevel(RankRecord serd,
			RankRecord cerd, HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		ArrayList<ProbeEvent[]> serd_contents = getUniContents(serd, failureMap);
		ArrayList<ProbeEvent[]> cerd_contents = getUniContents(cerd, failureMap);
		
		for(ProbeEvent[] c1: serd_contents)
			for(ProbeEvent[] c2: cerd_contents) 
				if(increaseProbInstance(c1, c2, failureMap, successMap)) return true;							
		
		
		return false;
	}

	private boolean increaseProbInstance(ProbeEvent[] c1, ProbeEvent[] c2,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		if(c1.length==0 || c2.length ==0) return false;
		
		int fnum_l1 = countSameContentNum(c1, failureMap);
		int snum_l1 = countSameContentNum(c1, successMap);
		
		int fnum_l2 = countSameContentNum(c2, failureMap);
		int snum_l2 = countSameContentNum(c2, successMap);
		
		ProbeEvent[] cg = new ProbeEvent[c1.length+c2.length];
		for(int i=0;i<c1.length;i++)
			cg[i] = c1[i];
		for(int i=0;i<c2.length;i++)
			cg[c1.length+i] = c2[i];
		
		int fnum_g = countSameContentNum(cg, failureMap);
		int snum_g = countSameContentNum(cg, successMap);
		
		double pc1 = (fnum_l1+snum_l1>0)?(fnum_l1*1.0)/(fnum_l1 + snum_l1) : 0;
		double pc2 = (fnum_l2+snum_l2>0)?(fnum_l2*1.0)/(fnum_l2 + snum_l2) : 0;
		double pcg = (fnum_g+snum_g>0)?(fnum_g*1.0)/(fnum_g + snum_g) : 0;
		
		return pcg > pc1 || pcg > pc2;
	}

	private int countSameContentNum(ProbeEvent[] c,
			HashMap<String, ArrayList<ProbeEvent>> map) {
		
		if(c == null || c.length==0) return 0;
		
		String eID = c[0].eventID;
		int count = 0;
		ArrayList<String> instIDs = getAllInstanceIDsWithSameContent(eID, c[0], map);
		for(String inst: instIDs) {
			if(existContentWithInstanceID(inst, c, map)) count++;
		}
		
		return count;
	}

	private boolean existContentWithInstanceID(String inst, ProbeEvent[] c,
			HashMap<String, ArrayList<ProbeEvent>> map) {
		
		for(ProbeEvent event: c) {
			if(!existInstanceWithContent(event,inst, map)) return false; 
		}
		
		return true;
	}

	private boolean existInstanceWithContent(ProbeEvent event, String inst,
			HashMap<String, ArrayList<ProbeEvent>> map) {
		
		String eID = event.eventID;
		ArrayList<ProbeEvent> list = map.get(eID);
		if(list==null) return false;
		for(ProbeEvent pe: list)
			if(pe.instanceID.equals(inst) && pe.isSameValue(event)) return true;
		
		return false;
	}

	private ArrayList<String> getAllInstanceIDsWithSameContent(String eID,
			ProbeEvent pe, HashMap<String, ArrayList<ProbeEvent>> map) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		ArrayList<ProbeEvent> list = map.get(eID);
		if(list!=null) {
			for(ProbeEvent event: list)
				if(pe.isSameValue(event) && !result.contains(event.instanceID))
					result.add(event.instanceID);
		}
		
		return result;
	}

	private ArrayList<ProbeEvent[]> getUniContents(RankRecord serd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap) {
		
		ArrayList<ProbeEvent[]> result = new ArrayList<ProbeEvent[]>();
		
		if(serd.eventIDs.length>0) {
			String eID = serd.eventIDs[0];
			ArrayList<String> instIDs = getAllInstances(eID, failureMap);
			for(String inst: instIDs) {
			    ProbeEvent[] contents = getEventContentsWithInstanceID(inst, serd.eventIDs, failureMap);
			    if(contents!=null && notExistSameContents(contents, result))
			    	result.add(contents);
			}
			
		}
		
		return result;
	}

	private boolean notExistSameContents(ProbeEvent[] contents,
		ArrayList<ProbeEvent[]> list) {
		
		for(ProbeEvent[] cts: list)
			if(hasSameContents(cts, contents)) return false;
		
		return true;
	}

	private boolean hasSameContents(ProbeEvent[] cts, ProbeEvent[] contents) {
		
		if(cts==null|| contents == null || cts.length!=contents.length) return false;
		
		for(int i=0;i<cts.length;i++)
			if(!cts[i].isSameValue(contents[i])) return false; 
		
		return true;
	}

	private ProbeEvent[] getEventContentsWithInstanceID(String inst,
			String[] eventIDs, HashMap<String, ArrayList<ProbeEvent>> failureMap) {

		if(eventIDs.length==0) return null;
		ProbeEvent[] result = new ProbeEvent[eventIDs.length];
		int index = 0;
		for(String eID: eventIDs) {
			ProbeEvent pe = getEventContentWithInstanceID(eID, failureMap, inst);
			if(pe==null) return null;
			result[index] = pe;
			index++;
		}
		
		return result;
	}

	private ProbeEvent getEventContentWithInstanceID(String eID,
			HashMap<String, ArrayList<ProbeEvent>> failureMap, String inst) {
		
		ArrayList<ProbeEvent> list = failureMap.get(eID);
		if(list==null) return null;
		for(ProbeEvent pe: list)
			if(pe.instanceID.equals(inst)) return pe;
		
		return null;
	}

	private boolean increaseProb(RankRecord serd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		double gpb = calculateGlobalProb(serd.eventIDs,failureMap, successMap);
		
		for(String eID: serd.eventIDs){
			double lgb = calculateLocalProb(eID, failureMap, successMap);
//			if(lgb >= gpb) return false;
			if(lgb < gpb) return true;
		}
		
//		return true;
		return false;
	}

	private double calculateLocalProb(String eID,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		ArrayList<ProbeEvent> flist = failureMap.get(eID);
		ArrayList<ProbeEvent> slist = successMap.get(eID);
		int fnum = (flist!=null)?flist.size():0;
		int snum = (slist!=null)?slist.size():0;
				
		return (fnum + snum > 0)? (fnum * 1.0)/(fnum + snum): 0;
	}

	private double calculateGlobalProb(String[] eventIDs, HashMap<String, ArrayList<ProbeEvent>> failureMap, HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		if(eventIDs.length==0) return 0;
		
		ArrayList<String> finsts = new ArrayList<String>();
		ArrayList<String> sinsts = new ArrayList<String>();
		String eID = eventIDs[0];
		
		ArrayList<ProbeEvent> flist = failureMap.get(eID);
		ArrayList<ProbeEvent> slist = successMap.get(eID);
			
		if(flist!=null)
			for(ProbeEvent pe: flist)
				finsts.add(pe.instanceID);
		
		if(slist!=null)
			for(ProbeEvent pe: slist)
				sinsts.add(pe.instanceID);
		
		int fnum = statisticsEventNum(eventIDs, finsts, failureMap);
		int snum = statisticsEventNum(eventIDs, sinsts, successMap);				
		
		return (fnum + snum>0)?(fnum*1.0)/(fnum + snum):0;
	}
	
	private int statisticsEventNum(String[] eventIDs, ArrayList<String> finsts,
			HashMap<String, ArrayList<ProbeEvent>> maps) {
		int count = 0;
		for(String inst: finsts) {
			if(existEventsWithInst(maps, inst, eventIDs)) count++;
		}
		
		return count;
	}

	private boolean existEventsWithInst(
			HashMap<String, ArrayList<ProbeEvent>> maps, String inst,
			String[] eventIDs) {
		
		for(String eID: eventIDs) {
			ArrayList<ProbeEvent> list = maps.get(eID);
			if(list==null) return false;
			boolean exist = false;
			for(ProbeEvent pe: list)
				if(pe.instanceID.equals(inst)) {
					exist = true;
					break;
				}
			
			if(!exist) return false;
		}
		
		return true;
	}

	private void filterCompositeEvents(ArrayList<RankRecord> rdlist,
			HashMap<String, RankRecord> rkcache, ArrayList<String> ngCElist) {
		
		if(!ngCElist.isEmpty()) {
		
			HashMap<Double, ArrayList<String>> buffer = new HashMap<Double, ArrayList<String>>();
		
			Set<String> keys = rkcache.keySet();
			for(String eID: keys) {
				RankRecord rd = rkcache.get(eID); 
				if(rd.eventIDs.length>1) {
					Double rankvalue = rd.rankValue;
					ArrayList<String> eIDs = buffer.get(rankvalue);
					if(eIDs==null) {
						eIDs = new ArrayList<String>();
						buffer.put(rankvalue, eIDs);
					}
				
					eIDs.add(eID);
				}
			}
		
			ArrayList<Double> value = new ArrayList<Double>();
			value.addAll(buffer.keySet());
			Double[] sv = new Double[value.size()];
			value.toArray(sv);
			Arrays.sort(sv);
			
			//int availablenum = topK;
			int availablenum = 100;
			for(int i=sv.length-1;i>=0;i--) {
				Double rankvalue = sv[i];
				ArrayList<String> eIDs = buffer.get(rankvalue);
				if(eIDs!=null) {
					if(availablenum>0) {
					    availablenum -= eIDs.size();					      
					} else {
					    for(String eID: eIDs) {
					    	RankRecord rd = rkcache.remove(eID);
					    	rdlist.remove(rd);
					    	ngCElist.remove(eID);
					    }
					}
				}
			}								
		}
	}
	
	private void outputCompositeEventInfo(RankRecord nerd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, RankRecord serd, RankRecord cerd) {
		
		//construct composite probeevent combing their fields
		String ncEventID = nerd.recordID;
		
		HashMap<String, ArrayList<ProbeEvent>> cfmap = constructCompositeEventMap(nerd.eventIDs, failureMap, ncEventID, false);
		HashMap<String, ArrayList<ProbeEvent>> csmap = constructCompositeEventMap(nerd.eventIDs, successMap, ncEventID, false);
		
		EventCluster[] clusters = eventClustering(ncEventID, cfmap, csmap);
		
		for(EventCluster clt: clusters) {
			ProbeEvent[] sub = clt.sub;
			
			if(sub.length==0) continue;			
			outputSNInfo(ncEventID, clt, cfmap);
			outputSNInfo(ncEventID, clt, csmap);

		}						
	}
	
	private double RankACompositeEventNOFuzzy(RankRecord nerd, 
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
	      HashMap<String, ArrayList<ProbeEvent>> successMap, 
	      RankRecord serd, RankRecord cerd) {

		  //construct composite probeevent combing their fields
		  String ncEventID = nerd.recordID;
          HashMap<String, ArrayList<ProbeEvent>> cfmap = constructCompositeEventMap(nerd.eventIDs, failureMap, ncEventID, false);
          HashMap<String, ArrayList<ProbeEvent>> csmap = constructCompositeEventMap(nerd.eventIDs, successMap, ncEventID, false);

          ProbeEvent[] buffer = extractUniqueEventContentsbyID(ncEventID, cfmap, csmap);
   		  ArrayList<ProbeEvent> flist = cfmap.get(ncEventID);
  		  ArrayList<ProbeEvent> slist = csmap.get(ncEventID);
  		  EventCluster[] clusters = nearestNeighbourClusteringMH(flist, slist, buffer);
          
  		  //EventCluster[] clusters = eventClustering(ncEventID, cfmap, csmap);

          RankRecord rd = new RankRecord();
          rd.rankValue = -100000;

          for(EventCluster clt: clusters) {
	          ProbeEvent[] sub = clt.sub;
	
	          if(sub.length==0) continue;				
	
	          rankCompositeEvent(ncEventID, clt, cfmap, csmap, rd);
          }														
		
          return rd.fn - rd.sn;
    }
	
	private boolean RankACompositeEvent(RankRecord nerd,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, RankRecord serd, RankRecord cerd) {
		
		//construct composite probeevent combing their fields
		String ncEventID = nerd.recordID;
		
		HashMap<String, ArrayList<ProbeEvent>> cfmap = constructCompositeEventMap(nerd.eventIDs, failureMap, ncEventID, false);
		HashMap<String, ArrayList<ProbeEvent>> csmap = constructCompositeEventMap(nerd.eventIDs, successMap, ncEventID, false);
		
		EventCluster[] clusters = eventClustering(ncEventID, cfmap, csmap);
		boolean success = false;
		
		for(EventCluster clt: clusters) {
			ProbeEvent[] sub = clt.sub;
			
			if(sub.length==0) continue;						
			rankCompositeEvent(ncEventID, clt, cfmap, csmap, nerd);

			success = true;
		}				
		
		return success;
	}


	//private void rankCompositeEvent(String ncEventID,
	private void rankCompositeEvent(String ncEventID,
			EventCluster clt, HashMap<String, ArrayList<ProbeEvent>> cfmap,
			HashMap<String, ArrayList<ProbeEvent>> csmap, RankRecord nerd) {
		
		int tn = instances.size();
		double fnum = statisticsEventNumber(ncEventID, clt, cfmap);
		double snum = statisticsEventNumber(ncEventID, clt, csmap);
		
		//debugging
		nerd.fn = fnum;
		nerd.sn = snum;
		
		rankEventWithType(tn, fnum, snum, nerd);
	}

	private HashMap<String, ArrayList<ProbeEvent>> constructCompositeEventMap(
			String[] eventIDs,
			HashMap<String, ArrayList<ProbeEvent>> inputMap, String ncEventID, boolean extended) {
		
		HashMap<String, ArrayList<ProbeEvent>> outputMap = new HashMap<String, ArrayList<ProbeEvent>>();
		
		ArrayList<ProbeEvent> feasible = new ArrayList<ProbeEvent>();
		
		//get all instances of the first event ID
		String eID = eventIDs[0]; 		
		ArrayList<String> allinsts = getAllInstances(eID, inputMap);
		
		//for each instance, get all the sub events
		for(String inst: allinsts) {
			ArrayList<ArrayList<ProbeEvent>> subevents = getSubEventsWithSameInstance(eventIDs, inputMap, inst, extended);
			if(subevents!=null) {
				for(ArrayList<ProbeEvent> sl: subevents) {
					CompositeProbeEvent cpevent = new CompositeProbeEvent(sl, ncEventID);
					feasible.add(cpevent);
				}
			}				
		}						
		
		outputMap.put(ncEventID, feasible);
		
		return outputMap;
	}		

	private ArrayList<ArrayList<ProbeEvent>> getSubEventsWithSameInstance(
			String[] eventIDs, HashMap<String, ArrayList<ProbeEvent>> inputMap,
			String inst, boolean extended) {
		
		ArrayList<ArrayList<ProbeEvent>> result = new ArrayList<ArrayList<ProbeEvent>>();
		
		int index = 0;
		HashMap<String, ArrayList<ProbeEvent>> buffer = new HashMap<String, ArrayList<ProbeEvent>>();
		
		while(index <eventIDs.length) {
			String eID =eventIDs[index];
			ArrayList<ProbeEvent> pe = getProbeEventWithInstanceEventID(eID, inst, inputMap);
			if(pe==null) {
				if (!extended) return null;
				//pe = createNOTAvailabeProbe(eID, inst, inputMap);
				//if (pe==null) return null;
			}
			
			buffer.put(eID, pe);
			//result.add(pe);
			index++;
		}				
		
		ArrayList<ProbeEvent> subevents = new ArrayList<ProbeEvent>();
		constructSubEventWithSameInstance(eventIDs, buffer, 0, result, subevents);
		
		return result; 
	}

	private void constructSubEventWithSameInstance(
			String[] eventIDs, HashMap<String, ArrayList<ProbeEvent>> buffer, 
			int index, ArrayList<ArrayList<ProbeEvent>> result, ArrayList<ProbeEvent> subevents) {			

		if(index < eventIDs.length) {
			String eID = eventIDs[index];
			ArrayList<ProbeEvent> evtlist = buffer.get(eID);
			for(ProbeEvent pe: evtlist) {
				ArrayList<ProbeEvent> nelist = new ArrayList<ProbeEvent>();
				nelist.addAll(subevents);
				nelist.add(pe);
				constructSubEventWithSameInstance(eventIDs, buffer, index+1, result, nelist);
			}
			
		} else
			result.add(subevents);
		
	}

	private ProbeEvent createNOTAvailabeProbe(String eID, String inst,
			HashMap<String, ArrayList<ProbeEvent>> inputMap) {
		
		ProbeEvent result = null;
		ArrayList<ProbeEvent> elist = inputMap.get(eID);
		if(elist!=null) {
			ProbeEvent pe = elist.get(0);
			result = new ProbeEvent();
			result.eventID = pe.eventID;
			result.instanceID = inst;
			result.serviceName = pe.serviceName;
			result.eventToken = pe.eventToken;
			result.isEncapsulated = pe.isEncapsulated;
			result.fields = new ArrayList<DataField>();
			for(DataField df: pe.fields) {
				DataField fitem  = new DataField();
				fitem.setName(df.getName());
				fitem.setType(df.getType());
				fitem.setValue(null);
				result.fields.add(fitem);
			}
		}
		
		return result;
	}

	private ArrayList<ProbeEvent> getProbeEventWithInstanceEventID(String eID,
			String inst, HashMap<String, ArrayList<ProbeEvent>> inputMap) {
		
		ArrayList<ProbeEvent> elist = inputMap.get(eID);
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		if(elist!=null) {
			for(ProbeEvent pe: elist) {
				if(inst.equals(pe.instanceID)) {
					result.add(pe);
				}
			}

			return result;
		}
		
		return null;
	}

	private ArrayList<String> getAllInstances(String eID,
			HashMap<String, ArrayList<ProbeEvent>> inputMap) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		ArrayList<ProbeEvent> elist = inputMap.get(eID);
		if(elist!=null) {
			for(ProbeEvent pe: elist) {
				String inst = pe.instanceID;
				if(!result.contains(inst)) result.add(inst);
			}
		}
		
		return result;
	}	

	private boolean partOfCompositeEvent(String seID, RankRecord rd) {
		for(String eID:rd.eventIDs)
			if(seID.equals(eID)) return true;
		
		return false;
	}
	
	private boolean partOfCompositeEventGID(String seID, RankRecord rd) {
		for(String eID:rd.groupIDs)
			if(seID.equals(eID)) return true;
		
		return false;
	}

	private void rankSuspiciousEventsSD(SuspiciousEventRank rank) {				
		
		HashMap<String, ArrayList<ProbeEvent>> failureMap = rank.failureEvents;
		HashMap<String, ArrayList<ProbeEvent>> successMap = rank.successEvents;
		int filternum=0;
		ArrayList<String> removed = new ArrayList<String>();
		
		Set<String> eIDS = failureMap.keySet();						
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.addAll(eIDS);
		
		Set<String> seID_list = successMap.keySet();
		for(String id: seID_list)
			if(!keyset.contains(id)) keyset.add(id);
		
		rank.totalEventsNum = keyset.size();
		HashMap<String, RankRecord> rkcache = new HashMap<String, RankRecord>();
		ArrayList<RankRecord> rdlist = new ArrayList<RankRecord>();
		for(String eID: keyset) {
			RankRecord rd = new RankRecord();
			
			/* for debugging
			if(eID.startsWith("Manufacturer_Task:task3")) {
				System.out.println("pause here");
			}*/
		
			double criteria = getStatisticsSDNew(eID, failureMap, successMap);
			
			//added on 2016.03.10 to filter the invalid predicates
			if(criteria<0) {
				filternum++;
				removed.add(eID);
				continue;
			}
			
			rd.eventIDs = new String[1];
			rd.eventIDs[0] = eID;
			rd.groupIDs = new String[1];
			rd.groupIDs[0] = eID;
			rd.rankValue = criteria;
			rd.recordID = eID;
			
			rdlist.add(rd);
			rkcache.put(eID, rd);
		}
		
		//for debugging
		System.out.println("SD filter " + filternum + " predicates!");
		
		//handle composite events
		if(m_solution.needCompositeEvent) {
			ArrayList<String> eIDList = new ArrayList<String>();
			eIDList.addAll(keyset);	
			eIDList.removeAll(removed);
			RankCBEvent4SD(rdlist, rkcache, failureMap, successMap, eIDList);
		}
		
		rank.ranklist = SuspiciousEventRank.sortRank(rdlist);
	}

			
	private void RankCBEvent4SD(ArrayList<RankRecord> rdlist,
			HashMap<String, RankRecord> rkcache,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap,
			ArrayList<String> eIDList) {
		
		//get the topK threshold rankvalue to filter CB predicates
		//added on 2016.03.10
		double threshold = getTopKRankValue(rdlist, topK);
		int filternum = 0;

		int num = eIDList.size();
		for(int i=0;i<num;i++) {
			
			String eID1 = eIDList.get(i);
			RankRecord rd1 = rkcache.get(eID1);
			
			for(int j=i+1;j<num;j++) {
				
				String eID2 = eIDList.get(j);
				RankRecord rd2 = rkcache.get(eID2);
				
				//calculate the estimated upbound
				//added on 2016.03.10
				int[] f1 = statisRuns(eID1, failureMap);
				int[] s1 = statisRuns(eID1, successMap);
				int[] f2 = statisRuns(eID2, failureMap);
				int[] s2 = statisRuns(eID2, successMap);
				
				double upbound1 = estimateLoginAndUnbound(f1, s1, f2, s2); 
				
				if(upbound1>threshold) {
					//conjunction
					double criteria = getStatisticsSDNewCB(eID1, eID2, failureMap, successMap, true);
				
					if(criteria>rd1.rankValue && criteria>rd2.rankValue) {				
						RankRecord rd = new RankRecord();
						rd.eventIDs = new String[2];
						rd.eventIDs[0] = eID1;
						rd.eventIDs[1] = eID2;
						rd.groupIDs = new String[2];
						rd.groupIDs[0] = eID1;
						rd.groupIDs[1] = eID2;
						rd.rankValue = criteria;
						rd.recordID = eID1 + eID2;
				
						rdlist.add(rd);
					}
				} else filternum++;
				
				//calculate the estimated upbound
				//added on 2016.03.10
				double upbound2 = estimateLoginORUnbound(f1, s1, f2, s2);
				
				if(upbound2>threshold) {
					//disjunction
					double criteria = getStatisticsSDNewCB(eID1, eID2, failureMap, successMap, false);
				
					if(criteria>rd1.rankValue && criteria>rd2.rankValue) {				
						RankRecord rd = new RankRecord();
						rd.eventIDs = new String[2];
						rd.eventIDs[0] = eID1;
						rd.eventIDs[1] = eID2;
						rd.groupIDs = new String[2];
						rd.groupIDs[0] = eID1;
						rd.groupIDs[1] = eID2;
						rd.rankValue = criteria;
						rd.recordID = eID1 + eID2;
				
						rdlist.add(rd);
					}
				} else filternum++;
			}
		}
		
		//for debugging
		System.out.println("SDCB filter " + filternum + " composite predicates!");
	}

    private int[] statisRuns(String eID,
			HashMap<String, ArrayList<ProbeEvent>> map) {
		
    	int[] result = new int[2];
    	int fn1 = 0, fn2 = 0;
    	
    	ArrayList<ProbeEvent> ffe_list = map.get(eID);
		filterSameInstanceEvents(ffe_list);
		if(ffe_list!=null)
			for(ProbeEvent pe: ffe_list) {
				DataField df = pe.fields.get(0);
				Boolean value = (Boolean) df.getValue();
				if(value) 
					fn1++;
				else
					fn2++;
			}
    	
		result[0] = fn1;
		result[1] = fn2;
		return result;
	}

    //added on 2016.03.11
	private double estimateLoginAndUnbound(int[] f1, int[] s1, int[] f2, int[] s2) {
		
		int fc = f1[0]>f2[0]?f2[0]:f1[0];

		int tmp = s1[0] + s2[0]- numPassed;
		int sc = tmp > 0 ? tmp:0;

		tmp = s1[1] + s2[1] + s1[0]>s2[0]?s2[0]:s1[0];
		int scobs = tmp>numPassed? numPassed:tmp;

		tmp = f1[0] + f2[0] + f1[1]>f2[1]?f2[1]:f1[1] - numFailed;
		int fcobs =f1[1]>f2[1]?f1[1]:f2[1] + tmp>0? tmp:0;
		
		double dr1 = sc+fc>0?fc/(sc+fc):0;
		double dr2 = scobs+fcobs>0?fcobs/(scobs+fcobs):0;
		double increase = dr1 - dr2;
		
		double sens1 = fc >0 ? Math.log(fc):0;
		double sens2 = numFailed >0 ? Math.log(numFailed):0;
		double sensitivity = sens2 !=0 ? sens1 / sens2 : 0;
		
		if(increase==0 || sensitivity ==0) return 0;
		
		double dr = 1/increase + 1/sensitivity;
		if(dr==0) return 0;
		
		return 2/dr; 
	}
	
    //added on 2016.03.11
	private double estimateLoginORUnbound(int[] f1, int[] s1, int[] f2, int[] s2) {
		
		int tmp = f1[0] + f2[0];
		int fc = numFailed> tmp?tmp:numFailed;

		int sc = s1[0] > s2[0] ? s1[0]:s2[0];

		tmp = s1[0] + s2[0] + s1[1]>s2[1]?s2[1]:s1[1];
		int scobs = tmp>numPassed? numPassed:tmp;

		tmp = f1[1] + f2[1] + f1[0]>f2[0]?f2[0]:f1[0] - numFailed;
		int fcobs =f1[0]>f2[0]?f1[0]:f2[0] + tmp>0? tmp:0;
		
		double dr1 = sc+fc>0?fc/(sc+fc):0;
		double dr2 = scobs+fcobs>0?fcobs/(scobs+fcobs):0;
		double increase = dr1 - dr2;
		
		double sens1 = fc >0 ? Math.log(fc):0;
		double sens2 = numFailed >0 ? Math.log(numFailed):0;
		double sensitivity = sens2 !=0 ? sens1 / sens2 : 0;
		
		if(increase==0 || sensitivity ==0) return 0;
		
		double dr = 1/increase + 1/sensitivity;
		if(dr==0) return 0;
		
		return 2/dr; 
	}

	//added on 2016.03.10
	private double getTopKRankValue(ArrayList<RankRecord> rdlist, int num) {
		
		double[] buffer = new double[num+1];
		for(int i=0;i<=num;i++) buffer[i] = -1;
		
		for(RankRecord rd: rdlist) {
			if(rd.rankValue>buffer[num]||buffer[0]<0) {
				buffer[0] = rd.rankValue;
				boolean change = true;
				//bubble sort to keep the smallest one at buffer[num]
				for(int j=0;j<num;j++) {
					if(Math.abs(buffer[j]-buffer[j+1])<0.000001) {
						 //duplicate, remove it
						 double tmp = buffer[0];
						 buffer[0] = buffer[j];
						 buffer[j] = tmp;
						 change = false;
						 break;
					}
					
					if(buffer[j+1]<0 || buffer[j]<buffer[j+1]) { //swap
						double tmp = buffer[j+1];
						buffer[j+1] = buffer[j];
						buffer[j] = tmp;
					}
				}
				
				if(change && buffer[0]>=0) {
					buffer[num] = buffer[num-1];
					buffer[num-1] = buffer[0];
				}
			}
		}
		
		return buffer[num];
	}

	private void rankSuspiciousEventsInstanceLevel(SuspiciousEventRank rank) throws FLTimeoutException {
		
		HashMap<String, ArrayList<ProbeEvent>> failureMap = rank.failureEvents;
		HashMap<String, ArrayList<ProbeEvent>> successMap = rank.successEvents;
				
		Set<String> eIDS = failureMap.keySet();						
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.addAll(eIDS);
		
		Set<String> seID_list = successMap.keySet();
		for(String id: seID_list)
			if(!keyset.contains(id)) keyset.add(id);
		
		rank.totalEventsNum = keyset.size();		

/*		
		if(topK<=0) {
			topK = (int) Math.round(rank.totalEventsNum * 0.15);			
		}
*/
		//this data structure is used to store events that need to be combined
		HashMap<String, ProbeEvent[]> egroups = new HashMap<String, ProbeEvent[]>();
		HashMap<String, RankRecord> rkcache = new HashMap<String, RankRecord>();
		
		ArrayList<RankRecord> rdlist = new ArrayList<RankRecord>();
		for(String eID: keyset) {						
			//ProbeEvent[][] clusters = eventClustering(eID, failureMap, successMap);
			EventCluster[] clusters = eventClustering(eID, failureMap, successMap);
			
			//ProbeEvent[][] reverse_clusters = eventClustering(eID, successMap, failureMap);
			
			int groupnum = 1;
			//for(ProbeEvent[] sub: clusters) {
			for(EventCluster clt: clusters) {
				ProbeEvent[] sub = clt.sub;
				
				if(sub.length==0) continue;
				
				RankRecord rd = new RankRecord();
				String groupID = eID + "_g"+ groupnum;
				rd.eventIDs = new String[1];
				rd.eventIDs[0] = eID;
				rd.groupIDs = new String[1];
				rd.groupIDs[0] = groupID;
				rd.recordID = groupID;
				
				double fnum = statisticsEventNumber(eID, clt, failureMap);
				double snum = statisticsEventNumber(eID, clt, successMap);
				int tn = instances.size();
				
				//debugging
				rd.fn = fnum;
				rd.sn = snum;
				
				rankEventWithType(tn,fnum,snum,rd);
				/*
				rd.rankValue = fnum - snum;
				rd.prob = (fnum *1.0) /(fnum + snum);
				*/
				
				//ProbeEvent[] reverse_sub = reverse_clusters[0];
				//int reverse_fnum = statisticsEventNumber(reverse_sub, failureMap);
				//int reverse_snum = statisticsEventNumber(reverse_sub, successMap);
				
				//rd.reverse_rankvalue = reverse_fnum - reverse_snum;
				//rd.reverse_prob = (reverse_fnum + reverse_snum)>0?(reverse_fnum *1.0) /(reverse_fnum + reverse_snum):0;

				rdlist.add(rd);
				
				rkcache.put(groupID, rd);				
				if(snum>0) {
					egroups.put(groupID, sub);
					//egroups.put("reverse_"+groupID, reverse_sub);
				}
				
				groupnum++;
				
				//for debugging
				//if(Config.getConfig().debugModel)
				//	outputEventInfo(eID, sub, failureMap, successMap, fnum, snum);
			}						
		}	
				
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		
		//handle the combination of events
		if(m_solution.needCompositeEvent)
			constructEventCombination(rdlist,failureMap, successMap, rkcache, egroups);
		
		rank.ranklist = SuspiciousEventRank.sortRank(rdlist);
	}
	
	private void outputEventInfo(String eID, ProbeEvent[] sub,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, int fnum,
			int snum) {
		
		//if(eID.equals("OrderProcess_datamodification_Task:task2_1") || 
		//		   eID.equals("CreditCardService_datamodification_Task:task1_3")) {
					
			 System.out.println("number of matched failure events:" + fnum);
			 outputMatchEvents(sub, failureMap, eID);
					
			 System.out.println("number of matched success events:" + snum);
			 outputMatchEvents(sub, successMap, eID);										
		//}
		
	}

	private void outputMatchEvents(ProbeEvent[] sub,
			HashMap<String, ArrayList<ProbeEvent>> eventMap, String eID) {
		
		ArrayList<ProbeEvent> eventlist = eventMap.get(eID);
		if(eventlist!=null){
				
			for(ProbeEvent event: sub)
				for(ProbeEvent ev: eventlist)  
					if(ev.isSameValue(event)) {
						System.out.println("InstanceID:" + ev.instanceID);
						System.out.println("EventID:" + ev.eventID);
						System.out.println("Data Fields:");
						for(DataField df: ev.fields) {
							System.out.println("   Name:" + df.getName());
							System.out.println("   Type:" + df.getType());
							System.out.println("   Value:" + df.getValue());
						}
					}
		}
				
	}

	private void constructEventCombination(ArrayList<RankRecord> rdlist,HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap,
			HashMap<String, RankRecord> rkcache,
			HashMap<String, ProbeEvent[]> egroups) throws FLTimeoutException {						
		
		ArrayList<String> IDList = new ArrayList<String>();
		IDList.addAll(egroups.keySet());
		
		ArrayList<String> generatedIDs = new ArrayList<String>();
		generatedIDs.addAll(IDList);
		
		constructEventCombination(rdlist, IDList, generatedIDs, failureMap, successMap, rkcache, egroups);

/*		
		while(!IDList.isEmpty()) {
			String eID = IDList.remove(0);
			constructEventCombination(rdlist, eID, IDList, failureMap, successMap, combine);
		}
*/		
	}

	private void constructEventCombination(ArrayList<RankRecord> rdlist,
			ArrayList<String> IDList, ArrayList<String> generatedIDs,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap,
			HashMap<String, RankRecord> rkcache,
			HashMap<String, ProbeEvent[]> egroups) throws FLTimeoutException  {
		
		ArrayList<String> ngIDs = new ArrayList<String>();
		
		int aaa=0;
		
		for(String seID: IDList) {
						
			RankRecord rd1 = rkcache.get(seID);
			if(rd1==null) continue;
			
			if(seID.startsWith("OrderProcess_dataread_Transition:t10_1")) {
				aaa++;
			}
						
			for(String deID: generatedIDs) {												
				RankRecord rd2 = rkcache.get(deID);
				
				if(deID.startsWith("OrderProcess_dataread_Transition:t10_1")) {
					aaa++;
				}
				
				if(rd2==null|| partOfCompositeEventGID(seID, rd2)) continue;
				
				String[] egIDs = new String[rd1.eventIDs.length + rd2.eventIDs.length];				
				{//Copy group IDs
					int egIDIndex = 0;
					for(String egID: rd1.groupIDs) {
						egIDs[egIDIndex] = egID;				    				    
						egIDIndex++;
					}

					for(String egID: rd2.groupIDs) {
						egIDs[egIDIndex] = egID;
						egIDIndex++;
					}
				}
				
				String[] eIDs = new String[egIDs.length];
				{ //copy event IDs					
					int eIDIndex = 0;
					for(String eID: rd1.eventIDs) {
						eIDs[eIDIndex] = eID;				    				    
						eIDIndex++;
					}				

					for(String eID: rd2.eventIDs) {
						eIDs[eIDIndex] = eID;
						eIDIndex++;
					}
				}
				
				Arrays.sort(egIDs);
				String cpEGID = "";
				for(String eID: egIDs) cpEGID+=eID;
				
				RankRecord cprd = rkcache.get(cpEGID);
				if(cprd!=null) continue;//already exists
				
				cprd = new RankRecord();
				cprd.recordID = cpEGID;
				cprd.eventIDs = eIDs;
				cprd.groupIDs = egIDs;							
				
				int fnum = statisticsEventNumber(egIDs, egroups, failureMap);
				int snum = statisticsEventNumber(egIDs, egroups, successMap);
				int tn = instances.size();
				rankEventWithType(tn, fnum, snum, cprd);				
				
				/*
				double rankvalue = fnum *1.0/tn -  (numFailed * 1.0/tn) * ((rd1.fn + rd1.sn) *1.0/tn)  
						                        -  (numFailed * 1.0/tn) * ((rd2.fn + rd2.sn) *1.0/tn)
						                        +  (numFailed * 1.0/tn) * ((fnum + snum) *1.0/tn);
						                        */
				/*
				int unum = statisticsUnitEventNumber(egIDs, egroups, failureMap, successMap);
				cprd.rankValue = 4 * (fnum *1.0/tn - (numFailed * 1.0/tn) * (unum*1.0/tn));
				*/
				/*
				cprd.rankValue = fnum - snum;
				cprd.prob = (fnum *1.0) /(fnum + snum);
				*/		
				
				//debugging
				cprd.fn = fnum;
				cprd.sn = snum;
				
				//calculateDependencyPenalty(cprd, rd1, rd2, failureMap, successMap);
				//boolean pbincrease = increaseProb(cprd, failureMap, successMap); 
				//boolean pbincrease = increaseProbInstanceLevel(rd1, rd2, failureMap, successMap);
				boolean pbincrease = true;
				if(cprd.prob>= rd1.prob && cprd.prob>=rd2.prob 
						&& cprd.rankValue > rd1.rankValue 
						&& cprd.rankValue > rd2.rankValue
						&& pbincrease) {
				    
					rdlist.add(cprd);				
				    rkcache.put(cpEGID, cprd);				
				    if(snum>0) ngIDs.add(cpEGID);
				}
				
				String[] reverse_egIDs = constructReverseEGIDs(egIDs); 
				int reverse_fnum = statisticsEventNumber(reverse_egIDs, egroups, failureMap);
				int reverse_snum = statisticsEventNumber(reverse_egIDs, egroups, successMap);
				
				cprd.reverse_rankvalue = reverse_fnum - reverse_snum;
				cprd.reverse_prob = (reverse_fnum + reverse_snum)>0?(reverse_fnum *1.0) /(reverse_fnum + reverse_snum):0;
				
				//for debugging
				//if(Config.getConfig().debugModel)
				//	outputCompositeEventInfo(cpEGID, egIDs, egroups, failureMap, successMap, fnum, snum);
			}
		}
		
		filterCompositeEvents(rdlist, rkcache, ngIDs);
		
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException(); 
			
		if(!ngIDs.isEmpty())
			constructEventCombination(rdlist, IDList, ngIDs, failureMap, successMap, rkcache, egroups);
	}

	private int statisticsUnitEventNumber(String[] egIDs,
			HashMap<String, ProbeEvent[]> egroups,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		ArrayList<String> unitinstances = new ArrayList<String>();
		
		for(int i=0;i<egIDs.length;i++) {
		    String egID1 = egIDs[i];
		    ProbeEvent[] eg1 = egroups.get(egID1);
		    if(eg1==null || eg1.length==0) continue;
		
		    String eID = eg1[0].eventID; 
		    ArrayList<String> allinsts = getAllInstances(eID, failureMap);
		    
		    for(String inst: allinsts) 
		    	if(!unitinstances.contains(inst)) unitinstances.add(inst);
		    
		    allinsts.clear();
		    allinsts = getAllInstances(eID, successMap);
		    for(String inst: allinsts) 
		    	if(!unitinstances.contains(inst)) unitinstances.add(inst);
		}
				
		return unitinstances.size();

	}

	private void rankCombinatedEvent(String[] egIDs,
			HashMap<String, ProbeEvent[]> egroups,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, RankRecord cprd) {
		// TODO Auto-generated method stub
		
	}

	private String[] constructReverseEGIDs(String[] egIDs) {
		
		String[] results = new String[egIDs.length];
		for(int i=0;i<results.length;i++)
			results[i] = "reverse_"+egIDs[i];
		
		return results;
	}

	private void outputCompositeEventInfo(String cpEGID, String[] egIDs,
			HashMap<String, ProbeEvent[]> egroups,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, int fnum,
			int snum) {
		
		//if(cpEGID.equals("CreditCardService_datamodification_Task:task1_3_g1OrderProcess_datamodification_Task:task2_1_g1")) {
		if(snum==0){
			System.out.println("number of matched failure events:" + fnum);
			outputFilteringEvents(egIDs, egroups, failureMap);
			
			System.out.println("number of matched success events:" + snum);
			outputFilteringEvents(egIDs, egroups, successMap);
		}
		
	}

	private void outputFilteringEvents(String[] egIDs,
			HashMap<String, ProbeEvent[]> egroups,
			HashMap<String, ArrayList<ProbeEvent>> inputMap) {
		
		for(String egID: egIDs) {
			ProbeEvent[] eg = egroups.get(egID);
			if(eg == null || eg.length == 0) continue;
			String eID = eg[0].eventID;
			
			ArrayList<String> allinsts = getAllInstances(eID, inputMap);
			for(String inst: allinsts) 
				outputMismatchedEvents(egIDs, egID, inputMap, inst, egroups);
			
		}
		
	}

	private void outputMismatchedEvents(String[] egIDs, String m_egID,
			HashMap<String, ArrayList<ProbeEvent>> inputMap, String inst,
			HashMap<String, ProbeEvent[]> egroups) {
		
		ProbeEvent[] m_eg = egroups.get(m_egID);
		if(m_eg!=null && m_eg.length>0) {
			String m_eID = m_eg[0].eventID;		
			boolean m_exist = countProbeEventWithInstanceEventID(m_eID, inst, inputMap, m_eg);
			if(m_exist) {		
				int index = 0;
				while(index <egIDs.length) {			
					String egID =egIDs[index];
					index++;
					if(egID.equals(m_egID)) continue;
			
					ProbeEvent[] eg = egroups.get(egID);
					if(eg==null || eg.length==0) continue;
					String eID = eg[0].eventID;
			
					boolean exist = countProbeEventWithInstanceEventID(eID, inst, inputMap, eg);
					if(!exist) {
						System.out.println("This event has been filtered due to composite event!");
						outputEventInfowithInst(m_egID, inputMap, egroups, inst);
						System.out.println("With the following reasons:");
						outputEventInfowithInst(egID, inputMap, egroups, inst);
						
						break;
					}						
				}				
			}
		}
	}

	private void outputEventInfowithInst(String egID,
			HashMap<String, ArrayList<ProbeEvent>> inputMap,
			HashMap<String, ProbeEvent[]> egroups,
			String inst) {
		
		ProbeEvent[] sub = egroups.get(egID);
		if(sub!=null && sub.length!=0) {
			String eID = sub[0].eventID;
		    
		    ArrayList<ProbeEvent> eventlist = inputMap.get(eID);
			if(eventlist!=null){
				boolean existed = false;							
				for(ProbeEvent ev: eventlist) 
				    if(ev.instanceID.equals(inst)) {	
				
				    	boolean satisfied = false; 
						for(ProbeEvent event: sub)
							if(ev.isSameValue(event)) 								
								satisfied = true;						    
				
				        if(!satisfied) {
				        	System.out.println("The following event does not satisfied the contraint!");
							System.out.println("InstanceID:" + ev.instanceID);
							System.out.println("EventID:" + ev.eventID);
							System.out.println("Data Fields:");
							for(DataField df: ev.fields) {
								System.out.println("   Name:" + df.getName());
								System.out.println("   Type:" + df.getType());
								System.out.println("   Value:" + df.getValue());																	
						    }

				        }
				        
				        existed = true;
				    } 
				    
				    if(!existed)
				        System.out.println("The event " + eID + " is not raised in instance " + inst);
			}
		}
		
	}

	private int statisticsEventNumber(String[] egIDs,
			HashMap<String, ProbeEvent[]> egroups,
			HashMap<String, ArrayList<ProbeEvent>> inputMap) {

		if(egIDs.length==0) return 0;
		
		String egID1 = egIDs[0];
		ProbeEvent[] eg1 = egroups.get(egID1);
		if(eg1==null || eg1.length==0) return 0;
		
		String eID = eg1[0].eventID; 
		ArrayList<String> allinsts = getAllInstances(eID, inputMap);
		
		int count=0;
		
		//for each instance, get all the sub events
		for(String inst: allinsts) 
			count += countEventsWithSameInstance(egIDs, inputMap, inst, egroups);	
		
		return count;
	}

	private int countEventsWithSameInstance(String[] egIDs,
			HashMap<String, ArrayList<ProbeEvent>> inputMap, String inst,
			HashMap<String, ProbeEvent[]> egroups) {
		
		int index = 0;
		while(index <egIDs.length) {
			String egID =egIDs[index];
			ProbeEvent[] eg = egroups.get(egID);
			if(eg==null || eg.length==0) return 0;
			String eID = eg[0].eventID;
			
			boolean exist = countProbeEventWithInstanceEventID(eID, inst, inputMap, eg);
			if(!exist) return 0;
						
			index++;
		}
		
		return 1;
	}

	private boolean countProbeEventWithInstanceEventID(String eID, String inst,
			HashMap<String, ArrayList<ProbeEvent>> inputMap, ProbeEvent[] eg) {
		
		ArrayList<ProbeEvent> elist = inputMap.get(eID);
		if(elist!=null) {
			for(ProbeEvent pe: elist) {
				if(inst.equals(pe.instanceID)) {
					for(ProbeEvent pg: eg)
						if(pg.isSameValue(pe)) return true;
				}
			}
		}
		
		return false;
	}

	private void constructEventCombination(ArrayList<RankRecord> rdlist,
			String eID, ArrayList<String> IDList,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap,
			HashMap<ProbeEvent[], String> combine) {
		
		//currently, we implement the combination of two events
		for(String nID: IDList) 
			constructTwoEventCombination(rdlist, eID, nID, failureMap, successMap, combine);		
	}

	private void constructTwoEventCombination(ArrayList<RankRecord> rdlist,
			String eID, String nID,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap,
			HashMap<ProbeEvent[], String> combine) {
		
		ArrayList<ProbeEvent[]> eIDsub = getCombinationEventPredicates(combine, eID);
		ArrayList<ProbeEvent[]> nIDsub = getCombinationEventPredicates(combine, nID);
		
		for(ProbeEvent[] eSub: eIDsub) 
			for(ProbeEvent[] nSub: nIDsub)
				constructTwoEventCombination(rdlist, eID, eSub, nID, nSub, failureMap, successMap);		
		
	}

	private void constructTwoEventCombination(ArrayList<RankRecord> rdlist,
			String eID, ProbeEvent[] eSub, String nID, ProbeEvent[] nSub,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {

		RankRecord rd = new RankRecord();
		rd.eventIDs = new String[2];
		rd.eventIDs[0] = eID;
		rd.eventIDs[1] = nID;
				
		int snum = statisticsTwoEventNumber(eID, eSub, nID, nSub, successMap);
		int fnum = statisticsTwoEventNumber(eID, eSub, nID, nSub, failureMap);
		
		rd.rankValue = fnum - snum;
		
		rdlist.add(rd);
	}

	private int statisticsTwoEventNumber(String eID, ProbeEvent[] eSub, String nID,
			ProbeEvent[] nSub, HashMap<String, ArrayList<ProbeEvent>> map) {

		ArrayList<ProbeEvent> elist = map.get(eID);
		ArrayList<ProbeEvent> nlist = map.get(nID);
		
        if(nlist==null || elist == null) return 0;
		
		int count = 0;
		for(ProbeEvent evt: elist)
			for(ProbeEvent nvt: nlist) {
				if(evt.instanceID.equals(nvt.instanceID)) { //from the same instance
					//both satisfy the predicates
					if(isIncludedEventInstance(eSub, evt) && isIncludedEventInstance(nSub, nvt)) count++;		
				}
			}
					
		return count;
	}

	private ArrayList<String> getCombinationEventIDs(
			HashMap<ProbeEvent[], String> combine) {

		ArrayList<String> result = new ArrayList<String>();		
		result.addAll(combine.values());
		
		return result;
	}
	
	private ArrayList<ProbeEvent[]> getCombinationEventPredicates(
			HashMap<ProbeEvent[], String> combine, String eID) {
		
		ArrayList<ProbeEvent[]> result = new ArrayList<ProbeEvent[]>();
		Set<ProbeEvent[]> keys = combine.keySet();
		for(ProbeEvent[] key: keys) {
			String value = combine.get(key);
			if(value.equals(eID)) result.add(key);
		}
		
		return result;
	}
	
	private int statisticsEventNumber(ProbeEvent[] sub,
			HashMap<String, ArrayList<ProbeEvent>> eventMap) {
		if(sub==null || sub.length==0) return 0;
		
		String eID = sub[0].eventID;
		
		return statisticsEventNumber(eID, sub, eventMap);
	}
	
	private double statisticsEventNumber(String eID, EventCluster clt,
			HashMap<String, ArrayList<ProbeEvent>> eventMap) {
		
		ProbeEvent[] sub = clt.sub;		
		if(sub==null || sub.length==0) return 0;

		ArrayList<ProbeEvent> eventlist = eventMap.get(eID);
		if(eventlist==null) return 0;
		
		double count = 0;
		
		//the following code is to handle services with loop (a single event can occur multiple times in one instance)
		HashMap<String, Integer> probability = new HashMap<String, Integer>();
		for(ProbeEvent pe: eventlist) {
			String instanceID = pe.instanceID;
			Integer pv = probability.get(instanceID);
			if(pv==null) pv = 0;
			pv++;
			probability.put(instanceID, pv);
		}
		
		for(ProbeEvent ev: eventlist) {//ev is real exposed event
			for(int i=0;i<sub.length;i++) {//event can be seemed as a predicate
				ProbeEvent event = sub[i];
				if(ev.isSameValue(event)) {
					Integer pv = probability.get(ev.instanceID);
					count += clt.prob[i] * 1.0/pv;
					break;
				}
			}
		}

		return count;				
	}
	
	private double outputSNInfo(String eID, EventCluster clt,
			HashMap<String, ArrayList<ProbeEvent>> eventMap) {
		
		ProbeEvent[] sub = clt.sub;		
		if(sub==null || sub.length==0) return 0;

		ArrayList<ProbeEvent> eventlist = eventMap.get(eID);
		if(eventlist==null) return 0;
		
		double count = 0;
		int an = 0;
		for(ProbeEvent ev: eventlist) {//ev is real exposed event
			for(int i=0;i<sub.length;i++) {//event can be seemed as a predicate
				ProbeEvent event = sub[i];
				if(ev.isSameValue(event)) {
					count += clt.prob[i];
					an++;
					System.out.print(" " + clt.prob[i]);
					break;
				}
			}
		}

		System.out.println(" = " + count + " (" + an + ")");
		return count;				
	}	

	private int statisticsEventNumber(String eID, ProbeEvent[] sub,
			HashMap<String, ArrayList<ProbeEvent>> eventMap) {
         
		ArrayList<ProbeEvent> eventlist = eventMap.get(eID);
		if(eventlist==null) return 0;
		
		int count = 0;
		for(ProbeEvent event: sub)//event can be seemed as a predicate
			for(ProbeEvent ev: eventlist) //ev is real exposed event
				//calculation the number of ev such that event => ev 
				if(ev.isSameValue(event)) count++;
		
		return count;
	}

	private boolean isIncludedEventInstance(ArrayList<ProbeEvent> eventlist, ProbeEvent event) {
		
		if(eventlist==null) return false;

		for(ProbeEvent ev: eventlist)
			if(ev.isSameValue(event)) return true;
		
		return false;
	}

	private boolean isIncludedEventInstance(ProbeEvent[] eventlist, ProbeEvent event) {

		for(ProbeEvent ev: eventlist)
			if(ev.isSameValue(event)) return true;
		
		return false;
	}
	
	//private ProbeEvent[][] eventClustering(String eID, HashMap<String, ArrayList<ProbeEvent>> failureMap, HashMap<String, ArrayList<ProbeEvent>> successMap) {
	private EventCluster[] eventClustering(String eID, HashMap<String, ArrayList<ProbeEvent>> failureMap, HashMap<String, ArrayList<ProbeEvent>> successMap) {
		
		ProbeEvent[] buffer = extractUniqueEventContentsbyID(eID, failureMap, successMap);
 		ArrayList<ProbeEvent> flist = failureMap.get(eID);
		ArrayList<ProbeEvent> slist = successMap.get(eID);
		
	//	if(eID.startsWith("LoanProcess_datamodification_Task:task7_1")) {
	//		System.out.println(eID);
	//	}
		
		HashMap<String, Integer> durations = constructDurations4Fields(buffer);
		
		switch(m_solution.clusteringPolicy) {
		case FaultLocalizationSolution.NearestNeighbour:
			 return nearestNeighbourClustering(flist, slist, buffer, 0, durations);
		case FaultLocalizationSolution.NearestNeighbourLocal:
			 return nearestNeighbourClusteringLocal(flist, slist, buffer, durations);
		case FaultLocalizationSolution.NearestNeighbourRelaxed:
			 return nearestNeighbourClustering(flist, slist, buffer, 1, durations);
		case FaultLocalizationSolution.FuzzyCluster:
			 return fuzzyClustering(flist, slist, buffer, durations); 
		case FaultLocalizationSolution.FuzzyClusterLocal:
			 return fuzzyClusteringLocal(flist, slist, buffer, durations);
		case FaultLocalizationSolution.FuzzyClusterNew:
			 return fuzzyClusteringNew(flist, slist, buffer, durations);
		//case FaultLocalizationSolution.FuzzyClusterVectorGlobal:
		//	 return fuzzyClusteringVectorGlobal(flist, slist, buffer);			 
		case FaultLocalizationSolution.ConfidenceInterval:
			 return nearestNeighbourClustering(flist, slist, buffer, 2, durations);
		case FaultLocalizationSolution.NearestNeighbourDensity:
			 return nearestNeighbourClustering(flist, slist, buffer, 3, durations);
		case FaultLocalizationSolution.NearestNeighbourMH:
			 return nearestNeighbourClusteringMH(flist, slist, buffer);			 
		case FaultLocalizationSolution.NearestNeighbourMHFuzzy:	 
			 return nearestNeighbourClusteringMHFuzzy(flist, slist, buffer);
		case FaultLocalizationSolution.NearestNeighbourMHFuzzyIntegration:	 
			 return nearestNeighbourClusteringMHFuzzyIntegration(flist, slist, buffer);	 
			 
		default:;
		}
		
		return null;
	}
	

	private HashMap<String, Integer> constructDurations4Fields(
			ProbeEvent[] buffer) {
		
		HashMap<String, Integer> min = new HashMap<String, Integer>();
		HashMap<String, Integer> max = new HashMap<String, Integer>();
		
		for(ProbeEvent pb: buffer) {
			ArrayList<DataField> fields = pb.getDataField();
			for(DataField fd: fields) {
				String name = fd.getName();
				String type = fd.getType();
				if(type.equals(DataType.INTEGER)) {
					Integer value = (Integer)fd.getValue();
					Integer minvalue = min.get(name);
					if(minvalue==null || minvalue > value) min.put(name, value);
					
					Integer maxvalue = max.get(name);
					if(maxvalue==null || maxvalue < value) max.put(name, value);											
				}
			}
		}
		
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		Set<String> keys = min.keySet();
		for(String key: keys) {
			Integer minvalue = min.get(key);
			Integer maxvalue = max.get(key);
			Integer du = maxvalue - minvalue;
			result.put(key, du);
		}
		
		return result;
	}

	private EventCluster[] fuzzyClusteringNew(ArrayList<ProbeEvent> flist,
			ArrayList<ProbeEvent> slist, ProbeEvent[] buffer, HashMap<String, Integer> durations) {
		EventCluster[] result = new EventCluster[1];
		result[0] = new EventCluster();
		result[0].sub = new ProbeEvent[buffer.length];
		result[0].prob = new double[buffer.length];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();		
		
		
		//1. add all failure event into the cluster				
		int index = 0;
		for(ProbeEvent evt: buffer)
			//if(isIncludedEventInstance(flist, evt)) failure_cluster.add(evt);
			if(isIncludedEventInstance(flist, evt)) {
				failure_cluster.add(evt);
				//result[0].sub[index] = evt;
				//result[0].prob[index] = 1; //assign 1 for all failed events
				//index++;
			}
		
		//2. calculate the center
		ClusterRadius crds = calculateClusterRadius(failure_cluster, 1, durations);
		
		for(ProbeEvent evt: buffer) {
			//if(failure_cluster.contains(evt)) continue;						

			double distance = crds.center!=null?calculateEventDistance(evt, crds.center, durations):0;
			
			double pv = distance> crds.radius ? 1 - distance + crds.radius:1;
			assert(pv<=1);
			assert(pv>=0);
			
			result[0].sub[index] = evt;
			result[0].prob[index] = pv; 
			index++;
		}
		
		return result;	

	}

	private EventCluster[] fuzzyClusteringLocal(ArrayList<ProbeEvent> flist,
			ArrayList<ProbeEvent> slist, ProbeEvent[] buffer, HashMap<String, Integer> durations) {
		EventCluster[] result = new EventCluster[1];
		result[0] = new EventCluster();
		result[0].sub = new ProbeEvent[buffer.length];
		result[0].prob = new double[buffer.length];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
		//1. add all failure event into the cluster
		int index = 0;
		for(ProbeEvent evt: buffer)
			if(isIncludedEventInstance(flist, evt)) {
				failure_cluster.add(evt);
				result[0].sub[index] = evt;
				result[0].prob[index] = 1; //assign 1 for all failed events
				index++;
			}
		
		//2. add all the others that are nearest to the events in the cluster
		//double max = calculateMaxDistance(buffer);
		//HashMap<String, Double> minDistances = calculateMinDistance(failure_cluster, durations);
		for(ProbeEvent evt: buffer) {
			if(failure_cluster.contains(evt)) continue;
							
			ProbeEvent neighbour = calculateNearestNeightbour(evt, failure_cluster, durations);
			
			result[0].sub[index] = evt;
			//result[0].prob[index] = 1;
			
			if(neighbour==null) 
				result[0].prob[index] = 1;
			else {
				double dist = calculateEventDistance(evt, neighbour, durations);
				//double min = minDistances.get(neighbour.eventID);
			    //result[0].prob[index] = dist> min ? min/dist: 1;
				int fnum = countSameContentNum(flist, neighbour);
				int snum = countSameContentNum(slist, neighbour);
				
				result[0].prob[index] = (1 - dist) * (snum+1) / (fnum + snum + 1) ;
				assert(dist<=1);
			}
			
			index++;
			
		}
		
		return result;	
	}
	
	private EventCluster[] fuzzyClusteringVectorGlobal(ArrayList<ProbeEvent> flist,
			ArrayList<ProbeEvent> slist, ProbeEvent[] buffer) {
		EventCluster[] result = new EventCluster[1];
		result[0] = new EventCluster();
		result[0].sub = new ProbeEvent[buffer.length];
		result[0].prob = new double[buffer.length];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
		//1. add all failure event into the cluster
		int index = 0;
		for(ProbeEvent evt: buffer)
			if(isIncludedEventInstance(flist, evt)) {
				failure_cluster.add(evt);
				result[0].sub[index] = evt;
				result[0].prob[index] = 1; //assign 1 for all failed events
				index++;
			}
		
		//2. add all the others that are nearest to the events in the cluster
		//double max = calculateMaxDistance(buffer);
		//HashMap<String, Double> minDistances = calculateMinDistance(failure_cluster, durations);
		for(ProbeEvent evt: buffer) {
			if(failure_cluster.contains(evt)) continue;
							
			ArrayList<EventVector> evlist = createEventVectors(evt, failure_cluster);			
			
			result[0].sub[index] = evt;						
     		result[0].prob[index] = calculateDotValues(evlist);
     					
			index++;
			
		}
		
		return result;	
	}	

	private double calculateDotValues(ArrayList<EventVector> evlist) {

		double result = 0;
		for(EventVector v1: evlist)
			for(EventVector v2: evlist)
				result += EventVector.dotValue(v1, v2);
		
		int size = evlist.size();
		result = size>0 ? result/(size * size): -1;
		
		//normalize the value to probability (0..1)	
		assert(result<=1 && result>=-1);
		
		return (1 - result)/2;
	}

	private ArrayList<EventVector> createEventVectors(ProbeEvent evt,
			ArrayList<ProbeEvent> failure_cluster) {

		ArrayList<EventVector> vlist = new ArrayList<EventVector>();
		
		for(ProbeEvent fe: failure_cluster) {
			EventVector vector = EventVector.constructEventVector(evt, fe);
			if(vector!=null) vlist.add(vector);
		}
			
		return vlist;
	}

	private int countSameContentNum(ArrayList<ProbeEvent> flist, ProbeEvent evt) {

		int num = 0;
		for(ProbeEvent pb: flist)
			if(pb.isSameValue(evt)) num++;
		
		return num;
	}

	private EventCluster[] fuzzyClustering(ArrayList<ProbeEvent> flist,
			ArrayList<ProbeEvent> slist, ProbeEvent[] buffer, HashMap<String, Integer> durations) {

		EventCluster[] result = new EventCluster[1];
		result[0] = new EventCluster();
		result[0].sub = new ProbeEvent[buffer.length];
		result[0].prob = new double[buffer.length];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();		
		//1. add all failure event into the cluster
		int index = 0;
		for(ProbeEvent evt: buffer)
			//if(isIncludedEventInstance(flist, evt)) failure_cluster.add(evt);
			if(isIncludedEventInstance(flist, evt)) {
				failure_cluster.add(evt);
				result[0].sub[index] = evt;
				result[0].prob[index] = 1; //assign 1 for all failed events
				index++;
			}
		
		//2. calculate the center
		ClusterRadius crds = calculateClusterRadius(failure_cluster, 1, durations);
		double max = 0;
		for(ProbeEvent evt: buffer) {
			double distance = crds.center!=null? calculateEventDistance(evt, crds.center, durations):0;
			if(max<distance) max = distance;
		}		
		
		for(ProbeEvent evt: buffer) {
			if(failure_cluster.contains(evt)) continue;						
			//if(isNeighbour(evt, failure_cluster, minDistances)) neighbours.add(evt);
			//if(isNeighbour(evt, crds)) neighbours.add(evt);
			double distance = crds.center!=null?calculateEventDistance(evt, crds.center, durations):0;							
			result[0].sub[index] = evt;
			result[0].prob[index] = max>0 ? 1 - distance/max: 1; 
			index++;
		}
		
		return result;	

	}

	//private ProbeEvent[][] nearestNeighbourClusteringLocal(
	private EventCluster[] nearestNeighbourClusteringLocal(
			ArrayList<ProbeEvent> failureList,
			ArrayList<ProbeEvent> successList, 
			ProbeEvent[] buffer, HashMap<String, Integer> durations) {

		ProbeEvent[][] result = new ProbeEvent[1][];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
		ArrayList<ProbeEvent> neighbours = new ArrayList<ProbeEvent>();
		//1. add all failure event into the cluster
		for(ProbeEvent evt: buffer)
			if(isIncludedEventInstance(failureList, evt)) failure_cluster.add(evt);
		
		//2. add all the others that are nearest to the events in the cluster
		HashMap<String, Double> minDistances = calculateMinDistance(failure_cluster, durations);		
		for(ProbeEvent evt: buffer) {
			if(failure_cluster.contains(evt)) continue;						
			if(isNeighbourLocal(evt, failure_cluster, minDistances, durations)) neighbours.add(evt);
		}
		
		//3. combine failure_cluster and neighbour
		neighbours.addAll(failure_cluster);
		ProbeEvent[] cl = new ProbeEvent[neighbours.size()];
		cl = neighbours.toArray(cl);
		result[0] = cl;
		
		EventCluster[] clusters = new EventCluster[1];
		clusters[0] = new EventCluster();
		clusters[0].sub = result[0];
		clusters[0].prob = new double[result[0].length];
		for(int i=0;i<clusters[0].sub.length;i++)
			clusters[0].prob[i] = 1;
		
		//return result;
		//if clusters is empty, then put all the events into the cluster, meaning that all are in the group.
		if(clusters[0].sub.length==0) {
			clusters[0].sub = buffer;
			clusters[0].prob = new double[buffer.length];
			for(int i=0;i<clusters[0].sub.length;i++)
				clusters[0].prob[i] = 1;
		}
		
		return clusters;	
	}
	
	//private ProbeEvent[][] nearestNeighbourClustering(
	private EventCluster[] nearestNeighbourClustering(
			ArrayList<ProbeEvent> failureList,
			ArrayList<ProbeEvent> successList, 
			ProbeEvent[] buffer, int isrelaxed, 
			HashMap<String, Integer> durations) {

		ProbeEvent[][] result = new ProbeEvent[1][];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
		ArrayList<ProbeEvent> neighbours = new ArrayList<ProbeEvent>();
		//1. add all failure event into the cluster
		if(isrelaxed == 3) {
			if (failureList!=null) failure_cluster.addAll(failureList);
		} else
		    for(ProbeEvent evt: buffer)
			    if(isIncludedEventInstance(failureList, evt)) failure_cluster.add(evt);
		
		//2. add all the others that are nearest to the events in the cluster
		//HashMap<String, Double> minDistances = calculateMinDistance(failure_cluster);
		int bfn=0, bsn=0;
		ArrayList<ProbeEvent> extra = new ArrayList<ProbeEvent>();
		ClusterRadius crds = calculateClusterRadius(failure_cluster, isrelaxed, durations);
		for(ProbeEvent evt: buffer) {
			if(isIncludedEventInstance(failureList, evt)) { 
				neighbours.add(evt);
				bfn++;
			} else 
			    if(isNeighbour(evt, crds, durations)) {
			    	neighbours.add(evt);
			    	bsn++;
			    } else
			    	extra.add(evt);
			
		}
		
		//extend the radius based on the ratio (bsn/(bsn+bfn))
		double ratio = bsn + bfn > 0 ? bsn *1.0/(bsn + bfn) + 1 : 1;
		crds.radius *=  ratio;
		
		for(ProbeEvent evt: extra) {
			if(isNeighbour(evt, crds, durations)) 
		    	neighbours.add(evt);
		}
		
		//3. combine failure_cluster and neighbour
		//neighbours.addAll(failure_cluster);
		ProbeEvent[] cl = new ProbeEvent[neighbours.size()];
		cl = neighbours.toArray(cl);
		result[0] = cl;
		
		EventCluster[] clusters = new EventCluster[1];
		clusters[0] = new EventCluster();
		clusters[0].sub = result[0];
		clusters[0].prob = new double[result[0].length];
		for(int i=0;i<clusters[0].sub.length;i++)
			clusters[0].prob[i] = 1;
		
		//return result;
		//if clusters is empty, then put all the events into the cluster, meaning that all are in the group.
		if(clusters[0].sub.length==0) {
			clusters[0].sub = buffer;
			clusters[0].prob = new double[buffer.length];
			for(int i=0;i<clusters[0].sub.length;i++)
				clusters[0].prob[i] = 1;
		}
		
		return clusters;	

	}
	
	//private static int maxfn = 0;
	private EventCluster[] nearestNeighbourClusteringMH(
			ArrayList<ProbeEvent> failureList,
			ArrayList<ProbeEvent> successList, 
			ProbeEvent[] buffer) {

		ProbeEvent[][] result = new ProbeEvent[1][];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
		ArrayList<ProbeEvent> neighbours = new ArrayList<ProbeEvent>();
	    for(ProbeEvent evt: buffer)
			if(isIncludedEventInstance(failureList, evt)) {
				failure_cluster.add(evt);
				neighbours.add(evt);
			}
		
		//2. add all the others that are nearest to the events in the cluster
		//HashMap<String, Double> minDistances = calculateMinDistance(failure_cluster);
	    ProbeEvent center = EventVector.calculateCenter(failure_cluster);
	    if(center!=null && failure_cluster.size()>1) {
	    	ArrayList<Integer> zlist = new ArrayList<Integer>();
	    	double[][] cm = EventVector.constructCovarianceMatrix(failure_cluster, center, zlist);
	    	Matrix cmt = new Matrix(cm);
	    	Matrix inverseCMT = null;
	    	if(Math.abs(cmt.det())<0.000001)
	    		inverseCMT = Matrix.identity(cmt.getColumnDimension(), cmt.getRowDimension());
	    	else	
	    	    inverseCMT = cmt.inverse();
	    	double sd = EventVector.calculateSD(failure_cluster, center, inverseCMT, zlist);	    	    
			
	    	//boolean haspassed = false;
	    	for(ProbeEvent evt: buffer) {
	    		if(!isIncludedEventInstance(failureList, evt)) { 	    							
	    			double distance = EventVector.calculateMHDistance(inverseCMT, center, evt, zlist);
	    			//if(distance <= sd * 4) {
	    			if(distance <= sd) {
	    				neighbours.add(evt);
	    				//haspassed = true;
	    			} 
	    		}			
	    	}
	    	
	    	//for debugging
	    	/*
	    	if(!haspassed && neighbours.size()>=maxfn) {
	    		maxfn = neighbours.size();
	    		ProbeEvent spe = neighbours.get(0);
	    		System.out.print("" + neighbours.size() + ":");
	    		for(DataField df: spe.fields) {
	    			String vname = df.getName();
	    			int fi = vname.lastIndexOf(":");
	    			System.out.print(vname.substring(fi+1) + ", ");	    			
	    		}
	    		System.out.println();
	    	}*/
	    }
			
		//3. combine failure_cluster and neighbour
		ProbeEvent[] cl = new ProbeEvent[neighbours.size()];
		cl = neighbours.toArray(cl);
	    result[0] = cl;		
		
		EventCluster[] clusters = new EventCluster[1];
		clusters[0] = new EventCluster();
		clusters[0].sub = result[0];
		clusters[0].prob = new double[result[0].length];
		for(int i=0;i<clusters[0].sub.length;i++)
			clusters[0].prob[i] = 1;
						
		//if clusters is empty, then put all the events into the cluster, meaning that all are in the group.
		if(clusters[0].sub.length==0) {
			clusters[0].sub = buffer;
			clusters[0].prob = new double[buffer.length];
			for(int i=0;i<clusters[0].sub.length;i++)
				clusters[0].prob[i] = 1;
		}
		
		return clusters;	

	}
	
	private EventCluster[] nearestNeighbourClusteringMHFuzzy(
			ArrayList<ProbeEvent> failureList,
			ArrayList<ProbeEvent> successList, 
			ProbeEvent[] buffer) {
		
		EventCluster[] clusters = new EventCluster[1];
		clusters[0] = new EventCluster();
		clusters[0].sub = buffer;
		clusters[0].prob = new double[buffer.length];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
	    for(ProbeEvent evt: buffer)
			if(isIncludedEventInstance(failureList, evt)) 
				failure_cluster.add(evt);			
		
		//2. add all the others that are nearest to the events in the cluster		    
	    ProbeEvent center = EventVector.calculateCenter(failure_cluster);
	    if(center!=null) {
	    	ArrayList<Integer> zlist = new ArrayList<Integer>();
	    	double[][] cm = EventVector.constructCovarianceMatrix(failure_cluster, center, zlist);
	    	Matrix cmt = new Matrix(cm);
	    	Matrix inverseCMT = null;
	    	if(Math.abs(cmt.det())<0.000001)
	    		inverseCMT = Matrix.identity(cmt.getColumnDimension(), cmt.getRowDimension());
	    	else	
	    	    inverseCMT = cmt.inverse();
	    	double sd = EventVector.calculateSD(failure_cluster, center, inverseCMT, zlist);	    	    
	    	
	    	for(int i=0;i<buffer.length;i++) {
	    		ProbeEvent evt = buffer[i];
	    		if(!isIncludedEventInstance(failureList, evt)) { 	    							
	    			double distance = EventVector.calculateMHDistance(inverseCMT, center, evt, zlist);
	    			//if(distance <= sd * 4) {
	    			if(distance <= sd) 
	    				clusters[0].prob[i] = 1;
	    			 else 
	    				clusters[0].prob[i] = sd / distance;	    			
	    			
	    		} else
	    			clusters[0].prob[i] = 1;
	    	}
	    	
	    } else {			
		     //if clusters is empty, then put all the events into the cluster, meaning that all are in the group.		
			for(int i=0;i<clusters[0].sub.length;i++)
				clusters[0].prob[i] = 1;		
	    }
		
		return clusters;	

	}	

	private EventCluster[] nearestNeighbourClusteringMHFuzzyIntegration(
			ArrayList<ProbeEvent> failureList,
			ArrayList<ProbeEvent> successList, 
			ProbeEvent[] buffer) {
		
		EventCluster[] clusters = new EventCluster[1];
		clusters[0] = new EventCluster();
		clusters[0].sub = buffer;
		clusters[0].prob = new double[buffer.length];
		
		ArrayList<ProbeEvent> failure_cluster = new ArrayList<ProbeEvent>();
	    for(ProbeEvent evt: buffer)
			if(isIncludedEventInstance(failureList, evt)) 
				failure_cluster.add(evt);			
		
		//2. add all the others that are nearest to the events in the cluster		    
	    ProbeEvent center = EventVector.calculateCenter(failure_cluster);
	    if(center!=null) {
	    	ArrayList<Integer> zlist = new ArrayList<Integer>();
	    	double[][] cm = EventVector.constructCovarianceMatrix(failure_cluster, center, zlist);
	    	Matrix cmt = new Matrix(cm);
	    	Matrix inverseCMT = null;
	    	if(Math.abs(cmt.det())<0.000001)
	    		inverseCMT = Matrix.identity(cmt.getColumnDimension(), cmt.getRowDimension());
	    	else	
	    	    inverseCMT = cmt.inverse();
	    	double sd = EventVector.calculateSD(failure_cluster, center, inverseCMT, zlist);	    	    
	    	
	    	for(int i=0;i<buffer.length;i++) {
	    		ProbeEvent evt = buffer[i];
	    		if(!isIncludedEventInstance(failureList, evt)) { 	    							
	    			double distance = EventVector.calculateMHDistance(inverseCMT, center, evt, zlist);
	    			
	    			clusters[0].prob[i] = 1 - MathUtils.normalIntegration(0, sd, -distance, distance);	    			    			
	    			
	    		} else
	    			clusters[0].prob[i] = 1;
	    	}
	    	
	    } else {			
		     //if clusters is empty, then put all the events into the cluster, meaning that all are in the group.		
			for(int i=0;i<clusters[0].sub.length;i++)
				clusters[0].prob[i] = 1;		
	    }
		
		return clusters;	

	}	
	
	private ClusterRadius calculateClusterRadius(
			ArrayList<ProbeEvent> failure_cluster, int type, 
			HashMap<String, Integer> durations) {
		
		ClusterRadius result = new ClusterRadius();	
		result.radius = 0;
		result.center = null;
		
		int fnum = failure_cluster.size();		
		double distance = 100000000;
		double gmax = 0;
		int eventnum = 1;
				
		for(int i=0;i<fnum;i++) {
			ProbeEvent fevent = failure_cluster.get(i);			
			
			double avagedistance = 0;
			double standarddeviation = 0;
						
			double maxdistance = 0;
			for(int j=0;j<fnum;j++) {
				if(i==j) continue;
				
				ProbeEvent nevent = failure_cluster.get(j);
				double dt = calculateEventDistance(fevent, nevent, durations);
				
				avagedistance += dt;
				if(maxdistance<dt) maxdistance = dt;
			}
			
			if(gmax<maxdistance) gmax = maxdistance;
			
			avagedistance = (fnum>1) ? avagedistance / (fnum-1):0;
							
			if(avagedistance < distance) {
				
				//calculate standard deviation
				for(int j=0;j<fnum;j++) {
					if(i==j) continue;
					
					ProbeEvent nevent = failure_cluster.get(j);
					double dt = calculateEventDistance(fevent, nevent, durations);
					
					standarddeviation += (dt - avagedistance) * (dt - avagedistance);
				}
				
				standarddeviation = fnum>2? Math.sqrt(standarddeviation/(fnum-2)): 0;
				double ci = fnum>2? 1.96 * standarddeviation / Math.sqrt(fnum-1): 0; 
				ci = ci + avagedistance;
				
				double sd = fnum>2?standarddeviation: avagedistance;
				
			    result.center = fevent;
			    switch(type) {
			    case 0: result.radius = avagedistance; break;
			    case 1: result.radius = maxdistance; break;
			    //case 2: result.radius = ci;break;
			    case 3: result.radius = maxdistance; break;
			    case 2:
			    case 4: result.radius = sd;break;
			    }
			    distance = avagedistance;
			   
			   
			}
		}
		
		
		//if(result.center!=null && type==2 && result.radius < gmax)
		//	result.radius = gmax;
		
		//debugging
		//result.radius = 1000000;
		if(result.center!=null) {
			eventnum = result.center.fields.size();
			result.radius = Math.sqrt(eventnum) * result.radius;
		}
				
		return result;
	}

/*	
	private ClusterRadius calculateClusterRadius2(
			ArrayList<ProbeEvent> failure_cluster, int type) {
		
		ClusterRadius result = new ClusterRadius();	
		result.radius = 0;
		result.center = new ProbeEvent();
		
		//calculate the centroid of the cluster
		boolean intial = false;
		for(ProbeEvent pe: failure_cluster) {
			if(!intial) {
				result.center.eventID = pe.eventID;
				result.center.eventToken = pe.eventToken;
				result.center.serviceName =  pe.serviceName;
				result.center.fields = new ArrayList<DataField>();
				//result.center.
			}
		}
		
		int fnum = failure_cluster.size();		
		double distance = 100000000;
		double gmax = 0;
		int eventnum = 1;
				
		for(int i=0;i<fnum;i++) {
			ProbeEvent fevent = failure_cluster.get(i);			
			
			double avagedistance = 0;
			double standarddeviation = 0;
						
			double maxdistance = 0;
			for(int j=0;j<fnum;j++) {
				if(i==j) continue;
				
				ProbeEvent nevent = failure_cluster.get(j);
				double dt = calculateEventDistance(fevent, nevent);
				
				avagedistance += dt;
				if(maxdistance<dt) maxdistance = dt;
			}
			
			if(gmax<maxdistance) gmax = maxdistance;
			
			avagedistance = (fnum>1) ? avagedistance / (fnum-1):0;
							
			if(avagedistance < distance) {
				
				//calculate standard deviation
				for(int j=0;j<fnum;j++) {
					if(i==j) continue;
					
					ProbeEvent nevent = failure_cluster.get(j);
					double dt = calculateEventDistance(fevent, nevent);
					
					standarddeviation += (dt - avagedistance) * (dt - avagedistance);
				}
				
				standarddeviation = fnum>2? Math.sqrt(standarddeviation/(fnum-2)): 0;
				double ci = fnum>2? 1.96 * standarddeviation / Math.sqrt(fnum-1): 0; 
				ci = ci + avagedistance;
				
				double sd = fnum>2?standarddeviation: avagedistance;
				
			    result.center = fevent;
			    switch(type) {
			    case 0: result.radius = avagedistance; break;
			    case 1: result.radius = maxdistance; break;
			    //case 2: result.radius = ci;break;
			    case 3: result.radius = maxdistance; break;
			    case 2:
			    case 4: result.radius = sd;break;
			    }
			    distance = avagedistance;
			   
			   
			}
		}
		
		
		//if(result.center!=null && type==2 && result.radius < gmax)
		//	result.radius = gmax;
		
		//debugging
		//result.radius = 1000000;
		if(result.center!=null) {
			eventnum = result.center.fields.size();
			result.radius = Math.sqrt(eventnum) * result.radius;
		}
				
		return result;
	}	
*/
	
	private boolean isNeighbour(ProbeEvent evt, ClusterRadius crds, HashMap<String, Integer> durations) {

		if(crds.center==null) return false;
		double distance = calculateEventDistance(evt, crds.center, durations);							
		return distance < crds.radius;						
	}
/*	
	private double calculateMaxDistance(
			ProbeEvent[] buffer) {
		double max = 0;		
		for(int i=0;i<buffer.length;i++) {
			ProbeEvent pe1 = buffer[i];
			for(int j=i+1;j<buffer.length;j++) {
				ProbeEvent pe2 = buffer[j];
				double distance = calculateEventDistance(pe1, pe2);
				if(distance>max) max = distance;
			}
		}
		
		return max;
	}
*/	
	private ProbeEvent calculateNearestNeightbour(
			ProbeEvent pe, ArrayList<ProbeEvent> failure_cluster, HashMap<String, Integer> durations) {
		
		if(failure_cluster.isEmpty()) return null;
		
		ProbeEvent result = null;
		
		double min = 1000000;		
		
		for(ProbeEvent fe: failure_cluster) {
			double distance = calculateEventDistance(pe, fe, durations);
			if(distance<min) {
				min = distance;
				result = fe;
			}
		}
		
		return result;
	}

/*	
	private double calculateMinDistanceToCluster(
			ProbeEvent pe, ArrayList<ProbeEvent> failure_cluster) {
		
		if(failure_cluster.isEmpty()) return 0;
		
		double min = 1000000;		
		
		for(ProbeEvent fe: failure_cluster) {
			double distance = calculateEventDistance(pe, fe);
			if(distance<min) min = distance;
		}
		
		return min;
	}
*/
	private HashMap<String, Double> calculateMinDistance(
			ArrayList<ProbeEvent> failure_cluster, HashMap<String, Integer> durations) {
		
		HashMap<String, Double> result = new HashMap<String, Double>();		
		int fnum = failure_cluster.size();
		
		for(int i=0;i<fnum;i++) {
			ProbeEvent fevent = failure_cluster.get(i);
			double distance = 100000000;
			boolean hasValidDistance = false;
			
			for(int j=0;j<fnum;j++) {
				if(i==j) continue;
				
				hasValidDistance = true;
				ProbeEvent nevent = failure_cluster.get(j);
				double dt = calculateEventDistance(fevent, nevent, durations);
				
				if(dt<distance) distance = dt;
			}
				
			if(!hasValidDistance) distance = 0;
			
			String eID = fevent.getEventID();
			distance  = distance * Math.sqrt(fevent.fields.size());
			result.put(eID, distance);
		}
		
		return result;
	}

	private boolean isNeighbourLocal(ProbeEvent evt, ArrayList<ProbeEvent> failure_cluster, HashMap<String, Double> minDistances, HashMap<String, Integer> durations) {
		
		double min = 1000000;
		ProbeEvent selected=null;
		for(ProbeEvent fevent: failure_cluster) {			
			double distance = calculateEventDistance(evt, fevent, durations);
			if(min> distance) {
				min = distance;
				selected = fevent;
			}
		}
		
		if(selected!=null) {
			String eID = selected.getEventID();
			Double dt = minDistances.get(eID);
			if(dt!=null && min < dt) return true;
		}
		
		return false;
	}
/*	
	private boolean isNeighbour(ProbeEvent evt, ArrayList<ProbeEvent> failure_cluster, HashMap<String, Double> minDistances) {
				
		for(ProbeEvent fevent: failure_cluster) {			
			double distance = calculateEventDistance(evt, fevent);			
			String eID = fevent.getEventID();
			Double dt = minDistances.get(eID);
			if(dt!=null && distance < dt) return true;
		}
		
		return false;
	}
*/
	private double calculateEventDistance(ProbeEvent evt, ProbeEvent fevent, HashMap<String, Integer> durations) {
				
		return evt.calculateEventDistance(fevent, durations);
	}

	private ProbeEvent[] extractUniqueEventContentsbyID(String ID, HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap) {

		ArrayList<ProbeEvent> buffer = new ArrayList<ProbeEvent>();
		
		ArrayList<ProbeEvent> faillist = failureMap.get(ID);
		if(faillist!=null) {
			for(ProbeEvent evt: faillist) {
				if(!isIncludedEventInstance(buffer, evt)) buffer.add(evt);
			}	
		}
		
		ArrayList<ProbeEvent> succlist = successMap.get(ID);
		if(succlist!=null) {
			for(ProbeEvent evt: succlist) {
				if(!isIncludedEventInstance(buffer, evt)) buffer.add(evt);
			}	
		}
		
		ProbeEvent[] result = new ProbeEvent[buffer.size()];
		result = buffer.toArray(result);
		return result;
	}

	public double getStatisticsSDNew(String eID, HashMap<String, ArrayList<ProbeEvent>> failureMap, HashMap<String, ArrayList<ProbeEvent>> successMap) {
		int fn1 = 0, fn2 = 0;
		int sn1 = 0, sn2 = 0;
		double dr1, dr2;
		
		ArrayList<ProbeEvent> ffe_list = failureMap.get(eID);
		
		filterSameInstanceEvents(ffe_list);
		if(ffe_list!=null)
			for(ProbeEvent pe: ffe_list) {
				DataField df = pe.fields.get(0);
				Boolean value = (Boolean) df.getValue();
				if(value) 
					fn1++;
				else
					fn2++;
			}
		
		ArrayList<ProbeEvent> sfe_list = successMap.get(eID);
		filterSameInstanceEvents(sfe_list);
		if(sfe_list!=null)
			for(ProbeEvent pe: sfe_list) {
				DataField df = pe.fields.get(0);
				Boolean value = (Boolean) df.getValue();
				if(value) 
					sn1++;
				else
					sn2++;			
			}

		dr1 = fn1 + sn1> 0? fn1 * 1.0/(fn1 + sn1):0;
		dr2 = (fn1 + fn2)/(fn1 + fn2 + sn1 + sn2);
		
		double increase = dr1 - dr2;
		
		//added on 2016.03.10 to filter predicates
		if(increase<0) return -1;
		
		double sens1 = fn1 >0 ? Math.log(fn1):0;
		double sens2 = numFailed >0 ? Math.log(numFailed):0;
		double sensitivity = sens2 !=0 ? sens1 / sens2 : 0;
		
		if(increase==0 || sensitivity ==0) return 0;
		
		double dr = 1/increase + 1/sensitivity;
		if(dr==0) return 0;
		
		return 2/dr;				
	}
	
	private void filterSameInstanceEvents(ArrayList<ProbeEvent> event_list) {
        if(event_list!=null) {
        	ArrayList<ProbeEvent> removed = new ArrayList<ProbeEvent>();
        	HashMap<String, ProbeEvent> buffer = new HashMap<String, ProbeEvent>();
        	for(ProbeEvent pe: event_list) {
        		String instanceID = pe.instanceID;
        		ProbeEvent saved = buffer.get(instanceID);
        		if(saved==null) {
        			buffer.put(instanceID, pe);
        		} else {
        			DataField df1 = saved.fields.get(0);
        			Boolean value1 = (Boolean) df1.getValue();
        			
        			DataField df2 = pe.fields.get(0);
        			Boolean value2 = (Boolean) df2.getValue();
        			
        			if(value1 || !value2) 
        				removed.add(pe); 
        			else {
        				removed.add(saved);
        				buffer.put(instanceID, pe);
        			}
        		}
        	}
        	
        	event_list.removeAll(removed);
        }
		
	}

	private double getStatisticsSDNewCB(String eID1, String eID2,
			HashMap<String, ArrayList<ProbeEvent>> failureMap,
			HashMap<String, ArrayList<ProbeEvent>> successMap, boolean type) {

		int fn1 = 0, fn2 = 0;
		int sn1 = 0, sn2 = 0;
		double dr1, dr2;
		
		ArrayList<ProbeEvent[]> ffe_list = getCBEventList(eID1, eID2, failureMap);
		filterSameInstanceCBEvents(ffe_list, type);
		if(ffe_list!=null)
			for(ProbeEvent[] pe: ffe_list) {
				DataField df1 = pe[0].fields.get(0);
				DataField df2 = pe[1].fields.get(0);
				Boolean value1 = (Boolean) df1.getValue();
				Boolean value2 = (Boolean) df2.getValue();
				Boolean value = type? value1 && value2 : value1 || value2;
				if(value) 
					fn1++;
				else
					fn2++;
			}
		
		ArrayList<ProbeEvent[]> sfe_list = getCBEventList(eID1, eID2, successMap);
		filterSameInstanceCBEvents(sfe_list, type);
		if(sfe_list!=null)
			for(ProbeEvent[] pe: sfe_list) {
				DataField df1 = pe[0].fields.get(0);
				DataField df2 = pe[1].fields.get(0);
				Boolean value1 = (Boolean) df1.getValue();
				Boolean value2 = (Boolean) df2.getValue();
				Boolean value = type? value1 && value2 : value1 || value2;
				if(value) 
					sn1++;
				else
					sn2++;			
			}

		dr1 = fn1 + sn1> 0? fn1 * 1.0/(fn1 + sn1):0;
		dr2 = fn1 + fn2 + sn1 + sn2 >0 ? (fn1 + fn2)/(fn1 + fn2 + sn1 + sn2) : 0;
		
		double increase = dr1 - dr2;
		
		double sens1 = fn1 >0 ? Math.log(fn1):0;
		double sens2 = numFailed >0 ? Math.log(numFailed):0;
		double sensitivity = sens2 !=0 ? sens1 / sens2 : 0;
		
		if(increase==0 || sensitivity ==0) return 0;
		
		double dr = 1/increase + 1/sensitivity;
		if(dr==0) return 0;
		
		return 2/dr;						
	}

	
	private void filterSameInstanceCBEvents(ArrayList<ProbeEvent[]> event_list, Boolean type) {
        if(event_list!=null) {
        	ArrayList<ProbeEvent[]> removed = new ArrayList<ProbeEvent[]>();
        	HashMap<String, ProbeEvent[]> buffer = new HashMap<String, ProbeEvent[]>();
        	for(ProbeEvent[] pe: event_list) {
        		String instanceID = pe[0].instanceID;
        		ProbeEvent[] saved = buffer.get(instanceID);
        		if(saved==null) {
        			buffer.put(instanceID, pe);
        		} else {
        			DataField df11 = saved[0].fields.get(0);
        			Boolean value11 = (Boolean) df11.getValue();
        			DataField df12 = saved[1].fields.get(0);
        			Boolean value12 = (Boolean) df12.getValue();
        			
        			DataField df21 = pe[0].fields.get(0);
        			Boolean value21 = (Boolean) df21.getValue();
        			DataField df22 = pe[1].fields.get(0);
        			Boolean value22 = (Boolean) df22.getValue();
        			
        			Boolean value1 = type? value11 && value12 : value11 || value12;
        			Boolean value2 = type? value21 && value22 : value21 || value22;
        			if(value1 || !value2) 
        				removed.add(pe); 
        			else {
        				removed.add(saved);
        				buffer.put(instanceID, pe);
        			}
        		}
        	}
        	
        	event_list.removeAll(removed);
        }		
	}

	private ArrayList<ProbeEvent[]> getCBEventList(String eID1, String eID2,
			HashMap<String, ArrayList<ProbeEvent>> map) {

		ArrayList<ProbeEvent[]> result = new ArrayList<ProbeEvent[]>();
		
		ArrayList<ProbeEvent> elist1 = map.get(eID1);
		ArrayList<ProbeEvent> elist2 = map.get(eID2);
		if(elist1==null || elist2 == null) return result;
		
		for(ProbeEvent pe1: elist1)
			for(ProbeEvent pe2: elist2)
				if(pe1.instanceID.equals(pe2.instanceID)) {
					ProbeEvent[] cpe = new ProbeEvent[2];
					cpe[0] = pe1;
					cpe[1] = pe2;
					result.add(cpe);
				}
		
		return result;
	}

	public double getStatisticsSD(String eID, HashMap<String, ArrayList<ProbeEvent>> failureMap, HashMap<String, ArrayList<ProbeEvent>> successMap) {
		int fn1;
		int sn1;
		double dr1;
		
		ArrayList<ProbeEvent> ffe_list = failureMap.get(eID);
		fn1 = (ffe_list!=null)?ffe_list.size():0;
		
		ArrayList<ProbeEvent> sfe_list = successMap.get(eID);
		sn1 = (sfe_list!=null)?sfe_list.size():0;
		
		dr1 = fn1 * 1.0/(fn1 + sn1);
		
		int fn2;
		int sn2;
		double dr2;
		
		fn2 = countPrefixState(eID, failureMap);
		sn2 = countPrefixState(eID, successMap);
		int fsnn = fn2 + sn2;
		dr2 = (fsnn!=0)? fn2 * 1.0/fsnn:0;
		
		return dr1-dr2;		
	}
	
    private int countPrefixState(String eID,
			HashMap<String, ArrayList<ProbeEvent>> map) {
		
    	int result = 0;    	
    	
    	ArrayList<String> preIDs =prebuf.get(eID);    	
    	
    	if(preIDs==null) return 0;
    	
    	for(String id: preIDs) {    		
    		    		
    		ArrayList<ProbeEvent> list = map.get(id);
    		result += (list!=null)?list.size():0;
    		
    	}
    	
		return result;
	}	

	
	private void statisEvents(HashMap<String, ArrayList<ProbeEvent>> map,
			ProbeEvent[] events) {

		for(ProbeEvent ev: events) {			
			String eID = ev.getEventID();
			
			ArrayList<ProbeEvent> saved = map.get(eID);
			if(saved == null) {
				saved = new ArrayList<ProbeEvent>();
				map.put(eID, saved);
			}
			saved.add(ev);
		}		
	}

	public boolean needRefinement(SuspiciousEventRank rank, long examizeNum) {
		if(rank==null || rank.ranklist == null || rank.ranklist.length<=0) return false;
		
		RankRecord top = rank.ranklist[0];				
		
		String[] eventIDs;
        int index=0;
        int count=0;       
        
        while(index<rank.ranklist.length && count < examizeNum) {
        	eventIDs = rank.ranklist[index].eventIDs;
        	for(String eID: eventIDs) {
        		if(canRefineEvent(rank, eID)) {
        			//switch the one that can be refined to top list
        			rank.ranklist[0] = rank.ranklist[index];
        		    rank.ranklist[index] = top;
        			return true;
        		}
        		
        		count++;
        	}
        	
        	index++;        	
        }

        return false;	
	}	
	
	private boolean canRefineEvent(SuspiciousEventRank rank, String eID) {
		
		ArrayList<ProbeEvent> list = rank.getAllEventsbyID(eID);		
		if(list==null || list.isEmpty()) return false;
		
		for(ProbeEvent event: list)
			if(event.canRefine()) return true;
		
		return false;
	}

	public ArrayList<String> refineEvents(SuspiciousEventRank rank) {
		ArrayList<String> needRefinedList = new ArrayList<String>();
		if(rank.ranklist!=null && rank.ranklist.length>0) {
			RankRecord rd = rank.ranklist[0];//refine the top
			String[] eventIDs = rd.eventIDs;
						
			for(String eID: eventIDs)
				if(isEncapsulated(eID, rank.failureEvents, rank.successEvents)) needRefinedList.add(eID);
			
			refineEvents(rank, needRefinedList);
		}
		
		return needRefinedList;
	}
	
	public ArrayList<String> refineEvents(SuspiciousEventRank rank, ArrayList<String> needRefinedList) {
		
		HashMap<String, ArrayList<ProbeEvent>> success_map = rank.successEvents;
		HashMap<String, ArrayList<ProbeEvent>> failure_map = rank.failureEvents;
		
		ArrayList<String> refinedEventIDs = new ArrayList<String>();
		
		for(String eID: needRefinedList) {
			ArrayList<ProbeEvent> flist = failure_map.remove(eID);
			ArrayList<ProbeEvent> slist = success_map.remove(eID);
			
			if(flist!=null) {
				for(ProbeEvent event: flist) {
					ArrayList<ProbeEvent> ret_ev = event.refine();
					
					for(ProbeEvent pe:ret_ev) {
					     addNewEvent(pe,failure_map);
					     String rfeID = pe.getEventID();
					     if(!refinedEventIDs.contains(rfeID))
					    	 refinedEventIDs.add(rfeID);
					}
									
				}
			}
				
			if(slist!=null) {
				for(ProbeEvent event: slist) {
					ArrayList<ProbeEvent> ret_ev = event.refine();
					
					for(ProbeEvent pe:ret_ev) {
					     addNewEvent(pe, success_map);					     			
					     String rfeID = pe.getEventID();
					     if(!refinedEventIDs.contains(rfeID))
					    	 refinedEventIDs.add(rfeID);
					}
				}				
			}
		}
		
		return refinedEventIDs;
		//further refine the events if the refined events are a subset of existing ones       	
        //if(!refinedEvent.isEmpty()) FurtherRefineEvents(rank, refinedEvent);                                
	}

	private void addNewEvent(ProbeEvent event,
			HashMap<String, ArrayList<ProbeEvent>> map) {

		if(event!=null) {
			String eID = event.getEventID();
			
			ArrayList<ProbeEvent> list = map.get(eID);
			if(list==null) {
				list = new ArrayList<ProbeEvent>();
				map.put(eID, list);
			}
			
			list.add(event);
		}		
	}
	
	private void furtherRefineEvents(SuspiciousEventRank rank) {
		 ArrayList<String> keys = new ArrayList<String>();
		 keys.addAll(rank.failureEvents.keySet());
		 
		 Set<String> sks = rank.successEvents.keySet();
		 for(String key: sks)
			 if(!keys.contains(key)) keys.add(key);
		 
		 for(String key: keys)
			 if(isEncapsulated(key, rank.failureEvents, rank.successEvents)) {
				  ArrayList<String> eIDs = getSubExistingEventIDs(key, keys);
				  if(!eIDs.isEmpty()) {
					   furtherRefineEvent(rank, key, eIDs);
				  }
			 }
	}

	private void furtherRefineEvent(SuspiciousEventRank rank, String key,
			ArrayList<String> eIDs) {
		HashMap<String, ArrayList<ProbeEvent>> success_map = rank.successEvents;
		HashMap<String, ArrayList<ProbeEvent>> failure_map = rank.failureEvents;
		
		ArrayList<ProbeEvent> refinedEvent = new ArrayList<ProbeEvent>();
				
	    ArrayList<ProbeEvent> flist = failure_map.remove(key);
		ArrayList<ProbeEvent> slist = success_map.remove(key);
			
		if(flist!=null) {
			for(ProbeEvent event: flist) {
				ArrayList<ProbeEvent> ret_ev = event.furtherRefine(eIDs);
					
				for(ProbeEvent pe:ret_ev) {
					addNewEvent(pe,failure_map);
					refinedEvent.add(pe);
				}									
			}
		}
				
		if(slist!=null) {
			for(ProbeEvent event: slist) {
				ArrayList<ProbeEvent> ret_ev = event.furtherRefine(eIDs);
					
				for(ProbeEvent pe:ret_ev) {
					addNewEvent(pe, success_map);					     			
				    refinedEvent.add(pe);
				}
			}				
		}		
	}

	private ArrayList<String> getSubExistingEventIDs(String key, ArrayList<String> keys){
        ArrayList<String> result = new ArrayList<String>();
        
        int index = key.indexOf("_encapsulate_");
		if(index>=0) {
			String srvn = key.substring(0, index);
			ArrayList<String> subn = new ArrayList<String>();
			index+="_encapsulate_".length();
			String tmp = key.substring(index);
			index = tmp.indexOf('_');
			while(index>=0) { 
			    String sn = tmp.substring(0, index);
			    subn.add(sn);
			    String bf = sn.replace(':', '_');
			    subn.add("t"+bf.substring(1));
			    tmp = tmp.substring(index+1);
			    index = tmp.indexOf('_');
			}
			
			if(!tmp.isEmpty()) {
				subn.add(tmp);
				String bf = tmp.replace(':', '_');
			    subn.add("t"+bf.substring(1));
			}
			
			for(String kn: keys) {
				if(kn.startsWith(srvn) && !kn.contains("_encapsulate_")) {
				    for(String pat: subn) {
					    if(kn.contains(pat))  
					    	result.add(pat);
				    }
				    subn.removeAll(result);
				}		
			}
			
			subn.clear();
			subn.addAll(result);
			result.clear();
			for(String sn: subn) {
				String token = sn;
				if(sn.contains("_")) {
					token = "T" + sn.replace('_', ':').substring(1);
				} else
					token = sn;
				if(!result.contains(token)) result.add(token);
			}
		}
					
		return result;
	}

	private void furtherRefineEvents(SuspiciousEventRank rank,
			ArrayList<ProbeEvent> refinedEvent) {
				
	   ArrayList<String> needRefined = new ArrayList<String>();
		   
	   HashMap<String, ArrayList<ProbeEvent>> failureMap = rank.failureEvents;
	   HashMap<String, ArrayList<ProbeEvent>> successMap = rank.successEvents;
		   
	   ArrayList<String> candidate = new ArrayList<String>();
	   candidate.addAll(failureMap.keySet());
	   Set<String> keys = successMap.keySet();
	   for(String key:keys)
		   if(!candidate.contains(key)) candidate.add(key);		   
		
	   for(String eID: candidate) {
		   for(ProbeEvent pe: refinedEvent) {
			   if(eID.equals(pe.getEventID())) continue;
				   
			   if(isCovered(rank, eID, pe)) {
				   needRefined.add(eID);
				   break;
			   }
		   }				   
	   }
		   		   
	   if(!needRefined.isEmpty()) refineEvents(rank, needRefined);				
	}

	private boolean isCovered(SuspiciousEventRank rank, String eID, ProbeEvent pe) {

		ArrayList<ProbeEvent> candidateEvent = rank.getAllEventsbyID(eID);		
		
		for(ProbeEvent ce: candidateEvent)			
			if(ce.canRefine() && ce.Cover(pe)) return true;
		
		return false;
	}

	public double calculateDistance(ProbeEvent ev) {
		
		String serviceName = ev.serviceName;
		ArrayList<String> mLoc_list = getMutationTokenByServicename(serviceName);
		if(mLoc_list.isEmpty()) return maxGlobalPath/2.0; 
					
		
		ArrayList<String> token_list = new ArrayList<String>();

		if(ev instanceof EncapsulateProbeEvent) 
			token_list.addAll(((EncapsulateProbeEvent)ev).getTokenList());
		else 
			if(ev instanceof EncapsulateProbeEventNew) 
				token_list.addAll(((EncapsulateProbeEventNew)ev).getTokenList());
			else 
			    token_list.add(ev.eventToken);
				
		double dist = 0;
		int count = 0;
		for(String mLoc:mLoc_list)
			for(String token: token_list) {
				Integer cd = distances.get(mLoc+":"+token);
								
				if(cd!=null) {
				   dist += cd;
				   count++;
				}
			}
						
		return dist*1.0/count;
	}


	private ExecutionRecord getExecutionRecord(String instanceID) {
        for(ExecutionRecord record: instances) {
        	if(record.getInstanceID().equals(instanceID)) return record;
        }
        
		return null;
	}

	public ArrayList<String> getMutationTokenByServicename(String serviceName) {
		ArrayList<String> mLoc_list = new ArrayList<String>();
		
		for(String mLoc: mutationLocations)
			if(mLoc.startsWith(serviceName)) mLoc_list.add(mLoc);
				
		return mLoc_list;
	}
	
	public ArrayList<String> getMutations() {
		return mutationLocations;
	}

	public int getFaultNum() {
		return mutationLocations.size();
	}

	private int topK = 0;
	public void setTopK(int topK) {
		this.topK = topK;
	}

    public Double calculateEP(SuspiciousEventRank rank, ProbeEvent ex, ProbeEvent ey) {

           String exID = ex.eventID;
           ArrayList<String> instx_f = getAllInstanceIDsWithSameContent(exID, ex, rank.failureEvents);
           ArrayList<String> instx_s = getAllInstanceIDsWithSameContent(exID, ex, rank.successEvents);
           
           ArrayList<String> insty_f;
           ArrayList<String> insty_s;
           if(!ey.equals(ex)) {
               String eyID = ey.eventID;
               insty_f = getAllInstanceIDsWithSameContent(eyID, ey, rank.failureEvents);
               insty_s = getAllInstanceIDsWithSameContent(eyID, ey, rank.successEvents);
           } else {
               insty_f = instx_f;
               insty_s = instx_s;
           }

           int count = 0;
           for(String inst: instx_f)
              if(insty_f.contains(inst)) count++;

           for(String inst: instx_s) 
              if(insty_s.contains(inst)) count++;

           int instNum = instances.size(); 
           return count * 1.0/instNum;
    }


	public HashMap<ProbeEvent, HashMap<ProbeEvent, Double>> calculateEventProb() {
		HashMap<ProbeEvent, HashMap<ProbeEvent, Double>> result = new HashMap<ProbeEvent, HashMap<ProbeEvent, Double>>();
		double spb = 1.0 / instances.size();
		
		for(ExecutionRecord record: instances) {
			ArrayList<String> sn_list = record.getAllServiceNames();
			ArrayList<ProbeEvent> events = new ArrayList<ProbeEvent>();
			for(String sn: sn_list) {
				ArrayList<ProbeEvent> te =exposeAllEvents4Service(sn, record); 
			    if(te!=null) events.addAll(te);
			}
			
			for(ProbeEvent pe: events) {
				HashMap<ProbeEvent, Double> map = getProbMap(result, pe);
				for(ProbeEvent ev: events) 
					recordEventsProb(map, ev, spb);
			}
		}
		
		return result;
	}

/*
	private int cnIndex = 0;
	private void calculateEventProbAdvanced(ArrayList<ProbeEvent> events) {
		if(eventprobs == null) 
			eventprobs = new HashMap<Integer, HashMap<Integer, Double>>();
		
		if(eventcontents == null)
			eventcontents = new HashMap<String, Integer>();
		
		double spb = 1.0 / instances.size();
		for(ProbeEvent pe: events) {
            String uID = pe.generateUID();
            Integer eIndex = eventcontents.get(uID);
            if(eIndex==null) {
                  eIndex = cnIndex;
                  cnIndex++;
                  eventcontents.put(uID, eIndex);
            }

            HashMap<Integer, Double> em = eventprobs.get(eIndex);
            if(em == null) {
                  em = new HashMap<Integer, Double>();
                  eventprobs.put(eIndex, em);
            }
                                                             
		    for(ProbeEvent ev: events) { 
                  String ev_uID = ev.generateUID();
                  Integer ev_eIndex = eventcontents.get(ev_uID);
                  if(ev_eIndex==null) {
                        ev_eIndex = cnIndex;
                        cnIndex++;
                        eventcontents.put(ev_uID, ev_eIndex);
                  }
   
                  Double pbvalue = em.get(ev_eIndex);
                  if(pbvalue == null) 
                       em.put(ev_eIndex, spb);
                  else
                       em.put(ev_eIndex, pbvalue + spb);
            }
		}
	}

	
	public HashMap<Integer, HashMap<Integer, Double>> getEventProbMap() {
		return eventprobs;
	}
	
	public HashMap<String, Integer> getEventContentmap() {
		return eventcontents;
	}
*/

	public HashMap<String, HashMap<String, Double>> calculateEventProbNew() {
		HashMap<String, HashMap<String, Double>> result = new HashMap<String, HashMap<String, Double>>();
		double spb = 1.0 / instances.size();
		
		for(ExecutionRecord record: instances) {
			ArrayList<String> sn_list = record.getAllServiceNames();
			ArrayList<ProbeEvent> events = new ArrayList<ProbeEvent>();
			for(String sn: sn_list) {
                    ArrayList<ProbeEvent> te;
                    if(m_solution.exposePolicy ==  FaultLocalizationSolution.EXPOSEALLWITHSD) 
                          te = exposeAllEvents4ServiceSD(sn, record); 
                    else
				          te = exposeAllEvents4Service(sn, record); 
			        if(te!=null) events.addAll(te);
			}
			
			for(ProbeEvent pe: events) {
				HashMap<String, Double> map = getProbMapNew(result, pe);
				for(ProbeEvent ev: events) 
					recordEventsProbNew(map, ev, spb);
			}
		}
		
		return result;
	}
        

 	public HashMap<Integer, HashMap<Integer, Double>> calculateEventProbNew(HashMap<String, Integer> contentMap) {
		HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<Integer, HashMap<Integer, Double>>();
		double spb = 1.0 / instances.size();
        int cnIndex = 0;	    	

		for(ExecutionRecord record: instances) {
			ArrayList<String> sn_list = record.getAllServiceNames();
			ArrayList<ProbeEvent> events = new ArrayList<ProbeEvent>();
			for(String sn: sn_list) {
                  ArrayList<ProbeEvent> te;
                  if(m_solution.exposePolicy ==  FaultLocalizationSolution.EXPOSEALLWITHSD) 
                        te = exposeAllEvents4ServiceSD(sn, record); 
                  else
				        te = exposeAllEvents4Service(sn, record); 
			      if(te!=null) events.addAll(te);
			}
			
			for(ProbeEvent pe: events) {
                String uID = pe.generateUID();
                Integer eIndex = contentMap.get(uID);
                if(eIndex==null) {
                      eIndex = cnIndex;
                      cnIndex++;
                      contentMap.put(uID, eIndex);
                }

                HashMap<Integer, Double> em = result.get(eIndex);
                if(em == null) {
                      em = new HashMap<Integer, Double>();
                      result.put(eIndex, em);
                }
                                                                 
			    for(ProbeEvent ev: events) { 
                      String ev_uID = ev.generateUID();
                      Integer ev_eIndex = contentMap.get(ev_uID);
                      if(ev_eIndex==null) {
                            ev_eIndex = cnIndex;
                            cnIndex++;
                            contentMap.put(ev_uID, ev_eIndex);
                      }
       
                      Double pbvalue = em.get(ev_eIndex);
                      if(pbvalue == null) 
                           em.put(ev_eIndex, spb);
                      else
                           em.put(ev_eIndex, pbvalue + spb);
                }
			}
		}

        //for debugging
        System.out.println(cnIndex);
		
		return result;
	}       

	private void recordEventsProb(HashMap<ProbeEvent, Double> map, ProbeEvent ev, double spb) {
		
		Set<ProbeEvent> keys = map.keySet();
		boolean existing = false;
		for(ProbeEvent pe: keys)
			if(pe.isSameValueWithSameEventID(ev)) {
				Double value = map.get(pe);
				value += spb;
				map.put(pe, value);
				existing = true;
				break;
			}
		
		if(!existing) {
		   Double value = spb;
		   map.put(ev, value);
		}
		
	}

    private void recordEventsProbNew(HashMap<String, Double> map, ProbeEvent ev, double spb) {

         String uID = ev.generateUID();
         Double value = map.get(uID);
         if(value==null) {
              value = spb;
              map.put(uID, value);
         } else {
              value = value + spb;
              map.put(uID, value);
         }
    }

	private HashMap<ProbeEvent, Double> getProbMap(
			HashMap<ProbeEvent, HashMap<ProbeEvent, Double>> result,
			ProbeEvent pe) {
		
		Set<ProbeEvent> keys = result.keySet();
		for(ProbeEvent event: keys)
			if(event.isSameValueWithSameEventID(pe)) return result.get(event);
		
		HashMap<ProbeEvent, Double> map = new HashMap<ProbeEvent, Double>();
		result.put(pe, map);
		return map;
	}

    private HashMap<String, Double> getProbMapNew(
                        HashMap<String, HashMap<String, Double>> result,
                        ProbeEvent pe) {

        String uID = pe.generateUID();
        HashMap<String, Double> map = result.get(uID);
        if(map==null) {
              map = new HashMap<String, Double>();
              result.put(uID, map);
        }
        
        return map;
    }


	public void setSolution(FaultLocalizationSolution solution) {
		m_solution = solution;		
	}

	public FaultLocalizationSolution getSolution() {
		return m_solution;		
	}
	
	public ArrayList<ExecutionRecord> getInstances() {
		return instances;
	}

	/*
	 * Save events to a text file
	 */
	public void saveEvents(String filename, boolean isCompleted) {
		
		String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		String rootElementStart = "<Instances>";
		String rootElementEnd = "</Instances>";
		
		
		try {
			BufferedWriter writer  = new BufferedWriter(new FileWriter(filename));
			writer.write(header);
			writer.newLine();
			writer.write(rootElementStart);
			writer.newLine();
			
			for(ExecutionRecord record: instances) {
				String instxml = transferRecord2XML(record);
				writer.write(instxml);
				writer.newLine();
			}
			
			if(isCompleted) writer.write(rootElementEnd);			
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 * Load event instances into a new encapsulation object.
	 * Need to invoke the snapshotServiceInstance before using the encapsulation object.
	 */
	public static Encapsulation loadEncapsulationFromSavedEvents(String filename) {
		Encapsulation eps = new Encapsulation();	
		MyHandler handler = new MyHandler(eps);
	    
	    try {
	        SAXParserFactory factory = SAXParserFactory.newInstance();
	        factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new File(filename), handler);
	    }catch(Exception e) {
	    	e.printStackTrace();
	    }
		
		return eps;
	}

	private String transferRecord2XML(ExecutionRecord record) {
		String indent = "   ";
		String result = "";
		String elementHeader = indent+ "<Instance>\n";
		String elementEnd = indent+"</Instance>";
		String eventsHeader = indent+ indent+ "<Events>\n";
		String eventsEnd = indent+ indent+ "</Events>\n";
		String IDHeader = indent+ indent+ "<ID>";
		String IDEnd = "</ID>\n";
		String passedHeader = indent+ indent+ "<Passed>";
		String passedEnd = "</Passed>\n";
		String seHeader = indent+ indent+ indent+"<Event>\n";
		String seEnd = indent+ indent+ indent+"</Event>\n";
		String msgHeader = indent+indent+ indent+ indent+"<Msg>\n";
		String msgEnd = indent+indent+ indent+ indent+"</Msg>\n";
		String msgNameHeader = indent+indent+ indent+ indent+indent+"<Name>";
		String msgNameEnd = "</Name>\n";		
		String msgTypeHeader = indent+indent+ indent+ indent+indent+"<Type>";
		String msgTypeEnd = "</Type>\n";
		String msgValueHeader = indent+indent+indent+ indent+ indent+"<Value>";
		String msgValueEnd = "</Value>\n";
		
		
		result+=elementHeader;
		String instanceID = record.getInstanceID();
		result+=IDHeader+ instanceID + IDEnd;

		boolean passed = record.isPassed();
		result+=passedHeader + (passed?"true":"false") + passedEnd;
		
		result+=eventsHeader;
		ArrayList<Event> events = record.getAllEvents();
		for(Event event: events) {
			result+=seHeader;
			ArrayList<Message> msgs = event.getMessages();			
			for(Message msg: msgs) {
				result+=msgHeader;
				
				result+=msgNameHeader + msg.getName() + msgNameEnd;
				result+=msgTypeHeader + msg.getType() + msgTypeEnd;
				String value = convertValue(msg);
				result+=msgValueHeader+value+msgValueEnd;
				result+=msgEnd;
			}
			
			result+=seEnd;
		}
		
		result+=eventsEnd;
		result+=elementEnd;
		return result;
	}

	private String convertValue(Message msg) {
		String type = msg.getType();
		Object value = msg.getValue();
		
		if(type.equals(DataType.BOOLEAN)) 
			return ((Boolean)value)?"true":"false";
		
		if(type.equals(DataType.INTEGER))
			return ((Integer)value).toString();
		
		if(type.equals(DataType.STRING))
			return (String)value;
		
		return "";
	}		
}

class ClusterRadius{
	public ProbeEvent center;
	public double radius;
}

class MyHandler extends DefaultHandler {

	private String instanceName = "Instance";
	private String ID = "ID";
	private String Passed = "Passed";
	private String eventName = "Event";
	private String Msg = "Msg";
	private String MN = "Name";
	private String MT = "Type";
	private String MV = "Value";
	
	private Encapsulation encapsulation = null; 
	private int step=-1;
	private ExecutionRecord record = null;
	private Message msg = null;
	private ArrayList<Message> msglist = new ArrayList<Message>();
	private ArrayList<String> event_names = new ArrayList<String>(); 
	private String charactervalue = "";
	private String rawXMLValue = "";
	
	public MyHandler(Encapsulation eps) {
		encapsulation = eps;
	}
	
	@Override
	public void startElement(String namespaceURL, String localName, String qName, Attributes atts) 
			          throws SAXException {
		
		//parsing XML contents inside MSG value
		if(step==4) {			
			rawXMLValue += constructElementTag(namespaceURL, localName, qName, atts);			
		}
		
		if(instanceName.equals(localName)) {
			record = new ExecutionRecord();
			step=-1;
		} else 
		
		if(ID.equals(localName)) 
		   step=0;
		else
		
		if(Passed.equals(localName))
			step = 1;
		else
		
		if(eventName.equals(localName)) 
			msglist.clear();
		else
			
		if(Msg.equals(localName)) {
			msg = new Message();
			msglist.add(msg);
		} else
			
		if(MN.equals(localName)) 
			step = 2;
		else
			
		if(MT.equals(localName))
			step = 3;
		else
			
		if(MV.equals(localName))
			step = 4;
		
		charactervalue = "";
		
		
	}
	
	private String constructElementTag(String namespaceURL, String localName,
			String qName, Attributes atts) {
		
		String result = "<";
		result += localName;
		
		//Not handle the namespace and attributes at this moment
		//if(!namespaceURL.isEmpty()) result += " xmlns=" + namespaceURL;
		/*
		int length = atts.getLength();
		for(int i=0;i<length;i++) {
			String attrqn = atts.getQName(i);
			String attrvalue = atts.getValue(i);
			result += " " + attrqn + " = " + attrvalue;
		}*/
		
		result += ">";
		return result;
	}

	@Override
	public void endElement(String namespaceURL, String localName, String qName) 
			          throws SAXException {
		
		if(step == 4) {
			if(!"".equals(charactervalue)) {
			     String val = charactervalue.trim();
			     rawXMLValue += val;
			     charactervalue = "";
			}
			
			if(localName.equals(MV)) {
			   Object vv = convertValue(msg.getType(), rawXMLValue);
			   msg.setValue(vv); 
			   step = -1;	
			   rawXMLValue = "";
			} else
			   rawXMLValue += "</" + localName + ">"; 
			
			return;
		}
		
		if(!"".equals(charactervalue)) {
		     String val = charactervalue.trim();
		     assignValue(val);
		     charactervalue = "";
		}
		
		if(eventName.equals(localName)) {
		    Event event = EventType.initializeEvent(msglist);
			
			if(event!=null) {
				String serviceName = event.getServiceName();
			    record.addServiceEvent(serviceName, event);	
			}
		} else
			
		if(instanceName.equals(localName)) {
			encapsulation.addInstanceRecord(record);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		//String val = new String(ch, start, length).trim();
		
		charactervalue += new String(ch, start, length);
		
        //assignValue(val);
	}
    
	private void assignValue(String val) {
		switch(step) {
		case 0:
			record.setInstanceID(val);
			step = -1;
			break;
		case 1:
			boolean ispassed = new Boolean(val);
			record.setPassed(ispassed);
			step = -1;
			break;
		case 2:
			msg.setName(val);
			step = -1;
			break;
		case 3:
			msg.setType(val);
			step = -1;
			break;
		case 4:
			Object vv = convertValue(msg.getType(), val);
			msg.setValue(vv);
			step = -1;
			break;
		default:;
		}
	}
	
	private Object convertValue(String type, String value) {
		if(type.equals(DataType.BOOLEAN)) 
			return new Boolean(value);
		
		if(type.equals(DataType.INTEGER))
			return  new Integer(value);
		
		if(type.equals(DataType.STRING)) 
			return (String)value;

		return null;
	}
	
	
}