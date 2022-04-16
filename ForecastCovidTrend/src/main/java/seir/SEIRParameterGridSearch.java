package seir;

import static java.util.stream.Collectors.toList;

import static seir.SEIRParametersRanges.MIN_ALPHA;
import static seir.SEIRParametersRanges.MAX_ALPHA;
import static seir.SEIRParametersRanges.MIN_BETA;
import static seir.SEIRParametersRanges.MAX_BETA;
import static seir.SEIRParametersRanges.MIN_INCUBATION_PERIOD;
import static seir.SEIRParametersRanges.MAX_INCUBATION_PERIOD;
import static seir.SEIRParametersRanges.MIN_INFECTIOUS_PERIOD;
import static seir.SEIRParametersRanges.MAX_INFECTIOUS_PERIOD;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
//import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.tuple.Pair;


import gridsearch.Grid;
import gridsearch.GridSearch;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
@Deprecated
public class SEIRParameterGridSearch {
	
	public SortedSet<Pair<ModelParameter, Double>> search(Table table,
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
				
		List<Double> alphas = arithematicSequences(MIN_ALPHA, MAX_ALPHA, numSteps);
		List<Double> betas = arithematicSequences(MIN_BETA, MAX_BETA, numSteps);
		//Incubation period is 5.2 days
		List<Double> epsilons = inverseSequences(MIN_INCUBATION_PERIOD, MAX_INCUBATION_PERIOD, 9);
		//Infectious period is 5 days
		List<Double> gammas = inverseSequences(MIN_INFECTIOUS_PERIOD, MAX_INFECTIOUS_PERIOD, 11);
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

	public static double rmse(double[] values, double[] targets){
		if ( values.length != targets.length)
			throw new IllegalArgumentException();

		double sum = 0;
		for (int i = 0 ; i < values.length ; i++ ) {
			sum += (values[i] - targets[i])*(values[i] - targets[i]);
		}
		return Math.sqrt(sum/values.length);
	}

}
