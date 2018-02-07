package engine.expression.ServiceInvocation;

import java.util.ArrayList;

public class ServiceInvocation {

	private String invokedServiceName;
	private ArrayList<Parameter> inputParameters;
	private ArrayList<Parameter> outputParameters;
	
	public ServiceInvocation(String invokedServiceName, ArrayList<Parameter> inputParameters, ArrayList<Parameter> outputParameters) {
		this.setInvokedServiceName(invokedServiceName);
		this.setInputParameters(inputParameters);
		this.setOutputParameters(outputParameters);
	}

	public String getInvokedServiceName() {
		return invokedServiceName;
	}

	public void setInvokedServiceName(String invokedServiceName) {
		this.invokedServiceName = invokedServiceName;
	}

	public ArrayList<Parameter> getInputParameters() {
		return inputParameters;
	}

	public void setInputParameters(ArrayList<Parameter> inputParameters) {
		this.inputParameters = inputParameters;
	}

	public ArrayList<Parameter> getOutputParameters() {
		return outputParameters;
	}

	public void setOutputParameters(ArrayList<Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}		
	
}
