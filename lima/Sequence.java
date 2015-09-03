/*
Classe représentant une séquence au format FASTA.
*/
package lima;
class Sequence {
	public String header;
	public String content;
	public Sequence(String header, StringBuffer content) {
		this.header = header;
		this.content = content.toString();
	}
}