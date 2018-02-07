package ServiceTesting.Mutation;

import ServiceTesting.ServiceTesting;

public class MutationFiltering {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String[] parameters = {
				"1", //random
				"1", //activity coverage
				args[2], //maxTestCaseNum
				"1", //test_count
				"0.95", //coverage_threshold
				args[0], //testscript_filename
				"11",//appendix		
				args[1] //result directory
		};
		
		ServiceTesting.filterMutations(parameters);

	}

}
