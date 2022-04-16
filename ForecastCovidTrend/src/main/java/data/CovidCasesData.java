package data;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;

import seir.ModelParameter;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.xlsx.XlsxReadOptions;

public class CovidCasesData {
//	static final int NUM_DAYS = 60;
	static final String GET_CASES_BRIEFINGS_CSV_URL = "https://raw.githubusercontent.com/wiki/djay/covidthailand/cases_briefings.csv";
	static final String POPULATION_FILE = "/Users/mananai/Downloads/sector_01_11101_EN_.xlsx";

	static final LocalDate START_3RD_WAVE_DATE = LocalDate.of(2021, 4, 1);
	static final boolean OUTPUT_PLOTS = false;

	public Table getCases() throws IOException{
		return getCases(POPULATION_FILE, GET_CASES_BRIEFINGS_CSV_URL);
	}
	
	public Table getCases(String populationFile, String casesBriefingURL) throws IOException{
		Table population = Table.read().usingOptions(XlsxReadOptions.builder(populationFile).build());
		ModelParameter.setPopulation(population.intColumn("Total").get(0));
		
		Table cases = Table.read().usingOptions(CsvReadOptions.builder(new URL(casesBriefingURL)));
		cases.retainColumns("Date", "Cases", "Recovered", "Deaths");

		var avgDeathsCol = ((NumericColumn<Integer>)cases.intColumn("Deaths")).rolling(7).mean().asIntColumn();
		avgDeathsCol.setName("7-day avg deaths");
		cases.addColumns(avgDeathsCol);
		
		cases = cases.where(cases.dateColumn("Date").isOnOrAfter(START_3RD_WAVE_DATE));
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
			
		return cases;
	}
}
