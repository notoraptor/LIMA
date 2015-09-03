package lima.ensemblProtists.gtf;
import java.util.TreeMap;
import java.util.Collection;
public class Gene {
	private String nom;
	private TreeMap<String, Transcript> transcrits;
	public Gene(String name) {
		nom = name;
		transcrits = new TreeMap<String, Transcript>();
	};
	public boolean hasTranscript(String s) {
		return transcrits.containsKey(s);
	};
	public void createTranscript(String s) {
		transcrits.put(s, new Transcript(s));
	};
	public Transcript getTranscript(String id) {
		Transcript transcrit = null;
		if(transcrits.containsKey(id)) transcrit = transcrits.get(id);
		return transcrit;
	};
	public void addToTranscript(String t, Sequence s) {
		if(!hasTranscript(t)) createTranscript(t);
		transcrits.get(t).addSequence(s);
	};
	public Collection<Transcript> getTranscripts() {
		return transcrits.values();
	};
	public String name() {
		return nom;
	};
};