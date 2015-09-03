package lima.ensemblProtists.gtf;
public class Sequence {
	public String feature;
	public long start;
	public long end;
	public char strand;
	public long minValue() {
		return start < end ? start : end;
	};
	public long maxValue() {
		return start > end ? start : end;
	};
};