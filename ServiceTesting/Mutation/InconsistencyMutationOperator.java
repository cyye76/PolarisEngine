package ServiceTesting.Mutation;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


import scripts.MutationScript.MutationDefinition;
import scripts.MutationScript.MutationType;
import scripts.ServiceScript.ActivityDefinition;
import scripts.ServiceScript.ActivityList;
import scripts.ServiceScript.ActivityType;
import scripts.ServiceScript.DataType;
import scripts.ServiceScript.ServiceDefinition;
import scripts.ServiceScript.VariableType;
import Configuration.Config;
import Service.Variable;
import Utils.XMLProcessing;

public class InconsistencyMutationOperator {	

   public static void main(String[] args) {
             //oldmain(args);
             //new InconsistencyMutationOperator().mutation(INServices);
           //isLoop = false;
	   //new InconsistencyMutationOperator().mutation(BOServices);
	   //new InconsistencyMutationOperator().mutation(LAServices);
	   //new InconsistencyMutationOperator().mutation(SCServices);
	   //new InconsistencyMutationOperator().mutation(INServices);
       	   isLoop = true;
       	   new InconsistencyMutationOperator().mutation(AUServices);

    }   
		
	private static String[] BOServices = {
				"Applications/BookOrdering/OrderProcess.xml",
				"Applications/BookOrdering/CreditCardService.xml"
		//"tmp/Applications/BookOrdering/OrderProcess.xml",
		//"tmp/Applications/BookOrdering/CreditCardService.xml"
	};
		
	private static String[] LAServices = {
				"Applications/LoanApproval/Approval.xml",
				"Applications/LoanApproval/LoanProcess.xml",
				"Applications/LoanApproval/RiskAssessment.xml"
	};
		
	private static String[] SCServices = {
			    "Applications/SupplyChain/Manufacturer.xml",
			    "Applications/SupplyChain/Retailer.xml"
	};
	
    private static String[] INServices = {
			    "Applications/Insurance/AGFIL.xml",
			    "Applications/Insurance/EuropAssist.xml",
			    "Applications/Insurance/LeeCS.xml",
			    "Applications/Insurance/Assessor.xml",
			    "Applications/Insurance/Garage.xml"
	};
    
	private static String[] AUServices = {
	    "Applications/Auction/Buyer1.xml",
	    "Applications/Auction/Saler.xml"
    };
    
	
	
	private static void oldmain(String[] args) {
		new InconsistencyMutationOperator().mutation(BOServices);
		new InconsistencyMutationOperator().mutation(LAServices);
		new InconsistencyMutationOperator().mutation(SCServices);
	}
	
