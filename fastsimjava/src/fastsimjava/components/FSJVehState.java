package fastsimjava.components;

import java.io.FileWriter;

import fastsimjava.*;
import fastsimjava.abs.*;

public class FSJVehState {
	//Header Strings
	private static final String Header_MotionInfo = "prevSpeedMPH,prevSpeedMS,curDesiredSpeedMPH,achCurSpeedMPH,achCurSpeedMS,curSpeedSlipMPH,curRoadGrade,metersFromLastState,milesSinceStart";
	private static final String Header_TimeInfo = "secSinceTripStart,deltaSecFromLastState,secFuelConvOn,secFuelConvOff";
	private static final String Header_BatterySOCInfo = "swingKWh,relSoC,absSoC";
	private static final String Header_EnergyUseInfo = "batteryKWhSinceLastState,batteryKWhSinceTripStart,fuelUseSinceLastState,fuelUseSinceTripStart";	
	private static final String Header_InstPowerInfo = "dragKW,ascentKW,rollResKW,accelKW,auxKW,hvacKW,fricBrakeKW,regenKW,fcPowerOut,fcPowerIn,fcEffn,mtPowerOut,mtPowerIn,mtEffn";
	public static final String HeaderString = Header_TimeInfo+","+Header_MotionInfo+","+Header_BatterySOCInfo+","+Header_EnergyUseInfo+","+Header_InstPowerInfo;
	
	
	//Link to simulation constants
	private FSJSimConstants simConsts;
	public FSJSimConstants simConsts() {return simConsts;}
	
	//Link to Vehicle model parameters
	private FSJVehModelParam vehModel;
	public FSJVehModelParam vehModel() {return vehModel;}
	
	//Fuel Converter
	private FSJFuelConverter fuelConv;
	public FSJFuelConverter fuelConv() {return fuelConv;}	
	
	//Electric Motor
	private FSJMotor motor;
	public FSJMotor motor() {return motor;}	

	//Three Parameter Tuning
	private ThreeParTuning tpt;
	
	//Data Objects
	public TimeInfo time;
	public MotionInfo motion;
	public BatterySOCInfo soc;
	public EnergyUseInfo energyUse;
	public InstPowerInfo instPower;
	
	//Constructors
	public FSJVehState(FSJSimConstants sConsts, FSJVehModelParam vModel, FSJEffCurvesManager curveMan) {
		reset(sConsts, vModel, curveMan);
	}
	public FSJVehState(FSJSimConstants sConsts, FSJVehModelParam vModel, FSJEffCurvesManager curveMan, float addMass, float addAux, float adjDEMult) {
		reset(sConsts, vModel, curveMan, addMass, addAux, adjDEMult);
	}	
	public FSJVehState(FSJVehState other) {
		//Copy links
		simConsts = other.simConsts;
		vehModel = other.vehModel;
		fuelConv = other.fuelConv;
		motor = other.motor;
		tpt = other.tpt;
		
		//Use copy constructor of data objects
		time = new TimeInfo(other.time);
		motion = new MotionInfo(other.motion);
		soc = new BatterySOCInfo(other.soc);
		energyUse = new EnergyUseInfo(other.energyUse);
		instPower = new InstPowerInfo(other.instPower);
	}	
	//Reset functions
	public void reset(FSJSimConstants sConsts, FSJVehModelParam vModel, FSJEffCurvesManager curveMan) {
		ThreeParTuning tmpTPT = new ThreeParTuning();
		reset(sConsts, vModel, curveMan, tmpTPT.addMass, tmpTPT.addAux, tmpTPT.adjDEMult);
	}
	public void reset(FSJSimConstants sConsts, FSJVehModelParam vModel, FSJEffCurvesManager curveMan, float addMass, float addAux, float adjDEMult) {
		//Link constants and vehicle model
		simConsts = sConsts;
		vehModel = vModel;
		
		//Create fuel converter and electric motor models
		fuelConv = new FSJFuelConverter(vehModel, curveMan);
		motor = new FSJMotor(vehModel, curveMan);
		
		//Create Three-Parameter Tuning Object
		tpt = new ThreeParTuning(addMass, addAux, adjDEMult);
		
		//Create data objects
		time = new TimeInfo();
		motion = new MotionInfo();
		soc = new BatterySOCInfo();
		energyUse = new EnergyUseInfo();
		instPower = new InstPowerInfo();
	}
	public void resetAllExceptSOC() {
		time.reset();
		motion.reset();
		energyUse.reset();
		instPower.reset();
	}
	
	//Sub-class for holding Three-parameter tuning values
	public static class ThreeParTuning {
		public float addMass, addAux, adjDEMult;
		
		public ThreeParTuning() {
			addMass = 0f;
			addAux = 0f;
			adjDEMult = 1f;
		}
		public ThreeParTuning(float aMass, float aAux, float aDEMult) {
			addMass = aMass;
			addAux = aAux;
			adjDEMult = aDEMult;
		}
	}
	
	//Sub-class for time-related information
	public class TimeInfo {
		public float secSinceTripStart,deltaSecFromLastState,secFuelConvOn,secFuelConvOff;
		private TimeInfo() {
			reset();
		}
		private TimeInfo(TimeInfo other) {
			secSinceTripStart = other.secSinceTripStart;
			deltaSecFromLastState = other.deltaSecFromLastState;
			secFuelConvOn = other.secFuelConvOn;
			secFuelConvOff = other.secFuelConvOff;
		}
		public void reset() {
			secSinceTripStart = 0f;
			deltaSecFromLastState = 0f;
			secFuelConvOn = -1;		//Initialization, value < 0 implies fuel converter is currently off
			secFuelConvOff = 0f;	//Initialization, value < 0 implies fuel converter is currently on
		}
		@Override public String toString() {
			return ""+secSinceTripStart+","+deltaSecFromLastState+","+secFuelConvOn+","+secFuelConvOff;
		}
	}
	public boolean isFuelConvOn() {
		if (time.secFuelConvOn < 0) return false;
		return true;
	}
	private void turnFuelConvOn() {
		if (time.secFuelConvOn > 0) return;
		time.secFuelConvOn = 0f;
		time.secFuelConvOff = -1;
		instPower.fcPowerOut = 0f;
	}
	private void turnFuelConvOff() {
		if (time.secFuelConvOn < 0) return;
		time.secFuelConvOn = -1;
		time.secFuelConvOff = 0f;
		instPower.fcPowerOut = 0f;
		instPower.fcPowerIn = 0f;
		instPower.fcEffn = 0f;
	}
	
