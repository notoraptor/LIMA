GUIDE DES ÉTAPES EXÉCUTÉES SUR LE JEU DE DONNÉEES UTILISÉ DANS LE MÉMOIRE
	DOSSIER DE TRAVAIL
		_test_data
	EXTRACTION DES ANNOTATIONS DES INTRONS
		Format de syntaxe:
			java lima.<plateforme>.<typeAnnotation>.getIntrons fichierAnnotation [prefixeGeneID] [prefixeTranscriptID] > fichierSortie.introns
				Les paramètres prefixeGeneID et prefixeTranscriptID ne sont pas disponibles pour les fichiers GFF3 de EnsemblProtists.
				Il s'agit de préfixes (facultatifs) à ajouter aux IDs des gènes et des transcrits dans les annotations des introns générées en sortie. Ils permettent de distinguer si nécessaire des annotations d'introns de différentes espèces si les IDs initiaux des gènes et des transcrits étaient de simples nombres. Par exemple, cela permettrait de distinguer, si nécessaire le transcrit 1000 de l'espèce Phythophthora infestans et le transcrit 1000 de l'espèce Phythophthora cinnammomi.
		Commandes utilisées:
			Dossier C:\donnees\programmation\java\classpath\lima\_test_data\work\introns_annotations
				java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Albugo_laibachii.ENA1.21.gtf > albu.introns
				java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Hyaloperonospora_arabidopsidis.HyaAraEmoy2_2.0.21.gtf > hyal.introns
				java lima.JGI.gff.getIntrons genome-annotations/uncompressed/Phyca11_filtered_genes.gff > phca.introns
				java lima.JGI.gff.getIntrons genome-annotations/uncompressed/Phyci1_GeneCatalog_genes_20120612.gff > phci.introns
				java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Phytophthora_infestans.ASM14294v1.21.gtf > phin.introns
				java lima.broadInstitute.gtf.getIntrons genome-annotations/uncompressed/phytophthora_parasitica_inra-310_2_transcripts.gtf > phpa.introns
				java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Phytophthora_ramorum.ASM14973v1.21.gtf > phra.introns
				java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Phytophthora_sojae.ASM14975v1.21.gtf > phso.introns
				java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Pythium_ultimum.pug.21.gtf > pyul.introns
	CONSTRUCTION DES FAMILLES DE PROTÉINES AVEC ORTHOMCL
		Le fichier etape_OrthoMCL.txt fait le bilan détaillé de l'exécution d'OrthoMCL sur notre jeu de données.