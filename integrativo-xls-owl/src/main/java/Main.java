import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		
		FileWriter writer = new FileWriter("output.owl");
		writer.append(new Main().convert(file));
		writer.close();
	}


	private String convert(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		Workbook wb = new XSSFWorkbook(in);
		StringBuilder out = new StringBuilder();
		
		Sheet elementsSheet = wb.getSheet("owl-elements2");
		for (int elementRowIndex = 1; elementRowIndex < 27; elementRowIndex++) {
			Row elementRow = elementsSheet.getRow(elementRowIndex);
			out.append(columnsJoinText(elementRow));
		}

		Sheet dataSheet = wb.getSheet("final-data");
		for (int dataRowIndex = 1; dataRowIndex < 48; dataRowIndex++) {
			Row dataRow = dataSheet.getRow(dataRowIndex);
			String[] dataRowArray = convertToDataRowArray(dataRow);
			
//			for (int elementRowIndex = 27; elementRowIndex < 52; elementRowIndex++) {
//				Row elementRow = elementsSheet.getRow(elementRowIndex);
//				out.append(expandData(columnsJoinText(elementRow), dataRow));
//			}
		}
		
		Row lastElementRow = elementsSheet.getRow(elementsSheet.getLastRowNum());
		out.append(columnsJoinText(lastElementRow));
		
		wb.close();
		
		return out.toString();
	}

	private List<String[]> listDataRowArray(Row dataRow) {
		String[][] dataRowArray = new String[12][];
		for (int column = 0; column < 12; column++) {
			String cellText = dataRow.getCell(column).getStringCellValue();
			dataRowArray[column] = cellText.split(";");
		}
		List<String[]> list = new ArrayList<>();
		appendDataRowArrayToList(dataRowArray, list);
		return list;
	}

	private void appendDataRowArrayToList(String[][] dataRowArray, List<String[]> list) {
		
	}
	
	private String columnsJoinText(Row elementRow) {
		StringBuilder out = new StringBuilder();
//		out.append("\n");
//		out.append(strComm(elementRow.getCell(0).getStringCellValue()));
//		out.append("\n");
		out.append(elementRow.getCell(1).getStringCellValue());
		out.append("\n");
		return out.toString();
	}
	
	private String formatColumnKey(String column) {
		return "$" + column + "$";
	}
	
	private String expandData(String elementText, Row dataRow) {
		for (String column : columnMap.keySet()) {
			String columnKey = formatColumnKey(column);
			if (elementText.contains(columnKey)) {
				elementText = elementText.replace(columnKey, dataRow.getCell(columnMap.get(column)).getStringCellValue());
			}
		}
		for (String predefinedColumn : predefinedColumnMap.keySet()) {
			String predefinedColumnKey = formatColumnKey(predefinedColumn);
			if (elementText.contains(predefinedColumnKey)) {
				elementText = elementText.replace(predefinedColumnKey, predefinedColumnMap.get(predefinedColumn));
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