	//Sub-class for motion information
	public class MotionInfo {
		public float prevSpeedMPH,prevSpeedMS,curDesiredSpeedMPH,achCurSpeedMPH,achCurSpeedMS,
			curSpeedSlipMPH,curRoadGrade,metersFromLastState,milesSinceStart,
			maxAccelMS2,maxDecelMS2;
		private MotionInfo() {
			reset();
		}
		private MotionInfo(MotionInfo other) {
			prevSpeedMPH = other.prevSpeedMPH;
			prevSpeedMS = other.prevSpeedMS;
			curDesiredSpeedMPH = other.curDesiredSpeedMPH;
			achCurSpeedMPH = other.achCurSpeedMPH;
			achCurSpeedMS = other.achCurSpeedMS;
			curSpeedSlipMPH = other.curSpeedSlipMPH;
			curRoadGrade = other.curRoadGrade;
			metersFromLastState = other.metersFromLastState;
			milesSinceStart = other.milesSinceStart;			
			maxAccelMS2 = other.maxAccelMS2;
			maxDecelMS2 = other.maxDecelMS2;
		}
		public void reset() {
			//Things that change during the trip
			prevSpeedMPH = 0f;
			prevSpeedMS = 0f;
			curDesiredSpeedMPH = 0f;
			achCurSpeedMPH = 0f;
			achCurSpeedMS = 0f;
			curSpeedSlipMPH = 0f;
			curRoadGrade = 0f;
			metersFromLastState = 0f;
			milesSinceStart = 0f;

			//Constants (calculated only once)
			maxAccelMS2 = vehModel.wheels.wheelCoeffOfFric*vehModel.general.driveAxleWeightFrac*simConsts.gravity/
					(1f + (vehModel.general.vehCgM*vehModel.wheels.wheelCoeffOfFric/vehModel.general.wheelBaseM));
			maxDecelMS2 = vehModel.wheels.wheelCoeffOfFric*simConsts.gravity;
		}
		@Override public String toString() {
			return ""+prevSpeedMPH+","+prevSpeedMS+","+curDesiredSpeedMPH
					+","+achCurSpeedMPH+","+achCurSpeedMS
					+","+curSpeedSlipMPH+","+curRoadGrade
					+","+metersFromLastState+","+milesSinceStart;
		}
	}
	
	//Sub-class for battery state of charge information
	public class BatterySOCInfo {
		public float swingKWh,relSoC,absSoC;
		private BatterySOCInfo() {
			reset();
		}
		private BatterySOCInfo(BatterySOCInfo other) {
			swingKWh = other.swingKWh;
			relSoC = other.relSoC;
			absSoC = other.absSoC;
		}
		
		public void reset() {
			switch (vehModel.general.vehPtType) {
			case cv:
				swingKWh = 0f;
				relSoC = 0f;
				absSoC = 0f;
				break;
			case hev:
			case bev:
			case phev:
				swingKWh = vehModel.batterySwingKWh();
				relSoC = 1f;
				absSoC = vehModel.chargeControl.maxSoCBatterySwing;
				break;
			}
		}
		@Override public String toString() {
			return ""+swingKWh+","+relSoC+","+absSoC;
		}
	}
	
	//Sub-class for fuel and electric energy usage
	public class EnergyUseInfo {
		//Note fuel use units are gallon-Gas, gallon-Diesel or kg-H2, depending on fuel converter type
		public float batteryKWhSinceLastState,batteryKWhSinceTripStart,fuelUseSinceLastState,fuelUseSinceTripStart;
		
		//Histogram for Fuel Converter Usage
		public FCFracLoadHistogram fcLoadHist;
		
		private EnergyUseInfo() {
			fcLoadHist = new FCFracLoadHistogram(vehModel.fuelConv.maxFuelConvKw);
			reset();
		}
		private EnergyUseInfo(EnergyUseInfo other) {
			batteryKWhSinceLastState = other.batteryKWhSinceLastState;
			batteryKWhSinceTripStart = other.batteryKWhSinceTripStart;
			fuelUseSinceLastState = other.fuelUseSinceLastState;
			fuelUseSinceTripStart = other.fuelUseSinceTripStart;
			
			fcLoadHist = new FCFracLoadHistogram(other.fcLoadHist);
		}

		public void reset() {
			batteryKWhSinceLastState = 0f;
			batteryKWhSinceTripStart = 0f;
			fuelUseSinceLastState = 0f;
			fuelUseSinceTripStart = 0f;
			
			fcLoadHist.reset();
		}
		@Override public String toString() {
			return ""+batteryKWhSinceLastState+","+batteryKWhSinceTripStart
					+","+fuelUseSinceLastState+","+fuelUseSinceTripStart;
		}
	}
		
	//Sub-class for various power consumption terms at the current instant
	public class InstPowerInfo {
		public float dragKW,ascentKW,rollResKW,accelKW,auxKW,hvacKW,fricBrakeKW,regenKW;
		public float fcPowerOut,fcPowerIn,fcEffn,mtPowerOut,mtPowerIn,mtEffn;
		
		private InstPowerInfo() {
			reset();
		}
		private InstPowerInfo(InstPowerInfo other) {
			dragKW = other.dragKW;
			ascentKW = other.ascentKW;
			rollResKW = other.rollResKW;
			accelKW = other.accelKW;
			
			auxKW = other.auxKW;
			hvacKW = other.hvacKW;
			
			fricBrakeKW = other.fricBrakeKW;
			regenKW = other.regenKW;
			
			fcPowerOut = other.fcPowerOut;
			fcPowerIn = other.fcPowerIn;
			fcEffn = other.fcEffn;
			
			mtPowerOut = other.mtPowerOut;
			mtPowerIn = other.mtPowerIn;
			mtEffn = other.mtEffn;
		}

