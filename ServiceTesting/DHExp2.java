package ServiceTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.PostscriptTerminal;

public class DHExp2 {
	
	public static void main(String[] args) {
		DHExp2 exp = new DHExp2();
		exp.path = args[0];
		exp.outdir = args[1];
		exp.handleData();
	}
	
	private void handleData() {
		handleData("LA", 3, 1.0);
		handleData("LA", 4, 1.9);
		
		handleData("BO", 3, 1.0);
		handleData("BO", 4, 33.0/17);
		
		handleData("SC", 3, 1.0);
		handleData("SC", 4, 1.0);
		
		handleData(3, 1.0, 1.0, 1.0, "[0.2:1.1]", "0.2,0.2,1");
		handleData(4, 1.9, 33.0/17, 1.0, "[0:1.1]","0,0.2,1");
	}
	
	private void handleData(int coverage, double fc_LA, double fc_BO, double fc_SC, String yrange, String ytic) {
		String appdix = "";
		
		//random (LA)
		String prefix = "LA";
		appdix = "_"+coverage + "_1_100.txt";
		double[][] rd_LA = extractData(prefix, appdix, fc_LA);
		
		//constraint
		appdix = "_"+coverage + "_2_100.txt";
		double[][] ct_LA = extractData(prefix, appdix, fc_LA);
		
		//hybrid 1
		appdix = "_" + coverage + "_3_75.txt";
		double[][] hb1_LA = extractData(prefix, appdix, fc_LA);
		
		//hybrid 2
		appdix = "_" + coverage + "_3_50.txt";
		double[][] hb2_LA = extractData(prefix, appdix, fc_LA);
		
		//random (BO)
		prefix = "BO";
		appdix = "_"+coverage + "_1_100.txt";
		double[][] rd_BO = extractData(prefix, appdix, fc_BO);
		
		//constraint
		appdix = "_"+coverage + "_2_100.txt";
		double[][] ct_BO = extractData(prefix, appdix, fc_BO);
		
		//hybrid 1
		appdix = "_" + coverage + "_3_75.txt";
		double[][] hb1_BO = extractData(prefix, appdix, fc_BO);
		
		//hybrid 2
		appdix = "_" + coverage + "_3_50.txt";
		double[][] hb2_BO = extractData(prefix, appdix, fc_BO);
		
		//random (SC)
		prefix = "SC";
		appdix = "_"+coverage + "_1_100.txt";
		double[][] rd_SC = extractData(prefix, appdix, fc_SC);
		
		//constraint
		appdix = "_"+coverage + "_2_100.txt";
		double[][] ct_SC = extractData(prefix, appdix, fc_SC);
		
		//hybrid 1
		appdix = "_" + coverage + "_3_75.txt";
		double[][] hb1_SC = extractData(prefix, appdix, fc_SC);
		
		//hybrid 2
		appdix = "_" + coverage + "_3_50.txt";
		double[][] hb2_SC = extractData(prefix, appdix, fc_SC);
		
		//String[] titles = {"Random", "Constraint", "Hybrid1", "Hybrid2"};
		String[] titles = {"RT", "CB", "HS1", "HS2"};
		String yLabel = "Coverage Percentage";
		
		double[][] rd = calculateAverage(rd_LA, rd_BO, rd_SC);
		double[][] ct = calculateAverage(ct_LA, ct_BO, ct_SC);
		double[][] hb1 = calculateAverage(hb1_LA, hb1_BO, hb1_SC);
		double[][] hb2 = calculateAverage(hb2_LA, hb2_BO, hb2_SC);
		
		String ff = "average_" + coverage;
		outputAverageData(ff, rd, ct, hb1, hb2);

		//draw coverage vs. TN
		prefix = "Average";
		plotData(rd, ct, hb1, hb2, 1, titles, yLabel, prefix, coverage, yrange, ytic);
		
		yLabel = "Time (s)";
		//draw time vs.TN
		plotData(rd, ct, hb1, hb2, 2, titles, yLabel, prefix, coverage, null, null);
		
		yLabel = "Information Leakage";
		//draw information leakage vs. TN
		plotData(rd, ct, hb1, hb2, 3, titles, yLabel, prefix, coverage, null, null);
		
		yLabel = "Time (s)";
		//draw information leakage vs. TN
		plotData(rd, ct, hb1, hb2, 4, titles, yLabel, prefix, coverage, null, null);
	}
	
