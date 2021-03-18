package fastsimjava;

import fastsimjava.components.FSJSimConstants;

public class FSJVehModelParam {
	
	//Constants
	public static final String Header_GeneralVehParam = "name,vehPtType,dragCoef,frontalAreaM2,gliderKg,vehCgM,driveAxleWeightFrac,wheelBaseM,cargoKg,hybridDriveType";
	public static final String Header_FuelStore = "fuelStorKwh,fuelStorKwhPerKg";
	public static final String Header_FuelConverter = "maxFuelConvKw,fcEffType,fuelConvSecsToPeakPwr,fuelConvBaseKg,fuelConvKwPerKg";
	public static final String Header_Motor = "maxMotorKw,motorPeakEff,motorSecsToPeakPwr,mcPeKgPerKw,mcPeBaseKg";
	public static final String Header_Battery = "maxEssKw,specialFlag,maxEssKwh,essKgPerKwh,essBaseKg,essRoundTripEff,essLifeCoeffA,essLifeCoeffB";
	//Note specialFlag replaces overrideMaxEsskw... if value *strictly less* than 0, then:
	//	int curveIDsCode = -specialFlag
	//	int fuelConverterCurveID = (curveIDsCode % 1000) - 1
	//	int motorCurveID = (curveIDsCode / 1000) - 1
	// negative value in fuelConverterCurveID or motorCurveID implies no external curve in use
	public static final String Header_Wheels = "wheelInteriaKgM2,numWheels,wheelRrCoeff,wheelRadiusM,wheelCoeffOfFric";
	public static final String Header_ChargeControl = "minSoCBatterySwing,maxSoCBatterySwing,essDischargeEffortMxFCEff,essChargeEffortMxFCEff,mphESSAccRsrvZero,percentESSReserveForAccel,percHighAccessoryBuff,mphFcOn,kwDemandFcOn,forceAuxOnFC";
	public static final String Header_Transmission = "altEff,chgEff,auxKw,maxRegen,transKg,transEff";
	public static final String Header_CompMassMult = "compMassMultiplier";
	public static final String HeaderString = Header_GeneralVehParam + "," + Header_FuelStore + "," + Header_FuelConverter + "," + Header_Motor + "," +
			Header_Battery + "," + Header_Wheels + "," + Header_ChargeControl + "," + Header_Transmission + "," + Header_CompMassMult;
	
	
	
	//General Vehicle Parameters
	public GeneralVehParam general;
	//Parameters for fuel storage tank
	public FuelStore fuelStore;
	//Parameters for fuel converter
	public FuelConverterParam fuelConv;
	//Parameters for the motor
	public MotorParam motor;
	//Parameters for the Battery
	public BatteryParam battery;
	//Parameters for the wheels
	public WheelsParam wheels;
	//Parameters for charge control
	public ChargeControlParam chargeControl;
	//Parameters for transmission
	public TransmissionParam transmission;
	//Component mass multiplier
	public float compMassMultiplier;
	
	
	//Summary of mass properties
	public MassProp massProp;
	
	//Quick access function to check if current vehicle model is BEV or PHEV
	public boolean isPlugin() {
		switch (general.vehPtType) {
		case bev:
		case phev:
			return true;
		default:
			return false;
		}
	}
	//Quick access function to calculate battery swing in kWh (for HEVs, PHEVs & BEVs only)
	public float batterySwingKWh() {
		switch (general.vehPtType) {
		case hev:
		case bev:
		case phev:
			return battery.maxEssKwh*(chargeControl.maxSoCBatterySwing-chargeControl.minSoCBatterySwing);
		default:
			return 0f;
		}
	}
	//Quick access function for estimating equivalent DynamometerCoefficient A
	public float dynoCoeffA() {
		FSJSimConstants fsc = new FSJSimConstants();
		return massProp.totalKg * fsc.gravity * wheels.wheelRrCoeff;
	}
	//Quick access function for estimating equivalent DynamometerCoefficient C
	public float dynoCoeffC() {
		FSJSimConstants fsc = new FSJSimConstants();
		return 0.5f * fsc.airDensity * general.frontalAreaM2 * general.dragCoef;
	}
	
