package seir;

import java.time.LocalDate;

import tech.tablesaw.api.Table;
import org.apache.commons.lang3.tuple.Pair;

public interface ModelParameterSearch {
//	Pair<ModelParameter, Double> search(Table cases);
	
	default Pair<ModelParameter, Double> search(Table cases){
		final int rowCount = cases.rowCount();
		
		//First row is for the initial conditions
		final LocalDate initDate =cases.dateColumn("Date").get(0);
//		final LocalDate endDate =dateColumn.get(rowCount-1);
		final double initInfectious = cases.doubleColumn("Cumulative Infectious").get(0);
		final double initRecovered = cases.doubleColumn("Cumulative Recovered").get(0);
		final double initDeaths = cases.doubleColumn("Cumulative Death").get(0);
		final double[] observedDeaths = cases.last(rowCount-1).intColumn("Deaths").asDoubleArray();
		final double minInitExposed = 0.5*initInfectious;
		final double maxInitExposed = 10*initInfectious;
		final double minInitInfectious = initInfectious;
		final double maxInitInfectious = 10*initInfectious;
		final double minInitRecovered = initRecovered;
		final double maxInitRecovered = 10*initRecovered;
		
		return search(initDate, 
				minInitExposed, maxInitExposed,
				minInitInfectious, maxInitInfectious,
				minInitRecovered, maxInitRecovered,
				initDeaths, observedDeaths);
	}
	
	Pair<ModelParameter, Double> search(LocalDate initDate, 
			double minInitExposed, double maxInitExposed, 
			double minInitInfectious, double maxInitInfectious,
			double minInitRecovered, double maxInitRecovered,
			double initDeaths, double[] observedDeaths);
}
