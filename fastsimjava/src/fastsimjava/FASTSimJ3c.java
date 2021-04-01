package fastsimjava;

import java.io.FileWriter;
import java.util.ArrayList;

import fastsimjava.abs.*;
import fastsimjava.components.*;

public class FASTSimJ3c {
	//Header String for Compact Summary
	public static final String Header_CTripSummay = "miles,fuelUse,batteryUse,maxSpeedSlipMPH,finalRelSoC,seconds,secondsIdling,secondsFuelConvOn,nFuelConvStarts";
	
	
	//Simulation constants
	public FSJSimConstants simConsts;
	
	//Current Vehicle State
	private FSJVehState vehState;
	//Vehicle state at all time instants during last simulated trip
	private FSJVehState[] lastTripVehStates;
	public FSJVehState[] lastTripVehStates() {return lastTripVehStates;}
	
	//Function to return a link to current vehicle model parameters
	public FSJVehModelParam getCurVehModel() {
		if (vehState==null) return null;
		return vehState.vehModel();
	}
	
	//Compact summary of last trip
	private TripCSummary lastTripSummary;
	public TripCSummary lastTripSummary() {return lastTripSummary;}

	//Constructor
	public FASTSimJ3c() {
		simConsts = new FSJSimConstants();
		vehState = null;
		resetLastTripInfo();
	}
	private void resetLastTripInfo() {
		lastTripVehStates = null;
		lastTripSummary = null;
	}

	//Function to set the vehicle model (do this before simulation, model remains until set to something else)
	// Note: BEVs and PHEVs are initialized to fully charge (relSoC = 1) by default, use SoC modification functions
	//       if a different initialization is desired
	public void setVehModel(FSJVehModelParam vehModel, FSJEffCurvesManager curveManager) {
		vehState = new FSJVehState(simConsts, vehModel, curveManager);
		resetLastTripInfo();
	}
	//With built-in support for 3-Parameter tuning
	public void setVehModel(FSJVehModelParam vehModel, FSJEffCurvesManager curveManager, float addMass, float addAux, float adjDEMult) {
		vehState = new FSJVehState(simConsts, vehModel, curveManager, addMass, addAux, adjDEMult);
		resetLastTripInfo();
	}	
	
	//Function to return the tire slip condition maximum acceleration
	public float tireSlipMaxAccelMS2() {
		if (vehState==null) return 0f;
		return vehState.motion.maxAccelMS2;
	}
	//Function to return the tire slip condition maximum deceleration
	public float tireSlipMaxDecelMS2() {
		if (vehState==null) return 0f;
		return vehState.motion.maxDecelMS2;
	}
	//Function for adjusting relative state of charge (only affects BEVs & PHEVs)
	public void setRelSoC(float relSoC) {
		//Exit if no vehicle model exists
		if (vehState==null) return;
		
		//Set relative and absolute state of charge
		float rSoC = Math.min(1f, Math.max(0f, relSoC));
		FSJVehModelParam.ChargeControlParam chPar = vehState.vehModel().chargeControl;
		vehState.soc.relSoC = rSoC;
		vehState.soc.absSoC = chPar.minSoCBatterySwing + (chPar.maxSoCBatterySwing - chPar.minSoCBatterySwing)*rSoC;
		
		resetLastTripInfo();
	}
	
	//Function for Compact run
	public void runC(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade) {
		runC(timeSec, speedDesiredMPH, roadGrade, null, null);
	}
	// ... with optional second-by-second additional auxiliary load and optionally variable pay load
	public void runC(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg) {
		FSJHybridPowerManagerDefault pwrMgr = new FSJHybridPowerManagerDefault();
		runC(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, pwrMgr);
	}
	// ...most general version
	public void runC(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg, FSJHybridPowerManagerBase hybPwrMgr) {
		//Inputs that may be null (and treatment if they are null) are:
		//	timeSec --> speedDesiredMPH is assumed to be at 1sec intervals
		//	roadGrade --> assumed to be zero
		//	otherAuxKW --> assumed to be zero
		//	payloadKg --> assumed to be zero
		//Note: hybPwrMgr CANNOT be null when invoking this version of run() function

		//Exit if no vehicle model exists
		if (vehState==null) return;
		
		//Kill previously saved results and only start compact record
		resetLastTripInfo();
		lastTripSummary = new TripCSummary();
		
		//Do single run (without change to SoC) if not HEV, otherwise iterate to balance for ~zero battery use
		if (vehState.vehModel().general.vehPtType == FSJVehModelParam.VehicleDriveTrainType.hev) {
			//HEV
			runFitHEV(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, hybPwrMgr);
		} else {
			//Not HEV
			runCHEV(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, hybPwrMgr, -1);
		}		
	}
	
