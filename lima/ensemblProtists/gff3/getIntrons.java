package lima.ensemblProtists.gff3;
import lima.ensemblProtists.Interval;
import lima.ensemblProtists.Intervals;
import lima.ensemblProtists.Introns;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

/*
Classe représentant un terme de l'ontologie associée aux fichiers GFF3.
Les termes sont les noms de feature utilisés dans la 3ème colonne des fichiers GFF3 de EnsemblProtists.
*/
class Term {
	// Attributs.
	protected Terms termes;
	protected String terme;
	protected ArrayList<String> isA;
	protected ArrayList<String> partOf;
	// Méthodes.
	public Term(String terme, Terms termes) {
		this.termes  = termes;
		this.terme = terme;
		isA = new ArrayList<String>(10);
		partOf = new ArrayList<String>(10);
	};
	public String term() {
		return terme;
	};
	public void addIsA(Term terme) {
		if(!isA.contains(terme.term()) && termes.contains(terme)) isA.add(terme.term());
	};
	public void addPartOf(Term terme) {
		if(!partOf.contains(terme.term()) && termes.contains(terme)) partOf.add(terme.term());
	};
	public boolean isA(String tname) {
		if(terme.equals(tname)) return true;
		else if(isA.isEmpty()) return false;
		else if(isA.contains(tname)) return true;
		boolean test = false;
		int taille = isA.size();
		for(int i = 0; !test && i < taille; ++i) {
			test = termes.get(isA.get(i)).isA(tname);
		};
		return test;
	};
	public boolean partOf(String tname) {
		if(partOf.isEmpty()) return false;
		else if(partOf.contains(tname)) return true;
		boolean test = false;
		int taille = partOf.size();
		for(int i = 0; !test && i < taille; ++i) {
			test = termes.get(partOf.get(i)).partOf(tname);
		};
		return test;
	};
};

/*
Classe qui extrait l'ensemble des termes de l'ontologie associée aux fichiers GFF3 de EnsemblProtists.
L'ontologie doit être décrite dans un fichier appelé "gff3-ontology.txt" et placé dans le même répertoire que cette classe getIntrons.
La dernière version de l'ontologie est téléchargeable à l'adresse suivante :
	http://sourceforge.net/p/song/svn/HEAD/tree/trunk/so-xp-simple.obo
	Site d'origine : http://www.sequenceontology.org/index.html
*/
class Terms {
	// Attributs.
	protected Hashtable<String, Term> termes;
	// Méthodes.
	public Terms() {
		termes = new Hashtable<String, Term>(1000);
	};
	public boolean contains(Term terme) {
		return termes.containsKey(terme.term());
	};
	public Term get(String tname) {
		return termes.containsKey(tname) ? termes.get(tname) : null;
	};
	public void add(Term terme) {
		if(!termes.containsKey(terme.term())) termes.put(terme.term(), terme);
	};
	public static Terms getTerms() throws Exception {
		File chemin = new File(Terms.class.getResource("").getFile());
		String filename = chemin.getAbsolutePath() + File.separator + "gff3-ontology.txt";
		return readFile(filename);
	};
	public static Terms readFile(String filename) throws Exception {
		String tagName = "name: ";
		String tagIsA = "is_a: ";
		String tagPartOf = "relationship: part_of ";
		Terms termes = new Terms();
		BufferedReader fichier = new BufferedReader(new FileReader(filename));
		String ligne = null;
		boolean lireFichier = true;
		while(lireFichier) {
			while((ligne = fichier.readLine()) != null && !ligne.trim().equals("[Term]"));
			if(ligne == null) lireFichier = false;
			else {
				String name = "";
				ArrayList<String> isA = new ArrayList<String>(10);
				ArrayList<String> partOf = new ArrayList<String>(10);
				while((ligne = fichier.readLine()) != null && !ligne.trim().isEmpty()) {
					ligne = ligne.trim();
					if(ligne.indexOf(tagName) == 0) {
						name = ligne.substring(tagName.length()).trim();
					} else if(ligne.indexOf(tagIsA) == 0) {
						isA.add(ligne.substring(ligne.lastIndexOf('!') + 1).trim());
					} else if(ligne.indexOf(tagPartOf) == 0) {
						partOf.add(ligne.substring(ligne.lastIndexOf('!') + 1).trim());
					};
				};
				if(ligne == null) lireFichier = false;
				Term terme = termes.get(name);
				if(terme == null) {
					terme = new Term(name, termes);
					termes.add(terme);
				};
				int compteIsA = isA.size();
				for(int i = 0; i < compteIsA; ++i) {
					String nomTermeIsA = isA.get(i);
					Term termeIsA = termes.get(nomTermeIsA);
					if(termeIsA == null) {
						termeIsA = new Term(nomTermeIsA, termes);
						termes.add(termeIsA);
					};
					terme.addIsA(termeIsA);
				};
				int comptePartOf = partOf.size();
				for(int i = 0; i < comptePartOf; ++i) {
					String nomTermePartOf = partOf.get(i);
					Term termePartOf = termes.get(nomTermePartOf);
					if(termePartOf == null) {
						termePartOf = new Term(nomTermePartOf, termes);
						termes.add(termePartOf);
					};
					terme.addPartOf(termePartOf);
				};
			};
		};
		fichier.close();
		return termes;
	};
};

