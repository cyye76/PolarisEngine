package ServiceTesting.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import scripts.TestingScript.TestCaseType;
import scripts.TestingScript.TestingScriptDefinition;

import engine.DataType;
import Configuration.Config;
import Deployment.LoadServiceScript;
import Service.AbstractService;
import Service.State;
import Service.Variable;
import ServiceTesting.ConstraintSolver.CSLVariable;
import ServiceTesting.ConstraintSolver.ConstraintSolver;
import ServiceTesting.ConstraintSolver.MyConstraint;

public class HybridServiceTestCase extends ServiceTestCase {

	public HybridServiceTestCase(AbstractService service) {
		super(service);		
	}

	private ArrayList<CSLVariable> bindVariables;
	
	@Override	
	public void initializeTestCase() {
		bindVariables = new ArrayList<CSLVariable>();
		
        State cs = m_service.getState();
        assert(cs!=null);

        Random rd = new Random(System.currentTimeMillis());
        
        ArrayList<Variable> variables = cs.getVariables();        
        for(Variable v: variables) {
        	String type = v.getType();
        	Object value = null;
        	
        	if(type.equals(DataType.BOOLEAN)) {//boolean
        		value = rd.nextBoolean();
        	} else
        		
        	if(type.equals(DataType.INTEGER)) { //Integer
        		int initValue = (Integer)v.getValue();
        		if(initValue == -1) //that means not initialized yet
        		   value = rd.nextInt(Config.getConfig().variableDomain);
        		else
        		   value = initValue;	
        	} else
        		value = ""+rd.nextLong(); //String
        	
        	CSLVariable csl = new CSLVariable();
        	csl.setName(v.getName());
        	csl.setServiceName(m_service.getName());
        	csl.setType(v.getType());
        	csl.setValue(value);
        	
        	bindVariables.add(csl);
        }                
        
	}
	
	private int cpointer = 0;
	
	private HashMap<Integer, ArrayList<ArrayList<CSLVariable>>> buffer 
	         = new HashMap<Integer, ArrayList<ArrayList<CSLVariable>>>();
	
	public boolean nextTestCase(AbstractService service) {
						
		m_service = service;						
		ArrayList<CSLVariable> solution = null; 
		
		boolean foundSolution = false;
		int num = constraint_list.size();
		int trycount = 0;
		while(!foundSolution) {
			initializeTestCase();
			
			if(trycount>5) {
				trycount = 0;
				cpointer++;
				if(cpointer >= num) cpointer=0;
			}
						
			ArrayList<MyConstraint> cont = constraint_list.get(cpointer);											
			ConstraintSolver solver = getConstraintSolver(cont);
				
			solver.setBindVariables(bindVariables);
				
			foundSolution = solver.generateNextResult();				
			solution = solver.getSolution();
				
			trycount++;													
		}			
		
		cpointer++;		
		if(cpointer >= num) cpointer=0;
			
		initializeTestCase(service, solution);
		//System.out.println("Selecting " + cpointer);
		return true;			
	}
	
	/*
	 * New strategy
	 */
	int[] matchedRecord = null;
	public boolean nextTestCase1(AbstractService service) {
		int num = constraint_list.size();
		int average = 200 / num + 1;
		
		if(matchedRecord==null) {
			matchedRecord = new int[num];
			for(int i=0;i<matchedRecord.length;i++)
				matchedRecord[i] = 0;
		}
		
		m_service = service;						
		ArrayList<CSLVariable> solution = null; 
		
		boolean foundSolution = false;
		
		while(!foundSolution) {
			initializeTestCase();
						
			for(int i=0;i<num;i++) {
				if(matchedRecord[i] > average) continue;
			    ArrayList<MyConstraint> cont = constraint_list.get(i);
			    ConstraintSolver solver = getConstraintSolver(cont);
			    solver.setBindVariables(bindVariables);
				
				foundSolution = solver.generateNextResult();				
				solution = solver.getSolution();
				
				if(foundSolution) {
					matchedRecord[i]++;
					break;
				}
			}
		}			
					
		initializeTestCase(service, solution);
		//System.out.println("Selecting " + cpointer);
		return true;			
	}
	
