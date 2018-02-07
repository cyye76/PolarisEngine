package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;

import Jama.Matrix;
import Utils.XMLProcessing;

import engine.DataField;
import engine.DataType;

public class EventVector {

	public double[] fields = null;
	
	static public double dotValue(EventVector v1, EventVector v2) {
		if(v1.fields==null||v2.fields==null) return 1;
		if(v1.fields.length!=v2.fields.length) return 1;
		
		double result = 0;
		double vleng1=0, vleng2=0;
		for(int i=0;i<v1.fields.length;i++) {
			vleng1 += v1.fields[i] * v1.fields[i];
			vleng2 += v2.fields[i] * v2.fields[i];
			result += v1.fields[i] * v2.fields[i];
		}
		
		result = result/Math.sqrt(vleng1 * vleng2);
		
		return result;
	}
	
	static public EventVector constructEventVector(ProbeEvent event1, ProbeEvent event2) {
		ArrayList<DataField> fields1 = event1.getDataField();
		ArrayList<DataField> fields2 = event2.getDataField();
		
		if(fields1==null || fields2==null) return null;
		//if(fields1.size()!=fields2.size()) return null;
		int num = fields2.size();
		EventVector vector = new EventVector();
		vector.fields = new double[num];
		
		for(int i=0;i<num;i++) {			
			DataField fd2 = fields2.get(i);
			String name = fd2.getName();
			String type = fd2.getType();
			//DataField fd1 = fields1.get(i);
			DataField fd1 = getDataFieldByNameType(fields1, name, type);
			vector.fields[i] = calculateDistance(fd1, fd2);
		}
		
		return vector;
	}

	private static double calculateDistance(DataField fd1, DataField fd2) {

		double b1=0, b2=0;
		Object value1 = fd1.getValue();
		Object value2 = fd2.getValue();
		String type1 = fd1.getType();
		
		if(!compareValue(value1, value2)) {
			
			if(type1.equals(DataType.BOOLEAN) || type1.equals(DataType.STRING)) return 1;			
				
			if(value1==null || value2==null) return 1;
						
			if(value1 instanceof Integer) 
				b1 = (Integer)value1;
			
			if(value1 instanceof Double)
				b1 = (Double)value1;
			
			if(value2 instanceof Integer) 
				b2 = (Integer)value2;
			
			if(value2 instanceof Double)
				b2 = (Double)value2;
			
			if((value1 instanceof String) && (value2 instanceof String)) {
				String v1 = (String) value1;
				String v2 = (String) value2;
				if(type1.equals(DataType.XML)) 
					return Math.abs( 1 - XMLProcessing.calculateSimilarity(v1, v2));
				else
					return  v1.equals(v2)?0:1;	
			}

			return b1 - b2 ;
		}

		return 0;
	}
	
	private static boolean compareValue(Object value1, Object value2) {

		if((value1==null) && (value2==null)) return true;
		
		if((value1 instanceof Integer) && (value2 instanceof Integer)) {
			int v1 = (Integer)value1;
		    int v2 = (Integer)value2;
			return v1 == v2; 
		
		} else 
		
		if((value1 instanceof String) && (value2 instanceof String)) {
			String v1 = (String) value1;
			String v2 = (String) value2;
			return v1.equals(v2);		
		
		} else 
			
		if((value1 instanceof Boolean) && (value2 instanceof Boolean))	{
		    Boolean v1 = (Boolean) value1;
		    Boolean v2 = (Boolean) value2;
		    return v1.equals(v2); 
		}
		
		return false;
		
	}	
	
	public static ArrayList<EventVector> constructEventVectorList(ArrayList<ProbeEvent> eventlist, ProbeEvent center) {
		ArrayList<EventVector> result = new ArrayList<EventVector>();
		for(ProbeEvent pe: eventlist) {
		     EventVector ev = constructEventVector(pe, center);
		     if(ev!=null) result.add(ev);
		}
		
		return result;
	}
	
	public static double[][] constructCovarianceMatrix(ArrayList<ProbeEvent> eventlist, ProbeEvent center, ArrayList<Integer> zlist) {
		ArrayList<EventVector> vlist = constructEventVectorList(eventlist, center);
		if(vlist.isEmpty()) return null;

		//remove the variables always equal to 0
		//checkZeroVariableList(vlist, zlist);
		//if(!zlist.isEmpty())
		//   for(EventVector ev: vlist) 
		//	  reformatVector(ev, zlist);
		
		
		int num = vlist.get(0).fields.length;
		
		double[][] result = new double[num][num];
		
		for(int i=0;i<num;i++)
			for(int j=0;j<num;j++) 
				result[i][j] = calculateCov(i,j,vlist);
		
		return result;
	}

	private static void reformatVector(EventVector ev, ArrayList<Integer> zlist) {
		if(!zlist.isEmpty()) {		
			int num = zlist.size();
			double[] nf = new double[ev.fields.length-num];
			int index = 0;
			for(int i=0;i<ev.fields.length;i++) 
				if(!zlist.contains(i)) {
					nf[index] = ev.fields[i];
					index++;
				}		
		
			ev.fields = nf;
		}
	}

	private static void checkZeroVariableList(
			ArrayList<EventVector> vlist, ArrayList<Integer> zlist) {

		for(int i=0;i<vlist.get(0).fields.length;i++) {
			boolean allzero = true;
			for(EventVector ev: vlist) {				
				if(Math.abs(ev.fields[i])>=0.000001) {
					allzero = false;
					continue;
				}
			}
			
			if(allzero) zlist.add(i);
		}		
	}

	private static double calculateCov(int i, int j,
			ArrayList<EventVector> vlist) {
		
		double value = 0;
		for(EventVector ev: vlist) {
			value += ev.fields[i] * ev.fields[j];
		}
		
		value = value/vlist.size();
		
		return value;
	}	
	