	//Function for running simulation of a trip w/ full Time-Record (i.e. retaining all the vehicle "states" at every time step)
	public void runTR(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade) {
		runTR(timeSec, speedDesiredMPH, roadGrade, null, null);
	}
	// ... with optional second-by-second additional auxiliary load and optionally variable pay load
	public void runTR(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg) {
		FSJHybridPowerManagerDefault pwrMgr = new FSJHybridPowerManagerDefault();
		runTR(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, pwrMgr, -1);
	}
	// ...most general version
	public void runTR(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg, 
			FSJHybridPowerManagerBase hybPwrMgr, float hevInitialRelSoC) {
		//Inputs that may be null (and treatment if they are null) are:
		//	timeSec --> speedDesiredMPH is assumed to be at 1sec intervals
		//	roadGrade --> assumed to be zero
		//	otherAuxKW --> assumed to be zero
		//	payloadKg --> assumed to be zero
		//Note #1: hybPwrMgr CANNOT be null when invoking this version of run() function
		//Note #2: hevInitialSoC has no effect unless the vehicle is HEV and the value is >= 0 
		//	-- otherwise the simulation continues with "last" (after previous trip), or the default initialization of SoC in FSJVehState class

		//Exit if no vehicle model exists
		if (vehState==null) return;
		
		//Kill previously saved results and start both a compact record, plus a list of the vehicle state at every time instant
		resetLastTripInfo();
		lastTripSummary = new TripCSummary();
		lastTripSummary.reset();
		
		vehState.resetAllExceptSOC();
		if ((vehState.vehModel().general.vehPtType == FSJVehModelParam.VehicleDriveTrainType.hev)&&(hevInitialRelSoC >= 0)) {
			setRelSoC(hevInitialRelSoC);
		}
		
		ArrayList<FSJVehState> lstTR = new ArrayList<FSJVehState>();
		lstTR.add(new FSJVehState(vehState));
		
		//Main run
		float secSinceStart = 0f;
		float zSpeedTolMZ = 0.001f;
		
		for (int i=1; i<speedDesiredMPH.length; i++) {			
			float deltaTime = 1f;
			if (timeSec!=null) deltaTime = timeSec[i]-timeSec[i-1];
			secSinceStart += deltaTime;
			
			float desiredMPH = speedDesiredMPH[i];
			
			float grade = 0f;
			if (roadGrade!=null) grade = roadGrade[i];
			
			boolean fuelConvWasOn = vehState.isFuelConvOn();
			float oAuxKW = 0f;
			if (otherAuxKW != null) oAuxKW = otherAuxKW[i];
			if (payloadKg == null) vehState.updateState(secSinceStart, desiredMPH, grade, oAuxKW, hybPwrMgr, 0f);
			else vehState.updateState(secSinceStart, desiredMPH, grade, oAuxKW, hybPwrMgr, payloadKg[i]);
			
			lstTR.add(new FSJVehState(vehState));
			
			if (lastTripSummary.maxSpeedSlipMPH < vehState.motion.curSpeedSlipMPH) lastTripSummary.maxSpeedSlipMPH = vehState.motion.curSpeedSlipMPH;
			if (vehState.isFuelConvOn()) {
				lastTripSummary.secondsFuelConvOn += deltaTime;
				if (!fuelConvWasOn) lastTripSummary.nFuelConvStarts += 1;
			}
			if (desiredMPH < zSpeedTolMZ) lastTripSummary.secondsIdling += deltaTime;
		}	

		//Extract information from final state into the compact summary
		lastTripSummary.miles = vehState.motion.milesSinceStart;
		lastTripSummary.fuelUse = vehState.energyUse.fuelUseSinceTripStart;
		lastTripSummary.fcLoadHistogram = vehState.energyUse.fcLoadHist;
		lastTripSummary.batteryUse = vehState.energyUse.batteryKWhSinceTripStart;
		lastTripSummary.finalRelSoC = vehState.soc.relSoC;
		lastTripSummary.seconds = vehState.time.secSinceTripStart;
		
		//Copy the list into an array
		lastTripVehStates = new FSJVehState[lstTR.size()];
		for (int i=0; i<lastTripVehStates.length; i++) lastTripVehStates[i] = lstTR.get(i);
	}
	