	//Utility Function for DynamometerData String Summary
	public String dynoDataStringSummary() {
		String st = general.name + "," + massProp.totalKg + "," + dynoCoeffA() + "," + dynoCoeffC();
		return st;
	}
	
	
	//Constructor via reading line-string
	public FSJVehModelParam(String readLine) {
		//Call function for parsing CSV line
		parseCSVLine(readLine);
		
		//Calculate component masses
		massProp = new MassProp();
	}
	//Copy constructor
	public FSJVehModelParam(FSJVehModelParam other) {
		//Use copy constructor of data classes
		general = new GeneralVehParam(other.general);
		fuelStore = new FuelStore(other.fuelStore);
		fuelConv = new FuelConverterParam(other.fuelConv);
		motor = new MotorParam(other.motor);
		battery = new BatteryParam(other.battery);
		wheels = new WheelsParam(other.wheels);
		chargeControl = new ChargeControlParam(other.chargeControl);
		transmission = new TransmissionParam(other.transmission);
		compMassMultiplier = other.compMassMultiplier;
		
		//Calculate mass of components
		massProp = new MassProp();
	}
	
		
	//Sub-class for general vehicle parameters
	public class GeneralVehParam {
		//Name tag for vehicle model
		public String name;
		//Type of power train (CV, HEV, PHEV, BEV)
		public VehicleDriveTrainType vehPtType;
		//Air drag coefficient
		public float dragCoef;
		//Front projected area of vehicle
		public float frontalAreaM2;
		//Mass of glider
		public float gliderKg;
		//CG relative position between wheel base
		public float vehCgM;
		//Fraction of weight on drive axle
		public float driveAxleWeightFrac;
		//Wheel base in meters
		public float wheelBaseM;
		//Cargo weight
		public float cargoKg;
		//Hybrid power train arrangement
		public HybridDriveType hybridDriveType;
		
		//Constructors
		private GeneralVehParam(GeneralVehParam other) {
			name = other.name;
			vehPtType = other.vehPtType;
			dragCoef = other.dragCoef;
			frontalAreaM2 = other.frontalAreaM2;
			gliderKg = other.gliderKg;
			vehCgM = other.vehCgM;
			driveAxleWeightFrac = other.driveAxleWeightFrac;
			wheelBaseM = other.wheelBaseM;
			cargoKg = other.cargoKg;
			hybridDriveType = other.hybridDriveType;
		}
		private GeneralVehParam(String[] strSplit) {
			name = strSplit[0];
			vehPtType = VehicleDriveTrainType.decode(strSplit[1]);
			

			dragCoef = Float.parseFloat(strSplit[2]);
			frontalAreaM2 = Float.parseFloat(strSplit[3]);
			gliderKg = Float.parseFloat(strSplit[4]);
			
			vehCgM = Float.parseFloat(strSplit[5]);
			driveAxleWeightFrac = Float.parseFloat(strSplit[6]);
			wheelBaseM = Float.parseFloat(strSplit[7]);
			cargoKg = Float.parseFloat(strSplit[8]);
			
			hybridDriveType = HybridDriveType.decode(strSplit[9]);
		}
		
