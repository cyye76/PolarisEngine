package engine.Queue;

import java.io.Serializable;
import java.util.ArrayList;

public class Publication implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7658047746161583819L;
	private ArrayList<Message> messages;

	public void setMessages(ArrayList<Message> messages) {
		this.messages = messages;
	}

	public ArrayList<Message> getMessages() {
		return messages;
	} 
	
	public boolean hasMessage(String name) {
		for(int i=0;i<messages.size();i++) {
			if(name.equals(messages.get(i).getName())) {
				return true;
			}
		}
		return false;
	}
	
	public Message getMessage(String name) {
		for(int i=0;i<messages.size();i++) {
			if(name.equals(messages.get(i).getName())) {
				return messages.get(i);
			}
		}
		return null;
	}
	
	public Message getMessage(String name, String type) {
		for(Message msg:messages) {
			if(name.equals(msg.getName()) && type.equals(msg.getType())) 
				return msg;			
		}
		return null;
	}
	
	public Message getMessageStartedWith(String name) {
		for(int i=0;i<messages.size();i++) {
			if(messages.get(i).getName().startsWith(name)) {
				return messages.get(i);
			}
		}
		return null;
	}
	
	public Publication() {
		messages = new ArrayList<Message>();
	}
	
	public void addMessage(Message msg) {
		messages.add(msg);
	}

	/*
	 * For debugging
	 */
	public void printMessage() {
		for(int i=0;i<messages.size();i++) {
			Message msg = messages.get(i);
			System.out.print(msg.getName()+",");
			System.out.print(msg.getType()+",");
			System.out.print(msg.getValue());
			System.out.println();			
		}
		System.out.println("---------------------------------------------------------------");
		System.out.println();
	}
	
}
