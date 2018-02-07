package Configuration;

// TODO: Auto-generated Javadoc
/**
 * The Class Config.
 */
public class Config {

	/** The my_config. */
	private static Config my_config = null;
	
	/**
	 * Instantiates a new config.
	 */
	private Config() {}
	
	/**
	 * Gets the config.
	 *
	 * @return my_config
	 */
	public static Config getConfig() {
		if(my_config == null) {
			my_config = new Config();
		}
		
		return my_config;
	}
	
	//configuration properties
	/** The debug model. */
	public boolean debugModel = false;
	//public boolean debugModel = true;
	
	/** The exposeevent. */
	public boolean exposeevent = true;
	
	//for generating test cases for service testing
	/** The variable domain. */
	public int variableDomain = 100;
	//public int variableDomain = 10;
	
	//this switches on/off the parsing requirements 
	public boolean parsingExpressionInLoading = true;
	
	//set the default maximum length of a path in path construction
	public int maxPathLength =  200;

    //set the mutation ratio, default = 1
    public int mutationRatio = 1;

}
