<?php
if($argc == 4) {
	$regexIn = $argv[1];
	$replacement = $argv[2];
	$filename = $argv[3];
	$file = @fopen($filename, "r");
	if($file !== false) {
		while(($line = fgets($file)) !== false) {
			echo preg_replace($regexIn, $replacement, trim($line))."\n";
		};
		@fclose($file);
	};
};
?>