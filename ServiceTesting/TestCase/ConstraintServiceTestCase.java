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

public class ConstraintServiceTestCase extends ServiceTestCase {

	public ConstraintServiceTestCase(AbstractService service) {
		super(service);		
	}

	@Override
	public void initializeTestCase() {
		
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
	
	public void generateTestCasePool(TestingScriptDefinition tsd, ArrayList<ServiceTestCase> pool, int[] weight, int max, long[] gts) {
		long start = System.currentTimeMillis();
		int num = constraint_list.size();
		if(weight==null) {
			weight = new int[num];
			for(int i=0;i<num;i++) weight[i] = max/num;
		} else {
			int cn = 0;
			for(int w:weight) cn+=w;
			for(int i=0;i<weight.length;i++)
				weight[i] = max * weight[i]/cn; 
		}
		
		ServiceTestCase[][] buffer = new ServiceTestCase[num][];
		int[] buIndex = new int[num];
		for(int i=0;i<buIndex.length;i++) buIndex[i]=0;
		
		int gn=0;
		int cp = 0;
		while(gn<max && cp<weight.length) {
						
			int tryout=0;
			buffer[cp] = new ServiceTestCase[weight[cp]];
			while(tryout<weight[cp]) {
				ArrayList<MyConstraint> cont = constraint_list.get(cp);
				ConstraintSolver solver = getConstraintSolver(cont);
				boolean hasresult = solver.generateNextResult();
				if(!hasresult) break;
				
				TestCaseType tct = tsd.getTestCase();
				assert(tct!=null);
				
				String filename = tct.getName();
				assert(filename!=null);
				
				AbstractService service = LoadServiceScript.loadService(filename);
				assert(service!=null);				
                
				ArrayList<CSLVariable> solution = solver.getSolution();
				ConstraintServiceTestCase cts = new ConstraintServiceTestCase(service);
				cts.initializeTestCase(service, solution);
				//pool.add(cts);
				buffer[cp][tryout] = cts;
				buIndex[cp]++;
				
				long current = System.currentTimeMillis();
				gts[gn] = current - start;
				gn++;
				tryout++;
			}
			
			cp++;						
		}
		
		int count = gn;
		cp = 0;
		while(count>0) {
			do {
				cp++;
				if(cp>=buIndex.length) cp = 0;				
			} while(buIndex[cp]<=0);
			
			buIndex[cp]--;
			pool.add(buffer[cp][buIndex[cp]]);
			
			count--;
		}
		
		while(gn<max) {
			TestCaseType tct = tsd.getTestCase();
			assert(tct!=null);
			
			String filename = tct.getName();
			assert(filename!=null);
			
			AbstractService service = LoadServiceScript.loadService(filename);
			assert(service!=null);		
			ConstraintServiceTestCase cts = new ConstraintServiceTestCase(service);
			cts.initializeTestCase();
			
			long current = System.currentTimeMillis();
			gts[gn] = current - start;
			
			pool.add(cts);			
			gn++;
		}
		
	}

	private int cpointer = -1;
	public boolean nextTestCase(AbstractService service) {
		m_service = service;
		int trycount = 0;
		
		int num = constraint_list.size();
		ArrayList<ArrayList<MyConstraint>> nosolutions = new ArrayList<ArrayList<MyConstraint>>();		
		
		if(cpointer<0) {
			Random rd = new Random(System.currentTimeMillis());
			cpointer = rd.nextInt(num);
		}
		
		while(trycount < num) {
			cpointer++;		
			if(cpointer >= num) cpointer=0;
			
			ArrayList<MyConstraint> cont = constraint_list.get(cpointer);
			ConstraintSolver solver = getConstraintSolver(cont);
			boolean hasresult = solver.generateNextResult();
			
			if(hasresult) {
				constraint_list.removeAll(nosolutions);
				cpointer = constraint_list.indexOf(cont);
				
				ArrayList<CSLVariable> solution = solver.getSolution();
				
				initializeTestCase(service, solution);
				return true;
				
			} else
				nosolutions.add(cont);
			
			trycount++;
		}
		
		return false;
	}
	
	private ConstraintSolver getConstraintSolver(ArrayList<MyConstraint> constraint) {
		ConstraintSolver solver = solvers.get(constraint);
		
		if(solver == null) {
			solver = new ConstraintSolver();
			for(MyConstraint mc: constraint)
				solver.addConstraint(mc);
			
			Set<String> sn_list = variable_schema.keySet();
			for(String sn: sn_list) {
				solver.setVariableSchema(sn, variable_schema.get(sn));
			}
			
			solvers.put(constraint, solver);//block it for test case generation time exp
		}
		
		return solver;
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
	
	private HashMap<ArrayList<MyConstraint>, ConstraintSolver> solvers = new HashMap<ArrayList<MyConstraint>, ConstraintSolver>();
}
