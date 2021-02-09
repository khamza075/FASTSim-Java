package fastsimjava.components;

import fastsimjava.*;
import fastsimjava.abs.*;

public class FSJHybridPowerManagerDefault extends FSJHybridPowerManagerBase {
	//Updated value for FC output power
	private float fuelConvKWOut;
	//Function to return current state of fuel converter output
	public float fuelConvKWOut() {
		return fuelConvKWOut;
	}
	
	//Tuning constants for the FC decision model
	public float engineOnStaysOnSec;
	
	//Constructor, sets the default values for tuning constants
	public FSJHybridPowerManagerDefault() {
		engineOnStaysOnSec = 4f;	//Adjust as needed by model of vehicle
	}

	public void setCurState(FSJVehState vehCurState, float mphDesired, float kWDesiredAtWheels, float fcMod,
			float essChgDischgEffn, float totalAuxKW) {
		//Data Objects
		FSJVehModelParam vehModel = vehCurState.vehModel();
		FSJSimConstants simConst = vehCurState.simConsts();
		FSJMotor motor = vehCurState.motor();
		FSJFuelConverter fuelConv = vehCurState.fuelConv();
		
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
		float accSOCBuffer = calcAccSOCBuffer(vehModel.chargeControl.minSoCBatterySwing, vehModel.chargeControl.maxSoCBatterySwing, 
				mphDesired,	vehModel, simConst);
		
		if (curAbsSOC < accSOCBuffer) {
			float regenSOCBuffer = calcRegenSOCBuffer(vehModel.chargeControl.minSoCBatterySwing, vehModel.chargeControl.maxSoCBatterySwing, 
					mphDesired,	vehModel, simConst);
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
			return;
		}
		
		//Condition #2: Power assist in (non-fuel cell parallel drive)
		//	Note: demand kW (from achieved vehicle speed) will not exceed capability of the electric motor unless it's a parallel drive
		if (x > curMaxMotorMechKWOut) {
			fuelConvKWOut = Math.min(maxFCKWout, Math.max(x - curMaxMotorMechKWOut, fcKWatMaxEff));
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
			return;
		}
		
		//Other Conditions that force fuel converter on (but not strictly required for driving)
			//Engine Had been turned On recently
		if ((vehCurState.isFuelConvOn())&&(vehCurState.time.secFuelConvOn < engineOnStaysOnSec)) {
			fuelConvKWOut = Math.min(maxFCKWout, fcKWatMaxEff);
			return;
		}
			//Power demand or speed exceeds some threshold
		if ((mphDesired > vehModel.chargeControl.mphFcOn)||(yb > vehModel.chargeControl.kwDemandFcOn)) {
			float regenSOCBuffer = calcRegenSOCBuffer(vehModel.chargeControl.minSoCBatterySwing, vehModel.chargeControl.maxSoCBatterySwing, 
					mphDesired,	vehModel, simConst);
			if (curAbsSOC < regenSOCBuffer) {
				fuelConvKWOut = Math.min(maxFCKWout, fcKWatMaxEff);
				return;
			}
		}
		
		//Fuel COnverter is Off
		fuelConvKWOut = 0f;
	}


