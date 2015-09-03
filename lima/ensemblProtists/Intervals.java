/*
Steven Bocco, 20 Février 2013.
Classe Intervals, utilisée pour gérer une suite d'exons ou de CDS.
*/

package lima.ensemblProtists;
import java.util.Collections;
import java.util.ArrayList;

public class Intervals {
	// Attributs.
	protected ArrayList<Interval> intervalles;
	// Méthodes.
	public Intervals() {
		intervalles = new ArrayList<Interval>(10);
	};
	public int size() {
		return intervalles.size();
	};
	public Interval get(int i) {
		return intervalles.get(i);
	};
	public void add(Interval intervalle) {
		intervalles.add(intervalle);
	};
	public void sort() {
		Collections.sort(intervalles);
	};
	public void setPositionsFromOne() {
		int compte = intervalles.size();
		if(compte > 0) {
			long a = intervalles.get(0).start;
			for(int i = 0; i < compte; ++i) {
				Interval intervalle = intervalles.get(i);
				intervalle.start -= a - 1;
				intervalle.end -= a - 1;
			};
		};
	};
	public void reverse() {
		int compte = intervalles.size();
		if(compte > 0) {
			long d = intervalles.get(compte - 1).end;
			for(int i = 0; i < compte; ++i) {
				Interval intervalle = intervalles.get(i);
				intervalle.start = d + 1 - intervalle.start;
				intervalle.end = d + 1 - intervalle.end;
			};
			ArrayList<Interval> nouvelleListe = new ArrayList<Interval>(compte);
			for(int i = compte - 1; i >= 0; --i) {
				Interval intervalle = intervalles.get(i);
				long temp = intervalle.start;
				intervalle.start = intervalle.end;
				intervalle.end = temp;
				nouvelleListe.add(intervalle);
			};
			intervalles = nouvelleListe;
		};
	};
};

