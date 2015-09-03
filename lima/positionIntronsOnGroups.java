/*
Génère des fichiers compatibles avec le logiciel MALIN en placant les informations sur les introns dans les entêtes des séquences.
Informations rajoutées dans les entêtes des séquences :
	/organism=<nom de l'espèce> {i p1,p2,...,pN i} /intron-length=l1,l2,...,lN
		pi = position du i-ième intron dans la séquence codante (CDS, en nucléotides) associée à la protéine.
		li = longueur du i-ième intron en nucléotides.
Syntaxe
	java maitrise.positionIntronsOnGroups intronsDir groupsDir outDir
		intronsDir
			Dossier contenant les annotations des introns.
			Les fichiers d'annotations sont normalement générés par les programmes d'analyse des fichiers GFF3/GFF/GTF que j'ai écrits au début du projet !
			Le nom des fichiers d'annotations doit avoir le format suivant :
				<nom de l'espèce>.introns
			<nom de l'espèce> doit être tel qu'il apparait dans les IDs des séquences du dossier groupsDir (exemple : "albu", "hyal", "pyul", etc.).
		groupsDir
			Dossier contenant les fichiers .fasta à "marquer" avec les annotations d'introns.
		outDir
			Dossier de sortie (sera créé, *** ne doit pas déjà exister).
			Les fichiers de sortie portent l'extension .marked.fasta.
*/
package lima;
import java.util.Hashtable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
class IntronsAnnotation {
	public String positions;
	public String lengths;
	public IntronsAnnotation(String p, String l) {
		positions = p;
		lengths = l;
	}
}
public class positionIntronsOnGroups {
	/* Hashtable qui classe les positions des introns par espèce et par ID de séquence. */
	public static Hashtable<String, Hashtable<String, IntronsAnnotation>> introns = new Hashtable<String, Hashtable<String, IntronsAnnotation>>();
	public static void writeSequence(String groupName, String header, StringBuffer content, BufferedReader inFile, BufferedWriter outFile) throws Exception {
		if(header != null) {
			String[] morceaux = header.split(" ", 2);
			if(morceaux.length > 0) {
				String piece1 = morceaux[0].trim();
				if(piece1.length() < 2) {
					outFile.close();
					inFile.close();
					throw new Exception("Groupe " + groupName + " : proteine " + piece1 + " : impossible d'analyser l'entete.");
				};
				String[] morceaux2 = piece1.substring(1).split("\\|");
				if(morceaux2.length != 2) {
					outFile.close();
					inFile.close();
					throw new Exception("Groupe " + groupName + " : proteine " + piece1 + " : impossible de reperer l'ID de la proteine.");
				};
				String speciesName =  morceaux2[0].trim();
				String proteinId = morceaux2[1].trim();
				if(!introns.containsKey(speciesName)) {
					outFile.close();
					inFile.close();
					throw new Exception("Espece " + speciesName + " non trouvee.");
				};
				Hashtable<String, IntronsAnnotation> speciesIntrons = introns.get(speciesName);
				if(!speciesIntrons.containsKey(proteinId)) {
					outFile.close();
					inFile.close();
					throw new Exception("Impossible de trouver une annotation d'introns pour la proteine " + proteinId + " de l'espece " + speciesName + ".");
				};
				IntronsAnnotation annotation = speciesIntrons.get(proteinId);
				String headerToWrite = header + " /organism=" + speciesName + " {i " + annotation.positions + " i} /intron-length=" + annotation.lengths;
				String contentToWrite = content.toString();
				outFile.write(headerToWrite, 0, headerToWrite.length());
				outFile.newLine();
				outFile.write(contentToWrite, 0, contentToWrite.length());
				outFile.newLine();
			};
		};
	}
	public static void main(String[] args) {
		if(args.length == 3) try {
			File intronsDir = new File(args[0]);
			File groupsDir = new File(args[1]);
			File outDir = new File(args[2]);
			if(!intronsDir.isDirectory()) throw new Exception("Le parametre 1 n'est pas un dossier.");
			if(!groupsDir.isDirectory()) throw new Exception("Le parametre 2 n'est pas un dossier.");
			if(!outDir.mkdir()) throw new Exception("Impossible de creer le dossier de sortie.");
			/*Mise en mémoire des positions des introns.*/
			// Annotations d'introns classées par espèces.
			String intronsExtension = ".introns";
			for(File intronsFile : intronsDir.listFiles()) {
				String intronsFilename = intronsFile.getName().toLowerCase();
				if(intronsFilename.indexOf(intronsExtension) == intronsFilename.length() - intronsExtension.length()) {
					String speciesName = intronsFilename.substring(0, intronsFilename.length() - intronsExtension.length());
					BufferedReader file = new BufferedReader(new FileReader(intronsFile.getAbsolutePath()));
					String ligne = null;
					while((ligne = file.readLine()) != null) {
						String[] columns = ligne.trim().split("\t");
						if(columns.length != 8) {
							file.close();
							throw new Exception("Une ligne ne contient pas 8 colonnes : fichier " + intronsFilename + " : ligne : " + ligne);
						};
						if(columns[0].trim().equals("CDS")) {
							String proteinId = columns[4].trim();
							String intronsPositions = columns[6].trim();
							String intronsLengths = columns[7].trim();
							if(!introns.containsKey(speciesName))
								introns.put(speciesName, new Hashtable<String, IntronsAnnotation>());
							Hashtable<String, IntronsAnnotation> speciesIntrons = introns.get(speciesName);
							if(speciesIntrons.containsKey(proteinId))
								throw new Exception("Annotation d'une proteine rencontree deux fois : " + proteinId + ".");
							speciesIntrons.put(proteinId, new IntronsAnnotation(intronsPositions, intronsLengths));
						};
					};
					file.close();
				};
			};
			/* Réécriture des groupes. */
			String newLine = System.getProperty("line.separator");
			String groupExtension = ".fasta";
			long groupsCount = 0;
			for(File group : groupsDir.listFiles()) {
				String groupFilename = group.getName().toLowerCase();
				if(groupFilename.indexOf(groupExtension) == groupFilename.length() - groupExtension.length()) {
					++groupsCount;
					String groupName = groupFilename.substring(0, groupFilename.length() - groupExtension.length());
					File groupOutFile = new File(outDir, groupName + ".marked.fasta");
					BufferedReader inFile = new BufferedReader(new FileReader(group.getAbsolutePath()));
					BufferedWriter outFile = new BufferedWriter(new FileWriter(groupOutFile.getAbsolutePath()));
					String header = null;
					StringBuffer content = new StringBuffer();
					String ligne = null;
					while((ligne = inFile.readLine()) != null) {
						ligne = ligne.trim();
						if(!ligne.isEmpty()) {
							if(ligne.charAt(0) == '>') {
								writeSequence(groupName, header, content, inFile, outFile);
								header = ligne;
								content = new StringBuffer();
							} else {
								content.append(ligne).append(newLine);
							};
						};
					};
					writeSequence(groupName, header, content, inFile, outFile);
					outFile.close();
					inFile.close();
					if(groupsCount % 1000 == 0) System.err.println(groupsCount + " groupes reecrits.");
				};
			};
			System.err.println(groupsCount + " groupes reecrits au total.");
		} catch(Exception e) {
			e.printStackTrace();
		};
	}
}