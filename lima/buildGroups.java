/*
Génération des fichiers FASTA des groupes calculés par OrthoMCL.
java maitrise.builGroups listeGroupesOrthoMCL dossierSequences dossierSortie
	listeGroupesOrthoMCL
		Fichier contenant la liste des groupes de gènes calculés par OrthoMCL.
		Chaque ligne doit représenter un groupe. Le format d'une ligne doit être le suivant :
			<nom du groupe>:ID1 ID2 ID3 ... IDn
		Les IDs sont les identifiants des séquences du groupe, séparés par des espaces.
	dossierSequences
		Dossier contenant les fichiers .fasta dans lesquels on peut trouver les séquences des groupes.
		Les entêtes des séquences de ces fichiers FASTA doivent être les IDs des séquences, tels qu'ils apparaissent dans listeGroupesOrthoMCL.
		De préférence, il faut utiliser le dossier compliantFasta/ de l'étape OrthoMCL car les IDs y sont déjà correctement formatés.
	dossierSortie
		Dossier de sortie (sera créé par ce programme). *** ne doit pas déjà exister !
		Le dossier contiendra un fichier .fasta pour chaque groupe.
*/
package lima;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Hashtable;

public class buildGroups {
	public static void saveSequence(String header, StringBuffer content, Hashtable<String, Sequence> sequences) throws Exception {
		if(header != null) {
			if(content.length() == 0)
				throw new Exception("Interdit de sauvegarder une sequence vide (entete : " + header + ").");
			String key =  header.charAt(0) == '>' ? header.substring(1) : header;
			if(sequences.containsKey(key))
				throw new Exception("Une sequence ayant le meme nom (" + key + ") est deja en memoire.");
			sequences.put(key, new Sequence(header, content));
		};
	}
	public static void writeSequence(Sequence sequence, BufferedWriter file) throws Exception {
		file.write(sequence.header, 0, sequence.header.length());
		file.newLine();
		file.write(sequence.content, 0, sequence.content.length());
		file.newLine();
	}
	public static void main(String[] args) {
		if(args.length == 3) {
			try {
				File groupsFilename = new File(args[0]);
				File sequencesDirname = new File(args[1]);
				File outDirname = new File(args[2]);
				if(!groupsFilename.isFile()) throw new Exception("Le parametre 1 n'est pas un fichier.");
				if(!sequencesDirname.isDirectory()) throw new Exception("Le parametre 2 n'est pas un dossier.");
				if(!outDirname.mkdir()) throw new Exception("Impossible de creer le dossier de sortie (parametre 3).");
				// Mise en memoire des séquences.
				Hashtable<String, Sequence> sequences = new Hashtable<String, Sequence>();
				String extension = ".fasta";
				String nouvelleLigne = System.getProperty("line.separator");
				File[] sequencesFilenames = sequencesDirname.listFiles();
				for(File sequencesFilename : sequencesFilenames) {
					if(sequencesFilename.isFile()) {
						String sfn = sequencesFilename.getName().toLowerCase();
						if(sfn.indexOf(extension) == sfn.length() - extension.length()) {
							// Fichier FASTA trouvé. Lecture.
							BufferedReader sequencesFile = new BufferedReader(new FileReader(sequencesFilename.getAbsolutePath()));
							String header = null;
							StringBuffer content = new StringBuffer();
							String ligne = null;
							while((ligne = sequencesFile.readLine()) != null) {
								ligne = ligne.trim();
								if(!ligne.isEmpty()) {
									if(ligne.charAt(0) == '>') {
										saveSequence(header, content, sequences);
										header = ligne;
										content = new StringBuffer();
									} else {
										// On enlève les astérisques (*) en fin de séquences pour éviter les futurs "WARNINGs" de MUSCLE.
										content.append(ligne.replaceAll("\\*$","")).append(nouvelleLigne);
									};
								};
							};
							sequencesFile.close();
							saveSequence(header, content, sequences);
						};
					};
				};
				System.err.println(sequences.size() + " sequences mises en memoire.");
				// Création des groupes.
				int compteSequencesEcrites = 0;
				BufferedReader groupsFile = new BufferedReader(new FileReader(groupsFilename.getAbsolutePath()));
				String ligne = null;
				while((ligne = groupsFile.readLine()) != null) {
					ligne = ligne.trim();
					if(!ligne.isEmpty()) {
						String[] morceaux = ligne.split(":");
						if(morceaux.length != 2) throw new Exception("Ligne incorrecte dans le fichier des groupes : " + ligne);
						String groupName = morceaux[0].trim();
						String[] sequencesList = morceaux[1].trim().split(" ");
						File groupFilename = new File(outDirname, groupName + ".fasta");
						BufferedWriter groupFile = new BufferedWriter(new FileWriter(groupFilename.getAbsolutePath()));
						for(String sequenceName : sequencesList) {
							sequenceName = sequenceName.trim();
							if(!sequenceName.isEmpty()) {
								if(!sequences.containsKey(sequenceName)) {
									groupFile.close();
									groupsFile.close();
									throw new Exception("Impossible de trouver la sequence " + sequenceName + " du groupe " + groupName + ".");
								};
								writeSequence(sequences.get(sequenceName), groupFile);
								++compteSequencesEcrites;
								if(compteSequencesEcrites % 10000 == 0)
									System.err.println(compteSequencesEcrites + " sequences ecrites.");
							};
						};
						groupFile.close();
					};
				};
				groupsFile.close();
				System.err.println(compteSequencesEcrites + " sequences ecrites au total.");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}