/*
Analyseur syntaxique pour le format Newick. Génère un arbre représenté par sa racine (class TreeNode).
Le programme suivant affiche l'arbre analysé sous la forme de lignes avec des tabulations (une ligne pour chaque noeud).
Syntaxe
	java maitrise.parsing.Newick newickFile
*/
package lima.parsing;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
public class Newick {
	// Constantes.
	private static final int TREE = 0;
	private static final int BRANCH = 1;
	private static final int STRING = 2;
	private static final int NUMBER = 3;
	private static final String emptyChars = " \t\r\n";
	private static final String specialDelimiters = "'\"";
	// Attributs.
	private StringBuffer parsed;
	private int parsedLength;
	private int cursor;
	// Méthodes.
	public Newick(StringBuffer sb) {
		parsed = sb;
		parsedLength = sb.length();
		cursor = 0;
	}
	public Newick(File filename) throws Exception {
		parsed = new StringBuffer();
		BufferedReader br = new BufferedReader(new FileReader(filename.getAbsolutePath()));
		String line = null;
		try {
			while((line = br.readLine()) != null) parsed.append(line.trim());
		} finally {
			br.close();
		};
		parsedLength = parsed.length();
		cursor = 0;
	}
	public void terminate() { cursor = parsedLength; }
	public boolean isEnd() { return cursor >= parsedLength; }
	public boolean isNumericChar(char c) { return c == '.' || (c >= '0' && c <= '9'); }
	public boolean isEmptyChar(char c) { return emptyChars.indexOf(c) >= 0; }
	public void parseString(TreeNode node, String s) {
		s = s.trim();
		if(!s.isEmpty()) {
			if(isNumericChar(s.charAt(0)))
				node.score = Double.parseDouble(s);
			else
				node.name = s;
		}
	}
	public TreeNode parse() {
		TreeNode root = new TreeNode();
		TreeNode node = root;
		TreeNode n = null;
		int state = TREE;
		String piece = "";
		while(!isEnd()) {
			char c = parsed.charAt(cursor++);
			switch(state) {
				case TREE:
					switch(c) {
						case '(':
							state = BRANCH;
							n = new TreeNode();
							node.addChild(n);
							node = n;
							break;
						case ';': terminate(); break;
						case ':': state = NUMBER; break;
						default:
							if(!isEmptyChar(c)) {
								state = STRING;
								piece += c;
							};
							break;
					}
					break;
				case BRANCH:
					switch(c) {
						case '(':
							n = new TreeNode();
							node.addChild(n);
							node = n;
							break;
						case ')':
							if(node.parent != null) node = node.parent;
							break;
						case ',':
							if(node == root) {
								root = new TreeNode();
								root.addChild(node);
							};
							n = new TreeNode();
							node.parent.addChild(n);
							node = n;
							break;
						case ';': terminate(); break;
						case ':': state = NUMBER; break;
						default:
							if(!isEmptyChar(c)) {
								state = STRING;
								piece += c;
							};
							break;
					};
					break;
				case STRING:
					int specialDelimiterIndex = specialDelimiters.indexOf(piece.charAt(0));
					if(specialDelimiterIndex >= 0) {
						char specialDelimiter = specialDelimiters.charAt(specialDelimiterIndex);
						if(c == specialDelimiter) {
							parseString(node, piece.substring(1));
							piece = "";
							if(!isEnd()) {
								do {
									c = parsed.charAt(cursor++);
								} while(isEmptyChar(c) && !isEnd());
								switch(c) {
									case ')' :
										state = TREE;
										if(node.parent != null) {
											node = node.parent;
											state = BRANCH;
										};
										break;
									case ',' :
										if(node == root) {
											root = new TreeNode();
											root.addChild(node);
										};
										n = new TreeNode();
										node.parent.addChild(n);
										node = n;
										state = BRANCH;
										break;
									case ':' : state = NUMBER; break;
									case ';' :
									default:
										terminate();
										break;
								};
							};
						} else {
							piece += c;
						};
					} else switch(c) {
						case ')' :
							parseString(node, piece);
							piece = "";
							state = TREE;
							if(node.parent != null) {
								node = node.parent;
								state = BRANCH;
							};
							break;
						case ',' :
							parseString(node, piece);
							piece = "";
							if(node == root) {
								root = new TreeNode();
								root.addChild(node);
							};
							n = new TreeNode();
							node.parent.addChild(n);
							node = n;
							state = BRANCH;
							break;
						case ';' :
							parseString(node, piece);
							piece = "";
							terminate();
							break;
						case ':' :
							parseString(node, piece);
							piece = "";
							state = NUMBER;
							break;
						default:
							piece += c;
							break;
					};
					break;
				case NUMBER:
					switch(c) {
						case ')' :
							node.branchLength = Double.parseDouble(piece.trim());
							piece = "";
							state = TREE;
							if(node.parent != null) {
								node = node.parent;
								state = BRANCH;
							};
							break;
						case ',' :
							node.branchLength = Double.parseDouble(piece.trim());
							piece = "";
							if(node == root) {
								root = new TreeNode();
								root.addChild(node);
							};
							n = new TreeNode();
							node.parent.addChild(n);
							node = n;
							state = BRANCH;
							break;
						case ';' :
							node.branchLength = Double.parseDouble(piece.trim());
							piece = "";
							terminate();
							break;
						default:
							piece += c;
							break;
					};
					break;
				default:
					break;
			};
		};
		return root;
	}
	// Fonction main.
	public static void main(String[] args) {
		if(args.length == 1) try {
			Newick parser = new Newick(new File(args[0]));
			TreeNode root = parser.parse();
			if(root == null) throw new Exception("Parsing failed.");
			if(root.name == null) root.name = "root";
			root.print("");
		} catch(Exception e) {
			e.printStackTrace();
		};
	}
}

