package fastsimjava.components;

import java.util.ArrayList;

import fastsimjava.FSJVehModelParam;

//Class for PHEVs Power management with advanced options, including charge hold and battery charge modes
public class FSJHybridPowerManagerAdvPHEV extends FSJHybridPowerManagerDefault {
	
	//Threshold for auto re-initialization at the beginning of a new trip
	public static final float MinSecondsBeforeAbleToChangeMode = 5f;
	public static final float MinMilesBeforeAbleToChangeMode = 0.15f;
	
	//Tuning constants
	public static float HighSpeedBufferMPH = 80f;
	public static float ChgDepleteBufferRelSOC = 0.04f;

	//Identifier for last engaged mode interval ID
	protected int lastModeIntervalID;
	//Saved values for SOC
	protected float prevSOC, dynamicTargetSOC, chgModeSOCStart;
	//Mode Segments
	protected AdvPHEVModeDistanceSegment[] modeSeg;
	//Tracker for how last decision was made
	protected FCKWOutDecisionCondition lastDecisionType;
	
	//Constructor
	public FSJHybridPowerManagerAdvPHEV() {
		//Call super
		super();
		
		//Other initialization
		resetModeIntervals();
	}
	
	public void addChargeMangementSegment_normal(float tripMilesStart) {
		AdvPHEVModeDistanceSegment nSegment = new AdvPHEVModeDistanceSegment();
		nSegment.mode = AdvPHEVMode.ChargeDeplete;
		nSegment.milesStartCurrentMode = tripMilesStart;
		addChargeMangementSegment(nSegment);
	}
	public void addChargeMangementSegment_chgHold(float tripMilesStart) {
		AdvPHEVModeDistanceSegment nSegment = new AdvPHEVModeDistanceSegment();
		nSegment.mode = AdvPHEVMode.ChargeHold;
		nSegment.milesStartCurrentMode = tripMilesStart;
		addChargeMangementSegment(nSegment);
	}
	public void addChargeMangementSegment_chgUp(float tripMilesStart, float milesToTargetSOC, float targetSOC) {
		AdvPHEVModeDistanceSegment nSegment = new AdvPHEVModeDistanceSegment();
		nSegment.mode = AdvPHEVMode.ChargeUp;
		nSegment.milesStartCurrentMode = tripMilesStart;
		nSegment.chgUpMode_milesToTargetSOC = milesToTargetSOC;
		nSegment.chgUpMode_targetSOC = targetSOC;
		addChargeMangementSegment(nSegment);
	}
	
	//Function to reset mode segments
	public void resetModeIntervals() {
		modeSeg = null;
		resetTrip();
	}
	//Function for re-initializing at the beginning of a trip
	public void resetTrip() {
		resetTrip(-1f);
	}
	public void resetTrip(float initialRelSOC) {
		lastModeIntervalID = 0;
		prevSOC = initialRelSOC;
		dynamicTargetSOC = initialRelSOC;
		chgModeSOCStart = initialRelSOC;
	}
	
