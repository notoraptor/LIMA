#CONSTRUCTION DE L'ARBRE PHYLOGÉNÉTIQUE

([Retour au guide principal](https://github.com/notoraptor/LIMA/blob/master/lima/_test_data/README.md))

##I. Sélection des familles à utiliser pour construire l’arbre phylogénétique.

###a) Sélection des familles strictement orthologues.

Une famille strictement orthologue est une famille qui contient exactement 1 protéine pour chaque espèce étudiée (donc 9 protéines pour les 9 espèces de notre jeu de données).

La 4ème colonne du fichier `stats.txt` contient true pour chaque famille strictement orthologue. On peut en déduire la liste des familles:
```
grep true stats.txt | cut -s -f 1 > trueOrthologs.txt
```

1924 familles sont strictement orthologues sur les 18 955 disponibles.

Nous avons ensuite copié les alignements de ces familles dans un dossier séparé pour pouvoir travailler spécifiquement avec eux.

Le script PHP `preg-replace.php` permet rapidement d’appliquer un remplacement de chaîne de caractères ligne par ligne en utilisant une expression régulière:
```
php preg-replace.php "/oomycetes[0-9]+/" "groups-aligned/\$0.aligned.fasta" trueOrthologs.txt > filesTrueOrthologs.txt
```

Ensuite on peut copier les alignements dans un dossier spécifique:
```
mkdir groups-aligned-trueOrthologs
cp `cat filesTrueOrthologs.txt` groups-aligned-trueOrthologs/
```

###b) Filtrage des familles avec Gblocks.

Ensuite nous sélectionnons quelques familles à utiliser pour construire l'arbre parmi les 1924. Nous avons utilisé comme critère de sélection le filtrage de l’alignement par le logiciel Gblocks.
Gblocks permet de filtrer un alignement pour n’en garder que les meilleures régions à utiliser dans les logiciels de phylogénie. En général il sélectionne des régions bien conservées mais avec quand même quelques sites variables. Il prend en entrée des fichiers FASTA et génère en sortie des fichiers FASTA-GB qui sont aussi des fichiers FASTA. Voici la page web du logiciel, contenant des liens de téléchargement:  http://molevol.cmima.csic.es/castresana/Gblocks.html

Nous avons utilisé Gblocks pour filtrer tous les 1924 groupes, en collectant pour chaque groupe deux informations:
* Le pourcentage de positions retenues par Gblocks.
* La longueur de l’alignement après filtrage.

À partir de ces informations, nous avons sélectionné:
* Les groupes qui avaient subi le moins de filtrage, donc les groupes les mieux conservés du point de vue de Gblocks.
* Suffisamment de groupes qui avaient subi le moins de filtrage pour que la concaténation de tous ces groupes contienne assez de positions variables, donc informatives, pour les logiciels de phylogénie.

La sélection a été réalisée à l'aide du script PHP `gblocks-on-aligned-groups.php` qui prend 3 paramètres :
* le dossier contenant les alignements.
* le pourcentage minimal de positions retenues qu’on veut pour les groupes les plus « sûrs ». Nous avons choisi 80%.
* la longueur minimale qu’on veut pour tous les groupes à sélectionner. Nous avons choisi 100 000, pour être sûr d'avoir assez de sites informatifs.

Le programme GBlocks doit se trouver dans le dossier d'exécution du script. Voici la commande utilisée:
```
php gblocks-on-aligned-groups.php groups-aligned-trueOrthologs/ 80 100000 > gblocks-filtering-80-100000.log
```

Le fichier de sortie gblocks-filtering-95-30000.log présente les groupes triés (de manière décroissante) par pourcentage de positions retenues puis par longueur de l’alignement. Donc les groupes des premières lignes du fichier sont ceux ayant le plus haut pourcentage de positions retenues (100, 99, 98, …, 95, etc.) et les plus longs alignements après filtrage. Voici les 7 colonnes exactes du fichier de sortie :

1. Numéro de la ligne courante.
2. Chemin vers l’alignement filtré (fichier fasta-gb dans le dossier de l’alignement initial).
3. « minPercent » si le taux de conservation de l’alignement vaut au moins le taux indiqué en arguments, « false » sinon.
4. « maxLength » si la longueur totale des alignements filtrés (du premier de la sortie jusqu’à l’alignement courant) est au plus la longueur totale indiquée en arguments, « false » sinon.
5. Le taux de conservation de l’alignement filtré (entier naturel, en pourcentage).
6. La longueur de l’alignement filtré (nombre de sites).
7. La longueur totale des alignements filtrés (du premier de la sortie jusqu’à l’alignement courant).

Les lignes d’intérêt sont donc celles qui contiennent à la fois les mots « minPercent » et « maxLength ». Il suffit donc de les sélectionner, puis de copier les fichiers FASTA-GB dans un dossier spécifique:
```
mkdir phylogeny
mkdir phylogeny/selectedGroups
cp `grep "#" -v gblocks-filtering-80-100000.log | grep minPercent | grep maxLength | cut -s -f 2` phylogeny/selectedGroups/
```

254 groupes ont donc été retenus après fitlrage pour faire la phylogénie. Ils sont dans le dossier phylogeny/selectedGroups. La longueur totale de leurs alignements après filtrage est 99 962 sites.

###c) Concaténation des groupes sélectionnés pour obtenir l’alignement final à utiliser dans les logiciels de phylogénie.

Pour pouvoir travailler avec tous les 254 familles sélectionnées en même temps, il faut obtenir pour chaque espèce toutes ses séquences dans ces familles (donc 254 séquences) mises bout à bout l’une à la suite de l’autre, en suivant toujours le même ordre pour toutes les espèces (ici l’ordre alphabétique des noms des familles). On obtient alors 9 séquences d’espèces qui, réunies dans un même fichier, donnent un alignement unique. C’est cet alignement qui sera utilisé par les logiciels de phylogénie.

Cette étape appelée concaténation des groupes est faite avec le programme java lima.concatGblocksResults. Il prend comme paramètres le dossier d’entrée (contenant des fichiers .fasta-gb) et le dossier de sortie. Voici la commande utilisée:
```
java lima.concatGblocksResults phylogeny/selectedGroups/ phylogeny/concatenatedBySpecies > phylogeny/concatenation.log
```

Le dossier phylogeny/concatenatedBySpecies contient donc 9 fichiers FASTA, 1 pour chaque espèce. Il suffit ensuite de les mettre ensemble pour obtenir l’alignement final.
```
cat phylogeny/concatenatedBySpecies/* > phylogeny/alignement-254-proteines-80-100000.fasta
```

Dernière étape : convertir ce fichier FASTA en fichier PHYLIP (.phy) pour qu’il soit utilisable par les logiciel de phylogénie.

La conversion a été réalisée à l'aide du programme graphique seaview téléchargeable ici : http://doua.prabi.fr/software/seaview .

Le fichier final `alignement-254-proteines-80-100000.phy` est dans le dossier `phylogeny`.

On peut maintenant construire l'arbre phylogénétique.

##II. Construction de l’arbre phylogénétique.

Nous présentons ici une construction de l'arbre à l'aide du logiciel RAxML avec détection automatique de la meilleure matrice et choix de l'espèce albu comme outgroup.

Par commodité, nous avons utilisé RAxML-GUI, qui permet une utilisation en interface graphique de RAxML tout en affichant les commandes exécutées.

Lien de téléchargement de RAxML-GUI: http://sourceforge.net/projects/raxmlgui/

Commande exécutée:
```
raxmlHPC-SSE3 -f a -x 12 -k -m PROTGAMMAAUTO -p 181 -N autoMR -o albu -s phylogeny/alignement-254-proteines-80-100000.phy -n alignement-254-proteines-80-100000.tre -O 
```

La sortie du logiciel est dans les fichiers RAxML_* du dossier phylogeny. Le fichier RAxML_info.alignement-254-proteines-80-100000.tre donne un maximum d’informations, et indique notamment que l’alignement a 15164 "distinct patterns".

L’arbre final est dans le fichier RAxML_bipartitionsBranchLabels.alignement-254-proteines-80-100000.tre, avec les longueurs des branches et les valeurs de bootstrap (50 bootstraps effectués).

L’arbre enraciné (avec l’espèce albu comme outgroup) est dans le fichier topology.tre. C'est ce fichier d'arbre qui sera utilisé dans la suite de la méthode.
