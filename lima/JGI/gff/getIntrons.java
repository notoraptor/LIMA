package lima.JGI.gff;
import lima.ensemblProtists.Interval;
import lima.ensemblProtists.Intervals;
import lima.ensemblProtists.Introns;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.FileReader;
import java.io.BufferedReader;

/*
Classe qui rassemble les informations sur les séquences lues depuis un fichier GTF.
*/
class Sequence {
	public String feature;
	public long start;
	public long end;
	public char strand;
	public long minValue() {
		return start < end ? start : end;
	};
	public long maxValue() {
		return start > end ? start : end;
	};
};

class Protein implements Comparable<Protein> {
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

class Transcript {
	private String nom;
	private ArrayList<Sequence> sequences;
	private ArrayList<Protein> proteines;
	public Transcript(String name) {
		nom = name;
		sequences = new ArrayList<Sequence>(10);
		proteines = new ArrayList<Protein>();
	};
	void addSequence(Sequence s) {
		sequences.add(s);
	};
	void addProtein(Protein p) {
		proteines.add(p);
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

class Gene {
	private String nom;
	private TreeMap<String, Transcript> transcrits;
	private TreeMap<String, Protein> proteines;
	public Gene(String name) {
		nom = name;
		transcrits = new TreeMap<String, Transcript>();
		proteines = new TreeMap<String, Protein>();
	};
	public boolean hasTranscript(String s) {
		return transcrits.containsKey(s);
	};
	public boolean hasProtein(String s) {
		return proteines.containsKey(s);
	};
	public void createTranscript(String s) {
		transcrits.put(s, new Transcript(s));
	};
	public void createProtein(String s) {
		proteines.put(s, new Protein(s));
	};
	public void addToTranscript(String t, Sequence s) {
		if(!hasTranscript(t)) createTranscript(t);
		transcrits.get(t).addSequence(s);
	};
	public void addToProtein(String p, Sequence s) {
		if(!hasProtein(p)) createProtein(p);
		proteines.get(p).addSequence(s);
	};
	public void mapProteinsToTranscripts() throws Exception {
		Iterator<Protein> iterateurProteines = proteines.values().iterator();
		while(iterateurProteines.hasNext()) {
			Protein proteine = iterateurProteines.next();
			long borneInferieureProteine = proteine.minPos();
			long borneSuperieureProteine = proteine.maxPos();
			Transcript transcrit = null;
			Iterator<Transcript> iterateurTranscrits = transcrits.values().iterator();
			while(transcrit == null && iterateurTranscrits.hasNext()) {
				Transcript transcritCourant = iterateurTranscrits.next();
				if(transcritCourant.minPos() <= borneInferieureProteine && borneSuperieureProteine <= transcritCourant.maxPos())
					transcrit = transcritCourant;
			};
			if(transcrit == null) throw new Exception("Impossible d'associer la proteine " + proteine.name() + " a un transcrit.");
			transcrit.addProtein(proteine);
		};
	};
	public Collection<Transcript> getTranscripts() {
		return transcrits.values();
	};
	public String name() {
		return nom;
	};
};

public class getIntrons {
	public static void getAnnotation(String cheminFichierGTF, String prefixeGeneId, String prefixeTranscriptId) throws Exception {
		String tagGeneId = "name ";
		String tagTranscriptId = "transcriptId ";
		String tagProteinId = "proteinId ";
		/*
		On utilise un TreeMap au lieu d'un Hashtable pour que les clés (noms des gènes) soient automatiquement triées.
		*/
		TreeMap<String, Gene> donnees = new TreeMap<String, Gene>();
		BufferedReader fichier = new BufferedReader(new FileReader(cheminFichierGTF));
		int numeroLigne = 0;
		String ligne = null;
		while((ligne = fichier.readLine()) != null) {
			++numeroLigne;
			ligne = ligne.trim();
			if(!ligne.isEmpty() && ligne.charAt(0) != '#') {
				String[] colonnes = ligne.split("\t");
				if(colonnes.length != 9) throw new Exception("Ligne " + numeroLigne + " : nombre de colonnes != 9.");
				String feature = colonnes[2].trim();
				if(feature.isEmpty() || feature.equals("."))
					throw new Exception("Ligne " + numeroLigne + " : feature non indiquee.");
				if(feature.equals("CDS") || feature.equals("exon")) {
					long start = Long.parseLong(colonnes[3].trim());
					long end = Long.parseLong(colonnes[4].trim());
					char strand;
					String champBrin = colonnes[6].trim();
					if(champBrin.length() != 1)
						throw new Exception("Ligne " + numeroLigne + " : brin incorrect (un seul caractere attendu : '+','-' ou '.').");
					strand = champBrin.charAt(0);
					if(strand != '+' && strand != '-' && strand != '.')
						throw new Exception("Ligne " + numeroLigne + " : brin incorrect (caractere attendu : '+','-' ou '.').");
					if(strand == '.')
						System.err.println("Ligne " + numeroLigne + " : brin indefini. Brin + choisi par defaut.");
					String geneId = null;
					String transcriptId = null;
					String proteinId = null;
					String[] informations = colonnes[8].split(";");
					for(int i = 0; i < informations.length; ++i) {
						String information = informations[i].trim();
						if(information.indexOf(tagGeneId) == 0) {
							geneId = information.substring(tagGeneId.length()).replace("\"","").trim();
						} else if(information.indexOf(tagTranscriptId) == 0) {
							transcriptId = information.substring(tagTranscriptId.length()).replace("\"","").trim();
						} else if(information.indexOf(tagProteinId) == 0) {
							proteinId = information.substring(tagProteinId.length()).replace("\"","").trim();
						};
					};
					if(geneId == null)
						throw new Exception("Ligne " + numeroLigne + " : impossible de detecter un ID de gene.");
					if((transcriptId == null && proteinId == null) || (transcriptId != null && proteinId != null))
						throw new Exception("Ligne : on ne s'attend pas a ce que les IDs de la proteine et du transcrit soient tous les deux absents ou presents en meme temps sur une meme ligne dans un fichier GFF fourni par JGI.");
					if(feature.equals("exon") && transcriptId == null)
						throw new Exception("Ligne " + numeroLigne + " : exon sans ID de transcrit.");
					if(feature.equals("CDS") && proteinId == null)
						throw new Exception("Ligne " + numeroLigne + " : CDS sans ID de proteine.");
					Sequence sequence = new Sequence();
					sequence.feature = feature;
					sequence.start = start;
					sequence.end = end;
					sequence.strand = strand;
					geneId = prefixeGeneId + geneId;
					if(!donnees.containsKey(geneId)) donnees.put(geneId, new Gene(geneId));
					if(proteinId != null) {
						proteinId = prefixeTranscriptId + proteinId;
						donnees.get(geneId).addToProtein(proteinId, sequence);
					} else {
						transcriptId = prefixeTranscriptId + transcriptId;
						donnees.get(geneId).addToTranscript(transcriptId, sequence);
					};
				};
			};
		};
		fichier.close();
		System.out.println("#annotation\tADNType\tsequenceId\ttranscriptId\tproteinId\tstrand\tintronsPos\tintronsLen");
		Iterator<Gene> iterateurGenes = donnees.values().iterator();
		while(iterateurGenes.hasNext()) {
			Gene gene = iterateurGenes.next();
			String geneId = gene.name();
			gene.mapProteinsToTranscripts();
			Iterator<Transcript> iterateurTranscrits = gene.getTranscripts().iterator();
			while(iterateurTranscrits.hasNext()) {
				Transcript transcrit = iterateurTranscrits.next();
				String transcriptId = transcrit.name();
				String proteinId = "";
				ArrayList<Protein> proteinesAssociees = transcrit.getProteins();
				ArrayList<Sequence> sequencesTranscrit = transcrit.getSequences();
				int compteProteinesAssociees = proteinesAssociees.size();
				int compteSequencesTranscrit = sequencesTranscrit.size();
				if(compteProteinesAssociees == 1)
					proteinId = proteinesAssociees.get(0).name();
				else if(compteProteinesAssociees > 1) {
					Collections.sort(proteinesAssociees);
					System.err.println("# Le transcrit " + transcriptId + " a " + compteProteinesAssociees + " proteines.");
					System.out.println("# Le transcrit " + transcriptId + " a " + compteProteinesAssociees + " proteines.");
				};
				Intervals exons = new Intervals();
				for(int i = 0; i < compteSequencesTranscrit; ++i) {
					Sequence sequence = sequencesTranscrit.get(i);
					exons.add(new Interval(sequence.start, sequence.end));
				};
				char strand = sequencesTranscrit.get(0).strand;
				Introns intronsExons = new Introns();
				if(exons.size() >= 2) {
					exons.sort();
					exons.setPositionsFromOne();
					if(strand == '-') exons.reverse();
					int compteExons = exons.size();
					long longueurExons = 0;
					for(int l = 1; l < compteExons; ++l) {
						longueurExons += exons.get(l - 1).count();
						long positionIntron = longueurExons;
						long tailleIntron = exons.get(l).start - exons.get(l - 1).end - 1;
						if(tailleIntron > 0) {
							intronsExons.add(positionIntron, tailleIntron);
						};
					};
				};
				System.out.println("transcript\t\t" + geneId + "\t" + transcriptId + "\t" + proteinId + "\t" + strand + "\t" + intronsExons.positionsToString() + "\t" + intronsExons.lengthsToString());
				for(int i = 0; i < compteProteinesAssociees; ++i) {
					Protein proteine = proteinesAssociees.get(i);
					proteinId = proteine.name();
					ArrayList<Sequence> sequencesProteine = proteine.getSequences();
					int compteSequencesProteine = sequencesProteine.size();
					Intervals CDS = new Intervals();
					for(int j = 0; j < compteSequencesProteine; ++j) {
						Sequence sequence = sequencesProteine.get(j);
						CDS.add(new Interval(sequence.start, sequence.end));
					};
					strand = sequencesProteine.get(0).strand;
					Introns intronsCDS = new Introns();
					if(CDS.size() >= 2) {
						CDS.sort();
						CDS.setPositionsFromOne();
						if(strand == '-') CDS.reverse();
						int compteCDS = CDS.size();
						long longueurCDS = 0;
						for(int l = 1; l < compteCDS; ++l) {
							longueurCDS += CDS.get(l - 1).count();
							long positionIntron = longueurCDS;
							long tailleIntron = CDS.get(l).start - CDS.get(l - 1).end - 1;
							if(tailleIntron > 0) {
								intronsCDS.add(positionIntron, tailleIntron);
							};
						};
					};
					System.out.println("CDS\t\t" + geneId + "\t" + transcriptId + "\t" + proteinId + "\t" + strand + "\t" + intronsCDS.positionsToString() + "\t" + intronsCDS.lengthsToString());
				};
			};
		};
	};
	public static void main(String[] args) {
		if(args.length > 0 && args.length < 4) {
			try {
				String prefixeGeneId = "";
				String prefixeTranscriptId = "";
				if(args.length > 1) prefixeGeneId = args[1];
				if(args.length > 2) prefixeTranscriptId = args[2];
				getAnnotation(args[0], prefixeGeneId, prefixeTranscriptId);
			} catch(Exception e) {
				System.err.println(e);
			};
		};
	};
};

