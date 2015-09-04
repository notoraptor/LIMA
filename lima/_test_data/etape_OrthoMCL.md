#DESCRIPTION DE L'EXÉCUTION D'ORTHOMCL EFFECTUÉE DANS LE CADRE DU DÉVELOPPEMENT DE LIMA

Les fichiers importants générés pendant l'exécution d'OrthoMCL sont dans le dossier `_test_data/work/my_orthomcl_dir`.

Lien de téléchargement d'OrthoMCL: http://orthomcl.org/common/downloads/software/v2.0/

##(1) install or get access to a supported relational database.  If using MySql, certain configurations are required, so it may involve working with your MySql administrator or installing your own MySql.  See the mysqlInstallationGuide.txt document provided with the orthomcl software.

J’ai installé MySQL sur mon propre ordinateur et j’ai créé une base de donnée appelée « orthomcl ».

##(2) download and install the mcl program according to provided instructions.

Lien de téléchargement : http://www.micans.org/mcl/src/mcl-latest.tar.gz

##(3) install and configure the OrthoMCL suite of programs.

La configuration demande de créer un fichier orthomcl.config dans le dossier `my_orthomcl_dir`.

##(4) run orthomclInstallSchema to install the required schema into the database.

**Commande:**
```
orthomclInstallSchema orthomcl.config
```

Le script crée des tables dans la base de données « orthomcl » de MySQL. Voici les tables créées :
```
+--------------------+
| Tables_in_orthomcl |
+--------------------+
| CoOrtholog         |
| InParalog          |
| InterTaxonMatch    |
| Ortholog           |
| SimilarSequences   |
+--------------------+
```
##(5) run orthomclAdjustFasta (or your own simple script) to generate protein fasta files in the required format.

**Dossier `my_orthomcl_dir/compliantFasta`.**

**Commandes:**
```
orthomclAdjustFasta albu ./original-proteins-files/uncompressed/Albugo_laibachii.ENA1.21.pep.all.fa 1
orthomclAdjustFasta hyal ./original-proteins-files/uncompressed/Hyaloperonospora_arabidopsidis.HyaAraEmoy2_2.0.21.pep.all.fa 1
orthomclAdjustFasta phca ./original-proteins-files/uncompressed/Phyca11_filtered_proteins.fasta 3
orthomclAdjustFasta phci ./original-proteins-files/uncompressed/Phyci1_GeneCatalog_proteins_20120612.aa.fasta 3
orthomclAdjustFasta phin ./original-proteins-files/uncompressed/Phytophthora_infestans.ASM14294v1.21.pep.all.fa 1
orthomclAdjustFasta phpa ./original-proteins-files/uncompressed/phytophthora_parasitica_inra-310_2_proteins.fasta 1
orthomclAdjustFasta phra ./original-proteins-files/uncompressed/Phytophthora_ramorum.ASM14973v1.21.pep.all.fa 1
orthomclAdjustFasta phso ./original-proteins-files/uncompressed/Phytophthora_sojae.ASM14975v1.21.pep.all.fa 1
orthomclAdjustFasta pyul ./original-proteins-files/uncompressed/Pythium_ultimum.pug.21.pep.all.fa 1
```

Sorties : dossier `my_orthomcl_dir/compliantFasta`:
```
albu.fasta
hyal.fasta
phca.fasta
phci.fasta
phin.fasta
phpa.fasta
phra.fasta
phso.fasta
pyul.fasta
```

##(6) run orthomclFilterFasta to filter away poor quality proteins, and optionally remove alternative proteins. Creates a single large goodProteins.fasta file (and a poorProteins.fasta file)

**Commande:**
```
orthomclFilterFasta compliantFasta/ 10 20
```

**Sortie: dossier `my_orthomcl_dir`:**
```
goodProteins.fasta
poorProteins.fasta
```

##(7) run all-v-all NCBI BLAST on goodProteins.fasta (output format is tab delimited text).

**Théorie**

