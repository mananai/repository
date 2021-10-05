package seir;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import tablesaw.TablesawUtils;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class SEIRPlotUtils {
	public static void plot(ModelParameter[] arrayOfPrameters, double[] targets, int numDays) {

		double[][] modelOutputArray = new double[arrayOfPrameters.length+1][];
		String[] labels = new String[arrayOfPrameters.length+1];
		modelOutputArray[0] = Arrays.copyOf(targets, numDays);
		if (numDays > targets.length)
			Arrays.fill(modelOutputArray[0], targets.length, numDays, Double.NaN);
		
		labels[0] = "Data";
		for (int i = 0 ; i< arrayOfPrameters.length ; i++) {
			modelOutputArray[i+1] = new SEIRModel(arrayOfPrameters[i]).run(numDays).getNewDeath();
			labels[i+1] = "Model#" + (i+1);
		}
		double[] inputData = IntStream.iterate(1, x-> x + 1).limit(numDays).asDoubleStream().toArray();
		TablesawUtils.plot("SEIR Model vs Data", inputData, "Days", modelOutputArray, labels);
	}

	public static void plot(ModelParameter[] arrayOfPrameters, double[] targets, LocalDate startDate, int numDays, String yAxis) {

		double[][] modelOutputArray = new double[arrayOfPrameters.length+1][];
		String[] labels = new String[arrayOfPrameters.length+1];
		modelOutputArray[0] = Arrays.copyOf(targets, numDays);
		if (numDays > targets.length)
			Arrays.fill(modelOutputArray[0], targets.length, numDays, Double.NaN);
		
		labels[0] = "Data";
		for (int i = 0 ; i< arrayOfPrameters.length ; i++) {
			modelOutputArray[i+1] = new SEIRModel(arrayOfPrameters[i]).run(numDays).getNewDeath();
			labels[i+1] = String.format("#%d-%s", (i+1), arrayOfPrameters[i].shortString());
		}
		System.out.printf("labels=%s\n", Arrays.toString(labels));
		LocalDate[] inputData = IntStream.iterate(0, i->i+1).limit(numDays).mapToObj(i->startDate.plusDays(i)).toArray(LocalDate[]::new);
		TablesawUtils.plot("SEIR Model vs Data", yAxis, modelOutputArray, labels, "Days", inputData);
	}

	public static void plotDataVsModel( String title, String yAxis, BiFunction<ModelParameter, Integer, double[]> function,
			ModelParameter[] arrayOfPrameters, int numDays, double[] data, LocalDate startDate) {

		double[][] valuesArray = new double[arrayOfPrameters.length+1][];
		String[] labels = new String[arrayOfPrameters.length+1];
		valuesArray[0] = Arrays.copyOf(data, numDays);
		if (numDays > data.length)
			Arrays.fill(valuesArray[0], data.length, numDays, Double.NaN);
		
		labels[0] = "Data";
		for (int i = 0 ; i< arrayOfPrameters.length ; i++) {
			valuesArray[i+1] = function.apply(arrayOfPrameters[i], numDays);
			labels[i+1] = String.format("#%d: %s", (i+1), arrayOfPrameters[i].shortString());
		}
		LocalDate[] dates = IntStream.iterate(0, i->i+1).limit(numDays).mapToObj(i->startDate.plusDays(i)).toArray(LocalDate[]::new);
		TablesawUtils.plot(title, yAxis, valuesArray, labels, "Date", dates);
	}
}
