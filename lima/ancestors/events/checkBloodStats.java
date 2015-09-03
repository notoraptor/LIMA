package lima.ancestors.events;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
public class checkBloodStats {
	public static void main(String[] args) {
		try {
			BufferedReader file = new BufferedReader(new FileReader(args[0]));
			ArrayList<ArrayList<String>> columns = new ArrayList<ArrayList<String>>();
			String line = null;
			while((line = file.readLine()) != null) if((line.charAt(0) != '\t')) {
				String[] pieces = line.trim().split("\t");
				columns.add(new ArrayList<String>(Arrays.asList(pieces)));
			}
			file.close();
			if(!columns.isEmpty()) {
				int rowCount = columns.size();
				System.err.println(rowCount + " rows.");
				int colCount = columns.get(0).size();
				for(ArrayList<String> row: columns) if(row.size() != colCount) throw new Exception("columns count is not the same in all rows.");
				System.err.println(colCount + " columns.");
				TreeMap<String, TreeMap<String, Integer>> data = new TreeMap<String, TreeMap<String, Integer>>();
				ArrayList<String> header = columns.get(0);
				for(int i = 1; i < rowCount; ++i) {
					ArrayList<String> row = columns.get(i);
					String blood = row.get(0) + "->" + row.get(1);
					TreeMap<String, Integer> bloodStats = new TreeMap<String, Integer>();
					for(int j = 2; j < colCount-1; ++j) if(!header.get(j).equals("\"E+E/E+E\"")) {
						int value = Integer.parseInt(row.get(j));
						if(value > 1) bloodStats.put(header.get(j), value);
					}
					data.put(blood, bloodStats);
				}
				for(String blood: data.keySet()) {
					TreeMap<String, Integer> stats = data.get(blood);
					System.out.print("#blood");
					for(String type: stats.keySet()) System.out.print("\t"+type);
					System.out.println();
					System.out.print(blood);
					for(int value: stats.values()) System.out.print("\t" + value);
					System.out.println();
					System.out.println();
					BufferedWriter outFile = new BufferedWriter(new FileWriter(blood.replace("->","_to_") + ".csv"));
					StringBuilder l = new StringBuilder();
					l.append("event;count");
					outFile.write(l.toString(), 0, l.length());
					outFile.newLine();
					for(String type: stats.keySet()) {
						int value = stats.get(type);
						l = new StringBuilder();
						l.append("\"" + type + "\";" + value);
						outFile.write(l.toString(), 0, l.length());
						outFile.newLine();
					}
					outFile.close();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}