import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main {

	private static String OUTPUT_FILE = "output2.owl"; 
	private static final List<Integer> COMB_COLUMNS;
	private static Map<String, Integer> columnMap = new HashMap<>();
	private static Map<String, String> predefinedColumnMap = new HashMap<>();
	
	static {
		COMB_COLUMNS = Arrays.asList(2, 3, 4, 5, 6, 7, 12);
		columnMap.put("Protein", 2);
		columnMap.put("Gene", 3);
		columnMap.put("Organism", 4);
		columnMap.put("Org", 4);
		columnMap.put("BioProcess", 5);
		columnMap.put("BiologicalProcess", 5);
		columnMap.put("MolecularFunction", 6);
		columnMap.put("MolFunc", 6);
		columnMap.put("CellComponent", 7);
		columnMap.put("Comp", 7);
//   	columnMap.put("Phenotype", 12);
		columnMap.put("Situation", 12);
		predefinedColumnMap.put("Molecule", "Homocysteine");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("XLS file not specified.");
			System.exit(0);
		}
		
		File file = new File(args[0]);
		if (!file.exists()) {
			throw new FileNotFoundException(args[0]);
		}
		
		long initialTimeMillis = System.currentTimeMillis();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE), 1024*1024*100);
		new Main().convert(file, writer);
		writer.close();
		
		System.out.println("\n\n* Finished *\nSee output2.owl\nTime elapsed: " + 
				((System.currentTimeMillis() - initialTimeMillis) / 1000.0 ) + "s" );
	}


	private void convert(File file, Writer writer) throws IOException {
		InputStream in = new FileInputStream(file);
		Workbook wb = new XSSFWorkbook(in);
		
		Sheet modelsSheet = wb.getSheet("owl-elements2");
		for (int modelRowIndex = 1; modelRowIndex < 29; modelRowIndex++) {
			Row modelRow = modelsSheet.getRow(modelRowIndex);
			writer.append(columnsJoinText(rowToList(modelRow)));
		}

		Sheet dataSheet = wb.getSheet("final-data");
		for (int dataRowIndex = 7; dataRowIndex < 52; dataRowIndex++) {
			Row dataRow = dataSheet.getRow(dataRowIndex);
			
			List<String> dataRowList = rowToList(dataRow);
//			writer.append("\n    " + strComm("XLS line: " + dataRowList.toString()) + "\n");
//			writer.append("   " + strComm("=========") + "\n");
			
			System.out.println("\n*\nXLS line " + (dataRowIndex + 1) + ": " + dataRowList.toString() + "\nCalculating rows...");
			Set<List<String>> derivedRows = getDerivedRows(dataRowList);
			System.out.println("Writing rows...");
			int rowNum = 1;
			for (List<String> derivedRow : derivedRows) {
//				writer.append("    " + strComm("Derived line (" + rowNum + "/" + derivedRows.size() + "): " + derivedRow.toString()) + "\n");
				
				// Loop que percorre as linhas de owl-elements2. Primeira linha tem índice 0.
				
				for (int modelRowIndex = 29; modelRowIndex < 107; modelRowIndex++) {
					List<String> modelRow = rowToList(modelsSheet.getRow(modelRowIndex));
					writer.append(expandData(columnsJoinText(modelRow), derivedRow));
				}
				System.out.print("\r" + rowNum + "/" + derivedRows.size());
				rowNum++;
			}
		}
		
		Row lastElementRow = modelsSheet.getRow(modelsSheet.getLastRowNum());
		
		writer.append(columnsJoinText(rowToList(lastElementRow)));
		
		wb.close();
	}
	
	private List<String> rowToList(Row row) {
		List<String> rowList = new ArrayList<>();
		for (int column = 0; column < row.getLastCellNum(); column++) {
			rowList.add(row.getCell(column).getStringCellValue());
		}
		return rowList;
	}

	private Set<List<String>> getDerivedRows(List<String> row) {
		Set<List<String>> derivedRows = new HashSet<>();
		for (Integer column : COMB_COLUMNS) {
			String colText = row.get(column);
			String[] elements = colText.split(";");
			if (elements.length > 1) {
				List<String> newRow = new ArrayList<>(row);
				newRow.set(column, colText.substring(colText.indexOf(";") + 1));
				derivedRows.addAll(getDerivedRows(newRow));
				row.set(column, colText.substring(0, colText.indexOf(";")));
			}
		}
		derivedRows.add(row);
		return derivedRows;
	}

	private String columnsJoinText(List<String> modelRow) {
		StringBuilder out = new StringBuilder();
//		out.append("\n");
//		out.append(strComm(elementRow.getCell(0).getStringCellValue()));
//		out.append("\n");
		out.append(modelRow.get(1));
		out.append("\n");
		return out.toString();
	}
	
	private String formatColumnKey(String column) {
		return "$" + column + "$";
	}
	
	private String expandData(String elementText, List<String> dataRow) {
		for (String column : columnMap.keySet()) {
			String columnKey = formatColumnKey(column);
			if (elementText.contains(columnKey)) {
				String columnValue = dataRow.get(columnMap.get(column));
				if (columnValue == null || columnValue.trim().isEmpty()) {
					elementText = "";
				} else {
					elementText = elementText.replace(columnKey, strNormalizedName(columnValue));
				}
			}
		}
		if (!elementText.trim().isEmpty()) {
			for (String predefinedColumn : predefinedColumnMap.keySet()) {
				String predefinedColumnKey = formatColumnKey(predefinedColumn);
				if (elementText.contains(predefinedColumnKey)) {
					elementText = elementText.replace(predefinedColumnKey, strNormalizedName(predefinedColumnMap.get(predefinedColumn)));
				}
			}
		}

		return elementText;
	}
	
	private String strComm(String text) {
		if (text == null || text.trim().length() == 0) {
			return "";
		}
		if (text.contains("--")) {
			text = text.replace("--", "__");
		}
		return "<!--" + text + "-->";
	}
	
	private String strNormalizedName(String text) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
					.contains(String.valueOf(text.charAt(i)))) {
				result.append(text.charAt(i));
			} else {
				result.append("_");
			}
		}
		return result.toString();
	}
}