	//Function to inform the vehicle state whether they are operating in "charge-sustain"-like conditions
	public boolean isInChargeSustain(FSJVehState vehCurState) {
		switch (vehCurState.vehModel().general.vehPtType) {
		case phev:
			if (modeSeg == null) {
				return defaultIsInChargeSustain(vehCurState);
			} else if (lastModeIntervalID < 0) {
				return defaultIsInChargeSustain(vehCurState);
			}
			
			switch (modeSeg[lastModeIntervalID].mode) {
			case ChargeDeplete:
			{
				if (prevSOC > ChgDepleteBufferRelSOC) return false;
				return true;
			}
			default:
				return true;
			}
		default:
			return defaultIsInChargeSustain(vehCurState);
		}		
	}
	
	
	//Function in-which "current" state is set, and calculation of the fuel converter power output (0 = off) is determined and stored in fuelConvKWOut
	public void setCurState(FSJVehState vehCurState, float mphDesired, float kWDesiredAtWheels, float fcMod,
			float essChgDischgEffn, float totalAuxKW) {
		//If not a PHEV, simply call the same function of parent class
		switch (vehCurState.vehModel().general.vehPtType) {
		case phev:
			if (modeSeg == null) {
				defaultCalculationOfFuelConvKWOut(vehCurState, mphDesired, kWDesiredAtWheels, fcMod, essChgDischgEffn, totalAuxKW);
				return;
			}
			break;	
		default:
			//Call default calculation (Decision model by NREL) for HEVs
			defaultCalculationOfFuelConvKWOut(vehCurState, mphDesired, kWDesiredAtWheels, fcMod, essChgDischgEffn, totalAuxKW);
			return;		
		}
		
		//Reaching here means it's a PHEV and is employing some advanced mode
			//Check if trip auto-reset is needed
		if ((prevSOC < 0) || ((lastModeIntervalID > 0) && (vehCurState.time.secSinceTripStart <= MinSecondsBeforeAbleToChangeMode))) {
			resetTrip(vehCurState.soc.relSoC); 
		}
		
			//Update SOC (even if not resetting trip)
		prevSOC = vehCurState.soc.relSoC;
		
			//Check if there should be a segment change
		float curMiles = vehCurState.motion.milesSinceStart;
		boolean segmentChanged = false;
		for (int i=lastModeIntervalID+1; i<modeSeg.length; i++) {
			if (curMiles >= modeSeg[i].milesStartCurrentMode) {
				segmentChanged =  true;
				break;
			}
		}
		if (segmentChanged) {
			lastModeIntervalID++;
			dynamicTargetSOC = prevSOC;	//Dynamically identify/set target SOC if charge hold mode was engaged
			chgModeSOCStart = prevSOC;
		}
		
		//Set target SOC depending on the charge control mode in current segment
		switch (modeSeg[lastModeIntervalID].mode) {
		case ChargeDeplete:
			dynamicTargetSOC = 0f;	//Let the battery drain
			break;
		case ChargeHold:
			//Do nothing, the value of dynamicTargetSOC was set when this segment was first entered
			break;
		case ChargeUp:
			if (curMiles >= (modeSeg[lastModeIntervalID].milesStartCurrentMode + modeSeg[lastModeIntervalID].chgUpMode_milesToTargetSOC)) {
				//Beyond the target point, SOC should be maintained at desired level
				dynamicTargetSOC = modeSeg[lastModeIntervalID].chgUpMode_targetSOC;				
			} else {
				float milesRemainingToTarget = modeSeg[lastModeIntervalID].milesStartCurrentMode 
						+ modeSeg[lastModeIntervalID].chgUpMode_milesToTargetSOC - curMiles;
				
				float c1 = milesRemainingToTarget/modeSeg[lastModeIntervalID].chgUpMode_milesToTargetSOC;
				float c2 = 1f - c1;
				dynamicTargetSOC = c1*chgModeSOCStart + c2*modeSeg[lastModeIntervalID].chgUpMode_targetSOC;				
			}
			break;
		}
		
		//Data Objects
		FSJVehModelParam vehModel = vehCurState.vehModel();
		FSJSimConstants simConst = vehCurState.simConsts();
		FSJMotor motor = vehCurState.motor();
		FSJFuelConverter fuelConv = vehCurState.fuelConv();
		
		//Calculate adjusted target SOC
		float adjustedMinAbsSOC = Math.min(
				calcRegenSOCBuffer(vehModel.chargeControl.minSoCBatterySwing, vehModel.chargeControl.maxSoCBatterySwing, HighSpeedBufferMPH, vehModel, simConst),				
				vehModel.chargeControl.minSoCBatterySwing 
				+ dynamicTargetSOC*(vehModel.chargeControl.maxSoCBatterySwing - vehModel.chargeControl.minSoCBatterySwing)
				);

		//Calculate fuel converter power output
		advPHEVModesCalculationOfFuelConvKWOut(vehCurState, mphDesired, kWDesiredAtWheels, fcMod, essChgDischgEffn, totalAuxKW,
				adjustedMinAbsSOC, vehModel, simConst, motor, fuelConv);
	}
	public FCKWOutDecisionCondition getLastDecisionCondition() {
		return lastDecisionType;
	}
	