		//Line String output
		@Override public String toString() {
			return name+","+vehPtType.codeValue+","+dragCoef+","+frontalAreaM2+","+gliderKg+","+
					vehCgM+","+driveAxleWeightFrac+","+wheelBaseM+","+cargoKg+","+hybridDriveType.codeValue;
		}
	}
	//Enum for type of power train
	public enum VehicleDriveTrainType {
		cv(1),
		hev(2),
		phev(3),
		bev(4),
		;
		public int codeValue;
		private VehicleDriveTrainType(int intValue) {
			codeValue = intValue;
		}	
		public static VehicleDriveTrainType decode(String sValue) {
			return decode(Integer.parseInt(sValue));
		}
		public static VehicleDriveTrainType decode(int intValue) {
			switch (intValue) {
			case 1:
				return VehicleDriveTrainType.cv;
			case 2:
				return VehicleDriveTrainType.hev;
			case 3:
				return VehicleDriveTrainType.phev;
			case 4:
				return VehicleDriveTrainType.bev;
			default:
				return VehicleDriveTrainType.cv;
			}
		}
	}
	//Enum for hybrid drive type
	public enum HybridDriveType {
		serial(1),					//No direct drive between fuel converter and wheels 
		parallelWAccelAssistInChDepletion(3),	//Default model for split parallel drive, where the fuel converter can come on to assist with acceleration in parallel with the motor
		parallelNoAccelAssistInChDepletion(2),	//Direct drive between fuel converter and wheels exist, but does not turn on for acceleration assist while in charge depletion mode
		;
		public int codeValue;
		private HybridDriveType(int intValue) {
			codeValue = intValue;
		}	
		public static HybridDriveType decode(String sValue) {
			return decode(Integer.parseInt(sValue));
		}
		public static HybridDriveType decode(int intValue) {
			switch (intValue) {
			case 1:
				return HybridDriveType.serial;
			case 2:
				return HybridDriveType.parallelNoAccelAssistInChDepletion;
			case 3:
				return HybridDriveType.parallelWAccelAssistInChDepletion;
			default:
				return HybridDriveType.parallelWAccelAssistInChDepletion;
			}
		}
	}

	//Sub-class for fuel store
	public class FuelStore {
		public float fuelStorKwh;
		public float fuelStorKwhPerKg;
		
		private FuelStore(FuelStore other) {
			fuelStorKwh = other.fuelStorKwh;
			fuelStorKwhPerKg = other.fuelStorKwhPerKg;
		}
		private FuelStore(String[] strSplit) {
			fuelStorKwh = Float.parseFloat(strSplit[10]);
			fuelStorKwhPerKg = Float.parseFloat(strSplit[11]);
		}
		
		@Override public String toString() {
			return ""+fuelStorKwh+","+fuelStorKwhPerKg;
		}
	}
		
	//Sub-class for fuel converter parameters
	public class FuelConverterParam {
		//Maximum power
		public float maxFuelConvKw;
		//Type
		public FuelConverterEffType fcEffType;
		//Time to reach maximum power
		public float fuelConvSecsToPeakPwr;
		//Base Weight
		public float fuelConvBaseKg;
		//Linear coefficient of weight
		public float fuelConvKwPerKg;
		
		private FuelConverterParam(FuelConverterParam other) {
			maxFuelConvKw = other.maxFuelConvKw;
			fcEffType = other.fcEffType;
			fuelConvSecsToPeakPwr = other.fuelConvSecsToPeakPwr;
			fuelConvBaseKg = other.fuelConvBaseKg;
			fuelConvKwPerKg = other.fuelConvKwPerKg;
		}
		private FuelConverterParam(String[] strSplit) {
			maxFuelConvKw = Float.parseFloat(strSplit[12]);
			fcEffType =  FuelConverterEffType.decode(strSplit[13]);
			fuelConvSecsToPeakPwr = Float.parseFloat(strSplit[14]);
			fuelConvBaseKg = Float.parseFloat(strSplit[15]);
			fuelConvKwPerKg = Float.parseFloat(strSplit[16]);				
		}
		
