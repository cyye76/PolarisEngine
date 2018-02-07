package MyQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import Service.Variable;
import Utils.SyntaxAnalyzer;

import Configuration.Config;

import engine.DataField;
import engine.InvalidExpressionException;
import engine.Queue.Message;
import engine.Queue.AbstractListener;
import engine.Queue.AbstractQueue;
import engine.Queue.Publication;
import engine.Queue.Subscription;

public class SampleQueue extends AbstractQueue implements Runnable{

	private HashMap<Subscription, AbstractListener> subs=null; //the set of subscribers		
	private ArrayList<Publication> m_queue = new ArrayList<Publication>(); //for update of contents
	
	//Termination flag
	private boolean terminated = false;
	
    public SampleQueue() {
    	subs = new HashMap<Subscription, AbstractListener>();    	
    }
	
	@Override
	public synchronized void publish(Publication pub) {
		
		if(Config.getConfig().debugModel) { //for debugging
			ArrayList<Message> msglist = pub.getMessages();
			for(int i=0;i<msglist.size();i++) {
				Message msg = msglist.get(i);
				if(msg == null) {
					System.out.println("Invalid message published!");
				}
			}
			System.out.println("receiving a new publication");
			pub.printMessage();
		}
				
		m_queue.add(pub);

	}

	@Override
	public synchronized void subscribe(Subscription sub, AbstractListener listener) {
		subs.put(sub, listener);	
	}
	
	public void stopQueue() {
		this.terminated  = true;
	}

	@Override
	public void run() {

		while(!this.terminated) {
			if(m_queue.isEmpty()) {
				try{
					//No publication, sleep 1 second
				    Thread.sleep(1000);
				} catch(Exception e) {}								
			} else {
				Publication pub;
				synchronized(this) {
					pub = m_queue.remove(0);//handle the first publication
					//updateContents(pub);
					match(pub);
				}
			}
		}
	}

	private void match(Publication pub) {

		Set<Subscription> keys = subs.keySet();
		ArrayList<Subscription> sub_list = new ArrayList<Subscription>();
		sub_list.addAll(keys);
		for(Subscription n_sub: sub_list) {
						
			ArrayList<Message> namelist = n_sub.getIds();
			boolean matched = true;
		
			for(int j=0;j<namelist.size();j++) {
				Message sub_msg = namelist.get(j);
				String name = sub_msg.getName();
				String type = sub_msg.getType();
				
				Message msg = pub.getMessage(name, type);
				if(msg == null) { //the content is not already
					matched = false;
					break;
				} 
			}
			
			if(matched && matchPubSub(pub, n_sub)) {
				
				AbstractListener notifier = subs.get(n_sub);
				if(notifier!=null)
				   notifier.onNotification(pub);
				else { 
					//for debugging
					if(Config.getConfig().debugModel) {

						ArrayList<Message> id_list = n_sub.getIds();
					    for(int k=0;k<id_list.size();k++) {
					    	String name = id_list.get(k).getName();
					    	System.out.print(name + ",");
					    	Message msg = pub.getMessage(name);
					    	if(msg != null) System.out.println(msg.getValue());					    	
					    }	
					
					    System.out.println(" has no subscriber");
					}
				}
			}
		}
		
	}

	/*
	 * Check whether the conditions in the subscription are satisfied
	 */
	private boolean matchPubSub(Publication pub, Subscription sub) {
		String conditions = sub.getCondition();
		
		SyntaxAnalyzer analyzer = new SyntaxAnalyzer(conditions);
		
		ArrayList<Message> msgs = pub.getMessages();
		HashMap<String, String> vNameschema = getVariableNames(msgs);
		ArrayList<DataField> datafields = new ArrayList<DataField>();
		datafields.addAll(msgs);
		analyzer.setVariables(vNameschema);
		
		try {
		    Object result = analyzer.getCSExpression().evaluateExpression(datafields);
		    
		    if(!(result instanceof Boolean))
		    	throw new InvalidExpressionException();
		    
		    return (Boolean)result;
		
		}catch(Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private HashMap<String, String> getVariableNames(ArrayList<Message> msgs) {

		HashMap<String, String> schema = new HashMap<String, String>();
		for(Message msg: msgs) {
			String vName = msg.getName();
			String vType = msg.getType();
			schema.put(vName, vType);
		}
		
		return schema;
	}

	@Override
	public void startQueue() {
		new Thread(this).start();		
	}

	@Override
	public synchronized void unsubscribe(AbstractListener listener) {		
		Set<Subscription> keys = subs.keySet();
		
		ArrayList<Subscription> rk = new ArrayList<Subscription>();
		for(Subscription sb : keys) {
			AbstractListener value = subs.get(sb);
			if(listener.equals(value)) rk.add(sb);
		}
		
		for(Subscription sb:rk)
			subs.remove(sb);
	}

	@Override
	public synchronized void unsubscribe(ArrayList<Subscription> sublist) {		
		for(Subscription sb:sublist)
			subs.remove(sb);
	}

	@Override
	public void reset() {
		subs.clear();
		m_queue.clear();
	}
}
