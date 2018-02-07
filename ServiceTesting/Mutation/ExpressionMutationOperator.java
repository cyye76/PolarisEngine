package ServiceTesting.Mutation;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;


import scripts.MutationScript.MutationDefinition;
import scripts.MutationScript.MutationType;
import scripts.ServiceScript.ActivityDefinition;
import scripts.ServiceScript.ActivityList;
import scripts.ServiceScript.ActivityType;
import scripts.ServiceScript.DataType;
import scripts.ServiceScript.ServiceDefinition;
import scripts.ServiceScript.TransitionList;
import scripts.ServiceScript.TransitionType;
import scripts.ServiceScript.VariableType;
import scripts.ServiceScript.VariablesListType;
import Utils.WordParser;
import Utils.XMLProcessing;

public class ExpressionMutationOperator extends AbstractMutationOperator {
	
	private static String appID = "";
	private static int MI = 1;
	private static HashMap<String, Integer> mt_cache = null;
	private static String mtfn = "tmp/mtcache.txt";
	private static String[] LAnl= {"tmp/Applications/LoanApproval/Approval.xml", "tmp/Applications/LoanApproval/LoanProcess.xml","tmp/Applications/LoanApproval/RiskAssessment.xml"};
	private static String[] BOnl= {"tmp/Applications/BookOrdering/OrderProcess.xml", "tmp/Applications/BookOrdering/CreditCardService.xml"};
	private static String[] SCnl= {"tmp/Applications/SupplyChain/Manufacturer.xml", "tmp/Applications/SupplyChain/Retailer.xml"};
	private static String[] INnl={"tmp/Applications/Insurance/AGFIL.xml", "tmp/Applications/Insurance/Garage.xml", "tmp/Applications/Insurance/EuropAssist.xml", "tmp/Applications/Insurance/LeeCS.xml", "tmp/Applications/Insurance/Assessor.xml"};
	private static String[] ACnl={"tmp/Applications/Auction/Buyer1.xml","tmp/Applications/Auction/Saler.xml"};
	
	public static void statistics() {
		mt_cache = new HashMap<String, Integer>();
		
		appID = "LASF";
		for(String fn: LAnl)
		  new ExpressionMutationOperator().mutation(fn);
		
		MI=1;
		appID = "BOSF";
		for(String fn: BOnl)
			new ExpressionMutationOperator().mutation(fn);
		
		MI=1;
		appID = "SCSF";
		for(String fn: SCnl)
			new ExpressionMutationOperator().mutation(fn);
		
		
		MI=1;
		appID = "INSF";
		for(String fn: INnl)
			new ExpressionMutationOperator().mutation(fn);
		
		MI=1;
		appID = "ACSF";
		for(String fn: ACnl)
			new ExpressionMutationOperator().mutation(fn);
		
		outputMTCache();
		
		int[][] ts = new int[5][3];		
		for(int i=0;i<ts.length;i++)
			for(int j=0;j<3;j++)
			     ts[i][j] = 0;
		
		Set<String> keys = mt_cache.keySet();
		for(String key: keys) {
			int type = mt_cache.get(key);
			if(key.startsWith("LASF"))
			   ts[0][type]++;
			else
			
			if(key.startsWith("BOSF"))
				ts[1][type]++;			
			else
			
			if(key.startsWith("SCSF"))
				ts[2][type]++;
			
			else
			
			if(key.startsWith("INSF"))
				ts[3][type]++;
			else
				
			if(key.startsWith("ACSF"))
				ts[4][type]++;
			
		}
		
		for(int i=0;i<ts.length;i++)
		   System.out.println("Operator:" + ts[i][0] + ", Variable:" + ts[i][1] + ", Constant:" + ts[i][2]);
		
	}
	

