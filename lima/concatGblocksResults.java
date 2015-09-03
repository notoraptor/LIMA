/*
Génère les séquences d'alignements concaténées pour des espèces présentes dans des groupes de gènes.
Le programme prend en entrée un dossier contenant des fichiers .fasta-gb (alignements au format FASTA filtrés avec le logiciel Gblocks) de groupes orthologues générés par OrthoMCL.
Chaque fichier .fasta-gb doit contenir au plus une séquence par espèce, et l'entête de la séquence doit avoir le format suivant :
	<nom de l'espèce>|<ID de la séquence>
Le programme génère un dossier de sortie contenant un fichier .concatenated.fasta pour chaque espèce rencontrée dans les groupes orthologues.
Le fichier .concatenated.fasta d'une espèce contient une unique séquence (entête ">nomEspece") dans laquelle toutes les séquences de l'espèce sont mises bout à bout, dans le même ordre pour toutes les espèces.
L'ordre est simplement l'ordre alphabétique des groupes orthologues.
Syntaxe :
	java maitrise.concatGblocksResults dossierAlignementsFastaGb dossierSortieAlignementsEspecesFasta
*/
package lima;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Enumeration;

public class concatGblocksResults {
	// Classement des séquences rencontrées par espèce puis par groupe (espèce (String) -> groupe orthologue (String) -> séquence (Sequence)).
	public static Hashtable<String, TreeMap<String, Sequence>> species = new Hashtable<String, TreeMap<String, Sequence>>();
	// Longueurs des alignements des groupes orthologues.
	public static TreeMap<String, Integer> alignLengths = new TreeMap<String, Integer>();

	// Met la séquence en mémoire dans le Hashtable species, et met aussi à jour le tableau des longueurs des alignements.
	public static void saveSequence(String groupName, String header, StringBuffer content) throws Exception {
		if(header != null) {
			int p = header.indexOf("|");
			if(p < 2 || p == header.length() - 1) throw new Exception("Impossible de reperer le nom de l'espece dans une sequence : " + header);
			String speciesName = header.substring(1, p);
			if(!species.containsKey(speciesName))
				species.put(speciesName, new TreeMap<String, Sequence>());
			TreeMap<String, Sequence> genes = species.get(speciesName);
			if(genes.containsKey(groupName)) throw new Exception("Groupe " + groupName + " deja vu pour l'espece " + speciesName + ".");
			genes.put(groupName, new Sequence(header, content));
			int length = content.length();
			if(!alignLengths.containsKey(groupName)) {
				alignLengths.put(groupName, new Integer(length));
			} else if(alignLengths.get(groupName).intValue() != length) {
				throw new Exception("Deux longueurs d'alignement differentes trouvees pour un meme groupe : " + length + " vs " + alignLengths.get(groupName).intValue());
			};
		};
	}

	public static void main(String[] args) {
		if(args.length == 2) {
			try {
				File inPath = new File(args[0]);
				File outPath = new File(args[1]);
				if(!inPath.isDirectory()) throw new Exception("Le parametre 1 n'est pas un dossier.");
				if(!outPath.mkdir()) throw new Exception("Impossible de creer le dossier de sortie.");
				// Mise en mémoire de séquences, rangées par espèce puis par groupe.
				File[] alignments = inPath.listFiles();
				String extension = ".fasta-gb";
				for(File alignment : alignments) if(alignment.isFile()) {
					String alignmentName = alignment.getName().toLowerCase();
					if(alignmentName.indexOf(extension) == alignmentName.length() - extension.length()) { 
						String groupName = alignmentName.substring(0, alignmentName.indexOf("."));
						BufferedReader alignmentFile = new BufferedReader(new FileReader(alignment.getAbsolutePath()));
						String header = null;
						StringBuffer content = new StringBuffer();
						String ligne = null;
						while((ligne = alignmentFile.readLine()) != null) {
							ligne = ligne.trim();
							if(!ligne.isEmpty()) {
								if(ligne.charAt(0) == '>') {
									saveSequence(groupName, header, content);
									header = ligne;
									content = new StringBuffer();
								} else {
									ligne = ligne.replace(" ","").replaceAll("\\*$","");
									content.append(ligne);
								};
							};
						};
						alignmentFile.close();
						saveSequence(groupName, header, content);
					};
				};
				// Si un groupe est absent dans une espèce, on met une séquence aussi longue que l'alignement du groupe absent.
				Enumeration<String> speciesNames = species.keys();
				while(speciesNames.hasMoreElements()) {
					String speciesName = speciesNames.nextElement();
					TreeMap<String, Sequence> genes = species.get(speciesName);
					for(String groupName : alignLengths.keySet()) if(!genes.containsKey(groupName)) {
						System.out.println(speciesName + " : pas de gene du groupe " + groupName + ".");
						int length = alignLengths.get(groupName).intValue();
						StringBuffer gap = new StringBuffer(length);
						for(int i = 0; i < length; ++i) gap.append('-');
						genes.put(groupName, new Sequence(">" + speciesName + "|" + groupName, gap));
					};
				};
				// Affichage de l'ordre de concaténation des gènes.
				// Il s'agit de l'ordre alphabétique des groupes.
				System.out.println("## Concatenation order.");
				System.out.println("#groupName\t#alignLength");
				long totalLength = 0;
				for(String groupName : alignLengths.keySet()) {
					int length = alignLengths.get(groupName).intValue();
					System.out.println(groupName + "\t" + length);
					totalLength += length;
				};
				System.out.println("#TOTAL\t" + totalLength);
				speciesNames = species.keys();
				// Concaténation proprement dite.
				while(speciesNames.hasMoreElements()) {
					String speciesName = speciesNames.nextElement();
					File speciesFilename = new File(outPath, speciesName + ".concatenated.fasta");
					BufferedWriter speciesFile = new BufferedWriter(new FileWriter(speciesFilename.getAbsolutePath()));
					String speciesHeader = ">" + speciesName;
					speciesFile.write(speciesHeader, 0, speciesHeader.length());
					speciesFile.newLine();
					TreeMap<String, Sequence> genes = species.get(speciesName);
					for(Sequence gene : genes.values()) {
						speciesFile.write(gene.content, 0, gene.content.length());
						speciesFile.newLine();
					};
					speciesFile.close();
				};
			} catch(Exception e) {
				e.printStackTrace();
			}
		};
	}
}