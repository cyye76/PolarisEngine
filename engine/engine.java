package engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import MyCommunication.SamplePort;
import MyCommunication.SamplePortCorrelation;
import MyCommunication.SamplePortMessage;
import MyQueue.SampleQueue;
import Service.AbstractService;
import ServiceIsolation.BCScheduler;
import engine.Communication.AbstractCorrelation;
import engine.Communication.AbstractPort;
import engine.Event.ExceptionEvent;
import engine.Event.StopServiceExecutionEvent;
import engine.Queue.AbstractQueue;
import engine.Queue.Subscription;

/*
 *  The main class of service engine
 *  This is a lightweight engine that is attached to each service after the
 *  service is compiled. 
 *  
 *  Author: Chunyang Ye
 */

public class engine {

	private AbstractQueue m_queue = null;			
	private static engine m_instance = null;
	private AbstractCorrelation m_correlation = null;
	private SamplePort m_port = null;
	
	//engine ID
	private String instanceID = null;
	
	//map to store service instances
	private HashMap<String, AbstractService> running_services;
	
	private engine() {
		generateIdentifiedInstanceID();
		running_services = new HashMap<String, AbstractService>();
		m_port = new SamplePort();
		m_correlation = new SamplePortCorrelation();
	}

	private static long counter = 1;
	private void generateIdentifiedInstanceID() {
		long now = System.currentTimeMillis();
		counter *= 2;
		Random rd = new Random(now*counter);		
		instanceID =  rd.nextLong() + "_" + now;		
	}

	public static engine getEngine() {
		if(m_instance == null) {
			m_instance = new engine();			
		}
		return m_instance;
	}
	
	public String getInstanceID() {
		return instanceID;
	}
	
	public void executeService(AbstractService service) {		
		//register service instance		
		String serviceID = service.getInstanceID();
		registerServiceInstance(serviceID, service);
		
		ServiceScheduler scheduler = new ServiceScheduler(m_queue, service);
		scheduler.start();
	}
	
	/**
	 * This method is used to execute simulation service for performance study.
	 * The engine use another service scheduler called BC scheduler to schedule the services.
	 */
	public void initScheduler(boolean isSandbox, int boxsize) {
		BCScheduler scheduler = BCScheduler.getBCScheduler(m_queue);
		scheduler.init(isSandbox, boxsize);
	}
	
	public void executeBCService(AbstractService service) {
		//register service instance
		String serviceID = service.getInstanceID();
		registerServiceInstance(serviceID, service);
		
		BCScheduler scheduler = BCScheduler.getBCScheduler(m_queue);
		scheduler.start(service);
	}
	
	public void stopBCScheduler() {
		BCScheduler scheduler = BCScheduler.getBCScheduler(m_queue);
		scheduler.stop();
	}
	
	public void startEngine() {
		m_queue = initializeQueue();
		//new Thread(m_queue).start();
				
		//register default exception handler
		//Subscription sub = ExceptionEvent.createExceptionSubscription(instanceID);
		Subscription sub = ExceptionEvent.createDefaultExceptionSubscription();
		DefaultExceptionHandler handler = new DefaultExceptionHandler();
		m_queue.subscribe(sub, handler);
		
		//register port 
		sub = SamplePortMessage.createSamplePortSubscription();
		m_queue.subscribe(sub, m_port);
		
		//start the queue
		m_queue.startQueue();
		
	}
	
	/*
	 * This function will be extended in the future
	 * to initialize a queue from the configuration of
	 * the engine. Currently, it initializes the default 
	 * queue in the engine. 
	 */
	private AbstractQueue initializeQueue() {
		return new SampleQueue();		
	}

	public void closeEngine() {
		m_queue.stopQueue();
	}

	public AbstractQueue getQueue() {
		return m_queue;
	}

	public synchronized AbstractService getService(String serviceID) {
		if(serviceID!=null) {
			AbstractService service = running_services.get(serviceID);
			return service;	
		}
		return null;
	}
	
	public synchronized void registerServiceInstance(String instanceID, AbstractService service) {
		if((instanceID!=null) && (service != null)) {						
			running_services.put(instanceID, service);
		}
	}

	public AbstractPort getPortInterface() {		
		return m_port;
	}

	public AbstractCorrelation getServiceCorrelation() {
		return m_correlation;
	}

	public synchronized void unregister(ArrayList<AbstractService> services) {		
		for(AbstractService service: services) {
			String serviceID = service.getInstanceID();
			running_services.remove(serviceID);
		}
	}
	
	public synchronized void unregisterService(String instanceID) {
		running_services.remove(instanceID);
	}

	public void terminateServices(ArrayList<AbstractService> services) {		
		for(AbstractService service : services)
			terminateOneService(service);
	}

	public void terminateOneService(AbstractService service) {		
		if(service!=null) {
			StopServiceExecutionEvent event = new StopServiceExecutionEvent(service.getInstanceID());
			m_queue.publish(event);
		}
			
	}		
	
}
