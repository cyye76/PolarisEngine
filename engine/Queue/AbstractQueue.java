package engine.Queue;

import java.util.ArrayList;

abstract public class AbstractQueue {

	public abstract void publish(Publication pub);
    
    public abstract void subscribe(Subscription sub, AbstractListener listener);
    
    public abstract void startQueue();
    
    public abstract void stopQueue();
    
    public abstract void unsubscribe(AbstractListener listener);

	public abstract void unsubscribe(ArrayList<Subscription> sublist);

	public abstract void reset();
}
