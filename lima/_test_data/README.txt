GUIDE DES ÉTAPES EXÉCUTÉES SUR LE JEU DE DONNÉEES UTILISÉ DANS LE MÉMOIRE
	DOSSIER DE TRAVAIL
		_test_data
	EXTRACTION DES ANNOTATIONS DES INTRONS
		Format de syntaxe:
			java lima.<plateforme>.<typeAnnotation>.getIntrons fichierAnnotation [prefixeGeneID] [prefixeTranscriptID] > fichierSortie.introns
				Les paramètres prefixeGeneID et prefixeTranscriptID ne sont pas disponibles pour les fichiers GFF3 de EnsemblProtists.
				Il s'agit de préfixes (facultatifs) à ajouter aux IDs des gènes et des transcrits dans les annotations des introns générées en sortie. Ils permettent de distinguer si nécessaire des annotations d'introns de différentes espèces si les IDs initiaux des gènes et des transcrits étaient de simples nombres. Par exemple, cela permettrait de distinguer, si nécessaire le transcrit 1000 de l'espèce Phythophthora infestans et le transcrit 1000 de l'espèce Phythophthora cinnammomi.
		Commandes utilisées:
			Dossier _test_data\work\introns_annotations
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
		Le fichier finalement généré est le fichier groups.txt disponible dans le dossier _test_data/work/my_orthomcl_dir.
		Ce fichier contient la liste des familles générées par OrthoMCL. Chaque ligne du fichier représente une famille, dans le format suivant:
			idFamille: idProteine1 idProteine2 idProteine3 ... idProteineN
		18 955 familles ont été générées.
	GÉNÉRATION DES FICHIERS FASTA DES FAMILLES DE PROTÉINES
		Format de syntaxe:
			java lima.builGroups listeGroupesOrthoMCL dossierSequences dossierSortie
				listeGroupesOrthoMCL: fichier groups.txt
				dossierSequences
					Dossier contenant les fichiers .fasta dans lesquels on peut trouver les séquences des groupes.
					Les entêtes des séquences de ces fichiers FASTA doivent être les IDs des séquences, tels qu'ils apparaissent dans listeGroupesOrthoMCL.
					Dossier compliantFasta généré pendant l'exécution d'OrthoMCL.
				dossierSortie
					Dossier de sortie (sera créé par ce programme). *** Le dossier ne doit pas déjà exister !
					Le dossier contiendra un fichier .fasta pour chaque groupe.
		Commande utilisée:
			Dossier _test_data\work\afterOrthoMCL
				java lima.buildGroups ../my_orthomcl_dir/groups.txt ../my_orthomcl_dir/compliantFasta/ groups
			Le dossier "groups" contenant les 18 955 familles au format FASTA est dans l'archive _test_data\work\afterOrthoMCL\groups.tar.gz
	COLLECTE DE STATISTIQUES SUR LES FAMILLES DE PROTÉINES GÉNÉRÉES
		Format de syntaxe:
			java lima.orthogroupsStats groupesOrthoMCL
				groupesOrthoMCL: fichier groups.txt
			Format de sortie
				Chaque ligne donne une information sur un groupe. Les informations sont des colonnes séparées par des tabulations :
					Nom du groupe
					Nombre de séquences dans le groupe
					Nombre d'espèces dans le groupe
					Booléen : le groupe est-il strictement orthologue ?
					9 colonnes donnant le nombre de séquences pour chaque espèce dans le groupe. Ordre des colonnes :
						albu, hyal, phca, phci, phin, phpa, phra, phso, pyul.
		Commande utilisée:
			Dossier _test_data\work\afterOrthoMCL
				java lima.orthogroupsStats ../my_orthomcl_dir/groups.txt > stats.txt
			Le fichier "stats.txt" est dans le dossier afterOrthoMCL.