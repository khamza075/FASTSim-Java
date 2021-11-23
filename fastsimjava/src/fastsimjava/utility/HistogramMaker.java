package fastsimjava.utility;

import java.io.FileWriter;

public class HistogramMaker {
	private float[] x, xWt;
	private float min, max;
	private boolean logMode;
	
	public HistogramMaker(int numBins, float minLimit, float maxLimit) {
		reset(numBins, minLimit, maxLimit);
	}
	public HistogramMaker(int numBins, float minLimit, float maxLimit, boolean logScaleX) {
		reset(numBins, minLimit, maxLimit, logScaleX);
	}
	
	public void reset(int numBins, float minLimit, float maxLimit) {
		reset(numBins, minLimit, maxLimit, false);
	}
	public void reset(int numBins, float minLimit, float maxLimit, boolean logScaleX) {
		min = minLimit;
		max = maxLimit;
		logMode = logScaleX;
		
		xWt = new float[numBins];
		x = new float[xWt.length+1];
		
		if (logMode) {
			float l10delta = (float)(Math.log10(max) - Math.log10(min))/(float)xWt.length;
			float l10x = (float)Math.log10(min);
			for (int i=0; i<x.length; i++) x[i] = l10x + i*l10delta;
		} else {
			float deltaBin = (max-min)/(float)numBins;
			for (int i=0; i<x.length; i++) x[i] = min + i*deltaBin;
		}
	}
	
	public Histogram getHistogramResult() {
		return new Histogram(x, xWt);
	}
	
	public void addDataPoint(float xValue) {
		addDataPoint(xValue, 1f);
	}
	public void addDataPoint(float xValue, float wt) {
		if (logMode) {
			if (xValue <= min) {
				xWt[0] += wt;
				return;
			}
			if (xValue >= max) {
				xWt[xWt.length-1] += wt;
				return;
			}
			float l10delta = (float)(Math.log10(max) - Math.log10(min))/(float)xWt.length;
			float l10x = (float)Math.log10(xValue);
			int binID = (int)((l10x - Math.log10(min))/l10delta);
			xWt[binID] += wt;
		} else {
			int numBins = xWt.length;
			int binID = Math.min(Math.max(0, (int)(numBins*(xValue-min)/(max-min))), numBins-1);
			xWt[binID] += wt;
		}
	}
	
	public void saveResultToFile(String fname) {
		saveResultToFile(fname, null);
	}
	public void saveResultToFile(String fname, AverageCalc averages) {
		Histogram hst = getHistogramResult();
		if (averages != null) hst.averagesCalcLine = averages.toString();
		try {
			FileWriter fout = new FileWriter(fname);

			fout.append(hst.toString());
			
			fout.flush();
			fout.close();
		} catch (Exception e) {}
	}
	

	public static class Histogram {
		private float[] xs, pdm, cdf;
		private float x05,x25,x50,x75,x95;
		
		private String averagesCalcLine;
		
		public float[] xs() {return xs;}
		public float[] pdm() {return pdm;}
		public float[] cdf() {return cdf;}
		
		public float x05() {return x05;}
		public float x25() {return x25;}
		public float x50() {return x50;}
		public float x75() {return x75;}
		public float x95() {return x95;}
		
		@Override public String toString() {
			String lsep = System.getProperty("line.separator");
			String st = ",Value"+lsep;
			st = st + "x05,"+x05+lsep;
			st = st + "x25,"+x25+lsep;
			st = st + "x50,"+x50+lsep;
			st = st + "x75,"+x75+lsep;
			st = st + "x95,"+x95+lsep;
			st = st + "lowErrBar,"+(x25-x05)+lsep;
			st = st + "s1_hide,"+(x25)+lsep;
			st = st + "s2_show,"+(x50-x25)+lsep;
			st = st + "s3_show,"+(x75-x50)+lsep;
			st = st + "highErrBar,"+(x95-x75)+lsep+lsep;
			
			if (averagesCalcLine != null) {
				st = st + averagesCalcLine +lsep+lsep;
			}

			st = st + "x,PDM,CDF"+lsep;
			for (int i=0; i<xs.length; i++) {
				st = st + "" + xs[i] + "," + pdm[i] + "," + cdf[i] + lsep;
			}

			return st;
		}
		
		private Histogram(float[] x, float[] binWts) {	//Note x must have .length of binWts.length + 1
			averagesCalcLine = null;
			
			xs = new float[x.length*2];
			pdm = new float[xs.length];
			cdf = new float[xs.length];
			
			for (int i=0; i<x.length; i++) {
				xs[i*2] = x[i];
				xs[i*2+1] = x[i];
			}
			
			float sumWt = 0f;
			for (int i=0; i<binWts.length; i++) sumWt += binWts[i];
			
			if (sumWt <= 0) {
				float x0 = x[0];
				float dX = x[x.length-1]-x0;
				
				x05 = x0 + 0.05f*dX;
				x25 = x0 + 0.25f*dX;
				x50 = x0 + 0.5f*dX;
				x75 = x0 + 0.75f*dX;
				x95 = x0 + 0.95f*dX;
				return;
			}
			
			float lastCDF = 0f;
			boolean notDoneX05 = true;
			boolean notDoneX25 = true;
			boolean notDoneX50 = true;
			boolean notDoneX75 = true;
			boolean notDoneX95 = true;
			float nextBXPCDFValue = 0.05f;
			
			for (int i=0; i<binWts.length; i++) {
				float curPDM = binWts[i]/sumWt;
				float curCDF = lastCDF + curPDM;
				
				cdf[2*i+2] = curCDF;
				cdf[2*i+3] = curCDF;
				
				pdm[2*i+1] = curPDM;
				pdm[2*i+2] = curPDM;
				
				if ((curCDF >= nextBXPCDFValue)&&notDoneX05) {
					float x1 = x[i];
					float x2 = x[i+1];
					float xc = x1 + (x2-x1)*(nextBXPCDFValue-lastCDF)/(curCDF-lastCDF);
					
					x05 = xc;
					notDoneX05 = false;
					nextBXPCDFValue = 0.25f;
				}				
				if ((curCDF >= nextBXPCDFValue)&&notDoneX25) {
					float x1 = x[i];
					float x2 = x[i+1];
					float xc = x1 + (x2-x1)*(nextBXPCDFValue-lastCDF)/(curCDF-lastCDF);
					
					x25 = xc;
					notDoneX25 = false;
					nextBXPCDFValue = 0.5f;
				}	
				if ((curCDF >= nextBXPCDFValue)&&notDoneX50) {
					float x1 = x[i];
					float x2 = x[i+1];
					float xc = x1 + (x2-x1)*(nextBXPCDFValue-lastCDF)/(curCDF-lastCDF);
					
					x50 = xc;
					notDoneX50 = false;
					nextBXPCDFValue = 0.75f;
				}				
				if ((curCDF >= nextBXPCDFValue)&&notDoneX75) {
					float x1 = x[i];
					float x2 = x[i+1];
					float xc = x1 + (x2-x1)*(nextBXPCDFValue-lastCDF)/(curCDF-lastCDF);
					
					x75 = xc;
					notDoneX75 = false;
					nextBXPCDFValue = 0.95f;
				}				
				if ((curCDF >= nextBXPCDFValue)&&notDoneX95) {
					float x1 = x[i];
					float x2 = x[i+1];
					float xc = x1 + (x2-x1)*(nextBXPCDFValue-lastCDF)/(curCDF-lastCDF);
					
					x95 = xc;
					notDoneX95 = false;
				}				
				
				lastCDF = curCDF;
			}			
		}
	}
}
