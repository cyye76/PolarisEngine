package engine.expression.PortExpression;

import java.util.ArrayList;

public class PortExpression {
	
	private String portOperator;	
	private ArrayList<PortVariable> portVariables;

	public PortExpression(String portOperator, ArrayList<PortVariable> portVariables) {
		this.setPortOperator(portOperator);
		this.setPortVariables(portVariables);
	}

	public void setPortOperator(String portOperator) {
		this.portOperator = portOperator;
	}

	public String getPortOperator() {
		return portOperator;
	}

	public void setPortVariables(ArrayList<PortVariable> portVariables) {
		this.portVariables = portVariables;
	}

	public ArrayList<PortVariable> getPortVariables() {
		return portVariables;
	}
	
}
