package engine.TaskAdapter;

import java.util.ArrayList;

import engine.InvalidExecutionException;
import engine.engine;
import engine.Communication.AbstractPort;
import engine.Queue.Message;
import engine.expression.PortExpression.PortExpression;
import engine.expression.PortExpression.PortVariable;
import Service.AbstractService;
import Service.Task;
import Utils.SyntaxAnalyzer;

public class PortTaskAdapter extends TaskAdapter {

	@Override
	public boolean execute(AbstractService service, Task task) throws InvalidExecutionException {
			
		//String effect = task.getEffect();		
		//String operator = SyntaxAnalyzer.getPortOperator(effect);
		//ArrayList<String> port_variables = SyntaxAnalyzer.getPortVariables(effect);
		
		PortExpression portexp = getPortExpression(task);
		String operator = portexp.getPortOperator();
		ArrayList<PortVariable> port_variables = portexp.getPortVariables();		
					
		if("send".equals(operator)) { 
			AbstractPort port = engine.getEngine().getPortInterface();
			ArrayList<Message> msg_list = service.constructMessage(port_variables);
			port.sendMessage(service, msg_list);
			return true;
		}
		 
		if("receive".equals(operator)) {
			return service.readIncomingMessage(port_variables);			
		}						

		throw new InvalidExecutionException();
	}

	private PortExpression getPortExpression(Task task) {
		
		if(!task.isParsing())
			task.parsingEffects();
		
		return task.getPortexp();
	}

}
