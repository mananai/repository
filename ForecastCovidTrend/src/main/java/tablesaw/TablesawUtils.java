package tablesaw;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.api.TimeSeriesPlot;
import tech.tablesaw.selection.Selection;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class TablesawUtils {
	public static Table group(Table table, List<String> columnNames, String dateName, String valueName, String groupName) {
		
		List<Table> tables = new ArrayList<>();
		columnNames.forEach(columnName->
		{
			DoubleColumn valueCol = ((NumericColumn<?>) table.column(columnName).copy()).asDoubleColumn();
			valueCol.setName(valueName);

			var groupCol = StringColumn.create(groupName, valueCol.size());
			groupCol.set(Selection.withRange(0, valueCol.size()), columnName);
			
			var newTable = Table.create().addColumns(table.dateColumn(dateName).copy(), valueCol, groupCol);
			
			tables.add(newTable);
		});

		Table groupTable = tables.stream().reduce(tables.get(0).emptyCopy(), Table::append);
		groupTable.setName("Group");
		return groupTable;
	}
	
	public static void plot(String plotName, double[] inputData, String inputName, double[][] valuesArray, String[] colNames) {
		
		List<Table> tables = new ArrayList<>();
		
		for (int i = 0 ; i < valuesArray.length ; i++) {
			double[] values = valuesArray[i];
			Table subtable = Table.create();
			
			DoubleColumn cInput = DoubleColumn.create(inputName, inputData);			
			DoubleColumn cValue = DoubleColumn.create("Value", values);
			StringColumn cGroup = StringColumn.create("Group", values.length);
			cGroup.set(Selection.withRange(0, values.length), colNames[i]);
			subtable.addColumns(cInput, cValue, cGroup);
			tables.add(subtable);
		}
		Table table = tables.stream().reduce(tables.get(0).emptyCopy(), Table::append);
		Plot.show(LinePlot.create(plotName, table, inputName, "Value", "Group"));
	}
	
	public static void plot(String plotName, String yAxis, double[][] valuesArray, String[] colNames, String xAxis, LocalDate[] dates) {
		
		List<Table> tables = new ArrayList<>();
		
		for (int i = 0 ; i < valuesArray.length ; i++) {
			double[] values = valuesArray[i];
			Table subtable = Table.create();
			
			DateColumn cInput = DateColumn.create(xAxis, dates);			
			DoubleColumn cValue = DoubleColumn.create(yAxis, values);
			StringColumn cGroup = StringColumn.create("Group", values.length);
			cGroup.set(Selection.withRange(0, values.length), colNames[i]);
			subtable.addColumns(cInput, cValue, cGroup);
			tables.add(subtable);
		}
		Table table = tables.stream().reduce(tables.get(0).emptyCopy(), Table::append);
		Plot.show(TimeSeriesPlot.create(plotName, table, xAxis, yAxis, "Group"));
	}

}