	//Sub-functions
	private float calcMtTargetEssKW(float accSOCBuffer, float regenSOCBuffer, float essAccRegenDischKW, float essAccBufferChgKW,
			float essAccBufferDischgKW, float desiredESSKW4FCEff, float maxEssRegenBufferChgKW) {
		if (accSOCBuffer > regenSOCBuffer) return essAccRegenDischKW;
		if (essAccBufferChgKW > 0) return Math.max(-maxEssRegenBufferChgKW, Math.min(desiredESSKW4FCEff, -essAccBufferChgKW));
		if (desiredESSKW4FCEff > 0) return Math.min(desiredESSKW4FCEff, essAccBufferDischgKW);
		return Math.max(desiredESSKW4FCEff, -maxEssRegenBufferChgKW);
	}
	private float calcMtEssAccRegenDischKW(float accSOCBuffer, float regenSOCBuffer, float curAbsSOC, float deltaSec, FSJVehModelParam vehModel,
			float accBufferChgKW, float essRegenBufferDischgKW) {
		float essKWh = vehModel.battery.maxEssKwh;
		
		if (regenSOCBuffer < accSOCBuffer) return (curAbsSOC - 0.5f*(regenSOCBuffer+accSOCBuffer))*essKWh*3600f/deltaSec;
		if (curAbsSOC > regenSOCBuffer) return essRegenBufferDischgKW;
		if (curAbsSOC < accSOCBuffer) return -accBufferChgKW;
		return 0f;
	}
	private float calcEssAccBufferDischgKW(float accSOCBuffer, float curAbsSOC, float deltaSec, FSJVehModelParam vehModel) {
		float essKWh = vehModel.battery.maxEssKwh;		
		float essKW = vehModel.battery.maxEssKw;
		
		return Math.min(essKW, Math.max(0f, (curAbsSOC - accSOCBuffer)*essKWh*3600f/deltaSec));
	}
	private float calcEssAccBufferChgKW(float accSOCBuffer, float curAbsSOC, float deltaSec, FSJVehModelParam vehModel) {
		float essKWh = vehModel.battery.maxEssKwh;		
		return Math.max(0f, (accSOCBuffer - curAbsSOC)*essKWh*3600f/deltaSec);
	}
	private float calcEssRegenBufferDischgKW(float regenSOCBuffer, float curAbsSOC, float deltaSec, FSJVehModelParam vehModel) {
		float essKWh = vehModel.battery.maxEssKwh;
		float essKW = vehModel.battery.maxEssKw;
		
		return Math.min(essKW, Math.max(0f, (curAbsSOC - regenSOCBuffer)*essKWh*3600f/deltaSec));
	}
	private float calcMaxEssRegenBufferChgKW(float regenSOCBuffer, float curAbsSOC, float deltaSec, FSJVehModelParam vehModel) {
		float essKWh = vehModel.battery.maxEssKwh;
		float essKW = vehModel.battery.maxEssKw;
		
		return Math.min(essKW, Math.max(0f, (regenSOCBuffer - curAbsSOC)*essKWh*3600f/deltaSec));
	}
	private float calcDesiredESSKW4FCEff(FSJFuelConverter fuelConv, FSJMotor motor, float mechKWPreTransmission, FSJVehModelParam vehModel) {
		float fcKWAtPeakEff = fuelConv.powerAtMaxEffKW();
		float kwX = mechKWPreTransmission - fcKWAtPeakEff;
		float delta = 0f;
		
		if (kwX > 0) {	//Motor is driving (since demand is beyond max efficiency point)
			delta = vehModel.chargeControl.essDischargeEffortMxFCEff*motor.inputPowerKW(kwX);
		} else if (kwX < 0) {	//Motor is generating (since demand is less than max efficiency point)
			delta = -vehModel.chargeControl.essChargeEffortMxFCEff*motor.outputPowerKW(-kwX);
		}
		
		return delta;
	}
	private float calcRegenSOCBuffer(float minAbsSOC, float maxAbsSOC, float speedMPH, FSJVehModelParam vehModel, FSJSimConstants simConst) {
		float speedMS = speedMPH/FSJSimConstants.mphPerMps;
		float vehKg = vehModel.massProp.totalKg;
		float maxRegen = vehModel.transmission.maxRegen;
		float motorPeakEffn = vehModel.motor.motorPeakEff;
		float essKWh = vehModel.battery.maxEssKwh;
			
		return Math.max(minAbsSOC, maxAbsSOC - (0.5f*vehKg*speedMS*speedMS*motorPeakEffn*maxRegen)/(3600f*1000f*essKWh));
	}
	private float calcAccSOCBuffer(float minAbsSOC, float maxAbsSOC, float speedMPH, FSJVehModelParam vehModel, FSJSimConstants simConst) {
		float speedMS = speedMPH/FSJSimConstants.mphPerMps;
		float vehKg = vehModel.massProp.totalKg;
		float bSpeedMS = vehModel.chargeControl.mphESSAccRsrvZero/FSJSimConstants.mphPerMps;
		float bFrac = vehModel.chargeControl.percentESSReserveForAccel;
		float essKWh = vehModel.battery.maxEssKwh;
		
		return Math.min(maxAbsSOC, 
				Math.max(minAbsSOC, 
						minAbsSOC + ((bSpeedMS*bSpeedMS - speedMS*speedMS)/(bSpeedMS*bSpeedMS))
							*Math.min(bFrac*(maxAbsSOC - minAbsSOC), 
									(0.5f*vehKg*27f*27f)/(3600f*1000f*essKWh))));
	}
}
