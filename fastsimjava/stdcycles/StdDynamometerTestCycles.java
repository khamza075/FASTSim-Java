package fastsimjava.stdcycles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class StdDynamometerTestCycles {
	private TestCycle[] cycles;
	
	public TestCycle getCycleByName(String cycleName) {
		return getCycleByID(getCycleID(cycleName));
	}
	public TestCycle getCycleByID(int id) {
		if (id < 0) return null;
		if (id >= cycles.length) return null;
		return cycles[id];
	}
	public int getNumCycles() {return cycles.length;}
	public int getCycleID(String cycleName) {
		int id = -1;
		for (int i=0; i<cycles.length; i++) {
			if (cycleName.equalsIgnoreCase(cycles[i].name)) return i;
		}
		return id;
	}
	
	
	public StdDynamometerTestCycles(String folderName) {
		String[] cyNames = reqCycleFileNames();
		cycles = new TestCycle[cyNames.length];
		for (int i=0; i<cycles.length; i++) {
			cycles[i] = new TestCycle(cyNames[i], folderName+"\\"+cyNames[i]+".csv");
		}
	}
	
	public static String[] reqCycleFileNames() {
		String[] fnames = {"FTP", "HWFET", "LA92", "NYC", "US06", "SC03", "UDDS"};
		return fnames;
	}
	public static float derateFTP_mpg(float simMPG) {
		return 1f/(0.003259f + (1.1805f/simMPG));
	}
	public static float derateHWFET_mpg(float simMPG) {
		return 1f/(0.001376f + (1.3466f/simMPG));
	}
	public static float derateFTP_kwhmp(float simKWHPM) {
		float kwhPerGGE = 33.7f;
		float ggePerMi = simKWHPM/kwhPerGGE;
		float deratedGGEPM = 0.003259f + 1.1805f*ggePerMi;
		return deratedGGEPM*kwhPerGGE;
	}
	public static float derateHWFET_kwhmp(float simKWHPM) {
		float kwhPerGGE = 33.7f;
		float ggePerMi = simKWHPM/kwhPerGGE;
		float deratedGGEPM = 0.001376f + 1.3466f*ggePerMi;
		return deratedGGEPM*kwhPerGGE;
	}
	public static float derateFTP_dieselMPG(float simMPG) {
		float kwhPerGGE = 33.7f;
		float kwhPerGDiesel =37.9527f;
		float ggePerMi = simMPG * kwhPerGGE / kwhPerGDiesel;
		float deratedGGEPM = 1f/(0.003259f + (1.1805f/ggePerMi));
		return deratedGGEPM * kwhPerGDiesel / kwhPerGGE;
	}
	public static float derateHWFET_dieselMPG(float simMPG) {
		float kwhPerGGE = 33.7f;
		float kwhPerGDiesel =37.9527f;
		float ggePerMi = simMPG * kwhPerGGE / kwhPerGDiesel;
		float deratedGGEPM = 1f/(0.001376f + (1.3466f/ggePerMi));
		return deratedGGEPM * kwhPerGDiesel / kwhPerGGE;
	}
	
	public void addCycle(String cycleName, float[] speedTrace) {
		TestCycle[] tmp = cycles;
		cycles = new TestCycle[tmp.length+1];
		for (int i=0; i<tmp.length; i++) cycles[i] = tmp[i];
		cycles[tmp.length] = new TestCycle(cycleName, speedTrace);
	}
	public void addCycle(String cycleName, String cycleFileName) {
		TestCycle[] tmp = cycles;
		cycles = new TestCycle[tmp.length+1];
		for (int i=0; i<tmp.length; i++) cycles[i] = tmp[i];
		cycles[tmp.length] = new TestCycle(cycleName, cycleFileName);
	}
	
	

	public static class TestCycle {
		private String name;
		private float[] speed;
		private float[] optRoadSlope;
		public String name() {return name;};
		public float[] speed() {return speed;}
		public float[] roadSlope() {return optRoadSlope;}
		
		private TestCycle() {}
		public TestCycle(String cycleName, String fname) {
			name = cycleName;
			readCycleFromFile(fname);
			optRoadSlope = null;
		}
		public TestCycle(String cycleName, float[] speedTrace) {
			name = cycleName;
			speed = new float[speedTrace.length];
			for (int i=0; i<speed.length; i++) speed[i] = speedTrace[i];
			optRoadSlope = null;
		}
		private void readCycleFromFile(String fname) {
			ArrayList<Float> lst = new ArrayList<Float>();
			
			try {
				BufferedReader readingBuffer=new BufferedReader(new FileReader(fname));
				String readLine;
				
				while ((readLine = readingBuffer.readLine())!=null) {
					String[] strSplit = readLine.split(",");
					lst.add(Float.parseFloat(strSplit[1]));
				}				
				readingBuffer.close();
			} catch (IOException e) {}
			
			speed = new float[lst.size()];
			for (int i=0; i<speed.length; i++) speed[i] = lst.get(i);
		}
		
		public void writeToFile(String fname) {
			try {
				FileWriter fWriter = new FileWriter(fname);
				String lsep = System.getProperty("line.separator");
				
				fWriter.append("timeSec,speedMPH");
				fWriter.append(lsep);
				
				for (int i=0; i<speed.length; i++) {
					fWriter.append(""+i+","+speed[i]);
					fWriter.append(lsep);
				}
				
				fWriter.flush();
				fWriter.close();
			} catch (IOException e) {}			
		}
	}
	
	public static TestCycle greenifyCycle(TestCycle baseCycle, float maxSpeedMPH, float maxAccelMperS2) {
		TestCycle giCycle = new TestCycle();
		giCycle.name = baseCycle.name+"_gi";
		
		float zTol = (float)1.0e-5;
		float mphPerMps = 2.2369f;
		float maxSpeedMS = maxSpeedMPH/mphPerMps;
		
		float[] baseSpeedMPH = baseCycle.speed;
		float[] giTimeSec = new float[baseSpeedMPH.length]; 
		float[] giSpeedMS = new float[baseSpeedMPH.length];
		
		float lastBaseSpeedMS = baseSpeedMPH[0]/mphPerMps;
		float lastGISpeedMS = lastBaseSpeedMS;
		giTimeSec[0] = 0f;
		giSpeedMS[0] = lastGISpeedMS;
		
		for (int i=1; i<baseSpeedMPH.length; i++) {
			float timeStep = 1.0f;
			float nextBaseSpeed = baseSpeedMPH[i]/mphPerMps;
			float distMeters = 0.5f*(lastBaseSpeedMS + nextBaseSpeed)*timeStep;
			
			float curMaxSpeed = Math.min(nextBaseSpeed, maxSpeedMS);
			float nextGISpeed = (2.0f*distMeters/timeStep) - lastGISpeedMS;
			
			//Other checks
			if (nextGISpeed < 0) {
				//Go to zero and shorten the time step
				nextGISpeed = 0f;
				if (lastGISpeedMS > zTol) {
					timeStep = 2.0f*distMeters/lastGISpeedMS;
				}
			} else if (nextGISpeed > curMaxSpeed) {
				//Cap the speed, increase time step to match the distance covered
				nextGISpeed = curMaxSpeed;
				timeStep = 2.0f*distMeters/(lastGISpeedMS + nextGISpeed);
				
				//Check that max acceleration isn't violated
				float curAccel = (nextGISpeed-lastGISpeedMS)/timeStep;
				if (curAccel > maxAccelMperS2) {
					//cap acceleration and recalculate speed and time step size
					float accel = maxAccelMperS2;
					timeStep = ((float)Math.sqrt(lastGISpeedMS*lastGISpeedMS + 2f*accel*distMeters)-lastGISpeedMS)/accel;
					nextGISpeed = lastGISpeedMS + timeStep*accel;
				}
			} else {
				//Check that max acceleration isn't violated
				float curAccel = (nextGISpeed-lastGISpeedMS)/timeStep;
				if (curAccel > maxAccelMperS2) {
					//Cap acceleration and recalculate speed and time step size
					float accel = maxAccelMperS2;
					timeStep = ((float)Math.sqrt(lastGISpeedMS*lastGISpeedMS + 2f*accel*distMeters)-lastGISpeedMS)/accel;
					nextGISpeed = lastGISpeedMS + timeStep*accel;
					
					//Check that max speed isn't exceeded
					if (nextGISpeed > curMaxSpeed) {
						//Cap speed and increase time step accordingly
						nextGISpeed = curMaxSpeed;
						timeStep = 2.0f*distMeters/(lastGISpeedMS + nextGISpeed);
					}
				}
			}
			
			//Record values
			giTimeSec[i] = giTimeSec[i-1] + timeStep;
			giSpeedMS[i] = nextGISpeed;
			
			//Set stage for next step
			lastBaseSpeedMS = nextBaseSpeed;
			lastGISpeedMS = nextGISpeed;
		}			
		
		//Interpolate the greenified cycle at 1sec intervals
		ArrayList<Float> lst = new ArrayList<Float>();
		int curPos = 0;
		float curTime = 0f;
		float finTime = giTimeSec[giTimeSec.length-1];
		float curSpeedMS = giSpeedMS[0];
		lst.add(curSpeedMS);
		
		while (curTime < finTime) {
			curTime += 1f;
			while (curTime > giTimeSec[curPos+1]) {
				curPos++;
				if (curPos >= giSpeedMS.length-2) break;
			}
			if (curPos >= giSpeedMS.length-2) break;
			

			float c2 = (curTime-giTimeSec[curPos])/(giTimeSec[curPos+1]-giTimeSec[curPos]);
			float c1 = 1f - c2;
			curSpeedMS = c1*giSpeedMS[curPos] + c2*giSpeedMS[curPos+2];
			
			lst.add(curSpeedMS);
		}
		lst.add(giSpeedMS[giSpeedMS.length-1]);
		
		
		//Convert to mph and copy to output
		giCycle.speed = new float[lst.size()];
		for (int i=0; i<giCycle.speed.length; i++) giCycle.speed[i] = lst.get(i)*mphPerMps;
		
		return giCycle;
	}
	
	public static TestCycle greenifyCycle(TestCycle baseCycle, float maxSpeedMPH, float maxAccelMperS2, float[] roadSlope) {
		TestCycle giCycle = new TestCycle();
		giCycle.name = baseCycle.name+"_gi";
		
		float zTol = (float)1.0e-5;
		float mphPerMps = 2.2369f;
		float maxSpeedMS = maxSpeedMPH/mphPerMps;
		
		float[] baseSpeedMPH = baseCycle.speed;
		float[] giTimeSec = new float[baseSpeedMPH.length]; 
		float[] giSpeedMS = new float[baseSpeedMPH.length];
		
		float lastBaseSpeedMS = baseSpeedMPH[0]/mphPerMps;
		float lastGISpeedMS = lastBaseSpeedMS;
		giTimeSec[0] = 0f;
		giSpeedMS[0] = lastGISpeedMS;
		
		for (int i=1; i<baseSpeedMPH.length; i++) {
			float timeStep = 1.0f;
			float nextBaseSpeed = baseSpeedMPH[i]/mphPerMps;
			float distMeters = 0.5f*(lastBaseSpeedMS + nextBaseSpeed)*timeStep;
			
			float curMaxSpeed = Math.min(nextBaseSpeed, maxSpeedMS);
			float nextGISpeed = (2.0f*distMeters/timeStep) - lastGISpeedMS;
			
			//Other checks
			if (nextGISpeed < 0) {
				//Go to zero and shorten the time step
				nextGISpeed = 0f;
				if (lastGISpeedMS > zTol) {
					timeStep = 2.0f*distMeters/lastGISpeedMS;
				}
			} else if (nextGISpeed > curMaxSpeed) {
				//Cap the speed, increase time step to match the distance covered
				nextGISpeed = curMaxSpeed;
				timeStep = 2.0f*distMeters/(lastGISpeedMS + nextGISpeed);
				
				//Check that max acceleration isn't violated
				float curAccel = (nextGISpeed-lastGISpeedMS)/timeStep;
				if (curAccel > maxAccelMperS2) {
					//cap acceleration and recalculate speed and time step size
					float accel = maxAccelMperS2;
					timeStep = ((float)Math.sqrt(lastGISpeedMS*lastGISpeedMS + 2f*accel*distMeters)-lastGISpeedMS)/accel;
					nextGISpeed = lastGISpeedMS + timeStep*accel;
				}
			} else {
				//Check that max acceleration isn't violated
				float curAccel = (nextGISpeed-lastGISpeedMS)/timeStep;
				if (curAccel > maxAccelMperS2) {
					//Cap acceleration and recalculate speed and time step size
					float accel = maxAccelMperS2;
					timeStep = ((float)Math.sqrt(lastGISpeedMS*lastGISpeedMS + 2f*accel*distMeters)-lastGISpeedMS)/accel;
					nextGISpeed = lastGISpeedMS + timeStep*accel;
					
					//Check that max speed isn't exceeded
					if (nextGISpeed > curMaxSpeed) {
						//Cap speed and increase time step accordingly
						nextGISpeed = curMaxSpeed;
						timeStep = 2.0f*distMeters/(lastGISpeedMS + nextGISpeed);
					}
				}
			}
			
			//Record values
			giTimeSec[i] = giTimeSec[i-1] + timeStep;
			giSpeedMS[i] = nextGISpeed;
			
			//Set stage for next step
			lastBaseSpeedMS = nextBaseSpeed;
			lastGISpeedMS = nextGISpeed;
		}			
		
		//Interpolate the greenified cycle at 1sec intervals
		ArrayList<Float> lst = new ArrayList<Float>();
		ArrayList<Float> lstSlope = new ArrayList<Float>();
		int curPos = 0;
		float curTime = 0f;
		float finTime = giTimeSec[giTimeSec.length-1];
		float curSpeedMS = giSpeedMS[0];
		float curSlope = roadSlope[0];
		lst.add(curSpeedMS);
		lstSlope.add(curSlope);
		
		while (curTime < finTime) {
			curTime += 1f;
			while (curTime > giTimeSec[curPos+1]) {
				curPos++;
				if (curPos >= giSpeedMS.length-2) break;
			}
			if (curPos >= giSpeedMS.length-2) break;
			

			float c2 = (curTime-giTimeSec[curPos])/(giTimeSec[curPos+1]-giTimeSec[curPos]);
			float c1 = 1f - c2;
			
			curSpeedMS = c1*giSpeedMS[curPos] + c2*giSpeedMS[curPos+2];
			curSlope = c1*roadSlope[curPos] + c2*roadSlope[curPos+2];
			
			lst.add(curSpeedMS);
			lstSlope.add(curSlope);
		}
		lst.add(giSpeedMS[giSpeedMS.length-1]);
		lstSlope.add(roadSlope[roadSlope.length-1]);
		
		
		//Convert to mph and copy to output
		giCycle.speed = new float[lst.size()];
		giCycle.optRoadSlope = new float[lst.size()];
		for (int i=0; i<giCycle.speed.length; i++) {
			giCycle.speed[i] = lst.get(i)*mphPerMps;
			giCycle.optRoadSlope[i] = lstSlope.get(i);
		}
		
		return giCycle;
	}
}
