package lima.ancestors.events;

import lima.tools.ProgramBase;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

//Classe simple qui représente un intervalle [a,b].
class Interval implements Comparable<Interval> {
	public int a;
	public int b;
	public Interval(int x, int y) {
		a = x;
		b = y;
	}
	public int compareTo(Interval o) {
		int t = a - o.a;
		if(t == 0) t = b - o.b;
		return t;
	}
}

// Classe qui représente une colonne dans un alignement simple.
class Column {
	public static double p = 1;
	public static double q = 0.5;
	public char ca;
	public char cd;
	public char oldCA;
	public Column(char c, char d) {
		ca = c;
		cd = d;
		oldCA = ca;
		if(ca != '.' && ca != '+' && ca != '-') ca = 'E';
		if(cd != '.' && cd != '+' && cd != '-') {
			if(ca == '.' || ca == '+' || ca == '-') cd = 'E';
			else if(cd == oldCA) cd = 'E';
			else cd = 'F';
		};
	}
	public boolean withIntron() {
		return (
			(ca == '.' && cd == '+') ||
			(ca == '+' && cd == '.') ||
			(ca == '+' && cd == '+')
		);
	}
	public boolean withError() {
		return (
			(ca == '.' && cd == '-') ||
			(ca == '.' && cd == 'E') ||
			(ca == '+' && cd == '-') ||
			(ca == '+' && cd == 'E') ||
			(ca == '-' && cd == '.') ||
			(ca == '-' && cd == '+') ||
			(ca == 'E' && cd == '.') ||
			(ca == 'E' && cd == '+')
		);
	}
	public boolean useless() {
		return (
			(ca == '.' && cd == '.') ||
			(ca == '-' && cd == '-')
		);
	}
	public boolean unstable() {
		return (
			(ca == '.' && cd == '+') ||
			(ca == '+' && cd == '.') ||
			(ca == '+' && cd == '+') ||
			(ca == '-' && cd == 'E') ||
			(ca == 'E' && cd == '-')
		);
	}
	public boolean sameAs(Column c) {
		char xa = ca;
		char xd = cd;
		char oa = c.ca;
		char od = c.cd;
		if(xd == 'F') xd = 'E';
		if(od == 'F') od = 'E';
		return (xa == oa && xd == od);
	}
	public double weight() throws Exception {
		double w = 0;
		if(ca == 'E' && cd == 'E')
			w = 1 - q;
		else if(ca == 'E' && cd == 'F')
			w = (2 - q)/2;
		else if(ca == 'E' && cd == '-')
			w = p;
		else if(ca == '-' && cd == 'E')
			w = p;
		else if(ca == '+' && cd == '+')
			w = 2*p;
		else if(ca == '+' && cd == '.')
			w = 4*p;
		else if(ca == '.' && cd == '+')
			w = 4*p;
		else throw new Exception("A column (" + ca + "/" + cd + ") has an undetermined weight.");
		return w;
	}
	public double applyPerturbation(double oldWeight) throws Exception {
		double w = oldWeight;
		if(unstable()) w = oldWeight + weight();
		else w = oldWeight*weight();
		return w;
	}
}

class MyInteger implements Comparable<MyInteger> {
	public int value;
	public MyInteger() { value = 0; }
	public MyInteger(int v) { value = v; }
	public void increment() { ++value; }
	public int compareTo(MyInteger other) { return other.value - value; }
	public boolean equals(Object o) { return o instanceof MyInteger && value == ((MyInteger)o).value; }
}

