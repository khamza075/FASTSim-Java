package fastsimjava;

import fastsimjava.abs.*;
import fastsimjava.components.*;

public class FASTSimJ3c {
	//Header String for Compact Summary
	public static final String Header_CTripSummay = "miles,fuelUse,batteryUse,maxSpeedSlipMPH,finalRelSoC,seconds,secondsIdling,secondsFuelConvOn,nFuelConvStarts";
	
	
	//Simulation constants
	public FSJSimConstants simConsts;
	
	//Current Vehicle State
	private FSJVehState vehState;
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
	}

	//Function to set the vehicle model (do this before simulation, model remains until set to something else)
	// Note: BEVs and PHEVs are initialized to fully charge (relSoC = 1) by default, use SoC modification functions
	//       if a different initialization is desired
	public void setVehModel(FSJVehModelParam vehModel, FSJEffCurvesManager curveManager) {
		vehState = new FSJVehState(simConsts, vehModel, curveManager);
	}
	//With built-in support for 3-Parameter tuning
	public void setVehModel(FSJVehModelParam vehModel, FSJEffCurvesManager curveManager, float addMass, float addAux, float adjDEMult) {
		vehState = new FSJVehState(simConsts, vehModel, curveManager, addMass, addAux, adjDEMult);
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
		FSJVehModelParam.ChargeControlParam chPar = vehState.vehModel().chargeControl;
		vehState.soc.relSoC = relSoC;
		vehState.soc.absSoC = chPar.minSoCBatterySwing + (chPar.maxSoCBatterySwing - chPar.minSoCBatterySwing)*relSoC;
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
		//Note: hybPwrMgr may CANNOT be null when invoking this version of run() function

		//Exit if no vehicle model exists
		if (vehState==null) return;
		
		//Kill previously saved results
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
}