	//Internal calculation function -- Line-Fitting to estimate equivalent HEV fuel economy -- multiple attempts with different number of points
	private void runFitHEV(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg, FSJHybridPowerManagerBase hybPwrMgr) {
		
		int startNumFitPoints = 5;
		int maxNumFitPoints = 20;
		boolean runFitPointsSuccessful = false;
		
		for (int i=startNumFitPoints; i<=maxNumFitPoints; i++) {
			try {
				runFitHEVgivenNumPoints(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, hybPwrMgr, i);
				runFitPointsSuccessful = true;
				break;
			} catch (Exception e) {
				runFitPointsSuccessful = false;
			}
		}
		
		if (!runFitPointsSuccessful) runCHEV(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, hybPwrMgr, 0.5f);
	}

	//Internal calculation function -- Line-Fitting to estimate equivalent HEV fuel economy -- given number of line fit points
	private void runFitHEVgivenNumPoints(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg, FSJHybridPowerManagerBase hybPwrMgr, int numFitPoints) throws Exception {
		
		float zTolerance = (float)1.0e-6;
		float[] xTrys = new float[numFitPoints];
		float[] yTrys = new float[numFitPoints];
		
		float deltaRelSOC = 1f/(float)numFitPoints;
		float curRelSoc = 0.5f*deltaRelSOC;
		
		for (int i=0; i<xTrys.length; i++) {
			runCHEV(timeSec, speedDesiredMPH, roadGrade, otherAuxKW, payloadKg, hybPwrMgr, curRelSoc);
			
			yTrys[i] = lastTripSummary.fuelUse;
			xTrys[i] = lastTripSummary.batteryUse;
			
			curRelSoc += deltaRelSOC;
		}
		
		float sumX2 = 0f;
		float sumX = 0f;
		float sumY = 0f;
		float sumXY = 0f;

		for (int i=0; i<xTrys.length; i++) {
			float x = xTrys[i];
			float y = yTrys[i];
			
			sumX2 += x*x;
			sumX += x;
			sumY += y;
			sumXY += x*y;
		}		
		
		float delta = numFitPoints*sumX2 - sumX*sumX;
		float delta0 = sumY*sumX2 - sumX*sumXY;
		
		if (delta < zTolerance) throw new Exception();
		
		lastTripSummary.fuelUse = delta0/delta;
		if (lastTripSummary.fuelUse < zTolerance) throw new Exception();
		lastTripSummary.batteryUse = 0f;
	}
	
