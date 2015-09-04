<?php
/*
Filtre les alignements .fasta d'un dossier avec le logiciel Gblocks.
Les alignements filtrés sont écrits dans des fichiers .fasta-gb dans le même dossier.
Le programme écrit dans la sortie des informations sur le filtrage.
Format de sortie :
	La sortie présente les alignements filtrés triés par ordre décroissant de leur taux de conservation et de leur longueur.
	Le taux de conservation est un pourcentage (entier naturel) qui indique quelle part de l'alignement initial a été retenue dans le filtrage.
	La longueur est la longueur de l'alignement filtré.
	Chaque ligne représente un alignement. Les informations sont réparties dans 7 colonnes :
		Numéro de la ligne courante.
		Chemin vers l'alignement filtré.
		"minPercent" si le taux de conservation de l'alignement vaut au moins le taux indiqué en arguments (voir la syntaxe), "false" sinon.
		"maxLength" si la longueur totale des alignements (du premier de la sortie jusqu'à l'alignement courant) est au plus la longueur totale indiquée en arguments (voir la syntaxe), "false" sinon.
		Le taux de conservation de l'alignement filtré (entier naturel, en pourcentage).
		La longueur de l'alignement filtré (nombre de sites).
		La longueur totale des alignements filtrés (du premier de la sortie jusqu'à l'alignement courant).
Syntaxe :
	php gblocks-on-aligned-groups.php dossierAlignements [pourcentageMinimal, 50 par défaut] [longueurTotaleMaximale, 10000 par défaut] > fichierSortie
*/
if($argc >= 2) {
	$path = rtrim($argv[1],"/\\");
	$minPercent = $argc > 2 ? intval($argv[2]) : 50;
	$maxLength = $argc > 3 ? intval($argv[3]) : 10000;
	if(!file_exists($path)) die("Le chemin specifie n'existe pas.");
	if(!is_dir($path)) die("Le chemin specifie n'est pas un dossier.");
	$filenames = scandir($path);
	$extension = ".fasta";
	$table = array();
	$compteLignes = 0;
	foreach($filenames as $filename) {
		if(strpos($filename, $extension) === strlen($filename) - strlen($extension)) {
			++$compteLignes;
			$fullFilename = $path."/".$filename;
			$output = array();
			exec(getcwd()."/Gblocks ".$fullFilename." -s=y -p=t -b5=n -e=-gb", $output);
			if($compteLignes % 100 == 0) fprintf(STDERR, "$compteLignes groupes lus.\n");
			$percent = -1;
			$length = -1;
			foreach($output as $outline) {
				$outline = trim($outline);
				$matches = array();
				if(preg_match("/^Gblocks +alignment\: +([0-9]+) +positions +\(([0-9]+) +\%\) +in +[0-9]+ +selected +block\(s\)$/", $outline, $matches)) {
					$length = intval($matches[1]);
					$percent = intval($matches[2]);
					break;
				};
			};
			if($percent === -1) die("Impossible de reperer le pourcentage de positions conservees dans la sortie de Gblocks (fichier $fullFilename).");
			if($length === -1) die("Impossible de reperer la longueur de l'alignement dans la sortie de Gblocks (fichier $fullFilename).");
			$table[] = array($fullFilename."-gb", $percent, $length);
		};
	};
	fprintf(STDERR, "$compteLignes groupes lus au total.\n");
	usort($table, function($a, $b) {
		$d = $a[1] - $b[1];
		if($d == 0) $d = $a[2] - $b[2];
		if($d == 0) $d = strcmp($a[0], $b[0]);
		return -$d;
	});
	$c = count($table);
	echo "# $c alignements.\n";
	echo "#number\t#file\t#minPercent?\t#maxLength?\t#percent\t#GblocksLength\t#GblocksTotalLength\n";
	$totalLength = 0.0;
	for($i = 0; $i < $c; ++$i) {
		$t = $table[$i];
		$totalLength += $t[2];
		echo ($i+1)."\t".$t[0]."\t".($t[1] >= $minPercent ? "minPercent" : "false")."\t".($totalLength <= $maxLength ? "maxLength" : "false")."\t".$t[1]."\t".$t[2]."\t".$totalLength."\n";
	};
	fprintf(STDERR,"fin\n");
};
?>