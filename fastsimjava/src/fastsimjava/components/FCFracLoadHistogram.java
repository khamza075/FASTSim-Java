package fastsimjava.components;

public class FCFracLoadHistogram {
	//Global constant for number of bins
	public static int nBins = 20;
	
	//Data
	private float maxKW, totalAnalyzedTime, operatingTime, deltaKW;
	private float[] timeOperatedAtFracMaxLoad;

	//Constructor
	public FCFracLoadHistogram(float fcMaxKWOut) {
		maxKW = fcMaxKWOut;
		reset();
	}
	//Copy Constructor
	public FCFracLoadHistogram(FCFracLoadHistogram other) {
		maxKW = other.maxKW;
		totalAnalyzedTime = other.totalAnalyzedTime;
		operatingTime = other.operatingTime;
		deltaKW = other.deltaKW;
		
		timeOperatedAtFracMaxLoad = new float[other.timeOperatedAtFracMaxLoad.length];
		for (int i=0; i<timeOperatedAtFracMaxLoad.length; i++) timeOperatedAtFracMaxLoad[i] = other.timeOperatedAtFracMaxLoad[i];
	}
	
	//Reset record function
	public void reset() {
		totalAnalyzedTime = 0f;
		operatingTime = 0f;
		deltaKW = maxKW/(float)nBins;
		
		timeOperatedAtFracMaxLoad = new float[nBins];
	}
	
	//Function to add time while operating
	public void addTimeOperating(float deltaSec, float kW) {
		totalAnalyzedTime += deltaSec;
		operatingTime += deltaSec;
		
		int binID = 0;
		if (deltaKW > 0) {
			binID = Math.max(0, Math.min((int)(kW/deltaKW), nBins-1));
		}
		timeOperatedAtFracMaxLoad[binID] += deltaSec;
	}
	//Function to add time while not operating
	public void addTimeNotOperating(float deltaSec) {
		totalAnalyzedTime += deltaSec;
	}
	
	@Override public String toString() {
		String lsep = System.getProperty("line.separator");
		
		float fracOperatingTime = 0f;
		if (totalAnalyzedTime > 0) fracOperatingTime = operatingTime/totalAnalyzedTime;
		
		String st = "totalAnalyzedTime,"+totalAnalyzedTime+lsep;
		st = st + "fracOperatingTime,"+fracOperatingTime;
		
		if (maxKW > 0) {
			st = st + lsep + "fracPowerBinsUpTo";
			for (int i=0; i<nBins; i++) st = st + "," + (((float)(i+1))/(float)nBins);
			
			st = st + lsep + "fracOfOperatingTime";
			if (operatingTime > 0) {
				for (int i=0; i<nBins; i++) st = st + "," + (timeOperatedAtFracMaxLoad[i]/operatingTime);
			} else {
				for (int i=0; i<nBins; i++) st = st + ",0.0";
			}
		}
		
		return st;
	}
}