	public void mutation(String serviceName) {
		int subindex = serviceName.lastIndexOf("/");
		String path = serviceName.substring(0,subindex+1);
		path += "Mutations/";
		
		try {
            FileInputStream input = new FileInputStream(serviceName);			
			ServiceDefinition service = XMLProcessing.unmarshal(ServiceDefinition.class, input);
			assert(service!=null);
			
			generateTaskMutation(service, path);
			
			generateTransitionMutation(service, path);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
				
	}
	
	private static void outputMTCache() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(mtfn,true));
			Set<String> keys = mt_cache.keySet();			
			for(String key: keys) {
				int type = mt_cache.get(key);
				String line = key + " " + type;
				writer.write(line);
				writer.newLine();
			}
			
			writer.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void generateTransitionMutation(ServiceDefinition service, String path) {
		TransitionList tl = service.getTransitions();
		assert(tl!=null);
		
		List<TransitionType> trlist = tl.getTransition();
		for(TransitionType tt: trlist) {
			TransitionMutation(service, tt, path);
		}
	}
	
	private void TransitionMutation(ServiceDefinition service,
			TransitionType tt, String path) {
		String serviceName = service.getName();
		String transName = tt.getName();
		
		String guard = tt.getGuard();
		HashMap<String, DataType> variables = getVariableDefinition(service);
		
		ArrayList<String> mutations = generateEffectMutations(guard, variables);
		ArrayList<String> mNames = new ArrayList<String>();
		
		int maxMuNumPerTrans = 100;
		
		for(String mt: mutations) {
			String mutationName = getMutationName(serviceName, transName, path);			
			String filename = path + mutationName + ".xml";
			mNames.add(filename);
			createMutation(mutationName, MutationType.TRANSITION_MUTATION, serviceName, mt, filename);
			
			maxMuNumPerTrans--;
			if(maxMuNumPerTrans <= 0) break;
		}
		
		
		//record all the filenames of mutations
		try {
			String filename = path + "mutations.txt";
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true));
			for(String name: mNames) {
				writer.write(name);
				writer.newLine();
			}
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	private void generateTaskMutation(ServiceDefinition service, String path) {
		ActivityList al = service.getActivities();
		assert(al!=null);
		
		List<ActivityDefinition> activities = al.getActivity();
		for(ActivityDefinition activity: activities) {
			ActivityType type = activity.getType();
			if(type.equals(ActivityType.LOCAL_TASK)) {
				TaskMutation(service, activity, path);
			}
			
			if(type.equals(ActivityType.SERVICE)) {
				ServiceMutation(service, activity, path);
			}
		}
	}
	
	private void ServiceMutation(ServiceDefinition service,
			ActivityDefinition task, String path) {
		
		String serviceName = service.getName();
		String taskName = task.getName();
		
		String effects = task.getEffect();
		HashMap<String, DataType> variables = getVariableDefinition(service);
		
		ArrayList<String> mutations = generateServiceEffectMutations(effects, variables);
		ArrayList<String> mNames = new ArrayList<String>();
		for(String mt: mutations) {
			String mutationName = getMutationName(serviceName, taskName, path);			
			String filename = path + mutationName + ".xml";
			mNames.add(filename);
			createMutation(mutationName, MutationType.TASK_MUTATION, serviceName, mt, filename);
		}
		
		
		//record all the filenames of mutations
		try {
			String filename = path + "mutations.txt";
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true));
			for(String name: mNames) {
				writer.write(name);
				writer.newLine();
			}
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	private ArrayList<String> generateServiceEffectMutations(String effects,
			HashMap<String, DataType> variables) {
		ArrayList<String> result = new ArrayList<String>();
		WordParser parser = new WordParser(effects);
		
		String[] expression = parser.getExpression();
		for(int i=0;i<expression.length;i++) {
			String word = expression[i];
			
			if(word.equals(":=")) {
				i++;//skip the name of the service
				continue;
			}
			if(word.equals(",")) continue;
			if(word.equals("(")) continue;
			if(word.equals(")")) continue;
			
			ArrayList<String> mutations = generateSingleMutation(word, variables);
			for(String mt: mutations) {
				String mt_effects = constructMutantEffect(expression, i, mt);
				result.add(mt_effects);
			}
		}		
		
		return result;
	}

