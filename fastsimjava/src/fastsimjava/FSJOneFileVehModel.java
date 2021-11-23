package fastsimjava;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class FSJOneFileVehModel {
	//Data
	public FSJVehModelParam vehModelParam;
	public FSJEffCurvesManager curveMan;
	public float addMassKg,addAuxKW,adjDEMult;

	//Constructor via Reading File
	public FSJOneFileVehModel(String fname) {
		try {
			BufferedReader fin = new BufferedReader(new FileReader(fname));
			
			String readLine = fin.readLine();
			
			if (readLine.split(",").length > 10) {
				//Horizontal mode input
				readLine = fin.readLine();			
				vehModelParam = new FSJVehModelParam(readLine);
			} else {
				//Vertical Mode input
				int numLines = FSJVehModelParam.HeaderString.split(",").length;
				String reconstructedReadLine = new String(readLine.split(",")[1]);
				
				for (int i=1; i<numLines; i++) {
					readLine = fin.readLine();
					reconstructedReadLine = reconstructedReadLine + "," + readLine.split(",")[1];
				}
				vehModelParam = new FSJVehModelParam(reconstructedReadLine);
			}
			
			readLine = fin.readLine();			
			readLine = fin.readLine();
			String[] sp = readLine.split(",");
			addMassKg = Float.parseFloat(sp[0]);
			addAuxKW = Float.parseFloat(sp[1]);
			adjDEMult = Float.parseFloat(sp[2]);

			curveMan = new FSJEffCurvesManager(fin);

			fin.close();
		} catch (Exception e) {
			vehModelParam = null;
			curveMan = null;
			addMassKg = 0f;
			addAuxKW = 0f;
			adjDEMult = 1f;
		}
	}
	
	//Constructors via Data
	public FSJOneFileVehModel(FSJVehModelParam vehModel, FSJEffCurvesManager cMan) {
		initViaData(vehModel, cMan, 0f, 0f, 1f);
	}
	public FSJOneFileVehModel(FSJVehModelParam vehModel, FSJEffCurvesManager cMan, float aKg, float aKW, float adjDE) {
		initViaData(vehModel, cMan, aKg, aKW, adjDE);
	}	
	private void initViaData(FSJVehModelParam vehModel, FSJEffCurvesManager cMan, float aKg, float aKW, float adjDE) {	
		//Copy
		vehModelParam = new FSJVehModelParam(vehModel);
		addMassKg = aKg;
		addAuxKW = aKW;
		adjDEMult = adjDE;
		
		//Re-encode the special flag
		int oldFCCurveID = vehModel.battery.fcCCurveID;
		int oldMtCurveID = vehModel.battery.mtCCurveID;
		
		curveMan = new FSJEffCurvesManager(cMan, oldFCCurveID, oldMtCurveID);
		
		int reFCCurveID = Math.min(oldFCCurveID, 0);
		int reMtCurveID = Math.min(oldMtCurveID, 0);
		
		int fcSpFlag = 0;
		if (reFCCurveID >= 0) fcSpFlag = -(reFCCurveID+1);
		
		int mtSpFlag = 0;
		if (reMtCurveID >= 0) mtSpFlag = -1000*(reMtCurveID+1);
		
		int spFlag = fcSpFlag + mtSpFlag;
		
		vehModelParam.battery.fcCCurveID = reFCCurveID;
		vehModelParam.battery.mtCCurveID = reMtCurveID;
		vehModelParam.battery.reEncodeSpecialFlag(spFlag);
	}
	
	//Function for saving to file
	public void saveToFile(String fname) {
		try {
			FileWriter fout = new FileWriter(fname);
			String lsep = System.getProperty("line.separator");
			
			fout.append(FSJVehModelParam.HeaderString+lsep);
			fout.append(vehModelParam.toString()+lsep);
			
			fout.append("addMassKg,addAuxKW,adjDEMult"+lsep);
			fout.append(""+addMassKg+","+addAuxKW+","+adjDEMult+lsep);
			
			curveMan.writeToFileStream(fout);
			
			fout.flush();
			fout.close();
		} catch (Exception e) {}
	}
	public void saveToFile(String fname, boolean verticalMode) {
		if (!verticalMode) {
			saveToFile(fname);
			return;
		}
		
		try {
			FileWriter fout = new FileWriter(fname);
			String lsep = System.getProperty("line.separator");
			
			String[] vehParamLines = vehModelParam.toString_multiLine();
			for (int i=0; i<vehParamLines.length; i++) fout.append(vehParamLines[i]+lsep);
			
			fout.append("addMassKg,addAuxKW,adjDEMult"+lsep);
			fout.append(""+addMassKg+","+addAuxKW+","+adjDEMult+lsep);
			
			curveMan.writeToFileStream(fout);
			
			fout.flush();
			fout.close();
		} catch (Exception e) {}
	}
}