	//Internal calculation function -- Main "work-horse" for compact simulation (without saving all the intermediate states during the trip)
	private void runCHEV(float[] timeSec, float[] speedDesiredMPH, float[] roadGrade, float[] otherAuxKW, float[] payloadKg, FSJHybridPowerManagerBase hybPwrMgr, float hevRelSOCTry) {
		
		//Reset all but the state of charge
		vehState.resetAllExceptSOC();
		lastTripSummary.reset();
		
		//If a value > 0 is supplied for relative SOC (only for HEVs), set it
		if (hevRelSOCTry >= 0f) setRelSoC(hevRelSOCTry);
		
		//Main run
		float secSinceStart = 0f;
		float zSpeedTolMZ = 0.001f;
		
		for (int i=1; i<speedDesiredMPH.length; i++) {			
			float deltaTime = 1f;
			if (timeSec!=null) deltaTime = timeSec[i]-timeSec[i-1];
			secSinceStart += deltaTime;
			
			float desiredMPH = speedDesiredMPH[i];
			
			float grade = 0f;
			if (roadGrade!=null) grade = roadGrade[i];
			
			boolean fuelConvWasOn = vehState.isFuelConvOn();
			float oAuxKW = 0f;
			if (otherAuxKW != null) oAuxKW = otherAuxKW[i];
			if (payloadKg == null) vehState.updateState(secSinceStart, desiredMPH, grade, oAuxKW, hybPwrMgr, 0f);
			else vehState.updateState(secSinceStart, desiredMPH, grade, oAuxKW, hybPwrMgr, payloadKg[i]);
			
			if (lastTripSummary.maxSpeedSlipMPH < vehState.motion.curSpeedSlipMPH) lastTripSummary.maxSpeedSlipMPH = vehState.motion.curSpeedSlipMPH;
			if (vehState.isFuelConvOn()) {
				lastTripSummary.secondsFuelConvOn += deltaTime;
				if (!fuelConvWasOn) lastTripSummary.nFuelConvStarts += 1;
			}
			if (desiredMPH < zSpeedTolMZ) lastTripSummary.secondsIdling += deltaTime;
		}	
		
		//Extract information from final state into the compact summary
		lastTripSummary.miles = vehState.motion.milesSinceStart;
		lastTripSummary.fuelUse = vehState.energyUse.fuelUseSinceTripStart;
		lastTripSummary.fcLoadHistogram = vehState.energyUse.fcLoadHist;
		lastTripSummary.batteryUse = vehState.energyUse.batteryKWhSinceTripStart;
		lastTripSummary.finalRelSoC = vehState.soc.relSoC;
		lastTripSummary.seconds = vehState.time.secSinceTripStart;
	}

	
	//Class for compact summary of last vehicle trip
	public class TripCSummary {
		public FCFracLoadHistogram fcLoadHistogram;
		public float miles,fuelUse,batteryUse,maxSpeedSlipMPH,finalRelSoC,
			seconds,secondsIdling,secondsFuelConvOn;
		public int nFuelConvStarts;
		public TripCSummary() {reset();}
		public TripCSummary(TripCSummary other) {
			fcLoadHistogram = other.fcLoadHistogram;
			miles = other.miles;
			fuelUse = other.fuelUse;
			batteryUse = other.batteryUse;
			maxSpeedSlipMPH = other.maxSpeedSlipMPH;
			finalRelSoC = other.finalRelSoC;
			seconds = other.seconds;
			secondsIdling = other.secondsIdling;
			secondsFuelConvOn = other.secondsFuelConvOn;
			nFuelConvStarts = other.nFuelConvStarts;
		}
		public void reset() {
			fcLoadHistogram = null;
			miles = 0;
			fuelUse = 0;
			batteryUse = 0;
			maxSpeedSlipMPH = 0;
			finalRelSoC = 0;
			seconds = 0;
			secondsIdling = 0;
			secondsFuelConvOn = 0;
			nFuelConvStarts = 0;
		}
		public float kgH2pm() {
			if (fuelUse<=0) return -1;
			if (miles<=0) return -1;
			return fuelUse/miles;
		}
		public float mpg() {
			if (fuelUse<=0) return -1;
			if (miles<=0) return -1;
			return miles/fuelUse;
		}
		public float literPer100km() {
			if (fuelUse<=0) return -1;
			if (miles<=0) return -1;
			return FSJSimConstants.litrePer100km(miles/fuelUse);
		}
		public float kwhpm() {
			if (miles<=0) return -1;
			return batteryUse/miles;
		}
		public float fracIdling() {
			if (seconds<=0) return -1;
			return secondsIdling/seconds;
		}
		public float fracFuelConvOn() {
			if (seconds<=0) return -1;
			return secondsFuelConvOn/seconds;
		}
		
		public float gmCO2Eq(FuelCalcConstants ghgParam) {
			float gmCO2fuel = 0f;
			float fuel = 0f;
			float elect = 0f;
			
			switch (vehState.vehModel().general.vehPtType) {
			case bev:
				if (batteryUse > 0) elect += batteryUse;
				break;
			case cv:
			case hev:
				if (fuelUse > 0) fuel += fuelUse;
				break;
			case phev:
				if (batteryUse > 0) elect += batteryUse;
				if (fuelUse > 0) fuel += fuelUse;
				break;
			}
			
			switch (vehState.vehModel().fuelConv.fcEffType) {
			case atkins:
			case sparkIgnition:
				gmCO2fuel += fuel*ghgParam.gmCO2perGalGas;
				break;
			case diesel:
			case hybridDiesel:
				gmCO2fuel += fuel*ghgParam.gmCO2perGalDiesel;
				break;
			case fuelCell:
				gmCO2fuel += fuel*ghgParam.gmCO2perKgH2;
				break;
			case cng:
				gmCO2fuel += fuel*ghgParam.gmCO2perM3CNG;
				break;
			}
			
			return gmCO2fuel + elect*ghgParam.gmCO2perKWhElect;
		}
		public float gmCO2perMile(FuelCalcConstants ghgParam) {
			if (miles<=0) return -1;
			return gmCO2Eq(ghgParam)/miles;
		}
		
		@Override public String toString() {
			return ""+miles+","+fuelUse+","+batteryUse+","+maxSpeedSlipMPH+","+finalRelSoC+","+seconds+","+
					secondsIdling+","+secondsFuelConvOn+","+nFuelConvStarts;
		}
	}	
	
