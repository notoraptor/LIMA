<?php
/*
Paramètres (dans l'ordre) :
	groupList	Liste des groupes, supposée déjà triée en fonction du nombre croissant de séquences par groupe.
	groupDir	Dossier contenant les groupes.
	outDir		Dossier de sortie.
	partNumber	Numéro de la section de la liste à traiter (1-n).
	partCount	Nombre de sections à considérer dans la liste (n).
*/
function countLines($filename) {
	$file = @fopen($filename, "r");
	if($file !== false) {
		$n = 0;
		while(fgets($file) !== false) ++$n;
		@fclose($file);
		return $n;
	};
	return -1;
}
if($argc == 6) {
	/* Collecte des paramètres. */
	$groupList = $argv[1];
	$groupDir = rtrim($argv[2], "/\\");
	$outDir = rtrim($argv[3],"/\\");
	$partNumber = intval($argv[4]);
	$partCount = intval($argv[5]);
	/* Vérification des paramètres. */
	if(!file_exists($groupList)) die("Parametre 1 : chemin inexistant.");
	if(!is_file($groupList)) die("Parametre 1 : le chemin n'est pas un fichier.");
	if(!file_exists($groupDir)) die("Parametre 2 : chemin inexistant.");
	if(!is_dir($groupDir)) die("Parametre 2 : le chemin n'est pas un dossier.");
	if(!file_exists($outDir)) {
		if(!mkdir($outDir)) die("Parametre 3 : impossible de creer le dossier.");
	} else if(!is_dir($outDir)) die("Parametre 3 : le chemin n'est pas un dossier.");
	if($partNumber <= 0) die("Parametre 4 : le nombre doit etre un entier naturel non nul.");
	if($partCount <= 0) die("Parametre 5 : le nombre doit etre un entier naturel non nul.");
	if($partNumber > $partCount) die("Parametres 4 et 5 : on s'attend a ce que parametre 4 <= parametre 5.");
	/* Exécution. */
	$lineCount = countLines($groupList);
	if($lineCount <= 0) die("Impossible d'analyser la liste de groupes : nombre de lignes <= 0.");
	$lineFrom = (int)(($lineCount * ($partNumber - 1)) / $partCount) + 1;
	$lineTo = ($partNumber == $partCount) ? $lineCount : (int)( $lineCount * $partNumber / $partCount );
	$file = @fopen($groupList, "r");
	if($file === false) die("Impossible de lire la liste de groupes.");
	$lineRead = 0;
	while($lineRead < $lineFrom - 1) {
		fgets($file);
		++$lineRead;
	};
	echo "Groupes $lineFrom - $lineTo ( ".($lineTo - $lineFrom + 1)." / $lineCount ).\n";
	$muscleCall = getcwd()."/muscle";
	while($lineRead < $lineTo) {
		$groupName = trim(fgets($file));
		++$lineRead;
		$inFilename = $groupDir."/".$groupName.".fasta";
		$outFilename = $outDir."/".$groupName.".aligned.fasta";
		passthru($muscleCall." -in ".$inFilename." -out ".$outFilename." -maxiters 1000");
	};
	@fclose($file);
};
?>