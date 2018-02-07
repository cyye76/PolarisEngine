package engine;


import Configuration.Config;
import Service.AbstractService;
import Service.Task;
import engine.Queue.Message;
import engine.Queue.AbstractListener;
import engine.Queue.AbstractQueue;
import engine.Queue.Publication;
import engine.Queue.Subscription;
import engine.TaskAdapter.LocalTaskAdapter;
import engine.TaskAdapter.PortTaskAdapter;
import engine.TaskAdapter.ServiceAdapter;
import engine.TaskAdapter.TaskAdapter;
import engine.Event.ExceptionEvent;
import engine.Event.ServiceCompleteEvent;
import engine.Event.ServiceScheduleEvent;
import engine.Event.StopServiceExecutionEvent;
import engine.Event.TaskScheduleEvent;
import engine.Event.TaskStatusEvent;
import engine.Event.Event;
import engine.Event.EventType;

abstract public class TaskScheduler extends AbstractListener{

	private AbstractQueue m_queue;
	protected AbstractService m_service;
		
	@Override
	public void onNotification(Publication pub) {
		Message msg = pub.getMessage(Event.fieldname_headName);
		if(msg!=null) {
			String type = (String)msg.getValue();
			if(EventType.scheduelTask_type.equals(type)) {
				handleTaskScheduleEvent(pub);
			} else 
				
			if(EventType.servicecomplete_type.equals(type)) {
				handleServiceCompleteEvent(pub);
			} else
				
			if(EventType.stopServiceExecution_type.equals(type)) {
				handleStopServiceExecutionEvent(pub);
			} else {				
				handleInvalidPublication();
			}
		} else {
			handleInvalidPublication();
		}
		
	}
	
	
	abstract protected void handleServiceCompleteEvent(Publication pub);


	private void handleInvalidPublication() {
		publishException(new InvalidExecutionException());
	}


	abstract protected void handleStopServiceExecutionEvent(Publication pub);			


	private void handleTaskScheduleEvent(Publication pub) {	

		Task task = getTask(pub);
		if(task == null) {
			publishException(new InvalidExecutionException());
		} else {				
			if(Config.getConfig().debugModel)
				System.out.println("Service:" + m_service.getName() + " starts to execute task:" + task.getName());

			scheduleTask(task);									
		}
	}
	
	/*
	 * This is an abstract schedule task function.
	 * It can be override to implement different scheduling 
	 * strategies
	 */
	abstract protected void scheduleTask(Task task);

	private Task getTask(Publication pub) {
		Message msg = pub.getMessage(TaskStatusEvent.fieldname_taskName);
		if(msg != null) {						
			String taskName = (String) msg.getValue();
			return m_service.getTaskbyName(taskName);			
		}	
		
		return null;
	}

	protected void publishException(InvalidExecutionException e) {

		String exceptionName = e.getClass().getName();
		String exceptionContext = e.toString();

		Publication pub = new ExceptionEvent(exceptionName, exceptionContext, m_service.getInstanceID());
		m_queue.publish(pub);
	}

	protected void publishCompleteInfo(String taskName) {

		Publication pub = new ServiceScheduleEvent(taskName, m_service.getInstanceID());				
		
		m_queue.publish(pub);
	}				
	
	public TaskScheduler(AbstractQueue m_queue, AbstractService m_service) {
		this.m_queue = m_queue;				
		this.m_service = m_service;		
		
		//set the subscription of task executor to receive message from scheduler		
		Subscription sub = TaskScheduleEvent.createTaskScheduleSubscription(m_service.getInstanceID());
		m_queue.subscribe(sub, this);
		
		//set the subscription of task executor to receive service complete event
		sub = ServiceCompleteEvent.createServiceCompleteEventSubscription(m_service.getInstanceID());
		m_queue.subscribe(sub, this);
		
		//set the subscription of task executor to receive commands from engine		
		sub = StopServiceExecutionEvent.createStopServiceExecutionEventSubscription(m_service.getInstanceID());
		m_queue.subscribe(sub, this);
	}	

	protected TaskAdapter getTaskAdapter(Task task) {
		if(task.isPort())
			return new PortTaskAdapter();
		
		if(task.isService())
			return new ServiceAdapter();
		
		return new LocalTaskAdapter();
	}

	protected void quitTaskScheduler() {
		m_queue.unsubscribe(this);
	}
		
}