	//[Utility] enumeration for quick extraction of time curves from record of previous trip after invoking the runTR() function
	public enum TripRecordOutput {
		Time_sec,				//Time in seconds
		Distance_mi,			//Distance in miles
		Speed_mph,				//Achieved vehicle speed in mile per hour
		RelSOC,					//Relative SOC (0 == min-level, 1 == max-level)
		FuelUse,				//Amount of fuel used (gal-Gas, gal-Diesel or kg-H2) since start of the trip
		FuelConverterKW,		//Output power from the Engine or Fuel Cell in kW
		MotorKW,				//Motor output power (positive = driving, negative = charging) in kW
		BatteryKW,				//Battery output power (positive = depleting, negative = charging) in kW
		//IMPORTANT ... every time a new quantity is added to this list, a new case in extractTimeRecord() function should be added (else output will be zero)
	}
	
	//Utility function for quick extraction of curves from previous trip simulation after invoking runTR() function
	public float[] extractTimeRecord(TripRecordOutput recRequest) {
		if (lastTripVehStates == null) return null;
		
		float[] outputCurve = new float[lastTripVehStates.length];
		
		switch (recRequest) {
		case Time_sec:
		{
			for (int i=0; i<outputCurve.length; i++) {
				outputCurve[i] = lastTripVehStates[i].time.secSinceTripStart;
			}
		}
			break;
		case Distance_mi:
		{
			for (int i=0; i<outputCurve.length; i++) {
				outputCurve[i] = lastTripVehStates[i].motion.milesSinceStart;
			}
		}
			break;
		case Speed_mph:
		{
			for (int i=0; i<outputCurve.length; i++) {
				outputCurve[i] = lastTripVehStates[i].motion.achCurSpeedMPH;
			}
		}
			break;
		case RelSOC:
		{
			for (int i=0; i<outputCurve.length; i++) {
				outputCurve[i] = lastTripVehStates[i].soc.relSoC;
			}
		}
			break;
		case FuelUse:
		{
			for (int i=0; i<outputCurve.length; i++) {
				outputCurve[i] = lastTripVehStates[i].energyUse.fuelUseSinceTripStart;
			}
		}
			break;
		case FuelConverterKW:
		{
			for (int i=0; i<outputCurve.length; i++) {
				outputCurve[i] = lastTripVehStates[i].instPower.fcPowerOut;
			}
		}
			break;
		case MotorKW:
		{
			for (int i=0; i<outputCurve.length; i++) {
				FSJVehState.InstPowerInfo instPower = lastTripVehStates[i].instPower;
				if (instPower.regenKW > 0) outputCurve[i] = -instPower.mtPowerOut;
				else outputCurve[i] = instPower.mtPowerOut;
			}
		}
			break;
		case BatteryKW:
		{
			for (int i=1; i<outputCurve.length; i++) {
				float deltaSec = lastTripVehStates[i].time.secSinceTripStart - lastTripVehStates[i-1].time.secSinceTripStart;
				float deltaKWh = lastTripVehStates[i].energyUse.batteryKWhSinceLastState;
				outputCurve[i] = deltaKWh*3600f/deltaSec;
			}
		}
			break;
		}
		
		return outputCurve;
	}
	// ...Multiple-output version
	public float[][] extractTimeRecord(TripRecordOutput[] recRequest) {
		if (lastTripVehStates == null) return null;
		if (recRequest == null) return null;
		
		float[][] outputCurves = new float[recRequest.length][];
		for (int i=0; i<recRequest.length; i++) {
			outputCurves[i] = extractTimeRecord(recRequest[i]);
		}
		return outputCurves;
	}
	// ...Output to file version
	public void extractTimeRecord(String fileName, TripRecordOutput[] recRequest) {
		float[][] outputCurves = extractTimeRecord(recRequest);
		if (outputCurves == null) return;
		if (recRequest.length < 1) return;
		
		try {
			FileWriter fout = new FileWriter(fileName);
			String lsep = System.getProperty("line.separator");
			
			String st = ""+recRequest[0].name();
			for (int i=1; i<recRequest.length; i++) st = st + "," + recRequest[i].name();
			fout.append(st+lsep);
			
			int nSteps = outputCurves[0].length;
			for (int j=0; j<nSteps; j++) {
				st = ""+outputCurves[0][j];
				for (int i=1; i<recRequest.length; i++) st = st + "," + outputCurves[i][j];
				fout.append(st+lsep);
			}			
			
			fout.flush();
			fout.close();
		} catch (Exception e) {}
	}
}
