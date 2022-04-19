package geneticalgorithm.seir;

import static seir.SEIRParametersRanges.MIN_ALPHA;
import static seir.SEIRParametersRanges.MAX_ALPHA;
import static seir.SEIRParametersRanges.MIN_BETA;
import static seir.SEIRParametersRanges.MAX_BETA;
import static seir.SEIRParametersRanges.MIN_INCUBATION_PERIOD;
import static seir.SEIRParametersRanges.MAX_INCUBATION_PERIOD;
import static seir.SEIRParametersRanges.MIN_INFECTIOUS_PERIOD;
import static seir.SEIRParametersRanges.MAX_INFECTIOUS_PERIOD;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import geneticalgorithm.Environment;
import geneticalgorithm.Genome;
import seir.ModelParameter;
import seir.SEIRModel;

public class ModelParameterGenome implements Genome {
	
	private final byte[] genes;
	private ModelParameter parameter;
	private final Random random = new Random();
	private final AtomicReference<Optional<Double>> fitness = new AtomicReference<>();

	public ModelParameterGenome(byte[] bytes) {
		this.genes = bytes;
	}

	@Override
	public ModelParameterGenome[] crossover(Genome theOther) {
		byte[] child0 = Arrays.copyOf(genes, genes.length);
		byte[] child1 = Arrays.copyOf(((ModelParameterGenome)theOther).genes, genes.length);
		
		int crossoverPoint = random.nextInt(genes.length);
		byte swapByte = child0[crossoverPoint];
		child0[crossoverPoint] = child1[crossoverPoint];
		child1[crossoverPoint] = swapByte;
		return new ModelParameterGenome[] {new ModelParameterGenome(child0), new ModelParameterGenome(child1) };
	}

	@Override
	public ModelParameterGenome mutate() {
		int geneToMutate = random.nextInt(genes.length);
		byte[] newGenome = Arrays.copyOf(genes, genes.length);
		newGenome[geneToMutate] = flipRandomBit(newGenome[geneToMutate]);
		return new ModelParameterGenome(newGenome);
	}
	
	private byte flipRandomBit(byte value) {
		int position = random.nextInt(Byte.SIZE);
		int mutated = Byte.toUnsignedInt(value)^ ( 1<< position);
		return (byte)mutated;
	}
	
	private ModelParameter decode(byte[] values, ModelEnvironment enviroment) {
		double alpha = decode(values[0], MIN_ALPHA, MAX_ALPHA);
		double beta = decode(values[1], MIN_BETA, MAX_BETA);
		double epsilon = decode(values[2], 1/MAX_INCUBATION_PERIOD, 1/MIN_INCUBATION_PERIOD);
		double gamma = decode(values[3], 1/MAX_INFECTIOUS_PERIOD, 1/MIN_INFECTIOUS_PERIOD);
		double exposed = decode(values[4], enviroment.getMinInitExposed(), enviroment.getMaxInitExposed() );
		double infectious = decode(values[5], enviroment.getMinInitInfectious(), enviroment.getMaxInitInfectious());
		double recovered = decode(values[6], enviroment.getMinInitRecovered(), enviroment.getMaxInitRecovered());
		return new ModelParameter(alpha, beta, epsilon, gamma, exposed, infectious, recovered, enviroment.getInitDeaths());
	}
	
	private double decode(byte value, double minValue, double maxValue) {
		return minValue + (maxValue - minValue)*Byte.toUnsignedInt(value)/255.0;
	}

	@Override
	public void calculateFitness(Environment env) {
		if (fitness.compareAndSet(null, Optional.empty())) {
			if (env instanceof ModelEnvironment) {
				ModelEnvironment environment = (ModelEnvironment) env;
				parameter = decode(this.genes, environment);
				double rmse = rmse(new SEIRModel(parameter).run(environment.getObservedDeaths().length).getNewDeath(), environment.getObservedDeaths());
				fitness.set(Optional.of( rmse));
			}
			else {
				throw new IllegalArgumentException();
			}
		}
	}
	
	@Override
	public double getFitness() {
		return fitness.get().get();
	}

	@Override
	public String toString() {
		return "ModelParameterGenome [fitness=" + getFitness() + ",\nparameter=" + parameter + ",\ngenome="+ genomeToString(genes) + "]";
	}
	
	private String genomeToString(byte[] bytes) {
		return "[" + 
				IntStream.range(0, bytes.length).
							mapToObj(i->bytes[i]).
							map(b->String.format("%02X", b)).
							collect(Collectors.joining(", ")) + 
				"]";
	}
	
	public ModelParameter getParameter() {
		return parameter;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(genes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelParameterGenome other = (ModelParameterGenome) obj;
		if (!Arrays.equals(genes, other.genes))
			return false;
		return true;
	}
	
	private double rmse(double[] values, double[] targets){
		if ( values.length != targets.length)
			throw new IllegalArgumentException();

		double sum = 0;
		for (int i = 0 ; i < values.length ; i++ ) {
			sum += (values[i] - targets[i])*(values[i] - targets[i]);
		}
		return Math.sqrt(sum/values.length);
	}

	
}
