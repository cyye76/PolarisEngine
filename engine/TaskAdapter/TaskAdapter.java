package engine.TaskAdapter;

import engine.InvalidExecutionException;
import Service.AbstractService;
import Service.Task;

abstract public class TaskAdapter {

	/*
	 *  return true: the task is complete successfully
	 *  return false: the task is not complete
	 *  throw exception: an error occurred during the execution of the task
	 */
	
	abstract public boolean execute(AbstractService service, Task task) 
	    throws InvalidExecutionException;
}