	public void generateTestCasePool(ArrayList<ServiceTestCase> pool, int maxNum, TestingScriptDefinition tsd) {
		int cNum = constraint_list.size();
		//int aNum = (int) (maxNum * 2/cNum);
		int aNum = maxNum /cNum + 1;
		Random rd = new Random(System.currentTimeMillis());
		
		HashMap<Integer, ArrayList<ServiceTestCase>> buffer = new HashMap<Integer, ArrayList<ServiceTestCase>>();
		//ServiceTestCase[][] buffer = new ServiceTestCase[cNum][aNum]; 
		int[] buIndex = new int[cNum];
		for(int i=0;i<cNum;i++)			
			buIndex[i] = 0;
		
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		int gn=0;
		int fnum = 0;
		while(gn<maxNum) {
						
			AbstractService service = LoadServiceScript.loadService(filename);
			assert(service!=null);		
			
			m_service = service;			
			initializeTestCase();
			
			int cIndex = testConstraintIndex(rd, buIndex, aNum, null);
			if(cIndex>=0) {
				addTestCase2Buffer(buffer, cIndex, m_service);
				//buffer[cIndex][buIndex[cIndex]] = new HybridServiceTestCase(m_service);
				buIndex[cIndex]++;
				gn++;
			} else {
				fnum++;
				if(fnum>=cNum) {
					aNum++;
					fnum=0;
				}
			}
		}
		
		int cp = 0;
		while(gn>0) {
			do {
				cp++;
				if(cp>=cNum) cp = 0;				
			} while(buIndex[cp]<=0);
			
			buIndex[cp]--;
			pool.add(getTestCasefromBuffer(buffer, cp));
			
			gn--;
		}
	}
	
	public void generateTestCasePool2(ArrayList<ServiceTestCase> pool, int maxNum, TestingScriptDefinition tsd, int[] weight, long[] gts) {
		
		long start = System.currentTimeMillis();
		
		int cNum = constraint_list.size();

		if(weight!=null) {
			cNum=0;
			for(int w: weight) cNum+=w;
		}
		
		int aNum = maxNum /cNum + 1;
		//int aNum = maxNum /cNum + 3;
		
		Random rd = new Random(System.currentTimeMillis());
		
		HashMap<Integer, ArrayList<ServiceTestCase>> buffer = new HashMap<Integer, ArrayList<ServiceTestCase>>();
		 
		int[] buIndex = new int[constraint_list.size()];
		for(int i=0;i<buIndex.length;i++)			
			buIndex[i] = 0;
		
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		int gn=0;
		int fnum = 0;
		//while(gn<maxNum && fnum < 500) {
		while(gn<maxNum && fnum < 1000) {
						
			AbstractService service = LoadServiceScript.loadService(filename);
			assert(service!=null);		
			
			m_service = service;			
			initializeTestCase();
			
			int cIndex = testConstraintIndex(rd, buIndex, aNum, weight);
			if(cIndex>=0) {
				addTestCase2Buffer(buffer, cIndex, m_service);
				//buffer[cIndex][buIndex[cIndex]] = new HybridServiceTestCase(m_service);
				buIndex[cIndex]++;
				long current = System.currentTimeMillis();
				gts[gn] = current - start;
				gn++;
			} 
			
			fnum++;
		}
		
		
		int count = gn;
		int cp = 0;
		while(gn>0) {
			do {
				cp++;
				if(cp>=buIndex.length) cp = 0;				
			} while(buIndex[cp]<=0);
			
			buIndex[cp]--;
			pool.add(getTestCasefromBuffer(buffer, cp));
			
			gn--;
		}
		
		while(count<maxNum) {
			AbstractService service = LoadServiceScript.loadService(filename);
			assert(service!=null);		
			
			m_service = service;			
			initializeTestCase();
			
			pool.add(new HybridServiceTestCase(service));
			long current = System.currentTimeMillis();
			gts[count] = current - start;
			
			count++;			
		}
	}
	
