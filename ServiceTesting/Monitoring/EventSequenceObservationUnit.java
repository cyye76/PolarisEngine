package ServiceTesting.Monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import ServiceTesting.ConstraintSolver.MyConstraint;
import engine.Event.Event;
import engine.Event.TaskCompleteEvent;

public class EventSequenceObservationUnit extends ObservationUnit {
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4683979991546063803L;
	
	//the map between service name and observation units
	private HashMap<String, ArrayList<String>> sequence = new HashMap<String,ArrayList<String>>();
	//indicate the next event to observe
	private HashMap<String, Integer> indexes = new HashMap<String, Integer>();
	
	private ArrayList<String> exposedEvents;
	
	//private HashMap<String, Integer> exposedIndexes = new HashMap<String, Integer>();
	//private HashMap<String, Integer> exposedCapacity = new HashMap<String, Integer>();
	
	//to evaluate whether the current event sequence is 
	//a prefix of the unit. If not, then not necessary to 
	//match this unit any more.
	private boolean isPrefix = true; 
		
	
	//this is used to calculate local path coverage
	//only available when the sequence represents a local path
	private String serviceName;
	public void setServiceName(String name) {
		serviceName = name;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	
	public static EventSequenceObservationUnit copyUnit(EventSequenceObservationUnit unit) {
		EventSequenceObservationUnit copy = new EventSequenceObservationUnit();
		
		copy.setServiceName(unit.getServiceName());
		
		copy.setExposedEvents(unit.exposedEvents);
		
		copy.setConstraint(unit.m_constraint);
		
		copy.setExposedPercentage(unit.exposedPercentage);
		
		Set<String> keys = unit.sequence.keySet();
		for(String key: keys) {
			copy.sequence.put(key, unit.sequence.get(key));			
		}
		
		keys = unit.indexes.keySet();
		for(String key:keys) {
			copy.indexes.put(key, unit.indexes.get(key));
		}
		
		return copy;
	}
	
	@Override
	public void feedEvent(Event event) {
		
		if(isPrefix && event instanceof TaskCompleteEvent) {
			String sn = event.getServiceName();
			ArrayList<String> service_queue = sequence.get(sn);
			Integer queue_index = indexes.get(sn);
			String taskName = ((TaskCompleteEvent)event).getTaskName();
			
			if((service_queue!=null)  
					&& (queue_index < service_queue.size())
				    && (exposedEvents.contains(sn+":"+taskName)))//filter non-exposed events 
			{
				String taskID = service_queue.get(queue_index);
				queue_index++;
				indexes.put(sn, queue_index);								
				
				if(!taskID.equals(taskName))
					isPrefix = false;
				
//				Integer ep_Index = exposedIndexes.get(sn);
//				Integer ep_cap = exposedCapacity.get(sn);
//				if(!event.isPublic()) {
//					ep_cap++;
//					exposedCapacity.put(sn, ep_cap);
//								
//				    if(ep_Index < ep_cap * exposedPercentage) {
//				    	event.setExposed(true);
//				    	ep_Index++;
//				    	exposedIndexes.put(sn, ep_Index);
//				    	
//				    	if(ep_cap > 1 && ep_Index > 1) 
//				    		System.out.println(""+ ep_cap + "," + ep_Index);
//				    }
//				}
				
				if(queue_index <= service_queue.size() * exposedPercentage)
				   event.setExposed(true);
			}
		}
	}

	@Override
	public boolean isObserved() {
		if(!isPrefix) return false;
		
		Set<String> keys = sequence.keySet();
		for(String sn: keys) {
			ArrayList<String> service_queue = sequence.get(sn);
			Integer queue_index = indexes.get(sn);
			if(queue_index<service_queue.size())
				return false;
		}
		
		return true;		
	}

	@Override
	public void rollback() {		
		isPrefix = true;
		Set<String> keys = indexes.keySet();
		for(String sn: keys) {
			indexes.put(sn, 0);
			//exposedIndexes.put(sn, 0);
			//exposedCapacity.put(sn, 0);
		}
	}

	public void setSequence(HashMap<String, ArrayList<String>> maps) {
		Set<String> keys = maps.keySet();
		
		for(String sn: keys) {
			ArrayList<String> path = new ArrayList<String>();
			
			ArrayList<String> op = maps.get(sn);
			if(op!=null) {	
				for(String tn: op)
			       path.add(tn);
				
			   sequence.put(sn, path);
			   indexes.put(sn, 0);
			}
			//exposedIndexes.put(sn, 0);
			//exposedCapacity.put(sn, 0);
		}
	}
	
	public HashMap<String, ArrayList<String>> getSequence() {
		return this.sequence;
	}

	@Override
	public boolean equals(ObservationUnit unit) {
		if(!(unit instanceof EventSequenceObservationUnit))
		    return false;
		
		HashMap<String, ArrayList<String>> p_seq = ((EventSequenceObservationUnit)unit).getSequence();
		
		Set<String> keys = sequence.keySet();
		for(String key: keys) {
			ArrayList<String> sq1 = sequence.get(key);
			ArrayList<String> sq2 = p_seq.get(key);
			if(!sameSequence(sq1,sq2)) return false;
		}
		
		return true;
	}

	private boolean sameSequence(ArrayList<String> sq1, ArrayList<String> sq2) {
        
		if((sq1==null) && (sq2 == null)) return true;
		if((sq1==null) && (sq2!=null))   return false;
		if((sq1!=null) && (sq2 == null)) return false;
		
		if(sq1.size()!=sq2.size()) return false;
		
		for(int i=0;i<sq1.size();i++)
			if(!sq1.get(i).equals(sq2.get(i))) return false;
		
		return true;
	}

	@Override
	public void print() {		
		Set<String> keys = sequence.keySet();
		for(String key: keys) {
			ArrayList<String> list = sequence.get(key);
			System.out.print(key + ":");
			for(String tn: list) 
				System.out.print(tn+" ");
			System.out.println();			
		}
	}
	
	public void setExposedEvents(ArrayList<String> events) {
		this.exposedEvents = events;
	}
	
	public ArrayList<String> getExposedEvents() {
		return this.exposedEvents;
	}
	
	private ArrayList<MyConstraint> m_constraint = null;
	public void setConstraint(ArrayList<MyConstraint> constraint) {
		m_constraint = new ArrayList<MyConstraint>(); 
		m_constraint.addAll(constraint);		
	}

	public ArrayList<MyConstraint> getConstraints() {		
		return m_constraint;
	}

	public boolean isSame(EventSequenceObservationUnit unit) {

		Set<String> sn_list = sequence.keySet();
		for(String sn: sn_list) {
			ArrayList<String> path1 = sequence.get(sn);
			ArrayList<String> path2 = unit.sequence.get(sn);
			
			if(path2==null) return false;
			if(path1.size()!=path2.size()) return false;
			
			for(int i=0;i<path1.size();i++)
				if(!path1.get(i).equals(path2.get(i))) return false;
		}
		
		return true;
	}	
}

/*	
	public ArrayList<String> generatePathConstraint(
			ArrayList<PathCoverageEventInterface> eventinterfaces) {				
		
		String[][] constraints = new String[eventinterfaces.size()][];
		for(int i=0;i<constraints.length;i++)
			constraints[i] = getLocalSequenceConstraint(eventinterfaces.get(i));
		
		ArrayList<String[]> feasibleConstraint = getFeasibleConstraint(constraints, 0);
		
		ArrayList<String> result = new ArrayList<String>();
		for(String[] fe: feasibleConstraint) {
			String cts = DeriveConstraint(fe);
			result.add(cts);
		}
		
		return result;
	}

	private String DeriveConstraint(String[] fe) {
		int[] pointers = new int[fe.length];
		for(int i=0;i<pointers.length;i++)
			pointers[i] = 0;
		
		ArrayList<ArrayList<VariableVersion>> queue = new ArrayList<ArrayList<VariableVersion>>();	
		ArrayList<HashMap<String,VariableVersion>> varV = new ArrayList<HashMap<String,VariableVersion>>();
		for(int i=0;i<pointers.length;i++) {
			queue.add(new ArrayList<VariableVersion>());
			varV.add(new HashMap<String,VariableVersion>());
		}				
		
		return DeriveConstraint(fe, pointers, queue, varV, "");	
	}

	private String DeriveConstraint(String[] fe, int[] pointers,
			ArrayList<ArrayList<VariableVersion>> queue, 
			ArrayList<HashMap<String, VariableVersion>> varV, String prefixC) {
		
		int cp=0;
		boolean canforward = false;
		while(cp<fe.length && !canforward) {
			String nextC = getNextCondition(fe[cp], pointers[cp]);
			
			if(nextC!=null) {
			
				if(isSendPort(nextC)) {
					canforward = true;
					pointers[cp]++;
					ArrayList<VariableVersion> sv = getSendVariableVersion(nextC, varV, cp);
					for(int i=0;i<fe.length;i++) {
						if(i==cp) continue;
						queue.get(i).addAll(sv);
					}
				} 
			
				if(isReceivePort(nextC)) {									
					HashMap<String, VariableVersion> version = getReceiveVariableVersion(nextC, queue.get(cp));
					if(version!=null) {
						updateVersion(version, varV.get(cp));
						canforward = true;
						pointers[cp]++;
					}
				}
		
				if(isLocal(nextC)) {
					canforward = true;
					pointers[cp]++;
					
					prefixC += transformExpression(nextC, varV.get(cp));
				}
			}
			
			cp++;			
		}
		
		if(canforward)
			DeriveConstraint(fe, pointers, queue, varV, prefixC);
				
		return null;
	}

	private String transformExpression(String nextC,
			HashMap<String, VariableVersion> varV) {
		
		WordParser parser = new WordParser(nextC);
		String[] expressions = parser.getExpression();
		
		if(expressions.length>1 && expressions[1].equals(":=")) { //assignment
			String ne = updateVariableName(expressions, 2, varV);
			String vn = expressions[0];
			VariableVersion ver = varV.get(vn);
			if(ver==null) {
				ver = new VariableVersion();
				ver.vname = vn;
				ver.version = 0;
				varV.put(vn, ver);				
			} else {
				ver.version++;
				vn = ver.getExtendedName();
			}
			
			return vn + " = " + ne;
		} else
			return updateVariableName(expressions, 0, varV);
	}

	private String updateVariableName(String[] expressions, int location,
			HashMap<String, VariableVersion> varV) {
		
		String result = "";
		for(int i=location;i<expressions.length;i++) {
			String word = expressions[i];
			if(WordParser.isKeyword(word))
				result += " " + word + " ";
			else {
				VariableVersion vv = varV.get(word);
				if(vv==null) {
					vv = new VariableVersion();
					vv.vname = word;
					vv.version = 0;
					varV.put(word, vv);
				}	
				
				if(vv.version>0)
					result += " " + vv.getExtendedName() + " ";
				else
					result += " " + word + " ";
			}
		}
		return result;
	}

	private void updateVersion(HashMap<String, VariableVersion> uv,
			HashMap<String, VariableVersion> ov) {		
		
		Set<String> keys = uv.keySet();
		for(String vn: keys) {
			VariableVersion vv = uv.get(vn);
			ov.put(vn, vv);
		}
	}

	private HashMap<String, VariableVersion> getReceiveVariableVersion(
			String nextC, ArrayList<VariableVersion> que) {
		
		HashMap<String, VariableVersion> map = new HashMap<String, VariableVersion>();
		String[] conts = nextC.split("\\s+");
				
		for(int i=1;i<conts.length;i++) {
			String vn = conts[i];
			boolean satisfied = false;
			for(VariableVersion vv: que) {
				if(vn.equals(vv.vname)) {
					map.put(vn, vv);
					satisfied = true;
					break;
				}
			}
			
			if(!satisfied) return null;
		}
		
		Collection<VariableVersion> values = map.values();
		for(VariableVersion vv: values)
			que.remove(vv);
		
		return map;
	}

	private ArrayList<VariableVersion> getSendVariableVersion(String nextC,
			ArrayList<HashMap<String, VariableVersion>> varV, int cp) {

		ArrayList<VariableVersion> result = new ArrayList<VariableVersion>();
		
		HashMap<String, VariableVersion> vs = varV.get(cp);
		String[] conts = nextC.split("\\s+");
		
		for(int i=1;i<conts.length;i++) {
			String vn = conts[i];
			VariableVersion version = vs.get(vn);
			if(version==null) {
				version = new VariableVersion();
				version.vname = vn;
				version.version = 0;
				vs.put(vn, version);
			}
			
			result.add(version);
		}
		
		return result;
	}

	private boolean isLocal(String nextC) {
		
		return !isReceivePort(nextC) && !isSendPort(nextC);
	}

	private boolean isReceivePort(String nextC) {		
		return nextC.startsWith("receive ");
	}

	private boolean isSendPort(String nextC) {		
		return nextC.startsWith("send ");
	}

	private String getNextCondition(String conditions, int location) {

		String sc = conditions;
		int index;
		while(location>0) {
			index = sc.indexOf(';');
			if(index<0 || index+1 >= conditions.length()) return null;
			sc = sc.substring(index+1);
		}
		
		index = sc.indexOf(';');
		if(index<0) return null;
		
		return sc.substring(0, index);
	}

	private ArrayList<String[]> getFeasibleConstraint(String[][] constraints, int location) {
		
		ArrayList<String[]> result = new ArrayList<String[]>();
		
		if(constraints == null || location>=constraints.length) return result;
		
		ArrayList<String[]> tr = getFeasibleConstraint(constraints, location+1);
		for(String ct: constraints[location]) {			
			for(String[] fi: tr) {
				String[] ni = new String[fi.length+1];
				ni[0] = ct;
				for(int i=1;i<ni.length;i++)
					ni[i] = fi[i-1];
				
				result.add(ni);
			}
		}
		
		return result;
	}

	private String[] getLocalSequenceConstraint(
			PathCoverageEventInterface pci) {
		
		String sn = pci.getServiceName();		
		
		ArrayList<String> path = sequence.get(sn);
		assert(path!=null);
		
		if(path.size()<2) return null;		
		
		String preceding=path.get(0);
		
		ArrayList<String> tr = getLocalSequenceConstraint(pci, preceding, path, 1, "");
		
		String[] result = new String[tr.size()];
		result = tr.toArray(result);
		
		return result;
	}

	private ArrayList<String> getLocalSequenceConstraint(PathCoverageEventInterface pci,
			String preceding,
			ArrayList<String> path, int location, String prefixC) {
		
		ArrayList<String> result = new ArrayList<String>();
		if(location>=path.size()) return result;
		
		String next = path.get(location);
		ArrayList<String> cds = pci.getCausalityCondition(preceding, next);
		assert(cds!=null);
		for(String item: cds) {
			String pfc = prefixC + item;
			ArrayList<String> tr = getLocalSequenceConstraint(pci, next, path, location+1, pfc);
			result.addAll(tr);
		}		

		return result;
	}


}

class VariableVersion{
	public String vname;
	public int version;
	public String getExtendedName() {
		return vname + "_v"+version;
	}
}
*/	