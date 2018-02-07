package engine;

import java.util.ArrayList;

import Configuration.Config;
import Service.AbstractExpression;
import Service.AbstractService;
import Service.Guard;
import Service.State;
import Service.Transition;
import Utils.SyntaxAnalyzer;
import engine.Event.Event;
import engine.Event.EventType;
import engine.Event.ExceptionEvent;
import engine.Event.ServiceCompleteEvent;
import engine.Event.ServiceScheduleEvent;
import engine.Event.TaskScheduleEvent;
import engine.Event.TaskStatusEvent;
import engine.Event.StopServiceExecutionEvent;
import engine.Event.TransitionFiringEvent;
import engine.Event.TransitionNotFiringEvent;
import engine.Queue.Message;
import engine.Queue.AbstractListener;
import engine.Queue.AbstractQueue;
import engine.Queue.Publication;
import engine.Queue.Subscription;
import engine.expression.BooleanExpression.BooleanExpression;
import engine.expression.BooleanExpression.InvalidBooleanExpressionException;

public class ServiceScheduler extends AbstractListener {
	
	private AbstractQueue m_queue;
	private AbstractService m_service;		
	
	public ServiceScheduler(AbstractQueue queue, AbstractService service) {
		m_queue = queue;
		m_service = service;					
		
		//set the subscription to receive message from task executor
		Subscription sub = ServiceScheduleEvent.createServiceScheduleSubscription(m_service.getInstanceID());		 
		m_queue.subscribe(sub, this);
		
		//set the subscription to receive command from engine
		sub = StopServiceExecutionEvent.createStopServiceExecutionEventSubscription(m_service.getInstanceID());
		m_queue.subscribe(sub, this);
		
		//start task scheduler		
		new Thread(new SequentialTaskScheduler(m_queue, m_service)).start();
	}

	public void start() {
		Publication pub = new TaskScheduleEvent(m_service.getStartTask(), m_service.getInstanceID());		
		
		m_queue.publish(pub);
	}

	public void onNotification(Publication pub) {
		Message msg = pub.getMessage(Event.fieldname_headName);
		if(msg != null) {
			String eventType = (String)msg.getValue();
			if(EventType.scheduleService_type.equals(eventType)) {
				handleScheduleServiceEvent(pub);
			} else
				
			if(EventType.stopServiceExecution_type.equals(eventType)) {
				handleStopServiceExecutionEvent(pub);
			} else {
				handleInvalidPublication();
			}
				
		} else {
			handleInvalidPublication();
		}
	}
	
	
	private void handleInvalidPublication() {
		String context = "No event type is found in the publication.";
		context += InvalidExecutionException.getCurrentContext();
		publishException("InvalidExecutionException", context);			
	}

	private void handleStopServiceExecutionEvent(Publication pub) {		
		quitServiceScheduler();
		
		//System.out.println("Stop ServiceScheduler for " + m_service.getName());
	}

	private void quitServiceScheduler() {
		//un-subscribe the message
		m_queue.unsubscribe(this);			
	}

	private void handleScheduleServiceEvent(Publication pub) {
		Message msg = pub.getMessage(TaskStatusEvent.fieldname_taskName);
		if(msg != null) { 								
			String taskName = (String)msg.getValue();
			if(taskName != null) {	
				//update state
				m_service.getState().updateTaskCompleteStatus(taskName);
				
				//schedule next task
				scheduleNextTask(taskName);							
			
			} else {				
				String context = "Task name in ServiceScheduelEvent is null.";
				context += InvalidExecutionException.getCurrentContext();
				publishException("InvalidExceptionException", context);
			}
		
		} else {
						
			String context = "No task name is found in ServiceScheduelEvent.";
			context += InvalidExecutionException.getCurrentContext();
			publishException("InvalidExecutionException", context);				
		}
	}
	
	private void publishException(String exceptionname, String exceptioncontext) {
		
		Publication pub = new ExceptionEvent(exceptionname, exceptioncontext, m_service.getInstanceID());

		m_queue.publish(pub);		
	}
	
	private void publishException(Exception e) {
		
		String exceptionName = e.getClass().getName();
		String exceptionContext = e.toString();

		Publication pub = new ExceptionEvent(exceptionName, exceptionContext, m_service.getInstanceID());
		m_queue.publish(pub);
	}


	private void scheduleNextTask(String taskName) {
		
		ArrayList<Transition> availableTransitions = m_service.getAvailableNextTransitions(taskName);
		
		if(availableTransitions==null || availableTransitions.isEmpty()) {			
			//issue a service complete event
			ServiceCompleteEvent event = new ServiceCompleteEvent(m_service.getInstanceID());
			m_queue.publish(event);
			
			//quite scheduler
			quitServiceScheduler();
			
		} else {
		
			for(int i=0;i<availableTransitions.size();i++) {
		
				Transition transition = availableTransitions.get(i);
				if(evaluateGurad(transition.getGuard())) {		
					scheduleTransition(transition);
					break;
				} else {
					//publish transition not firing event
					if(Config.getConfig().exposeevent) {
						String tn = transition.getName();
						String serviceName = m_service.getName();
						String serviceInstanceID = m_service.getInstanceID();						
						TransitionNotFiringEvent tfe = new TransitionNotFiringEvent(serviceInstanceID, serviceName, tn);						
						m_queue.publish(tfe);
					}
				}
			}
		}
	}


	private void scheduleTransition(Transition transition) {
		
		String taskName = transition.getSink();
		String serviceInstanceID = m_service.getInstanceID();
		
		//signal the task executor				
		Publication pub = new TaskScheduleEvent(taskName, serviceInstanceID);				
		m_queue.publish(pub);
		
		//publish transition firing event
		if(Config.getConfig().exposeevent) {
			String tn = transition.getName();
			String serviceName = m_service.getName();
			TransitionFiringEvent tfe = new TransitionFiringEvent(serviceInstanceID, serviceName, tn);						
			m_queue.publish(tfe);
		}
	}

	private boolean evaluateGurad(Guard guard) {
		
		State cs = m_service.getState();
		//String condition = guard.getGuard();				
		//SyntaxAnalyzer analyzer = new SyntaxAnalyzer(condition);
		//analyzer.setVariables(m_service.getVariableSchema());
		
		try{
		
			AbstractExpression be = getExpression(guard);
		    ArrayList<DataField> datafields = new ArrayList<DataField>();
		    datafields.addAll(cs.getVariables());
		    Object result = be.evaluateExpression(datafields);
		    if(!(result instanceof Boolean)) 
		    	throw new InvalidBooleanExpressionException();
		    
		    return (Boolean)result;
		
		} catch(Exception e) {
			publishException(e);
		}		
		
		return false;
	}

	private AbstractExpression getExpression(Guard guard) {
		if(!guard.isParsed())
			guard.parseCondition();
		
		return guard.getGuardexp();
	}

}
