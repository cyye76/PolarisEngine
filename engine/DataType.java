package engine;

public class DataType {

	public static final String BOOLEAN = "BOOLEAN";
	public static final String INTEGER = "INTEGER";
	public static final String STRING = "STRING";
	public static final String XML = "XML";
	
	public static boolean sameValue(Object value1, Object value2) {
		if((value1 instanceof Boolean) && (value2 instanceof Boolean)) {
			Boolean bv1 = (Boolean)value1;
			Boolean bv2 = (Boolean)value2;
			return bv1.equals(bv2);
		}
		
		if((value1 instanceof Integer) && (value2 instanceof Integer)) {
			Integer iv1 = (Integer)value1;
			Integer iv2 = (Integer)value2;
			return iv1.equals(iv2);
		}
		
		if((value1 instanceof String) && (value2 instanceof String)) {
			String sv1 = (String)value1;
			String sv2 = (String)value2;
			return sv1.equals(sv2);			
		}
		
		return false;
	}

}
