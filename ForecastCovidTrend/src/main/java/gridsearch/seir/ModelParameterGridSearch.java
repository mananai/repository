package gridsearch.seir;

import static java.util.stream.Collectors.toList;
import static seir.SEIRParametersRanges.MAX_ALPHA;
import static seir.SEIRParametersRanges.MAX_BETA;
import static seir.SEIRParametersRanges.MAX_INCUBATION_PERIOD;
import static seir.SEIRParametersRanges.MAX_INFECTIOUS_PERIOD;
import static seir.SEIRParametersRanges.MIN_ALPHA;
import static seir.SEIRParametersRanges.MIN_BETA;
import static seir.SEIRParametersRanges.MIN_INCUBATION_PERIOD;
import static seir.SEIRParametersRanges.MIN_INFECTIOUS_PERIOD;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.tuple.Pair;

import gridsearch.Grid;
import gridsearch.GridSearch;
import seir.ModelParameter;
import seir.ModelParameterSearch;
import seir.SEIRModel;

public class ModelParameterGridSearch implements ModelParameterSearch {
//	private final int numSteps;
	private final List<Integer> numSteps;
	
	
//	public ModelParameterGridSearch(int numSteps) {
//		super();
//		this.numSteps = numSteps;
//	}

	public ModelParameterGridSearch(List<Integer> numSteps) {
		super();
		
		assert numSteps.size() == 7;
		
		this.numSteps = numSteps;
	}

	@Override
	public Pair<ModelParameter, Double> search(LocalDate initDate, double minInitExposed, double maxInitExposed,
			double minInitInfectious, double maxInitInfectious, double minInitRecovered, double maxInitRecovered,
			double initDeaths, double[] observedDeaths) {
		
		
		List<Double> alphas = arithematicSequences(MIN_ALPHA, MAX_ALPHA, numSteps.get(0));
		List<Double> betas = arithematicSequences(MIN_BETA, MAX_BETA, numSteps.get(1));
		//Incubation period is 5.2 days
		List<Double> epsilons = inverseSequences(MIN_INCUBATION_PERIOD, MAX_INCUBATION_PERIOD, numSteps.get(2)); //9);
		//Infectious period is 5 days
		List<Double> gammas = inverseSequences(MIN_INFECTIOUS_PERIOD, MAX_INFECTIOUS_PERIOD, numSteps.get(3)); //11);
		List<Double> exposedes = geometricSequences(minInitExposed, maxInitExposed, numSteps.get(4));
		List<Double> infectiouses = geometricSequences(minInitInfectious, maxInitInfectious, numSteps.get(5));
		List<Double> recoveredes = geometricSequences(minInitRecovered, maxInitRecovered, numSteps.get(6));
		List<Double> deaths = Arrays.asList(initDeaths);

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
				new SEIRModel(param).run(observedDeaths.length).getNewDeath(), observedDeaths);
		return new GridSearch<ModelParameter>().run(parameterGrid, p->true, objectiveFunction, 1).first();
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