	protected void advPHEVModesCalculationOfFuelConvKWOut(FSJVehState vehCurState, float mphDesired, float kWDesiredAtWheels, float fcMod,
			float essChgDischgEffn, float totalAuxKW, float adjustedMinAbsSOC, FSJVehModelParam vehModel, FSJSimConstants simConst, 
			FSJMotor motor, FSJFuelConverter fuelConv) {
		
		//Limit for maximum output mechanical power by electric motor
		float deltaSec = vehCurState.time.deltaSecFromLastState;
		float curMaxMotorMechKWOut = Math.min(vehModel.motor.maxMotorKw, 
				Math.max(0, vehCurState.instPower.mtPowerOut)+(deltaSec*vehModel.motor.maxMotorKw/vehModel.motor.motorSecsToPeakPwr));
		
		//Limit for fuel converter maximum power output
		float maxFCKWout = fcMod*Math.min(vehModel.fuelConv.maxFuelConvKw, 
				vehCurState.instPower.fcPowerOut + deltaSec*vehModel.fuelConv.maxFuelConvKw/vehModel.fuelConv.fuelConvSecsToPeakPwr);
		
		//Fuel converter output power at maximum efficiency
		float fcKWatMaxEff = fcMod*fuelConv.powerAtMaxEffKW();
		
		
		//Current Power demand before transmission
		float x = 0f;
		if (kWDesiredAtWheels > 0) {
			x = kWDesiredAtWheels/vehModel.transmission.transEff;
		} else if (kWDesiredAtWheels < 0) {
			x = kWDesiredAtWheels*vehModel.transmission.transEff;
		}

		//Condition #1: Check that SOC did not dip too low
		float curAbsSOC = vehCurState.soc.absSoC;
		float accSOCBuffer = calcAccSOCBuffer(adjustedMinAbsSOC, vehModel.chargeControl.maxSoCBatterySwing, 
				mphDesired,	vehModel, simConst);
		
		if (curAbsSOC < accSOCBuffer) {
			float regenSOCBuffer = calcRegenSOCBuffer(adjustedMinAbsSOC, vehModel.chargeControl.maxSoCBatterySwing, mphDesired,	vehModel, simConst);
			float maxEssRegenBufferChgKW = calcMaxEssRegenBufferChgKW(regenSOCBuffer, curAbsSOC, deltaSec, vehModel);
			float essRegenBufferDischgKW = calcEssRegenBufferDischgKW(regenSOCBuffer, curAbsSOC, deltaSec, vehModel);
			float essAccBufferChgKW = calcEssAccBufferChgKW(accSOCBuffer, curAbsSOC, deltaSec, vehModel);
			float essAccBufferDischgKW = calcEssAccBufferDischgKW(accSOCBuffer, curAbsSOC, deltaSec, vehModel);
			float essAccRegenDischKW = Math.max(-vehModel.battery.maxEssKw,
					calcMtEssAccRegenDischKW(accSOCBuffer, regenSOCBuffer, curAbsSOC, deltaSec, vehModel, essAccBufferChgKW, essRegenBufferDischgKW));
			float desiredESSKW4FCEff = calcDesiredESSKW4FCEff(fuelConv, motor, x, vehModel);
			
			float targetEssKW = Math.max(-vehModel.battery.maxEssKw,
					calcMtTargetEssKW(accSOCBuffer, regenSOCBuffer, essAccRegenDischKW, essAccBufferChgKW,
					essAccBufferDischgKW, desiredESSKW4FCEff, maxEssRegenBufferChgKW));	//This is a negative value when this if-statement condition is in effect
			
			//float targetMotorMechKWIn = Math.min(motor.inputPowerKW(-targetEssKW + totalAuxKW)/essChgDischgEffn, vehModel.motor.maxMotorKw);	//Older version, no longer used 
			float targetMotorMechKWIn = Math.min(motor.inputPowerKW(-targetEssKW + totalAuxKW), vehModel.motor.maxMotorKw);	//This keeps things in-line with "ESS kW"-notation in Excel version
			fuelConvKWOut = Math.min(maxFCKWout, Math.max(0, x)+targetMotorMechKWIn);
			lastDecisionType = FCKWOutDecisionCondition.Condition1;
			return;
		}
		
		//Condition #2: Power assist in (non-fuel cell parallel drive)
		//	Note: demand kW (from achieved vehicle speed) will not exceed capability of the electric motor unless it's a parallel drive
		if (x > curMaxMotorMechKWOut) {
			fuelConvKWOut = Math.min(maxFCKWout, Math.max(x - curMaxMotorMechKWOut, fcKWatMaxEff));
			lastDecisionType = FCKWOutDecisionCondition.Condition2;
			return;
		}

		//Condition #3: Compensating low KW capability of Battery
		float ym = 0f;
		if (x > 0) ym = motor.inputPowerKW(x);
		float yb = (ym + totalAuxKW)/essChgDischgEffn;
		if ((!vehModel.battery.overrideMaxEsskw)&&(yb > vehModel.battery.maxEssKw)) {
			float fcAccelAssistKW = 0f;
			
			switch (vehModel.fuelConv.fcEffType) {
			case fuelCell:
				fcAccelAssistKW = yb - vehModel.battery.maxEssKw;
				break;
			default:
				ym = Math.min(curMaxMotorMechKWOut, motor.outputPowerKW(vehModel.battery.maxEssKw-totalAuxKW));
				fcAccelAssistKW = x - ym;
				break;			
			}	
			
			fuelConvKWOut = Math.min(maxFCKWout, Math.max(fcAccelAssistKW, fcKWatMaxEff));
			lastDecisionType = FCKWOutDecisionCondition.Condition3;
			return;
		}
		
		//Other Conditions that force or keep fuel converter on (but not strictly required for driving)
		//Engine Had been turned On recently
		if ((vehCurState.isFuelConvOn())&&(vehCurState.time.secFuelConvOn < engineOnStaysOnSec)) {
			fuelConvKWOut = Math.min(maxFCKWout, fcKWatMaxEff);
			lastDecisionType = FCKWOutDecisionCondition.Condition4;
			return;
		}
		//Power demand or speed exceeds some threshold
		if ((mphDesired > vehModel.chargeControl.mphFcOn)||(yb > vehModel.chargeControl.kwDemandFcOn)) {
			float regenSOCBuffer = calcRegenSOCBuffer(adjustedMinAbsSOC, vehModel.chargeControl.maxSoCBatterySwing, mphDesired,	vehModel, simConst);
			if (curAbsSOC < regenSOCBuffer) {
				fuelConvKWOut = Math.min(maxFCKWout, fcKWatMaxEff);
				lastDecisionType = FCKWOutDecisionCondition.Condition5;
				return;
			}
		}
		//Otherwise Fuel COnverter is Off
		fuelConvKWOut = 0f;
		lastDecisionType = FCKWOutDecisionCondition.Condition6;
	}
	
