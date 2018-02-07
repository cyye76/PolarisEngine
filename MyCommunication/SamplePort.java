package MyCommunication;

import java.util.ArrayList;

import engine.InvalidExecutionException;
import engine.engine;
import engine.Communication.AbstractCorrelation;
import engine.Communication.AbstractPort;
import engine.Event.ExceptionEvent;
import engine.Queue.AbstractListener;
import engine.Queue.Message;
import engine.Queue.Publication;
import Service.AbstractService;

/*
 * A sample port implementation supporting asynchronous
 * communication among services in a service composition.
 * This implementation re-uses the sampleQueue to exchange
 * messages among services.
 */
public class SamplePort extends AbstractListener implements AbstractPort {
		

	@Override
	public void onNotification(Publication pub) {		
		String serviceName = SamplePortMessage.getServiceName(pub);
		String serviceID = SamplePortMessage.getServiceID(pub);
		if((serviceName == null) || (serviceID == null)) {
			//throw an exception
			publishException(new InvalidExecutionException());
		
		} else {						
			
			ArrayList<Message> body = SamplePortMessage.getMessageBody(pub);
			AbstractCorrelation correlation = engine.getEngine().getServiceCorrelation();
			
			for(Message msg:body) {
				
				String msg_name = msg.getName();
				ArrayList<String> serviceIDs = correlation.getReceivingServices(serviceName, serviceID, msg_name);
				
				for(String id: serviceIDs) {
					AbstractService service = engine.getEngine().getService(id);
					if(service!=null)
					   service.addIncomingMessage(msg);
					else
						//throw exception
						publishException(new InvalidExecutionException());
				}
			}
						
		}
		
	}


	@Override
	public void sendMessage(AbstractService service, ArrayList<Message> msgs) {		
		String serviceName = service.getName();
		String serviceID = service.getInstanceID();
		Publication pub = new SamplePortMessage(serviceName, serviceID, msgs);
		
		engine.getEngine().getQueue().publish(pub);
	}
		
    private void publishException(Exception e) {
		
		String exceptionName = e.getClass().getName();
		String exceptionContext = e.toString();

		Publication pub = new ExceptionEvent(exceptionName, exceptionContext, engine.getEngine().getInstanceID());
		engine.getEngine().getQueue().publish(pub);
	}
}
