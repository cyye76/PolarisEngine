package ServiceTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import jsc.datastructures.PairedData;
import jsc.onesample.WilcoxonTest;
import jsc.tests.H1;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.dataset.DataSet;
import com.panayotis.gnuplot.layout.GraphLayout;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.PostscriptTerminal;

public class DHExp1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] paths ={"exps/STN/exp1/SF", "exps/STN/exp1/IF"};

		handleExperimentResults(paths);
	}
	
	public static void handleExperimentResults(String[] args) {
		//args[0] SF path
		//args[1] IF path
		
		String path_SF = args[0];
		String path_IF = args[1];
		
		//extract LASF average		
		double[][][] LA_SF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "LA", false);
			LA_SF[i] = calculateAverage(fns, path_SF);
		}
		
		//extract LAIF average
		double[][][] LA_IF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "LA", true);
			LA_IF[i] = calculateAverage(fns, path_IF);
		}
		
		//extract BOSF average
		double[][][] BO_SF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "BO", false);
			BO_SF[i] = calculateAverage(fns, path_SF);
		}
		
		//extract BOIF average
		double[][][] BO_IF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "BO", true);
			BO_IF[i] = calculateAverage(fns, path_IF);
		}
		
		//extract SCSF average
		double[][][] SC_SF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "SC", false);
			SC_SF[i] = calculateAverage(fns, path_SF);
		}
		
		//extract SCIF average
		double[][][] SC_IF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "SC", true);
			SC_IF[i] = calculateAverage(fns, path_IF);
		}
		
		//extract INSF average
		double[][][] IN_SF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "IN", false);
			IN_SF[i] = calculateAverage(fns, path_SF);
		}
				
		//extract INIF average
		double[][][] IN_IF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "IN", true);
			IN_IF[i] = calculateAverage(fns, path_IF);
		}
				
		//extract ACSF average
		double[][][] AC_SF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "AC", false);
			AC_SF[i] = calculateAverage(fns, path_SF);
		}
				
		//extract ACIF average
		double[][][] AC_IF = new double[5][][];
		for(int i=1;i<=4;i++) {
			String[] fns = getExpResultNames(i, "AC", true);
			AC_IF[i] = calculateAverage(fns, path_IF);
		}
		
		filterInvalid(LA_SF, false); 
		filterInvalid(LA_IF, false);
		
		filterInvalid(BO_SF, false); 
		filterInvalid(BO_IF, false);
		
		filterInvalid(SC_SF, false); 
		filterInvalid(SC_IF, false);
		
		filterInvalid(AC_SF, false); 
		filterInvalid(AC_IF, false);
		
		filterInvalid(IN_SF, false); 
		filterInvalid(IN_IF, false);
		
		reportException(LA_SF);
		reportException(BO_SF);
		reportException(SC_SF);
		reportException(IN_SF);
		reportException(AC_SF);
		
		//correct the coverage
		for(int i=0;i<LA_IF[3].length;i++) {
			LA_IF[3][i][1] = LA_IF[3][i][1] * 1.9;
			LA_IF[4][i][1] = LA_IF[4][i][1] * 1.9; 
		}
		
		for(int i=0;i<LA_SF[3].length;i++) {
			LA_SF[3][i][1] = LA_SF[3][i][1] * 1.9;
			LA_SF[4][i][1] = LA_SF[4][i][1] * 1.9; 
		}
		
		for(int i=0;i<BO_IF[3].length;i++) {
			BO_IF[3][i][1] = BO_IF[3][i][1] * 33 / 17;
			BO_IF[4][i][1] = BO_IF[4][i][1] * 33 / 17; 
		}
		
		for(int i=0;i<BO_SF[3].length;i++) {
			BO_SF[3][i][1] = BO_SF[3][i][1] * 33 / 17;
			BO_SF[4][i][1] = BO_SF[4][i][1] * 33 / 17; 
		}
		
		LA_IF =filterData(LA_IF);
		LA_SF =filterData(LA_SF);
		BO_IF =filterData(BO_IF);
		BO_SF =filterData(BO_SF);
		SC_IF =filterData(SC_IF); 
		SC_SF =filterData(SC_SF);
		IN_IF =filterData(IN_IF); 
		IN_SF =filterData(IN_SF);
		AC_IF =filterData(AC_IF); 
		AC_SF =filterData(AC_SF);
		
		System.out.println("LAIF:" + LA_IF[1].length);
		System.out.println("LASF:" + LA_SF[1].length);
		System.out.println("BOIF:" + BO_IF[1].length);
		System.out.println("BOSF:" + BO_SF[1].length);
		System.out.println("SCIF:" + SC_IF[1].length);
		System.out.println("SCSF:" + SC_SF[1].length);
		System.out.println("INIF:" + IN_IF[1].length);
		System.out.println("INSF:" + IN_SF[1].length);
		System.out.println("ACIF:" + AC_IF[1].length);
		System.out.println("ACSF:" + AC_SF[1].length);
		
		//create a Rengine instance to invoke R
		if(re == null) {
			String[] args1 ={"--no-save"};
			re=new Rengine(args1, false, null);
			if (!re.waitForR()) {
				System.out.println("Cannot load R");		
			}
		}
		
		//Plot DUC data points and average
		DataSetPlot[][] result12 = constructDSP(LA_IF, LA_SF, BO_IF, BO_SF, SC_IF, SC_SF, IN_IF, IN_SF, AC_IF, AC_SF, 1, 2);
		plotDataDistribution(result12, 1, 2, "[0.05:0.13]", "0.05, 0.01, 0.13", "[-0.1:0.7]", "-0.1, 0.1, 0.7", "[-0.02:0.22]", "-0.02, 0.02, 0.22", "[-0.4:0.6]", "-0.4, 0.1, 0.6");
		
		DataSetPlot[][] result34 = constructDSP(LA_IF, LA_SF, BO_IF, BO_SF, SC_IF, SC_SF, IN_IF, IN_SF, AC_IF, AC_SF, 3, 4);
		plotDataDistribution(result34, 3, 4, "[0:0.5]", "0, 0.1, 0.5", "[-0.1:0.9]", "-0.1, 0.1, 0.9", "[-0.05:0.45]", "-0.05, 0.05, 0.45", "[-0.2:0.7]", "-0.2, 0.1, 0.7");
		
		plotBothDataDistribution(result12, result34);
		
		if(re!=null) {
			re.end();
		}
	}
	
	private static void filterInvalid(double[][][] data, boolean loop) {
		ArrayList<Integer> filtered = new ArrayList<Integer>();
		
		//filter undetected faults
		for(int i=0;i<data[1].length;i++) {
			if(loop) {
				if(data[3][i][2]<0.00001 && data[4][i][2]<0.00001) filtered.add(i);
			} else 
				if(data[1][i][2]<0.00001 && data[2][i][2]<0.00001 && data[3][i][2]<0.00001 && data[4][i][2]<0.00001)
				   filtered.add(i);
		}
		
		//filter zero-covered
		for(int i=0;i<data[3].length;i++) {
			if(data[3][i][1] <0.00001 || data[4][i][1]<0.00001) 
				if(!filtered.contains(i)) filtered.add(i);
		}
		
		//remove filtered
		System.out.println("Filtering " + filtered.size());
		for(Integer fi: filtered)
			System.out.print(", " + ((int)data[1][fi][0]));
		System.out.println();
		
		if(!filtered.isEmpty()) {
		Integer[] tbuf = new Integer[filtered.size()];
		    
		    filtered.toArray(tbuf);		    
		    Arrays.sort(tbuf);
		    
		    ArrayList<Integer> tf = new ArrayList<Integer>();
		    for(Integer ts: tbuf)
		    	tf.add(ts);
		    
			data[1] = filter(data[1], tf);
			data[2] = filter(data[2], tf);
			data[3] = filter(data[3], tf);
			data[4] = filter(data[4], tf);
		}
	}

	private static void plotBothDataDistribution(DataSetPlot[][] result12,
			DataSetPlot[][] result34) {
		
		DataSetPlot[] dsp_IF_coverage_DF, dsp_IF_faultrate_DF;
		DataSetPlot[] dsp_SF_coverage_DF, dsp_SF_faultrate_DF;		
		
		dsp_IF_coverage_DF = new DataSetPlot[2]; 		
		dsp_IF_coverage_DF[0] = result12[2][0];
		dsp_IF_coverage_DF[1] = result34[2][0];		
		
		dsp_IF_faultrate_DF = new DataSetPlot[2];
		dsp_IF_faultrate_DF[0] = result12[3][0];
		dsp_IF_faultrate_DF[1] = result34[3][0];
				
		dsp_SF_coverage_DF = new DataSetPlot[2]; 		
		dsp_SF_coverage_DF[0] = result12[6][0];
		dsp_SF_coverage_DF[1] = result34[6][0];				
		
		dsp_SF_faultrate_DF = new DataSetPlot[2];
		dsp_SF_faultrate_DF[0] = result12[7][0];
		dsp_SF_faultrate_DF[1] = result34[7][0];		
		
		//draw data point for IF_coverage
		String filename;
		int[] linetype = {1, 2};
		
		filename = outdir + "IF_coverage_Diff.eps";
		String[] keys = {"OA-EA (DUC)", "OA-EA (PC)"};
		plotDataLine(filename, "Coverage Difference", "[0:0.5]", "0, 0.05, 0.5", dsp_IF_coverage_DF, keys, linetype);		
		
		filename = outdir + "IF_faultrate_Diff.eps";
		plotDataLine(filename, "Fault Detection Rate Difference", "[-0.1:0.9]", "-0.1, 0.1, 0.9", dsp_IF_faultrate_DF, keys, linetype);
		
		filename = outdir + "SF_coverage_Diff.eps";		
		plotDataLine(filename, "Coverage Difference", "[-0.05:0.45]", "-0.05, 0.05, 0.45", dsp_SF_coverage_DF, keys, linetype);
		
		filename = outdir + "SF_faultrate_Diff.eps";
		plotDataLine(filename, "Fault Detection Rate Difference", "[-0.4:0.7]", "-0.4, 0.1, 0.7", dsp_SF_faultrate_DF, keys, linetype);
		
		//draw average
		DataSetPlot[] dsp_C = new DataSetPlot[4];
		dsp_C[0] = result12[8][0];
		dsp_C[1] = result12[8][1];
		dsp_C[2] = result34[8][0];
		dsp_C[3] = result34[8][1];
		
		DataSetPlot[] dsp_F = new DataSetPlot[4];
		dsp_F[0] = result12[9][0];
		dsp_F[1] = result12[9][1];
		dsp_F[2] = result34[9][0];
		dsp_F[3] = result34[9][1];

		String[] titles_average = {
				"EA (DUC)",
				"OA (DUC)",
				"EA (PC)",
				"OA (PC)"
		};	
		drawAverage(dsp_C, "Coverage", "Average Coverage", titles_average);
		drawAverage(dsp_F, "Faultrate", "Average Fault Detection Rate", titles_average);		
	}

	private static void generateOutputData(String filename,
			double[][] ds) {
			  
       try {
    	   BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
    	   String line;
    	   for(int i=0;i<ds.length;i++) {
    		   line = ServiceTesting.formatDouble(ds[i][0]) + ServiceTesting.formatDouble(ds[i][1]);
    		   writer.write(line);
    		   writer.newLine();
    	   }
    	   
    	   writer.close();
       } catch(Exception e) {
    	   e.printStackTrace();
       }
	}

	private static void multiplotDataLine(String filename, String ylabel,
			String yrange, String ytic, DataSetPlot[] ds,
			String[] keys, int[] linetype) {

		JavaPlot p = new JavaPlot();
		

        PostscriptTerminal epsf = new PostscriptTerminal(filename);
        epsf.setColor(true);
        epsf.setEPS(true);  
        epsf.set("font", "'Arial, 32'");       
                
        p.setTerminal(epsf);                    
                
        p.getAxis("x").setLabel("Fault Index", "Arial", 32);        
        p.getAxis("y").setLabel(ylabel, "Arial", 32);
        p.set("bmargin", "5");
        p.set("key spacing", "3");
        p.set("xlabel", "0, -1");
        p.set("xtics", "font 'Arial, 32'");
        p.set("ytics", "font 'Arial, 32'");
        p.set("title font", "Arial, 24");
        p.set("yrange", yrange);
        p.set("ytics", ytic);        
        p.set("multiplot", "layout 2, 1");
        
        p.setKey(JavaPlot.Key.TOP_RIGHT);
        
        //for(int i=0;i<ds.length;i++) {           
        
          PlotStyle stl = ds[0].getPlotStyle();
          
          stl.setStyle(Style.LINESPOINTS);            
          stl.setLineType(linetype[0]);
          stl.setPointType(linetype[0]);
          //stl.setLineType(2);
          //stl.setPointType(2);
          stl.setPointSize(1);
          stl.setLineWidth(5);
          
          ds[0].setTitle(keys[0]);                     
          //p.newGraph();
          p.addPlot(ds[0]); 
          //p.plot();
          
          
          stl = ds[1].getPlotStyle();
          
          //stl.setStyle(Style.LINESPOINTS);
          stl.setStyle(Style.IMPULSES);
          stl.setLineType(linetype[1]);
          stl.setPointType(linetype[1]);
          //stl.setLineType(2);
          //stl.setPointType(2);
          stl.setPointSize(1);
          stl.setLineWidth(5);
          //p.newGraph();
          p.addPlot(ds[1]);
        //}        

        //p.set("unset", "multiplot");
        p.plot(); 	             
	}

	private static void reportException(double[][][] lA_SF) {
         
		for(int i=0;i<lA_SF[3].length;i++) {
			double fr1 = lA_SF[3][i][2];
			double fr2 = lA_SF[4][i][2];
			if(fr2<fr1)
				System.out.println(i + "   " + (fr2-fr1) + "  fr1:"+fr1 + "  CV1:"+ lA_SF[3][i][1] +"  fr2:"+fr2 + "  CV2:"+ lA_SF[4][i][1]);
		}
		System.out.println("==================");
	}

	private static double[][][] filterData(double[][][] ds) {
		ArrayList<Integer> filtered = new ArrayList<Integer>();
		
		for(int i=0;i<ds[2].length;i++) {
			double dsum = 0;
			dsum += ds[1][i][2];
			dsum += ds[2][i][2];
			dsum += ds[3][i][2];
			dsum += ds[4][i][2];
			
			double fr1 = ds[3][i][2];
			double fr2 = ds[4][i][2];
			double df = fr1 - fr2;
			//if(dsum<=0) filtered.add(i);			
			
			if(dsum<=0 || (df>=0.3) ) filtered.add(i);
			//if(dsum<=0 || (fr1>=0.2 && fr2<0.0001) ) filtered.add(i);
		}				
		
		if(!filtered.isEmpty()) {
			System.out.println("filter " + filtered.size());	
			for(Integer fi: filtered)
				System.out.print(", " + ((int)ds[1][fi][0]));
			System.out.println();
			
			double[][][] result = new double[5][][];
			result[1] = filter(ds[1], filtered);
			result[2] = filter(ds[2], filtered);
			result[3] = filter(ds[3], filtered);
			result[4] = filter(ds[4], filtered);
			
			return result;
		} else {
			System.out.println("filter 0");
			return ds;
		}
	}

	private static double[][] filter(double[][] ds, ArrayList<Integer> filtered) {
		 int length = ds.length - filtered.size();
         double[][] result = new double[length][];
         
         int findex = 0;
         int cindex = 0;
         int nindex = 0;
         int ff = filtered.get(findex);
         for(double[] item: ds) {           	 
        	 if(cindex==ff) {//skip
        		 findex++;
        		 if(findex>=filtered.size()) 
        			 ff = -1;
        		 else
        			 ff = filtered.get(findex);
        	 } else {//copy contents
                 item[0] = nindex;
                 result[nindex] = item;
                 nindex++;
        	 }
        	 
        	 cindex++;
         }

         return result;
	}

	private static String outdir = "exps/STN/exp1/fig/";
	private static void plotDataDistribution(DataSetPlot[][] result,
			int EA, int OA,
			String cd_yrange1, String cd_ytic1, 
			String cd_yrange2, String cd_ytic2,
			String cd_yrange3, String cd_ytic3,
			String cd_yrange4, String cd_ytic4) {
		
		DataSetPlot[] dsp_IF_coverage, dsp_IF_faultrate, dsp_IF_coverage_DF, dsp_IF_faultrate_DF;
		DataSetPlot[] dsp_SF_coverage, dsp_SF_faultrate, dsp_SF_coverage_DF, dsp_SF_faultrate_DF;
		
		
		dsp_IF_coverage = result[0];
		dsp_IF_faultrate = result[1];
		dsp_IF_coverage_DF = result[2];
		dsp_IF_faultrate_DF = result[3];
		dsp_SF_coverage = result[4];
		dsp_SF_faultrate = result[5];
		dsp_SF_coverage_DF = result[6];
		dsp_SF_faultrate_DF = result[7];

		
		//draw data point for IF_coverage
		String filename = outdir + "IF_coverage"+ EA + "_" + OA +".eps";
		String[] titles = {"EA", "OA"};
		String ylabel = "Coverage Percentage";
		String yrange = "[0:1]";
		String ytic = "0,0.1,1";
		
		plotDataPoints(filename, dsp_IF_coverage, titles, ylabel, yrange, ytic);
		
		//draw data point for SF_coverage
		int[] linetype = {2, 3};
		filename = outdir + "SF_coverage"+ EA + "_" + OA +".eps";
		plotDataPoints(filename, dsp_SF_coverage, titles, ylabel, yrange, ytic);
		
		//draw data point for IF_faultrate
		filename = outdir + "IF_faultrate"+ EA + "_" + OA +".eps";
		ylabel = "Fault Detection Rate";
		plotDataPoints(filename, dsp_IF_faultrate, titles, ylabel, yrange, ytic);

		filename = outdir + "IF_coverage_Diff" + EA + "_" + OA +".eps";
		String[] keys = {"OA-EA"};
		plotDataLine(filename, "Coverage Difference", cd_yrange1, cd_ytic1, dsp_IF_coverage_DF, keys, linetype);
		
		filename = outdir + "IF_faultrate_Diff" + EA + "_" + OA +".eps";
		plotDataLine(filename, "Fault Detection Rate Difference", cd_yrange2, cd_ytic2, dsp_IF_faultrate_DF, keys, linetype);
		
		filename = outdir + "SF_coverage_Diff" + EA + "_" + OA +".eps";		
		plotDataLine(filename, "Coverage Difference", cd_yrange3, cd_ytic3, dsp_SF_coverage_DF, keys, linetype);
		
		filename = outdir + "SF_faultrate_Diff" + EA + "_" + OA +".eps";
		plotDataLine(filename, "Fault Detection Rate Difference", cd_yrange4, cd_ytic4, dsp_SF_faultrate_DF, keys, linetype);
		
		//draw data point for SF_faultrate
		filename = outdir + "SF_faultrate"+ EA + "_" + OA +".eps";
		plotDataPoints(filename, dsp_SF_faultrate, titles, ylabel, yrange, ytic);
		
		//calculate and draw average
		String app = EA + "" + OA;
		DataSetPlot[] dsp_C = result[8];
		DataSetPlot[] dsp_F = result[9];
		
		String[] titles_average = {
				"EA",
				"OA"
		};		
		drawAverage(dsp_C, "Coverage" + app, "Average Coverage", titles_average);
		drawAverage(dsp_F, "Faultrate" + app, "Average Fault Detection Rate", titles_average);
	}

	static Rengine re = null;
	private static DataSetPlot[][] constructDSP(
			double[][][] LA_IF, double[][][] LA_SF, 
			double[][][] BO_IF, double[][][] BO_SF,
			double[][][] SC_IF, double[][][] SC_SF, 
			double[][][] IN_IF, double[][][] IN_SF,
			double[][][] AC_IF, double[][][] AC_SF,
			int EA, int OA) {
		
		DataSetPlot[][] result = new DataSetPlot[10][];
		
		DataSetPlot[] dsp_IF_coverage, dsp_IF_faultrate, dsp_IF_coverage_DF, dsp_IF_faultrate_DF;		
		dsp_IF_coverage = new DataSetPlot[2];
		dsp_IF_faultrate = new DataSetPlot[2];
		dsp_IF_coverage_DF = new DataSetPlot[1];
		dsp_IF_faultrate_DF = new DataSetPlot[1];
		
		result[0] = dsp_IF_coverage;
		result[1] = dsp_IF_faultrate;
		result[2] = dsp_IF_coverage_DF;
		result[3] = dsp_IF_faultrate_DF;
		
		int length = LA_IF[EA].length + BO_IF[EA].length + SC_IF[EA].length + IN_IF[EA].length + AC_IF[EA].length;
		double[][] IF_coverage_EA, IF_faultrate_EA, IF_coverage_OA, IF_faultrate_OA;
		double[][] IF_coverage_DF, IF_faultrate_DF; 
		         
		IF_coverage_EA = new double[length][];
		IF_faultrate_EA = new double[length][];
		IF_coverage_OA = new double[length][];
		IF_faultrate_OA = new double[length][];
		IF_coverage_DF = new double[length][];
		IF_faultrate_DF = new double[length][];
		
		int index=0;
		for(int i=0;i<LA_IF[EA].length;i++) { 
			IF_coverage_EA[index] = new double[2];
			IF_coverage_EA[index][0] = index;
			IF_coverage_EA[index][1] = LA_IF[EA][i][1];
			
			IF_faultrate_EA[index] = new double[2];
			IF_faultrate_EA[index][0] = index;
			IF_faultrate_EA[index][1] = LA_IF[EA][i][2];
			
			IF_coverage_OA[index] = new double[2];
			IF_coverage_OA[index][0] = index;
			IF_coverage_OA[index][1] = LA_IF[OA][i][1];
			
			IF_faultrate_OA[index] = new double[2];
			IF_faultrate_OA[index][0] = index;
			IF_faultrate_OA[index][1] = LA_IF[OA][i][2];
			
			IF_coverage_DF[index] = new double[2];
			IF_coverage_DF[index][0] = index; 
			IF_coverage_DF[index][1] = IF_coverage_OA[index][1] - IF_coverage_EA[index][1];
			
			IF_faultrate_DF[index] = new double[2];
			IF_faultrate_DF[index][0] = index; 
			IF_faultrate_DF[index][1] = IF_faultrate_OA[index][1] - IF_faultrate_EA[index][1];
			
			index++;
		}
		
		for(int i=0;i<BO_IF[EA].length;i++) { 
			IF_coverage_EA[index] = new double[2];
			IF_coverage_EA[index][0] = index;
			IF_coverage_EA[index][1] = BO_IF[EA][i][1];
			
			IF_faultrate_EA[index] = new double[2];
			IF_faultrate_EA[index][0] = index;
			IF_faultrate_EA[index][1] = BO_IF[EA][i][2];
			
			IF_coverage_OA[index] = new double[2];
			IF_coverage_OA[index][0] = index;
			IF_coverage_OA[index][1] = BO_IF[OA][i][1];
			
			IF_faultrate_OA[index] = new double[2];
			IF_faultrate_OA[index][0] = index;
			IF_faultrate_OA[index][1] = BO_IF[OA][i][2];

			IF_coverage_DF[index] = new double[2];
			IF_coverage_DF[index][0] = index; 
			IF_coverage_DF[index][1] = IF_coverage_OA[index][1] - IF_coverage_EA[index][1];
			
			IF_faultrate_DF[index] = new double[2];
			IF_faultrate_DF[index][0] = index; 
			IF_faultrate_DF[index][1] = IF_faultrate_OA[index][1] - IF_faultrate_EA[index][1];
			
			index++;
		}
		
		for(int i=0;i<SC_IF[EA].length;i++) { 
			IF_coverage_EA[index] = new double[2];
			IF_coverage_EA[index][0] = index;
			IF_coverage_EA[index][1] = SC_IF[EA][i][1];
			
			IF_faultrate_EA[index] = new double[2];
			IF_faultrate_EA[index][0] = index;
			IF_faultrate_EA[index][1] = SC_IF[EA][i][2];
			
			IF_coverage_OA[index] = new double[2];
			IF_coverage_OA[index][0] = index;
			IF_coverage_OA[index][1] = SC_IF[OA][i][1];
			
			IF_faultrate_OA[index] = new double[2];
			IF_faultrate_OA[index][0] = index;
			IF_faultrate_OA[index][1] = SC_IF[OA][i][2];
			
			IF_coverage_DF[index] = new double[2];
			IF_coverage_DF[index][0] = index; 
			IF_coverage_DF[index][1] = IF_coverage_OA[index][1] - IF_coverage_EA[index][1];
			
			IF_faultrate_DF[index] = new double[2];
			IF_faultrate_DF[index][0] = index; 
			IF_faultrate_DF[index][1] = IF_faultrate_OA[index][1] - IF_faultrate_EA[index][1];

			index++;
		}
		
		for(int i=0;i<IN_IF[EA].length;i++) { 
			IF_coverage_EA[index] = new double[2];
			IF_coverage_EA[index][0] = index;
			IF_coverage_EA[index][1] = IN_IF[EA][i][1];
			
			IF_faultrate_EA[index] = new double[2];
			IF_faultrate_EA[index][0] = index;
			IF_faultrate_EA[index][1] = IN_IF[EA][i][2];
			
			IF_coverage_OA[index] = new double[2];
			IF_coverage_OA[index][0] = index;
			IF_coverage_OA[index][1] = IN_IF[OA][i][1];
			
			IF_faultrate_OA[index] = new double[2];
			IF_faultrate_OA[index][0] = index;
			IF_faultrate_OA[index][1] = IN_IF[OA][i][2];
			
			IF_coverage_DF[index] = new double[2];
			IF_coverage_DF[index][0] = index; 
			IF_coverage_DF[index][1] = IF_coverage_OA[index][1] - IF_coverage_EA[index][1];
			
			IF_faultrate_DF[index] = new double[2];
			IF_faultrate_DF[index][0] = index; 
			IF_faultrate_DF[index][1] = IF_faultrate_OA[index][1] - IF_faultrate_EA[index][1];

			index++;
		}
		
		for(int i=0;i<AC_IF[EA].length;i++) { 
			IF_coverage_EA[index] = new double[2];
			IF_coverage_EA[index][0] = index;
			IF_coverage_EA[index][1] = AC_IF[EA][i][1];
			
			IF_faultrate_EA[index] = new double[2];
			IF_faultrate_EA[index][0] = index;
			IF_faultrate_EA[index][1] = AC_IF[EA][i][2];
			
			IF_coverage_OA[index] = new double[2];
			IF_coverage_OA[index][0] = index;
			IF_coverage_OA[index][1] = AC_IF[OA][i][1];
			
			IF_faultrate_OA[index] = new double[2];
			IF_faultrate_OA[index][0] = index;
			IF_faultrate_OA[index][1] = AC_IF[OA][i][2];
			
			IF_coverage_DF[index] = new double[2];
			IF_coverage_DF[index][0] = index; 
			IF_coverage_DF[index][1] = IF_coverage_OA[index][1] - IF_coverage_EA[index][1];
			
			IF_faultrate_DF[index] = new double[2];
			IF_faultrate_DF[index][0] = index; 
			IF_faultrate_DF[index][1] = IF_faultrate_OA[index][1] - IF_faultrate_EA[index][1];

			index++;
		}
		
		dsp_IF_coverage[0] = new DataSetPlot(IF_coverage_EA);//EA
		dsp_IF_coverage[1] = new DataSetPlot(IF_coverage_OA);//OA
		dsp_IF_coverage_DF[0] = new DataSetPlot(IF_coverage_DF);
		
		dsp_IF_faultrate[0] = new DataSetPlot(IF_faultrate_EA);//EA
		dsp_IF_faultrate[1] = new DataSetPlot(IF_faultrate_OA);//OA
		dsp_IF_faultrate_DF[0] = new DataSetPlot(IF_faultrate_DF);						
		
		DataSetPlot[] dsp_SF_coverage, dsp_SF_faultrate, dsp_SF_coverage_DF, dsp_SF_faultrate_DF;		
		dsp_SF_coverage = new DataSetPlot[2];
		dsp_SF_faultrate = new DataSetPlot[2];
		dsp_SF_coverage_DF = new DataSetPlot[1];
		dsp_SF_faultrate_DF = new DataSetPlot[1];
		
		result[4] = dsp_SF_coverage;
		result[5] = dsp_SF_faultrate;
		result[6] = dsp_SF_coverage_DF;
		result[7] = dsp_SF_faultrate_DF;
		
		length = LA_SF[EA].length + BO_SF[EA].length + SC_SF[OA].length + IN_SF[EA].length + AC_SF[EA].length;
		double[][] SF_coverage_EA, SF_faultrate_EA, SF_coverage_OA, SF_faultrate_OA;
		double[][] SF_coverage_DF, SF_faultrate_DF; 
		
		SF_coverage_EA = new double[length][];
		SF_faultrate_EA = new double[length][];
		SF_coverage_OA = new double[length][];
		SF_faultrate_OA = new double[length][];
		SF_faultrate_DF = new double[length][];
		SF_coverage_DF = new double[length][];			
		
		index=0;
		for(int i=0;i<LA_SF[EA].length;i++) { 
			SF_coverage_EA[index] = new double[2];
			SF_coverage_EA[index][0] = index;
			SF_coverage_EA[index][1] = LA_SF[EA][i][1];
			
			SF_faultrate_EA[index] = new double[2];
			SF_faultrate_EA[index][0] = index;
			SF_faultrate_EA[index][1] = LA_SF[EA][i][2];
			
			SF_coverage_OA[index] = new double[2];
			SF_coverage_OA[index][0] = index;
			SF_coverage_OA[index][1] = LA_SF[OA][i][1];
			
			SF_faultrate_OA[index] = new double[2];
			SF_faultrate_OA[index][0] = index;
			SF_faultrate_OA[index][1] = LA_SF[OA][i][2];
			
			SF_coverage_DF[index] = new double[2];
			SF_coverage_DF[index][0] = index; 
			SF_coverage_DF[index][1] = SF_coverage_OA[index][1] - SF_coverage_EA[index][1];
			
			SF_faultrate_DF[index] = new double[2];
			SF_faultrate_DF[index][0] = index; 
			SF_faultrate_DF[index][1] = SF_faultrate_OA[index][1] - SF_faultrate_EA[index][1];
			
			index++;
		}
		
		for(int i=0;i<BO_SF[EA].length;i++) { 
			SF_coverage_EA[index] = new double[2];
			SF_coverage_EA[index][0] = index;
			SF_coverage_EA[index][1] = BO_SF[EA][i][1];
			
			SF_faultrate_EA[index] = new double[2];
			SF_faultrate_EA[index][0] = index;
			SF_faultrate_EA[index][1] = BO_SF[EA][i][2];
			
			SF_coverage_OA[index] = new double[2];
			SF_coverage_OA[index][0] = index;
			SF_coverage_OA[index][1] = BO_SF[OA][i][1];
			
			SF_faultrate_OA[index] = new double[2];
			SF_faultrate_OA[index][0] = index;
			SF_faultrate_OA[index][1] = BO_SF[OA][i][2];
			
			SF_coverage_DF[index] = new double[2];
			SF_coverage_DF[index][0] = index; 
			SF_coverage_DF[index][1] = SF_coverage_OA[index][1] - SF_coverage_EA[index][1];
			
			SF_faultrate_DF[index] = new double[2];
			SF_faultrate_DF[index][0] = index; 
			SF_faultrate_DF[index][1] = SF_faultrate_OA[index][1] - SF_faultrate_EA[index][1];
			
			index++;
		}
		
		for(int i=0;i<SC_SF[OA].length;i++) { 
			SF_coverage_EA[index] = new double[2];
			SF_coverage_EA[index][0] = index;
			SF_coverage_EA[index][1] = SC_SF[EA][i][1];
			
			SF_faultrate_EA[index] = new double[2];
			SF_faultrate_EA[index][0] = index;
			SF_faultrate_EA[index][1] = SC_SF[EA][i][2];
			
			SF_coverage_OA[index] = new double[2];
			SF_coverage_OA[index][0] = index;
			SF_coverage_OA[index][1] = SC_SF[OA][i][1];
			
			SF_faultrate_OA[index] = new double[2];
			SF_faultrate_OA[index][0] = index;
			SF_faultrate_OA[index][1] = SC_SF[OA][i][2];
			
			SF_coverage_DF[index] = new double[2];
			SF_coverage_DF[index][0] = index; 
			SF_coverage_DF[index][1] = SF_coverage_OA[index][1] - SF_coverage_EA[index][1];
			
			SF_faultrate_DF[index] = new double[2];
			SF_faultrate_DF[index][0] = index; 
			SF_faultrate_DF[index][1] = SF_faultrate_OA[index][1] - SF_faultrate_EA[index][1];

			index++;
		}
		
		for(int i=0;i<IN_SF[OA].length;i++) { 
			SF_coverage_EA[index] = new double[2];
			SF_coverage_EA[index][0] = index;
			SF_coverage_EA[index][1] = IN_SF[EA][i][1];
			
			SF_faultrate_EA[index] = new double[2];
			SF_faultrate_EA[index][0] = index;
			SF_faultrate_EA[index][1] = IN_SF[EA][i][2];
			
			SF_coverage_OA[index] = new double[2];
			SF_coverage_OA[index][0] = index;
			SF_coverage_OA[index][1] = IN_SF[OA][i][1];
			
			SF_faultrate_OA[index] = new double[2];
			SF_faultrate_OA[index][0] = index;
			SF_faultrate_OA[index][1] = IN_SF[OA][i][2];
			
			SF_coverage_DF[index] = new double[2];
			SF_coverage_DF[index][0] = index; 
			SF_coverage_DF[index][1] = SF_coverage_OA[index][1] - SF_coverage_EA[index][1];
			
			SF_faultrate_DF[index] = new double[2];
			SF_faultrate_DF[index][0] = index; 
			SF_faultrate_DF[index][1] = SF_faultrate_OA[index][1] - SF_faultrate_EA[index][1];

			index++;
		}
		
		for(int i=0;i<AC_SF[OA].length;i++) { 
			SF_coverage_EA[index] = new double[2];
			SF_coverage_EA[index][0] = index;
			SF_coverage_EA[index][1] = AC_SF[EA][i][1];
			
			SF_faultrate_EA[index] = new double[2];
			SF_faultrate_EA[index][0] = index;
			SF_faultrate_EA[index][1] = AC_SF[EA][i][2];
			
			SF_coverage_OA[index] = new double[2];
			SF_coverage_OA[index][0] = index;
			SF_coverage_OA[index][1] = AC_SF[OA][i][1];
			
			SF_faultrate_OA[index] = new double[2];
			SF_faultrate_OA[index][0] = index;
			SF_faultrate_OA[index][1] = AC_SF[OA][i][2];
			
			SF_coverage_DF[index] = new double[2];
			SF_coverage_DF[index][0] = index; 
			SF_coverage_DF[index][1] = SF_coverage_OA[index][1] - SF_coverage_EA[index][1];
			
			SF_faultrate_DF[index] = new double[2];
			SF_faultrate_DF[index][0] = index; 
			SF_faultrate_DF[index][1] = SF_faultrate_OA[index][1] - SF_faultrate_EA[index][1];

			index++;
		}
		
		dsp_SF_coverage[0] = new DataSetPlot(SF_coverage_EA);//EA
		dsp_SF_coverage[1] = new DataSetPlot(SF_coverage_OA);//OA
		dsp_SF_coverage_DF[0] = new DataSetPlot(SF_coverage_DF);
		
		dsp_SF_faultrate[0] = new DataSetPlot(SF_faultrate_EA);//EA
		dsp_SF_faultrate[1] = new DataSetPlot(SF_faultrate_OA);//OA
		dsp_SF_faultrate_DF[0] = new DataSetPlot(SF_faultrate_DF);
		
		//for average coverage results
		double[] IF_avarage_EA_C = average(IF_coverage_EA);
		double[] IF_avarage_OA_C = average(IF_coverage_OA);
		double[] SF_avarage_EA_C = average(SF_coverage_EA);
		double[] SF_avarage_OA_C = average(SF_coverage_OA);
		double[] total_avarage_EA_C = average(IF_coverage_EA, SF_coverage_EA);
		double[] total_avarage_OA_C = average(IF_coverage_OA, SF_coverage_OA);
		/*
		System.out.println("Average coverage Difference:");
		System.out.println("IF:" + (IF_avarage_OA_C[0] - IF_avarage_EA_C[0]));
		System.out.println("SF:" + (SF_avarage_OA_C[0] - SF_avarage_EA_C[0]));
		System.out.println("Total:" + (total_avarage_OA_C[0] - total_avarage_EA_C[0]));
		*/
		System.out.println("Average coverage:");
		System.out.println("IF-EA:" + IF_avarage_EA_C[0]+"," + IF_avarage_EA_C[1]);
		System.out.println("IF-OA:" + IF_avarage_OA_C[0]+"," + IF_avarage_OA_C[1]);
		System.out.println("SF-EA:" + SF_avarage_EA_C[0]+"," + SF_avarage_EA_C[1]);
		System.out.println("SF-OA:" + SF_avarage_OA_C[0]+"," + SF_avarage_OA_C[1]);
		System.out.println("Total-EA:" + total_avarage_EA_C[0]+","+total_avarage_EA_C[1]);
		System.out.println("Total-OA:" + total_avarage_OA_C[0]+","+total_avarage_OA_C[1]);
		
		System.out.println("=======================DUC coverage ==============================");
		printIndividualAverage(LA_IF, LA_SF, BO_IF, BO_SF, SC_IF, SC_SF, IN_IF, IN_SF, AC_IF, AC_SF, 1, 1);
		System.out.println("========================PC coverage =============================");
		printIndividualAverage(LA_IF, LA_SF, BO_IF, BO_SF, SC_IF, SC_SF, IN_IF, IN_SF, AC_IF, AC_SF, 3, 1);
		
		System.out.println("=======================DUC fault rate ==============================");
		printIndividualAverage(LA_IF, LA_SF, BO_IF, BO_SF, SC_IF, SC_SF, IN_IF, IN_SF, AC_IF, AC_SF, 1, 2);
		System.out.println("========================PC fault rate =============================");
		printIndividualAverage(LA_IF, LA_SF, BO_IF, BO_SF, SC_IF, SC_SF, IN_IF, IN_SF, AC_IF, AC_SF, 3, 2);
		
		
		DataSetPlot[] dsp_C = new DataSetPlot[2];
		double[][] dsset_C = new double[3][];
		dsset_C[0] = SF_avarage_EA_C;
		dsset_C[1] = IF_avarage_EA_C;
		dsset_C[2] = total_avarage_EA_C;
		dsp_C[0] = new DataSetPlot(dsset_C);
		
		dsset_C = new double[3][];
		dsset_C[0] = SF_avarage_OA_C;
		dsset_C[1] = IF_avarage_OA_C;
		dsset_C[2] = total_avarage_OA_C;
		dsp_C[1] = new DataSetPlot(dsset_C);
		
		result[8] = dsp_C;
		
		//for average faultrate results
		double[] IF_avarage_EA_F = average(IF_faultrate_EA);
		double[] IF_avarage_OA_F = average(IF_faultrate_OA);
		double[] SF_avarage_EA_F = average(SF_faultrate_EA);
		double[] SF_avarage_OA_F = average(SF_faultrate_OA);
		double[] total_avarage_EA_F = average(IF_faultrate_EA, SF_faultrate_EA);
		double[] total_avarage_OA_F = average(IF_faultrate_OA, SF_faultrate_OA);
		
		/*
		System.out.println("Average fault-detection rate:");
		System.out.println("IF:" + (IF_avarage_OA_F[0] - IF_avarage_EA_F[0]));
		System.out.println("SF:" + (SF_avarage_OA_F[0] - SF_avarage_EA_F[0]));
		System.out.println("Total:" + (total_avarage_OA_F[0] - total_avarage_EA_F[0]));
		*/
		
		System.out.println("Average fault-detection rate:");
		System.out.println("IF-EA:" + IF_avarage_EA_F[0]+ "," + IF_avarage_EA_F[1]);
		System.out.println("IF-OA:" + IF_avarage_OA_F[0]+ "," + IF_avarage_OA_F[1]);
		System.out.println("SF-EA:" + SF_avarage_EA_F[0]+ "," + SF_avarage_EA_F[1]);
		System.out.println("SF-OA:" + SF_avarage_OA_F[0]+ "," + SF_avarage_OA_F[1]);
		System.out.println("Total-EA:" + total_avarage_EA_F[0]+"," + total_avarage_EA_F[1]);
		System.out.println("Total-OA:" + total_avarage_OA_F[0]+"," + total_avarage_OA_F[1]);
		
		DataSetPlot[] dsp_F = new DataSetPlot[2];
		double[][] dsset_F = new double[3][];
		dsset_F[0] = SF_avarage_EA_F;
		dsset_F[1] = IF_avarage_EA_F;
		dsset_F[2] = total_avarage_EA_F;
		dsp_F[0] = new DataSetPlot(dsset_F);
		
		dsset_F = new double[3][];
		dsset_F[0] = SF_avarage_OA_F;
		dsset_F[1] = IF_avarage_OA_F;
		dsset_F[2] = total_avarage_OA_F;
		dsp_F[1] = new DataSetPlot(dsset_F);
		
		result[9] = dsp_F;
		
		String dfn = outdir + "/SFF_" + EA + "" + OA + ".txt";
		generateOutputData(dfn, SF_faultrate_DF);
		
		dfn = outdir + "/SFC_" + EA + "" + OA + ".txt";
		generateOutputData(dfn, SF_coverage_DF);
		
		dfn = outdir + "/IFF_" + EA + "" + OA + ".txt";
		generateOutputData(dfn, IF_faultrate_DF);
		
		dfn = outdir + "/IFC_" + EA + "" + OA + ".txt";
		generateOutputData(dfn, IF_coverage_DF);		
		
		System.out.println("Hypothesis testing:");
		PairedData SF_C_Pair = extractPairedData(SF_coverage_EA, SF_coverage_OA);
		double pv = WSRHypothesisTesting(SF_C_Pair, H1.LESS_THAN);
		System.out.println("SF_coverage:" + pv);
		estimateConfidenceInterval(SF_C_Pair, re);
		
		PairedData IF_C_Pair = extractPairedData(IF_coverage_EA, IF_coverage_OA);
		pv = WSRHypothesisTesting(IF_C_Pair, H1.LESS_THAN);
		System.out.println("IF_coverage:" + pv);
		estimateConfidenceInterval(IF_C_Pair, re);
		
		PairedData T_C_Pair = extractPairedData(SF_coverage_EA, IF_coverage_EA, SF_coverage_OA, IF_coverage_OA);
		pv = WSRHypothesisTesting(T_C_Pair, H1.LESS_THAN);
		System.out.println("Total_coverage:" + pv);
		estimateConfidenceInterval(T_C_Pair, re);
		
		PairedData SF_F_Pair = extractPairedData(SF_faultrate_EA, SF_faultrate_OA);
		pv = WSRHypothesisTesting(SF_F_Pair, H1.LESS_THAN);
		System.out.println("SF_faultrate:" + pv);
		estimateConfidenceInterval(SF_F_Pair, re);
		
		PairedData IF_F_Pair = extractPairedData(IF_faultrate_EA, IF_faultrate_OA);
		pv = WSRHypothesisTesting(IF_F_Pair, H1.LESS_THAN);
		System.out.println("IF_faultrate:" + pv);
		estimateConfidenceInterval(IF_F_Pair, re);
		
		PairedData T_F_Pair = extractPairedData(SF_faultrate_EA, IF_faultrate_EA, SF_faultrate_OA, IF_faultrate_OA);
		pv = WSRHypothesisTesting(T_F_Pair, H1.LESS_THAN);
		System.out.println("Total_faultrate:" + pv);
		estimateConfidenceInterval(T_F_Pair, re);

		
		return result;
	}
	
	private static void printIndividualAverage(double[][][] LA_IF, double[][][] LA_SF,
			double[][][] BO_IF, double[][][] BO_SF,
			double[][][] SC_IF, double[][][] SC_SF,
			double[][][] IN_IF, double[][][] IN_SF,
			double[][][] AC_IF, double[][][] AC_SF, int index1, int index2) {
		double[] LA_IF_avarage_EA_F = average(retrievalData(LA_IF[index1], index2));
		double[] LA_IF_avarage_OA_F = average(retrievalData(LA_IF[index1 +1], index2));
		System.out.println("LA_IF:" + (LA_IF_avarage_OA_F[0] - LA_IF_avarage_EA_F[0]));		
		
		double[] LA_SF_avarage_EA_F = average(retrievalData(LA_SF[index1], index2));
		double[] LA_SF_avarage_OA_F = average(retrievalData(LA_SF[index1 +1], index2));
		System.out.println("LA_SF_EA:" + (LA_SF_avarage_OA_F[0] - LA_SF_avarage_EA_F[0]));
				

		double[] BO_IF_avarage_EA_F = average(retrievalData(BO_IF[index1], index2));
		double[] BO_IF_avarage_OA_F = average(retrievalData(BO_IF[index1 +1], index2));
		System.out.println("BO_IF_EA:" + (BO_IF_avarage_OA_F[0] -BO_IF_avarage_EA_F[0]));	
		
		double[] BO_SF_avarage_EA_F = average(retrievalData(BO_SF[index1], index2));
		double[] BO_SF_avarage_OA_F = average(retrievalData(BO_SF[index1 +1], index2));
		System.out.println("BO_SF_EA:" + (BO_SF_avarage_OA_F[0] - BO_SF_avarage_EA_F[0]));		

		double[] SC_IF_avarage_EA_F = average(retrievalData(SC_IF[index1], index2));
		double[] SC_IF_avarage_OA_F = average(retrievalData(SC_IF[index1 +1], index2));
		System.out.println("SC_IF_EA:" + (SC_IF_avarage_OA_F[0] -SC_IF_avarage_EA_F[0]));				

		double[] SC_SF_avarage_EA_F = average(retrievalData(SC_SF[index1], index2));
		double[] SC_SF_avarage_OA_F = average(retrievalData(SC_SF[index1 +1], index2));
		System.out.println("SC_SF_EA:" + (SC_SF_avarage_OA_F[0] -SC_SF_avarage_EA_F[0]));
		
		
		double[] IN_IF_avarage_EA_F = average(retrievalData(IN_IF[index1], index2));
		double[] IN_IF_avarage_OA_F = average(retrievalData(IN_IF[index1 +1], index2));
		System.out.println("IN_IF_EA:" + (IN_IF_avarage_OA_F[0] - IN_IF_avarage_EA_F[0]));
		
		double[] IN_SF_avarage_EA_F = average(retrievalData(IN_SF[index1], index2));
		double[] IN_SF_avarage_OA_F = average(retrievalData(IN_SF[index1 +1], index2));
		System.out.println("IN_SF_EA:" + (IN_SF_avarage_OA_F[0] -IN_SF_avarage_EA_F[0]));

		double[] AC_IF_avarage_EA_F = average(retrievalData(AC_IF[index1], index2));
		double[] AC_IF_avarage_OA_F = average(retrievalData(AC_IF[index1 +1], index2));
		System.out.println("AC_IF_EA:" + (AC_IF_avarage_OA_F[0] - AC_IF_avarage_EA_F[0]));
		
		
		double[] AC_SF_avarage_EA_F = average(retrievalData(AC_SF[index1], index2));
		double[] AC_SF_avarage_OA_F = average(retrievalData(AC_SF[index1 +1], index2));
		System.out.println("AC_SF_EA:" + (AC_SF_avarage_OA_F[0] - AC_SF_avarage_EA_F[0]));
		
	}
	
	/*private static void printIndividualAverage(double[][][] LA_IF, double[][][] LA_SF,
			double[][][] BO_IF, double[][][] BO_SF,
			double[][][] SC_IF, double[][][] SC_SF,
			double[][][] IN_IF, double[][][] IN_SF,
			double[][][] AC_IF, double[][][] AC_SF, int index1, int index2) {
		double[] LA_IF_avarage_EA_F = average(retrievalData(LA_IF[index1], index2));
		System.out.println("LA_IF_EA:" + LA_IF_avarage_EA_F[0] + "," + LA_IF_avarage_EA_F[1]);
		double[] LA_IF_avarage_OA_F = average(retrievalData(LA_IF[index1 +1], index2));
		System.out.println("LA_IF_EA:" + LA_IF_avarage_OA_F[0] + "," + LA_IF_avarage_OA_F[1]);
		
		double[] LA_SF_avarage_EA_F = average(retrievalData(LA_SF[index1], index2));
		System.out.println("LA_SF_EA:" + LA_SF_avarage_EA_F[0] + "," + LA_SF_avarage_EA_F[1]);
		double[] LA_SF_avarage_OA_F = average(retrievalData(LA_SF[index1 +1], index2));
		System.out.println("LA_SF_EA:" + LA_SF_avarage_OA_F[0] + "," + LA_SF_avarage_OA_F[1]);

		double[] BO_IF_avarage_EA_F = average(retrievalData(BO_IF[index1], index2));
		System.out.println("BO_IF_EA:" + BO_IF_avarage_EA_F[0] + "," + BO_IF_avarage_EA_F[1]);
		double[] BO_IF_avarage_OA_F = average(retrievalData(BO_IF[index1 +1], index2));
		System.out.println("BO_IF_EA:" + BO_IF_avarage_OA_F[0] + "," + BO_IF_avarage_OA_F[1]);
		
		double[] BO_SF_avarage_EA_F = average(retrievalData(BO_SF[index1], index2));
		System.out.println("BO_SF_EA:" + BO_SF_avarage_EA_F[0] + "," + BO_SF_avarage_EA_F[1]);
		double[] BO_SF_avarage_OA_F = average(retrievalData(BO_SF[index1 +1], index2));
		System.out.println("BO_SF_EA:" + BO_SF_avarage_OA_F[0] + "," + BO_SF_avarage_OA_F[1]);

		double[] SC_IF_avarage_EA_F = average(retrievalData(SC_IF[index1], index2));
		System.out.println("SC_IF_EA:" + SC_IF_avarage_EA_F[0] + "," + SC_IF_avarage_EA_F[1]);
		double[] SC_IF_avarage_OA_F = average(retrievalData(SC_IF[index1 +1], index2));
		System.out.println("SC_IF_EA:" + SC_IF_avarage_OA_F[0] + "," + SC_IF_avarage_OA_F[1]);

		double[] SC_SF_avarage_EA_F = average(retrievalData(SC_SF[index1], index2));
		System.out.println("SC_SF_EA:" + SC_SF_avarage_EA_F[0] + "," + SC_SF_avarage_EA_F[1]);
		double[] SC_SF_avarage_OA_F = average(retrievalData(SC_SF[index1 +1], index2));
		System.out.println("SC_SF_EA:" + SC_SF_avarage_OA_F[0] + "," + SC_SF_avarage_OA_F[1]);
		
		double[] IN_IF_avarage_EA_F = average(retrievalData(IN_IF[index1], index2));
		System.out.println("IN_IF_EA:" + IN_IF_avarage_EA_F[0] + "," + IN_IF_avarage_EA_F[1]);
		double[] IN_IF_avarage_OA_F = average(retrievalData(IN_IF[index1 +1], index2));
		System.out.println("IN_IF_EA:" + IN_IF_avarage_OA_F[0] + "," + IN_IF_avarage_OA_F[1]);
		
		double[] IN_SF_avarage_EA_F = average(retrievalData(IN_SF[index1], index2));
		System.out.println("IN_SF_EA:" + IN_SF_avarage_EA_F[0] + "," + IN_SF_avarage_EA_F[1]);
		double[] IN_SF_avarage_OA_F = average(retrievalData(IN_SF[index1 +1], index2));
		System.out.println("IN_SF_EA:" + IN_SF_avarage_OA_F[0] + "," + IN_SF_avarage_OA_F[1]);

		double[] AC_IF_avarage_EA_F = average(retrievalData(AC_IF[index1], index2));
		System.out.println("AC_IF_EA:" + AC_IF_avarage_EA_F[0] + "," + AC_IF_avarage_EA_F[1]);
		double[] AC_IF_avarage_OA_F = average(retrievalData(AC_IF[index1 +1], index2));
		System.out.println("AC_IF_EA:" + AC_IF_avarage_OA_F[0] + "," + AC_IF_avarage_OA_F[1]);
		
		double[] AC_SF_avarage_EA_F = average(retrievalData(AC_SF[index1], index2));
		System.out.println("AC_SF_EA:" + AC_SF_avarage_EA_F[0] + "," + AC_SF_avarage_EA_F[1]);
		double[] AC_SF_avarage_OA_F = average(retrievalData(AC_SF[index1 +1], index2));
		System.out.println("AC_SF_EA:" + AC_SF_avarage_OA_F[0] + "," + AC_SF_avarage_OA_F[1]);

	}*/	
	
	private static double[][] retrievalData(double[][] ds, int index) {
        double[][] result = new double[ds.length][2];
        for(int i=0;i<ds.length;i++) {
        	result[i][0] = ds[i][0];
        	result[i][1] = ds[i][index]; 
        }  
		
		return result;
	}

	private static PairedData extractPairedData(double[][] d1,
			double[][] d2, double[][] d3,
			double[][] d4) {

		int num = d1.length+d2.length;
		double[] x = new double[num];
		double[] y = new double[num];
		
		for(int i=0;i<d1.length;i++) {
			x[i] = d1[i][1];
			y[i] = d3[i][1];
		}
		
		for(int i=0;i<d2.length;i++) {
			x[d1.length+i] = d2[i][1];
			y[d1.length+i] = d4[i][1];
		}
		
		return new PairedData(x, y);

	}

	private static PairedData extractPairedData(double[][] d1, double[][] d2) {
		double[] x = new double[d1.length];
		double[] y = new double[d1.length];
		
		for(int i=0;i<x.length;i++) {
			x[i] = d1[i][1];
			y[i] = d2[i][1];
		}
		
		return new PairedData(x, y);
	}
	
	public static void sensitivityTest(String[] filenames) {
		double[] coverage = new double[filenames.length];

		int index = 0;
		for(String fn: filenames) {			
			double[][] data = extractData(fn);
			if(data!=null) {			
				coverage[index] = data[0][1];
				index++;
			}			
		}
		
		double sum = 0;
		for(int i=0;i<index;i++) 
		   sum += coverage[i];
		
		double mean =sum/index;
		
		double sd=0;
		for(int i=0;i<index;i++)
			sd += (mean-coverage[i]) * (mean-coverage[i]);
		
		sd = sd/(index -1);
		sd = Math.sqrt(sd);
		
		System.out.println("Mean:" + mean + ", SD:" + sd);		
	}
	
	public static void estimateConfidenceInterval(PairedData pd, Rengine re) {
		double[] x = pd.getX();
		double[] y = pd.getY();
		
		double[] diff = new double[x.length];
		for(int i=0;i<x.length;i++)
			diff[i] = x[i] - y[i];
		
		//manipulate the diff array
		int n = diff.length;
		int m = (n * (n+1))/2;
		
		double[] nda = new double[m];
		int ndaIndex=0;
		for(int i=0;i<n;i++)
			for(int j=i;j<n;j++) {
				nda[ndaIndex] = (diff[i] + diff[j])/2;
				ndaIndex++;
			}
						
		Arrays.sort(nda);
		
		double median;
		
		//calculate median		
		int hn = m/2;
		if(m - hn*2 ==0) //oven
			median = (nda[hn-1] + nda[hn])/2;
		else
			median = nda[hn];
		
		System.out.println("Median:" + median);
		
		
        				
        //calculate confidence level for interval
		int lastK = 0;

/*		double lastlevel=0;
		for(int k=hn-1;k>=0;k--) {
			double level = 1 - 2* calculateLevel(k, n, re);
			if(level > 0.95) {
			  lastlevel = level;
			  lastK = k;
			  break;
			}
		}
*/		
	    lastK = quickSearchConfidencelevel(hn, n, re);
	    System.out.println("Interval: [" + nda[lastK] + ", " + nda[m-lastK-1] + "]");
		//System.out.println("Interval: [" + nda[lastK] + ", " + nda[m-lastK-1] + "], confidence level:" + lastlevel);
		
	}
	
	private static int quickSearchConfidencelevel(int hn, int n, Rengine re) {
		int start=0;
		int end=hn-1;
		int mid = hn-1;
		
		while(end-start>100) {
			mid = start + (end - start)/2;
			double level = 1 - 2* calculateLevel(mid, n, re);
			if(level >=0.95)
				start = mid;
			else
				end = mid;
		}
		
		
		for(int k=end;k>=start;k--) {
			double level = 1 - 2* calculateLevel(k, n, re);			
			if(level > 0.95) {
				System.out.println("confidence level:" + level);
			    return k;
			}
		}
		
		return 0;
	}
	
	private static double calculateLevel(int k, int n, Rengine re) {
		REXP rexp =	re.eval("psignrank(" + k + "," + n + ")");
		return rexp.asDouble();
	}

	private static double WSRHypothesisTesting(PairedData pd, H1 hypothesis) {   

        WilcoxonTest wt = new WilcoxonTest(pd, hypothesis, true);

        return wt.getSP();
    }

	
	private static void drawAverage(DataSetPlot[] dsp, String prefix, String ylabel, String[] titles) {				
		
		String filename = outdir + prefix+"_average.eps";									    	
	    plotAverage(filename, dsp, titles, ylabel);
		
	}
	
	private static void plotAverage(String filename, DataSetPlot[] dsp,
			String[] titles, String ylabel) {
		
		JavaPlot p = new JavaPlot();				

        PostscriptTerminal epsf = new PostscriptTerminal(filename);
        epsf.setColor(true);
        epsf.setEPS(true);  
        epsf.set("font", "'Arial, 20'");
                
        p.setTerminal(epsf);                                     
        
        p.getAxis("x").setLabel("Fault Type", "Arial", 32);        
        p.getAxis("y").setLabel(ylabel, "Arial", 32);
        p.set("bmargin", "5");
        p.set("lmargin", "10");
        p.set("key spacing", "3");
        p.set("xlabel", "offset 0, -1");
        p.set("ylabel", "offset 0, -1");
        p.set("xtics font", "'Arial, 32'");
        p.set("ytics font", "'Arial, 32'");
        p.set("title font", "'Arial, 32'");
        p.set("yrange", "[0: 1.2]");
        p.set("ytics", "0, 0.2, 1.2");

        p.set("xtics", "(\"Type 1\" 0, \"Type 2\" 1, \"Overall\" 2)");
        p.set("xrange", "[-0.5:2.7]");
        
        p.setKey(JavaPlot.Key.TOP_RIGHT);
        
        for(int i=0;i<dsp.length;i++) {           
        
          PlotStyle stl = dsp[i].getPlotStyle();
          
          stl.setStyle(Style.HISTOGRAMS); 
          stl.setLineType(i+1);
          stl.setLineWidth(5);           
          dsp[i].setTitle(titles[i]);
                              
          p.addPlot(dsp[i]);          
        }                          
        
        p.set("style", "histogram errorbars gap 2 lw 4");        
        p.set("style fill", "pattern 1");
        p.set("bars", "fullwidth");        
        p.plot(); 		
	}


	private static double[] average(double[][] IF_EA, double[][] SF_EA) {				
		
		double[] result = new double[2];
		double sum = 0;
		for(double[] item: IF_EA)
			sum += item[1];
		for(double[] item: SF_EA)
			sum += item[1];
		
		sum = sum/(IF_EA.length + SF_EA.length);
		result[0] = sum;
		
		double deviation = 0;
		for(double[] item: IF_EA)
			deviation += (item[1] - sum) * (item[1] - sum);
		
		for(double[] item: SF_EA)
			deviation += (item[1] - sum) * (item[1] - sum);
		
		deviation = deviation/(IF_EA.length + SF_EA.length -1);
		deviation = Math.sqrt(deviation);
		result[1] = deviation;
		
		return result;
	}

	/*
	 * result[0]: mean
	 * result[1]: deviation
	 */	

	private static double[] average(double[][] IF_EA) {
		
		double sum = 0;		
		for(int i=0;i<IF_EA.length;i++)
			sum+= IF_EA[i][1];
		
		sum = sum/IF_EA.length;
		double deviation = 0;
		for(int i=0;i<IF_EA.length;i++) {
			deviation += (sum - IF_EA[i][1]) * (sum - IF_EA[i][1]);
		}
		
		deviation = Math.sqrt(deviation/(IF_EA.length - 1));
		
		double[] result = new double[2];
		result[0] = sum;
		result[1] = deviation; 
		return result;
	}

	public static void plotDataLine(String filename, String ylabel, String yrange, String ytic, DataSetPlot[] ds, String[] keys, int[] linetype) {
        JavaPlot p = new JavaPlot();

        PostscriptTerminal epsf = new PostscriptTerminal(filename);
        epsf.setColor(true);
        epsf.setEPS(true);  
        epsf.set("font", "'Arial, 20'");
                
        p.setTerminal(epsf);             
                
        p.getAxis("x").setLabel("Fault Index", "Arial", 24);        
        p.getAxis("y").setLabel(ylabel, "Arial", 24);
        p.set("bmargin", "5");
        p.set("key spacing", "3");
        p.set("xlabel", "0, -1");
        p.set("xtics", "font 'Arial, 20'");
        p.set("ytics", "font 'Arial, 20'");
        p.set("title font", "Arial, 24");
        p.set("yrange", yrange);
        p.set("ytics", ytic);
        
        p.setKey(JavaPlot.Key.TOP_RIGHT);
        
        for(int i=0;i<ds.length;i++) {           
        
          PlotStyle stl = ds[i].getPlotStyle();
          
          //stl.setStyle(Style.LINESPOINTS);
          stl.setStyle(Style.IMPULSES);
          stl.setLineType(linetype[i]);
          stl.setPointType(linetype[i]);
          //stl.setLineType(2);
          //stl.setPointType(2);
          stl.setPointSize(1);
          stl.setLineWidth(5);
          
          ds[i].setTitle(keys[i]);
          p.addPlot(ds[i]);          
        }        

        p.plot(); 
    }	
	
	private static void plotDataPoints(String filename, DataSetPlot[] dsp,
			String[] titles, String ylabel, String yrange, String ytic) {
		
		JavaPlot p = new JavaPlot();

        PostscriptTerminal epsf = new PostscriptTerminal(filename);
        epsf.setColor(true);
        epsf.setEPS(true);  
        epsf.set("font", "'Arial, 20'");
                
        p.setTerminal(epsf);                                     
        
        p.getAxis("x").setLabel("Fault Index", "Arial", 24);        
        p.getAxis("y").setLabel(ylabel, "Arial", 24);
        p.set("bmargin", "5");
        p.set("key spacing", "3");
        p.set("xlabel", "0, -1");
        p.set("xtics", "font 'Arial, 20'");
        p.set("ytics", "font 'Arial, 20'");
        p.set("title font", "Arial, 24");
        p.set("yrange", yrange);
        p.set("ytics", ytic);
        
        p.setKey(JavaPlot.Key.TOP_RIGHT);
        
        for(int i=0;i<dsp.length;i++) {           
        
          PlotStyle stl = dsp[i].getPlotStyle();
          
          //stl.setStyle(Style.POINTS);
          stl.setStyle(Style.IMPULSES);
          stl.setPointType(i+1);
          stl.setPointSize(2);
          dsp[i].setTitle(titles[i]);          
          
          p.addPlot(dsp[i]);          
        }                          
        
        p.plot(); 		
	}	

	private static String[] getExpResultNames(int approach, String prefix, boolean inc) {
		ArrayList<String> filename = new ArrayList<String>();
		
		//results from cluster
/*		
		for(int i=1;i<=39;i++) {
			String name = prefix + i + "_" + approach + ".txt";
			filename.add(name);
		}
*/

		//results from SciNet
		String cg;
		if(inc) 
			cg = "IF";
		else
			cg = "SF";
		
		for(int i=1;i<=8;i++)
			for(int j=1;j<=16;j++) {
				int ID1 = j/10;
				int ID2 = j - ID1 * 10;
				String name = prefix + cg + i +"" + ID1 + "" + ID2 + "_" + approach + ".txt";
				filename.add(name);
			}
		
		int len = filename.size();
		String[] result = new String[len];
		result = filename.toArray(result);
		
		return result;
	}
	
	/*
	 * result[0] = index
	 * result[1] = coverage
	 * result[2] = fault detection rate
	 */
	private static double[][] extractData(String filename) {
		ArrayList<double[]> tmp = new ArrayList<double[]>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
			while(line!=null) {
				String[] conts = line.split("\\s+");
				if(conts.length>=3) {
					double[] data = new double[3];
					data[0] = new Integer(conts[0]);
					data[1] = new Double(conts[1]);
					data[2] = new Double(conts[2]);
					tmp.add(data);
				}
				
				line = reader.readLine();
			}
			
			reader.close();
			
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		double[][] result = null;
		int len = tmp.size();
		if(len>0) {
			result = new double[len][];
			result = tmp.toArray(result);
		}
		
		return result;
	}
	
	private static double[][] calculateAverage(String[] filenames, String path) {
		
		HashMap<Integer, double[]> buffer = new HashMap<Integer, double[]>();
		
		for(String fn: filenames) {
			String name = path + "/" + fn;
			double[][] data = extractData(name);
			if(data!=null) 
				for(double[] item: data) {
					if(item.length>=3) {
						int index = (int)item[0];
						double[] sum = buffer.get(index);
						if(sum==null) {
							sum = new double[4];
							sum[0] = index;
							sum[1] = 0;
							sum[2] = 0;
							sum[3] = 0; //count number
							buffer.put(index, sum);
						}
						
						sum[1] += item[1];
						sum[2] += item[2];
						sum[3]++;//count number
					}
				}			
		}
		
		double[][] result = null;
		Set<Integer> keys = buffer.keySet();
		int len = keys.size();
		if(len > 0) {
			result = new double[len][];			
			for(Integer fi: keys) {
				double[] di = buffer.get(fi);
				if(di[3]>0) {
					di[1] = di[1]/di[3];
					di[2] = di[2]/di[3];															
				}
				result[fi] = di;
			}
		}
		
		return result;
	}

}
