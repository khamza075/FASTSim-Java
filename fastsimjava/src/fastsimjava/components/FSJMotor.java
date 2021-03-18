package fastsimjava.components;

import fastsimjava.FSJEffCurvesManager;
import fastsimjava.FSJVehModelParam;

public class FSJMotor {
	//Constant for look-up arrays
	private static final int NumPointsForLookup = 101;

	//Maximum output power
	private float maxOutPowerKW;
	public float maxOutPowerKW() {return maxOutPowerKW;}
	
	//Maximum input power
	private float maxKWIn;
	public float maxKWIn() {return maxKWIn;}
	
	//Minimum efficiency of the motor
	private float minMotorEff;
	public float minMotorEff() {return minMotorEff;}
	
	//Lookup arrays
	private float[] outKWArr, inKWArr;
		
	//Function to calculate necessary input (electric) power for desired output (mechanical) power
	public float inputPowerKW(float outPowerKW) {
		if (outPowerKW <= 0) return 0f;
		if (outPowerKW >= maxOutPowerKW) return maxKWIn;
		
		int arrIDinOutKW = 0;
		for (int i=1; i<outKWArr.length; i++) {
			if (outPowerKW < outKWArr[i]) break;
			arrIDinOutKW++;
		}
		
		if ((arrIDinOutKW+1)>=outKWArr.length) return maxKWIn;
		
		float y1 = inKWArr[arrIDinOutKW];
		float y2 = inKWArr[arrIDinOutKW+1];
		float c2 = (outPowerKW-outKWArr[arrIDinOutKW])/(outKWArr[arrIDinOutKW+1]-outKWArr[arrIDinOutKW]);
		float c1 = 1f - c2;		

		return c1*y1 + c2*y2;
	}
	//Function to calculate the output (mechanical) power for a given input (electric) power
	public float outputPowerKW(float inPowerKW) {
		if (inPowerKW <= 0) return 0f;
		if (inPowerKW >= maxKWIn) return maxOutPowerKW;
		
		int arrIDinInKW = 0;
		for (int i=1; i<inKWArr.length; i++) {
			if (inPowerKW < inKWArr[i]) break;
			arrIDinInKW++;
		}

		float y1 = outKWArr[arrIDinInKW];
		float y2 = outKWArr[arrIDinInKW+1];
		float c2 = (inPowerKW-inKWArr[arrIDinInKW])/(inKWArr[arrIDinInKW+1]-inKWArr[arrIDinInKW]);
		float c1 = 1f - c2;		
		
		return c1*y1 + c2*y2;
	}	

	
	//Constructor
	public FSJMotor(FSJVehModelParam vehModelParam, FSJEffCurvesManager curveMan) {
		//Set maximum power
		maxOutPowerKW = vehModelParam.motor.maxMotorKw;

		//Attempt to obtain a custom curve
		PieceWiseLinearEfficiencyCurve customCurve = curveMan.getMtCurve(vehModelParam.battery.mtCCurveID);
		
		//If no custom curve available, then use default
		if (customCurve == null) {
			defaultInit(vehModelParam.motor.motorPeakEff);
			return;
		}

		//Initialize with custom curve
		float[] fracPowerLoad = customCurve.fracOfMaxPower;
		float[] baseEff = customCurve.effValues;

		float maxBaseEff = baseEff[0];
		for (int i=1; i<baseEff.length; i++) {
			if (maxBaseEff < baseEff[i]) {
				maxBaseEff = baseEff[i];
			}
		}
		
		float effAdj = vehModelParam.motor.motorPeakEff - maxBaseEff;
		float[] adjEffMap = new float[baseEff.length];		
		minMotorEff = vehModelParam.motor.motorPeakEff;
		
		for (int i=0; i<adjEffMap.length; i++) {
			adjEffMap[i] = baseEff[i] + effAdj;
			
			if (minMotorEff > adjEffMap[i]) {
				minMotorEff = adjEffMap[i];
			}
		}

		float[] xs = new float[fracPowerLoad.length];
		float[] ys = new float[fracPowerLoad.length];
		for (int i=1; i<xs.length; i++) {
			xs[i] = fracPowerLoad[i]*maxOutPowerKW;
			ys[i] = xs[i]/adjEffMap[i];
		}
		
		outKWArr = new float[NumPointsForLookup];
		inKWArr = new float[NumPointsForLookup];
		
		maxKWIn = 0f;
		
		float deltaKW = maxOutPowerKW/((float)(outKWArr.length-1));
		for (int i=1; i<outKWArr.length; i++) {
			float curKWout = i*deltaKW;
			float curKWin = linInterpolate(xs, ys, curKWout);

			outKWArr[i] = curKWout;
			inKWArr[i] = curKWin;
			
			if (maxKWIn < curKWin) {
				maxKWIn = curKWin;
			}
		}		
}
	
	
	//Default initialization
	private void defaultInit(float motorPeakEff) {
		float[] fracPowerLoad = {0.00f, 0.02f, 0.04f, 0.06f, 0.08f, 0.10f, 0.20f, 0.40f, 0.60f, 0.80f, 1.00f};
		float[] baseEffAt75kW = {0.83f, 0.85f, 0.87f, 0.89f, 0.90f, 0.91f, 0.93f, 0.94f, 0.94f, 0.93f, 0.92f};
		float[] lessEffAt7hkW = {0.12f, 0.16f, 0.21f, 0.29f, 0.35f, 0.42f, 0.75f, 0.92f, 0.93f, 0.93f, 0.92f};
		
		float maxBaseLineEff = baseEffAt75kW[0];
		for (int i=1; i<baseEffAt75kW.length; i++) {
			if (maxBaseLineEff < baseEffAt75kW[i]) {
				maxBaseLineEff = baseEffAt75kW[i];
			}
		}
		
		float effAdj = motorPeakEff-maxBaseLineEff;
		float[] baseAdjMap = new float[baseEffAt75kW.length];
		for (int i=0; i<baseAdjMap.length; i++) {
			baseAdjMap[i] = baseEffAt75kW[i] + effAdj;
		}
		
		float zeta = Math.max(0f, Math.min((maxOutPowerKW-7.5f)/(75f-7.5f), 1f));
		float[] adjEffMap = new float[baseAdjMap.length];		
		minMotorEff = motorPeakEff;
		
		for (int i=0; i<adjEffMap.length; i++) {
			adjEffMap[i] = zeta*baseAdjMap[i] + (1f-zeta)*lessEffAt7hkW[i];
			
			if (minMotorEff > adjEffMap[i]) {
				minMotorEff = adjEffMap[i];
			}
		}
		
		float[] xs = new float[fracPowerLoad.length];
		float[] ys = new float[fracPowerLoad.length];
		for (int i=1; i<xs.length; i++) {
			xs[i] = fracPowerLoad[i]*maxOutPowerKW;
			ys[i] = xs[i]/adjEffMap[i];
		}
		
		outKWArr = new float[NumPointsForLookup];
		inKWArr = new float[NumPointsForLookup];
		
		maxKWIn = 0f;
		
		float deltaKW = maxOutPowerKW/((float)(outKWArr.length-1));
		for (int i=1; i<outKWArr.length; i++) {
			float curKWout = i*deltaKW;
			float curKWin = linInterpolate(xs, ys, curKWout);

			outKWArr[i] = curKWout;
			inKWArr[i] = curKWin;
			
			if (maxKWIn < curKWin) {
				maxKWIn = curKWin;
			}
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
