package engine.expression.ServiceInvocation;

public class Parameter {

	  private String name;
	   private String vLoc;
	   
	   public Parameter(String name, String vLoc) {
		   this.name = name;
		   this.vLoc = vLoc;
	   }

	   public String getName() {
		   return name;
	   }

	   public String getvLoc() {
		   return vLoc;
	   }
}
