package ServiceDebugging.FaultLocalization;

import java.io.Serializable;
import java.util.ArrayList;

public class FaultLocalizationSolution  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6674713950796672073L;
	public int ranktype; //0-12	
	public int exposePolicy; //EXPOSEALL vs. EXPOSEPORTONLY vs. NearestNeighbourLocal
	public int clusteringPolicy; //	NearestNeighbour vs. NearestNeighbourLocal
	
	public boolean needFinedEncapsulation = true; //only useful when exposePolicy = EXPOSEENCAPSULATION
    public boolean improvedEncapsulation = true;//only useful when exposePolicy = EXPOSEENCAPSULATION
	public int refinementPolicy; //only useful when exposePolicy = EXPOSEENCAPSULATION
	public boolean needCompositeEvent = true;
	public int clustercompositionPolicy;//only useful when needCompositeEvent = true
	public int penaltyType;	//only useful when needCompositeEvent = true
	public String name; //solution name
	
	
	public static final int NearestNeighbour = 100;
	public static final int NearestNeighbourLocal = 101;
	public static final int NearestNeighbourRelaxed = 102;
	public static final int FuzzyCluster = 103;
	public static final int FuzzyClusterLocal = 104;
	public static final int FuzzyClusterNew = 105;
	public static final int ConfidenceInterval = 106;
	public static final int NearestNeighbourDensity = 107;
	public static final int FuzzyClusterVectorGlobal = 108;
	public static final int NearestNeighbourMH = 109;
	public static final int NearestNeighbourMHFuzzy = 110;
	public static final int NearestNeighbourMHFuzzyIntegration = 111;

	public static final int EXPOSEALL = 1000;
	public static final int EXPOSEPORTONLY = 1001;
	public static final int EXPOSEENCAPSULATION = 1002;
	public static final int EXPOSEALLWITHSD = 1003;
	public static final int EXPOSEENCAPSULATIONNEW = 1004;
	public static final int EXPOSEENCAPSULATIONFOLD = 1005;
	
	public static final int PENALTYMINUS = 2000;
	public static final int PENALTYPERCANTAGE = 2001;	
	public static final int NOPENALTY = 2002;
	
	public static final int CLUSTERLOGICAND = 3000;
	public static final int CLUSTERJOINDIST = 3001;
	public static final int NOCLUSTER = 3002;
	
	public static final int COARSEREFINEMENT = 10;
	public static final int FINEDREFINEMENT = 11;
	public static final int HYBRIDREFINEMENT = 12;
	
	
	
	
	
	public static ArrayList<FaultLocalizationSolution> init() {
		ArrayList<FaultLocalizationSolution> solutions = new ArrayList<FaultLocalizationSolution>();
		
		//Solution 0: ranktype = 0, EXPOSEALL, needCompositeEvent=false, NearestNeighbour
		FaultLocalizationSolution solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEALL;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);
		
		//Solution 2: ranktype = 0, EXPOSEENCAPSULATION, needCompositeEvent=false, NearestNeighbour
		//          needFinedEncapsulation = false, refinementPolicy = FINEDREFINEMENT
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.needFinedEncapsulation = false;
		solution.refinementPolicy = FINEDREFINEMENT;
		
		solutions.add(solution);
		
		//Solution 4: ranktype = 0, EXPOSEENCAPSULATION, needCompositeEvent=false, NearestNeighbour
		//          needFinedEncapsulation = true, refinementPolicy = FINEDREFINEMENT
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.needFinedEncapsulation = true;
		solution.refinementPolicy = FINEDREFINEMENT;
		
		solutions.add(solution);		
		
		//Solution 5: ranktype = 0, EXPOSEENCAPSULATION, needCompositeEvent=false, NearestNeighbour
		//          needFinedEncapsulation = true, refinementPolicy = COARSEREFINEMENT
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.needFinedEncapsulation = true;
		solution.refinementPolicy = COARSEREFINEMENT;
		
		solutions.add(solution);				
		
		//Solution 6: ranktype = 0, EXPOSEENCAPSULATION, needCompositeEvent=false, NearestNeighbour
		//          needFinedEncapsulation = true, refinementPolicy = HYBRIDREFINEMENT
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.needFinedEncapsulation = true;
		solution.refinementPolicy = HYBRIDREFINEMENT;
		
		solutions.add(solution);				

		//5-9
		//repeat the aforementioned solutions with clusteringPolicy = NearestNeighbourLocal
		ArrayList<FaultLocalizationSolution> buffer = new ArrayList<FaultLocalizationSolution>();
		for(FaultLocalizationSolution sol: solutions) {
			solution = copy(sol);
			solution.clusteringPolicy = NearestNeighbourLocal;
			buffer.add(solution);
		}
		solutions.addAll(buffer);
		
		//10-19
		//repeat the aforementioned solutions with needCompositeEvent = true, penaltyType = PENALTYMINUS
		//clustercompositionPolicy = CLUSTERLOGICAND
		buffer.clear();
		for(FaultLocalizationSolution sol: solutions) {
			solution = copy(sol);
			solution.needCompositeEvent = true;
			solution.penaltyType = PENALTYMINUS;
			solution.clustercompositionPolicy = CLUSTERLOGICAND;
			buffer.add(solution);
		}
		//20-29
		//repeat solutions 0-9 with needCompositeEvent = true, penaltyType = PENALTYMINUS
		//clustercompositionPolicy = CLUSTERJOINDIST
		for(FaultLocalizationSolution sol: solutions) {
			solution = copy(sol);
			solution.needCompositeEvent = true;
			solution.penaltyType = PENALTYPERCANTAGE;
			solution.clustercompositionPolicy = CLUSTERJOINDIST;
			buffer.add(solution);
		}
		solutions.addAll(buffer);
		
		//30-49
		//repeat solutions 10-29 with penaltyType = PENALTYPERCANTAGE
		ArrayList<FaultLocalizationSolution> newbuffer = new ArrayList<FaultLocalizationSolution>();
		for(FaultLocalizationSolution sol: buffer) {
			solution = copy(sol);
			solution.penaltyType = PENALTYPERCANTAGE;
			newbuffer.add(solution);
		}		
		solutions.addAll(newbuffer);
		
		//50-69
		//repeat solutions 30-49 with ranktype = 5
		for(FaultLocalizationSolution sol: newbuffer) {
			solution = copy(sol);
			solution.ranktype = 5;
			solutions.add(solution);
		}		
		
		//70
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = NOCLUSTER
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clustercompositionPolicy = NOCLUSTER;
		solutions.add(solution);
		
		return solutions;
	}
	
	private static FaultLocalizationSolution copy(FaultLocalizationSolution solution) {
		FaultLocalizationSolution result = new FaultLocalizationSolution();
		result.ranktype = solution.ranktype;
		result.exposePolicy = solution.exposePolicy;
		result.needCompositeEvent = solution.needCompositeEvent;
		result.clusteringPolicy = solution.clusteringPolicy;
		result.clustercompositionPolicy = solution.clustercompositionPolicy;
		result.needFinedEncapsulation = solution.needFinedEncapsulation;
		result.refinementPolicy = solution.refinementPolicy;
		result.penaltyType = solution.penaltyType;		
		
		return result;
	}
	
	public static ArrayList<FaultLocalizationSolution> initGaltonCompositeEvents() {
		ArrayList<FaultLocalizationSolution> solutions = new ArrayList<FaultLocalizationSolution>();
		
		FaultLocalizationSolution solution;
		
		//ranktype=0, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = NOCLUSTER
		//Statistical debugging
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALLWITHSD;
		solution.ranktype = 0;
		solution.needCompositeEvent = false;
		solution.clustercompositionPolicy = NOCLUSTER;
		solution.name="Statistical debugging without composition";
				
		solutions.add(solution);

		
		//ranktype=0, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = NOCLUSTER
		//Statistical debugging
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALLWITHSD;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clustercompositionPolicy = NOCLUSTER;
		solution.name="Statistical debugging with composition";
				
		solutions.add(solution);
		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = NOCLUSTER
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clustercompositionPolicy = NOCLUSTER;
		solution.name = "expose all, Galton without cluster, no composition";
		
		solutions.add(solution);
/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);

		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);				
