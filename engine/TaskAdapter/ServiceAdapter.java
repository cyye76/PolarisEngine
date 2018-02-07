package engine.TaskAdapter;

import java.util.ArrayList;

import Service.AbstractService;
import Service.State;
import Service.Task;
import Service.Variable;
import engine.InvalidExecutionException;
import engine.expression.ServiceInvocation.Parameter;
import engine.expression.ServiceInvocation.ServiceInvocation;

public class ServiceAdapter extends TaskAdapter {

	@Override
	public boolean execute(AbstractService service, Task task)
			throws InvalidExecutionException {
		
		ServiceInvocation invocation = getInvocation(task);
		String invokedServiceName = invocation.getInvokedServiceName();
		
		ArrayList<Variable> inputparameters = getVariables(invocation.getInputParameters(), service);
		ArrayList<Variable> outputparameters = getVariables(invocation.getOutputParameters(), service);
		
		invokeService(invokedServiceName, inputparameters, outputparameters);
		
		return true;
	}

	private void invokeService(String invokedServiceName,
			ArrayList<Variable> inputparameters,
			ArrayList<Variable> outputparameters) throws InvalidExecutionException {
		
		if("MAX".equals(invokedServiceName)) invokeMaxService(inputparameters, outputparameters);
		
	}

	private void invokeMaxService(ArrayList<Variable> inputparameters,
			ArrayList<Variable> outputparameters) throws InvalidExecutionException {
		
		int maxID = -1;
		int maxValue = -1;
		
		for(int i=0;i<inputparameters.size()/2;i++) {
			int ID = (Integer)inputparameters.get(i*2).getValue();
			int value = (Integer)inputparameters.get(i*2+1).getValue();
			
			if(value > maxValue) {
				maxValue = value;
				maxID = ID;
			}
		}
		
		if(outputparameters.size()<2) throw new InvalidExecutionException();
		
		Variable v1 = outputparameters.get(0);
		Variable v2 = outputparameters.get(1);
		
		v1.setValue(maxID);
		v2.setValue(maxValue);
	}

	private ArrayList<Variable> getVariables(
			ArrayList<Parameter> parameters, AbstractService service) {
		
		State state = service.getState();		
		
		ArrayList<Variable> vlist = new ArrayList<Variable>();
		for(Parameter pv: parameters) {
			String vn = pv.getName();
			vlist.add(state.getVariablebyName(vn));
		}
		
		return vlist;
	}

	private ServiceInvocation getInvocation(Task task) {
		if(!task.isParsing())
			task.parsingEffects();
		
		return task.getInvocation();
	}
	
	

}
