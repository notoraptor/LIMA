package lima.ensemblProtists.gtf;
import java.util.ArrayList;
public class Transcript {
	private String nom;
	private ArrayList<Sequence> sequences;
	private ArrayList<Protein> proteines;
	public Transcript(String name) {
		nom = name;
		sequences = new ArrayList<Sequence>(10);
		proteines = new ArrayList<Protein>();
	};
	public void addSequence(Sequence s) {
		sequences.add(s);
	};
	public void addProtein(Protein p) {
		proteines.add(p);
	};
	public Protein getProtein(String id) {
		Protein proteine = null;
		int compteProteines = proteines.size();
		for(int i = 0; proteine == null && i < compteProteines; ++i) {
			if(proteines.get(i).name().equals(id)) proteine = proteines.get(i);
		};
		return proteine;
	};
	public String name() {
		return nom;
	};
	public ArrayList<Protein> getProteins() {
		return proteines;
	};
	public ArrayList<Sequence> getSequences() {
		return sequences;
	};
	public long minPos() throws Exception {
		int compte = sequences.size();
		if(compte == 0) throw new Exception("Impossible de calculer la borne inferieure d'un transcrit vide.");
		long minimum = sequences.get(0).minValue();
		for(int i = 1; i < compte; ++i) {
			long valeur = sequences.get(i).minValue();
			if(valeur < minimum) minimum = valeur;
		};
		return minimum;
	};
	public long maxPos() throws Exception {
		int compte = sequences.size();
		if(compte == 0) throw new Exception("Impossible de calculer la borne superieure d'un transcrit vide.");
		long maximum = sequences.get(0).maxValue();
		for(int i = 1; i < compte; ++i) {
			long valeur = sequences.get(i).maxValue();
			if(valeur > maximum) maximum = valeur;
		};
		return maximum;
	};
};