La stratégie suivante a été adoptée à cette étape:
1. créer une base de données regroupant toutes les protéines des 9 espèces étudiées.
2. exécuter blast avec chaque espèce contre la base de données.
3. fusionner les sorties obtenues dans un seul fichier.

Pour le faire, on voulait initialement utiliser NCBI Toolkit, qui fournit les programmes « formatdb » et « blastall ».

J’ai pu utiliser « formatdb », mais « blastall » ne fournissait pas l’option « -m 8″ pour formater la sortie de BLAST.

Or la dernière version de BLAST de l'époque (version 2.2.29+) fournisait non seulement le programme « blastp » avec l’option « -outfmt 6″ équivalente de « -m 8″, mais aussi le programme « makeblastdb » équivalent de « formatdb ».

Donc nous avons carrément utilisé la dernière version de BLAST du début à la fin, pour être sûr que tous les fichiers manipulés seront compatibles.

Lien de téléchargement de la dernière version de BLAST: ftp://ftp.ncbi.nlm.nih.gov/blast/executables/LATEST/

**Application**

**_1. Création de la base de données:_**

*Commande:*
```
makeblastdb -dbtype prot -in goodProteins.fasta -input_type fasta -title goodProteinsBlastDB -hash_index -out goodProteinsBlastDB
```
*Sortie: plusieurs fichiers générés par `makeblastdb`:*
```
goodProteinsBlastDB.phd
goodProteinsBlastDB.pin
goodProteinsBlastDB.psi
goodProteinsBlastDB.phi
goodProteinsBlastDB.pog
goodProteinsBlastDB.psq
goodProteinsBlastDB.phr
goodProteinsBlastDB.psd
````

**_2. exécution de BLAST pour chaque espèce contre la même base de données:_**

Exécution parallèle de 9 scripts, chacun pour 1 espèce:
```
#script 1
blastp -query compliantFasta/albu.fasta -db goodProteinsBlastDB -out albu.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-albu.txt 2> erreur-albu.txt
echo fin albu >> sortie-albu.txt
#script 2
blastp -query compliantFasta/hyal.fasta -db goodProteinsBlastDB -out hyal.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-hyal.txt 2> erreur-hyal.txt
echo fin hyal >> sortie-hyal.txt
#script 3
blastp -query compliantFasta/phca.fasta -db goodProteinsBlastDB -out phca.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-phca.txt 2> erreur-phca.txt
echo fin phca >> sortie-phca.txt
#script 4
blastp -query compliantFasta/phci.fasta -db goodProteinsBlastDB -out phci.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-phci.txt 2> erreur-phci.txt
echo fin phci >> sortie-phci.txt
#script 5
blastp -query compliantFasta/phin.fasta -db goodProteinsBlastDB -out phin.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-phin.txt 2> erreur-phin.txt
echo fin phin >> sortie-phin.txt
#script 6
blastp -query compliantFasta/phpa.fasta -db goodProteinsBlastDB -out phpa.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-phpa.txt 2> erreur-phpa.txt
echo fin phpa >> sortie-phpa.txt
#script 7
blastp -query compliantFasta/phra.fasta -db goodProteinsBlastDB -out phra.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-phra.txt 2> erreur-phra.txt
echo fin phra >> sortie-phra.txt
#script 8
blastp -query compliantFasta/phso.fasta -db goodProteinsBlastDB -out phso.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-phso.txt 2> erreur-phso.txt
echo fin phso >> sortie-phso.txt
#script 9
blastp -query compliantFasta/pyul.fasta -db goodProteinsBlastDB -out ../blastOut/pyul.out.blast -evalue 10 -outfmt 6 -seg yes -num_threads 2 > sortie-pyul.txt 2> erreur-pyul.txt
echo fin pyul >> sortie-pyul.txt
```

*Sorties finales:*
* Fichiers sortie-[espece].txt et erreur-[espece].txt pour chacune des 9 espèces.
  * Tous les fichiers sortie-*.txt contenaient « fin [espèce] ».
  * Seules 2 erreurs détectées dans les fichiers erreur-phca.txt et erreur-phci.txt. Contenu de ces deux fichiers :
    * `Warning: lcl|Query_887 phca|109327: Warning: Could not calculate ungapped Karlin-Altschul parameters due to an invalid query sequence or its translation. Please verify the query sequence(s) and/or filtering options`
    * `Warning: lcl|Query_8449 phci|92918: Warning: Could not calculate ungapped Karlin-Altschul parameters due to an invalid query sequence or its translation. Please verify the query sequence(s) and/or filtering options`
  * Donc deux protéines au total ont été ignorées :
    * `phca|109327`
    * `phci|92918`
* Fichiers [espece].out.blast pour chacune des 9 espèces

**_3. Concaténation des fichiers de sortie de BLASTP_**

*Commande:*
```
cat *.out.blast > superFile.out.blast
```

Le fichier `superFile.out.blast` fait 2,10 Go (mesuré sur Windows 8.1). Il s'agit du fichier final de cette étape.

##(8) run orthomclBlastParser on the NCBI BLAST tab output to create a file of similarities in the required format

**Commande:**
```
orthomclBlastParser superFile.out.blast compliantFasta/ > similarSequences.txt
```

**Sortie: dossier `my_orthomcl_dir`:**
* Fichier `similarSequences.txt` (1,20 Go; mesuré sur Windows 8.1).

##(9) run orthomclLoadBlast to load the output of orthomclBlastParser into the database.

**Commande:**
```
orthomclLoadBlast orthomcl.config similarSequences.txt
```

**Sortie:**

Le script a apparemment travaillé sur les tables de la base de données MySQL « orthomcl ». Certaines tables ont été remplies.
```
mysql> SELECT COUNT(*) FROM InterTaxonMatch
+----------+
| COUNT(*) |
+----------+
| 18801550 |
mysql> SELECT COUNT(*) FROM SimilarSequences;
+----------+
| COUNT(*) |
+----------+
| 22784385 |
+----------+
```

Cette étape a duré plusieurs heures, mais je n'avais pas mesuré précisément le nombre total d'heures écoulées.

**Problèmes rencontrés:**

Pour que cette étape fonctionne, il fallait que MySQL soit exécutée avec une option « local-infile ».

J’ai finalement trouvé une solution sur stackoverflow : http://stackoverflow.com/questions/13155057/load-local-infile-not-allowed-perl-mysql

Il fallait modifier le fichier orthomcl.config et le fichier /etc/mysql/my.conf !

##(10) run the orthomclPairs program to compute pairwise relationships.
**Commande:**
```
orthomclPairs orthomcl.config orthomclPairs.logfile.txt cleanup=yes  > sortie-orthomcl-pairs.txt 2> erreurs-orthomcl-pairs.txt &
```

L’exécution s’était arrêtée quand tout mon disque avait été rempli. Pour relancer l’exécution, j’ai utilisé la commande suivante:
```
orthomclPairs orthomcl.config orthomclPairs.logfile.txt cleanup=yes startAfter=useLog  > sortie-orthomcl-pairs.txt 2> erreurs-orthomcl-pairs.txt &
```

**Sortie:** modification de la base de données par OrthoMCL.

##(11) run the orthomclDumpPairsFiles program to dump the pairs/ directory from the database

**Sortie:** dossier « pairs » et fichier « mclInput », disponibles dans my_orthomcl_dir/pairs-and-mcl-input.zip

##(12) run the mcl program on the mcl_input.txt file created in Step 11.

Exécution dans le dossier my_orthomcl_dir.

Sortie : fichier mclOutput, fichier journal journal=mcl.txt, disponibles dans my_orthomcl_dir.

##(13) run orthomclMclToGroups to convert mcl output to groups.txt

Sortie : Fichier final groups.txt, disponibles dans my_orthomcl_dir.

Le fichier `groups.txt` contient donc la liste des familles trouvées par OrthoMCL !
