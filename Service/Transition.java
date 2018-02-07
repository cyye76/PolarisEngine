package Service;

public class Transition {

	private String name;
	private String source;
	private String sink;
	private Guard guard;	
	private String instanceID;
	
	public Transition() {
		//setInstanceID("" + System.currentTimeMillis());
		setInstanceID("");
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSink(String sink) {
		this.sink = sink;
	}
	
	public String getSink() {
		return sink;
	}

	public void setGuard(Guard guard) {
		this.guard = guard;
	}

	public Guard getGuard() {
		return guard;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	private void setInstanceID(String instanceID) {
		this.instanceID = instanceID;
	}

	public String getInstanceID() {
		return instanceID;
	}
	
	
}
