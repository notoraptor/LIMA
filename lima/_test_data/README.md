# GUIDE DES ÉTAPES EXÉCUTÉES SUR LE JEU DE DONNÉEES UTILISÉ DANS LE MÉMOIRE

Dossier de travail: `_test_data`

## EXTRACTION DES ANNOTATIONS DES INTRONS

**Format de syntaxe:**
```
java lima.<plateforme>.<typeAnnotation>.getIntrons fichierAnnotation [prefixeGeneID] [prefixeTranscriptID] > fichierSortie.introns
```
* Les paramètres prefixeGeneID et prefixeTranscriptID ne sont pas disponibles pour les fichiers GFF3 de EnsemblProtists.
* Il s'agit de préfixes (facultatifs) à ajouter aux IDs des gènes et des transcrits dans les annotations des introns générées en sortie. Ils permettent de distinguer si nécessaire des annotations d'introns de différentes espèces si les IDs initiaux des gènes et des transcrits étaient de simples nombres. Par exemple, cela permettrait de distinguer, si nécessaire le transcrit 1000 de l'espèce Phythophthora infestans et le transcrit 1000 de l'espèce Phythophthora cinnammomi.

**Commandes utilisées: dossier `_test_data\work\introns_annotations`:**
```
java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Albugo_laibachii.ENA1.21.gtf > albu.introns
java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Hyaloperonospora_arabidopsidis.HyaAraEmoy2_2.0.21.gtf > hyal.introns
java lima.JGI.gff.getIntrons genome-annotations/uncompressed/Phyca11_filtered_genes.gff > phca.introns
java lima.JGI.gff.getIntrons genome-annotations/uncompressed/Phyci1_GeneCatalog_genes_20120612.gff > phci.introns
java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Phytophthora_infestans.ASM14294v1.21.gtf > phin.introns
java lima.broadInstitute.gtf.getIntrons genome-annotations/uncompressed/phytophthora_parasitica_inra-310_2_transcripts.gtf > phpa.introns
java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Phytophthora_ramorum.ASM14973v1.21.gtf > phra.introns
java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Phytophthora_sojae.ASM14975v1.21.gtf > phso.introns
java lima.ensemblProtists.gtf.getIntrons genome-annotations/uncompressed/Pythium_ultimum.pug.21.gtf > pyul.introns
```

##COLLECTE DE STATISTIQUES SUR LES LONGUEURS DES INTRONS

Le programme java lima.intronsLengthsBySpecies collecte des informations sur les longueurs des introns des espèces étudiées. Il analyse les fichiers « .introns » d’un dossier et en déduit pour chaque espèce:
* les longueurs d’introns rencontrées.
* le nombre d’occurences de chaque longueur.
Le programme génère aussi un rapport dans la sortie standard sous la forme d’un tableau donnant quelques informations sur chaque espèce. Les colonnes du tableau sont:
* Le nom de l’espèce.
* Le nombre d’introns dans cette espèce (nombre d’introns trouvés dans le fichier .introns).
* La plus petite longueur d’intron rencontrée.
* La plus grande longueur d’intron rencontrée.
* La longueur moyenne d’un intron.
* La longueur d’intron la plus représentée.
* Le nombre d’occurrences de la longueur d’intron la plus représentée.

**Syntaxe d'utilisation:**
```
java lima.intronsLengthsBySpecies dossierIntrons dossierSortie
```
**Commande utilisée: dossier `introns_annotations`:**
```
java lima.intronsLengthsBySpecies . intronsLengthsBySpecies > intronsLenghtsBySpecies.txt
```
Le rapport intronsLenghtsBySpecies.txt a ensuite été déplacé dans le dossier nouvellement créé intronsLenghtsBySpecies.

Le dossier intronsLengthsBySpecies contient pour chaque espèce un fichier .lengths à deux colonnes : longueurs d’introns et nombre d’occurences pour chaque longueur. Les colonnes sont triées par ordre croissant des longueurs d’introns.

##CONSTRUCTION DES FAMILLES DE PROTÉINES AVEC ORTHOMCL

Le fichier etape_OrthoMCL.txt fait le bilan détaillé de l'exécution d'OrthoMCL sur notre jeu de données.

Le fichier finalement généré est le fichier groups.txt disponible dans le dossier _test_data/work/my_orthomcl_dir.

Ce fichier contient la liste des familles générées par OrthoMCL. Chaque ligne du fichier représente une famille, dans le format suivant:
```
idFamille: idProteine1 idProteine2 idProteine3 ... idProteineN
```
18 955 familles ont été générées.

##GÉNÉRATION DES FICHIERS FASTA DES FAMILLES DE PROTÉINES

