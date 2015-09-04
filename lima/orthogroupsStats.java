/*
Calcule de sstatistiques sur les groupes calculés par OrthoMCL.
java lima.orthogroupsStats groupesOrthoMCL
	groupesOrthoMCL est le fichier retourné par OrthoMCL et contenant la description des groupes.
	Chaque ligne doit correspondre à un groupe et doit avoir le format suivant :
		<nom du groupe>: <id-sequence-1> <ide-sequence-2> ... <ide-sequence-n>
	Les IDs des séquences doivent être séparés par des espaces.
Format de sortie
	Chaque ligne donne une information sur un groupe. Les informations sont des colonnes séparées par des tabulations :
		Nom du groupe
		Nombre de séquences dans le groupe
		Nombre d'espèces dans le groupe
		Booléen : le groupe est-il strictement orthologue ?
		9 colonnes donnant le nombre de séquences pour chaque espèce dans le groupe. Ordre des colonnes :
			albu, hyal, phca, phci, phin, phpa, phra, phso, pyul.
*/
package lima;

import java.io.FileReader;
import java.io.BufferedReader;

public class orthogroupsStats {
	public static void main(String args[]) {
		try {
			if(args.length == 1) {
				String[] nomsEspeces = new String[]{
					"albu",
					"hyal",
					"phca",
					"phci",
					"phin",
					"phpa",
					"phra",
					"phso",
					"pyul"
				};
				BufferedReader fichierGroupes = new BufferedReader(new FileReader(args[0]));
				String ligne = null;
				System.out.print("#group\tseqCount\tspeciesCount\tisOrthologous");
				for(String espece : nomsEspeces) System.out.print("\t" + espece);
				System.out.println();
				while((ligne = fichierGroupes.readLine()) != null) {
					ligne = ligne.trim();
					if(!ligne.isEmpty()) {
						String[] morceaux = ligne.split(":");
						if(morceaux.length != 2) throw new Exception("Ligne au format incorrect : " + ligne);
						String nomGroupe = morceaux[0].trim();
						String[] listeSequences = morceaux[1].split(" ");
						int[] compteSequencesParEspece = new int[nomsEspeces.length];
						int compteTotalSequences = 0;
						for(String sequence : listeSequences) {
							sequence = sequence.trim();
							if(!sequence.isEmpty()) {
								int indexEspece = -1;
								for(indexEspece = 0; indexEspece < nomsEspeces.length && sequence.indexOf(nomsEspeces[indexEspece]) != 0; ++indexEspece);
								if(indexEspece < nomsEspeces.length) {
									++compteSequencesParEspece[indexEspece];
									++compteTotalSequences;
								};
							};
						};
						boolean isOrthologous = true;
						for(int compte : compteSequencesParEspece) {
							if(compte != 1) {
								isOrthologous = false;
								break;
							};
						};
						int speciesCount = 0;
						for(int compte: compteSequencesParEspece) {
							if(compte != 0) ++speciesCount;
						}
						System.out.print(nomGroupe + "\t" + compteTotalSequences + "\t" + speciesCount + "\t" + isOrthologous);
						for(int compte : compteSequencesParEspece) {
							System.out.print("\t" + compte);
						};
						System.out.println();
					};
				};
				fichierGroupes.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}