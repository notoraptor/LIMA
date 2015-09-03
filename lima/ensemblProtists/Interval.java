/*
Steven Bocco, 20 Février 2013.
Classe Interval représentant un intervalle d'entiers.
*/

package lima.ensemblProtists;

public class Interval implements Comparable<Interval> {
	// Attributs.
	public long start;
	public long end;
	// Méthodes.
	public Interval(long a, long b) {
		start = a;
		end = b;
	};
	public Interval(long valeur) {
		start = end = valeur;
	};
	public int compareTo(Interval autre) {
		long test = start - autre.start;
		if(test == 0) test = end - autre.end;
		if(test > 0) return 1;
		else if(test < 0) return -1;
		return 0;
	};
	public long count() {
		return end - start + 1;
	};
};