Format de syntaxe:
```
java lima.builGroups listeGroupesOrthoMCL dossierSequences dossierSortie
```
* listeGroupesOrthoMCL: fichier groups.txt
* dossierSequences
  * Dossier contenant les fichiers .fasta dans lesquels on peut trouver les séquences des groupes.
  * Les entêtes des séquences de ces fichiers FASTA doivent être les IDs des séquences, tels qu'ils apparaissent dans listeGroupesOrthoMCL.
  * Dossier compliantFasta généré pendant l'exécution d'OrthoMCL.
* dossierSortie
  * Dossier de sortie (sera créé par ce programme). *** Le dossier ne doit pas déjà exister !
  * Le dossier contiendra un fichier .fasta pour chaque groupe.

Commande utilisée: dossier _test_data\work\afterOrthoMCL
```
java lima.buildGroups ../my_orthomcl_dir/groups.txt ../my_orthomcl_dir/compliantFasta/ groups
```
Le dossier "groups" contenant les 18 955 familles au format FASTA est dans l'archive `_test_data\work\afterOrthoMCL\groups.tar.gz`.

##COLLECTE DE STATISTIQUES SUR LES FAMILLES DE PROTÉINES GÉNÉRÉES

**Format de syntaxe:**
```
java lima.orthogroupsStats groupesOrthoMCL
```
* groupesOrthoMCL: fichier groups.txt
**Format de sortie:**

Chaque ligne donne une information sur un groupe. Les informations sont des colonnes séparées par des tabulations :
* Nom du groupe
* Nombre de séquences dans le groupe
* Nombre d'espèces dans le groupe
* Booléen : le groupe est-il strictement orthologue ?
* 9 colonnes donnant le nombre de séquences pour chaque espèce dans le groupe. Ordre des colonnes :
  * albu, hyal, phca, phci, phin, phpa, phra, phso, pyul.

**Commande utilisée: dossier `_test_data\work\afterOrthoMCL`:**
```
java lima.orthogroupsStats ../my_orthomcl_dir/groups.txt > stats.txt
```
Le fichier "stats.txt" est dans le dossier afterOrthoMCL.

##ALIGNEMENT DES FAMILLES DE PROTÉINES

Le fichier etape_alignement.txt décrit la procédure d'alignement exécutée.

Elle consiste en l'exécution du programme MUSCLE avec l'option "-maxiters 1000" sur chaque famille disponible.

Un script PHP a été écrit pour permettre l'alignement en parallèle de plusieurs lots de familles, afin d'accélérer l'étape.

Les alignements générés ont pour extension .aligned.fasta.

Les alignements des familles sont dans le dossier groups-aligned (archive groupes-aligned.tar.gz dans le dossier afterOrthoMCL).

##CONSTRUCTION DE L'ARBRE PHYLOGÉNÉTIQUE

La construction de l'arbre phylogénétique est décrite dans le fichier etape_construction_arbre.txt

Le résult final est un arbre au format NEWICK dans le fichier topology.tre dans le dossier afterOrthoMCL/phylogeny.

##GÉNÉRATION DE FICHIERS FASTA PERSONNALISÉS ASSOCIANT LES ALIGNEMENTS DES FAMILLES ET LES POSITIONS DES INTRONS SUR LES PROTÉINES
Le positionnement des introns a été effectué avec le programme java `lima.positionIntronsOnGroups`. Il prend 3 paramètres :
* le dossier contenant les annotations des introns (fichiers .introns). Les fichiers doivent être rigoureusement nommés <nom raccourci de l’espèce>.introns pour que le programme puisse associer les bons IDs de séquences.
* Le dossier contenant les alignements à marquer.
* Le dossier de sortie.

Voici les commandes utilisées:
```
java lima.positionIntronsOnGroups ../introns_annotations/ groups-aligned-trueOrthologs groups-aligned-trueOrthologs-marked
java lima.positionIntronsOnGroups ../introns_annotations/ groups-aligned groups-aligned-marked
```
Le programme génère des fichiers AMF ayant l'extension .aligned.marked.fasta.

##COLLECTE DE STATISTIQUES SUR LA DISTRIBUTION DES INTRONS AUTOUR DES TROUS

Le programme JAVA lima.FisherTestOnMarkedAlignments parcourt les fichiers AMF et compte des fenêtres (portions d’alignements) ayant des caractéristiques données. 4 types de fenêtres sont recensées:
* Les fenêtre qui ne contiennent que des trous (fenêtres « GAP »).
* Les fenêtres qui ne contiennent que des introns (fenêtres « INTRON »).
* Les fenêtres qui contiennent à la fois des introns et des trous (fenêtres « GAPINTRON »).
* Les fenêtres qui ne contiennent rien (ni introns ni trous) (fenêtres « EMPTY »).

