package ServiceTesting.ConstraintSolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import engine.DataField;
import Utils.WordParser;

public class MyConstraint implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7780539094068957717L;
	public String condition;
	public String serviceName;
	
	public CSLVariable rstV = null;//used to store the variable that is assigned a new value in the expression
	
	public boolean isSend() {
		return condition.startsWith("send ");
	}
	
	public boolean isReceive() {
		return condition.startsWith("receive ");
	}
	
	public boolean isAssignment() {
		WordParser parser = new WordParser(condition);
		String[] consts = parser.getExpression();
		if(consts.length >=2) return ":=".equals(consts[1]);
		return false;
	}
	
	public boolean isBooleanExpression() {
		return !(isSend() || isReceive() || isAssignment());
	}
	
	public Object m_expression = null;
	
	public ArrayList<String> getExpressionVariables(HashMap<String, String> schema, boolean isAssignment) {
		ArrayList<String> result = new ArrayList<String>();
		WordParser parser = new WordParser(condition);		
		String[] consts = parser.getExpression();
		int startindex = isAssignment? 2:0;
		for(int i=startindex;i<consts.length;i++) {
			String  cst = consts[i];
			String type = schema.get(cst);
			if(type!=null) result.add(cst);
		}
		
		return result;
	}
	
	public ArrayList<CSLVariable> searchVariables = null;
	public ArrayList<DataField> datafields = null;
	public ArrayList<CSLVariable> sendVariables = null;
	public ArrayList<CSLVariable> recvFree = null;
	public ArrayList<CSLVariable> recvRestore = null;
	public ArrayList<CSLVariable> recvVariables = null;
	public ArrayList<CSLVariable> bindingVariables = null;

	public MyConstraint backup() {
		MyConstraint mc = new MyConstraint();
		mc.condition = condition;
		mc.serviceName = serviceName;
		return mc;
	}

	public ArrayList<String> getPortVariables() {
		
		ArrayList<String> result = new ArrayList<String>();
		String[] cnts = condition.split("\\s+");
		if(cnts!=null) {
			for(int i=1;i<cnts.length;i++)
				result.add(cnts[i]);
		}
		
		return result;
	}

	public String getAssignmentVariable() {
		
		WordParser parser = new WordParser(condition);
		String[] cnts = parser.getExpression();
		if(cnts!=null) {
			return cnts[0];
		}
		
		return null;
	}
	
	public boolean isSame(MyConstraint mc) {
		return condition.equals(mc.condition) && serviceName.equals(mc.serviceName);
	}
}
