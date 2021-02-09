package fastsimjava;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import fastsimjava.components.PieceWiseLinearEfficiencyCurve;

public class FSJEffCurvesManager {
	
	//Storage of curves
	private PieceWiseLinearEfficiencyCurve[] fcCCurves;
	private PieceWiseLinearEfficiencyCurve[] mtCCurves;
	
	//Function to return list of curves for fuel converter
	public PieceWiseLinearEfficiencyCurve getFCCurve(int cID) {
		if (fcCCurves == null) return null;
		if (cID < 0) return null;
		if (cID >= fcCCurves.length) return null;
		return fcCCurves[cID];
	}
	//Function to return list of curves for motor
	public PieceWiseLinearEfficiencyCurve getMtCurve(int cID) {
		if (mtCCurves == null) return null;
		if (cID < 0) return null;
		if (cID >= mtCCurves.length) return null;
		return mtCCurves[cID];
	}
	

	//Constructor for List of curves reading mode
	public FSJEffCurvesManager(String fname) {
		try {
			BufferedReader fin = new BufferedReader(new FileReader(fname));		
			readFromFileStream(fin);
			fin.close();
		} catch (Exception e) {}
	}
	//Constructor from last-section of file reading mode (to be coupled with "One-File" Vehicle model)
	public FSJEffCurvesManager(BufferedReader fin) throws Exception {
		readFromFileStream(fin);
	}
	//Constructor via copying (up to) one motor and one fuel converter curve
	public FSJEffCurvesManager(FSJEffCurvesManager otherCM, int fcCurveID, int mtCurveID) {
		if (fcCurveID < 0) {
			fcCCurves = new PieceWiseLinearEfficiencyCurve[0];
		} else {
			fcCCurves = new PieceWiseLinearEfficiencyCurve[1];
			fcCCurves[0] = new PieceWiseLinearEfficiencyCurve(otherCM.fcCCurves[fcCurveID]);
		}
		
		if (mtCurveID < 0) {
			mtCCurves = new PieceWiseLinearEfficiencyCurve[0];
		} else {
			mtCCurves = new PieceWiseLinearEfficiencyCurve[1];
			mtCCurves[0] = new PieceWiseLinearEfficiencyCurve(otherCM.mtCCurves[mtCurveID]);
		}
	}
	
	//Function for reading from file stream
	private void readFromFileStream(BufferedReader fin) throws Exception {
		ArrayList<PieceWiseLinearEfficiencyCurve> lstFC = new ArrayList<PieceWiseLinearEfficiencyCurve>();
		ArrayList<PieceWiseLinearEfficiencyCurve> lstMt = new ArrayList<PieceWiseLinearEfficiencyCurve>();
		
		String readLine;
		while ((readLine = fin.readLine())!=null) {
			String[] hLineSplit = readLine.split(",");
			if (hLineSplit.length < 2) break;
			String fracPowerLine = fin.readLine();
			String effLine = fin.readLine();
			
			CurveType cType = CurveType.parseString(hLineSplit[1]);
			switch (cType) {
			case fuelConverter:
				lstFC.add(new PieceWiseLinearEfficiencyCurve(hLineSplit[0], cType, fracPowerLine, effLine));
				break;
			case motor:
				lstMt.add(new PieceWiseLinearEfficiencyCurve(hLineSplit[0], cType, fracPowerLine, effLine));
				break;
			}
		}
		
		fcCCurves = new PieceWiseLinearEfficiencyCurve[lstFC.size()];
		for (int i=0; i<fcCCurves.length; i++) fcCCurves[i] = lstFC.get(i);
		
		mtCCurves = new PieceWiseLinearEfficiencyCurve[lstMt.size()];
		for (int i=0; i<mtCCurves.length; i++) mtCCurves[i] = lstMt.get(i);
	}
	
	//Function to write into file stream
	public void writeToFileStream(FileWriter fout) throws Exception {
		String lsep = System.getProperty("line.separator");
		for (int i=0; i<fcCCurves.length; i++) fout.append(fcCCurves[i].toString()+lsep);
		for (int i=0; i<mtCCurves.length; i++) fout.append(mtCCurves[i].toString()+lsep);
	}
	
	//Enumeration for Curve Type
	public enum CurveType {
		fuelConverter(0), 
		motor(1)
		;
		
		public int vID;
		private CurveType(int value) {
			vID = value;
		}
		
		public static CurveType parseString(String st) {
			int value = Integer.parseInt(st);
			
			for (int i=0; i<values().length; i++) {
				if (values()[i].vID == value) return values()[i];
			}
			
			return null;
		}
	}
}
