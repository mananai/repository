package geneticalgorithm;

import static java.util.Arrays.asList;
import static tech.tablesaw.aggregate.AggregateFunctions.*;
import static tech.tablesaw.plotly.traces.ScatterTrace.Mode.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.CovidCasesData;
import executorservice.CommonExecutorService;
import geneticalgorithm.seir.ModelParameterGASearch;
import geneticalgorithm.seir.ModelParameterGenome;
import gridsearch.seir.ModelParameterGridSearch;
import seir.ModelParameter;
import seir.ModelParameterSearch;
import seir.SEIRModel;
import tablesaw.Line3DPlot;
import tablesaw.TablesawPlotUtils;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.components.Marker.Palette;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.selection.Selection;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class SEIRModelForecast {
	

	Logger logger = LoggerFactory.getLogger(this.getClass());

	static final int POPULATION_SIZE = 1_000;
	static final int MAX_GENERATION = 20_000;
	static final int MAX_GENERATION_WITH_SAME_GENOME = 4_000;
	static final double CROSSOVER_RATE = 0.7;
	static final double MUTATION_RATE = 0.8;
	static final int TOURNAMENT_SIZE = 2;
	static final int ELITE_GROUP_SIZE = Math.min((int)(0.05*POPULATION_SIZE), 100);

	public static void main(String[] args) throws IOException {
		final int NUM_DAYS = 30;
		
		boolean doFatalitiesPlot = false;
		boolean doGaSearch = false;
		boolean doR0Plot = false;
		boolean doHistoricalPlot = false;
		boolean doFitnessBoxPlot = false;
		boolean doGridSearch = false;
		
		for (var arg: args) {
			switch(arg) {
				case "-f" : 
					doFatalitiesPlot = true;
					break;
				case "-ga" : 
					doGaSearch = true;
					break;
				case "-hist" :
					doHistoricalPlot = true;
					break;
				case "-box" :
					doFitnessBoxPlot = true;
					break;
				case "-r0" :
					doR0Plot = true;
					break;
				case "-grid" :
					doGridSearch = true;
					break;
				default:
					throw new IllegalArgumentException(String.format("The argument %s not recognized", arg));
			}
		}
		try {
			new SEIRModelForecast().run(NUM_DAYS, doFatalitiesPlot, doGaSearch, doHistoricalPlot, doFitnessBoxPlot, doR0Plot, doGridSearch);
		} finally {
			CommonExecutorService.shutdown();
		}
	}

	public void run(int numDays, boolean doFatalitiesPlot, boolean doGaSearch, boolean doHistoricalPlot, 
			boolean doFitnessBoxPlot, boolean doR0Plot, boolean doGridSearch) throws IOException {

		Table cases = new CovidCasesData().getCases();

		if (doFatalitiesPlot)
			plotFatalities(cases);

		final Table recentCases = cases.last(numDays+1);
		final DateColumn dateCol = recentCases.dateColumn("Date");
		final LocalDate initDate =dateCol.get(0);
		final LocalDate endDate =dateCol.get(recentCases.rowCount()-1);
		
		ModelParameter gaModelParam = null;
		Double gaRmse = null;
		Double gaSearchTime = null;
		
		if (doGaSearch) {
			logger.info("Processing data from {} to {} using GA search...", initDate, endDate);
			var gaSearch = new ModelParameterGASearch(POPULATION_SIZE, MAX_GENERATION, MAX_GENERATION_WITH_SAME_GENOME, 
					CROSSOVER_RATE, MUTATION_RATE, TOURNAMENT_SIZE, ELITE_GROUP_SIZE);
			HistoricalWinnersKeeper historicalWinnersKeeper = null; 
			FitnessDistributionKeeper fitnessDistributionKeeper = null;
			if (doHistoricalPlot ) {
				historicalWinnersKeeper = new HistoricalWinnersKeeper();
				gaSearch.addEventListener(historicalWinnersKeeper);
			}
			if ( doFitnessBoxPlot) {
				final int maxGenInChart = 7;
				fitnessDistributionKeeper = new FitnessDistributionKeeper(maxGenInChart);
				gaSearch.addEventListener(fitnessDistributionKeeper);
			}
			
			long startTime = System.currentTimeMillis();
			var gaSearchResult = gaSearch.search(recentCases);
			long finishTime = System.currentTimeMillis();
			gaSearchTime = (finishTime-startTime)/1000.0;
			gaModelParam = gaSearchResult.getKey();
			gaRmse = gaSearchResult.getValue();
			logger.info("For the last {} days, R0 is {}, RMSE is {}", numDays, gaModelParam.getR0(), gaRmse);
			logger.info("Took {} seconds to process", gaSearchTime);

			
			plotModelsVsObserved(recentCases, String.format("SEIR: RMSE=%.3f", gaRmse), gaModelParam );

			if ( doHistoricalPlot) {
				var historicalWinners = historicalWinnersKeeper.getHistoricalWinners();
				var table = historicalWinnersTable(historicalWinners);
				logger.debug("Historical table:\n {}", table.print());
				this.plotHistoricalWinner(table);
				
				final int numGenerations = 4;
				var indexes = IntStream.range(0, Math.min(numGenerations-1, historicalWinners.size()-1)).boxed().collect(Collectors.toList());
				indexes.add(historicalWinners.size()-1);
				var labels = indexes.stream().
									map(historicalWinners::get).
									map(p->String.format("Gen#%d: RMSE=%.3f", p.getKey(), ((ModelParameterGenome)p.getValue()).getFitness())).
									collect(Collectors.toList());
				var params = indexes.stream().
									map(historicalWinners::get).
									map(Pair::getValue).
									map(g->(ModelParameterGenome)g).
									map(ModelParameterGenome::getParameter).
									collect(Collectors.toList());
				
				plotModelsVsObserved(recentCases, labels, params);
				gaSearch.removeEventListner(historicalWinnersKeeper);
			}
			
			if (doFitnessBoxPlot) {
				var pairOfList = this.toBoxChartData(fitnessDistributionKeeper.getFitnessesDistributions());
				TablesawPlotUtils.plotBoxChart("Fitness distribution", "Generation", "Fitness", pairOfList.getLeft(), pairOfList.getRight());
				gaSearch.removeEventListner(fitnessDistributionKeeper);
			}

			if ( doR0Plot )
				plotReproductionRates(gaSearch, cases);
		}
		
		if (doGridSearch) {
			logger.info("Processing data from {} to {} using grid search...", initDate, endDate);
			var gridSearch = new ModelParameterGridSearch(asList(17, 17, 9, 11, 17, 17, 17));
			var startTime = System.currentTimeMillis();
			var gridSearchResult = gridSearch.search(recentCases);
			var finishTime = System.currentTimeMillis();
			var gridModelParam = gridSearchResult.getKey();
			var gridRmse = gridSearchResult.getValue();
			var gridSearchTime = (finishTime-startTime)/1000.0;
			logger.info("For the last {} days, R0 is {}, rmse is {}", numDays, gridModelParam.getR0(), gridRmse);
			logger.info("Took {} seconds to process", gridSearchTime);
	
			List<String> labels = null;
			List<ModelParameter> modelParameters = null;
			if ( gaModelParam != null) {
				labels= asList( String.format("GA search SEIR: RMSE=%.3f", gaRmse), 
						String.format("Grid search SEIR: RMSE=%.3f", gridRmse));
				modelParameters= asList( gaModelParam, gridModelParam );
			}
			else {
				labels= asList(String.format("Grid search SEIR: RMSE=%.3f", gridRmse));
				modelParameters= asList( gridModelParam );
			}
			plotModelsVsObserved(recentCases, labels, modelParameters);
			if ( gaSearchTime != null ) {
			    String title = "Time to search";
				String xAxisTitle = "Seconds";

				TablesawPlotUtils.plotHorizontalBar(title, xAxisTitle, new String[] {"Genetic Algorithm", "Grid"}, new double[] {gaSearchTime, gridSearchTime}, 300, 120);
			}
		}
	}
	
	private Table historicalWinnersTable(List<Pair<Integer,Genome>> genomes) {
		Table table = Table.create();
		var generationCol = IntColumn.create("generation", genomes.stream().map(Pair::getKey).mapToInt(Integer::intValue));
		var fitnessCol = DoubleColumn.create("fitness", genomes.stream().map(Pair::getValue).map(Genome::getFitness).mapToDouble(Double::doubleValue));
		var labelCol = StringColumn.create("label",
				genomes.stream()
						.map(Pair::getValue)
						.map(Genome::getFitness)
						.map(d->String.format("%.4f", d)).toArray(String[]::new));
		Supplier<Stream<ModelParameter>> paramStream = ()->
				genomes.stream()
						.map(Pair::getValue)
						.map(e->(ModelParameterGenome)e)
						.map(ModelParameterGenome::getParameter);
		
		var alphaCol=DoubleColumn.create("alpha", paramStream.get().map(ModelParameter::getAlpha).mapToDouble(Double::doubleValue));
		var betaCol=DoubleColumn.create("beta",paramStream.get().map(ModelParameter::getBeta).mapToDouble(Double::doubleValue));
		var epsilonCol=DoubleColumn.create("epsilon", paramStream.get().map(ModelParameter::getEpsilon).mapToDouble(Double::doubleValue));
		var gammaCol=DoubleColumn.create("gamma", paramStream.get().map(ModelParameter::getGamma).mapToDouble(Double::doubleValue));
		var initExposed=DoubleColumn.create("exposed", paramStream.get().map(ModelParameter::getExposed).mapToDouble(Double::doubleValue));
		var initInfectious=DoubleColumn.create("infectious", paramStream.get().map(ModelParameter::getInfectious).mapToDouble(Double::doubleValue));
		var initRecovered=DoubleColumn.create("recovered", paramStream.get().map(ModelParameter::getRecovered).mapToDouble(Double::doubleValue));//.collect(toList()));//.mapToDouble(Double::doubleValue).toArray());		

		table.addColumns(generationCol, fitnessCol, labelCol, alphaCol, betaCol, epsilonCol, gammaCol, initExposed, initInfectious, initRecovered );
		return table;
	}
	
	private void plotHistoricalWinner(Table table) {
		Plot.show(
			    Line3DPlot.create("Generations of SEIR Parameters",
			    			table,		// table
			                "alpha",  	// x
			                "beta", 	// y
			                "epsilon", 	// z
			                "fitness",	// size
			                "fitness",	// color
			                "label", 	// text
			                Palette.JET)); // color scale
	}

	private Pair<Object[], double[]> toBoxChartData(List<Pair<Integer,List<Double>>> fitnessDistributionList) {
		List<Integer> xlist = new ArrayList<>();
		List<Double> ylist = new ArrayList<>();
		
		for ( var pair : fitnessDistributionList) {
			Integer generation = pair.getKey();
			if (generation != 0 ) {
				for (var fitness: pair.getValue()) {
					xlist.add(generation);
					ylist.add(fitness);
				}
			}
		}
		Integer[] xArray = xlist.toArray(new Integer[xlist.size()]);
		Double[] boxed = ylist.toArray(new Double[ylist.size()]);
		double[] yArray = Stream.of(boxed).mapToDouble(Double::doubleValue).toArray();
		return new ImmutablePair<>(xArray, yArray);
	}

	private void plotFatalities(Table cases) {
		var initDate = cases.dateColumn("Date").get(0);
		var rowCount = cases.rowCount();
		var deathCounts = cases.last(rowCount-1).intColumn("Deaths").asDoubleArray();
		var avgDeathCounts = cases.last(rowCount-1).intColumn("7-day avg deaths").asDoubleArray();

		TablesawPlotUtils.plotTimeseries("Fatalities", initDate, rowCount-1, "Fatalities", 
				asList("Daily", "7-day average"), 
				asList(deathCounts, avgDeathCounts));
	}

	private void plotModelsVsObserved(Table cases, String modelLabel, ModelParameter modelParam) {
		plotModelsVsObserved(cases, asList(modelLabel),asList(modelParam), asList(ScatterTrace.Mode.LINE));
	}
	
	private void plotModelsVsObserved(Table cases, List<String> modelLabels, List<ModelParameter> modelParams) {
		var modelModes = Collections.nCopies(modelLabels.size(), ScatterTrace.Mode.LINE);
		plotModelsVsObserved(cases, modelLabels, modelParams, modelModes);
	}
	
	private void plotModelsVsObserved(Table cases, List<String> modelLabels, List<ModelParameter> modelParams,
			List<ScatterTrace.Mode> modelModes) {
		assert modelLabels.size() == modelParams.size();
		assert modelParams.size() == modelModes.size();
		
		var initDate = cases.dateColumn("Date").get(0);
		var rowCount = cases.rowCount();
		var observedDeathCounts = cases.last(rowCount-1).intColumn("Deaths").asDoubleArray();
		var labelList = new ArrayList<String>();
		var dataList = new ArrayList<double[]>();
		var modeList= new ArrayList<ScatterTrace.Mode>();

		labelList.add("Observed Data");
		dataList.add(observedDeathCounts);
		modeList.add(MARKERS);
		
		for (int i=0 ; i < modelParams.size() ; i++) {
			labelList.add(modelLabels.get(i));
			dataList.add(new SEIRModel(modelParams.get(i)).run(rowCount-1).getNewDeath());
			modeList.add(modelModes.get(i));
		}
		
		TablesawPlotUtils.plotTimeseries("SEIR model result vs Observed data", initDate, rowCount-1, "Fatalities", 
				labelList, 
				dataList , 
				modeList);
	}
	
	private void plotReproductionRates(ModelParameterSearch modelParameterSearch, Table cases) {

		var dateCol = cases.dateColumn("Date");
		var casesByYearAndMonth = cases.summarize(dateCol, count).by(dateCol.year(), dateCol.month());
		casesByYearAndMonth= casesByYearAndMonth.where(casesByYearAndMonth.numberColumn("Count [Date]").isGreaterThanOrEqualTo(28.0));

		logger.debug("casesByYearAndMonth: {}", casesByYearAndMonth);
		
		BiFunction<String, DateColumn, Selection> dateInMonthSelection = (str, col)->{
			switch(str) {
				case "JANUARY" : return col.isInJanuary();
				case "FEBRUARY" : return col.isInFebruary();
				case "MARCH" : return col.isInMarch();
				case "APRIL" : return col.isInApril();
				case "MAY" : return col.isInMay();
				case "JUNE" : return col.isInJune();
				case "JULY" : return col.isInJuly();
				case "AUGUST" : return col.isInAugust();
				case "SEPTEMBER" : return col.isInSeptember();
				case "OCTOBER" : return col.isInOctober();
				case "NOVEMBER" : return col.isInNovember();
				case "DECEMBER" : return col.isInDecember();
				default: throw new IllegalArgumentException();
			}
		};
		
		var labels = new ArrayList<String>();
		var searchResults = new ArrayList<Pair<ModelParameter, Double>>();
		
		var years =casesByYearAndMonth.intColumn(0).asObjectArray();
		var months =casesByYearAndMonth.stringColumn(1).asObjectArray();

		for (int i = 0 ; i < years.length ; i++ ) {
			int year = years[i];
			String month = months[i];
			
			var monthlyCases = cases.where(dateCol.isInYear(year).and(dateInMonthSelection.apply(month, dateCol)));
			logger.trace("Processing year {}, month {}...", years[i], months[i]);
			var searchResult = modelParameterSearch.search(monthlyCases);
			searchResults.add(searchResult);

			labels.add(month+ " " + year); 
		}
			
		Table r0Table = Table.create("R0 by month");
		var monthColumn = StringColumn.create("Month", labels);
		var r0Column = DoubleColumn.create("R0", searchResults.stream().map(Pair::getKey).map(ModelParameter::getR0).mapToDouble(Double::doubleValue));
		var rmseColumn = DoubleColumn.create("rmse", searchResults.stream().map(Pair::getValue).mapToDouble(Double::doubleValue));
		var ifrColumn = DoubleColumn.create("IFR", searchResults.stream().map(Pair::getKey).map(ModelParameter::getIFR).mapToDouble(Double::doubleValue));
		r0Table.addColumns(monthColumn, r0Column, rmseColumn, ifrColumn);

		r0Table.addColumns(DoubleColumn.create("alpha", searchResults.stream().map(Pair::getKey).map(ModelParameter::getAlpha).mapToDouble(Double::doubleValue)));//.collect(toList())));
		r0Table.addColumns(DoubleColumn.create("beta", searchResults.stream().map(Pair::getKey).map(ModelParameter::getBeta).mapToDouble(Double::doubleValue)));//.collect(toList())));
		r0Table.addColumns(DoubleColumn.create("gamma", searchResults.stream().map(Pair::getKey).map(ModelParameter::getGamma).mapToDouble(Double::doubleValue)));//).collect(toList())));
		
		logger.debug(r0Table.print());
		
	    Layout layout = Layout.builder("Reproduction Rate", "Month", "R0").build();
	    ScatterTrace lineTrace = ScatterTrace.builder(monthColumn, r0Column).name("R0").mode(ScatterTrace.Mode.LINE).build();
	    Plot.show(new Figure(layout, lineTrace));
	}
	
}