	//Assisting Classes and Functions
	public enum FCKWOutDecisionCondition {
		Condition1, Condition2, Condition3, Condition4, Condition5, Condition6
	}
	
	protected enum AdvPHEVMode {
		ChargeDeplete, ChargeHold, ChargeUp
	}
	protected class AdvPHEVModeDistanceSegment {
		protected AdvPHEVMode mode;
		protected float milesStartCurrentMode;		
		protected float chgUpMode_milesToTargetSOC, chgUpMode_targetSOC;
		protected AdvPHEVModeDistanceSegment() {}
	}
	private void addChargeMangementSegment(AdvPHEVModeDistanceSegment nSegment) {
		//Should not be able to do this while in the middle of a trip simulation, only after resetting a trip (or the class being "newly created")
		if (dynamicTargetSOC >= 0) return;
		
		//First time adding a segment
		if (modeSeg == null) {
			if (nSegment.milesStartCurrentMode > MinMilesBeforeAbleToChangeMode) {
				//Starts later than beginning of trip, add a default segment until then
				modeSeg = new AdvPHEVModeDistanceSegment[2];
				modeSeg[1] = nSegment;

				modeSeg[0] = new AdvPHEVModeDistanceSegment();
				modeSeg[0].mode = AdvPHEVMode.ChargeDeplete;
				modeSeg[0].milesStartCurrentMode = 0f;
			} else {
				//Starts at beginning of trip
				modeSeg = new AdvPHEVModeDistanceSegment[1];
				modeSeg[0] = nSegment;
				modeSeg[0].milesStartCurrentMode = 0f;
			}
			return;
		}
		
		//Adding "another" segment (and other segments, including one from start of the rip already exist)
		if (nSegment.milesStartCurrentMode <= MinMilesBeforeAbleToChangeMode) {
			modeSeg[0] = nSegment;	//Simply replace the first segment
			return;
		}
		
		for (int i=1; i<modeSeg.length; i++) {
			if (Math.abs(modeSeg[i].milesStartCurrentMode - nSegment.milesStartCurrentMode) <= MinMilesBeforeAbleToChangeMode) {
				modeSeg[i] = nSegment;	//Simply replacing an existing segment
				return;
			}
		}
		
		ArrayList<AdvPHEVModeDistanceSegment> lst = new ArrayList<AdvPHEVModeDistanceSegment>();
		for (int i=0; i<modeSeg.length; i++) lst.add(modeSeg[i]);
		
		int insertPos = 1;
		for (int i=1; i<lst.size(); i++) {
			if (lst.get(i).milesStartCurrentMode > nSegment.milesStartCurrentMode) break;
			insertPos++;
		}
		
		lst.add(insertPos, nSegment);
		modeSeg = new AdvPHEVModeDistanceSegment[lst.size()];
		for (int i=0; i<modeSeg.length; i++) modeSeg[i] = lst.get(i);
	}
}
