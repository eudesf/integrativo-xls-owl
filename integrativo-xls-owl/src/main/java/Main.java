import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main {

	private static Map<String, Integer> columnNameMap = new HashMap<>();
	
	static {
		columnNameMap.put("Protein", 2);
		columnNameMap.put("Gene", 3);
		columnNameMap.put("Organism", 4);
		columnNameMap.put("Org", 4);
		columnNameMap.put("BioProcess", 5);
		columnNameMap.put("BiologicalProcess", 5);
		columnNameMap.put("MolecularFunction", 6);
		columnNameMap.put("MolFunc", 6);
		columnNameMap.put("CellComponent", 7);
		columnNameMap.put("Comp", 7);
		columnNameMap.put("Situation", 8);
		columnNameMap.put("Phenotype", 11);
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
			for (int elementRowIndex = 27; elementRowIndex < 52; elementRowIndex++) {
				Row elementRow = elementsSheet.getRow(elementRowIndex);
				out.append(expandData(columnsJoinText(elementRow), dataRow));
			}
		}
		
		Row lastElementRow = elementsSheet.getRow(elementsSheet.getLastRowNum());
		out.append(columnsJoinText(lastElementRow));
		
		wb.close();
		
		return out.toString();
	}

	private String columnsJoinText(Row elementRow) {
		StringBuilder out = new StringBuilder();
		out.append("\n");
		out.append(strComm(elementRow.getCell(0).getStringCellValue()));
		out.append("\n");
		out.append(elementRow.getCell(1).getStringCellValue());
		out.append("\n");
		return out.toString();
	}
	
	private String expandData(String elementText, Row dataRow) {
		for (String columnName : columnNameMap.keySet()) {
			String columnNameKey = "$" + columnName + "$";
			if (elementText.contains(columnNameKey)) {
				elementText = elementText.replace(columnNameKey, dataRow.getCell(columnNameMap.get(columnName)).getStringCellValue());
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