		public void reset() {
			dragKW = 0f;
			ascentKW = 0f;
			rollResKW = 0f;
			accelKW = 0f;
			
			auxKW = 0f;
			hvacKW = 0f;
			
			fricBrakeKW = 0f;
			regenKW = 0f;
			
			fcPowerOut = 0f;
			fcPowerIn = 0f;
			fcEffn = 0f;
			
			mtPowerOut = 0f;
			mtPowerIn = 0f;
			mtEffn = 0f;
		}
		
		@Override public String toString() {
			return ""+dragKW+","+ascentKW+","+rollResKW+","+accelKW+","+auxKW+","+hvacKW
					+","+fricBrakeKW+","+regenKW+","+fcPowerOut+","+fcPowerIn+","+fcEffn
					+","+mtPowerOut+","+mtPowerIn+","+mtEffn;
		}
	}
	
	//Function for returning a comma-separated string of all values
	@Override public String toString() {
		return time.toString()+","+motion.toString()+","+soc.toString()
				+","+energyUse.toString()+","+instPower.toString();
	}
	
	//Utility function to save array to file
	public static void saveArrayToFile(String fname, FSJVehState[] arr) {
		try {
			FileWriter fWriter = new FileWriter(fname);
			String lsep = System.getProperty("line.separator");
		
			fWriter.append(HeaderString);
			fWriter.append(lsep);
			
			for (int i=0; i<arr.length; i++) {
				fWriter.append(arr[i].toString());
				fWriter.append(lsep);
			}
			
			fWriter.flush();
			fWriter.close();
		} catch (Exception e) {}
	}

	//Function to advance to next state
	public void updateState(float nextTimeSec, float desiredMPH, float roadGrade, float oAux, FSJHybridPowerManagerBase hybPwrMgr, float addPayloadKg) {
		switch (vehModel.general.vehPtType) {
		case bev:
			updateState_bev(nextTimeSec, desiredMPH, roadGrade, oAux, addPayloadKg);
			break;
		case cv:
			updateState_cv(nextTimeSec, desiredMPH, roadGrade, oAux, addPayloadKg);
			break;
		case hev:
		case phev:
			updateState_hev(nextTimeSec, desiredMPH, roadGrade, oAux, hybPwrMgr, addPayloadKg);
			break;
		}
	}
	
	//Hybrid or Plug-in Hybrid vehicles (including FC-HEV & FC-PHEV)
	private void updateState_hev(float nextTimeSec, float desiredMPH, float roadGrade, float oAux, FSJHybridPowerManagerBase hybPwrMgr, float addPayloadKg) {
		//Note: Fuel converter starts off at the beginning of the trip (when reset all but SOC is invoked)
		
		//Constants for calculation tuning
		float achSpeedMSStep = 0.01f;
		float zeroSpeedTol = 0.001f;
		float fuelUseTol = 0.000001f;
		float drivePowerTol = 0.00001f;
		
		//Time step
		float deltaSec = nextTimeSec - time.secSinceTripStart;
		
		//Get modifier values from add-ons and environmental conditions
		float headWindMPH = 0f;
		float hvacKW = 0f;
		float additionalAuxKW = tpt.addAux;
		float essMod = 1.0f;
		float fcMod = 1.0f;
		float airDensModifier = 1.0f;
		float driveEnergyMod = tpt.adjDEMult;
				
		float headWindMS = headWindMPH/FSJSimConstants.mphPerMps;
		float totalAuxKW = additionalAuxKW + vehModel.transmission.auxKw + oAux;
		float essChgDischgEffn = essMod*(float)Math.sqrt(vehModel.battery.essRoundTripEff);
		
		//Update time and motion objects
		time.secSinceTripStart = nextTimeSec;
		time.deltaSecFromLastState = deltaSec;
		if (time.secFuelConvOn < zeroSpeedTol) {	//Fuel converter had been off
			time.secFuelConvOff += deltaSec;
		}
		if (time.secFuelConvOff < zeroSpeedTol) {	//Fuel converter had been on
			time.secFuelConvOn += deltaSec;
		}

		motion.prevSpeedMPH = motion.achCurSpeedMPH;
		motion.prevSpeedMS = motion.achCurSpeedMS;
		motion.curRoadGrade = roadGrade;
		motion.curDesiredSpeedMPH = desiredMPH;
		
		//Target speed, constrained by tire slip limits
		float targetSpeedMS = desiredMPH/FSJSimConstants.mphPerMps;
		float maxMSIncrease = deltaSec*motion.maxAccelMS2;
		if (targetSpeedMS > motion.prevSpeedMS+maxMSIncrease) targetSpeedMS = motion.prevSpeedMS+maxMSIncrease;
		float maxMSDecrease = deltaSec*motion.maxDecelMS2;
		if (targetSpeedMS < motion.prevSpeedMS-maxMSDecrease) targetSpeedMS = motion.prevSpeedMS-maxMSDecrease;

		
		//Limit for maximum driving power output (before transmission) -- Note: not using minimum SoC as a limiter
		float maxMotorKWOut = Math.min(vehModel.motor.maxMotorKw, 
				Math.max(0, instPower.mtPowerOut)+(deltaSec*vehModel.motor.maxMotorKw/vehModel.motor.motorSecsToPeakPwr));
		float maxFCKWout = fcMod*Math.min(vehModel.fuelConv.maxFuelConvKw, 
				instPower.fcPowerOut + deltaSec*vehModel.fuelConv.maxFuelConvKw/vehModel.fuelConv.fuelConvSecsToPeakPwr);
		float maxKWFromEss = vehModel.battery.maxEssKw*essChgDischgEffn;
		float maxDriveKWpreTrnsm = 0f;

		switch (vehModel.fuelConv.fcEffType) {
		case fuelCell:	//Fuel Cell
			switch (vehModel.general.hybridDriveType) {
			case parallelWAccelAssistInChDepletion:	//Fuel cell can assist with additional power if battery kW is lower than what the motor needs
				maxKWFromEss += maxFCKWout - totalAuxKW/essChgDischgEffn;	//Total available electric power that can be directed to the motor
				maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
				maxDriveKWpreTrnsm = maxMotorKWOut;
				break;
			default:	//Serial-Equivalent drive
				if (energyUse.fuelUseSinceTripStart > fuelUseTol) {	//Fuel cell assists only if not in charge depletion mode (some fuel already used)
					maxKWFromEss += maxFCKWout - totalAuxKW/essChgDischgEffn;	//Total available electric power that can be directed to the motor
					maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
					maxDriveKWpreTrnsm = maxMotorKWOut;
				} else {
					maxKWFromEss += -totalAuxKW/essChgDischgEffn;	//Total available electric power that can be directed to the motor
					maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
					maxDriveKWpreTrnsm = maxMotorKWOut;
				}
				break;
			}
			break;
		default:	//Not Fuel Cell
			switch (vehModel.general.hybridDriveType) {
			case parallelWAccelAssistInChDepletion:
				maxKWFromEss += -totalAuxKW/essChgDischgEffn;
				if (!vehModel.battery.overrideMaxEsskw) maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
				maxDriveKWpreTrnsm = maxMotorKWOut + maxFCKWout;
				break;
			case parallelNoAccelAssistInChDepletion:
				if (energyUse.fuelUseSinceTripStart > fuelUseTol) {	//Engine assists only if not in charge depletion mode (some fuel already used)
					maxKWFromEss += -totalAuxKW/essChgDischgEffn;
					if (!vehModel.battery.overrideMaxEsskw) maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
					maxDriveKWpreTrnsm = maxMotorKWOut + maxFCKWout;
					
				} else {	//Vehicle is in charge depletion mode, engine will not assist
					if (!vehModel.battery.overrideMaxEsskw) {
						maxKWFromEss +=  -totalAuxKW/essChgDischgEffn;
						maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
						maxDriveKWpreTrnsm = maxMotorKWOut;
					}
				}
				break;
			case serial:
				if (!vehModel.battery.overrideMaxEsskw) {	//If battery capability override not in effect, adjust maximum motor power by what the battery+engine can provide
					if (energyUse.fuelUseSinceTripStart > fuelUseTol) {	//Engine assists only if not in charge depletion mode (some fuel already used)
						maxKWFromEss += maxFCKWout - totalAuxKW/essChgDischgEffn;	//Total available electric power that can be directed to the motor
					} else {	//Vehicle is in charge depletion mode, engine will not assist
						maxKWFromEss +=  -totalAuxKW/essChgDischgEffn;
					}
					maxMotorKWOut = Math.min(maxMotorKWOut, motor.outputPowerKW(maxKWFromEss));
				}
				
				maxDriveKWpreTrnsm = maxMotorKWOut;
				break;
			}
			break;		
		}
		
		//Iterate to identify achievable speed
		float achSpeedMS = targetSpeedMS;
		
		float dragPowerKW = calcDragKW(motion.prevSpeedMS+headWindMS, achSpeedMS+headWindMS,
				simConsts.airDensity*airDensModifier);
		float accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
		float ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		float rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		
		float totalReqDriveKWpreTrnsm = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW)/vehModel.transmission.transEff;

