package engine.Event;

import java.util.ArrayList;

import engine.DataType;
import engine.Queue.Message;

/*
 * This class defines the type of events. 
 *   - control: used by engine only (to schedule the tasks)
 *   - application: used by applications
 */

public class EventType {
	public static final String scheduleService_type = "ScheduleService";
	public static final String scheduelTask_type = "ScheduleTask";
	public static final String dataread_type = "DataReadEvent";
	public static final String datawrite_type = "DataModification";
	public static final String taskstart_type = "TaskStart";
	public static final String taskcomplete_type = "TaskComplete";
	public static final String taskfailure_type = "TaskFailure";
	public static final String exception_type = "Exception";
	public static final String servicecomplete_type = "ServiceComplete";
	public static final String stopServiceExecution_type = "StopServiceExecution";	
	public static final String transitionfiring_type = "TransitionFiring";
	public static final String transitionnotfiring_type = "TransitionNotFiring";
	public static final String scopefailure_type = "ScopeFailure";
	
	//the following fields are reserved for event head. Other data fields are 
	//not allowed to use these names	
	public static final String STRING = DataType.STRING;
	public static final String INTEGER = DataType.INTEGER;
	
	public static Event initializeEvent(ArrayList<Message> mlist) {
		String eventType = null;
		for(Message msg: mlist) {
		    if(Event.fieldname_headName.equals(msg.getName())) {
		    	eventType = (String)msg.getValue();
		    	break;
		    }
		}
		
		if(eventType!=null) {
		     if(eventType.equals(scheduleService_type)) return initializeScheduleServiceEvent(mlist);
		     if(eventType.equals(scheduelTask_type)) return initializeScheduleTaskEvent(mlist);
		     if(eventType.equals(dataread_type)) return initializeDataReadEvent(mlist);
		     if(eventType.equals(datawrite_type)) return initializeDataModificationEvent(mlist);
		     if(eventType.equals(taskstart_type)) return initializeTaskStartEvent(mlist);
		     if(eventType.equals(taskcomplete_type)) return initializeTaskCompleteEvent(mlist);
		     if(eventType.equals(exception_type)) return initializeExceptionEventType(mlist);
		     if(eventType.equals(servicecomplete_type)) return initializeServiceCompleteEvent(mlist);
		     if(eventType.equals(stopServiceExecution_type)) return initializeStopServiceExecutionEvent(mlist);
		     if(eventType.equals(transitionfiring_type)) return initializeTransitionFiringEvent(mlist);
		     if(eventType.equals(transitionnotfiring_type)) return initializeTransitionNotFiringEvent(mlist);
		     if(eventType.equals(taskfailure_type)) return initializeTaskFailureEvent(mlist);
		     if(eventType.equals(scopefailure_type)) return initializeScopeFailureEvent(mlist);
		}
		
		return null;
	}