	public void mutation(String[] serviceNames) {
		int subindex = serviceNames[0].lastIndexOf("/");
		String path = serviceNames[0].substring(0,subindex+1);
		path += "INCMutations/";
		
		try {
			
			ArrayList<ServiceDefinition> sdefs = new ArrayList<ServiceDefinition>();
			for(String serviceName: serviceNames) {
				FileInputStream input = new FileInputStream(serviceName);			
				ServiceDefinition service = XMLProcessing.unmarshal(ServiceDefinition.class, input);
				assert(service!=null);
				
				sdefs.add(service);
			}
			
			generateINCMutation(sdefs, path);						
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void generateINCMutation(ArrayList<ServiceDefinition> sdefs, String path) {
		
		for(int i=0;i<sdefs.size();i++)
			for(int j=i+1;j<sdefs.size();j++) {
				ServiceDefinition s1 = sdefs.get(i);
				ServiceDefinition s2 = sdefs.get(j);
				
				generateINCMuation(s1, s2, path);
			}					
	}

	private void generateINCMuation(ServiceDefinition s1, ServiceDefinition s2,
			String path) {
		
		ActivityList al1 = s1.getActivities();
		assert(al1!=null);
		
		ActivityList al2 = s2.getActivities();
		assert(al2!=null);
				
		List<ActivityDefinition> activities1 = al1.getActivity();
		List<ActivityDefinition> activities2 = al2.getActivity();
		for(ActivityDefinition activity1: activities1) {
			ActivityType type1 = activity1.getType();
			if(type1.equals(ActivityType.LOCAL_TASK)) 
				for(ActivityDefinition activity2: activities2) {
					ActivityType type2 = activity2.getType();
					if(type2.equals(ActivityType.LOCAL_TASK)) 
						if(isLoop) {
							//MutationINCTasks4LoopNew(s1, activity1, s2, activity2, path);
							MutationINCTasks4Loop(s1, activity1, s2, activity2, path);
						} else 
						   // MutationINCTasks(s1, activity1, s2, activity2, path);
						    MutationINCTasksNew(s1, activity1, s2, activity2, path);

				}
			
		}		
		
	}

	private void MutationINCTasks(ServiceDefinition s1, ActivityDefinition activity1,
			ServiceDefinition s2, ActivityDefinition activity2, String path) {
		
		//Random rd = new Random(System.currentTimeMillis());
		//int value1 = rd.nextInt(Config.getConfig().variableDomain) + 1;
		//int value2 = rd.nextInt(Config.getConfig().variableDomain) + 1;		 
		
		//String mt1 = MVName + ":=" + value1 + ";";
		//String mt2 = MVName + ":=" + value2 + ";";

		String mt1 = MVName + ":= RandomVariable;";
		String mt2 = MVName + ":= RandomVariable;";

		
		String mtfn1 = TaskMutation(s1, activity1, path, mt1);
		String mtfn2 = TaskMutation(s2, activity2, path, mt2);
		
		//record all the filenames of mutations
		try {
			String categoryfilename = path + "mutations.txt";
			BufferedWriter writer = new BufferedWriter(new FileWriter(categoryfilename,true));
			String line = mtfn1 + "   " + mtfn2;
			writer.write(line);
			writer.newLine();			
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private static Random rd_static = new Random(System.currentTimeMillis());
	private String selectVariable(ServiceDefinition s1) {
		
		List<VariableType> vlist = s1.getVariables().getVariable();
		ArrayList<String> candidates = new ArrayList<String>();
		for(VariableType vt: vlist) {
			if(vt.getType().equals(DataType.INTEGER)) candidates.add(vt.getName()); 
		}
		
		candidates.remove("ICMutationVariable");
		candidates.remove("RandomVariable");
		candidates.remove("ICLoop");
		candidates.remove("ID1");
		candidates.remove("ID2");
		candidates.remove("ID3");
		candidates.remove("ID4");
		candidates.remove("ID5");
		candidates.remove("ID6");
		candidates.remove("ID7");
		candidates.remove("ID8");
		candidates.remove("ID9");
		candidates.remove("ID10");
		
		int num = candidates.size();
		//int sel = index - (index /num) * num;		
		int index = rd_static.nextInt(num);
		return candidates.get(index);
	}
	
    //private static int vindex1 = 0;
    //private static int vindex2 = 0;
	private void MutationINCTasksNew(ServiceDefinition s1, ActivityDefinition activity1,
			ServiceDefinition s2, ActivityDefinition activity2, String path) {
		
		Random rd = new Random(System.currentTimeMillis());
		String vname1 = selectVariable(s1);
		String vname2 = selectVariable(s2);		
		
		System.out.println(vname1 + ", " + vname2);
		
		//Random rd = new Random(System.currentTimeMillis());
		int value1 = rd.nextInt(Config.getConfig().variableDomain/4) + 1;
		int value2 = rd.nextInt(Config.getConfig().variableDomain/4) + 1;
		
		String mt1 = MVName + ":=" + vname1 + " + " + value1 + " ;";
		String mt2 = MVName + ":=" + vname2 + " + " + value2 + " ;";
		
		//String mt1 = MVName + ":=" + vname1 + ";";
		//String mt2 = MVName + ":=" + vname2 + ";";		
		
		String mtfn1 = TaskMutation(s1, activity1, path, mt1);
		String mtfn2 = TaskMutation(s2, activity2, path, mt2);
		
		//record all the filenames of mutations
		try {
			String categoryfilename = path + "mutations.txt";
			BufferedWriter writer = new BufferedWriter(new FileWriter(categoryfilename,true));
			String line = mtfn1 + "   " + mtfn2;
			writer.write(line);
			writer.newLine();			
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	private static String LVName = "ICLoop";
	private static String RDName = "round";
	private static int loopLevel = 2;
	private static String[] nolooptasks = {"init"};
	private static boolean isLoop = false;
	
	private void MutationINCTasks4LoopNew(ServiceDefinition s1, ActivityDefinition activity1,
			ServiceDefinition s2, ActivityDefinition activity2, String path) {
		
		for(int i=1;i<=loopLevel;i++) {						
			
			//Random rd = new Random(System.currentTimeMillis());
			//int value1 = rd.nextInt(Config.getConfig().variableDomain) + 1;
		    //int value2 = rd.nextInt(Config.getConfig().variableDomain) + 1;		 
		
			//String mt1 = MVName + ":=" + value1 + ";" + LVName + ":=" + LVName + "+" + RDName + "/" +  i + ";" ;
			//String mt2 = MVName + ":=" + value2 + ";" + LVName + ":=" + LVName + "+" + RDName + "/" +  i + ";" ;
			String mt1 = MVName + ":= RandomVariable;" + LVName + ":=" + LVName + "+" + RDName + "/" +  i + ";" ;
			String mt2 = MVName + ":= RandomVariable;" + LVName + ":=" + LVName + "+" + RDName + "/" +  i + ";" ;
			
			String mtfn1 = TaskMutation(s1, activity1, path, mt1);
			String mtfn2 = TaskMutation(s2, activity2, path, mt2);
		
			//record all the filenames of mutations
			try {
				String categoryfilename = path + "mutations.txt";
				BufferedWriter writer = new BufferedWriter(new FileWriter(categoryfilename,true));
				String line = mtfn1 + "   " + mtfn2;
				writer.write(line);
				writer.newLine();			
				writer.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			String tn1 = activity1.getName();
			String tn2 = activity2.getName();
			boolean quit = false;
			for(String nlt: nolooptasks) {
				if(tn1.equals(nlt) || tn2.equals(nlt)) {
					quit = true;
					break;
				}					
			}
						
			if(quit) break;
		}
	}
	
	private void MutationINCTasks4Loop(ServiceDefinition s1, ActivityDefinition activity1,
			ServiceDefinition s2, ActivityDefinition activity2, String path) {
		
		for(int i=1;i<=loopLevel;i++) {						
			
		        String vname1 = selectVariable(s1);
		        String vname2 = selectVariable(s2);
		
			Random rd = new Random(System.currentTimeMillis());
			int value1 = rd.nextInt(Config.getConfig().variableDomain/4) + 1;
			int value2 = rd.nextInt(Config.getConfig().variableDomain/4) + 1;		 
		
			String mt1 = MVName + ":=" + vname1 + " + " + value1 + " ;" + LVName + ":=" + LVName + "+" + RDName + "/" +  i + ";" ;
			String mt2 = MVName + ":=" + vname2 + " + " + value2 + " ;" + LVName + ":=" + LVName + "+" + RDName + "/" +  i + ";" ;
		
			String mtfn1 = TaskMutation(s1, activity1, path, mt1);
			String mtfn2 = TaskMutation(s2, activity2, path, mt2);
		
			//record all the filenames of mutations
			try {
				String categoryfilename = path + "mutations.txt";
				BufferedWriter writer = new BufferedWriter(new FileWriter(categoryfilename,true));
				String line = mtfn1 + "   " + mtfn2;
				writer.write(line);
				writer.newLine();			
				writer.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			String tn1 = activity1.getName();
			String tn2 = activity2.getName();
			boolean quit = false;
			for(String nlt: nolooptasks) {
				if(tn1.equals(nlt) || tn2.equals(nlt)) {
					quit = true;
					break;
				}					
			}
						
			if(quit) break;
		}
	}
	
	private String TaskMutation(ServiceDefinition service, ActivityDefinition task, String path, String mt) {
		String serviceName = service.getName();
		String taskName = task.getName();
		
		String effects = task.getEffect();
		effects += mt;

		String mutationName = getMutationName(serviceName, taskName, path);			
		String mtfilename = path + mutationName + ".xml";
		createMutation(mutationName, MutationType.TASK_MUTATION, serviceName, effects, mtfilename);		
		
		return mtfilename;
	}

	private void createMutation(String mutationName, MutationType taskMutation,
			String serviceName, String mt, String mtfilename) {
		
		scripts.MutationScript.ObjectFactory factory = new scripts.MutationScript.ObjectFactory();
		MutationDefinition md = factory.createMutationDefinition();
		
		md.setMutationName(mutationName);
		md.setServiceName(serviceName);
		md.setType(taskMutation);
		md.setContents(mt);
				
		try {				
			XMLProcessing.writeDocument(factory.createMutation(md), ExpressionMutationOperator.getXMLSerializer(mtfilename).asContentHandler());			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static String MVName = "ICMutationVariable";
	//private static String MVType = DataType.INTEGER;
	//private static int mutationNo = 1;
	private static int mutationNo = 1000;
	private String getMutationName(String serviceName, String taskName, String path) {
		String name = mutationNo + "_" + serviceName + "_" + taskName;
		mutationNo++;
		return name;
	}
	
	
}
