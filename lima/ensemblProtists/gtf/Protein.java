package lima.ensemblProtists.gtf;
import java.util.ArrayList;

public class Protein implements Comparable<Protein> {
	private String nom;
	private ArrayList<Sequence> sequences;
	public Protein(String name) {
		nom = name;
		sequences = new ArrayList<Sequence>(10);
	};
	public void addSequence(Sequence s) {
		sequences.add(s);
	};
	public String name() {
		return nom;
	};
	public int compareTo(Protein autre) {
		return nom.compareTo(autre.name());
	};
	public ArrayList<Sequence> getSequences() {
		return sequences;
	};
	public long minPos() throws Exception {
		int compte = sequences.size();
		if(compte == 0) throw new Exception("Impossible de calculer la borne inferieure d'une proteine vide.");
		long minimum = sequences.get(0).minValue();
		for(int i = 1; i < compte; ++i) {
			long valeur = sequences.get(i).minValue();
			if(valeur < minimum) minimum = valeur;
		};
		return minimum;
	};
	public long maxPos() throws Exception {
		int compte = sequences.size();
		if(compte == 0) throw new Exception("Impossible de calculer la borne superieure d'une proteine vide.");
		long maximum = sequences.get(0).maxValue();
		for(int i = 1; i < compte; ++i) {
			long valeur = sequences.get(i).maxValue();
			if(valeur > maximum) maximum = valeur;
		};
		return maximum;
	};
};