Les fenêtres considérées ont une longueur qu’on peut spécifier au programme. Les fenêtres sont disjointes : par exemple pour une longueur de fenêtre de 10, le programme analyse les 10 premières colonnes de l’alignement, puis les 10 suivantes, et ainsi de suite.

**Syntaxe d'utilisation:**
```
java lima.FisherTestOnMarkedAlignments dossierAlignements largeurFenetre > rapport.txt
```
Le programme a été exécuté sur l’ensemble des groupes trouvés par OrthoMCL (18955 groupes au total). La largeur de fenêtre utilisée fut de 10 colonnes. Les commandes ont été exécutées dans un sous-dossier « FisherTest » du dossier afterOrthoMCL :

Commandes utilisées: dossier afterOrthoMCL:
```
mkdir FisherTest
cd FisherTest
java lima.FisherTestOnMarkedAlignments ../groups-aligned-marked 10 > onAllGroups-window10.txt
```

##ANALYSE SPÉCIFIQUE DES FAMILLES DE PROTÉINES STRICTEMENT ORTHOLOGUES

###RECONSTRUCTION DES SÉQUENCES ANCESTRALES

Syntaxe d'utilisation:
```
java lima.ancestors.Rebuild.orthologFamily treeFile=arbreNEWICK alignmentPath=dossierFichiersAMF
```
**Format de sortie:**
Pour chaque alignement, 3 fichiers sont produits :
* Un fichier .withAncestors.seqtree qui montre l’arbre des espèces avec en face l’alignement de toutes les séquences (connues et reconstituées).
* un fichier .withAncestors.pca (pca = « parent-children alignment ») qui montre l’alignement de chaque ancêtre avec ses descendants immédiats.
* Un fichier .withAncestors.fasta qui contient les séquences alignées ancestrales reconstituées, et les séquences des gènes traitées (avec les nouveaux caractères pour repérer les introns).
Commande utilisée: dossier afterOrthoMCL:
```
mkdir trueOrthologsAncestorsRebuilt
cd trueOrthologsAncestorsRebuilt
java lima.ancestors.Rebuild.orthologFamily treeFile=../phylogeny/topology.tre alignmentPath=../groups-aligned-trueOrthologs-marked logfile=log.txt
```

###DÉTECTION DES ÉVÈNEMENTS

Syntaxe d'utilisation:
```
java lima.ancestors.events.Detect path=dossierFichiersPCA p=1 q=0.5
```
* dossierFichiersPCA. Les fichiers .pca (Parent-Children Alignments) ont été générés à l’étape précédente de reconstruction des séquences ancestrales. Ces fichiers contiennent les alignements de chaque ancêtre avec ses deux descendants directs, donc c’est facile de récupérer les paires ancêtre-descendant pour les analyser.
* p: paramètre de contrôle du poids des colonnes instables (1 par défaut).
* q: paramètre de contrôle de l'effet des colonnes stables (0.5 par défaut).

**Format de sortie:**

Pour chaque fichier .pca analysé, le programme génère deux fichiers .instability et .events dans le dossier de son exécution.

Le fichier .instability présente chaque paire de séquence ancêtre-descendant en montrant les zones « instables » détectées (des astérisques sont en dessous de chaque zone instable de l’alignement).

Le fichier .events décrit les évènements trouvés. Chaque évènement est décrit sur 3 lignes:
* La ligne 1 commence par @ et indique des informations sur l’évènement. Dans l’ordre:
  * Le nom de la famille de gènes (exemple : oomycetes4365).
  * Le nom de l’ancêtre (exemple : root, ancester1, hyal, etc.).
  * Le nom du descendant.
  * La longueur de l’alignement complet ancêtre-descendant.
  * La position de début de l’évènement dans l’alignement (les positions commencent à partir de 1).
  * La position de fin de l’évènement dans l’alignement (les positions commencent à partir de 1).
  * (après 3 tabulations, donc 9ème colonne) le « type » déduit pour cet évènement, dans le format « séquenceAncêtre/séquenceDescendant ».
  * (après 3 tabulations, donc 12ème colonne) L’évènement proprement dit (vraies séquences) toujours dans le format « séquenceAncêtre/séquenceDescendant ».
* Les lignes 2 et 3 affichent le « type » et l’évènement sur deux lignes (au lieu du format « séquenceAncêtre/séquenceDescendant ») pour une meilleure lisibilité.

**Commande utilisée: dossier `afterOrthoMCL`:**
```
mkdir trueOrthologsEvents
cd trueOrthologsEvents
java lima.ancestors.events.Detect path=../trueOrthologsAncestorsRebuilt
```