/*
Classe rassemblant les informations sur une séquence.
*/
class Sequence implements Comparable<Sequence> {
	public static final int UNKNOWN = 0;
	public static final int GENE = 1;
	public static final int TRANSCRIPT = 2;
	public static final int EXON = 3;
	public static final int CDS = 4;
	// Attributs.
	public String id;
	public String parentId;
	public int featureClass;
	public String feature;
	public long start;
	public long end;
	public char strand;
	// Méthodes.
	public Sequence(String id, String feature, long start, long end, char strand) {
		this.id = id;
		this.parentId = null;
		this.featureClass = UNKNOWN;
		this.feature = feature;
		this.start = start;
		this.end = end;
		this.strand = strand;
	};
	public int compareTo(Sequence autre) {
		int comparaison = 0;
		if(parentId == null && autre.parentId != null) comparaison = -1;
		else if(parentId != null && autre.parentId == null) comparaison = 1;
		else if(parentId == null && autre.parentId == null) comparaison = id.compareTo(autre.id);
		else if(id.equals(autre.parentId) && !autre.id.equals(parentId)) comparaison = -1;
		else if(autre.id.equals(parentId) && !id.equals(autre.parentId)) comparaison = 1;
		else {
			comparaison = parentId.compareTo(autre.parentId);
			if(comparaison == 0) comparaison = id.compareTo(autre.id);
		};
		return comparaison;
	};
};

class RNASequence extends Sequence {
	public ArrayList<Sequence> exons;
	public ArrayList<Sequence> codingSequences;
	public RNASequence(String id, String feature, long start, long end, char strand) {
		super(id, feature, start, end, strand);
		featureClass = Sequence.TRANSCRIPT;
		exons = new ArrayList<Sequence>();
		codingSequences = new ArrayList<Sequence>();
	};
};

class DNASequence extends Sequence {
	public ArrayList<RNASequence> transcripts;
	public DNASequence(String id, String feature, long start, long end, char strand) {
		super(id, feature, start, end, strand);
		featureClass = Sequence.GENE;
		transcripts = new ArrayList<RNASequence>();
	};
};

