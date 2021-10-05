package seir;

import static java.lang.System.out;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import executorservice.CommonExecutorService;
import tablesaw.TablesawUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.xlsx.XlsxReadOptions;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.api.TimeSeriesPlot;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class SEIRModelForcast {
	static final int NUM_DAYS = 60;
	static final String GET_CASES_BRIEFINGS_CSV_URL = "https://raw.githubusercontent.com/wiki/djay/covidthailand/cases_briefings.csv";
	static final String POPULATION_FILE = "/Users/mananai/Downloads/sector_01_11101_EN_.xlsx";

	static final LocalDate START_NEW_WAVE_DATE = LocalDate.of(2021, 4, 1);
	static final boolean OUTPUT_PLOTS = true;
	static final int MAX_SEARCH_RESULT = 10;
	static final int NUM_STEPS = 20;
	

	public static void main(String[] args ) throws MalformedURLException, IOException {//throws MalformedURLException, IOException, CloneNotSupportedException {
		
		try {
			new SEIRModelForcast().run();
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
		finally {
			CommonExecutorService.shutdown();
		}
	}

	public void run() throws MalformedURLException, IOException {
		
		Table population = Table.read().usingOptions(XlsxReadOptions.builder(POPULATION_FILE).build());
		ModelParameter.setPopulation(population.intColumn("Total").get(0));
		
		Table cases = Table.read().usingOptions(CsvReadOptions.builder(new URL(GET_CASES_BRIEFINGS_CSV_URL)));
		cases.retainColumns("Date", "Cases", "Recovered", "Deaths");

		var avgDeathsCol = ((NumericColumn<Integer>)cases.intColumn("Deaths")).rolling(7).mean().asIntColumn();
		avgDeathsCol.setName("7-day avg deaths");
		cases.addColumns(avgDeathsCol);
		
		cases = cases.where(cases.dateColumn("Date").isOnOrAfter(START_NEW_WAVE_DATE));
		DoubleColumn infectiousColumn = cases.intColumn("Cases").subtract(cases.intColumn("Recovered"));
		infectiousColumn.setName("Infectious");
		cases.addColumns(infectiousColumn);
		DoubleColumn cumCases = cases.intColumn("Cases").cumSum();
		cumCases.setName("Cumulative Cases");
		DoubleColumn cumInfectious = cases.doubleColumn("Infectious").cumSum();
		cumInfectious.setName("Cumulative Infectious");
		DoubleColumn cumRecovered = cases.intColumn("Recovered").cumSum();
		cumRecovered.setName("Cumulative Recovered");
		DoubleColumn cumDeath = cases.intColumn("Deaths").cumSum();
		cumDeath.setName("Cumulative Death");
		cases.addColumns(
				cumCases, 
				cumInfectious, 
				cumRecovered, 
				cumDeath);
			
		Table deaths = TablesawUtils.group(cases, Arrays.asList("Deaths", "7-day avg deaths"), "Date", "Count", "Group");
		Plot.show(TimeSeriesPlot.create("Daily Deaths", deaths, "Date", "Count", "Group"));

		Table cumsumInfectious = TablesawUtils.group(cases, Arrays.asList("Cumulative Cases", "Cumulative Infectious", "Cumulative Recovered"), "Date", "Count", "Group");
		Plot.show(TimeSeriesPlot.create("Cumulative data", cumsumInfectious, "Date", "Count", "Group"));

		Table dailyInfectious = TablesawUtils.group(cases, Arrays.asList("Cases", "Infectious", "Recovered"), "Date", "Count", "Group");
		Plot.show(TimeSeriesPlot.create("Daily data", dailyInfectious, "Date", "Count", "Group"));
		
		Table tLatest = cases.last(NUM_DAYS+1);
		final LocalDate initDate =tLatest.dateColumn("Date").get(0);
		final double[] newDeaths = tLatest.last(tLatest.rowCount()-1).intColumn("Deaths").asDoubleArray();
		Predicate<ModelParameter> constraint = param->true;

		var searchResult = new SEIRParameterGridSearch().search(tLatest, constraint, MAX_SEARCH_RESULT, NUM_STEPS );

		Table tResult = Table.create();
		tResult.addColumns(
				DoubleColumn.create("rmse"),
				DoubleColumn.create("R0"),
				DoubleColumn.create("IFR"),
				DoubleColumn.create("Alpha"),
				DoubleColumn.create("Beta"),
				DoubleColumn.create("Epsilon"),
				DoubleColumn.create("Gamma"),
				DoubleColumn.create("Init Exposed Count"),
				DoubleColumn.create("Init Infectious Count"),
				DoubleColumn.create("Init Recovered Count")
				);
		searchResult.stream().limit(MAX_SEARCH_RESULT).forEach(e->{
			Row row = tResult.appendRow();
			row.setDouble("rmse", e.getValue());
			row.setDouble("R0", e.getKey().getR0());
			row.setDouble("IFR", e.getKey().getIFR());
			row.setDouble("Alpha", e.getKey().getAlpha());
			row.setDouble("Beta", e.getKey().getBeta());
			row.setDouble("Epsilon", e.getKey().getEpsilon());
			row.setDouble("Gamma", e.getKey().getGamma());
			row.setDouble("Init Exposed Count", e.getKey().getExposed());
			row.setDouble("Init Infectious Count", e.getKey().getInfectious());
			row.setDouble("Init Recovered Count", e.getKey().getRecovered());
		});

		out.println(tResult);
		tResult.write().csv("SEIRResult-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");

		if (searchResult.size() > 0) {
			var arrayOfParameters = searchResult.stream().map(e -> e.getKey()).limit(5)
					.toArray(ModelParameter[]::new);

			BiFunction<ModelParameter, Integer, double[]> deathFunc = (params,
					numDays) -> new SEIRModel(params).run(numDays).getNewDeath();
			SEIRPlotUtils.plotDataVsModel("Data vs Model", "Deaths", deathFunc, arrayOfParameters, NUM_DAYS,
					newDeaths, initDate);

			final double[] accInfectiouses = cases.last(NUM_DAYS).doubleColumn("Cumulative Infectious")
					.asDoubleArray();
			BiFunction<ModelParameter, Integer, double[]> infectiousFunc = (params,
					numDays) -> new SEIRModel(params).run(numDays).getInfectious();
			SEIRPlotUtils.plotDataVsModel("Data vs Model", "Deaths", deathFunc, arrayOfParameters, 120,
					newDeaths, initDate);
			SEIRPlotUtils.plotDataVsModel("Data vs Model", "Infectious", infectiousFunc, arrayOfParameters, 120,
					accInfectiouses, initDate);
		}
		
		Table r0ByMonth = getReproductionRateByMonth(cases, constraint, 15);
		Plot.show(LinePlot.create("Reproduction Ratio", r0ByMonth, "Month", "R0"));

	}
	
	private Table getReproductionRateByMonth(Table cases, Predicate<ModelParameter> constraint, int parameterGridSize) {
		var dateColumn = cases.dateColumn("Date");
		List<Table> tables = Arrays.asList(
				cases.where(dateColumn.isInApril()),
				cases.where(dateColumn.isInMay()),
				cases.where(dateColumn.isInJune()),
				cases.where(dateColumn.isInJuly()),
				cases.where(dateColumn.isInAugust()),
				cases.where(dateColumn.isInSeptember())
				);
		
		int[] months = {4, 5, 6, 7, 8, 9};
		List<Double> r0Values = new ArrayList<>();
		tables.forEach(table->{
			int maxSearchResult = 1;
			var searchResult = new SEIRParameterGridSearch().search(table, constraint, maxSearchResult, parameterGridSize );
			r0Values.add(searchResult.first().getKey().getR0());
		});
		
		Table table = Table.create("R0");
		table.addColumns(IntColumn.create("Month", months));
		table.addColumns(DoubleColumn.create("R0", r0Values));
		return table;
	}
	
}
