package ServiceTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.PostscriptTerminal;

public class DataHandling {
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		new DataHandling().HandleCFData();
	}
	
	public void HandleCFData() {
		String[] args = {"Applications/LoanApproval/Mutations/mutations.txt", 
				         "filter.txt", 
				         "tmp", 
				         "tmp", 
				         "1"};		

        //handleCoverageFaultDetectionRateData(args);
        
        String[][] datas = {{"tmp/1_1_CF.txt"}, 
	            {"tmp/1_2_CF.txt"},
	            {"tmp/1_3_CF.txt"},
	            {"tmp/1_4_CF.txt"}};
        
        int coveragepolicy = 1;        
        String titles[]= {"OA", "EA"}; 
        for(String[] fn: datas) {
        	String outfn = coveragepolicy+".eps";
        	plotCoverageFaultDetectionRate(fn, titles, outfn);
        	coveragepolicy++;
        }
	}
	
	private void plotCoverageFaultDetectionRate(String[] fns, String[] titles, String outfn) {
		//String[] titles = {"OA", "EA"};
		
		HashMap<String, double[][]> datas = new HashMap<String, double[][]>();
		
		int index=0;
		int num = fns.length;
		for(String fn: fns) {
			extractCFPlotData(fn, datas, index, num);
			index++;
		}
		
		String[] keys = extractKey(datas); //for consistent index of all the figures
		int[] linetype = new int[num];
		for(int i=0;i<num;i++)
			linetype[i] = i+1;
		
		//draw coverage
		DataSetPlot[] cvds = generateDataSetPlot(datas, keys, num, 0);
		generateEPS("CV_"+outfn, "", cvds, titles, linetype);
		
		//draw fault-detection
		DataSetPlot[] ftds = generateDataSetPlot(datas, keys, num, 1);
		generateEPS("FT_"+outfn, "", ftds, titles, linetype);
		
	}
	
	private String[] extractKey(HashMap<String, double[][]> datas) {
		
		Set<String> keys = datas.keySet();
		String[] result = new String[keys.size()];
		int index = 0;
		for(String key: keys) {
			result[index] = key;
			index++;
		}
		
		return result;
	}

	/*
	 * Index = 0: coverage
	 * Index = 1: fault-detection
	 */
	private DataSetPlot[] generateDataSetPlot(
			HashMap<String, double[][]> datas, String[] keys, int num, int index) {
		
		DataSetPlot[] dsp = new DataSetPlot[num];
		
		for(int ln=0;ln<num;ln++) {
			double[][] dsset = new double[keys.length][2];
			int dsIndex = 0;
			for(String key: keys) {
				double[][] cts = datas.get(key);
				dsset[dsIndex][0] = dsIndex;//x Axis
				dsset[dsIndex][1] = cts[ln][index]; //y Axis
				
				if(Double.isNaN(dsset[dsIndex][1])) dsset[dsIndex][1] = 0;
				dsIndex++;
			}
			
			dsp[ln] = new DataSetPlot(dsset);
		}
				
		return dsp;
	}

	public static void generateEPS(String filename, String title, DataSetPlot[] ds, String[] keys, int[] linetype) {
        JavaPlot p = new JavaPlot();

        PostscriptTerminal epsf = new PostscriptTerminal(filename);
        epsf.setColor(true);
        epsf.setEPS(true);  
        epsf.set("font", "'Arial, 20'");
                
        p.setTerminal(epsf);             
                        
        p.getAxis("x").setLabel("Training data size", "Arial", 24);        
        p.getAxis("y").setLabel("Accuracy", "Arial", 24);
        p.set("bmargin", "5");
        p.set("key spacing", "3");
        p.set("xlabel", "0, -1");
        p.set("xtics", "font 'Arial, 20'");
        p.set("ytics", "font 'Arial, 20'");
        p.set("title font", "Arial, 24");
        
        p.setKey(JavaPlot.Key.BOTTOM_RIGHT);
        
        for(int i=0;i<ds.length;i++) {           
        
          PlotStyle stl = ds[i].getPlotStyle();
          
          stl.setStyle(Style.LINESPOINTS);                      
          stl.setLineType(linetype[i]);
          stl.setPointType(linetype[i]);
          stl.setPointSize(1);                                         
          stl.setLineWidth(10);
              
          ds[i].setTitle(keys[i]);          
          
          p.addPlot(ds[i]);
          
        }        
                          
        p.plot();         
    }	
	
	private void extractCFPlotData(String fn,
			HashMap<String, double[][]> datas, int index, int num) {		
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(fn));
			String line = reader.readLine();
			while(line!=null) {
				String[] conts = line.split("\\s+");
				if(conts.length>=3) {
					double[][] dt = datas.get(conts[2]);
					if(dt==null) {
						dt = new double[num][2];
						datas.put(conts[2], dt);
					}
					
					dt[index][0] = new Double(conts[0]);
					dt[index][1] = new Double(conts[1]);					
				}
				
				line = reader.readLine();
			}
			
			reader.close();
				
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void handleCoverageFaultDetectionRateData(String[] args) {
		String categoryName = args[0];
		String filterName = args[1];
		String expoutdir = args[2];
		String resultoutdir = args[3];
		int approach = new Integer(args[4]);
		
		handleCoverageFaultDetectionRateData(categoryName, filterName, expoutdir, resultoutdir, approach);
	}
	
	/*
	 * Input: name of mutation categories
	 *      : n copies of result file for each running
	 */
	private void handleCoverageFaultDetectionRateData(String categoryName, String filterName, String expoutdir, String resultoutdir, int approach) {
		ArrayList<String> mtNames = extractMutationNames(categoryName);
		ArrayList<String> filter = extractMutationNames(filterName);
		
		mtNames.removeAll(filter);
		
		for(String mt: mtNames) {
			
			int preindex = mt.lastIndexOf('/');
			int postindex = mt.lastIndexOf('.');
			String filename = mt.substring(preindex+1,postindex);			
			
			for(int coveragepolicy=1; coveragepolicy<=4; coveragepolicy++) {
			    
				String[] filenames = new String[20];				
				for(int node=1;node<=20;node++) 															
				    filenames[node-1] = expoutdir + "/" + approach+"_" + filename + "_1_" + coveragepolicy + "_" + node + ".txt";				    				
				
				outputSummary(filenames, approach, coveragepolicy, filename, resultoutdir);
			}
		}
		
 	}
	
	private ArrayList<String> extractMutationNames(String categoryName) {
		ArrayList<String> result = new ArrayList<String>();
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(categoryName));
			
			String line = reader.readLine();			
			while(line!=null) {
				result.add(line);
				line = reader.readLine();
			}
			
			reader.close();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

    private void outputSummary(String[] filenames, int approach, int coveragepolicy, String mt, String outdir) {
    	double[] summary = summarizeCoverageFaultDetectionRate(filenames);
    	String line ="";
    	line += formatDouble(summary[0]);
    	line += formatDouble(summary[1]);
    	line += mt;
    	
    	String filename = getResultName(approach, coveragepolicy, outdir);
    	try{
    		BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write(line);
            writer.newLine();
    		writer.close();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    private String getResultName(int approach, int coveragepolicy, String outdir) {		
		return outdir + "/" + approach+"_"+coveragepolicy+"_CF.txt";
	}

	private static String formatDouble(double coverage) {
		if(coverage<0.0001) 
			return formatString("0");
		
		String value = ""+coverage;
		int length = value.length()>6? 6: value.length();
		value = value.substring(0, length);
		return formatString(value);
	}

    private static String formatString(String value) {
		String result = value;
		for(int i=value.length();i<20;i++)
			result += " ";
		
		return result;
	}

	private double[] summarizeCoverageFaultDetectionRate(String[] filenames) {
		double[] result = new double[2];
		result[0] = 0;
		result[1] = 0;
		int num = 0;
		
		for(String fn: filenames) {
			ArrayList<double[]> fcontents = readCoverageFaultDetectionRate(fn);
			if(fcontents==null) continue;
			
			for(double[] ct: fcontents) {
				result[0]+= ct[0];
				result[1]+= ct[1];
				num++;
			}
		}
		
		result[0] = result[0]/num;
		result[1] = result[1]/num;
		
		return result;
	}

	private ArrayList<double[]> readCoverageFaultDetectionRate(String fn) {
		ArrayList<double[]> result = new ArrayList<double[]>();
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(fn));
			String line = reader.readLine();
			while(line!=null) {
				String[] conts = line.split("\\s+");
				if(conts.length>=2) {
					double[] nc = new double[2];
					nc[0] = new Double(conts[0]);
					nc[1] = new Double(conts[1]);
					result.add(nc);
				}
				
				line = reader.readLine();
			}
			
			reader.close();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	

}
