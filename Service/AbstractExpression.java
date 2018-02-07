package Service;

import java.util.ArrayList;

import engine.DataField;
import engine.InvalidExpressionException;

public abstract class AbstractExpression {
		
	/*
	 *  The grammar supported:
	 *  SE  := ASE | CSE
	 *  ASE := VSE ':=' CSE
	 *  CSE := BE 
	 *  BE  := BOE
	 *  BOE := BAE | BAE '||' BOE
	 *  BAE := BNE | BNE '&&' BAE
	 *  BNE := PE | '!' BE
	 *  PE  := AE | AE '<' AE | AE '<=' AE | AE '>' AE | AE '>=' AE | AE '==' AE | AE '!=' AE
	 *  AE  := ATDE | ATDE '+' AE | ATDE '-' AE
	 *  ATDE:= SSE | SSE '*' ATDE  | SSE '/' ATDE 
	 *  SSE := VSE | CTE | '(' CSE ')'
	 *  VSE := variable
	 *  CTE := constant 
	 */

	public void print() {
		
	}
	
	//abstract public Object evaluateExpression(ArrayList<DataField> datafields) 
	//               throws InvalidExpressionException;
	
	public Object evaluateExpression(ArrayList<DataField> datafields) 
                   throws InvalidExpressionException {
		return null;
	}
}
