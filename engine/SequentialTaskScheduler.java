package engine;

import java.util.ArrayList;

import engine.Queue.AbstractQueue;
import engine.Queue.Publication;
import engine.TaskAdapter.TaskAdapter;

import Configuration.Config;
import Service.AbstractService;
import Service.Task;

/*
 *  This scheduler schedules tasks in a sequential way.
 */
public class SequentialTaskScheduler extends TaskScheduler implements Runnable{
	
	//this is the waiting list for the tasks to be executed
	private ArrayList<Task> task_list;
	
	//flag to control the scheduler
	private boolean terminated = false;
	private boolean servicecomplete = false;
	
	public SequentialTaskScheduler(AbstractQueue m_queue,
			AbstractService m_service) {
		super(m_queue, m_service);
		
		task_list = new ArrayList<Task>();
	}

	@Override
	public void run() {
		boolean noWorkload = false;
		while(!terminated && (!servicecomplete || task_list.size()>0)) {
			
			if(task_list.isEmpty() || noWorkload) {
				try{
					//No waiting task, sleep 1 second
				    Thread.sleep(1000);
				    noWorkload = false;	
				} catch(Exception e) {}								
			
			} else {											
				
				noWorkload = true;
				Task task = getWaitingTask();//handle the first task
                TaskAdapter adapter = getTaskAdapter(task);
                                
                try {
                	boolean success = adapter.execute(m_service, task);
                    if(success) {
                       	//publish completion info of the task
                       	publishCompleteInfo(task.getName());
                       	noWorkload = false;
                       	
                       	if(Config.getConfig().debugModel)
        					System.out.println("Service:"+m_service.getName()+ " finishes executing task:"+task.getName());

                    } else {
                       	//put the task back into the waiting list
                        addTask2WaitingList(task);	
                    }	
                    
                } catch(InvalidExecutionException e) {
                	publishException(e);
                }
                                    
			}
		}
		
		quitTaskScheduler();		
	}
	
	//force the task executor to stop and quit immediately
	private void setTerminate(boolean terminated) {
		this.terminated = terminated;
	}	
	
	private synchronized Task getWaitingTask() {
		return task_list.remove(0);
	}
	
	private synchronized void addTask2WaitingList(Task task) {
		task_list.add(task);
	}

	@Override
	protected void scheduleTask(Task task) {
		//add the task to waiting list
		addTask2WaitingList(task);			
	}

	//notify the completion of service
	private void setServicecomplete(boolean servicecomplete) {
		this.servicecomplete = servicecomplete;
	}

	@Override
	protected void handleServiceCompleteEvent(Publication pub) {
		setServicecomplete(true);					
	}

	@Override
	protected void handleStopServiceExecutionEvent(Publication pub) {		
		setTerminate(true);
		
		//System.out.println("Stop TaskScheduler for " + m_service.getName());
	}	
}
