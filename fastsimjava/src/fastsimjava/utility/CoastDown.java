package fastsimjava.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class CoastDown {
	//Unit conversion constants
	public static final float PoundsPerKg = 2.205f;
	public static final float MetersperMile = 1609f;
	public static final float GravityAccel = 9.81f;
	public static final float SecondsPerHour = 3600f;
	
	private static final float MilesPerHourToMetersPerSec = MetersperMile/SecondsPerHour;
	
	
	//Utility functions for reading and/or converting speed signal
	public static float milesPerHr_to_metersPerSecond(float milesPerHour) {
		return milesPerHour*MilesPerHourToMetersPerSec;
	}
	public static float[] milesPerHr_to_metersPerSecond(float[] milesPerHour) {
		float[] metersPerSec = new float[milesPerHour.length];
		for (int i=0; i<metersPerSec.length; i++) metersPerSec[i] = MilesPerHourToMetersPerSec*milesPerHour[i];
		return metersPerSec;
	}
	public static float metersPerSecond_to_milesPerHr(float metersPerSecond) {
		return metersPerSecond/MilesPerHourToMetersPerSec;
	}
	public static float[] metersPerSecond_to_milesPerHr(float[] metersPerSecond) {
		float[] milesPerHour = new float[metersPerSecond.length];
		for (int i=0; i<milesPerHour.length; i++) milesPerHour[i] = metersPerSecond[i]/MilesPerHourToMetersPerSec;
		return milesPerHour;
	}
	public static float[] readSingleColumnFloatValues(String fname) {
		try {
			String readLine;
			ArrayList<Float> lst = new ArrayList<Float>();
			BufferedReader fin = new BufferedReader(new FileReader(fname));
			while ((readLine = fin.readLine())!=null) {
				lst.add(Float.parseFloat(readLine));
			}
			fin.close();
						
			float[] arr = new float[lst.size()];
			for (int i=0; i<arr.length; i++) arr[i] = lst.get(i);
			return arr;
		} catch (Exception e) {
			return null;
		}
	}
	public static void writeSingleColumnFloatValues(String fname, float[] values) {
		try {
			FileWriter fout = new FileWriter(fname);
			String lsep = System.getProperty("line.separator");
			
			for (int i=0; i<values.length; i++) {
				fout.append(""+values[i]+lsep);
			}

			fout.flush();
			fout.close();
		} catch (Exception e) {}
	}
	
	

	
	//Class for holding normalized values of A, B & C coefficients
	// note: normalized value := coefficient-value / mass
	public static class NormalizedABC {
		private float nA, nB, nC;	//In SI units
		
		public float nA() {return nA;}	//Units of meter/second^2
		public float nB() {return nB;}	//Units of 1/second
		public float nC() {return nC;}	//Units of 1/meter
		
		public float cA_newtons(float mass_kg) {return mass_kg*nA;}	//Dynamometer coefficient A [N]
		public float cB_newtonsPerMeterPerSec(float mass_kg) {return mass_kg*nB;}	//Dynamometer coefficient B [N/(m/s)]
		public float cC_newtonsPerMeterPerSecSq(float mass_kg) {return mass_kg*nC;}	//Dynamometer coefficient C [N/((m/s)^2)]
		
		public float cA_pounds(float mass_lb) {return mass_lb*nA/GravityAccel;}	//Dynamometer coefficient A [pound-force]
		public float cB_poundsPerMilePerHour(float mass_lb) {return (mass_lb*nB/GravityAccel)*MilesPerHourToMetersPerSec;}	//Dynamometer coefficient B [pound-force per MPH]
		public float cC_poundsPerMilePerHourSq(float mass_lb) {return (mass_lb*nC/GravityAccel)*MilesPerHourToMetersPerSec*MilesPerHourToMetersPerSec;}	//Dynamometer coefficient C [pound-force per MPH^2]
		
		public String toLineString_metric(float mass_kg) {
			return ""+mass_kg+","+cA_newtons(mass_kg)+","+cB_newtonsPerMeterPerSec(mass_kg)+","+cC_newtonsPerMeterPerSecSq(mass_kg);
		}
		public String toLineString_english(float mass_lb) {
			return ""+mass_lb+","+cA_pounds(mass_lb)+","+cB_poundsPerMilePerHour(mass_lb)+","+cC_poundsPerMilePerHourSq(mass_lb);
		}
		@Override public String toString() {
			return ""+nA+","+nB+","+nC;
		}
		public String toString_onlyAC() {
			return ""+nA+","+nC;
		}
		
		private NormalizedABC() {}	//Prevent external instantiation
		
		public NormalizedABC equivalentNormalizedABC_ACOnly() {
			float startSpeed = milesPerHr_to_metersPerSecond(65f);
			float cutOffSpeed = milesPerHr_to_metersPerSecond(15f);
			float tStep = 1f;
			CoastDownReSim eqCoastDown = createCoastCDownReSim(this, startSpeed, tStep, -1, cutOffSpeed);
			return createNormalizedABC_fromCoastDown_onlyAC(eqCoastDown.speedMetersPerSec, tStep);
		}
	}
	//Functions for creation of normalized values of A, B & C coefficients via inputing values
	public static NormalizedABC createNormalizedABC_metric(float mass_kg, float cA_newtons, float cB_newtonsPerMeterPerSec, float cC_newtonsPerMeterPerSecSq) {
		NormalizedABC nABC = new NormalizedABC();
		nABC.nA = cA_newtons/mass_kg;
		nABC.nB = cB_newtonsPerMeterPerSec/mass_kg;
		nABC.nC = cC_newtonsPerMeterPerSecSq/mass_kg;
		return nABC;
	}
	public static NormalizedABC createNormalizedABC_english(float mass_lb, float cA_pounds, float cB_poundsPerMilePerHour, float cC_poundsPerMilePerHourSq) {
		NormalizedABC nABC = new NormalizedABC();
		nABC.nA = cA_pounds*GravityAccel/mass_lb;
		nABC.nB = (cB_poundsPerMilePerHour*GravityAccel/mass_lb)/MilesPerHourToMetersPerSec;
		nABC.nC = (cC_poundsPerMilePerHourSq*GravityAccel/mass_lb)/(MilesPerHourToMetersPerSec*MilesPerHourToMetersPerSec);
		return nABC;
	}
	//Function for creation of normalized values of A, B & C coefficients via optimizing for minimum error along coast-down test data
	public static NormalizedABC createNormalizedABC_fromCoastDown(float[] speedMetersPerSec, float timeStepSecond) {
		int n = speedMetersPerSec.length - 2;
		if (n < 3) return null;
		
		float vPrev = speedMetersPerSec[0];
		float vCur = speedMetersPerSec[1];
		float vNext = speedMetersPerSec[2];
		float deltaV = vPrev - vNext;
		
		float sy = vCur;
		float sy2 = vCur*vCur;
		float sy3 = vCur*vCur*vCur;
		float sy4 = vCur*vCur*vCur*vCur;
		float sz = deltaV;
		float syz = vCur*deltaV;
		float sy2z = vCur*vCur*deltaV;
		
		for (int i=1; i<n; i++) {
			vPrev = vCur;
			vCur = vNext;
			vNext = speedMetersPerSec[i+2];
			deltaV = vPrev - vNext;
			
			sy += vCur;
			sy2 += vCur*vCur;
			sy3 += vCur*vCur*vCur;
			sy4 += vCur*vCur*vCur*vCur;
			sz += deltaV;
			syz += vCur*deltaV;
			sy2z += vCur*vCur*deltaV;
		}
		
		float[] sol = solveThreeSymmetricEquations(n, sy, sy2, sy3, sy4, sz, syz, sy2z);		
		
		NormalizedABC nABC = new NormalizedABC();
		nABC.nA = sol[0]/(2f*timeStepSecond);
		nABC.nB = sol[1]/(2f*timeStepSecond);
		nABC.nC = sol[2]/(2f*timeStepSecond);

		return nABC;
	}
	//Function for creation of normalized values of A, & C coefficients (without B) via optimizing for minimum error along coast-down test data
	public static NormalizedABC createNormalizedABC_fromCoastDown_onlyAC(float[] speedMetersPerSec, float timeStepSecond) {
		int n = speedMetersPerSec.length - 2;
		if (n < 3) return null;
		
		float vPrev = speedMetersPerSec[0];
		float vCur = speedMetersPerSec[1];
		float vNext = speedMetersPerSec[2];
		float deltaV = vPrev - vNext;
		
		float sy2 = vCur*vCur;
		float sy4 = vCur*vCur*vCur*vCur;
		float sz = deltaV;
		float sy2z = vCur*vCur*deltaV;
		
		for (int i=1; i<n; i++) {
			vPrev = vCur;
			vCur = vNext;
			vNext = speedMetersPerSec[i+2];
			deltaV = vPrev - vNext;
			
			sy2 += vCur*vCur;
			sy4 += vCur*vCur*vCur*vCur;
			sz += deltaV;
			sy2z += vCur*vCur*deltaV;
		}
		
		float[] sol = solveTwoSymmetricEquations(n, sy2, sy4, sz, sy2z);		
		
		NormalizedABC nABC = new NormalizedABC();
		nABC.nA = sol[0]/(2f*timeStepSecond);
		nABC.nB = 0f;
		nABC.nC = sol[1]/(2f*timeStepSecond);

		return nABC;
	}
	private static float[] solveThreeSymmetricEquations(float n, float sy, float sy2, float sy3, float sy4, float sz, float syz, float sy2z) {
		float d = n*(sy2*sy4 - sy3*sy3) - sy*(sy*sy4 - sy2*sy3) + sy2*(sy*sy3 - sy2*sy2);
		float d1 = sz*(sy2*sy4 - sy3*sy3) - sy*(syz*sy4 - sy2z*sy3) + sy2*(syz*sy3 - sy2z*sy2);
		float d2 = n*(syz*sy4 - sy2z*sy3) - sz*(sy*sy4 - sy2*sy3) + sy2*(sy*sy2z - sy2*syz);
		float d3 = n*(sy2*sy2z - sy3*syz) - sy*(sy*sy2z - sy2*syz) + sz*(sy*sy3 - sy2*sy2);
				
		float a = d1/d;
		float b = d2/d;
		float c = d3/d;
		float[] sol = {a, b, c};
		return sol;
	}
	private static float[] solveTwoSymmetricEquations(float n, float sy2, float sy4, float sz, float sy2z) {
		float d = n*sy4  - sy2*sy2;
		float d1 = sz*sy4 - sy2z*sy2;
		float d2 = n*sy2z - sy2*sz;
				
		float a = d1/d;
		float c = d2/d;
		float[] sol = {a, c};
		return sol;
	}
	
	
	
	//Class for re-simulating a coast-down event via normalized A, B & C coefficients
	public static class CoastDownReSim {
		private float timeStepSeconds;
		private float[] speedMetersPerSec;
		
		public float[] timeSec() {
			float[] t = new float[speedMetersPerSec.length];
			for (int i=0; i<t.length; i++) t[i] = i*timeStepSeconds;
			return t;
		}
		public float[] speedMetersPerSec() {
			float[] v = new float[speedMetersPerSec.length];
			for (int i=0; i<v.length; i++) v[i] = speedMetersPerSec[i];
			return v;
		}
		public float[] speedMilesPerHour() {
			return metersPerSecond_to_milesPerHr(speedMetersPerSec);
		}

		private CoastDownReSim(NormalizedABC nABC, float startSpeedMetersPerSec, float tStepSec, int nSteps, float stopSpeedMetersPerSec) {
			//Note: set nSteps <= 0 to continue simulation until the last time step before falling below stopSpeedMetersPerSec
			timeStepSeconds = tStepSec;
			
			if (nSteps > 0) {
				speedMetersPerSec = new float[nSteps+1];
				
				float curSpeed = startSpeedMetersPerSec;
				speedMetersPerSec[0] = curSpeed;
				
				for (int i=0; i<nSteps; i++) {
					curSpeed = advanceToNextStep(nABC, curSpeed);
					speedMetersPerSec[i+1] = curSpeed;
				}				
			} else {
				ArrayList<Float> lst = new ArrayList<Float>();
				float curSpeed = startSpeedMetersPerSec;
				
				while (curSpeed > stopSpeedMetersPerSec) {
					lst.add(curSpeed);
					curSpeed = advanceToNextStep(nABC, curSpeed);
				}
				
				speedMetersPerSec = new float[lst.size()];
				for (int i=0; i<speedMetersPerSec.length; i++) speedMetersPerSec[i] = lst.get(i);
			}
		}
		private float advanceToNextStep(NormalizedABC nABC, float curSpeed) {
			final float targetRKSolStep = 0.001f;
			int nSubSteps = (int)((timeStepSeconds/targetRKSolStep) + 0.5f);
			if (nSubSteps < 1) nSubSteps = 1;
			float dh = timeStepSeconds/((float)nSubSteps);
			
			float cSpeed = curSpeed;
			for (int i=0; i<nSubSteps; i++) cSpeed = nextRKStep(nABC, cSpeed, dh);
			
			return cSpeed;
		}
		private float nextRKStep(NormalizedABC nABC, float v, float dh) {
			float k1 = dh*vDot(nABC, v);
			float k2 = dh*vDot(nABC, v + 0.5f*k1);
			float k3 = dh*vDot(nABC, v + 0.5f*k2);
			float k4 = dh*vDot(nABC, v + k3);
			
			return v + (k1 + 2*k2 + 2*k3 + k4)/6f;
		}
		private float vDot(NormalizedABC nABC, float v) {
			return -(nABC.nA + nABC.nB*v + nABC.nC*v*v);
		}
	}
	
	//Functions for creation of Re-Simulated Coast-Down
	//Note: set nSteps <= 0 to continue simulation until the last time step before falling below stopSpeedMetersPerSec
	public static CoastDownReSim createCoastCDownReSim(NormalizedABC nABC, float startSpeedMetersPerSec, float tStepSec, int nSteps, float stopSpeedMetersPerSec) {
		return new CoastDownReSim(nABC, startSpeedMetersPerSec, tStepSec, nSteps, stopSpeedMetersPerSec);
	}
	
}
