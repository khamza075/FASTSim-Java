package fastsimjava.utility;

public class KineticIntensityPlus {
	
	public static KineticIntensityPlus createViaAnalyzingCycle(float[] speedMPH, float[] roadSlope) {
		try {
			return new KineticIntensityPlus(speedMPH, roadSlope);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static final float SecondsPerHour = 3600f;
	private static final float MetersPerMile = 1609f;
	private static final float MPHtoMPS = MetersPerMile/SecondsPerHour;
	private static final float GravityAccelMPS2 = 9.81f;
	private static final float MillimetersPerInch = 25.4f;
	private static final float MillimetersPerFoot = MillimetersPerInch*12f;
	private static final float FeetPerMeter = 1000f/MillimetersPerFoot;

	//Data storage
	private float durationSec, nonIdlingSec, distanceM, avSpeedMPS, avNonZeroSpeedMPS, maxSpeedMPS, charAccelMPS2, aeroDynSpeedM2PS2, kiPM, maxAccMPS2, maxDecMPS2;
	private int numStops;
	
	//Output functions
	public float durationHrs() {return durationSec/SecondsPerHour;}
	public float distanceMiles() {return distanceM/MetersPerMile;}
	public float avSpeedMPH() {return avSpeedMPS/MPHtoMPS;}
	public float fracIdling() {return (durationSec - nonIdlingSec)/durationSec;}
	public float avNonZeroSpeedMPH() {return avNonZeroSpeedMPS/MPHtoMPS;}
	public float maxSpeedMPH() {return maxSpeedMPS/MPHtoMPS;}
	public float stopsPerMile() {return (float)numStops/distanceMiles();}
	public float charAccelMPS2() {return charAccelMPS2;}
	public float charAccelFtPS2() {return charAccelMPS2*FeetPerMeter;}
	public float maxAccelMPS2() {return maxAccMPS2;}
	public float maxAccelFtPS2() {return maxAccMPS2*FeetPerMeter;}
	public float maxDecelMPS2() {return maxDecMPS2;}
	public float maxDecelFtPS2() {return maxDecMPS2*FeetPerMeter;}
	public float kineticIntensityPerMile() {return kiPM*MetersPerMile;}


	//Constructor via analysis of drive cylce or trip
	private KineticIntensityPlus(float[] speedMPH, float[] roadSlope) throws Exception {
		if (speedMPH == null) throw new Exception("Null drive cylce");
		if (speedMPH.length < 4) throw new Exception("Not enough data points in drive cylce");
		
		float[] slope = roadSlope;
		if (slope == null) slope = new float[speedMPH.length];
		
		//Time duration
		durationSec = speedMPH.length - 1f;
		nonIdlingSec = 0f;
		
		//Speed integrals
		avNonZeroSpeedMPS = 0f;
		float speedIntegral = 0f;
		float speedCubedIntegral = 0f;
		float prevMPS = 0f;
		maxSpeedMPS = prevMPS;
		
		numStops = 0;
		boolean previouslyMoving = false;
		
		for (int i=0; i<speedMPH.length; i++) {
			float curMPS = speedMPH[i] * MPHtoMPS;
			
			if (curMPS > 0) {
				nonIdlingSec += 1f;
				if (!previouslyMoving) previouslyMoving = true;
			} else {
				if (previouslyMoving) {
					numStops++;
					previouslyMoving = false;
				}
			}
			
			if (maxSpeedMPS < curMPS) maxSpeedMPS = curMPS;
			
			speedIntegral += 0.5f*(prevMPS + curMPS);
			speedCubedIntegral += 0.5f*(prevMPS*prevMPS*prevMPS + curMPS*curMPS*curMPS);
			
			prevMPS = curMPS;
		}
		
		distanceM = speedIntegral;
		avSpeedMPS = speedIntegral/durationSec;
		aeroDynSpeedM2PS2 = speedCubedIntegral/speedIntegral;
		if (nonIdlingSec > 0) avNonZeroSpeedMPS = distanceM/nonIdlingSec;
		
		//Acceleration Integrals
		int ilim = speedMPH.length-1;
		prevMPS = speedMPH[0] * MPHtoMPS;
		float curMPS = speedMPH[1] * MPHtoMPS;
		float accWorkIntegral = 0f;
		
		maxAccMPS2 = 0f;
		maxDecMPS2 = 0f;
		
		for (int i=1; i<ilim; i++) {
			float nextMPS = speedMPH[i+1] * MPHtoMPS;
			float curAccMPS2 = (nextMPS - prevMPS)/2f + GravityAccelMPS2 * (float)Math.sin(Math.atan(slope[i]));
			
			if (curAccMPS2 > 0) accWorkIntegral += curAccMPS2*curMPS;		
			
			if (maxAccMPS2 < curAccMPS2) maxAccMPS2 = curAccMPS2;
			if (maxDecMPS2 > curAccMPS2) maxDecMPS2 = curAccMPS2;
			
			prevMPS = curMPS;
			curMPS = nextMPS;
		}
		
		float startAcc = (speedMPH[1] * MPHtoMPS)/2f + GravityAccelMPS2 * (float)Math.sin(Math.atan(slope[0]));
		if (startAcc > 0) accWorkIntegral += 0.5f * startAcc * speedMPH[0] * MPHtoMPS;
		if (maxAccMPS2 < startAcc) maxAccMPS2 = startAcc;
		if (maxDecMPS2 > startAcc) maxDecMPS2 = startAcc;
		
		float endAcc = (-speedMPH[speedMPH.length-2])/2f + GravityAccelMPS2 * (float)Math.sin(Math.atan(slope[speedMPH.length-1]));
		if (endAcc > 0) accWorkIntegral += 0.5f * endAcc * speedMPH[speedMPH.length-1] * MPHtoMPS;
		if (maxAccMPS2 < endAcc) maxAccMPS2 = endAcc;
		if (maxDecMPS2 > endAcc) maxDecMPS2 = endAcc;

		charAccelMPS2 = accWorkIntegral/distanceM;
		kiPM = charAccelMPS2/aeroDynSpeedM2PS2;
	}
}
