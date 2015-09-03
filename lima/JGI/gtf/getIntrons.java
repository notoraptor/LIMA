package lima.JGI.gtf;
import lima.ensemblProtists.Interval;
import lima.ensemblProtists.Intervals;
import lima.ensemblProtists.Introns;
import lima.ensemblProtists.gtf.Sequence;
import lima.ensemblProtists.gtf.Protein;
import lima.ensemblProtists.gtf.Transcript;
import lima.ensemblProtists.gtf.Gene;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.FileReader;
import java.io.BufferedReader;

public class getIntrons {
	public static void getAnnotation(String cheminFichierGTF, String prefixeGeneId, String prefixeTranscriptId) throws Exception {
		String tagGeneId = "gene_id ";
		String tagTranscriptId = "transcript_id ";
		String tagProteinId = "protein_id ";
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
					if(transcriptId == null && proteinId == null)
						throw new Exception("Ligne " + numeroLigne + " : ID du transcrit et de la proteine absents.");
					if(feature.equals("exon") && transcriptId == null)
						throw new Exception("Ligne " + numeroLigne + " : exon sans ID de transcrit.");
					if(feature.equals("CDS") && (proteinId == null || transcriptId == null))
						throw new Exception("Ligne " + numeroLigne + " : CDS sans ID de proteine ou de transcrit.");
					Sequence sequence = new Sequence();
					sequence.feature = feature;
					sequence.start = start;
					sequence.end = end;
					sequence.strand = strand;
					geneId = prefixeGeneId + geneId;
					transcriptId = prefixeTranscriptId + transcriptId;
					proteinId = prefixeTranscriptId + proteinId;
					if(!donnees.containsKey(geneId)) donnees.put(geneId, new Gene(geneId));
					Gene gene = donnees.get(geneId);
					if(feature.equals("exon")) {
						gene.addToTranscript(transcriptId, sequence);
					} else {
						if(!gene.hasTranscript(transcriptId)) gene.createTranscript(transcriptId);
						Transcript transcrit = gene.getTranscript(transcriptId);
						Protein proteine = transcrit.getProtein(proteinId);
						if(proteine == null) {
							proteine = new Protein(proteinId);
							transcrit.addProtein(proteine);
						};
						proteine.addSequence(sequence);
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

