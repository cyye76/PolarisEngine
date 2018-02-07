package ServiceTesting.EventInterface;

import java.util.ArrayList;

import Service.AbstractService;
import Service.Task;

public class ActivityCoverageEventInterface extends EventInterface {
	
	private ArrayList<String> activityIDs = new ArrayList<String>();

	@Override
	public void deriveEventInterface(AbstractService service) {	
		
		setServiceName(service.getName());
		
		ArrayList<Task> taskList = service.getTasks();
		for(Task task : taskList)
			activityIDs.add(task.getName());
	}
	
	public ArrayList<String> getActivityList() {
		return activityIDs;
	}

}