	private static Event initializeScopeFailureEvent(ArrayList<Message> mlist) {
		String scopeName = "";
		String serviceInstanceID = "";
		String serviceName = "";
		String reason = "";
		int lineno = -1;
		
		for(Message msg:mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(Event.fieldname_serviceName.equals(name))
				serviceName = (String) msg.getValue();
			
			if(ScopeFailureEvent.fieldname_ScopeName.equals(name))
				scopeName = (String) msg.getValue();
			
			if(ScopeFailureEvent.fieldname_ScopefaultReason.equals(name))
				reason = (String) msg.getValue();
			
			if(ScopeFailureEvent.fieldname_ScopefaultLineNo.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new ScopeFailureEvent(serviceName, serviceInstanceID, scopeName, reason, lineno);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeTaskFailureEvent(ArrayList<Message> mlist) {
		String taskName = "";
		String serviceInstanceID = "";
		String serviceName = "";
		String reason = "";
		int lineno = -1;
		
		for(Message msg:mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(Event.fieldname_serviceName.equals(name))
				serviceName = (String) msg.getValue();
			
			if(TaskFailureEvent.fieldname_taskName.equals(name))
				taskName = (String) msg.getValue();
			
			if(TaskFailureEvent.fieldname_reason.equals(name))
				reason = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new TaskFailureEvent(serviceName, serviceInstanceID, taskName, reason);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeTransitionNotFiringEvent(
			ArrayList<Message> mlist) {
		
		String instanceID= "";
		String serviceName="";
		String transitionName="";
		int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name)) 
				instanceID = (String) msg.getValue();
			
			if(Event.fieldname_serviceName.equals(name)) 
				serviceName = (String) msg.getValue();
			
			if(TransitionNotFiringEvent.fieldname_transitionName.equals(name))
				transitionName = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new TransitionNotFiringEvent(instanceID, serviceName, transitionName);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeTransitionFiringEvent(
			ArrayList<Message> mlist) {
		
		String instanceID= "";
		String serviceName="";
		String transitionName="";
		int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name)) 
				instanceID = (String) msg.getValue();
			
			if(Event.fieldname_serviceName.equals(name)) 
				serviceName = (String) msg.getValue();
			
			if(TransitionFiringEvent.fieldname_transitionName.equals(name))
				transitionName = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new TransitionFiringEvent(instanceID, serviceName, transitionName);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeStopServiceExecutionEvent(
			ArrayList<Message> mlist) {
		String instanceID = "";
		int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				instanceID = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new StopServiceExecutionEvent(instanceID);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeServiceCompleteEvent(ArrayList<Message> mlist) {
		
        String instanceID = "";
        int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				instanceID = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new ServiceCompleteEvent(instanceID);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeExceptionEventType(ArrayList<Message> mlist) {
		String exceptionName = "";
		String exceptionContext = "";
		String serviceInstanceID = "";
		int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(ExceptionEvent.fieldname_exceptionName.equals(name))
				exceptionName = (String) msg.getValue();
			
			if(ExceptionEvent.fieldname_exceptionContext.equals(name))
				exceptionContext = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new ExceptionEvent(exceptionName, exceptionContext, serviceInstanceID);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeTaskCompleteEvent(ArrayList<Message> mlist) {
		String taskName = "";
		String serviceInstanceID = "";
		String serviceName = "";
		int lineno = -1;
		
		for(Message msg:mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(Event.fieldname_serviceName.equals(name))
				serviceName = (String) msg.getValue();
			
			if(TaskStatusEvent.fieldname_taskName.equals(name))
				taskName = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new TaskCompleteEvent(taskName, serviceInstanceID, serviceName);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeTaskStartEvent(ArrayList<Message> mlist) {
		String taskName = "";
		String serviceInstanceID = "";
		String serviceName = "";
		int lineno = -1;
		
		for(Message msg:mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(Event.fieldname_serviceName.equals(name))
				serviceName = (String) msg.getValue();
			
			if(TaskStatusEvent.fieldname_taskName.equals(name))
				taskName = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new TaskStartEvent(taskName, serviceInstanceID, serviceName);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeDataModificationEvent(
			ArrayList<Message> mlist) {
		String vName = "";
		String vType = "";
		Object old_value = null;
		Object new_value = null;
		String instanceID = "";
		String vLoc = "";
		String serviceName = "";
		int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceName.equals(name))
				serviceName = (String) msg.getValue();
			
			if(Event.fieldname_serviceInstanceID.equals(name))
				instanceID = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableName.equals(name))
				vName = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableType.equals(name))
				vType = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableLOC.equals(name))
				vLoc = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableOldValue.equals(name))
				old_value = msg.getValue();
			
			if(VariableEvent.fieldname_variableNewValue.equals(name))
				new_value = msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event =  new DataModificationEvent(vName, vType, old_value, new_value, instanceID, vLoc, serviceName);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeDataReadEvent(ArrayList<Message> mlist) {
		String vName = "";
		String vType = "";
		Object value = null;
		String serviceInstanceID = "";
		String vLoc = "";
		String serviceName = "";
		int lineno = -1;
		
		for(Message msg: mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceName.equals(name))
				serviceName = (String) msg.getValue();
			
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableName.equals(name))
				vName = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableType.equals(name))
				vType = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableLOC.equals(name))
				vLoc = (String) msg.getValue();
			
			if(VariableEvent.fieldname_variableReadValue.equals(name))
				value = msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event = new DataReadEvent(vName, vType, value, serviceInstanceID, vLoc, serviceName);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeScheduleTaskEvent(ArrayList<Message> mlist) {
		String taskName = "";
		String serviceInstanceID = "";
		int lineno = -1;
	
		for(Message msg:mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(TaskStatusEvent.fieldname_taskName.equals(name))
				taskName = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event = new TaskScheduleEvent(taskName, serviceInstanceID);
		event.setLineNumber(lineno);
		return event;
	}

	private static Event initializeScheduleServiceEvent(ArrayList<Message> mlist) {
		String taskName = "";
		String serviceInstanceID = "";
		int lineno = -1;
		
		for(Message msg:mlist) {
			String name = msg.getName();
			if(Event.fieldname_serviceInstanceID.equals(name))
				serviceInstanceID = (String) msg.getValue();
			
			if(TaskStatusEvent.fieldname_taskName.equals(name))
				taskName = (String) msg.getValue();
			
			if(Event.fieldname_lineNumber.equals(name))
				lineno = (Integer)msg.getValue();
		}
		
		Event event = new ServiceScheduleEvent(taskName, serviceInstanceID);
		event.setLineNumber(lineno);
		return event;
	}
	
}
