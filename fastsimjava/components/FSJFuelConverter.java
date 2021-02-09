package fastsimjava.components;

import fastsimjava.*;

public class FSJFuelConverter {
	//Constant for fine-grid array of kWIn points
	private static final int NumPointsForKWOut = 101;
	
	//Maximum output power
	private float maxOutPowerKW;
	public float maxOutPowerKW() {return maxOutPowerKW;}
	
	//Power at maximum efficiency
	private float powerAtMaxEffKW;
	public float powerAtMaxEffKW() {return powerAtMaxEffKW;}
	
	//Array of values for the input kW to achieve a desired output kW
	private float[] kWInValues;

	//Function for calculating the input power corresponding to a desired output power
	public float inputPowerKW(float outPowerKW) {	
		float deltaPowerOutKW = maxOutPowerKW/(float)(NumPointsForKWOut-1);
		int arrayID = (int)(outPowerKW/deltaPowerOutKW);
		
		if (arrayID < 0) return kWInValues[0];
		if (arrayID >= (NumPointsForKWOut-1)) return kWInValues[NumPointsForKWOut-1];
		
		float y1 = kWInValues[arrayID];
		float y2 = kWInValues[arrayID+1];
		float c2 = (outPowerKW - arrayID*deltaPowerOutKW)/deltaPowerOutKW;
		float c1 = 1f - c2;
		
		return c1*y1 + c2*y2;
	}	

	//Constructor
	public FSJFuelConverter(FSJVehModelParam vehModelParam, FSJEffCurvesManager curveMan) {
		//Set maximum output power
		maxOutPowerKW = vehModelParam.fuelConv.maxFuelConvKw;

		//Attempt to obtain a custom curve
		PieceWiseLinearEfficiencyCurve customCurve = curveMan.getFCCurve(vehModelParam.battery.fcCCurveID);
		
		//If no custom curve available, then use default
		if (customCurve == null) {
			defaultInit(vehModelParam.fuelConv.fcEffType);
			return;
		}
		
		//Initialization via custom curve
		float[] powX = new float[customCurve.fracOfMaxPower.length];
		float[] powY = new float[powX.length];
		
		float bestEff = 0f; 
		
		for (int i=1; i<powX.length; i++) {
			powX[i] = customCurve.fracOfMaxPower[i]*maxOutPowerKW;
			float curEff = customCurve.effValues[i];
			
			if (curEff > 0) powY[i] = powX[i]/curEff;		
			if (curEff > bestEff) {
				bestEff = curEff;
				powerAtMaxEffKW = powX[i];
			}
		}
		
		kWInValues = new float[NumPointsForKWOut];
		for (int i=1; i<kWInValues.length; i++) {
			float curKWOut = ((float)i/(float)(NumPointsForKWOut-1))*maxOutPowerKW;
			kWInValues[i] = linInterpolate(powX, powY, curKWOut);
		}		
	}
	
	private void defaultInit(FSJVehModelParam.FuelConverterEffType fcType) {
		float[] xPercPow = {0.0f, 0.005f, 0.015f, 0.04f, 0.06f, 0.10f, 0.14f, 0.20f, 0.40f, 0.60f, 0.80f, 1.00f};

		float[] eff12_SI = {0.0f, 0.12f, 0.16f, 0.22f, 0.28f, 0.33f, 0.35f, 0.36f, 0.35f, 0.34f, 0.32f, 0.30f};	//Spark ignition 2018
		float[] eff12_SI_CNG = {0.0f, 0.12f, 0.16f, 0.22f, 0.28f, 0.33f, 0.35f, 0.36f, 0.35f, 0.34f, 0.32f, 0.30f};	//Place-Holder for Spark-Ignition for CNG
		float[] eff12_AT = {0.0f, 0.12f, 0.28f, 0.35f, 0.38f, 0.39f, 0.40f, 0.40f, 0.38f, 0.37f, 0.36f, 0.35f};	//Atkinson 2018
		float[] eff12_DZ = {0.0f, 0.14f, 0.20f, 0.26f, 0.32f, 0.39f, 0.41f, 0.42f, 0.41f, 0.38f, 0.36f, 0.34f};	//Diesel 2018
		float[] eff12_FC = {0.0f, 0.20f, 0.28f, 0.38f, 0.45f, 0.52f, 0.55f, 0.57f, 0.56f, 0.54f, 0.52f, 0.49f};	//Hydrogen Fuel cell 2018
		float[] eff12_HD = {0.0f, 0.14f, 0.20f, 0.26f, 0.32f, 0.39f, 0.41f, 0.42f, 0.41f, 0.38f, 0.36f, 0.34f};	//Hybrid Diesel 2018

		float[] powX = new float[xPercPow.length];
		float[] powY = new float[xPercPow.length];
		
		float bestEff = 0f; 
		
		for (int i=1; i<powX.length; i++) {
			powX[i] = xPercPow[i]*maxOutPowerKW;
			float curEff = 0f;
			
			switch(fcType) {
			case sparkIgnition:
				curEff = eff12_SI[i];
				break;
			case cng:
				curEff = eff12_SI_CNG[i];
				break;
			case atkins:
				curEff = eff12_AT[i];
				break;
			case diesel:
				curEff = eff12_DZ[i];
				break;
			case fuelCell:
				curEff = eff12_FC[i];
				break;
			case hybridDiesel:
				curEff = eff12_HD[i];
				break;
			}
			
			if (curEff > 0) powY[i] = powX[i]/curEff;
			
			if (curEff > bestEff) {
				bestEff = curEff;
				powerAtMaxEffKW = powX[i];
			}
		}
		
		kWInValues = new float[NumPointsForKWOut];
		for (int i=1; i<kWInValues.length; i++) {
			float curKWOut = ((float)i/(float)(NumPointsForKWOut-1))*maxOutPowerKW;
			kWInValues[i] = linInterpolate(powX, powY, curKWOut);
		}		
	}
	private static float linInterpolate(float[] xs, float[] ys, float x) {
		if (x <= xs[0]) return ys[0];
		if (x >= xs[xs.length-1]) return ys[xs.length-1];
		
		int intervalID = 0;
		while (xs[intervalID+1] < x) intervalID++;
		
		float x1 = xs[intervalID];
		float x2 = xs[intervalID+1];
		float z = (x-x1)/(x2-x1);
		
		return ys[intervalID] + z*(ys[intervalID+1]-ys[intervalID]);
	}

}