		@Override public String toString() {
			return ""+maxFuelConvKw+","+fcEffType.codeValue+","+fuelConvSecsToPeakPwr+","+
					fuelConvBaseKg+","+fuelConvKwPerKg;
		}
	}
	//Enum for fuel converter type
	public enum FuelConverterEffType {
		//2018 Models only
		sparkIgnition(1),
		atkins(2),
		diesel(3),
		fuelCell(4),
		hybridDiesel(5),
		cng(6),
		;
		public int codeValue;
		private FuelConverterEffType(int intValue) {
			codeValue = intValue;
		}	
		public static FuelConverterEffType decode(String sValue) {
			return decode(Integer.parseInt(sValue));
		}
		public static FuelConverterEffType decode(int intValue) {
			FuelConverterEffType rtrnValue = null;
			switch (intValue) {
			case 1:
				rtrnValue = FuelConverterEffType.sparkIgnition;
				break;
			case 2:
				rtrnValue = FuelConverterEffType.atkins;
				break;
			case 3:
				rtrnValue = FuelConverterEffType.diesel;
				break;
			case 4:
				rtrnValue = FuelConverterEffType.fuelCell;
				break;
			case 5:
				rtrnValue = FuelConverterEffType.hybridDiesel;
				break;
			case 6:
				rtrnValue = FuelConverterEffType.cng;
				break;
			}
			
			return rtrnValue;
		}
	}
	
	//Sub-class for motor parameters
	public class MotorParam {
		//Maximum power (in kW) of electric motor
		public float maxMotorKw;
		//Peak efficiency of electric motor -- IMPORTANT NOTE: use negative of the value to invoke 2012 motor model
		public float motorPeakEff;
		//Time for electric motor to reach maximum power
		public float motorSecsToPeakPwr;
		//Unit weight (per kW) of electric motor
		public float mcPeKgPerKw;
		//Base weight of electric motor
		public float mcPeBaseKg;

		private MotorParam(MotorParam other) {
			maxMotorKw = other.maxMotorKw;
			motorPeakEff = other.motorPeakEff;
			motorSecsToPeakPwr = other.motorSecsToPeakPwr;
			mcPeKgPerKw = other.mcPeKgPerKw;
			mcPeBaseKg = other.mcPeBaseKg;
		}
		private MotorParam(String[] strSplit) {
			maxMotorKw = Float.parseFloat(strSplit[17]);
			motorPeakEff = Float.parseFloat(strSplit[18]);
			motorSecsToPeakPwr = Float.parseFloat(strSplit[19]);				
			mcPeKgPerKw = Float.parseFloat(strSplit[20]);
			mcPeBaseKg = Float.parseFloat(strSplit[21]);
		}
		
		@Override public String toString() {
			return ""+maxMotorKw+","+motorPeakEff+","+motorSecsToPeakPwr+","+mcPeKgPerKw+","+mcPeBaseKg;
		}
	}

	//Sub-class for battery parameters
	public class BatteryParam {
		//Maximum battery power in kW
		public float maxEssKw;
		
		//Special Flag
		private int specialFlag;
		//Override flag for battery KW limit
		public boolean overrideMaxEsskw;
		//Special CurveID for Fuel Converter
		public int fcCCurveID;
		//Special CurveID for Motor
		public int mtCCurveID;
		