		while (totalReqDriveKWpreTrnsm > maxDriveKWpreTrnsm) {
			achSpeedMS = achSpeedMS - achSpeedMSStep;
			if (achSpeedMS < 0) {
				achSpeedMS = 0f;
				break;
			}
		
			dragPowerKW = calcDragKW(motion.prevSpeedMS+headWindMS, achSpeedMS+headWindMS,
					simConsts.airDensity*airDensModifier);
			accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
			ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			
			totalReqDriveKWpreTrnsm = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW)/vehModel.transmission.transEff;
		}
		
		//Remove effect of drag if vehicle is stopped
		if (achSpeedMS < zeroSpeedTol) {
			achSpeedMS = 0f;
			dragPowerKW = 0f;
			accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
			ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		}
		
		//Calculate power required to the wheels
		float sumKWReqToWheels = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW);
		
		//Call the Hybrid Power Management Logic to determine how much power is coming from Fuel Converter (and whether it is On) 
		hybPwrMgr.setCurState(this, achSpeedMS*FSJSimConstants.mphPerMps, sumKWReqToWheels, fcMod, essChgDischgEffn, totalAuxKW);
		float fuelConvKWOut = hybPwrMgr.fuelConvKWOut();	//This is set to be what the FC actually puts out (demand from input will be modified via fcMod)
		
		if (fuelConvKWOut > fuelUseTol) turnFuelConvOn();
		else turnFuelConvOff();
		
		//Check if breaking or driving
		float fricBreaksKW = 0f;
		float regenKW = 0f;
		float motorKWOut = 0f;
		float motorKWIn = 0f;
		float mtEffn = 0f;
		float essKWOut = 0f;	//Positive value means battery is discharging, negative means the battery is being charged
		
		if (sumKWReqToWheels > 0f) {
			//Driving
			switch (vehModel.fuelConv.fcEffType) {
			case fuelCell:	//Power to wheels comes from the motor
				motorKWOut = sumKWReqToWheels/vehModel.transmission.transEff;
				motorKWIn = motor.inputPowerKW(motorKWOut);
				if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
				
				float excessFuelCellPower = fuelConvKWOut - (motorKWIn + totalAuxKW); 	//If this has a positive value, the battery is being charged
				if (excessFuelCellPower > 0) {
					essKWOut = -excessFuelCellPower*essChgDischgEffn;	//Negative value implies battery is being charged
				} else {
					essKWOut = -excessFuelCellPower/essChgDischgEffn;	//This is a positive value (since excessFuelCellPower is negative), battery is discharging
				}					
				break;
			default:	//Not fuel cell
				float driveKWbeforeTransmission = sumKWReqToWheels/vehModel.transmission.transEff;
				float excessEnginePower = fuelConvKWOut - driveKWbeforeTransmission;	//If this has a positive value, the motor is operating as a generator and battery is being charged if the excess is more than auxillary load
				
				if (excessEnginePower > 0) {
					//motor is operating as a generator and battery (may be) getting charged
					float maxKWEssCharging = (1f/essChgDischgEffn)*Math.min(vehModel.battery.maxEssKw, 
							(vehModel.chargeControl.maxSoCBatterySwing - soc.absSoC)*vehModel.battery.maxEssKwh*3600f/deltaSec);
					float maxMechanicalRegenKWintoMotor = motor.inputPowerKW(Math.min(maxKWEssCharging, 
							vehModel.motor.maxMotorKw));
					
					motorKWIn = Math.min(excessEnginePower, maxMechanicalRegenKWintoMotor);
					motorKWOut = motor.outputPowerKW(motorKWIn);
					if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
					
					if (motorKWOut*essChgDischgEffn > totalAuxKW) {	//If the power generated by the motor is more than auxiliary load, the battery is getting charged
						//Battery is being charged
						essKWOut = -motorKWOut*essChgDischgEffn + totalAuxKW;	//negative value implies charging
					} else {
						//Battery is being discharged
						essKWOut = Math.max(0,(totalAuxKW-motorKWOut)/essChgDischgEffn);
					}
					
				} else {
					//motor is providing assist power
					motorKWOut = -excessEnginePower;
					motorKWIn = motor.inputPowerKW(motorKWOut);
					if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
					
					essKWOut = (motorKWIn + totalAuxKW)/essChgDischgEffn;
				}
				break;			
			}			
		} else if (sumKWReqToWheels < -drivePowerTol) {
			//Breaking
			float maxKWEssCharging = (1f/essChgDischgEffn)*Math.min(vehModel.battery.maxEssKw, 
					(vehModel.chargeControl.maxSoCBatterySwing - soc.absSoC)*vehModel.battery.maxEssKwh*3600f/deltaSec);	//Positive Value
			float maxMechanicalRegenKWintoMotor = motor.inputPowerKW(Math.min(maxKWEssCharging, vehModel.motor.maxMotorKw));	//Positive Value
			float percentRegen = maxContrLimPercentRegen(vehModel.transmission.maxRegen, 
					0.5f*(motion.prevSpeedMPH+achSpeedMS*FSJSimConstants.mphPerMps));		//Positive Value
			
			float breakingKW = -sumKWReqToWheels;	//Positive Value because request to wheels is negative
			float mechRegenKW = percentRegen*breakingKW*vehModel.transmission.transEff;
			mechRegenKW = Math.min(mechRegenKW, maxMechanicalRegenKWintoMotor);

			switch (vehModel.fuelConv.fcEffType) {
			case fuelCell:
				motorKWIn = mechRegenKW;
				motorKWOut = motor.outputPowerKW(motorKWIn);
				if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
				
				essKWOut = -(motorKWOut+fuelConvKWOut-totalAuxKW)*essChgDischgEffn;
				
				fricBreaksKW = breakingKW - mechRegenKW/vehModel.transmission.transEff;
				regenKW = motorKWOut;			
				break;
			default:	//Not fuel cell
				motorKWIn = mechRegenKW+fuelConvKWOut;	//Fuel converter power is always >= 0
				motorKWOut = motor.outputPowerKW(motorKWIn);
				if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
			
				essKWOut = -(motorKWOut-totalAuxKW)*essChgDischgEffn;			
				
				fricBreaksKW = breakingKW - mechRegenKW/vehModel.transmission.transEff;
				regenKW = motorKWOut*mechRegenKW/motorKWIn;			
				break;			
			}
		} else {
			//Zero drive power
			regenKW = 0f;
			
			switch (vehModel.fuelConv.fcEffType) {
			case fuelCell:
				if (fuelConvKWOut > fuelUseTol) {
					essKWOut = -(fuelConvKWOut-totalAuxKW)*essChgDischgEffn;
				} else {
					essKWOut = totalAuxKW/essChgDischgEffn;
				}				
				break;
			default: 
				if (fuelConvKWOut > fuelUseTol) {
					motorKWIn = fuelConvKWOut;	//Fuel converter power is always >= 0
					motorKWOut = motor.outputPowerKW(motorKWIn);
					if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
				
					essKWOut = -(motorKWOut-totalAuxKW)*essChgDischgEffn;			
				} else {
					essKWOut = totalAuxKW/essChgDischgEffn;
				}						
				break;
			}
		}
		
		//Calculate fuel usage
		float fcConvKWOut = 0f;
		float fcConvKWIn = 0f;
		float fcEffn = 0f;
		
		if (fuelConvKWOut > fuelUseTol) {
			fcConvKWOut = fuelConvKWOut;
			fcConvKWIn = fuelConv.inputPowerKW(fcConvKWOut/fcMod);
			fcEffn = fcConvKWOut/fcConvKWIn;	
			turnFuelConvOn();	//Resets FC tracking timers if first time turning on, otherwise has no effect 
		} else {
			turnFuelConvOff();	//Resets FC tracking timers if first time turning off, otherwise has no effect 
		}
		
		float fuelSinceLastStep = 0f;
		switch (vehModel.fuelConv.fcEffType) {
		case fuelCell:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.h2KWhPerKg);
			break;
		case diesel:
		case hybridDiesel:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.kWhPerGalDiesel);
			break;
		case cng:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.cngKWhPerM3);
			break;
		default:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.kWhPerGGE);
			break;		
		}
		
		//Update Energy use and state of charge
		energyUse.batteryKWhSinceLastState = essKWOut*deltaSec/3600f;
		energyUse.batteryKWhSinceTripStart += energyUse.batteryKWhSinceLastState;
		soc.absSoC += -energyUse.batteryKWhSinceLastState/vehModel.battery.maxEssKwh;
		soc.relSoC += -energyUse.batteryKWhSinceLastState/vehModel.batterySwingKWh();
		energyUse.fuelUseSinceLastState = fuelSinceLastStep;
		energyUse.fuelUseSinceTripStart += energyUse.fuelUseSinceLastState;
		
		//Update data records
		motion.achCurSpeedMS = achSpeedMS;
		motion.achCurSpeedMPH = motion.achCurSpeedMS*FSJSimConstants.mphPerMps;
		motion.curSpeedSlipMPH = motion.curDesiredSpeedMPH - motion.achCurSpeedMPH;
		motion.metersFromLastState = 0.5f*(motion.prevSpeedMS+motion.achCurSpeedMS)*deltaSec;
		motion.milesSinceStart += motion.metersFromLastState/FSJSimConstants.metersPerMile;
		
		instPower.accelKW = accelPowerKW;
		instPower.dragKW = dragPowerKW;
		instPower.ascentKW = ascentKW;
		instPower.rollResKW = rollResKW;
		instPower.regenKW = regenKW;
		
		instPower.auxKW = totalAuxKW-hvacKW;
		instPower.hvacKW = hvacKW;
		
		instPower.fricBrakeKW = fricBreaksKW;
		
		instPower.fcPowerIn = fcConvKWIn;
		instPower.fcPowerOut = fcConvKWOut;
		instPower.fcEffn = fcEffn;
		
		instPower.mtPowerIn = motorKWIn;
		instPower.mtPowerOut = motorKWOut;
		instPower.mtEffn = mtEffn;
		
		if (fcConvKWOut > 0) {
			energyUse.fcLoadHist.addTimeOperating(deltaSec, fcConvKWOut);
		} else {
			energyUse.fcLoadHist.addTimeNotOperating(deltaSec);
		}
	}	
	
	//Pure electric vehicles
	private void updateState_bev(float nextTimeSec, float desiredMPH, float roadGrade, float oAux, float addPayloadKg) {
		//Note: No need to ensure fuel converter off since 
		//	- Initialization (reset all but SOC is invoked) already does that
		//  - Fuel converter never gets turned on in a BEV
		
		//Constants for calculation tuning
		float achSpeedMSStep = 0.01f;
		float zeroSpeedTol = 0.001f;
		float zeroPowerTol = 0.00001f;
		
		//Time step
		float deltaSec = nextTimeSec - time.secSinceTripStart;
		
		//Get modifier values from add-ons and environmental conditions
		float headWindMPH = 0f;
		float hvacKW = 0f;
		float additionalAuxKW = tpt.addAux;
		float essMod = 1.0f;
		float airDensModifier = 1.0f;
		float driveEnergyMod = tpt.adjDEMult;
				
		float headWindMS = headWindMPH/FSJSimConstants.mphPerMps;
		float totalAuxKW = additionalAuxKW + vehModel.transmission.auxKw + oAux;
		float essChgDischgEffn = essMod*(float)Math.sqrt(vehModel.battery.essRoundTripEff);
		
		//Update time and motion objects
		time.secSinceTripStart = nextTimeSec;
		time.deltaSecFromLastState = deltaSec;
		time.secFuelConvOff += deltaSec;	//Sort of redundant info (at the end of the trip the FC will have spent the trip duration off), but included for coding consistency
		
		motion.prevSpeedMPH = motion.achCurSpeedMPH;
		motion.prevSpeedMS = motion.achCurSpeedMS;
		motion.curRoadGrade = roadGrade;
		motion.curDesiredSpeedMPH = desiredMPH;
		
		//Target speed, constrained by tire slip limits
		float targetSpeedMS = desiredMPH/FSJSimConstants.mphPerMps;
		float maxMSIncrease = deltaSec*motion.maxAccelMS2;
		if (targetSpeedMS > motion.prevSpeedMS+maxMSIncrease) targetSpeedMS = motion.prevSpeedMS+maxMSIncrease;
		float maxMSDecrease = deltaSec*motion.maxDecelMS2;
		if (targetSpeedMS < motion.prevSpeedMS-maxMSDecrease) targetSpeedMS = motion.prevSpeedMS-maxMSDecrease;
				
		//Limits on motor power
		float maxKWFromEssToMotor = vehModel.battery.maxEssKw*essChgDischgEffn - totalAuxKW;
		float maxMotorKWOut = Math.min(vehModel.motor.maxMotorKw, 
				Math.max(0, instPower.mtPowerOut)+(deltaSec*vehModel.motor.maxMotorKw/vehModel.motor.motorSecsToPeakPwr));
		if (!vehModel.battery.overrideMaxEsskw) {
			float maxMotorOutKWForMaxEssOutKW = motor.outputPowerKW(maxKWFromEssToMotor);
			maxMotorKWOut = Math.min(maxMotorKWOut, maxMotorOutKWForMaxEssOutKW);
		}
		
		//Iterate to identify achievable speed
		float achSpeedMS = targetSpeedMS;
		
		float dragPowerKW = calcDragKW(motion.prevSpeedMS+headWindMS, achSpeedMS+headWindMS,
				simConsts.airDensity*airDensModifier);
		float accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
		float ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		float rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		
		float totalReqMotorKWOut = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW)/vehModel.transmission.transEff;
		
		while (totalReqMotorKWOut > maxMotorKWOut) {
			achSpeedMS = achSpeedMS - achSpeedMSStep;
			if (achSpeedMS < 0) {
				achSpeedMS = 0f;
				break;
			}
		
			dragPowerKW = calcDragKW(motion.prevSpeedMS+headWindMS, achSpeedMS+headWindMS,
					simConsts.airDensity*airDensModifier);
			accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
			ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			
			totalReqMotorKWOut = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW)/vehModel.transmission.transEff;
		}
		
		//Remove effect of drag if vehicle is stopped
		if (achSpeedMS < zeroSpeedTol) {
			achSpeedMS = 0f;
			dragPowerKW = 0f;
			accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
			ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		}
		
		//Calculate power required to the wheels
		float sumKWReqToWheels = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW);
		
		//Check if breaking or driving
		float fricBreaksKW = 0f;
		float regenKW = 0f;
		float motorKWOut = 0f;
		float motorKWIn = 0f;
		float mtEffn = 0f;
		float essKWOut = 0f;
		
		if (sumKWReqToWheels > 0f) {
			//Driving
			motorKWOut = sumKWReqToWheels/vehModel.transmission.transEff;
			motorKWIn = motor.inputPowerKW(motorKWOut);
			if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
			essKWOut = (motorKWIn + totalAuxKW)/essChgDischgEffn;
			
		} else if (sumKWReqToWheels < -zeroPowerTol) {
			//Breaking
			float maxKWEssCharging = (1f/essChgDischgEffn)*Math.min(vehModel.battery.maxEssKw, 
					(vehModel.chargeControl.maxSoCBatterySwing - soc.absSoC)*vehModel.battery.maxEssKwh*3600f/deltaSec);	//Positive value
			float maxMechanicalRegenKWintoMotor = motor.inputPowerKW(Math.min(maxKWEssCharging, 
					vehModel.motor.maxMotorKw));	//Positive value
			float percentRegen = maxContrLimPercentRegen(vehModel.transmission.maxRegen, 
					0.5f*(motion.prevSpeedMPH+achSpeedMS*FSJSimConstants.mphPerMps));		//Positive value
			
			float breakingKW = -sumKWReqToWheels;	//Positive value because request to wheels is negative
			float mechRegenKW = percentRegen*breakingKW*vehModel.transmission.transEff;
			mechRegenKW = Math.min(mechRegenKW, maxMechanicalRegenKWintoMotor);
			
			fricBreaksKW = breakingKW - mechRegenKW/vehModel.transmission.transEff;
			regenKW = motor.outputPowerKW(mechRegenKW);
			
			motorKWIn = mechRegenKW;
			motorKWOut = regenKW;
			if (motorKWIn > 0) mtEffn = motorKWOut/motorKWIn;
		
			essKWOut = -(regenKW-totalAuxKW)*essChgDischgEffn;	
		} else {
			//Zero power for the drive
			regenKW = 0f;
			essKWOut = totalAuxKW/essChgDischgEffn;
		}
		
		//Update Energy use and state of charge
		energyUse.batteryKWhSinceLastState = essKWOut*deltaSec/3600f;
		energyUse.batteryKWhSinceTripStart += energyUse.batteryKWhSinceLastState;
		soc.absSoC += -energyUse.batteryKWhSinceLastState/vehModel.battery.maxEssKwh;
		soc.relSoC += -energyUse.batteryKWhSinceLastState/vehModel.batterySwingKWh();
		
		//Update data records
		motion.achCurSpeedMS = achSpeedMS;
		motion.achCurSpeedMPH = motion.achCurSpeedMS*FSJSimConstants.mphPerMps;
		motion.curSpeedSlipMPH = motion.curDesiredSpeedMPH - motion.achCurSpeedMPH;
		motion.metersFromLastState = 0.5f*(motion.prevSpeedMS+motion.achCurSpeedMS)*deltaSec;
		motion.milesSinceStart += motion.metersFromLastState/FSJSimConstants.metersPerMile;
		
		instPower.accelKW = accelPowerKW;
		instPower.dragKW = dragPowerKW;
		instPower.ascentKW = ascentKW;
		instPower.rollResKW = rollResKW;
		instPower.regenKW = regenKW;
		
		instPower.auxKW = totalAuxKW-hvacKW;
		instPower.hvacKW = hvacKW;
		
		instPower.fricBrakeKW = fricBreaksKW;
		
		instPower.mtPowerIn = motorKWIn;
		instPower.mtPowerOut = motorKWOut;
		instPower.mtEffn = mtEffn;
		
		energyUse.fcLoadHist.addTimeNotOperating(deltaSec);
	}	
	//Conventional vehicles
	private void updateState_cv(float nextTimeSec, float desiredMPH, float roadGrade, float oAux, float addPayloadKg) {
		//Always turn fuel converter on if first time updating state (has no effect if it's already turned on)
		turnFuelConvOn();
		
		//Constants for calculation tuning
		float achSpeedMSStep = 0.01f;
		float zeroSpeedTol = 0.001f;
		
		//Time step
		float deltaSec = nextTimeSec - time.secSinceTripStart;
		
		//Get modifier values from add-ons and environmental conditions
		float headWindMPH = 0f;
		float hvacKW = 0f;
		float additionalAuxKW = tpt.addAux;
		float fcMod = 1.0f;
		float airDensModifier = 1.0f;
		float driveEnergyMod = tpt.adjDEMult;
						
		float headWindMS = headWindMPH/FSJSimConstants.mphPerMps;
		float totalAuxKW = additionalAuxKW + vehModel.transmission.auxKw + oAux;
		
		//Update time and motion objects
		time.secSinceTripStart = nextTimeSec;
		time.deltaSecFromLastState = deltaSec;
		time.secFuelConvOn += deltaSec;	//Not so redundant info (at the end of the trip the FC will have spent the trip duration on), may be used by a Cold-start object for next trip
		
		motion.prevSpeedMPH = motion.achCurSpeedMPH;
		motion.prevSpeedMS = motion.achCurSpeedMS;
		motion.curRoadGrade = roadGrade;
		motion.curDesiredSpeedMPH = desiredMPH;
		
		//Target speed, constrained by tire slip limits
		float targetSpeedMS = desiredMPH/FSJSimConstants.mphPerMps;
		float maxMSIncrease = deltaSec*motion.maxAccelMS2;
		if (targetSpeedMS > motion.prevSpeedMS+maxMSIncrease) targetSpeedMS = motion.prevSpeedMS+maxMSIncrease;
		float maxMSDecrease = deltaSec*motion.maxDecelMS2;
		if (targetSpeedMS < motion.prevSpeedMS-maxMSDecrease) targetSpeedMS = motion.prevSpeedMS-maxMSDecrease;
		
		//Maximum fuel converter power
		float maxFCKWout = fcMod*Math.min(vehModel.fuelConv.maxFuelConvKw, 
				instPower.fcPowerOut + deltaSec*vehModel.fuelConv.maxFuelConvKw/vehModel.fuelConv.fuelConvSecsToPeakPwr);
		
		//Iterate to identify achievable speed
		float achSpeedMS = targetSpeedMS;
		
		float dragPowerKW = calcDragKW(motion.prevSpeedMS+headWindMS, achSpeedMS+headWindMS,
				simConsts.airDensity*airDensModifier);
		float accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
		float ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		float rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		
		float totalReqFuelConvKW = totalAuxKW + driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW)/vehModel.transmission.transEff;
		
		while (totalReqFuelConvKW > maxFCKWout) {
			achSpeedMS = achSpeedMS - achSpeedMSStep;
			if (achSpeedMS < 0) {
				achSpeedMS = 0f;
				break;
			}
		
			dragPowerKW = calcDragKW(motion.prevSpeedMS+headWindMS, achSpeedMS+headWindMS,
					simConsts.airDensity*airDensModifier);
			accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
			ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			
			totalReqFuelConvKW = totalAuxKW + driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW)/vehModel.transmission.transEff;
		}
		
		//Remove effect of drag if vehicle is stopped
		if (achSpeedMS < zeroSpeedTol) {
			achSpeedMS = 0f;
			dragPowerKW = 0f;
			accelPowerKW = calcAccelAndInertiaKW(motion.prevSpeedMS, achSpeedMS, deltaSec, addPayloadKg);
			ascentKW = calcAscentKW(roadGrade, motion.prevSpeedMS, achSpeedMS, addPayloadKg);
			rollResKW = calcRollResKW(motion.prevSpeedMS, achSpeedMS, addPayloadKg);
		}
		
		//Calculate power required from the engine and/or breaks
		float sumKWReqToWheel = driveEnergyMod*(dragPowerKW+accelPowerKW+ascentKW+rollResKW);
		float fricBreaksKW = 0f;
		if (sumKWReqToWheel < 0) {
			fricBreaksKW = -sumKWReqToWheel;
			totalReqFuelConvKW = totalAuxKW;
		} else {
			totalReqFuelConvKW = totalAuxKW + sumKWReqToWheel/vehModel.transmission.transEff;
		}
		
		//Calculate required input power, efficiency and fuel amount
		float fcConvKWIn = fuelConv.inputPowerKW(totalReqFuelConvKW/fcMod);
		float fcEffn = totalReqFuelConvKW/fcConvKWIn;
		float fuelSinceLastStep = 0f;
		switch (vehModel.fuelConv.fcEffType) {
		case diesel:
		case hybridDiesel:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.kWhPerGalDiesel);
			break;
		case cng:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.cngKWhPerM3);
			break;
		default:
			fuelSinceLastStep = fcConvKWIn*deltaSec/(3600f*simConsts.kWhPerGGE);
			break;		
		}
		
		//Update data objects
		motion.achCurSpeedMS = achSpeedMS;
		motion.achCurSpeedMPH = motion.achCurSpeedMS*FSJSimConstants.mphPerMps;
		motion.curSpeedSlipMPH = motion.curDesiredSpeedMPH - motion.achCurSpeedMPH;
		motion.metersFromLastState = 0.5f*(motion.prevSpeedMS+motion.achCurSpeedMS)*deltaSec;
		motion.milesSinceStart += motion.metersFromLastState/FSJSimConstants.metersPerMile;
		
		energyUse.fuelUseSinceLastState = fuelSinceLastStep;
		energyUse.fuelUseSinceTripStart += energyUse.fuelUseSinceLastState;
		
		instPower.accelKW = accelPowerKW;
		instPower.dragKW = dragPowerKW;
		instPower.ascentKW = ascentKW;
		instPower.rollResKW = rollResKW;
		
		instPower.auxKW = totalAuxKW-hvacKW;
		instPower.hvacKW = hvacKW;
		
		instPower.fricBrakeKW = fricBreaksKW;
		
		instPower.fcPowerOut = totalReqFuelConvKW;
		instPower.fcPowerIn = fcConvKWIn;
		instPower.fcEffn = fcEffn;
		
		energyUse.fcLoadHist.addTimeOperating(deltaSec, totalReqFuelConvKW);
	}
	
	//Function for calculation of percentage regenerative breaking
	private static float maxContrLimPercentRegen(float maxRegenFrac, float mph) {
		float a = 500f;
		float b = 0.99f;
		return maxRegenFrac/(1f+a*(float)Math.exp(-b*(1f+mph)));
	}

	//Function for calculating drag power
	private float calcDragKW(float prevNetSpeedMS, float netSpeedMS, float airDensity) {
		float avSpeed = 0.5f*(prevNetSpeedMS + netSpeedMS);
		return 0.5f*airDensity*vehModel.general.dragCoef*vehModel.general.frontalAreaM2
				*avSpeed*avSpeed*avSpeed/1000f;
	}
	//Function for calculating acceleration and wheel inertia power
	private float calcAccelAndInertiaKW(float prevSpeedMS, float targetSpeedMS, float deltaSec, float addPayloadKg) {
		float curWheelRadPS = prevSpeedMS/vehModel.wheels.wheelRadiusM;
		float reqWheelRadPS = targetSpeedMS/vehModel.wheels.wheelRadiusM;
		float inertiaKW = 0.5f*vehModel.massProp.allWheelsKgM2*
				(reqWheelRadPS*reqWheelRadPS - curWheelRadPS*curWheelRadPS)/(deltaSec*1000f);
		
		float accelKW = 0.5f*(vehModel.massProp.totalKg + tpt.addMass + addPayloadKg)*
				(targetSpeedMS*targetSpeedMS - prevSpeedMS*prevSpeedMS)/(deltaSec*1000f);
		return inertiaKW+accelKW;
	}
	//Function for calculating road grade power
	private float calcAscentKW(float roadGrade, float prevSpeedMS, float targetSpeedMS, float addPayloadKg) {
		float avSpeed = 0.5f*(prevSpeedMS+targetSpeedMS);
		return (vehModel.massProp.totalKg + tpt.addMass + addPayloadKg)*simConsts.gravity*(float)Math.sin(Math.atan(roadGrade))*avSpeed/1000f;
	}
	//Function for calculating tire rolling resistance
	private float calcRollResKW(float prevSpeedMS, float targetSpeedMS, float addPayloadKg) {
		float avSpeed = 0.5f*(prevSpeedMS+targetSpeedMS);
		return simConsts.gravity*(vehModel.massProp.totalKg + tpt.addMass + addPayloadKg)*vehModel.wheels.wheelRrCoeff*avSpeed/1000f;
	}
	
	/*
	//Function for calculating air density modifier
	private float calcAirDensModifier(FSJTimeIntrpltTripInfo atmPresBar, FSJTimeIntrpltTripInfo ambTempCelsius, float timeSec) {
		if ((atmPresBar==null)&&(ambTempCelsius==null)) return 1.0f;
		
		float kelvinTempOffset = 273.15f;
		float p1bar = simConsts.refAtmPressureBar;
		float p2bar = p1bar;
		float t1k = kelvinTempOffset + simConsts.refAmbTempC;
		float t2k = t1k;
		
		if (atmPresBar!=null) p2bar = atmPresBar.yValue(timeSec);
		if (ambTempCelsius!=null) t2k = kelvinTempOffset + ambTempCelsius.yValue(timeSec);
		
		return (p2bar*t1k)/(p1bar*t2k);
	}*/
}
