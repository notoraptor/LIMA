/*
Syntaxe :
	java maitrise.FisherTestOnMarkedAlignments dossierAlignementsMarqués longueurFenetre > rapport.txt
Description :
	Le programme analyse des fichiers .aligned.marked.fasta dans un dossier et calcule le nombre de fenêtres :
		- qui ne contiennt rien (ni trous ni introns).
		- qui contiennent uniquement des trous.
		- qui contiennent uniquement des introns.
		- qui contiennent des trous et des introns.
	La longueur des fenêtres considérées est spécifiable dans le deuxième paramètre.
	Les fenêtres considérées sont disjointes dans les alignements.
*/
package lima;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
class IntronPosition {
	public int position;
	public boolean internal;
	public IntronPosition(int p, boolean i) {
		position = p;
		internal = i;
	}
}
class Alignment {
	public int length;
	public Alignment(int l) {
		length = l;
	}
	public void checkLength(StringBuffer content) throws Exception {
		int contentLength = content.length();
		if(length == 0) length = contentLength;
		else if(length != contentLength) throw new Exception("alignement non coherent.");
	}
}
public class FisherTestOnMarkedAlignments {
	public static final int NOTHING    = 0;	//00
	public static final int ONLYGAP    = 1;	//01
	public static final int ONLYINTRON = 2;	//10
	public static final int GAPINTRON  = 3;	//11
	public static final int casesCount = 4;
	/*
	La fonction suivante formate une séquence alignée.
	Les acides aminés qui contiennt des introns (introns en phase 1 ou 2) sont remplacés par la lettre 'i'.
	Les acides aminés qui précèdent des introns (introns en phase 3) sont remplacés par 'I'.
	Tous les autres acides aminés sont remplacés par 'X'.
	Les trous ('-') ne sont pas modifiés.
	*/
	public static void formatSequence(String header, StringBuffer content) throws Exception {
		String s1 = "{i ";
		String s3 = " i}";
		int s1Position = header.indexOf(s1);
		int s3Position = header.indexOf(s3);
		if(s1Position >= 0 && s3Position > s1Position + s1.length()) {
			Hashtable<Integer, IntronPosition> intronsPositions = new Hashtable<Integer, IntronPosition>();
			String[] pieces = header.substring(s1Position + s1.length(), s3Position).trim().split(",");
			for(String piece : pieces) if(!pieces.equals("00")) {
				int value = Integer.parseInt(piece);
				boolean internal = (value % 3 != 0);
				int position = (value / 3) + (internal ? 1 : 0);
				intronsPositions.put(new Integer(position), new IntronPosition(position, internal));
			};
			int sequenceLength = content.length();
			int aaCount = 0;
			for(int i = 0; i < sequenceLength; ++i) {
				if(content.charAt(i) != '-') {
					++aaCount;
					int position =  new Integer(aaCount);
					if(intronsPositions.containsKey(position)) {
						if(intronsPositions.get(position).internal)
							content.setCharAt(i, 'i');
						else
							content.setCharAt(i, 'I');
					} else {
						content.setCharAt(i, 'X');
					};
				};
			};
		} else throw new Exception("Sequence non marquee rencontree.");
	};
	public static void checkCases(
		ArrayList<Sequence> sequences, Alignment alignment, int[] cases, int windowLength, int[] intronsCounts
	) {
		boolean withGap = false;
		boolean withIntron = false;
		int intronsCount = 0;
		for(int i = 0; i < alignment.length; ++i) {
			if(i != 0 && i % windowLength == 0) {
				int type = -1;
				if(!withIntron && !withGap) type = NOTHING;
				else if(!withIntron &&  withGap) type = ONLYGAP;
				else if( withIntron && !withGap) type = ONLYINTRON;
				else type = GAPINTRON;
				++cases[type];
				intronsCounts[type] += intronsCount;
				withGap = false;
				withIntron = false;
				intronsCount = 0;
			};
			for(Sequence sequence : sequences) {
				char c = sequence.content.charAt(i);
				if(c == '-') withGap = true;
				else if(c == 'i' || c == 'I') {
					withIntron = true;
					++intronsCount;
				};
			};
		};
		int type = -1;
		if(!withIntron && !withGap) type = NOTHING;
		else if(!withIntron &&  withGap) type = ONLYGAP;
		else if( withIntron && !withGap) type = ONLYINTRON;
		else type = GAPINTRON;
		++cases[type];
		intronsCounts[type] += intronsCount;
	}
	public static void main(String[] args) {
		if(args.length == 2) try {
			File dir = new File(args[0]);
			int windowLength = Integer.parseInt(args[1]);
			if(!dir.isDirectory()) throw new Exception("Le parametre 1 n'est pas un dossier.");
			if(windowLength < 1) throw new Exception("Le parametre 2 doit etre un entier naturel non nul.");
			String extension = ".aligned.marked.fasta";
			int filesCount = 0;
			int[] cases = new int[casesCount];
			int[] intronsCounts = new int[casesCount];
			for(File path : dir.listFiles()) if(path.isFile()) {
				String filename = path.getName().toLowerCase();
				if(filename.indexOf(extension) == filename.length() - extension.length()) {
					// Fichier d'alignement marqué détecté.
					ArrayList<Sequence> sequences = new ArrayList<Sequence>();
					Alignment alignment = new Alignment(0);
					BufferedReader file = new BufferedReader(new FileReader(path.getAbsolutePath()));
					try {
						String header = null;
						StringBuffer content = new StringBuffer();
						String line = null;
						while((line = file.readLine()) != null) if(!line.isEmpty()) {
							line = line.trim();
							if(!line.isEmpty()) if(line.charAt(0) == '>') {
								if(header != null && content.length() != 0) {
									alignment.checkLength(content);
									formatSequence(header, content);
									sequences.add(new Sequence(header, content));
								};
								header = line;
								content = new StringBuffer();
							} else {
								content.append(line);
							};
						};
						if(header != null && content.length() != 0) {
							alignment.checkLength(content);
							formatSequence(header, content);
							sequences.add(new Sequence(header, content));
						};
						++filesCount;
					} finally {
						file.close();
					};
					checkCases(sequences, alignment, cases, windowLength, intronsCounts);
					/*
					// Debug : Affichage des séquences formatées.
					for(Sequence s : sequences) {
						System.err.println(s.header);
						System.err.println(s.content);
					};
					*/
					if(filesCount % 500 == 0) System.err.println(filesCount + " fichiers traitEs.");
				};
			};
			System.err.println(filesCount + " fichiers finalement traitEs.");
			System.out.println("##filesChecked\t" + filesCount);
			System.out.println("##windowLength\t" + windowLength);
			System.out.println("#EMPTY\tGAP\tINTRON\tGAPINTRON");
			System.out.println(cases[NOTHING] + "\t" + cases[ONLYGAP] + "\t" + cases[ONLYINTRON] + "\t" + cases[GAPINTRON]);
			System.out.println(intronsCounts[NOTHING] + "\t" + intronsCounts[ONLYGAP] + "\t" + intronsCounts[ONLYINTRON] + "\t" + intronsCounts[GAPINTRON]);
			System.out.println();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
