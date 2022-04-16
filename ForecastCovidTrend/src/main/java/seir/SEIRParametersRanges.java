package seir;

public class SEIRParametersRanges {

	public static final double MIN_ALPHA = 1e-6;//5e-6;
	public static final double MAX_ALPHA = 1e-3;//1e-4;
	
	public static final double MIN_BETA = 1e-3;//0.05;
	public static final double MAX_BETA = 1.0;

	public static final double MIN_INCUBATION_PERIOD = 4.8;
	public static final double MAX_INCUBATION_PERIOD = 14; //5.6;

	public static final double MIN_INFECTIOUS_PERIOD = 4.0;
	public static final double MAX_INFECTIOUS_PERIOD = 14.0;
}
