package seir;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

import gridsearch.Grid;
import gridsearch.GridSearch;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class SEIRParameterGridSearch {
	
	private static final double START_ALPHA = 5e-6;
	private static final double END_ALPHA = 1e-4;
	private static final double START_BETA = 0.05;
	private static final double END_BETA = 1.0;
	private static final double START_INCUBATION_PERIOD = 4.8;
	private static final double END_INCUBATION_PERIOD = 5.6;
	private static final double START_INFECTIOUS_PERIOD = 4.0;
	private static final double END_INFECTIOUS_PERIOD = 9.0;

	public SortedSet<Entry<ModelParameter, Double>> search(Table table,
			Predicate<ModelParameter> constraint, int maxSearchResult, int numSteps){
		final int rowCount = table.rowCount();
		//First row is for the initial conditions
		DateColumn dateColumn = table.dateColumn("Date");
		final LocalDate initDate =dateColumn.get(0);
		final LocalDate endDate =dateColumn.get(rowCount-1);
		System.out.printf("Processing data from %s to %s\n", initDate, endDate);
		final double initInfectiousCount = table.doubleColumn("Cumulative Infectious").get(0);
		final double initRecoveredCount = table.doubleColumn("Cumulative Recovered").get(0);
		final double initDeathCount = table.doubleColumn("Cumulative Death").get(0);
		//The rest of the table is for the observed data
		final double[] newDeaths = table.last(rowCount-1).intColumn("Deaths").asDoubleArray();
		final double initExposed = 2*initInfectiousCount;
				
		List<Double> alphas = arithematicSequences(START_ALPHA, END_ALPHA, numSteps);
		List<Double> betas = arithematicSequences(START_BETA, END_BETA, numSteps);
		//Incubation period is 5.2 days
		List<Double> epsilons = inverseSequences(START_INCUBATION_PERIOD, END_INCUBATION_PERIOD, 9);
		//Infectious period is 5 days
		List<Double> gammas = inverseSequences(START_INFECTIOUS_PERIOD, END_INFECTIOUS_PERIOD, 11);
		List<Double> exposedes = geometricSequences(initExposed, 10*initExposed, numSteps);
		List<Double> infectiouses = geometricSequences(initInfectiousCount, 10*initInfectiousCount, numSteps);
		List<Double> recoveredes = geometricSequences(initRecoveredCount, 10*initRecoveredCount, numSteps);
		List<Double> deaths = Arrays.asList(initDeathCount);

		Grid<ModelParameter> parameterGrid = consumer->{
			alphas.forEach(alpha->{
				betas.forEach(beta->{
					epsilons.forEach(epsilon->{
						gammas.forEach(gamma->{
							exposedes.forEach(exposed->{
								infectiouses.forEach(infectious->{
									recoveredes.forEach(recovered->{
										deaths.forEach(death->{
											var modelParameter =new ModelParameter(alpha, beta, epsilon, gamma, exposed, infectious, recovered, death);
											consumer.accept(modelParameter);										
										});
									});
								});
							});
						});
					});
				});
			});
		};
		ToDoubleFunction<ModelParameter> objectiveFunction = param->rmse(
				new SEIRModel(param).run(newDeaths.length).getNewDeath(), newDeaths);
		return new GridSearch<ModelParameter>().run(parameterGrid, constraint, objectiveFunction, maxSearchResult);
	}

	protected List<Double> arithematicSequences(double start, double end, int length){
		var step = (end-start)/(length-1);
		return DoubleStream.iterate(start, x->x + step).limit(length).boxed().collect(toList());
	}

	protected List<Double> inverseSequences(double start, double end, int length){
		var step = (end-start)/(length-1);
		return DoubleStream.iterate(start, x->x + step).limit(length).map(x->1/x).boxed().collect(toList());
	}

	
	protected List<Double> geometricSequences(double start, double end, int length){
		double commonRatio = Math.pow(10, Math.log10(end/start)/(length-1));
		return DoubleStream.iterate(start, x->x*commonRatio).limit(length).boxed().collect(toList());
	}

	protected double rmse(double[] values, double[] targets){
		if ( values.length != targets.length)
			throw new IllegalArgumentException();

		double sum = 0;
		for (int i = 0 ; i < values.length ; i++ ) {
			sum += (values[i] - targets[i])*(values[i] - targets[i]);
		}
		return Math.sqrt(sum/values.length);
	}

}