	private double[][] calculateAverage(double[][] rd_LA, double[][] rd_BO,
			double[][] rd_SC) {
		double[][] result = new double[rd_LA.length][rd_LA[0].length];
		for(int i=0;i<result.length;i++) {
			for(int j=0;j<result[i].length;j++) {
				result[i][j] = (rd_LA[i][j] + rd_BO[i][j] + rd_SC[i][j])/3;
			}
		}
		
		return result;
	}

	private void handleData(String prefix, int coverage, double factor) {
		String appdix = "";
		
		//random
		appdix = "_"+coverage + "_1_100.txt";
		double[][] rd = extractData(prefix, appdix, factor);
		
		//constraint
		appdix = "_"+coverage + "_2_100.txt";
		double[][] ct = extractData(prefix, appdix, factor);
		
		//hybrid 1
		appdix = "_" + coverage + "_3_75.txt";
		double[][] hb1 = extractData(prefix, appdix, factor);
		
		//hybrid 2
		appdix = "_" + coverage + "_3_50.txt";
		double[][] hb2 = extractData(prefix, appdix, factor);
		
		String ff = prefix + coverage;
		outputAverageData(ff, rd, ct, hb1, hb2);
		
		//String[] titles = {"Random", "Constraint", "Hybrid1", "Hybrid2"};
		String[] titles = {"RT", "CB", "HS1", "HS2"};
		String yLabel = "Coverage";
		//draw coverage vs. TN
		plotData(rd, ct, hb1, hb2, 1, titles, yLabel, prefix, coverage, "[0:1.1]", "0.2:0.2:1");
		
		yLabel = "Time (s)";
		//draw time vs.TN
		plotData(rd, ct, hb1, hb2, 2, titles, yLabel, prefix, coverage, null, null);
		
		yLabel = "Information Leakage";
		//draw information leakage vs. TN
		plotData(rd, ct, hb1, hb2, 3, titles, yLabel, prefix, coverage, null, null);
		
		yLabel = "Time (s)";
		//draw time vs.TN
		plotData(rd, ct, hb1, hb2, 4, titles, yLabel, prefix, coverage, null, null);
	}

	private void outputAverageData(String ff, double[][] rd, double[][] ct,
			double[][] hb1, double[][] hb2) {
		String filename= outdir + "/" + ff + "_Coverage.txt";
		outputAverageData(filename, rd, ct, hb1, hb2, 1);
		
		filename = outdir + "/" + ff + "_Time.txt";
		outputAverageData(filename, rd, ct, hb1, hb2, 2);
		
		filename = outdir + "/" + ff + "_InformationLeakage.txt";
		outputAverageData(filename, rd, ct, hb1, hb2, 3);
		
		filename = outdir + "/" + ff + "_GT.txt";
		outputAverageData(filename, rd, ct, hb1, hb2, 4);
		
	}

