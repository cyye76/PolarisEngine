package engine.Communication;

import java.util.ArrayList;

import Service.AbstractService;

import engine.Queue.Message;



public interface AbstractPort {
	
	public void sendMessage(AbstractService service, ArrayList<Message> msgs);
}
