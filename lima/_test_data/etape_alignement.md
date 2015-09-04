ALIGNEMENT DES FAMILLES DE PROTÉINES

Les familles ont été alignées avec le logiciel MUSCLE (MUSCLE v3.8.31 by Robert C. Edgar) à l'aide du script PHP `align-groups.php`.

Le script fonctionne à partir d’une liste de familles et peut être lancé plusieurs fois en parallèle sur la même liste pour aligner rapidement toutes les familles.

**Syntaxe d'utilisation du script:**
```
php align-groups.php groupList groupDir outDir partNumber partCount
```
* `groupList`: la liste des familles à aligner. Il s’agit d’un fichier qui doit contenir un nom de famille par ligne (seulement le nom, sans extension). En pratique, nous avons utilisé une liste des familles triées par ordre croissant du nombre de séquences par familles. Ainsi les familles contenant le plus de séquences sont alignées en dernier, les familles contenant le moins de séquences en premier, ce qui permet de ne pas bloquer l’exécution du script, et d’avoir très vite beaucoup de fichiers d’alignements (on peut donc observer ces alignements en attendant la fin de l’exécution du script).
* `groupDir`: le dossier contenant les familles. Chaque fichier doit être nommé comme suit : <nom de la famille>.fasta.
* `outDir`: le dossier de sortie (sera créé s’il n’existe pas, peut déjà exister).
* `partNumber`: partie de la liste à aligner (1 à partCount).
* `partCount`: nombre de parties à considérer dans la liste (entier naturel non nul). Par exemple, si la liste contient 100 lignes et que partNumber = 2 et partCount = 5, alors l’exécution du script va aligner les familles de la 2ème des 5 parties de la liste. Une partie contient 100/5 = 20 lignes (donc 20 familles), donc la 2ème partie va de la 21ème à la 40ème ligne.

**_Génération de la groupList: dossier afterOrthoMCL:_**

Le fichier `stats.txt` précédemment généré décrit dans ses deux premières colonnes le nom et le nombre de séquences pour chaque famille.

On peut donc en déduire notre groupList. On supprime aussi les lignes commentées qui commencent par « # » :
```
grep "#" -v stats.txt | cut -s -f 1,2 | sort -n -k 2 | cut -s -f 1 > sorted-seqcount-asc.txt
```

La liste est donc dans le fichier `sorted-seqcount-asc.txt`.

**Commandes utilisées:**

Le programme MUSCLE doit se trouver dans le dossier d’exécution du script PHP.

Lien de téléchargement de MUSCLE: http://www.drive5.com/muscle/downloads.htm .

10 parties ont été considérées pour la liste.  Les commandes exactes utilisées sont les suivantes:
```
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 1 10 > alignment-1.out 2> alignment-1.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 2 10 > alignment-2.out 2> alignment-2.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 3 10 > alignment-3.out 2> alignment-3.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 4 10 > alignment-4.out 2> alignment-4.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 5 10 > alignment-5.out 2> alignment-5.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 6 10 > alignment-6.out 2> alignment-6.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 7 10 > alignment-7.out 2> alignment-7.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 8 10 > alignment-8.out 2> alignment-8.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 9 10 > alignment-9.out 2> alignment-9.err &
php align-groups.php sorted-seqcount-asc.txt groups groups-aligned 10 10 > alignment-10.out 2> alignment-10.err &
```

Les fichiers `alignment-*.err` contiennent les sorties de MUSCLE (informations sur les itérations pour chaque famille alignée).

Les fichiers `alignment-*.out` décrivent les parties de la liste considérées pour chaque exécution. Voici le contenu de ces fichiers:
```
Groupes 1896 - 3791 ( 1896 / 18955 ).
Groupes 3792 - 5686 ( 1895 / 18955 ).
Groupes 5687 - 7582 ( 1896 / 18955 ).
Groupes 7583 - 9477 ( 1895 / 18955 ).
Groupes 9478 - 11373 ( 1896 / 18955 ).
Groupes 11374 - 13268 ( 1895 / 18955 ).
Groupes 13269 - 15164 ( 1896 / 18955 ).
Groupes 15165 - 17059 ( 1895 / 18955 ).
Groupes 17060 - 18955 ( 1896 / 18955 ).
```

Les 18 955 familles disponibles ont donc été alignées dans le dossier `groups-aligned` (archive `groups-aligned.tar.gz`).
