package Utils;

import java.util.ArrayList;
import java.util.Collection;

public class WordParser {
    
	private String[] expression;
	private int index=0;
	private final static String[] keywords = {
		"(", ")", "&&", "&", "||", "|", ":=", "==", "<=", ">=", "!=", "<", ">", ";",
		"+", "-", "*", "/", "!", ","
	}; 

	public WordParser(String expression) {
		String t = expression.trim();
		String[] tmp = t.split("\\s+");
		this.expression = furtherExtraction(tmp);
	}
	
	private String[] furtherExtraction(String[] tmp) {
		ArrayList<String> result = new ArrayList<String>();
		
		for(String item: tmp) 
			result.addAll(splitWord(item));
					
		
		String[] exp = new String[result.size()];
		for(int i=0;i<exp.length;i++)
			exp[i] = result.get(i);
		
		return exp;
	}

	private ArrayList<String> splitWord(String item) {
		
		ArrayList<String> result = new ArrayList<String>();
		if(item==null || item.length()==0) return result;
		
		for(String kw: keywords) {		    
	    	int si = item.indexOf(kw);
	    	if(si>=0) {
	    	   String subItem1 = item.substring(0, si);
	    	   String subItem2 = item.substring(si+kw.length());
	    	   result.addAll(splitWord(subItem1));	
	    	   result.add(kw);
	    	   result.addAll(splitWord(subItem2));
	    	   return result;
	    	}		    
		}
		
		result.add(item);
		return result;
	}

	public boolean hasNext() {
		return index<expression.length;
	}
	
	public String getNextWord() {
		if(hasNext())
		   return expression[index++];
		else
		   return "";	
	}
	
	public String[] getExpression() {
		return this.expression;
	}
	
	public static boolean isKeyword(String word) {
		for(int i=0;i<keywords.length;i++)
			if(word.equals(keywords[i])) return true;
		return false;
	}
}