*/		
/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);				

/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = ConfidenceInterval;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);				
		

		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbourDensity;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);				
*/				
/*
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyCluster;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterNew;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);
*/
/*
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterLocal;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterLocal;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);			
*/		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.name = "expose all, Galton with cluster, no composition";
		
		solutions.add(solution);					

		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.name = "expose all, Galton with cluster, with composition";
		
		solutions.add(solution);	
		
		
		//ranktype=5, EXPOSEPORTONLY, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEPORTONLY;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.name = "expose port only, Galton with cluster, with composition";
		
		solutions.add(solution);	
	
		/*
		//ranktype=5, EXPOSEENCAPSULATION, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMH;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.needFinedEncapsulation = false;
		solution.refinementPolicy = FINEDREFINEMENT;
		solution.name = "Galton with cluster, with composition, encapsulation, coarse-refinement";
		
		solutions.add(solution);
		//it turns out this solution works poor than the one with solution.needFinedEncapsulation = true;
		//2015.12.29	
		*/			

		//ranktype=5, EXPOSEENCAPSULATION, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.needFinedEncapsulation = true;
		solution.improvedEncapsulation = false;
		solution.refinementPolicy = FINEDREFINEMENT;
		solution.name = "Galton with cluster, with composition, fine-encapsulation, fine-refinement";
		
		solutions.add(solution);				

		//ranktype=5, EXPOSEENCAPSULATION, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.needFinedEncapsulation = true;
		solution.improvedEncapsulation = true;
		solution.refinementPolicy = FINEDREFINEMENT;
		solution.name = "Galton with cluster, with composition, fine and improved encapsulation, fine-refinement";
		
		solutions.add(solution);						
		
		//ranktype=5, EXPOSEENCAPSULATION, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEENCAPSULATIONNEW;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.refinementPolicy = FINEDREFINEMENT;
		solution.name = "Galton with cluster, with composition, block-based encapsulation, fine-refinement";
		
		solutions.add(solution);						
		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbourMHFuzzyIntegration;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.name = "expose all, Galton with cluster, no composition";
		
		solutions.add(solution);					

		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzyIntegration;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.name = "expose all, Galton with cluster, with composition";
		
		solutions.add(solution);	
			
		//ranktype=5, EXPOSEENCAPSULATION, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEENCAPSULATIONFOLD;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solution.refinementPolicy = FINEDREFINEMENT;
		solution.name = "Galton with cluster, with composition, block-based encapsulation, fine-refinement";
		
		solutions.add(solution);			
		
		/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMH;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);			
		
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);
		
		
		//5-9
		//the following is for refinement and encapsulation
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.needFinedEncapsulation = true;
		solution.refinementPolicy = FINEDREFINEMENT;
		
		solutions.add(solution);		
		
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.needFinedEncapsulation = true;
		solution.refinementPolicy = COARSEREFINEMENT;
		
		solutions.add(solution);				
		
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.needFinedEncapsulation = true;
		solution.refinementPolicy = HYBRIDREFINEMENT;
		
		solutions.add(solution);						
		
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.needFinedEncapsulation = false;
		solution.refinementPolicy = FINEDREFINEMENT;
		
		solutions.add(solution);		
		
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.needFinedEncapsulation = false;
		solution.refinementPolicy = COARSEREFINEMENT;
		
		solutions.add(solution);				
		
		solution = new FaultLocalizationSolution();
		solution.ranktype = 0;
		solution.exposePolicy = EXPOSEENCAPSULATION;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourMHFuzzy;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.needFinedEncapsulation = false;
		solution.refinementPolicy = HYBRIDREFINEMENT;
		
		solutions.add(solution);								
		
		