	public static double calculateMHDistance(Matrix m2, ProbeEvent center, ProbeEvent evt, ArrayList<Integer> zlist) {
		//double[][] cm = constructCovarianceMatrix(eventlist, center);
		
		EventVector ev = constructEventVector(evt, center);
		//reformatVector(ev, zlist);
		
		double[][] evmt = new double[1][];
		evmt[0] = ev.fields;
		
		Matrix m1 = new Matrix(evmt);
		//Matrix m2 = new Matrix(cm);
		Matrix m3 = m1.transpose();
		
		Matrix result = m1.times(m2).times(m3);
		return Math.sqrt(result.get(0, 0));		
	}
	
	public static ProbeEvent calculateCenter(ArrayList<ProbeEvent> eventlist) {
		if(eventlist==null||eventlist.isEmpty()) return null;
		
		if(eventlist.size()<=1) return eventlist.get(0);  
		
		ProbeEvent evt = eventlist.get(0);				
		ProbeEvent pe = new ProbeEvent();
		pe.eventID = evt.eventID;
		pe.eventToken = evt.eventToken;
		pe.serviceName = evt.serviceName;
		pe.fields = new ArrayList<DataField>();
		
		ArrayList<String> unifieldlist = new ArrayList<String>();
		ArrayList<String> types = new ArrayList<String>();
		getUniqueFieldList(eventlist, unifieldlist, types);		
		
		int num = unifieldlist.size();
		for(int i=0;i<num;i++) {
			
			DataField df = new DataField();
			String name = unifieldlist.get(i);
			String type = types.get(i); 
			df.setName(name);
			df.setType(type);			
			df.setValue(getAverageValue(eventlist, name, type));
			pe.fields.add(df);
		}
				
		return pe;
	}
	
	private static void getUniqueFieldList(
			ArrayList<ProbeEvent> eventlist, ArrayList<String> unifieldlist, ArrayList<String> types) {         
         
		 for(ProbeEvent pe: eventlist) {
			 if(pe.fields!=null)
        	    for(DataField df: pe.fields) {
        	    	String name = df.getName();
        	    	String type = df.getType();
        	    	if(!unifieldlist.contains(name)) {
        	    		unifieldlist.add(name);
        	    		types.add(type);
        	    	}
        	    }
         }
		 		 
	}

	private static Object getAverageValue(ArrayList<ProbeEvent> eventlist, String name, String type) {
		
		ArrayList<DataField> buffer = new ArrayList<DataField>();
		for(ProbeEvent pe: eventlist)                    
			buffer.add(getDataFieldByNameType(pe.fields, name, type));		
		
		return getAverageValue(buffer);
	}

	private static DataField getDataFieldByNameType(
			ArrayList<DataField> flist, String name, String type) {

		DataField df = new DataField();
		df.setName(name);
		df.setType(type);
		Object value = null;
		
		if(flist!=null)
			for(DataField field: flist) {
				String fdn = field.getName();
				String fdt = field.getType();
				if(fdn.equals(name) && fdt.equals(type)) {
					value = field.getValue();
					break;
				}
			}
		
		df.setValue(value);
		return df;
	}

	private static Object getAverageValue(ArrayList<DataField> buffer) {

		if(buffer==null||buffer.isEmpty()) return null;
		
		DataField df = buffer.get(0);
		String type = df.getType();
		if(type.equals(DataType.INTEGER)) 
			return getNumericAverageValue(buffer);		
		
		return getNonNumericAverageValue(buffer);
	}

	private static Object getNonNumericAverageValue(ArrayList<DataField> buffer) {

		Object average = null;
		double min = 1000000;
		
		for(int i=0;i<buffer.size();i++) {
			
			DataField cd = buffer.get(i);			
			double distance = 0;
			
			for(int j=0;j<buffer.size();j++) {
				DataField nf = buffer.get(j);
				//if(!ProbeEvent.compareValue(cd.getValue(), nf.getValue())) 
				//	distance +=1;
				//revised on 2016.09.04 to calculate the distance between xml data
				distance+= ProbeEvent.calculateValueDistance(cd.getValue(), nf.getValue(), cd.getType());
			}
			
			if(distance < min) {
				min = distance;
				average = cd.getValue();
			}
		}
		
		return average;
	}

	private static double getNumericAverageValue(ArrayList<DataField> buffer) {
		
		int value = 0;
		int num = 0;
		for(DataField df:buffer) {
		   Object dv = df.getValue();
		   if(dv!=null) {
		       value+= (Integer)dv;
		       num++;
		   }
		}
		
		return value*1.0 / num;
	}
	
	public static double calculateSD(ArrayList<ProbeEvent> eventlist, ProbeEvent center, Matrix m2, ArrayList<Integer> zlist) {
		double[] buffer = new double[eventlist.size()];
		double average  = 0;
		for(int i=0;i<buffer.length;i++) {
			ProbeEvent pe = eventlist.get(i);
			double dist = calculateMHDistance(m2, center, pe, zlist);
			buffer[i] = dist;
			average += dist;
		}
		
		average = average/buffer.length;
        double sd = 0;
		for(int i=0;i<buffer.length;i++) {
			sd += (buffer[i] - average) * (buffer[i] - average);
		}
		
		return Math.sqrt(sd/buffer.length);
	}

	public static void main(String args[]) {
		double[][] m1 = {{1, 2}};
		double[][] m2 = {{3},{4}};
		
		Matrix mt1 = new Matrix(m1);
		Matrix mt2 = new Matrix(m2);
		
		Matrix result = mt1.times(mt2);
		System.out.println(result.get(0,0));
		
	}
}
