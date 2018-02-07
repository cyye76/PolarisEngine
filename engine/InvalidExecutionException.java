package engine;

public class InvalidExecutionException extends Exception {

	public static String getCurrentContext() {
		String context = "";
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for (int i=2 ; i<stackTraceElements.length; i++) {
			StackTraceElement ste = stackTraceElements[i];
			String classname = ste.getClassName();
			String methodName = ste.getMethodName();
			int lineNumber = ste.getLineNumber();
		
			context += classname+"."+methodName+":"+lineNumber;			
		}	
		
		return context;
	}
}
