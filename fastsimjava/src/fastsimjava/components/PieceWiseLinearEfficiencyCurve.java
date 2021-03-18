package fastsimjava.components;

import fastsimjava.FSJEffCurvesManager;

public class PieceWiseLinearEfficiencyCurve {
	//Name for the Curve
	public String shortName;
	//Curve Type
	public FSJEffCurvesManager.CurveType cType;
	//Curve Values
	public float[] fracOfMaxPower, effValues;

	//Constructor via contents read from a curves file
	public PieceWiseLinearEfficiencyCurve(String readShortName, FSJEffCurvesManager.CurveType curveType, String readLineFracPower, String readLineEff) {
		shortName = new String(readShortName);
		cType = curveType;

		String[] sp = readLineFracPower.split(",");
		fracOfMaxPower = new float[sp.length];
		
		for (int i=0; i<fracOfMaxPower.length; i++) {
			fracOfMaxPower[i] = Float.parseFloat(sp[i]);
		}
		
		effValues = new float[fracOfMaxPower.length];
		sp = readLineEff.split(",");
		
		for (int i=0; i<effValues.length; i++) {
			effValues[i] = Float.parseFloat(sp[i]);
		}
	}
	
	//Copy constructor
	public PieceWiseLinearEfficiencyCurve(PieceWiseLinearEfficiencyCurve other) {
		shortName = new String(other.shortName);
		cType = other.cType;
		
		fracOfMaxPower = new float[other.fracOfMaxPower.length];
		effValues = new float[fracOfMaxPower.length];
		
		for (int i=0; i<fracOfMaxPower.length; i++) {
			fracOfMaxPower[i] = other.fracOfMaxPower[i];
			effValues[i] = other.effValues[i];
		}
	}
	
	//Equivalent String
	@Override public String toString() {
		String lsep = System.getProperty("line.separator");
		String st = shortName + "," + cType.vID + lsep;
		
		st = st + fracOfMaxPower[0];
		for (int i=1; i<fracOfMaxPower.length; i++) st = st + "," + fracOfMaxPower[i];
		
		st = st + lsep + effValues[0];
		for (int i=1; i<effValues.length; i++) st = st + "," + effValues[i];
		
		return st;
	}
}
