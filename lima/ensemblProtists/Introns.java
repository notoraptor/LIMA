/*
Steven Bocco, 20 Février 2013.
Classe Introns, qui permet de gérer la liste des annotations d'introns extraites pour une séquence depuis un fichier GFF3 ou GTF.
*/

package lima.ensemblProtists;
import java.util.ArrayList;

public class Introns {
	// Attributs.
	protected ArrayList<Long> positions;
	protected ArrayList<Long> longueurs;
	// Méthodes.
	public Introns() {
		positions = new ArrayList<Long>(10);
		longueurs = new ArrayList<Long>(10);
	};
	public void add(long position, long longueur) {
		positions.add(new Long(position));
		longueurs.add(new Long(longueur));
	};
	public String positionsToString() {
		String chaine = "";
		int compte = positions.size();
		if(compte > 0) {
			chaine = positions.get(0).toString();
			for(int i = 1; i < compte; ++i) {
				chaine += "," + positions.get(i).toString();
			};
		} else chaine = "00";
		return chaine;
	};
	public String lengthsToString() {
		String chaine = "";
		int compte = longueurs.size();
		if(compte > 0) {
			chaine = longueurs.get(0).toString();
			for(int i = 1; i < compte; ++i) {
				chaine += "," + longueurs.get(i).toString();
			};
		} else chaine = "00";
		return chaine;
	};
	public int size() {
		return positions.size();
	};
};

