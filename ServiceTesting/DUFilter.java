package ServiceTesting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import scripts.TestingScript.TestCaseType;
import scripts.TestingScript.TestingScriptDefinition;
import Configuration.Config;
import Deployment.LoadServiceScript;
import Service.AbstractService;
import ServiceTesting.ConstraintSolver.ConstraintSolver;
import ServiceTesting.ConstraintSolver.MyConstraint;
import ServiceTesting.Monitoring.CoverageMonitor;

public class DUFilter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DUFilter filter = new DUFilter();
		
		filter.FilterDU(args);
	}
	
	public void FilterDU(String[] args) {
		//Config.getConfig().variableDomain = 5;
		
		if(args.length>=5) {
			testscript = args[0];						 
			outdir = args[1];
			appendix = args[2];	
			coverageStrategy = new Integer(args[3]);
			exposedPercentage = new Double(args[4]);
		}
		
		TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript);
		ServiceTesting.isBaseline = false;		
		CoverageMonitor coverage_monitor = ServiceTesting.constructCoverageMonitor(tsd, coverageStrategy);
		coverage_monitor.setExposedPercentage(exposedPercentage);
		
		ArrayList<AbstractService> services = ServiceTesting.loadTestingService(tsd);
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);		
		String filename = tct.getName();
		assert(filename!=null);		
		AbstractService service = LoadServiceScript.loadService(filename);
		assert(service!=null);		
		services.add(service);
		
		for(AbstractService as: services)
			addServiceSchema(as.getName(), as.getVariableSchema());
		
		ArrayList<ArrayList<MyConstraint>> constraint_list = coverage_monitor.getConstraints(coverageStrategy, testscript);
				
		printConstraint(constraint_list);				
		
		for(int i=0;i<constraint_list.size();i++) {
			ArrayList<MyConstraint> cont = constraint_list.get(i);
			conIndex = ""+i;
			long start = System.currentTimeMillis();
			verifyConstraints(cont);
			long end = System.currentTimeMillis();
			
			System.out.println(end - start);
		}
				
	}
	
	private String testscript;
	private int coverageStrategy = 3;
	private String appendix;
	private String outdir;
	private String conIndex;
	private double exposedPercentage = 1;

	private HashMap<String, HashMap<String,String>> variable_schema = new HashMap<String, HashMap<String,String>>();
	private void addServiceSchema(String sn, HashMap<String, String> map) {
		variable_schema.put(sn, map);
	}
	
	private void verifyConstraints(ArrayList<MyConstraint> cont) {
		ConstraintSolver solver = getConstraintSolver(cont);
		boolean hasresult = solver.generateNextResult();
		if(!hasresult) {
			outputConstraint(cont);
			System.out.println(conIndex + "has no solution!");
		} else 
			System.out.println(conIndex + "has a solution!");
	}	
	
	private void outputConstraint(ArrayList<MyConstraint> cont) {
		String filename = outdir + "/" + appendix + conIndex + ".txt";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));			
			for(MyConstraint mc: cont) {
				writer.write(mc.serviceName);
				writer.newLine();
				writer.write(mc.condition);
				writer.newLine();
			}
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private ConstraintSolver getConstraintSolver(ArrayList<MyConstraint> constraint) {
		ConstraintSolver solver = new ConstraintSolver();
		
		for(MyConstraint mc: constraint)
			solver.addConstraint(mc);
					
		Set<String> sn_list = variable_schema.keySet();
		for(String sn: sn_list) {
			solver.setVariableSchema(sn, variable_schema.get(sn));
		}
							
		return solver;
	}	
	
	private void printConstraint(ArrayList<ArrayList<MyConstraint>> constraint_list) {
		int index=0;
		for(ArrayList<MyConstraint> mc_list: constraint_list) {
			System.out.print(index + ":");
			printConstraintList(mc_list);
			index++;
		}
	}

	private void printConstraintList(ArrayList<MyConstraint> mc_list) {
		for(MyConstraint mc: mc_list) {
			System.out.print(mc.condition + ";");
		}
		System.out.println();
	}

}
