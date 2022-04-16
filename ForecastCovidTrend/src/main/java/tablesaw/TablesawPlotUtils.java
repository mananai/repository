package tablesaw;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.components.Margin;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.BoxTrace;
import tech.tablesaw.plotly.traces.HeatmapTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */

public class TablesawPlotUtils {
	
	public static void plotHeatmap(Table table, String column1, String column2, String heatColumn,
			String chartTitle) {
		
		table = table.sortOn(column1, column2);

//		var col1 = ((NumberColumn<?,?>) table.column(column1)).unique();
		var col1 = table.numberColumn(column1).unique();
		col1.sortAscending();
		Number[] values1 = col1.asObjectArray();

//		var col2 = ((NumberColumn<?,?>) table.column(column2)).unique();
		var col2 = table.numberColumn(column2).unique();
		col2.sortAscending();
		Number[] values2 = col2.asObjectArray();
		
		double[][] heat2d = new double[values1.length][values2.length];
		
//		NumberColumn<?,?> heatCol = (NumberColumn<?,?>) table.column(heatColumn);
		var heatCol = table.numberColumn(heatColumn);
		Number[] heat1d = heatCol.asObjectArray();
		
		assert heat1d.length == values1.length*values2.length;
		
		int k=0;
		for (int i = 0 ; i < values1.length ; i++) {
			for(int j=0 ; j < values2.length ; j++ ) {
				heat2d[i][j] = heat1d[k].doubleValue();
				k++;
			}
		}
		TablesawPlotUtils.plotHeatMap(chartTitle, column1, column2, values1, values2, heat2d);
	}
	
	public static  void plotHeatMap(String title, String yTitle, String xTitle, Number[] yvalues, Number[] xvalues, 
			double[][] heat) {
	    Layout layout = Layout.builder(title, xTitle, yTitle).build();
	    HeatmapTrace trace = HeatmapTrace.builder(xvalues, yvalues, heat ).build();
	    Plot.show(new Figure(layout, trace));
	}

	
//	public static <T extends Number, U extends Number>  void plotHeatMap(String title, String xAxis, String yAxis, T[] columns, U[] rows, 
//			double[][] heat) {
//	    Layout layout = Layout.builder(title, xAxis, yAxis).build();
//	    HeatmapTrace trace = HeatmapTrace.builder(columns, rows, heat ).build();
//	    Plot.show(new Figure(layout, trace));
//	}
//
	
	
	public  static void plotHeatMap(String title, String xAxis, String yAxis, Double[] columns, Double[] rows, double[][] heat) {
	    Layout layout = Layout.builder(title, xAxis, yAxis).build();
	    HeatmapTrace trace = HeatmapTrace.builder(columns, rows, heat ).build();
	    Plot.show(new Figure(layout, trace));
	}

	
	public static void plotTimeseries( String title, LocalDate startDate, int numDays, String yAxis, 
			List<String> labelList, List<double[]> dataList) {
		
		var modeList = Collections.nCopies(labelList.size(), ScatterTrace.Mode.LINE);
		plotTimeseries(title, startDate, numDays, yAxis, labelList, dataList, modeList);
	}

	
	public static void plotTimeseries( String title, LocalDate startDate, int numDays, String yAxis, 
			List<String> labelList, List<double[]> dataList, List<ScatterTrace.Mode> modeList) {
		
		assert labelList.size() == dataList.size();
		assert dataList.size() == modeList.size();
		
		LocalDate[] dates = IntStream.iterate(0, i->i+1).limit(numDays).mapToObj(i->startDate.plusDays(i)).toArray(LocalDate[]::new);
		
		var dateCol = DateColumn.create("Date", dates);
		var traces = new Trace[dataList.size()];
		for (int i=0; i < dataList.size() ; i++) {
			var dataCol = DoubleColumn.create(labelList.get(i), dataList.get(i));
			traces[i] =  ScatterTrace.builder(dateCol, dataCol).name(labelList.get(i)).mode(modeList.get(i)).build();
		}
	    Layout layout = Layout.builder(title, "Date", yAxis).build();
	    Plot.show(new Figure(layout, traces));
	}

	public static void plotHorizontalBar(String title, String xAxisTitle, String[] labels, double[] values, int height, int leftMargin) {
		Axis xAxis = Axis.builder().title(xAxisTitle).build();
		Margin margin = Margin.builder().left(leftMargin).build();
		Layout layout = Layout.builder().title(title).xAxis(xAxis).height(height).margin(margin).build();
	    BarTrace trace = BarTrace.builder(labels, values).orientation(BarTrace.Orientation.HORIZONTAL).build();
	    Plot.show(new Figure(layout, trace));
	}

	public static void plotBoxChart(String title, String xAxisTitle, String yAxisTitle, Object[] x, double[] y) {
		Axis xAxis = Axis.builder().title(xAxisTitle).build();
		Axis yAxis = Axis.builder().title(yAxisTitle).build();
	    Layout layout = Layout.builder().title(title).xAxis(xAxis).yAxis(yAxis).build();
	    BoxTrace trace = BoxTrace.builder(x, y).build();
	    Plot.show(new Figure(layout, trace));
	}

}
