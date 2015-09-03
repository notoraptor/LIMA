/*
java maitrise.intronsLengthsBySpecies intronsDir outputDir
*/
package lima;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.TreeMap;

public class intronsLengthsBySpecies {
	public static void main(String[] args) {
		if(args.length == 2) {
			try {
				File intronsDir = new File(args[0]);
				File outDir = new File(args[1]);
				if(!intronsDir.isDirectory())
					throw new Exception("Le parametre 1 doit indiquer un dossier contenant des fichiers .introns .");
				if(!outDir.mkdir())
					throw new Exception("Impossible de creer le dossier de sortie (parametre 2).");
				String extension = ".introns";
				File[] intronsFilenames = intronsDir.listFiles();
				System.out.println("#species\tcount\tmin\tmax\tmean\tmostViewed\tmostViewedCount");
				for(File intronsFilename : intronsFilenames) if(intronsFilename.isFile()) {
					String filename = intronsFilename.getName().toLowerCase();
					if(filename.indexOf(extension) == filename.length() - extension.length()) {
						// Fichier .introns trouvé, lecture.
						TreeMap<Integer, Integer> lengths = new TreeMap<Integer, Integer>();
						BufferedReader file = new BufferedReader(new FileReader(intronsFilename.getAbsolutePath()));
						String line = null;
						try {
							while((line = file.readLine()) != null) if(!line.isEmpty() && line.charAt(0) != '#') {
								line = line.trim();
								if(!line.isEmpty()) {
									String[] pieces = line.split("\t");
									if(pieces.length != 8)
										throw new Exception("Fichier d'introns : " + intronsFilename.getName() + " : une ligne ne contient pas exactement 8 colonnes :\r\n" + line);
									if(pieces[0].trim().equals("CDS") && !pieces[7].trim().equals("00")) {
										String[] values = pieces[7].trim().split(",");
										for(String value : values) {
											Integer length = new Integer(Integer.parseInt(value));
											if(!lengths.containsKey(length))
												lengths.put(length, new Integer(1));
											else {
												int newValue = lengths.get(length).intValue() + 1;
												lengths.put(length, new Integer(newValue));
											};
										};
									};
								};
							};
						} finally {
							file.close();
						};
						// Longueurs comptées. Ecriture dans un nouveau fichier.
						String name = filename.substring(0, filename.length() - extension.length());
						File outFilename = new File(outDir, filename + ".lengths");
						BufferedWriter outFile = new BufferedWriter(new FileWriter(outFilename.getAbsolutePath()));
						long totalLength = 0;
						long totalCount = 0;
						long mostCommonLength = 0;
						long mostCommonCount = 0;
						try {
							String header = "#length\tcount";
							outFile.write(header, 0, header.length());
							outFile.newLine();
							for(Integer length : lengths.keySet()) {
								long lengthValue = length.longValue();
								long lengthCount = lengths.get(length).longValue();
								totalCount += lengthCount;
								totalLength += lengthValue*lengthCount;
								if(mostCommonCount < lengthCount) {
									mostCommonLength = lengthValue;
									mostCommonCount = lengthCount;
								};
								String info = lengthValue + "\t" + lengthCount;
								outFile.write(info, 0, info.length());
								outFile.newLine();
							};
						} finally {
							outFile.close();
						};
						System.out.println(name + "\t" + totalCount + "\t" + lengths.firstKey().intValue() + "\t" + lengths.lastKey().intValue() + "\t" + ((double)totalLength/totalCount) + "\t" + mostCommonLength + "\t" + mostCommonCount);
					};
				};
			} catch(Exception e) {
				e.printStackTrace();
			};
		};
	}
}