class GFF3Parser {
	private Terms termes;
	public GFF3Parser() throws Exception {
		termes = Terms.getTerms();
	};
	public Collection<DNASequence> parseFile(String cheminFichierGFF3) throws Exception {
		Hashtable<String, DNASequence> genome = new Hashtable<String, DNASequence>();
		Hashtable<String, RNASequence> transcriptome = new Hashtable<String, RNASequence>();
		ArrayList<Sequence> sequencesNonPlacees = new ArrayList<Sequence>();
		String tagID = "ID=";
		String tagIdParent = "Parent=";
		BufferedReader fichier = new BufferedReader(new FileReader(cheminFichierGFF3));
		long numeroLigne = 0;
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
				Term terme = termes.get(feature);
				if(terme == null)
					throw new Exception("Ligne " + numeroLigne + " : feature inconnue dans l'ontologie utilisee.");
				if(
					feature.equals("exon") ||
					feature.equals("CDS") ||
					terme.isA("gene") ||
					terme.isA("transcript") ||
					terme.isA("pseudogene") ||
					terme.isA("pseudogenic_region")
				) {
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
					String id = null;
					String idParent = null;
					String[] informations = colonnes[8].split(";");
					for(int i = 0; i < informations.length; ++i) {
						String information = informations[i].trim();
						if(information.indexOf(tagID) == 0) {
							id = information.substring(tagID.length()).trim();
						} else if(information.indexOf(tagIdParent) == 0) {
							idParent = information.substring(tagIdParent.length()).trim();
						};
					};
					if(id == null)
						throw new Exception("Ligne " + numeroLigne + " : impossible de determiner l'ID de la sequence.");
					int featureClass = Sequence.UNKNOWN;
					if(feature.equals("exon")) featureClass = Sequence.EXON;
					else if(feature.equals("CDS")) featureClass = Sequence.CDS;
					else if(terme.isA("transcript")) featureClass = Sequence.TRANSCRIPT;
					else if(terme.isA("gene")) featureClass = Sequence.GENE;
					else if(terme.isA("pseudogene") || terme.isA("pseudogenic_region")) {
						if(idParent == null) featureClass = Sequence.GENE;
						else featureClass = Sequence.TRANSCRIPT;
					};
					if(featureClass == Sequence.GENE && idParent != null)
						throw new Exception("Ligne " + numeroLigne + " : une feature qui est un gene ne doit pas avoir de sequence parente.");
					if(featureClass != Sequence.GENE && idParent == null)
						throw new Exception("Ligne " + numeroLigne + " : une feature qui n'est pas un gene doit avoir une sequence parente.");
					if(featureClass == Sequence.GENE) {
						if(genome.containsKey(id))
							throw new Exception("Ligne " + numeroLigne + " : une sequence dans le genome a deja l'ID " + id + ".");
						DNASequence sequenceGenomique = new DNASequence(id, feature, start, end, strand);
						genome.put(id, sequenceGenomique);
					} else if(featureClass == Sequence.TRANSCRIPT) {
						if(transcriptome.containsKey(id))
							throw new Exception("Ligne " + numeroLigne + " : une sequence dans le transcriptome a deja l'ID " + id + ".");
						RNASequence sequenceTranscrite = new RNASequence(id, feature, start, end, strand);
						sequenceTranscrite.parentId = idParent;
						if(genome.containsKey(idParent)) {
							genome.get(idParent).transcripts.add(sequenceTranscrite);
							transcriptome.put(id, sequenceTranscrite);
						} else {
							sequencesNonPlacees.add(sequenceTranscrite);
						};
					} else {
						Sequence sequence = new Sequence(id, feature, start, end, strand);
						sequence.featureClass = featureClass;
						sequence.parentId = idParent;
						if(transcriptome.containsKey(idParent)) {
							RNASequence parent = transcriptome.get(idParent);
							if(featureClass == Sequence.CDS) parent.codingSequences.add(sequence);
							else parent.exons.add(sequence);
						} else {
							sequencesNonPlacees.add(sequence);
						};
					};
				};
			};
		};
		fichier.close();
		Collections.sort(sequencesNonPlacees);
		int compteSequencesNonPlacees = sequencesNonPlacees.size();
		for(int i = 0; i < compteSequencesNonPlacees; ++i) {
			Sequence sequence = sequencesNonPlacees.get(i);
			if(sequence.featureClass == Sequence.TRANSCRIPT) {
				RNASequence sequenceTranscrite = (RNASequence)sequence;
				if(transcriptome.containsKey(sequenceTranscrite.id))
					throw new Exception("Une sequence dans le transcriptome a deja l'ID " + sequenceTranscrite.id + ".");
				if(!genome.containsKey(sequenceTranscrite.parentId))
					throw new Exception("Impossible de trouver le gene " + sequenceTranscrite.parentId + " parent du transcrit " + sequenceTranscrite.id + ".");
				genome.get(sequenceTranscrite.parentId).transcripts.add(sequenceTranscrite);
				transcriptome.put(sequenceTranscrite.id, sequenceTranscrite);
			} else {
				if(!transcriptome.containsKey(sequence.parentId))
					throw new Exception("Impossible de trouver le transcrit " + sequence.parentId + " parent de la sequence " + sequence.feature + " " + sequence.id + ".");
				RNASequence parent = transcriptome.get(sequence.parentId);
				if(sequence.featureClass == Sequence.CDS) parent.codingSequences.add(sequence);
				else parent.exons.add(sequence);
			};
		};
		Enumeration<String> identifiantsGenes = genome.keys();
		Enumeration<String> identifiantsTranscrits = transcriptome.keys();
		while(identifiantsGenes.hasMoreElements()) {
			DNASequence gene = genome.get(identifiantsGenes.nextElement());
			if(gene.transcripts.isEmpty())
				throw new Exception("La sequence genomique " + gene.id + " n'a ete associee a aucun transcrit.");
			Collections.sort(gene.transcripts);
		};
		while(identifiantsTranscrits.hasMoreElements()) {
			RNASequence transcrit = transcriptome.get(identifiantsTranscrits.nextElement());
			if(transcrit.exons.isEmpty())
				throw new Exception("Le transcrit " + transcrit.id + " n'a ete associe a aucun exon.");
			if(!transcrit.codingSequences.isEmpty()) {
				String idProteine = transcrit.codingSequences.get(0).id;
				Iterator<Sequence> iterateurSequences = transcrit.codingSequences.iterator();
				iterateurSequences.next();
				while(iterateurSequences.hasNext()) {
					if(!iterateurSequences.next().id.equals(idProteine))
						throw new Exception("Les CDS du transcrit " + transcrit.id + " n'ont pas tous le meme identifiant.");
				};
			};
		};
		TreeMap<String, DNASequence> triGenome = new TreeMap<String, DNASequence>();
		identifiantsGenes = genome.keys();
		while(identifiantsGenes.hasMoreElements()) {
			String identifiant = identifiantsGenes.nextElement();
			triGenome.put(identifiant, genome.get(identifiant));
		};
		return triGenome.values();
	};
	public void annotateIntrons(Collection<DNASequence> genome) throws Exception {
		System.out.println("#annotation\tADNType\tsequenceId\ttranscriptId\tproteinId\tstrand\tintronsPos\tintronsLen");
		Iterator<DNASequence> genes = genome.iterator();
		while(genes.hasNext()) {
			DNASequence gene = genes.next();
			Term terme = termes.get(gene.feature);
			String typeADN = "";
			if(terme.isA("gene")) typeADN = "gene";
			else if(terme.isA("pseudogene")) typeADN = "pseudogene";
			else if(terme.isA("pseudogenic_region")) typeADN = "pseudogenic_region";
			if(typeADN == null)
				throw new Exception("Les sequences genomiques devraient etre des genes, des pseudogenes ou des regions pseudogeniques.");
			Iterator<RNASequence> transcrits = gene.transcripts.iterator();
			while(transcrits.hasNext()) {
				RNASequence transcrit = transcrits.next();
				String idProteine = "";
				int compteCDS = transcrit.codingSequences.size();
				if(compteCDS > 0) idProteine = transcrit.codingSequences.get(0).id;
				char strand = transcrit.strand;
				/*
				Le fichier GFF3 fournit les coordonnées du transcrit, indépendemment des coordonnées de ses exons.
				On peut donc en profiter pour vérifier s'il n'y a pas d'introns avant le premier exon ou après le dernier exon.
				Pour cela, on ajoute le début (E.start) et la fin (E.end) du transcrit sous forme d'ntervalles de longueur 1 nucléotide.
				On ne fait pas cette vérification pour les CDS, car dans ce cas seuls les introns qui séparent les CDS nous intéressent.
				*/
				Intervals exons = new Intervals();
				Introns intronsExons = new Introns();
				int compteExons = transcrit.exons.size();
				exons.add(new Interval(transcrit.start));
				for(int i = 0; i < compteExons; ++i) {
					Sequence exon = transcrit.exons.get(i);
					exons.add(new Interval(exon.start, exon.end));
				};
				exons.add(new Interval(transcrit.end));
				if(exons.size() - 2 >= 2) {
					exons.sort();
					exons.setPositionsFromOne();
					if(strand == '-') exons.reverse();
					int compteIntervallesExons = exons.size();
					long longueurExons = 0;
					for(int l = 1; l < compteIntervallesExons; ++l) {
						if(l != 1) longueurExons += exons.get(l - 1).count();
						long positionIntron = longueurExons;
						long tailleIntron = exons.get(l).start - exons.get(l - 1).end - 1;
						if(tailleIntron > 0) {
							if(l == 1) System.out.println("# Intron avant le premier exon dans ce transcrit.");
							else if(l == compteIntervallesExons - 1) System.out.println("# Intron apres le dernier exon dans ce transcrit.");
							intronsExons.add(positionIntron, tailleIntron);
						};
					};
				};
				System.out.println("transcript\t" + typeADN + "\t" + gene.id + "\t" + transcrit.id + "\t" + idProteine + "\t" + strand + "\t" + intronsExons.positionsToString() + "\t" + intronsExons.lengthsToString());
				if(compteCDS > 0) {
					Intervals CDS = new Intervals();
					Introns intronsCDS = new Introns();
					for(int i = 0; i < compteCDS; ++i) {
						Sequence sequenceCodante = transcrit.codingSequences.get(i);
						CDS.add(new Interval(sequenceCodante.start, sequenceCodante.end));
					};
					if(CDS.size() >= 2) {
						CDS.sort();
						CDS.setPositionsFromOne();
						if(strand == '-') CDS.reverse();
						int compteIntervallesCDS = CDS.size();
						long longueurCDS = 0;
						for(int l = 1; l < compteIntervallesCDS; ++l) {
							longueurCDS += CDS.get(l - 1).count();
							long positionIntron = longueurCDS;
							long tailleIntron = CDS.get(l).start - CDS.get(l - 1).end - 1;
							if(tailleIntron > 0) {
								intronsCDS.add(positionIntron, tailleIntron);
							};
						};
					};
					System.out.println("CDS\t" + typeADN + "\t" + gene.id + "\t" + transcrit.id + "\t" + idProteine + "\t" + strand + "\t" + intronsCDS.positionsToString() + "\t" + intronsCDS.lengthsToString());
				};
			};
		};
	};
};

public class getIntrons {
	public static void main(String[] args) {
		if(args.length == 1) {
			try {
				GFF3Parser analyseurGFF3 = new GFF3Parser();
				analyseurGFF3.annotateIntrons(analyseurGFF3.parseFile(args[0]));
			} catch(Exception e) {
				e.printStackTrace();
			};
		};
	};
};

