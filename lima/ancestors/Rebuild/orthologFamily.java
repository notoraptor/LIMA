/*
Reconstruction des séquences alignées ancestrales via la méthode de parcimonie.
Syntaxe :
	java maitrise.ancestors.Rebuild.orthologFamily NewickFile AlignedMarkedFastaFile
	java maitrise.ancestors.Rebuild.orthologFamily NewickFile AlignedMarkedFastaFolder
Sortie (dans le dossier d'exécution) : pour chaque alignement :
	Fichier .withAncestors.fasta contenant les séquences alignées ancestrales.
	Fichier .withAncestors.seqtree mettant en évidence tous les séquences disponibles (connues et reconstruites).
	Fichier .withAncestors.pca montrant les alignements de chaque ancêtre avec ses descendants immédiats.
*/
package lima.ancestors.Rebuild;
import lima.tools.ProgramBase;
import lima.parsing.TreeNode;
import lima.parsing.Newick;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.TreeMap;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
class Intron {
	public int positionCDS;
	public int length;
	public int phase;
	public Sequence parent;
	public Intron() {
		positionCDS = 0;
		length = 0;
		phase = -1;
		parent = null;
	}
}
class Sequence {
	public String id;
	public String species;
	public String content;
	public Hashtable<Integer, Intron> introns;
	public Sequence() {
		id = null;
		species = null;
		content = null;
		introns = new Hashtable<Integer, Intron>();
	}
}
class State implements Comparable<State> {
	// PS : L'acide aminé "X" est rajouté car certaines séquences contiennent ce caractère (pour indiquer un caractère inconnu).
	// PS : Le nucléotide "N" est rajouté car certaines séquences contiennent ce caractère (pour indiquer un caractère inconnu).
	public static final String lowerAminoAcids = "acdefghiklmnpqrstvwxy";
	public static final String aminoAcids = "ACDEFGHIKLMNPQRSTVWXY";
	public static final String nucleotides = "ATCGN";
	public static final String emptyChars = " \t\r\n";
	// Classes d'états : absents (trou, ou intron absent) ou présents (molécule, ou intron présent).
	public static final int ABSENT = 0;
	public static final int PRESENT = 1;
	public char value;
	public StateSet undecidedFrom;
	public State(char c) { value = c; undecidedFrom = null; }
	public boolean equals(Object o) {
		return o != null && o instanceof State && value == ((State)o).value;
	}
	public int compareTo(State other) {
		return ((int)value - (int)other.value);
	}
	public char type() {
		char type = value;
		if(aminoAcids.indexOf(value) >= 0) type = 'X';
		else if(lowerAminoAcids.indexOf(value) >= 0) type = 'x';
		else if(nucleotides.indexOf(value) >= 0) type = 'N';
		return type;
	}
	public static boolean isMolecularType(char c) {
		return c == 'X' || c == 'x' || c == 'N';
	}
	public boolean isAbsent() {
		return value == '.' || value == '-';
	}
	public static  boolean isLowerChar(char c) {
		return c >= 'a' && c <= 'z';
	}
	public static boolean isEmptyChar(char c) {
		return emptyChars.indexOf(c) >= 0;
	}
	public static char toLowerChar(char c) {
		if(c >= 'A' && c <= 'Z') {
			int distance = (int)'A' -(int)'a';
			c = (char)((int)c - distance);
		};
		return c;
	}
	public static ArrayList<ArrayList<State>> classifyStates(StateSet states) {
		ArrayList<ArrayList<State>> classes = new ArrayList<ArrayList<State>>();
		ArrayList<State> absent = new ArrayList<State>();
		ArrayList<State> present = new ArrayList<State>();
		for(State state : states) {
			if(state.isAbsent()) absent.add(state);
			else present.add(state);
		}
		if(!absent.isEmpty()) classes.add(absent);
		if(!present.isEmpty()) classes.add(present);
		return classes;
	}
}
class IntronState extends State {
	public int length;
	public IntronState(Intron intron) {
		super('+');
		length = intron.length;
	}
	public boolean equals(Object o) {
		if(o != null && o instanceof IntronState) {
			IntronState other = (IntronState)o;
			return value == other.value && length == other.length;
		}
		return false;
	}
}
class StateSet extends TreeSet<State> {
	public StateSet() {
		super();
	}
}
class States {
	private ArrayList<State> states;
	public States() {
		states = new ArrayList<State>();
	}
	public States(char c) {
		this();
		states.add(new State(c));
	}
	public States(Intron intron) {
		this();
		states.add(new IntronState(intron));
	}
	public States(String s) {
		this();
		int length = s.length();
		for(int i = 0; i < length; ++i) states.add(new State(s.charAt(i)));
	}
	public States add(State state) {
		states.add(state);
		return this;
	}
	public States add(char c) {
		states.add(new State(c));
		return this;
	}
	public States add(Intron intron) {
		states.add(new IntronState(intron));
		return this;
	}
	public States add(States other) {
		states.addAll(other.states);
		return this;
	}
	public States set(int p, State state) {
		states.set(p, state);
		return this;
	}
	public States set(int p, char c) {
		states.set(p, new State(c));
		return this;
	}
	public States set(int p, Intron intron) {
		states.set(p, new IntronState(intron));
		return this;
	}
	public States set(char c) {
		states.clear();
		states.add(new State(c));
		return this;
	}
	public States set(Intron intron) {
		states.clear();
		states.add(new IntronState(intron));
		return this;
	}
	public State get(int p) { return states.get(p); }
	public int size() { return states.size(); }
	public StringBuffer asString() {
		StringBuffer s = new StringBuffer();
		for(State state : states) s.append(state.value);
		return s;
	}
}
class StateSequence {
	public Sequence parent;
	public States content;
	public StateSequence(Sequence parentSequence) {
		parent = parentSequence;
		content = new States();
	}
}
class InlineSequence {
	public int depth;
	public String name;
	public String id;
	public States states;
	public StringBuffer asString(int depthSize, int maxLength) {
		StringBuffer s = new StringBuffer();
		int depthLength = depth*depthSize;
		int toAdd = maxLength - depthLength - name.length();
		for(int i = 0; i < depthLength; ++i) s.append(' ');
		s.append(name);
		for(int i = 0; i < toAdd; ++i) s.append(' ');
		s.append('\t');
		s.append(states.asString());
		return s;
	}
}
enum AlignmentType { AA, NT, UNKNOWN }
class StateAlignment {
	public AlignmentType type;
	public int length;
	public Hashtable<String, StateSequence> sequences;
	public StateAlignment() {
		type = AlignmentType.UNKNOWN;
		length = 0;
		sequences = new Hashtable<String, StateSequence>();
	}
}
class Alignment {
	public AlignmentType type;
	public int length;
	public Hashtable<String, Sequence> sequences;
	public Alignment() {
		type = AlignmentType.UNKNOWN;
		length = 0;
		sequences = new Hashtable<String, Sequence>();
	}
	// Vérifie qu'au moins une feuille dans l'arbre "node" porte le nom "name".
	public boolean checkSequenceName(String name, TreeNode node) {
		if(node.isLeaf()) return node.name.equals(name);
		for(TreeNode child : node.children) if(checkSequenceName(name, child)) return true;
		return false;
	}
	// Vérifie que toutes les séquences de l'alignement sont représentées dans les feuilles de l'arbre "tree".
	public void checkSequenceNamesIn(TreeNode tree) throws Exception {
		if(tree.leavesCount() != sequences.size()) throw new Exception("Exception : LeavesCount != SequencesCount");
		for(String species : sequences.keySet()) if(!checkSequenceName(species, tree))
			throw new Exception("Exception : Species Not Found in Tree");
	}
	public void saveSequence(String header, StringBuffer content) throws Exception {
		if(header != null) {
			int headerLength = header.length();
			String tagOrganism = "/organism=";
			String tagLengths = "/intron-length=";
			String tagPositions = "{i ";
			String tagEndPositions = " i}";
			String id = "";
			String species = "";
			int[] positions = null;
			int[] lengths = null;
			// Récupérer identifiant.
			int i = 0;
			for(i = 1; i < headerLength && !State.isEmptyChar(header.charAt(i)); ++i) id += header.charAt(i);
			if(i == headerLength)
				throw new Exception("Exception : missing infos from FASTA sequence header (organismName, introns infos).");
			// Récupérer les autres informations.
			int countToRead = 3;
			while(countToRead != 0) {
				header = header.substring(i).trim();
				headerLength = header.length();
				if(header.indexOf(tagOrganism) == 0) {
					for(i = tagOrganism.length(); i < headerLength && !State.isEmptyChar(header.charAt(i)); ++i)
						species += header.charAt(i);
					--countToRead;
				} else if(header.indexOf(tagPositions) == 0) {
					int endPositions = header.indexOf(tagEndPositions);
					if(endPositions <= tagPositions.length())
						throw new Exception("Exception : missing intron positions from FASTA sequence header.");
					String string = header.substring(tagPositions.length(), endPositions).trim();
					if(!string.equals("00")) {
						String[] pieces = string.split(",");
						if(lengths != null && lengths.length != pieces.length)
							throw new Exception("Exception : intron positions and lengths counts different for a FASTA sequence header.");
						positions = new int[pieces.length];
						for(int j = 0; j < pieces.length; ++j) {
							positions[j] = Integer.parseInt(pieces[j]);
						};
					};
					--countToRead;
					i = endPositions + tagEndPositions.length();
				} else if(header.indexOf(tagLengths) == 0) {
					String string = "";
					for(i = tagLengths.length(); i < headerLength && !State.isEmptyChar(header.charAt(i)); ++i)
						string += header.charAt(i);
					string = string.trim();
					if(!string.equals("00")) {
						String[] pieces = string.split(",");
						if(positions != null && positions.length != pieces.length)
							throw new Exception("Exception : intron lengths and positions counts different for a FASTA sequence header.");
						lengths = new int[pieces.length];
						for(int j = 0; i < pieces.length; ++j) {
							lengths[j] = Integer.parseInt(pieces[j]);
						}
					};
					--countToRead;
				} else throw new Exception("Exception : missing info from FASTA sequence header.");
				if(countToRead > 0 && i == headerLength) throw new Exception("Exception : missing info from FASTA sequence header.");
			};
			if(sequences.containsKey(species)) throw new Exception("Exception : species found more than 1 time in alignment.");
			// Enregistrer la séquence.
			Sequence s = new Sequence();
			s.id = id;
			s.species = species;
			s.content = content.toString();
			if(positions != null) for(int j = 0; j < positions.length; ++j) {
				int p = positions[j];
				int l = lengths[j];
				Intron intron = new Intron();
				intron.positionCDS = p;
				intron.length = l;
				intron.phase = p % 3;
				intron.parent = s;
				s.introns.put(new Integer(p), intron);
			};
			sequences.put(species, s);
		}
	}
	public void checkType() throws Exception {
		for(Sequence sequence : sequences.values()) {
			AlignmentType seqType = AlignmentType.UNKNOWN;
			int seqLength = sequence.content.length();
			boolean isNT = true;
			for(int i = 0; isNT && i < seqLength; ++i) {
				char c = sequence.content.charAt(i);
				if(c != '-' && State.nucleotides.indexOf(c) < 0) isNT = false;
			}
			if(isNT) seqType = AlignmentType.NT;
			else {
				boolean isAA = true;
				for(int i = 0; isAA && i < seqLength; ++i) {
					char c = sequence.content.charAt(i);
					if(c != '-' && State.aminoAcids.indexOf(c) < 0) isAA = false;
				}
				if(isAA) seqType = AlignmentType.AA;
			};
			if(seqType == AlignmentType.UNKNOWN)
				throw new Exception("Exception : unknown type for sequence " + sequence.id + ".");
			if(type == AlignmentType.UNKNOWN) type = seqType;
			else if(type != seqType) throw new Exception("Inconsistent alignment type.");
		}
	}
	public void parseFile(String filename) throws Exception {
		// Lire les séquences.
		String header = null;
		StringBuffer content = null;
		String line = null;
		BufferedReader file = new BufferedReader(new FileReader(filename));
		try {
			while((line = file.readLine()) != null) if(!((line = line.trim()).isEmpty())) {
				if(line.charAt(0) == '>') {
					saveSequence(header, content);
					header = line;
					content = new StringBuffer();
				} else {
					content.append(line);
				};
			}
		} catch(Exception e) {
			throw e;
		} finally {
			file.close();
		}
		saveSequence(header, content);
		// Vérifier les données.
		// Au moins 2 séquences dans l'alignement.
		if(sequences.size() < 2) throw new Exception("Exception : at least 2 sequences is required for an alignment.");
		for(Sequence sequence : sequences.values()) {
			int sequenceLength = sequence.content.length();
			if(length == 0) length = sequenceLength;
			else if(length != sequenceLength)
				throw new Exception("Exception : all sequences must have same length in an alignment.");
		}
		// L'alignement doit être cohérent.
		checkType();
	}
	public Hashtable<Integer, ArrayList<Intron>> intronPositionsToAlignment() {
		Hashtable<Integer, ArrayList<Intron>> introns = new Hashtable<Integer, ArrayList<Intron>>();
		if(type == AlignmentType.NT) {
			for(Sequence sequence : sequences.values()) {
				int elementsCount = 0;
				for(int i = 0; i < length; ++i) if(sequence.content.charAt(i) != '-') {
					++elementsCount;
					Integer oldPosition = new Integer(elementsCount);
					if(sequence.introns.containsKey(oldPosition)) {
						Integer position = new Integer(i + 1);
						if(!introns.containsKey(position)) introns.put(position, new ArrayList<Intron>());
						introns.get(position).add(sequence.introns.get(oldPosition));
					}
				};
			};
		} else {
			for(Sequence sequence : sequences.values()) {
				int elementsCount = 0;
				for(int i = 0; i < length; ++i) if(sequence.content.charAt(i) != '-') {
					++elementsCount;
					boolean ip0 = false;	//have intron in phase 0 ?
					boolean ip1 = false;	//have intron in phase 1 ?
					boolean ip2 = false;	//have intron in phase 2 ?
					int ntCount = elementsCount*3;
					if(sequence.introns.containsKey(new Integer(ntCount))) ip0 = true;
					if(sequence.introns.containsKey(new Integer(ntCount - 1))) ip2 = true;
					if(sequence.introns.containsKey(new Integer(ntCount - 2))) ip1 = true;
					if(ip0 || ip1 || ip2) {
						Integer position = new Integer(i + 1);
						if(!introns.containsKey(position)) introns.put(position, new ArrayList<Intron>());
						if(ip0) introns.get(position).add(sequence.introns.get(new Integer(ntCount)));
						if(ip1) introns.get(position).add(sequence.introns.get(new Integer(ntCount - 2)));
						if(ip2) introns.get(position).add(sequence.introns.get(new Integer(ntCount - 1)));
					};
				};
			};
		};
		return introns;
	}
	public StateAlignment getStateAlignment() {
		StateAlignment alignment = new StateAlignment();
		alignment.type = type;
		for(String species : sequences.keySet()) alignment.sequences.put(species, new StateSequence(sequences.get(species)));
		Hashtable<Integer, ArrayList<Intron>> introns = intronPositionsToAlignment();
		if(type == AlignmentType.NT) {
			for(int i = 0; i < length; ++i) {
				Hashtable<String, States> miniSequences = new Hashtable<String, States>();
				Integer count = new Integer(i+1);
				for(String species : sequences.keySet())
					alignment.sequences.get(species).content.add(sequences.get(species).content.charAt(i));
				if(introns.containsKey(count)) {
					for(String species : sequences.keySet())
						miniSequences.get(species).set('.');
					for(Intron intron : introns.get(count))
						miniSequences.get(intron.parent.species).set(intron);
					for(String species : sequences.keySet())
						alignment.sequences.get(species).content.add(miniSequences.get(species));
				};
			};
		} else {
			for(int i = 0; i < length; ++i) {
				Hashtable<String, States> miniSequences = new Hashtable<String, States>();
				Integer count = new Integer(i+1);
				if(!introns.containsKey(count)) {
					for(String species : sequences.keySet())
						alignment.sequences.get(species).content.add(sequences.get(species).content.charAt(i));
				} else {
					ArrayList<ArrayList<Intron>> phases = new ArrayList<ArrayList<Intron>>();
					phases.add(new ArrayList<Intron>());
					phases.add(new ArrayList<Intron>());
					phases.add(new ArrayList<Intron>());
					for(Intron intron : introns.get(count)) phases.get(intron.phase).add(intron);
					if(!phases.get(1).isEmpty() && phases.get(2).isEmpty()) {
						// Introns en phase 1 mais pas en phase 2.
						for(String species : sequences.keySet()) {
							char c = State.toLowerChar(sequences.get(species).content.charAt(i));
							miniSequences.put(species, new States(c + "." + c + "" + c));
						};
						for(Intron intron : phases.get(1)) miniSequences.get(intron.parent.species).set(1, intron);
						for(String species : sequences.keySet())
							alignment.sequences.get(species).content.add(miniSequences.get(species));
					} else if(phases.get(1).isEmpty() && !phases.get(2).isEmpty()) {
						// Introns en phase 2 mais pas en phase 1.
						for(String species : sequences.keySet()) {
							char c = State.toLowerChar(sequences.get(species).content.charAt(i));
							miniSequences.put(species, new States(c + "" + c + "." + c));
						};
						for(Intron intron : phases.get(2)) miniSequences.get(intron.parent.species).set(2, intron);
						for(String species : sequences.keySet())
							alignment.sequences.get(species).content.add(miniSequences.get(species));
					} else if(!phases.get(1).isEmpty() && !phases.get(2).isEmpty()) {
						// Introns en phase 1 et en phase 2.
						for(String species : sequences.keySet()) {
							char c = State.toLowerChar(sequences.get(species).content.charAt(i));
							miniSequences.put(species, new States(c + "." + c + "." + c));
						};
						for(Intron intron : phases.get(1)) miniSequences.get(intron.parent.species).set(1, intron);
						for(Intron intron : phases.get(2)) miniSequences.get(intron.parent.species).set(3, intron);
						for(String species : sequences.keySet())
							alignment.sequences.get(species).content.add(miniSequences.get(species));
					} else {
						for(String species : sequences.keySet())
							alignment.sequences.get(species).content.add(sequences.get(species).content.charAt(i));
					};
					if(!phases.get(0).isEmpty()) {
						// Introns en phase 0.
						for(String species : sequences.keySet())
							miniSequences.put(species, new States('.'));
						for(Intron intron : phases.get(0))
							miniSequences.get(intron.parent.species).set(intron);
						for(String species : sequences.keySet())
							alignment.sequences.get(species).content.add(miniSequences.get(species));
					}
				};
			};
		};
		alignment.length = alignment.sequences.get(alignment.sequences.keys().nextElement()).content.size();
		return alignment;
	}
}
class StatesNode {
	public static StateSet getSet(TreeNode node) {
		return (StateSet)(node.tag);
	}
	public static void addState(TreeNode node, State state) {
		getSet(node).add(state);
	}
	public static boolean setIsEmpty(TreeNode node) {
		return getSet(node).isEmpty();
	}
	public static boolean hasState(TreeNode node) {
		return getSet(node).size() == 1;
	}
	public static State getState(TreeNode node) {
		return getSet(node).first();
	}
	private static void countStatesOnLeaves(TreeNode node, Hashtable<Character, Integer> counts) {
		if(node.isLeaf()) {
			Character state = new Character(StatesNode.getState(node).value);
			if(!counts.containsKey(state)) counts.put(state, new Integer(0));
			int count = counts.get(state).intValue() + 1;
			counts.put(state, new Integer(count));
		} else for(TreeNode child : node.children) {
			countStatesOnLeaves(child, counts);
		}
	}
	private static void countStateClassesOnLeaves(TreeNode node, int[] counts) {
		if(node.isLeaf()) {
			State state = StatesNode.getState(node);
			int stateClass = state.isAbsent() ? State.ABSENT : State.PRESENT;
			++counts[stateClass];
		} else for(TreeNode child : node.children) {
			countStateClassesOnLeaves(child, counts);
		}
	}
	private static void chooseMolecularState(TreeNode node, ArrayList<State> choice) throws Exception {
		char type = choice.get(0).type();
		if(!State.isMolecularType(type)) throw new Exception("A molecular type is expected in a node in a column.");
		for(int i = 1; i < choice.size(); ++i)
			if(type != choice.get(i).type()) throw new Exception("There is a node with inconsistent type in a column.");
		// On compte le nombre d'occurences de chaque état qui apparait dans les feuilles de l'arbre ayant pour racine ce noeud.
		Hashtable<Character, Integer> statesCount = new Hashtable<Character, Integer>();
		countStatesOnLeaves(node, statesCount);
		// On cherche la plus grande occurence d'un état dans l'ensemble d'états parmi lesquels choisir.
		int maxCount = 0;
		for(State state : choice) {
			int v = statesCount.get(new Character(state.value)).intValue();
			if(maxCount < v) maxCount = v;
		}
		// On collecte tous les etats ayant la plus grande occurence dans l'ensemble d'états parmi lesquels choisir.
		ArrayList<State> bests = new ArrayList<State>();
		for(State state : choice) {
			if(statesCount.get(new Character(state.value)).intValue() == maxCount) bests.add(state);
		}
		// Si on a un seul etat, on le prend, sinon on chosiit le type d'état comme état de ce noeud.
		StateSet set = getSet(node);
		node.tag = new StateSet();
		State state = null;
		if(bests.size() == 1) state = bests.get(0);
		else state = new State(type);
		state.undecidedFrom = set;
		StatesNode.addState(node, state);
	}
	public static void chooseState(TreeNode node) throws Exception {
		StateSet set = getSet(node);
		ArrayList<ArrayList<State>> classes = State.classifyStates(set);
		if(node.parent != null) {
			// Nous sommes à un noeud interne.
			if(classes.size() == 1)
				chooseMolecularState(node, classes.get(0));
			else
				chooseMolecularState(node, classes.get(State.PRESENT));
		} else {
			// Nous sommes à la racine.
			if(classes.size() == 1) {
				chooseMolecularState(node, classes.get(0));
			} else {
				if(classes.get(State.PRESENT).size() > classes.get(State.ABSENT).size()) {
					chooseMolecularState(node, classes.get(State.PRESENT));
				} else {
					int[] classesCount = new int[]{0, 0};
					countStateClassesOnLeaves(node, classesCount);
					node.tag = new StateSet();
					State state = null;
					if(classesCount[State.ABSENT] > classesCount[State.PRESENT])
						state = classes.get(State.ABSENT).get(0);
					else if(classesCount[State.ABSENT] < classesCount[State.PRESENT])
						state = classes.get(State.PRESENT).get(0);
					else
						state = classes.get(State.ABSENT).get(0);
					state.undecidedFrom = set;
					StatesNode.addState(node, state);
				}
			}
		}
	}
}
class StatesTree {
	public static void getAncestorNames(TreeNode node, Hashtable<String, States> ancestors) {
		if(!node.isLeaf()) {
			ancestors.put(node.name, new States());
			for(TreeNode child : node.children) getAncestorNames(child, ancestors);
		}
	}
	public static void tagNodes(TreeNode node, StateAlignment alignment, int position) {
		node.tag = new StateSet();
		if(node.isLeaf()) StatesNode.addState(node, alignment.sequences.get(node.name).content.get(position));
		else for(TreeNode child : node.children) tagNodes(child, alignment, position);
	}
	public static void saveAncestorStates(TreeNode node, Hashtable<String, States> ancestors) {
		if(ancestors.containsKey(node.name)) ancestors.get(node.name).add(StatesNode.getState(node));
		for(TreeNode child : node.children) saveAncestorStates(child, ancestors);
	}
	public static void tagWithSequences(TreeNode node, Hashtable<String, States> ancestors, Hashtable<String, StateSequence> sequences) {
		if(node.isLeaf()) {
			node.tag = sequences.get(node.name);
		} else {
			StateSequence sequence = new StateSequence(null);
			sequence.content = ancestors.get(node.name);
			node.tag = sequence;
		};
		for(TreeNode child : node.children) tagWithSequences(child, ancestors, sequences);
	}
	public static void linearizeNodes(TreeNode node, ArrayList<InlineSequence> list, int depth) {
		StateSequence sequence = (StateSequence)node.tag;
		InlineSequence linear = new InlineSequence();
		linear.depth = depth;
		linear.name = node.name;
		linear.id = null;
		if(sequence.parent != null) linear.id = sequence.parent.id;
		linear.states = sequence.content;
		list.add(linear);
		for(TreeNode child : node.children) linearizeNodes(child, list, depth + 1);
	}
	public static void writeFasta(TreeNode node, BufferedWriter file) throws Exception {
		String header = ">" + node.name;
		StringBuffer content = ((StateSequence)node.tag).content.asString();
		file.write(header, 0, header.length());
		file.newLine();
		file.write(content.toString(), 0, content.length());
		file.newLine();
		for(TreeNode child : node.children) writeFasta(child, file);
	}
	public static void writeParentChildrenAlignment(TreeNode node, BufferedWriter file) throws Exception {
		if(!node.isLeaf()) {
			ArrayList<InlineSequence> list = new ArrayList<InlineSequence>();
			InlineSequence root = new InlineSequence();
			root.depth = 0;
			root.name = node.name;
			root.states = ((StateSequence)node.tag).content;
			list.add(root);
			for(TreeNode child : node.children) {
				InlineSequence descendant = new InlineSequence();
				descendant.depth = 1;
				descendant.name = child.name;
				descendant.states = ((StateSequence)child.tag).content;
				list.add(descendant);
			};
			int max = 0;
			for(InlineSequence is : list) {
				int l = 2*is.depth + is.name.length();
				if(max < l) max = l;
			};
			for(InlineSequence is : list) {
				StringBuffer sb = is.asString(2, max);
				file.write(sb.toString(), 0, sb.length());
				file.newLine();
			};
			file.newLine();
			for(TreeNode child : node.children) writeParentChildrenAlignment(child, file);
		};
	}
}
class FitchAlgorithm {
	public static void deduceSets(TreeNode node) throws Exception {
		for(TreeNode child : node.children) deduceSets(child);
		if(StatesNode.setIsEmpty(node)) {
			StateSet set = StatesNode.getSet(node);
			set.addAll(StatesNode.getSet(node.children.get(0)));
			int childrenCount = node.children.size();
			for(int i = 1; i < childrenCount; ++i) set.retainAll(StatesNode.getSet(node.children.get(i)));
			if(set.isEmpty()) for(TreeNode child : node.children) set.addAll(StatesNode.getSet(child));
		};
	}
	public static void deduceStates(TreeNode node) throws Exception {
		if(!StatesNode.hasState(node)) {
			if(node.parent != null && StatesNode.getSet(node).contains(StatesNode.getState(node.parent)))
				node.tag = node.parent.tag;
			else StatesNode.chooseState(node);
		};
		for(TreeNode child : node.children) deduceStates(child);
	}
}
class EndsErrorsReparator {
	private StateAlignment alignment;
	private TreeNode tree;
	private TreeNode treeCopy;
	private Hashtable<String, Boolean> started;
	private Hashtable<String, Boolean> problems;
	private Hashtable<String, State> states;
	public EndsErrorsReparator(TreeNode tree, StateAlignment alignment) {
		this.tree = tree;
		this.alignment = alignment;
		treeCopy = tree.copyStructure();
		started = new Hashtable<String, Boolean>();
		problems = new Hashtable<String, Boolean>();
		states = new Hashtable<String, State>();
	}
	private void initDataFrom(TreeNode node) {
		started.put(node.name, new Boolean(false));
		problems.put(node.name, new Boolean(false));
		for(TreeNode child : node.children) initDataFrom(child);
	}
	private boolean atLeastOneSequenceUnstarted() {
		for(Boolean b : started.values()) if(!b.booleanValue()) return true;
		return false;
	}
	private boolean atLeastOneProblem() {
		for(Boolean b : problems.values()) if(b.booleanValue()) return true;
		return false;
	}
	private void checkColumn(TreeNode node, int position) {
		if(!started.get(node.name).booleanValue()) {
			State state = ((StateSequence)node.tag).content.get(position);
			if(state.value != '-' && state.value != '.') {
				if(state.value == '+') problems.put(node.name, true);
				else started.put(node.name, true);
			}
		}
		for(TreeNode child : node.children) checkColumn(child, position);
	}
	private void preventProblem(TreeNode node) {
		if(problems.get(node.name).booleanValue()) {
			StatesNode.addState(node, new State('.'));
			problems.put(node.name, false);
		}
		for(TreeNode child : node.children) preventProblem(child);
	}
	private void updateTree(TreeNode node, int position) {
		((StateSequence)node.tag).content.set(position, states.get(node.name));
		for(TreeNode child : node.children) updateTree(child, position);
	}
	private void getStates(TreeNode node) {
		states.put(node.name, StatesNode.getState(node));
		for(TreeNode child : node.children) getStates(child);
	}
	private void see(TreeNode node) {
		//System.err.println(node.name + " " + StatesNode.getState(node).value);
		//System.err.println(node.name + " " + node.tag);
		System.err.print(node.name);
		for(State state : StatesNode.getSet(node)) System.err.print(" " + state.value);
		System.err.println();
		for(TreeNode child : node.children) see(child);
	}
	private int repairError(int position) throws Exception {
		int hasError = 0;
		checkColumn(tree, position);
		//Si une erreur est détectée
		if(atLeastOneProblem()) {
			hasError = 1;
			//initialiser la copie de l'arbre avec les etats des feuilles.
			StatesTree.tagNodes(treeCopy, alignment, position);
			//pour chaque noeud ayant une erreur, initialiser l'état de ce noeud dans la copie de l'arbre avec l'état "absence d'intron".
			preventProblem(treeCopy);
			//exécuter l'algorithme de Fitch sur la copie de l'arbre.
			FitchAlgorithm.deduceSets(treeCopy);
			FitchAlgorithm.deduceStates(treeCopy);
			//Mettre à jour la colonne avec les nouveaux états obtenus dans la copie de l'arbre.
			getStates(treeCopy);
			updateTree(tree, position);
		}
		return hasError;
	}
	private void repairStartErrors() throws Exception {
		states.clear();
		initDataFrom(tree);
		int errors = 0;
		for(int i = 0; atLeastOneSequenceUnstarted() && i < alignment.length; ++i)
			errors += repairError(i);
		if(errors > 0) orthologFamily.base.logfile.println(orthologFamily.currentGroup + " : " + errors + " start errors.");
	}
	private void repairEndErrors() throws Exception {
		states.clear();
		initDataFrom(tree);
		int errors = 0;
		for(int i = alignment.length - 1; atLeastOneSequenceUnstarted() && i >= 0; --i)
			errors += repairError(i);
		if(errors > 0) orthologFamily.base.logfile.println(orthologFamily.currentGroup + " : " + errors + " end errors.");
	}
	public void repairErrors() throws Exception {
		repairStartErrors();
		repairEndErrors();
	}
}
public class orthologFamily {
	public static ProgramBase base = null;
	public static String currentGroup = null;
	public static final String extension = ".aligned.marked.fasta";
	public static final String ancestorBaseName = "ancestor";
	public static final TreeMap<String, Integer> intronsCounts = new TreeMap<String, Integer>();
	public static void countIntrons(TreeNode node) {
		int count = intronsCounts.containsKey(node.name) ? intronsCounts.get(node.name) : 0;
		States sequence = ((StateSequence)node.tag).content;
		int sequenceSize = sequence.size();
		for(int i = 0; i < sequenceSize; ++i) if(sequence.get(i).value == '+') ++count;
		intronsCounts.put(node.name, count);
		for(TreeNode child: node.children) countIntrons(child);
	}
	public static void rebuild(File alignmentFile, TreeNode tree) throws Exception {
		currentGroup = alignmentFile.getName();
		// Lecture de l'alignement.
		Alignment rawAlignment = new Alignment();
		rawAlignment.parseFile(alignmentFile.getAbsolutePath());
		// Vérifier les données.
		rawAlignment.checkSequenceNamesIn(tree);
		// Générer l'alignement dérivé qu'on va utiliser.
		StateAlignment alignment = rawAlignment.getStateAlignment();
		// Récupérer les noms des noeuds ancestraux.
		Hashtable<String, States> ancestors = new Hashtable<String, States>();
		StatesTree.getAncestorNames(tree, ancestors);
		// Exécuter l'algorithme de Fitch colonne par colonne sur l'alignement et en déduire les états ancestraux.
		for(int i = 0; i < alignment.length; ++i) {
			StatesTree.tagNodes(tree, alignment, i);
			FitchAlgorithm.deduceSets(tree);
			FitchAlgorithm.deduceStates(tree);
			StatesTree.saveAncestorStates(tree, ancestors);
		};
		// Générer un arbre final en associant à chaque noeud sa séquence.
		StatesTree.tagWithSequences(tree, ancestors, alignment.sequences);
		// Les étiquettes de l'arbre sont désormais des StateSequence.
		// Réparer les débuts et les fins des séquences.
		EndsErrorsReparator reparator = new EndsErrorsReparator(tree, alignment);
		reparator.repairErrors();
		// Compter les introns trouvés.
		countIntrons(tree);
		// Générer les fichiers de sortie.
		String filename = alignmentFile.getName().toLowerCase();
		String baseFilename = filename.substring(0, filename.indexOf(extension));
		ArrayList<InlineSequence> linearSequences = new ArrayList<InlineSequence>();
		StatesTree.linearizeNodes(tree, linearSequences, 0);
		// Fichier .withAncestors.seqtree
		int depthSize = 2;
		int maxLength = 0;
		for(InlineSequence is : linearSequences) {
			int length = is.depth*depthSize + is.name.length();
			if(maxLength < length) maxLength = length;
		};
		File wastFilename = new File(baseFilename + ".withAncestors.seqtree");
		BufferedWriter wastFile = new BufferedWriter(new FileWriter(wastFilename.getAbsolutePath()));
		try {
			for(InlineSequence is : linearSequences) {
				StringBuffer isString = is.asString(depthSize, maxLength);
				wastFile.write(isString.toString(), 0, isString.length());
				wastFile.newLine();
			};
		} finally {
			wastFile.close();
		};
		// Fichier .withAncestors.pca
		File wapcaFilename = new File(baseFilename + ".withAncestors.pca");
		BufferedWriter wapcaFile = new BufferedWriter(new FileWriter(wapcaFilename.getAbsolutePath()));
		try {
			StatesTree.writeParentChildrenAlignment(tree, wapcaFile);
		} finally {
			wapcaFile.close();
		};
		// Fichier .withAncestors.fasta
		File wafFilename = new File(baseFilename + ".withAncestors.fasta");
		BufferedWriter wafFile = new BufferedWriter(new FileWriter(wafFilename.getAbsolutePath()));
		try {
			StatesTree.writeFasta(tree, wafFile);
		} finally {
			wafFile.close();
		};
	}
	public static void main(String[] args) {
		if(args.length >= 2) try {
			base = new ProgramBase(args, "treeFile", "alignmentPath");
			base.logfile.println(base.toString());
			if(!base.hasParameter("treeFile")) throw new Exception("Parameter 'treeFile' is required.");
			if(!base.hasParameter("alignmentPath")) throw new Exception("Parameter 'alignmentPath' is required.");
			File treeFile = new File(base.parameter("treeFile"));
			File alignmentPath = new File(base.parameter("alignmentPath"));
			// Lecture de l'arbre.
			Newick newick = new Newick(treeFile);
			TreeNode tree = newick.parse();
			if(tree == null) throw new Exception("Unable to parse tree file.");
			// Nommage des noeuds ancestraux de l'arbre.
			if(tree.name == null) tree.name = "root";
			tree.nameNodes(ancestorBaseName, 1);
			// Reconstruction.
			if(alignmentPath.isFile()) {
				// Vérification du nom du fichier d'alignement.
				String filename = alignmentPath.getName().toLowerCase();
				if(filename.indexOf(extension) != filename.length() - extension.length())
					throw new Exception("Alignment file must have '.aligned.marked.fasta' extension.");
				rebuild(alignmentPath, tree);
			} else if(alignmentPath.isDirectory()) {
				int countFiles = 0;
				for(File alignmentFile : alignmentPath.listFiles()) {
					// Vérification du nom du fichier d'alignement.
					String filename = alignmentFile.getName().toLowerCase();
					if(filename.indexOf(extension) == filename.length() - extension.length()) {
						try {
							rebuild(alignmentFile, tree);
						} catch(Exception e) {
							e.printStackTrace();
							throw new Exception("Exception in file : " + alignmentFile.getName());
						};
						++countFiles;
						if(countFiles % 100 == 0) System.err.println(countFiles + " files read.");
					};
				};
				System.err.println(countFiles + " total files read.");
			} else throw new Exception("Exception : alignment path is not a file nor a directory.");
			if(!intronsCounts.isEmpty()) {
				BufferedWriter is = new BufferedWriter(new FileWriter("intronsCounts.tsv"));
				String output = "#species\tintronsCount";
				is.write(output, 0, output.length());
				is.newLine();
				for(String species: intronsCounts.keySet()) {
					output = species + "\t" + intronsCounts.get(species);
					is.write(output, 0, output.length());
					is.newLine();
				}
				is.close();
			}
			base.end();
		} catch(Exception e) {
			e.printStackTrace();
		};
	}
}