		//Rated battery energy storage in kWh
		public float maxEssKwh;
		//Unit weight (per kW) of battery
		public float essKgPerKwh;
		//Base weight of battery
		public float essBaseKg;
		//"Round-trip" (i.e. charge and discharge) efficiency of battery 
		public float essRoundTripEff;
		//Coefficient A for battery life
		public float essLifeCoeffA;
		//Coefficient A for battery life
		public float essLifeCoeffB;

		
		private BatteryParam(BatteryParam other) {
			maxEssKw = other.maxEssKw;
			overrideMaxEsskw = other.overrideMaxEsskw;
			maxEssKwh = other.maxEssKwh;
			essKgPerKwh = other.essKgPerKwh;
			essBaseKg = other.essBaseKg;
			essRoundTripEff = other.essRoundTripEff;
			essLifeCoeffA = other.essLifeCoeffA;
			essLifeCoeffB = other.essLifeCoeffB;
		}
		private BatteryParam(String[] strSplit) {
			maxEssKw = Float.parseFloat(strSplit[22]);
			
			specialFlag = Integer.parseInt(strSplit[23]);			
			overrideMaxEsskw = false;
			fcCCurveID = -1;
			mtCCurveID = -1;
			
			if (specialFlag > 0) {
				overrideMaxEsskw = true;
			} else if (specialFlag < 0) {
				int curveIDsCode = -specialFlag;
				fcCCurveID = (curveIDsCode % 1000) - 1;
				mtCCurveID = (curveIDsCode / 1000) - 1;
			}
			
			maxEssKwh = Float.parseFloat(strSplit[24]);
			essKgPerKwh = Float.parseFloat(strSplit[25]);		
			essBaseKg = Float.parseFloat(strSplit[26]);
			essRoundTripEff = Float.parseFloat(strSplit[27]);
			essLifeCoeffA = Float.parseFloat(strSplit[28]);
			essLifeCoeffB = Float.parseFloat(strSplit[29]);
		}
		
		public void reEncodeSpecialFlag(int sfValue) {//Does NOT re-adjust curve IDs, use cautiously
			specialFlag = sfValue;
		}
		
		@Override public String toString() {
			String strOut = ""+maxEssKw+","+specialFlag+","+maxEssKwh+","+essKgPerKwh+","+essBaseKg+","+
					essRoundTripEff+","+essLifeCoeffA+","+essLifeCoeffB;
			return strOut;
		}
	}
		
	//Sub-class for wheels' parameters
	public class WheelsParam {
		//Wheel inertia in kg.m^2
		public float wheelInteriaKgM2;
		//Number of wheels
		public float numWheels;
		//Wheel rolling resistance coefficient
		public float wheelRrCoeff;
		//Wheel radius in meters
		public float wheelRadiusM;
		//Wheel coefficient of friction
		public float wheelCoeffOfFric;

		
		public WheelsParam(WheelsParam other) {
			wheelInteriaKgM2 = other.wheelInteriaKgM2;
			numWheels = other.numWheels;
			wheelRrCoeff = other.wheelRrCoeff;
			wheelRadiusM = other.wheelRadiusM;
			wheelCoeffOfFric = other.wheelCoeffOfFric;
		}
		public WheelsParam(String[] strSplit) {
			wheelInteriaKgM2 = Float.parseFloat(strSplit[30]);		
			numWheels = Float.parseFloat(strSplit[31]);
			wheelRrCoeff = Float.parseFloat(strSplit[32]);
			wheelRadiusM = Float.parseFloat(strSplit[33]);
			wheelCoeffOfFric = Float.parseFloat(strSplit[34]);
		}

		@Override public String toString() {
			return ""+wheelInteriaKgM2+","+numWheels+","+wheelRrCoeff+","+wheelRadiusM+","+wheelCoeffOfFric;
		}
	}
	
	//Sub-class for charge control parameters
	public class ChargeControlParam {
		//Minimum battery swing (as fraction of rated kWh)
		public float minSoCBatterySwing;
		//Maximum battery swing (as fraction of rated kWh)
		public float maxSoCBatterySwing;
		//ESS discharge effort toward max FC efficiency (percent)
		public float essDischargeEffortMxFCEff;
		//ESS charge effort toward max FC efficiency (percent)
		public float essChargeEffortMxFCEff;
		//Speed where the battery reserved for accelerating is zero
		public float mphESSAccRsrvZero;
		//Percent of usable battery energy reserved to help accelerate
		public float percentESSReserveForAccel;
		//Percent SOC buffer for high accessory loads during cycles with long idle time
		public float percHighAccessoryBuff;
		//Speed at which engine is commanded on (mph)
		public float mphFcOn;
		//Power demand at which engine is commanded on (kW)
		public float kwDemandFcOn;
		//Flag for forcing Aux on FC
		public boolean forceAuxOnFC;

