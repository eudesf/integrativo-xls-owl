import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
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

	private static Map<String, Integer> columnMap = new HashMap<>();
	private static Map<String, String> predefinedColumnMap = new HashMap<>();
	
	static {
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
		columnMap.put("Situation", 8);
		columnMap.put("Phenotype", 11);
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
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("output.owl"), 1024*1024*50);
		new Main().convert(file, writer);
		writer.close();
	}


	private void convert(File file, Writer writer) throws IOException {
		InputStream in = new FileInputStream(file);
		Workbook wb = new XSSFWorkbook(in);
		
		Sheet modelsSheet = wb.getSheet("owl-elements2");
		for (int modelRowIndex = 1; modelRowIndex < 27; modelRowIndex++) {
			Row modelRow = modelsSheet.getRow(modelRowIndex);
			writer.append(columnsJoinText(rowToList(modelRow)));
		}

		Sheet dataSheet = wb.getSheet("final-data");
		for (int dataRowIndex = 1; dataRowIndex < 48; dataRowIndex++) {
			Row dataRow = dataSheet.getRow(dataRowIndex);
			
			List<String> dataRowList = rowToList(dataRow);
			writer.append("\n    " + strComm("XLS line: " + dataRowList.toString()) + "\n");
			writer.append("   " + strComm("=========") + "\n");
			
			System.out.println("\n\n***** " + dataRowList.toString() + "\nCalculating rows...");
			Set<List<String>> rowRows = getRowRows(dataRowList);
			System.out.println("Writing rows...");
			int rowNum = 1;
			for (List<String> rowRow : rowRows) {
				writer.append("    " + strComm("Derived line (" + rowNum + "/" + rowRows.size() + "): " + rowRow.toString()) + "\n");
				for (int modelRowIndex = 27; modelRowIndex < 52; modelRowIndex++) {
					List<String> modelRow = rowToList(modelsSheet.getRow(modelRowIndex));
					writer.append(expandData(columnsJoinText(modelRow), rowRow));
				}
				System.out.print("\r" + rowNum + "/" + rowRows.size());
				rowNum++;
			}
			break;
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

	private Set<List<String>> getRowRows(List<String> row) {
		Set<List<String>> rowRows = new HashSet<>();
		for (int col = 0; col < row.size(); col++) {
			String colText = row.get(col);
			String[] elements = colText.split(";");
			if (elements.length > 1) {
				List<String> newRow = new ArrayList<>(row);
				newRow.set(col, colText.substring(colText.indexOf(";") + 1));
				rowRows.addAll(getRowRows(newRow));
				row.set(col, colText.substring(0, colText.indexOf(";")));
			}
		}
		rowRows.add(row);
		return rowRows;
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
				elementText = elementText.replace(columnKey, strNormalizedName(dataRow.get(columnMap.get(column))));
			}
		}
		for (String predefinedColumn : predefinedColumnMap.keySet()) {
			String predefinedColumnKey = formatColumnKey(predefinedColumn);
			if (elementText.contains(predefinedColumnKey)) {
				elementText = elementText.replace(predefinedColumnKey, strNormalizedName(predefinedColumnMap.get(predefinedColumn)));
			}
		}

		return elementText;
	}
	
	private String strComm(String text) {
		if (text == null || text.trim().length() == 0) {
			return "";
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
