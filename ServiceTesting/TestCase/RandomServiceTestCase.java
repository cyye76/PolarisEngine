package ServiceTesting.TestCase;

import java.util.ArrayList;
import java.util.Random;

import engine.DataType;

import Configuration.Config;
import Service.AbstractService;
import Service.State;
import Service.Variable;

public class RandomServiceTestCase extends ServiceTestCase {

	public RandomServiceTestCase(AbstractService service) {
		super(service);		
	}

	@Override
	public void initializeTestCase() {
        State cs = m_service.getState();
        assert(cs!=null);

        Random rd1 = new Random(System.currentTimeMillis());
        Random rd2 = new Random(System.currentTimeMillis());
        Random rd3 = new Random(System.currentTimeMillis());
        
        ArrayList<Variable> variables = cs.getVariables();
        for(Variable v: variables) {
        	String type = v.getType();
        	
        	if(type.equals(DataType.BOOLEAN)) {//boolean
        		v.setValue(rd1.nextBoolean());
     
        	} else
        		
        	if(type.equals(DataType.INTEGER)) { //Integer
        		int initValue = (Integer)v.getValue();
        		if(initValue == -1) //that means not initialized yet
        		   v.setValue(rd2.nextInt(Config.getConfig().variableDomain));
        	} else
        		v.setValue(""+rd3.nextLong()); //String
        }
        
	}	

}
