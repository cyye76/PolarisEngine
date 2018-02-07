package ServiceTesting.TestCase;

import Service.AbstractService;

abstract public class ServiceTestCase {
	
	final static public int RANDOM = 1;
	final static public int CONSTRAINT = 2;
	final static public int HYBRID = 3;
	
	protected AbstractService m_service;
      
	public ServiceTestCase(AbstractService service) {		
		m_service = service;   
	}
	
	abstract public void initializeTestCase();
	
	public AbstractService getService() {		
		return m_service;
	}		
}