		//Local Constructors
		private ChargeControlParam(ChargeControlParam other) {
			minSoCBatterySwing = other.minSoCBatterySwing;
			maxSoCBatterySwing = other.maxSoCBatterySwing;
			essDischargeEffortMxFCEff = other.essDischargeEffortMxFCEff;
			essChargeEffortMxFCEff = other.essChargeEffortMxFCEff;
			mphESSAccRsrvZero = other.mphESSAccRsrvZero;
			percentESSReserveForAccel = other.percentESSReserveForAccel;
			percHighAccessoryBuff = other.percHighAccessoryBuff;
			mphFcOn = other.mphFcOn;
			kwDemandFcOn = other.kwDemandFcOn;
			forceAuxOnFC = other.forceAuxOnFC;
		}
		private ChargeControlParam(String[] strSplit) {
			minSoCBatterySwing = Float.parseFloat(strSplit[35]);
			maxSoCBatterySwing = Float.parseFloat(strSplit[36]);
			essDischargeEffortMxFCEff = Float.parseFloat(strSplit[37]);
			essChargeEffortMxFCEff = Float.parseFloat(strSplit[38]);
			mphESSAccRsrvZero = Float.parseFloat(strSplit[39]);
			percentESSReserveForAccel = Float.parseFloat(strSplit[40]);
			percHighAccessoryBuff = Float.parseFloat(strSplit[41]);
			mphFcOn = Float.parseFloat(strSplit[42]);	
			kwDemandFcOn = Float.parseFloat(strSplit[43]);
			forceAuxOnFC = false;
			if (Float.parseFloat(strSplit[44])!=0) forceAuxOnFC = true;
		}	
		
		@Override public String toString() {
			String strOut = ""+minSoCBatterySwing+","+maxSoCBatterySwing+","+essDischargeEffortMxFCEff+","+
					essChargeEffortMxFCEff+","+mphESSAccRsrvZero+","+percentESSReserveForAccel+","+
					percHighAccessoryBuff+","+mphFcOn+","+kwDemandFcOn+",";
			if (forceAuxOnFC) strOut = strOut + "1";
			else strOut =strOut + "0";
			return strOut;
		}
	}

	//Sub-class for transmission parameters
	public class TransmissionParam {
		//Alternator efficiency
		public float altEff;
		//Charging efficiency
		public float chgEff;
		//Base auxiliary power
		public float auxKw;
		//Max regeneration (regenerative breaking)
		public float maxRegen;
		//Transmission mass
		public float transKg;
		//Transmission efficiency
		public float transEff;
		
		//Local Constructors
		private TransmissionParam(TransmissionParam other) {
			altEff = other.altEff;
			chgEff = other.chgEff;
			auxKw = other.auxKw;
			maxRegen = other.maxRegen;
			transKg = other.transKg;
			transEff = other.transEff;
		}
		private TransmissionParam(String[] strSplit) {
			altEff = Float.parseFloat(strSplit[45]);
			chgEff = Float.parseFloat(strSplit[46]);
			auxKw = Float.parseFloat(strSplit[47]);
			maxRegen = Float.parseFloat(strSplit[48]);
			transKg = Float.parseFloat(strSplit[49]);		
			transEff = Float.parseFloat(strSplit[50]);
		}
		
		@Override public String toString() {
			return ""+altEff+","+chgEff+","+auxKw+","+maxRegen+","+transKg+","+transEff;
		}
	}

	//Sub-class for summary of mass properties
	public class MassProp {
		public float allWheelsKgM2;
		
		public float gliderKg;
		public float cargoKg;
		public float fuelStoreKg;
		public float fuelConvKg;
		public float motorKg;
		public float batteryKg;
		public float transmissionKg;
		
		public float totalKg;
		public float motorKWtoTotalKg;
		