	private void TaskMutation(ServiceDefinition service, ActivityDefinition task, String path) {
		String serviceName = service.getName();
		String taskName = task.getName();
		
		String effects = task.getEffect();
		HashMap<String, DataType> variables = getVariableDefinition(service);
		
		ArrayList<String> mutations = generateEffectMutations(effects, variables);
		ArrayList<String> mNames = new ArrayList<String>();
		for(String mt: mutations) {
			String mutationName = getMutationName(serviceName, taskName, path);			
			String filename = path + mutationName + ".xml";
			mNames.add(filename);
			createMutation(mutationName, MutationType.TASK_MUTATION, serviceName, mt, filename);
		}
		
		
		//record all the filenames of mutations
		try {
			String filename = path + "mutations.txt";
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true));
			for(String name: mNames) {
				writer.write(name);
				writer.newLine();
			}
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String MVName = "ICMutationVariable";
	private static int mutationNo = 1;
	private String getMutationName(String serviceName, String taskName, String path) {
		String name = mutationNo + "_" + serviceName + "_" + taskName;
		mutationNo++;
		return name;
	}

	private ArrayList<String> generateEffectMutations(String effects,
			HashMap<String, DataType> variables) {
		ArrayList<String> result = new ArrayList<String>();
		
		WordParser parser = new WordParser(effects);
		String[] expression = parser.getExpression();
		for(int i=0;i<expression.length;i++) {
			String word = expression[i];
			ArrayList<String> mutations = generateSingleMutation(word, variables);
			for(String mt: mutations) {
				String mt_effects = constructMutantEffect(expression, i, mt);
				result.add(mt_effects);
			}
		}		
		
		return result;
	}

	private String constructMutantEffect(String[] expression, int i, String mt) {
		String result = "";
		for(int j=0;j<i;j++) {
			result += expression[j];
			result += " ";
		}
		
		result += mt;
		result += " ";
		
		for(int j=i+1;j<expression.length;j++) {
			result += expression[j];
			result += " ";
		}
		
		return result;
	}
	
	/*
	 * type:0 Operator, 1 Variable, 2 Constant
	 */
	private static void cacheMT(int type) {
		MI++;
		mt_cache.put(appID+MI, type);
	}
	
	private ArrayList<String> generateSingleMutation(String word,
			HashMap<String, DataType> variables) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		//mutation for boolean operators
		if("&&".equals(word)) {
			result.add("||");
			cacheMT(0);
			return result;
		}
		
		if("||".equals(word)) {
			result.add("&&");
			cacheMT(0);
			return result;
		}
		
		if("!".equals(word)) {
			result.add("");
			cacheMT(0);
			return result;
		}
		
		//mutation for logic operators
		if("&".equals(word)) {
			result.add("|");
			cacheMT(0);
			return result;
		}
		
		if("|".equals(word)) {
			result.add("&");
			cacheMT(0);
			return result;
		}
		
		
		if("==".equals(word)) {
			result.add("!=");
			cacheMT(0);
			return result;
		}
		
		if("!=".equals(word)) {
			result.add("==");
			cacheMT(0);
			return result;
		}
		
		//mutation for predicate operators
		//String[] predicateOps = {"==", "<=", ">=", "!=", "<", ">"};
		String[] predicateOps = {"<=", ">=", "<", ">"};
		int wordindex = -1;
		for(int i=0;i<predicateOps.length;i++) {
			if(predicateOps[i].equals(word)) {
				wordindex = i;
				break;
			}
		}
		
		if(wordindex>=0) {
			for(int i=0;i<predicateOps.length;i++) {
			    if(i==wordindex) continue;
				result.add(predicateOps[i]);
				cacheMT(0);
			}
			
			return result;
		}
		
		//mutation for mathematical operators
		String[] mathOps = {"+", "-", "*", "/"};
		wordindex = -1;
		for(int i=0;i<mathOps.length;i++) {
			if(mathOps[i].equals(word)) {
				wordindex = i;
				break;
			}
		}
		
		if(wordindex>=0) {
			for(int i=0;i<mathOps.length;i++) {
			    if(i==wordindex) continue;
				result.add(mathOps[i]);
				cacheMT(0);
			}
			
			return result;
		}
		
