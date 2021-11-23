package fastsimjava.utility;

public class AverageCalc {
	private float allAverage, allWt, nonOutlierAverage, nonOutlierWt, xMin, xMax;
	
	public float allAverage() {return allAverage;}
	public float nonOutlierAverage() {return nonOutlierAverage;}

	public AverageCalc() {
		reset();
	}
	public AverageCalc(float minNonOutlierValue, float maxNonOutlierValue) {
		reset(minNonOutlierValue, maxNonOutlierValue);
	}
	
	public void reset() {
		reset(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
	}
	public void reset(float minNonOutlierValue, float maxNonOutlierValue) {
		xMin = minNonOutlierValue;
		xMax = maxNonOutlierValue;
		
		allAverage = 0f;
		allWt = 0f;
		nonOutlierAverage = 0f;
		nonOutlierWt = 0f;
	}
	
	public void addPoint(float x) {
		addPoint(x, 1f);
	}
	public void addPoint(float x, float wt) {
		allAverage = (allAverage*allWt + x*wt)/(allWt+wt);
		allWt += wt;
		
		if (x < xMin) return;
		if (x > xMax) return;
		
		nonOutlierAverage = (nonOutlierAverage*nonOutlierWt + x*wt)/(nonOutlierWt+wt);
		nonOutlierWt += wt;
	}
	
	@Override public String toString() {
		String lsep = System.getProperty("line.separator");
		return "allAverage,"+allAverage+lsep+"nonOutlierAverage,"+nonOutlierAverage;
	}
}