		private MassProp() {
			allWheelsKgM2 = wheels.numWheels * wheels.wheelInteriaKgM2;
			
			gliderKg = general.gliderKg;
			cargoKg = general.cargoKg;
			transmissionKg = compMassMultiplier*transmission.transKg;
			
			fuelStoreKg = 0f;
			fuelConvKg = 0f;
			motorKg = 0f;
			batteryKg = 0f;
			
			switch (general.vehPtType) {
			case bev:
				motorKg = compMassMultiplier*(motor.mcPeBaseKg + motor.mcPeKgPerKw*motor.maxMotorKw);
				batteryKg = compMassMultiplier*(battery.essBaseKg + battery.essKgPerKwh*battery.maxEssKwh);
				break;
			case cv:
				fuelStoreKg = compMassMultiplier*(fuelStore.fuelStorKwh/fuelStore.fuelStorKwhPerKg);
				fuelConvKg = compMassMultiplier*(fuelConv.fuelConvBaseKg + fuelConv.maxFuelConvKw/fuelConv.fuelConvKwPerKg);
				break;
			case hev:
			case phev:
				fuelStoreKg = compMassMultiplier*(fuelStore.fuelStorKwh/fuelStore.fuelStorKwhPerKg);
				fuelConvKg = compMassMultiplier*(fuelConv.fuelConvBaseKg + fuelConv.maxFuelConvKw/fuelConv.fuelConvKwPerKg);
				motorKg = compMassMultiplier*(motor.mcPeBaseKg + motor.mcPeKgPerKw*motor.maxMotorKw);
				batteryKg = compMassMultiplier*(battery.essBaseKg + battery.essKgPerKwh*battery.maxEssKwh);
				break;
			}
			
			totalKg = gliderKg+cargoKg+transmissionKg+fuelStoreKg+fuelConvKg+motorKg+batteryKg;
			if (general.vehPtType == VehicleDriveTrainType.cv) motorKWtoTotalKg = 0f;
			else motorKWtoTotalKg = motor.maxMotorKw/totalKg;
		}
	}
	
	//Function to re-adjust the component mass multiplier while maintaining total mass via re-adjusting the glider mass
	public void reAdjustCompMass(float newCompMassMultiplier) {
		float oldTotalKg = massProp.totalKg;
		
		compMassMultiplier = newCompMassMultiplier;
		massProp = new MassProp();
		
		float newTotalKg = massProp.totalKg;
		general.gliderKg += oldTotalKg - newTotalKg;
		
		massProp = new MassProp();
	}

	//Functions for parsing CSV line
	private void parseCSVLine(String readLine) {
		//Split the line at commas
		String[] strSplit = readLine.split(",");

		general = new GeneralVehParam(strSplit);
		fuelStore = new FuelStore(strSplit);
		fuelConv = new FuelConverterParam(strSplit);
		
		motor = new MotorParam(strSplit);
		battery = new BatteryParam(strSplit);
		wheels = new WheelsParam(strSplit);
		
		chargeControl = new ChargeControlParam(strSplit);
		
		transmission = new TransmissionParam(strSplit);
		compMassMultiplier = Float.parseFloat(strSplit[51]);
	}


	//Output to a line string
	@Override public String toString() {
		return general.toString()+","+fuelStore.toString()+","+fuelConv.toString()
				+","+motor.toString()+","+battery.toString()+","+wheels.toString()
				+","+chargeControl.toString()+","+transmission.toString()+","+compMassMultiplier;
	}
	
	//Vertical mode (array of strings) Output
	public String[] toString_multiLine() {
		String[] spValues = toString().split(",");
		String[] spHeader = HeaderString.split(",");
		String[] lines = new String[spValues.length];
		
		for (int i=0; i<lines.length; i++) {
			lines[i] = spHeader[i] + "," + spValues[i];
		}
		return lines;
	}
}