		//handle variable mutation
		//new version:replace one variable instead of all
		DataType type = variables.get(word);
		if(type!=null) {
			Set<String> keys = variables.keySet();
			ArrayList<String> buffer = new ArrayList<String>();
			for(String vname: keys) {
				if(vname.equals(word)) continue;
				
				DataType v_type = variables.get(vname);
				if(v_type.equals(type)) 
					//result.add(vname);
					buffer.add(vname);
			}
			
			int length = buffer.size();
			if(length>0) {
			   Random rd = new Random(System.currentTimeMillis());
			   int index = rd.nextInt(length);
			   result.add(buffer.get(index));
			   cacheMT(1);
			}
			return result;
		}
		
		//mutation for constants
		if(isInteger(word)) {
			Random rd = new Random(System.currentTimeMillis());
			int value = new Integer(word);			
			result.add("" + (rd.nextInt(value * 2 + 2) + 1));
			cacheMT(2);
			return result;
		}
		
		//mutation for boolean constants
		if("true".equals(word)) {
			result.add("false");
			cacheMT(2);
			return result;
		}
		
		if("false".equals(word)) {
			result.add("true");
			cacheMT(2);
			return result;
		}

		//mutation for String constants
		//How?
		
		return result;
	}

	private boolean isInteger(String word) {
		try {
		    Integer.valueOf(word);
		    return true;
		}catch(Exception e) {
			return false;
		}		
	}

	private HashMap<String, DataType> getVariableDefinition(ServiceDefinition service) {
		HashMap<String, DataType> map = new HashMap<String, DataType>();
		VariablesListType vlt = service.getVariables();
		assert(vlt!=null);
		
		List<VariableType> variables = vlt.getVariable();
		for(VariableType vt: variables) {						
			String vname = vt.getName();
			DataType vtype = vt.getType();

			//exclude the inconsistency mutation variable
			if(!vname.equals(MVName)) map.put(vname, vtype);
		}
		
		return map;
	}
	
	/*
	 * For testing
	 */
	public static void main(String[] args) {		
		
		//createMutation("mutation1",MutationType.TASK_MUTATION, "service1", "a:=x>=2 && y<z;","muation1.xml");
		
		//new ExpressionMutationOperator().mutation(args[0]);
		
		statistics();
	}
	
	private static void createMutation(String mutationName, MutationType type, String serviceName, String contents, String filename) {
		scripts.MutationScript.ObjectFactory factory = new scripts.MutationScript.ObjectFactory();
		MutationDefinition md = factory.createMutationDefinition();
		
		md.setMutationName(mutationName);
		md.setServiceName(serviceName);
		md.setType(type);
		md.setContents(contents);
				
		try {				
			XMLProcessing.writeDocument(factory.createMutation(md), getXMLSerializer(filename).asContentHandler());			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
		
	public static XMLSerializer getXMLSerializer(String filename) throws IOException {
		 // configure an OutputFormat to handle CDATA
		 OutputFormat of = new OutputFormat();

		 // specify which of your elements you want to be handled as CDATA.
		 // The use of the '^' between the namespaceURI and the localname
		 // seems to be an implementation detail of the xerces code.
		 // When processing xml that doesn't use namespaces, simply omit the
		 // namespace prefix as shown in the third CDataElement below.
	     
		 String[] cdata = { 		    		
				   "http://www.example.org/MutationSchema^contents" };
		 of.setCDataElements(cdata);   //		 
		 		 
		 
	     // set any other options you'd like
	     of.setPreserveSpace(false);
	     of.setIndenting(true);

	     // create the serializer
	     XMLSerializer serializer = new XMLSerializer(of);
	     serializer.setOutputByteStream(new FileOutputStream(filename));	     	     

	     return serializer;
	}
}

/*
 * For debugging
 
class MyOutputFormat extends OutputFormat {
	
	public boolean isCDataElement(String arg0) {
		System.out.print(arg0 + ":" + super.isCDataElement(arg0));
		
		return super.isCDataElement(arg0);
	}
}
*/