// Classe qui détecte les évènements évolutifs autour des introns dans les alignements ancêtre-descendant de fichiers PCA (Parent-Ancestor Alignments).
public class Detect {
	public static final String extension = ".pca";
	public static ProgramBase base = null;
	public static TreeMap<String, MyInteger> types = null;
	public static long eventsCount = 0;
	public static TreeSet<String> filters = new TreeSet<String>();
	// Structure qui permet de compter chaque type d'évènement dans chaque couple (ancêtre; descendant).
	// TreeMap<String ancêtre, TreeMap<String descendant, TreeMap<String type, MyInteger compte>>>
	public static TreeMap<String, TreeMap<String, TreeMap<String, MyInteger>>> typesCountByBlood = null;
	public static void detectEvents(
		String family, String ancestor, String descendant, String seqAncestor, String seqDescendant,
		BufferedWriter instabilityFile, BufferedWriter eventsFile
	) throws Exception {
		int seqLength = seqAncestor.length();
		ArrayList<Interval> intervals = new ArrayList<Interval>();
		for(int cursor = 0; cursor < seqLength; ++cursor) {
			Column column = new Column(seqAncestor.charAt(cursor), seqDescendant.charAt(cursor));
			if(column.withIntron()) {
				double weightBack = column.weight();
				double weightForward = weightBack;
				int x = 0; int y = 0;
				for(x = cursor - 1; (int)weightBack > 0 && x >= 0; --x) {
					Column c = new Column(seqAncestor.charAt(x), seqDescendant.charAt(x));
					if(c.withError()) throw new Exception("Unexpected column (" + c.ca + "/" + c.cd + ")");
					else if(!c.useless()) weightBack = c.applyPerturbation(weightBack);
				}
				for(y = cursor + 1; (int)weightForward > 0 && y < seqLength; ++y) {
					Column c = new Column(seqAncestor.charAt(y), seqDescendant.charAt(y));
					if(c.withError()) throw new Exception("Unexpected column (" + c.ca + "/" + c.cd + ")");
					else if(!c.useless()) weightForward = c.applyPerturbation(weightForward);
				}
				++x; --y;
				intervals.add(new Interval(x,y));
			}
		}
		// Traitement des intervalles.
		if(!intervals.isEmpty()) {
			Collections.sort(intervals);
			ArrayList<Interval> postIntervals = new ArrayList<Interval>(intervals.size());
			postIntervals.add(intervals.get(0));
			int intervalsCount = intervals.size();
			for(int i = 1; i < intervalsCount; ++i) {
				Interval interval = intervals.get(i);
				Interval previous = postIntervals.get(postIntervals.size() - 1);
				if(interval.a <= previous.b) {
					if(previous.b < interval.b) previous.b = interval.b;
				} else postIntervals.add(interval);
			}
			// Intervalles traités.
			// Écriture de l'alignement avec mise en évidence des zones instables découvertes, dans le fichier .instability .
			StringBuffer instability = new StringBuffer();
			for(int i = 0; i < seqLength; ++i) instability.append(' ');
			for(Interval interval : postIntervals) {
				for(int j = interval.a; j <= interval.b; ++j) instability.setCharAt(j, '*');
			}
			String outputAncestorName = ancestor;
			String outputDescendantName = "  " + descendant;
			String outputInstabilityName = "";
			int initialLengthOuputAncestorName = outputAncestorName.length();
			int initialLengthOutputDescendantName = outputDescendantName.length();
			int maxOutputNameLength = Math.max(initialLengthOuputAncestorName, initialLengthOutputDescendantName);
			for(int i = 0; i < maxOutputNameLength; ++i) outputInstabilityName += ' ';
			for(int i = 0; i < maxOutputNameLength - initialLengthOuputAncestorName; ++i) outputAncestorName += ' ';
			for(int i = 0; i < maxOutputNameLength - initialLengthOutputDescendantName; ++i) outputDescendantName += ' ';
			String outputLine1 = "\t" + outputAncestorName + "\t" + seqAncestor;
			String outputLine2 = "\t" + outputDescendantName + "\t" + seqDescendant;
			String outputLine3 = "\t" + outputInstabilityName + "\t" + instability.toString();
			instabilityFile.write(family, 0, family.length());
			instabilityFile.newLine();
			instabilityFile.write(outputLine1, 0, outputLine1.length());
			instabilityFile.newLine();
			instabilityFile.write(outputLine2, 0, outputLine2.length());
			instabilityFile.newLine();
			instabilityFile.write(outputLine3, 0, outputLine3.length());
			instabilityFile.newLine();
			// Chaque évènement est donc actuellement défini par un intervalle dans le tableau postIntervals.
			// Typage des évènements.
			for(Interval interval : postIntervals) {
				ArrayList<Column> columns = new ArrayList<Column>();
				String eventA = seqAncestor.substring(interval.a, interval.b + 1);
				String eventD = seqDescendant.substring(interval.a, interval.b + 1);
				int eventLength = interval.b - interval.a + 1;
				for(int i = 0; i < eventLength; ++i) {
					Column c = new Column(eventA.charAt(i), eventD.charAt(i));
					if(!c.useless()) columns.add(c);
				}
				if(!columns.isEmpty()) {
					ArrayList<Column> type = new ArrayList<Column>();
					type.add(columns.get(0));
					int columnsCount = columns.size();
					for(int i = 1; i < columnsCount; ++i) {
						Column c = columns.get(i);
						Column cp = type.get(type.size() - 1);
						if(c.withIntron()) type.add(c);
						else if(!c.sameAs(cp)) type.add(c);
					}
					// Typage effectué.
					// Affichage des informations sur cet évènement.
					String typeA = "";
					String typeD = "";
					for(Column c : type) {
						char xa = c.ca;
						char xd = c.cd;
						if(xd == 'F') xd = 'E';
						typeA += xa;
						typeD += xd;
					}
					// Incrémentation du nombre d'évènements détectés pour ce type et du nombre total d'évènements détectés.
					String typeString = typeA + "/" + typeD;
					if(!types.containsKey(typeString)) types.put(typeString, new MyInteger());
					types.get(typeString).increment();
					++eventsCount;
					// --
					// Prise en compte de cet évènement dans la table de compte des types d'évènements par lignée.
					if(!typesCountByBlood.containsKey(ancestor))
							typesCountByBlood.put(ancestor, new TreeMap<String, TreeMap<String, MyInteger>>());
					if(!typesCountByBlood.get(ancestor).containsKey(descendant))
							typesCountByBlood.get(ancestor).put(descendant, new TreeMap<String, MyInteger>());
					if(!typesCountByBlood.get(ancestor).get(descendant).containsKey(typeString))
							typesCountByBlood.get(ancestor).get(descendant).put(typeString, new MyInteger(0));
					typesCountByBlood.get(ancestor).get(descendant).get(typeString).increment();
					// ...
					String outputLine4 = "@" + family + "\t" + ancestor + "\t" + descendant + "\t" + seqLength + "\t" + (interval.a + 1) + "\t" + (interval.b + 1) + "\t\t\t" + typeA + "/" + typeD + "\t\t\t" + eventA + "/" + eventD;
					String outputLine5 = "\t" + typeA + "\t\t\t" + eventA;
					String outputLine6 = "\t" + typeD + "\t\t\t" + eventD;
					eventsFile.write(outputLine4, 0, outputLine4.length());
					eventsFile.newLine();
					eventsFile.write(outputLine5, 0, outputLine5.length());
					eventsFile.newLine();
					eventsFile.write(outputLine6, 0, outputLine6.length());
					eventsFile.newLine();
				}
			}
		}
	}
	public static void parsePCA(File file) throws Exception {
		String filename = file.getName().toLowerCase();
		String outputFilename = filename.substring(0, filename.indexOf(extension));
		File outputInstabilityFile = new File(outputFilename + ".instability");
		File outputEventsFile = new File(outputFilename + ".events");
		BufferedWriter instabilityFile = new BufferedWriter(new FileWriter(outputInstabilityFile.getAbsolutePath()));
		BufferedWriter eventsFile = new BufferedWriter(new FileWriter(outputEventsFile.getAbsolutePath()));
		// Écriture de l'entête dans eventsFile.
		String outputLineHeader1 = "#family\tancestor\tdescendant\tseqLength\tfrom\tto\t\t\ttype(A/D)\t\t\tevent(A/D)";
		String outputLineHeader2 = "#\ttypeA\t\t\teventA";
		String outputLineHeader3 = "#\ttypeD\t\t\teventD";
		eventsFile.write(outputLineHeader1, 0, outputLineHeader1.length());
		eventsFile.newLine();
		eventsFile.write(outputLineHeader2, 0, outputLineHeader2.length());
		eventsFile.newLine();
		eventsFile.write(outputLineHeader3, 0, outputLineHeader3.length());
		eventsFile.newLine();
		eventsFile.newLine();
		// --
		String family = filename.substring(0, filename.indexOf("."));
		BufferedReader pca = new BufferedReader(new FileReader(file.getAbsolutePath()));
		int lineCount = 0;
		String line = null;
		String[] lines = new String[4];
		try {
			while((line = pca.readLine()) != null) {
				lines[lineCount++] = line;
				if(lineCount == 4) {
					lineCount = 0;
					String[] m1 = lines[0].split("\t");
					String[] m2 = lines[1].split("\t");
					String[] m3 = lines[2].split("\t");
					String ancestor = m1[0].trim();
					String descendant1 = m2[0].trim();
					String descendant2 = m3[0].trim();
					String seqAncestor = m1[1].trim();
					String seqDescendant1 = m2[1].trim();
					String seqDescendant2 = m3[1].trim();
					detectEvents(family, ancestor, descendant1, seqAncestor, seqDescendant1, instabilityFile, eventsFile);
					detectEvents(family, ancestor, descendant2, seqAncestor, seqDescendant2, instabilityFile, eventsFile);
				}
			}
		} catch(Exception e) {
			throw e;
		} finally {
			pca.close();
			instabilityFile.close();
			eventsFile.close();
		}
	}
	public static void writeTypesInfos() throws Exception {
		if(!types.isEmpty()) {
			BufferedWriter typesFileSortedByType = new BufferedWriter(new FileWriter("events.types.counted.sortedByType"));
			BufferedWriter typesFileSortedByCount = new BufferedWriter(new FileWriter("events.types.counted.sortedByCount"));
			TreeMap<MyInteger, TreeSet<String>> typesSortedByCount = new TreeMap<MyInteger, TreeSet<String>>();
			ArrayList<String> typesInfo = new ArrayList<String>();
			typesInfo.add("@eventsCount\t" + eventsCount);
			typesInfo.add("@typeCount\t" + types.size());
			for(String type : types.keySet()) {
				MyInteger count =  types.get(type);
				if(!typesSortedByCount.containsKey(count)) typesSortedByCount.put(count, new TreeSet<String>());
				typesSortedByCount.get(count).add(type);
			}
			try {
				for(String output : typesInfo) {
					typesFileSortedByType.write(output, 0, output.length());
					typesFileSortedByType.newLine();
					typesFileSortedByCount.write(output, 0, output.length());
					typesFileSortedByCount.newLine();
				}
				for(String type : types.keySet()) {
					MyInteger count = types.get(type);
					String output = type + "\t" + count.value;
					typesFileSortedByType.write(output, 0, output.length());
					typesFileSortedByType.newLine();
				}
				for(MyInteger count : typesSortedByCount.keySet()) for(String type : typesSortedByCount.get(count)) {
					String output = type + "\t" + count.value;
					typesFileSortedByCount.write(output, 0, output.length());
					typesFileSortedByCount.newLine();
				}
			} catch(Exception e) {
				throw e;
			} finally {
				typesFileSortedByType.close();
				typesFileSortedByCount.close();
			}
			// Affichage du tableau de compte des types par lignée.
			BufferedWriter bloodTable = new BufferedWriter(new FileWriter("events.types.counted.sortedByBlood.tsv"));
			try {
				// Écriture de l'entête.
				StringBuffer output = new StringBuffer();
				output.append("#ancestor\tdescendant\t");
				for(String typeName: types.keySet()) output.append("\"" + typeName + "\"\t");
				output.append("TOTAL");
				bloodTable.write(output.toString(), 0, output.length());
				bloodTable.newLine();
				MyInteger zero = new MyInteger(0);
				// Écriture de chaque ligne.
				for(String ancestor: typesCountByBlood.keySet()) {
					for(String descendant: typesCountByBlood.get(ancestor).keySet()) {
						StringBuffer lineOutput = new StringBuffer();
						long lineCount = 0;
						lineOutput.append(ancestor + "\t" + descendant + "\t");
						for(String typeName: types.keySet()) {
							MyInteger count = typesCountByBlood.get(ancestor).get(descendant).get(typeName);
							if(count == null) count = zero;
							lineOutput.append(count.value + "\t");
							lineCount += count.value;
						}
						lineOutput.append(lineCount);
						bloodTable.write(lineOutput.toString(), 0, lineOutput.length());
						bloodTable.newLine();
					}
				}
				// Écriture des comptes totaux par colonne.
				StringBuffer lastLine = new StringBuffer();
				lastLine.append("\t\t");
				for(MyInteger count: types.values()) lastLine.append(count.value + "\t");
				lastLine.append(eventsCount);
				bloodTable.write(lastLine.toString(), 0, lastLine.length());
				bloodTable.newLine();
			} catch(Exception e) {
				throw e;
			} finally {
				bloodTable.close();
			}
			// Écriture de la table avec seulement les types qui apparaissent dans le filtre.
			if(!filters.isEmpty()) {
				BufferedWriter filteredTable = new BufferedWriter(new FileWriter("events.types.filtered.counted.sortedByBlood.tsv"));
				try {
					// Écriture de l'entête.
					StringBuffer output = new StringBuffer();
					output.append("#ancestor\tdescendant\t");
					for(String typeName: filters) output.append("\"" + typeName + "\"\t");
					output.append("TOTAL");
					filteredTable.write(output.toString(), 0, output.length());
					filteredTable.newLine();
					MyInteger zero = new MyInteger(0);
					// Écriture de chaque ligne.
					for(String ancestor: typesCountByBlood.keySet()) {
						for(String descendant: typesCountByBlood.get(ancestor).keySet()) {
							StringBuffer lineOutput = new StringBuffer();
							long lineCount = 0;
							lineOutput.append(ancestor + "\t" + descendant + "\t");
							for(String typeName: filters) {
								MyInteger count = typesCountByBlood.get(ancestor).get(descendant).get(typeName);
								if(count == null) count = zero;
								lineOutput.append(count.value + "\t");
								lineCount += count.value;
							}
							lineOutput.append(lineCount);
							filteredTable.write(lineOutput.toString(), 0, lineOutput.length());
							filteredTable.newLine();
						}
					}
					// Écriture des comptes totaux par colonne.
					StringBuffer lastLine = new StringBuffer();
					lastLine.append("\t\t");
					long total = 0;
					for(String typeName: filters) {
						MyInteger count = types.get(typeName);
						lastLine.append(count.value + "\t");
						total += count.value;
					}
					lastLine.append(total);
					filteredTable.write(lastLine.toString(), 0, lastLine.length());
					filteredTable.newLine();
				} catch(Exception e) {
					throw e;
				} finally {
					filteredTable.close();
				}
			}
		}
	}
	public static void main(String[] args) {
		if(args.length > 0) try {
			base = new ProgramBase(args, "path", "p", "q", "typeFilter");
			// Récupération des arguments.
			if(!base.hasParameter("path")) throw new Exception("Parameter \"path\" is required.");
			File path = new File(base.parameter("path"));
			if(base.hasParameter("p")) {
				Column.p = Double.parseDouble(base.parameter("p"));
			}
			if(base.hasParameter("q")) {
				double q = Double.parseDouble(base.parameter("q"));
				if(q < 0 || q > 1) throw new Exception("Parameter \"q\" must be in [0;1] interval.");
				Column.q = q;
			}
			if(base.hasParameter("typeFilter")) {
				BufferedReader typeFilterFile = new BufferedReader(new FileReader(base.parameter("typeFilter")));
				String line = null;
				while((line = typeFilterFile.readLine()) != null) {
					line = line.trim();
					if(!line.isEmpty() && line.charAt(0) != '#') filters.add(line);
				}
				typeFilterFile.close();
			}
			base.log(base.toString());
			//---
			types = new TreeMap<String, MyInteger>();
			typesCountByBlood = new TreeMap<String, TreeMap<String, TreeMap<String, MyInteger>>>();
			if(path.isFile()) {
				String filename = path.getName().toLowerCase();
				if(filename.indexOf(extension) != filename.length() - extension.length())
					throw new Exception("File must have '.pca' extension.");
				parsePCA(path);
			} else if(path.isDirectory()) {
				int filesCount = 0;
				for(File file : path.listFiles()) if(file.isFile()) {
					String filename = file.getName().toLowerCase();
					if(filename.indexOf(extension) == filename.length() - extension.length()) {
						++filesCount;
						parsePCA(file);
						if(filesCount % 100 == 0) base.logfile.println(filesCount + " PCA files read.");
					}
				}
				base.logfile.println(filesCount + " PCA files read.");
			} else throw new Exception("Exception: path is not a file nor a directory.");
			writeTypesInfos();
			base.end();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
