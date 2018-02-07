package engine.expression.PortExpression;

public class PortVariable {
   private String name;
   private String vLoc;
   
   public PortVariable(String name, String vLoc) {
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