	public void generateTestCasePool3(ArrayList<ServiceTestCase> pool, int maxNum, TestingScriptDefinition tsd, ArrayList<AbstractService> services) {
		int cNum = constraint_list.size();
		int aNum = maxNum /cNum + 1;
		Random rd = new Random(System.currentTimeMillis());
		
		HashMap<Integer, ArrayList<ServiceTestCase>> buffer = new HashMap<Integer, ArrayList<ServiceTestCase>>(); 
		int[] buIndex = new int[cNum];
		for(int i=0;i<cNum;i++)			
			buIndex[i] = 0;
		
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		int gn=0;
		int fnum = 0;
		while(gn<maxNum && fnum < 500) {
						
			AbstractService service = LoadServiceScript.loadService(filename);
			assert(service!=null);		
			
			m_service = service;			
			initializeTestCase();
			
			int cIndex = testConstraintIndex(rd, buIndex, aNum, null);
			if(cIndex>=0) {
				addTestCase2Buffer(buffer, cIndex, m_service);
				buIndex[cIndex]++;
				gn++;
			} 
			
			fnum++;
		}
		
		for(int i=0;i<buIndex.length;i++) {
			while(buIndex[i]<aNum) {
				
			}
		}
		
		int cp = 0;
		while(gn>0) {
			do {
				cp++;
				if(cp>=cNum) cp = 0;				
			} while(buIndex[cp]<=0);
			
			buIndex[cp]--;
			pool.add(getTestCasefromBuffer(buffer, cp));
			
			gn--;
		}
		
		
	}
	
	private ServiceTestCase getTestCasefromBuffer(HashMap<Integer, ArrayList<ServiceTestCase>> map, int cp) {
		ArrayList<ServiceTestCase> list = map.get(cp);
		return list.remove(0);
	}

	private void addTestCase2Buffer(
			HashMap<Integer, ArrayList<ServiceTestCase>> map, int cIndex, AbstractService service) {
		
		ArrayList<ServiceTestCase> list = map.get(cIndex);
		if(list==null) {
			list = new ArrayList<ServiceTestCase>();
			map.put(cIndex, list);
		}
		
		list.add(new HybridServiceTestCase(service));
	}

	private int testConstraintIndex(Random rd, int[] buIndex, int max, int[] weight) {
				
		int cp = rd.nextInt(buIndex.length);
		int tryout = 0;
		
		if(weight==null) {
			weight = new int[buIndex.length];
			for(int i=0;i<weight.length;i++) weight[i] = 1;
		}
		
		while(tryout<buIndex.length) {
			
			do{
	    		cp++;		    	
	    		if(cp>=buIndex.length) cp = 0;
	    		tryout++;
	    	} while(buIndex[cp]>=max*weight[cp]);
			
			ArrayList<MyConstraint> cont = constraint_list.get(cp);
			ConstraintSolver solver = getConstraintSolver(cont);
		    solver.setBindVariables(bindVariables);
		    boolean foundSolution = solver.generateNextResultWithoutBackward();
		    ArrayList<CSLVariable> solution = solver.getSolution();
		    
		    if(foundSolution) {
		    	initializeTestCase(m_service, solution);
		    	return cp;		    	
		    } 
		    		    
		}
				
		return -1;
	}