/*		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);			
/*
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterVectorGlobal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);					
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = FuzzyClusterVectorGlobal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);					
				
/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
				
		solutions.add(solution);		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
				
		solutions.add(solution);				
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
				
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = ConfidenceInterval;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
				
		solutions.add(solution);	
		
		
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourDensity;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);				
		
		
/*
		//ranktype=0, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		
		solutions.add(solution);
*/
		return solutions;
	}
	
	public static ArrayList<FaultLocalizationSolution> initGaltonAllEvents() {
		ArrayList<FaultLocalizationSolution> solutions = new ArrayList<FaultLocalizationSolution>();
		
		FaultLocalizationSolution solution;

		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = NOCLUSTER
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clustercompositionPolicy = NOCLUSTER;
		
		solutions.add(solution);
		
		

/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);
*/
		
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);
/*
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);
*/		
		
		//ranktype=0, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);

		//ranktype=0, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = false;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		
		solutions.add(solution);
		
		
		//ranktype=0, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = PENALTYMINUS;
		
		solutions.add(solution);

		//ranktype=0, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 0;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = PENALTYMINUS;
		
		solutions.add(solution);
		
/*		
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = PENALTYPERCANTAGE;
				
		solutions.add(solution);

		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERJOINDIST
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = PENALTYPERCANTAGE;
				
		solutions.add(solution);				
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERLOGICAND
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = PENALTYPERCANTAGE;
		
		solutions.add(solution);		

		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clustercompositionPolicy = CLUSTERJOINDIST
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourRelaxed;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = PENALTYPERCANTAGE;
		
		solutions.add(solution);
*/		

		return solutions;
	}
	
	public static ArrayList<FaultLocalizationSolution> initGalton() {
		ArrayList<FaultLocalizationSolution> solutions = new ArrayList<FaultLocalizationSolution>();
		
		FaultLocalizationSolution solution;
		//ranktype=5, EXPOSEALL, needCompositeEvent=false, clustercompositionPolicy = NOCLUSTER
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = false;
		solution.clustercompositionPolicy = NOCLUSTER;
		solutions.add(solution);

		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbour, 
		//clustercompositionPolicy = CLUSTERLOGICAND, penaltyType = NOPENALTY
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = NOPENALTY;
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbour, 
		//clustercompositionPolicy = CLUSTERJOINDIST, penaltyType = NOPENALTY
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solutions.add(solution);		

		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbourLocal, 
		//clustercompositionPolicy = CLUSTERLOGICAND, penaltyType = NOPENALTY
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = NOPENALTY;
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbourLocal, 
		//clustercompositionPolicy = CLUSTERJOINDIST, penaltyType = NOPENALTY
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = NOPENALTY;
		solutions.add(solution);				

		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbour, 
		//clustercompositionPolicy = CLUSTERLOGICAND, penaltyType = PENALTYPERCANTAGE
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = PENALTYPERCANTAGE;
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbour, 
		//clustercompositionPolicy = CLUSTERJOINDIST, penaltyType = PENALTYPERCANTAGE
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbour;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = PENALTYPERCANTAGE;
		solutions.add(solution);		

		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbourLocal, 
		//clustercompositionPolicy = CLUSTERLOGICAND, penaltyType = PENALTYPERCANTAGE
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERLOGICAND;
		solution.penaltyType = PENALTYPERCANTAGE;
		solutions.add(solution);
		
		//ranktype=5, EXPOSEALL, needCompositeEvent=true, clusteringPolicy = NearestNeighbourLocal, 
		//clustercompositionPolicy = CLUSTERJOINDIST, penaltyType = PENALTYPERCANTAGE
		solution = new FaultLocalizationSolution();
		solution.exposePolicy = EXPOSEALL;
		solution.ranktype = 5;
		solution.needCompositeEvent = true;
		solution.clusteringPolicy = NearestNeighbourLocal;
		solution.clustercompositionPolicy = CLUSTERJOINDIST;
		solution.penaltyType = PENALTYPERCANTAGE;
		solutions.add(solution);
		
		return solutions;
	}
}
