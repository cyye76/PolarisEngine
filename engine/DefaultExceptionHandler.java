package engine;

import java.util.ArrayList;

import engine.Event.ExceptionEvent;
import engine.Queue.Message;
import engine.Queue.AbstractListener;
import engine.Queue.Publication;

public class DefaultExceptionHandler extends AbstractListener {
	
	private ArrayList<String> exceptions;
	
	public DefaultExceptionHandler() {
		exceptions = new ArrayList<String>(); 		
	}
	
	public void registerException(String except) {
		exceptions.add(except);
		
	}
	
	@Override
	public void onNotification(Publication pub) {
		
		Message msg = pub.getMessage(ExceptionEvent.fieldname_exceptionName);
		if(msg!=null) {
		   System.out.println("ExceptionType:" + msg.getValue());	           
		}
		
		msg = pub.getMessage(ExceptionEvent.fieldname_exceptionContext);
		if(msg!=null) {
			System.out.println("ExceptionContext:" + msg.getValue());
		}
	}

}