	public boolean nextTestCase2(AbstractService service) {
		ArrayList<CSLVariable> solution = null; 
		m_service = service;
		int num = constraint_list.size();
		
		ArrayList<ArrayList<CSLVariable>> cbu = getBuffer(cpointer);
		
		if(!cbu.isEmpty()) {
			solution = cbu.remove(0);			
		
		} else {
		
			boolean foundSolution = false;
			ArrayList<MyConstraint> cont = constraint_list.get(cpointer);			
									
			while(!foundSolution) {
				initializeTestCase();				
				
				ConstraintSolver solver = getConstraintSolver(cont);
			    solver.setBindVariables(bindVariables);
			    foundSolution = solver.generateNextResult();
			    solution = solver.getSolution();
			    			    				
				if(!foundSolution) {//try to find another constraint that match the test case and buffer it for late usage
					for(int i=0;i<num;i++) {
						if(i==cpointer) continue;
						
						ArrayList<MyConstraint> bu_cont = constraint_list.get(i);
						solver = getConstraintSolver(bu_cont);
					    solver.setBindVariables(bindVariables);
					    boolean bu_foundSolution = solver.generateNextResult();
					    ArrayList<CSLVariable> bu_solution = solver.getSolution();
					    
					    if(bu_foundSolution) {
					    	addBufferTestCase(i, bu_solution);
					    	break;
					    }
					}
				}
			}
		}			
				
		cpointer++;		
		if(cpointer >= num) cpointer=0;
		initializeTestCase(service, solution);

		return true;			
	}
	
	private void addBufferTestCase(
			int i, ArrayList<CSLVariable> bu_solution) {
		ArrayList<ArrayList<CSLVariable>> cbu = getBuffer(i);
		cbu.add(bu_solution);		
	}
	
	private ArrayList<ArrayList<CSLVariable>> getBuffer(int index) {
		ArrayList<ArrayList<CSLVariable>> cbu = buffer.get(index);
		if(cbu==null) {
			cbu = new ArrayList<ArrayList<CSLVariable>>();
			buffer.put(index, cbu);
		}
		
		return cbu;
	}


	private ConstraintSolver getConstraintSolver(ArrayList<MyConstraint> constraint) {
		ConstraintSolver solver = new ConstraintSolver();
		for(MyConstraint mc: constraint) {
			solver.addConstraint(mc.backup());
		}
			
		Set<String> sn_list = variable_schema.keySet();
		for(String sn: sn_list) {
			solver.setVariableSchema(sn, variable_schema.get(sn));
		}							
		
		return solver;
	}	
	
	private void initializeTestCase(AbstractService service, ArrayList<CSLVariable> solution) {
		State cs = m_service.getState();
		String sn = service.getName();
        assert(cs!=null);
        
        Random rd = new Random(System.currentTimeMillis());
        
        ArrayList<Variable> variables = cs.getVariables();
        for(Variable v: variables) {
        	String type = v.getType();
        	String vn = v.getName();
        	
        	CSLVariable csl = getVariableByName(solution, sn, vn, type);
        	
        	if(csl!=null) { //assign the value
        		
        		v.setValue(csl.getValue());
        		
        	} else {//random initialization
        		        	        	
        		if(type.equals(DataType.BOOLEAN)) {//boolean
        			v.setValue(rd.nextBoolean());
        		} else
        		
        		if(type.equals(DataType.INTEGER)) { //Integer
        			int initValue = (Integer)v.getValue();
        			if(initValue == -1) //that means not initialized yet
        				v.setValue(rd.nextInt(Config.getConfig().variableDomain));
        		} else
        			v.setValue(""+rd.nextLong()); //String
        	}
        }        
	}
	
	private CSLVariable getVariableByName(ArrayList<CSLVariable> solution,
			String sn, String vn, String type) {
		
		for(CSLVariable csl: solution) {
			if(sn.equals(csl.getServiceName()) && vn.equals(csl.getName()) 
					&& type.equals(csl.getType()))
			    return csl;
		}
		
		return null;
	}	

	public void setConstraint_list(ArrayList<ArrayList<MyConstraint>> constraint_list) {
		this.constraint_list = constraint_list;
	}

	public ArrayList<ArrayList<MyConstraint>> getConstraint_list() {
		return constraint_list;
	}
	
	public void addServiceSchema(String sn, HashMap<String, String> map) {
		variable_schema.put(sn, map);
	}

	private HashMap<String, HashMap<String, String>> variable_schema = new HashMap<String, HashMap<String, String>>();
	
	private ArrayList<ArrayList<MyConstraint>> constraint_list;
}
