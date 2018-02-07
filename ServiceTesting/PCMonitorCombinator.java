package ServiceTesting;

import java.util.ArrayList;

import ServiceTesting.Monitoring.CoverageMonitor;
import ServiceTesting.Monitoring.GlobalObservation;
import ServiceTesting.Monitoring.LocalObservation;
import ServiceTesting.Monitoring.Observation;
import ServiceTesting.Monitoring.ObservationUnit;
import ServiceTesting.Monitoring.EventSequenceObservationUnit;

public class PCMonitorCombinator {

	/**
	 * @param args
	 */
	
	private static String pcname = "tmp/PathCoverageMonitor_";
	private static String[] copies = {
		"1", "2", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"
	};
	
	public static void main(String[] args) {
		
		String appID = args[0];
		CoverageMonitor cb_monitor = new CoverageMonitor();
		Observation testingobservation = null;
		Observation globalObservation = new GlobalObservation();
		ServiceTesting.isBaseline = true;
		
		for(int i=1;i<=122;i++) {
			
		//for(int i=1;i<=8;i++)
			//for(int j=1;j<=16;j++) {
				
			 
		     //String filename = pcname + i + "" + j/10 + "" + (j - (j/10) * 10) + "" + appID; 
		//for(String cp: copies) {
			//String filename = pcname+appID + cp;
			
			String filename = pcname + "AU" + i; 
			
			CoverageMonitor monitor = new CoverageMonitor();
			ServiceTesting.loadPathCoverageMonitor(monitor, filename);
			
			if(testingobservation==null)
			    testingobservation = monitor.getTestingObservation();
			else {
				Observation t_lc = monitor.getTestingObservation();
				ArrayList<ObservationUnit> unitlist = t_lc.getObservationUnit();
				for(ObservationUnit unit: unitlist) {
					if(notInclude(testingobservation, unit)) {
						testingobservation.addObservationUnit(unit);
						String sn = ((EventSequenceObservationUnit)unit).getServiceName();
						((LocalObservation)testingobservation).addMapping(sn, unit);
					}
				}
			}
			
			Observation t_gb = monitor.getGlobalObservation();
			ArrayList<ObservationUnit> unitlist = t_gb.getObservationUnit();
			for(ObservationUnit unit: unitlist) {
				if(notInclude(globalObservation, unit)) 
					globalObservation.addObservationUnit(unit);
			}
			
			
		}
		
		cb_monitor.setTestingObservation(testingobservation);
		cb_monitor.setGlobalObservation(globalObservation);
		
		String dfn = pcname + appID;
		ServiceTesting.savePathCoverageMonitor(cb_monitor, dfn);

	}

	private static boolean notInclude(Observation globalObservation,
			ObservationUnit unit) {

		ArrayList<ObservationUnit> unitlist = globalObservation.getObservationUnit();
		for(ObservationUnit ut: unitlist)
			if(((EventSequenceObservationUnit)ut).isSame((EventSequenceObservationUnit)unit)) return false;
		
		return true;
	}

}
