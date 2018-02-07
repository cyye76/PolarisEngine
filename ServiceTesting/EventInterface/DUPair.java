package ServiceTesting.EventInterface;

import java.util.ArrayList;

public class DUPair {

	private String def;
	private String use;
	private String serviceName;
	
	public DUPair(String def, String use) {
		this.setDef(def);
		this.setUse(use);
	}

	public void setDef(String def) {
		this.def = def;
	}

	public String getDef() {
		return def;
	}

	public void setUse(String use) {
		this.use = use;
	}

	public String getUse() {
		return use;
	}
	
	public boolean equal(DUPair ndu) {
		return def.equals(ndu.getDef()) && use.equals(ndu.getUse());
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}
	
	public void addConditions(String conditions) {
		this.conditions.add(conditions);
	}

	public ArrayList<String> getConditions() {
		return conditions;
	}

	private ArrayList<String> conditions = new ArrayList<String>();
	
}