	private void outputAverageData(String filename, double[][] rd,
			double[][] ct, double[][] hb1, double[][] hb2, int index) {
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for(int i=0;i<rd.length;i++) {
				String line = formatDouble(rd[i][0]) + formatDouble(rd[i][index]) +
				              formatDouble(ct[i][index]) + formatDouble(hb1[i][index]) +
				              formatDouble(hb2[i][index]);
				
				writer.write(line);
				writer.newLine();
			}
			
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static String formatDouble(double coverage) {
		if(Math.abs(coverage)<0.001) 
			return formatString("0");
		
		String value = ""+coverage;
		int length = value.length()>6? 6: value.length();
		value = value.substring(0, length);
		return formatString(value);
	}
	
	private static String formatString(String value) {
		String result = value;
		for(int i=value.length();i<10;i++)
			result += " ";
		
		return result;
	}

	private String outdir="";
	private String[] cg = {
		"",
		"Coverage",
		"Time",
		"InformationLeakage",
		"GTime"
	};
	
	private void plotData(double[][] rd, double[][] ct, double[][] hb1,
			double[][] hb2, int i, String[] titles, String yLabel, String prefix, int coverage, String yrange, String ytic) {		
		
		DataSetPlot[] dsp = new DataSetPlot[4];
		dsp[0] = constructDSP(rd, i);
		dsp[1] = constructDSP(ct, i);
		dsp[2] = constructDSP(hb1, i);
		dsp[3] = constructDSP(hb2, i);
		
		String filename = outdir + "/" + prefix + coverage + "_" + cg[i] + ".eps";
		plotDataLine(filename, yLabel, dsp, titles, yrange, ytic);
	}
	
	public static void plotDataLine(String filename, String ylabel, DataSetPlot[] ds, String[] keys, String yrange, String ytic) {
        JavaPlot p = new JavaPlot();

        PostscriptTerminal epsf = new PostscriptTerminal(filename);
        epsf.setColor(true);
        epsf.setEPS(true);  
        epsf.set("font", "'Arial, 20'");
                
        p.setTerminal(epsf);             
                
        p.getAxis("x").setLabel("Number of Test Cases", "Arial", 24);        
        p.getAxis("y").setLabel(ylabel, "Arial", 24);
        p.set("bmargin", "5");
        p.set("key spacing", "3");
        p.set("xlabel", "0, -1");
        p.set("xtics", "font 'Arial, 20'");
        p.set("ytics", "font 'Arial, 20'");
        p.set("title font", "Arial, 24");
        
        if(yrange!=null)
           p.set("yrange", yrange);
        
        if(ytic!=null) p.set("ytics", ytic);
        
        p.setKey(JavaPlot.Key.BOTTOM_RIGHT);
        
        for(int i=0;i<ds.length;i++) {           
        
          PlotStyle stl = ds[i].getPlotStyle();
          
          //stl.setStyle(Style.LINESPOINTS);
          stl.setStyle(Style.LINES);
          stl.setLineType(i+1);
          //stl.setPointType(linetype[i]);
          //stl.setLineType(2);
          //stl.setPointType(2);
          //stl.setPointSize(1);
          stl.setLineWidth(7);
          
          ds[i].setTitle(keys[i]);
          p.addPlot(ds[i]);          
        }        

        p.plot(); 
    }	

	private DataSetPlot constructDSP(double[][] rd, int i) {

		double[][] buffer = new double[rd.length][2];
		for(int index=0;index<buffer.length;index++) {
			buffer[index][0] = rd[index][0];
			buffer[index][1] = rd[index][i];
		}
		return new DataSetPlot(buffer);
	}

	private String path="";
	private double[][] extractData(String prefix, String appdix, double factor) {
		double[][] result = new double[200][6];
		for(int i=0;i<result.length;i++) {
			result[i][0] = i+1;
			result[i][1] = 0;
			result[i][2] = 0;
			result[i][3] = 0;
			result[i][4] = 0;
			result[i][5] = 0;
		}
		
		for(int i=1;i<=8;i++) {
			for(int j=1;j<=32;j++) {
				int ID1 = j /10;
				int ID2 = j - ID1 * 10;
				String filename = path + "/" + prefix + i + "" + ID1 + "" + ID2 + appdix;
				double[][] buffer = extractSingleFile(filename);
				if(buffer==null) continue;
				for(int index=0;index<buffer.length;index++) {
					if(buffer[index][0] == -1) break;
					result[index][1] += buffer[index][1];
					result[index][2] += buffer[index][2];
					result[index][3] += buffer[index][3];
					result[index][4] += buffer[index][4];
					result[index][5]++;
				}
			}				
		}
		
		for(int i=1;i<=120;i++) {			
			String filename = path + "/" + prefix + i + appdix;
			double[][] buffer = extractSingleFile(filename);
			if(buffer==null) continue;
			for(int index=0;index<buffer.length;index++) {				
				if(buffer[index][0] == -1) break;
				
				result[index][1] += buffer[index][1];
				result[index][2] += buffer[index][2];
				result[index][3] += buffer[index][3];
				result[index][4] += buffer[index][4];
				result[index][5]++;
			}				
		}
		
		for(int i=0;i<result.length;i++) {
			result[i][1] = result[i][1]/result[i][5] * factor; //adjust the coverage to correct one
			result[i][2] = result[i][2]/(1000* result[i][5]); //ms => s   
			result[i][3] = result[i][3]/result[i][5];
			result[i][4] = result[i][4]/result[i][5];
		}
		
		return result;
	}
	
	private double[][] extractSingleFile(String filename) {
		double[][] result = new double[200][5];
		for(double[] item: result) 
			item[0] = -1;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
			int index = 0;
			while(line!=null && index<200) {
				String[] cts = line.split("\\s+");
				if(cts!=null && cts.length>=5) {					
					result[index][0] = new Double(cts[0]);
					result[index][1] = new Double(cts[1]);
					result[index][2] = new Double(cts[2]);
					result[index][3] = new Double(cts[3]);
					result[index][4] = new Double(cts[4]);
				}
				index++;
				
				line = reader.readLine();
			}
			
			reader.close();
		} catch(Exception e) {
			//e.printStackTrace();
			return null;
		}
		
		return result;
	}
}


