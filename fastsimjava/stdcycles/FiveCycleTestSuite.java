package fastsimjava.stdcycles;

import fastsimjava.*;
import fastsimjava.FSJVehModelParam.FuelConverterEffType;
import fastsimjava.components.*;

public class FiveCycleTestSuite {
	private static final int uddsBag1End = 505;
	//private static final int ftpWaitBag2ToBag3 = 600;	//may come in use when thermal modeling modules are included in FASTSim
	private static final int us06HighwayStart = 132;
	private static final int us06HighwayEnd = 494;
	
	private float[] uddsBag1, uddsBag2, ftpFull, hwfetFull, us06City1, us06City2, us06Highway, us06Full, sc03Full;
	
	
	public FiveCycleTestSuite(StdDynamometerTestCycles testCycles) throws Exception {
		StdDynamometerTestCycles.TestCycle udds = testCycles.getCycleByName("UDDS");
		StdDynamometerTestCycles.TestCycle ftp = testCycles.getCycleByName("FTP");
		StdDynamometerTestCycles.TestCycle hwfet = testCycles.getCycleByName("HWFET");
		StdDynamometerTestCycles.TestCycle us06 = testCycles.getCycleByName("US06");
		StdDynamometerTestCycles.TestCycle sc03 = testCycles.getCycleByName("SC03");
		
		float[] mph = udds.speed();
		uddsBag1 = new float[uddsBag1End+1];
		uddsBag2 = new float[mph.length-uddsBag1End];
		
		for (int i=0; i<uddsBag1.length; i++) uddsBag1[i] = mph[i];
		for (int i=0; i<uddsBag2.length; i++) uddsBag2[i] = mph[uddsBag1End+i];
		
		mph = ftp.speed();
		ftpFull = new float[mph.length];
		for (int i=0; i<ftpFull.length; i++) ftpFull[i] = mph[i];		
		
		mph = hwfet.speed();
		hwfetFull = new float[mph.length];
		for (int i=0; i<hwfetFull.length; i++) hwfetFull[i] = mph[i];		
		
		mph = sc03.speed();
		sc03Full = new float[mph.length];
		for (int i=0; i<sc03Full.length; i++) sc03Full[i] = mph[i];
		
		mph = us06.speed();
		us06Full = new float[mph.length];
		for (int i=0; i<us06Full.length; i++) us06Full[i] = mph[i];
		
		us06Highway = new float[us06HighwayEnd-us06HighwayStart+1];
		us06City1 = new float[us06HighwayStart+1];
		us06City2 = new float[mph.length - us06HighwayEnd];
		
		for (int i=0; i<us06Highway.length; i++) us06Highway[i] = mph[us06HighwayStart+i];
		for (int i=0; i<us06City1.length; i++) us06City1[i] = mph[i];
		for (int i=0; i<us06City2.length; i++) us06City2[i] = mph[us06HighwayEnd+i];
	}

	//Function for performing FASTSim simulation of EPA 5-Cycle test procedures of a given Vehicle model -- Default Inputs otherwise 
	//This remains Work in progress until FASTSim-HOT
	public FiveCycleTestResult performTest(FSJVehModelParam vehModel, FSJEffCurvesManager curveManager) {
		FiveCycleTestResult res = new FiveCycleTestResult();
		FASTSimJ3c fsj = new FASTSimJ3c();
		fsj.setVehModel(vehModel, curveManager);		
		
		float chargerEfficiency = vehModel.transmission.chgEff;
		//System.out.println("Assumed Charger Efficiency for Equivalent EPA Test = " + chargerEfficiency);
		
		switch (vehModel.general.vehPtType) {
		case cv:
		{
			//FIRST TEST: Run the "normal temperature FTP"
			//Bag-1 //Thermal modules would use Ambient Temperature 75F and cold engine start
			fsj.runC(null, uddsBag1, null);
			float ftp75F_Bag1_miles = fsj.lastTripSummary().miles;
			float ftp75F_Bag1_gal = fsj.lastTripSummary().fuelUse;
			
			//Bag-2 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 75F
			fsj.runC(null, uddsBag2, null);
			float ftp75F_Bag2_miles = fsj.lastTripSummary().miles;
			float ftp75F_Bag2_gal = fsj.lastTripSummary().fuelUse;
						
			//Bag-3 //Thermal modules would use Ambient Temperature 75F, Vehicle & Engine left to cool off for a period of ftpWaitBag2ToBag3 seconds
			fsj.runC(null, uddsBag1, null);
			float ftp75F_Bag3_miles = fsj.lastTripSummary().miles;
			float ftp75F_Bag3_gal = fsj.lastTripSummary().fuelUse;
			
			
			
			//SECOND TEST: Run the "Cold temperature FTP"
			//Bag-1 //Thermal modules would use Ambient Temperature 20F and cold engine start
			fsj.runC(null, uddsBag1, null);
			float ftp20F_Bag1_miles = fsj.lastTripSummary().miles;
			float ftp20F_Bag1_gal = fsj.lastTripSummary().fuelUse;
			
			//Bag-2 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 20F
			fsj.runC(null, uddsBag2, null);
			float ftp20F_Bag2_miles = fsj.lastTripSummary().miles;
			float ftp20F_Bag2_gal = fsj.lastTripSummary().fuelUse;
			
			//Bag-3 //Thermal modules would use Ambient Temperature 20F, Vehicle & Engine left to cool off for a period of ftpWaitBag2ToBag3 seconds
			fsj.runC(null, uddsBag1, null);
			float ftp20F_Bag3_miles = fsj.lastTripSummary().miles;
			float ftp20F_Bag3_gal = fsj.lastTripSummary().fuelUse;
			
			
			
			//THIRD TEST: Run US06
			//City Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
			fsj.runC(null, us06City1, null);
			float us06_city_miles = fsj.lastTripSummary().miles;
			float us06_city_gal = fsj.lastTripSummary().fuelUse;
			
			//Highway Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
			fsj.runC(null, us06Highway, null);
			float us06_highway_miles = fsj.lastTripSummary().miles;
			float us06_highway_gal = fsj.lastTripSummary().fuelUse;
			
			fsj.runC(null, us06City2, null);
			us06_city_miles += fsj.lastTripSummary().miles;
			us06_city_gal += fsj.lastTripSummary().fuelUse;
			
			
			//FOURTH TEST: Run HWFET
			//City Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
			fsj.runC(null, hwfetFull, null);
			float hwfet_miles = fsj.lastTripSummary().miles;
			float hwfet_gal = fsj.lastTripSummary().fuelUse;
						
			
			//FIFTH TEST: Run SC03
			//City Portion //Thermal modules would use Ambient Temperature 95 (with AC on) and warmed up engine
			fsj.runC(null, sc03Full, null);
			float sc03_miles = fsj.lastTripSummary().miles;
			float sc03_gal = fsj.lastTripSummary().fuelUse;
			
			
			//CITY Rating Calculation
			float startFuel75 = 3.6f*((ftp75F_Bag1_gal/ftp75F_Bag1_miles) - (ftp75F_Bag3_gal/ftp75F_Bag3_miles));
			float startFuel20 = 3.6f*((ftp20F_Bag1_gal/ftp20F_Bag1_miles) - (ftp20F_Bag3_gal/ftp20F_Bag3_miles));
			
			float startFC = 0.33f*(0.76f*startFuel75 + 0.24f*startFuel20)/4.1f;
			float runningFC = 0.82f*(0.48f*ftp75F_Bag2_gal/ftp75F_Bag2_miles + 0.41f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.11f*us06_city_gal/us06_city_miles) +
					0.18f*(0.5f*ftp20F_Bag2_gal/ftp20F_Bag2_miles + 0.5f*ftp20F_Bag3_gal/ftp20F_Bag3_miles) + 
					0.133f*1.083f*(sc03_gal/sc03_miles - (0.61f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.39f*ftp75F_Bag2_gal/ftp75F_Bag2_miles));
			
			res.city.mpg = 0.905f/(startFC + runningFC);
			
			
			//HIGHWAY Rating Calculation
			startFC = 0.33f*(0.76f*startFuel75 + 0.24f*startFuel20)/60f;
			runningFC = 1.007f*(0.79f*us06_highway_gal/us06_highway_miles + 0.21f*hwfet_gal/hwfet_miles) + 
					0.133f*0.377f*(sc03_gal/sc03_miles - (0.61f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.39f*ftp75F_Bag2_gal/ftp75F_Bag2_miles));
			
			res.highway.mpg = 0.905f/(startFC + runningFC);
			
			//COMBINED Rating Calculation
			res.combined.mpg = 1f/(0.55f/res.city.mpg + 0.45f/res.highway.mpg);
		}
			break;
		case hev:
			if (vehModel.fuelConv.fcEffType == FuelConverterEffType.fuelCell) {	//Fuel Cell Hybrid
				//Using the 0.7 factor method and one run of FTP + HWFET -- No Thermal Effects Correction seem to be employed or needed as of 2017 standard 
				fsj.runC(null, ftpFull, null);
				float ftp_miles = fsj.lastTripSummary().miles;
				float ftp_kgH2 = fsj.lastTripSummary().fuelUse;
				
				fsj.runC(null, hwfetFull, null);
				float hwfet_miles = fsj.lastTripSummary().miles;
				float hwfet_kgH2 = fsj.lastTripSummary().fuelUse;
				
				res.city.mpg = (ftp_miles*0.7f)/ftp_kgH2;
				res.highway.mpg = (hwfet_miles*0.7f)/hwfet_kgH2;
				res.combined.mpg = 1f/(0.55f/res.city.mpg + 0.45f/res.highway.mpg);
			} 
			else 
			{
				//FIRST TEST: Run the "normal temperature FTP"
				//Bag-1 //Thermal modules would use Ambient Temperature 75F and cold engine start
				fsj.runC(null, uddsBag1, null);
				float ftp75F_Bag1_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag1_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-2 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 75F
				fsj.runC(null, uddsBag2, null);
				float ftp75F_Bag2_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag2_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-3 //Thermal modules would use Ambient Temperature 75F, Vehicle & Engine left to cool off for a period of ftpWaitBag2ToBag3 seconds
				fsj.runC(null, uddsBag1, null);
				float ftp75F_Bag3_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag3_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-4 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 75F
				fsj.runC(null, uddsBag2, null);
				float ftp75F_Bag4_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag4_gal = fsj.lastTripSummary().fuelUse;
				
				
				
				//SECOND TEST: Run the "Cold temperature FTP"
				//Bag-1 //Thermal modules would use Ambient Temperature 20F and cold engine start
				fsj.runC(null, uddsBag1, null);
				float ftp20F_Bag1_miles = fsj.lastTripSummary().miles;
				float ftp20F_Bag1_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-2 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 20F
				fsj.runC(null, uddsBag2, null);
				float ftp20F_Bag2_miles = fsj.lastTripSummary().miles;
				float ftp20F_Bag2_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-3 //Thermal modules would use Ambient Temperature 20F, Vehicle & Engine left to cool off for a period of ftpWaitBag2ToBag3 seconds
				fsj.runC(null, uddsBag1, null);
				float ftp20F_Bag3_miles = fsj.lastTripSummary().miles;
				float ftp20F_Bag3_gal = fsj.lastTripSummary().fuelUse;
				
				
				
				//THIRD TEST: Run US06
				//City Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
				fsj.runC(null, us06City1, null);
				float us06_city_miles = fsj.lastTripSummary().miles;
				float us06_city_gal = fsj.lastTripSummary().fuelUse;
				
				//Highway Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
				fsj.runC(null, us06Highway, null);
				float us06_highway_miles = fsj.lastTripSummary().miles;
				float us06_highway_gal = fsj.lastTripSummary().fuelUse;
				
				fsj.runC(null, us06City2, null);
				us06_city_miles += fsj.lastTripSummary().miles;
				us06_city_gal += fsj.lastTripSummary().fuelUse;
				
				
				//FOURTH TEST: Run HWFET
				//City Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
				fsj.runC(null, hwfetFull, null);
				float hwfet_miles = fsj.lastTripSummary().miles;
				float hwfet_gal = fsj.lastTripSummary().fuelUse;
				
				
				
				//FIFTH TEST: Run SC03
				//City Portion //Thermal modules would use Ambient Temperature 95 (with AC on) and warmed up engine
				fsj.runC(null, sc03Full, null);
				float sc03_miles = fsj.lastTripSummary().miles;
				float sc03_gal = fsj.lastTripSummary().fuelUse;
				
				
				//CITY Rating Calculation
				float startFuel75 = 3.6f*((ftp75F_Bag1_gal/ftp75F_Bag1_miles) - (ftp75F_Bag3_gal/ftp75F_Bag3_miles)) +
						3.9f*((ftp75F_Bag2_gal/ftp75F_Bag2_miles) - (ftp75F_Bag4_gal/ftp75F_Bag4_miles));
				float startFuel20 = 3.6f*((ftp20F_Bag1_gal/ftp20F_Bag1_miles) - (ftp20F_Bag3_gal/ftp20F_Bag3_miles));
				
				float startFC = 0.33f*(0.76f*startFuel75 + 0.24f*startFuel20)/4.1f;
				float runningFC = 0.82f*(0.48f*ftp75F_Bag4_gal/ftp75F_Bag4_miles + 0.41f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.11f*us06_city_gal/us06_city_miles) +
						0.18f*(0.5f*ftp20F_Bag2_gal/ftp20F_Bag2_miles + 0.5f*ftp20F_Bag3_gal/ftp20F_Bag3_miles) + 
						0.133f*1.083f*(sc03_gal/sc03_miles - (0.61f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.39f*ftp75F_Bag4_gal/ftp75F_Bag4_miles));
				
				res.city.mpg = 0.905f/(startFC + runningFC);
				
				
				//HIGHWAY Rating Calculation
				startFC = 0.33f*(0.76f*startFuel75 + 0.24f*startFuel20)/60f;
				runningFC = 1.007f*(0.79f*us06_highway_gal/us06_highway_miles + 0.21f*hwfet_gal/hwfet_miles) + 
						0.133f*0.377f*(sc03_gal/sc03_miles - (0.61f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.39f*ftp75F_Bag4_gal/ftp75F_Bag4_miles));
				
				res.highway.mpg = 0.905f/(startFC + runningFC);
				
				//COMBINED Rating Calculation
				res.combined.mpg = 1f/(0.55f/res.city.mpg + 0.45f/res.highway.mpg);
			}
			break;
		case bev:
		{
			//Using the 0.7 factor method and one run of FTP + HWFET -- No Thermal Effects Correction seem to be employed or needed as of 2017 standard 
			fsj.setRelSoC(1f);
			fsj.runC(null, ftpFull, null);
			float ftp_miles = fsj.lastTripSummary().miles;
			float ftp_kWh = fsj.lastTripSummary().batteryUse;
			
			fsj.setRelSoC(1f);
			fsj.runC(null, hwfetFull, null);
			float hwfet_miles = fsj.lastTripSummary().miles;
			float hwfet_kWh = fsj.lastTripSummary().batteryUse;
			
			res.city.kwhpm = ftp_kWh/(ftp_miles*0.7f*chargerEfficiency);
			res.highway.kwhpm = hwfet_kWh/(hwfet_miles*0.7f*chargerEfficiency);
			res.combined.kwhpm = 0.55f*res.city.kwhpm + 0.45f*res.highway.kwhpm;
		}
			break;
		case phev:
		{
			//CHARGE DEPLETION MODE: Using the 0.7 factor method and one run of FTP + HWFET -- No Thermal Effects Correction seem to be employed or needed as of 2017 standard 
			fsj.setRelSoC(1f);
			fsj.runC(null, ftpFull, null);
			float ftp_miles = fsj.lastTripSummary().miles;
			float ftp_kWh = fsj.lastTripSummary().batteryUse;
			
			fsj.setRelSoC(1f);
			fsj.runC(null, hwfetFull, null);
			float hwfet_miles = fsj.lastTripSummary().miles;
			float hwfet_kWh = fsj.lastTripSummary().batteryUse;
			
			res.city.kwhpm = ftp_kWh/(ftp_miles*0.7f*chargerEfficiency);
			res.highway.kwhpm = hwfet_kWh/(hwfet_miles*0.7f*chargerEfficiency);
			res.combined.kwhpm = 0.55f*res.city.kwhpm + 0.45f*res.highway.kwhpm;
						
			if (vehModel.fuelConv.fcEffType == FuelConverterEffType.fuelCell) {	//Fuel Cell Hybrid
				//Using the 0.7 factor method and one run of FTP + HWFET -- No Thermal Effects Correction seem to be employed or needed as of 2017 standard 
				fsj.setRelSoC(0f);
				fsj.runC(null, ftpFull, null);
				float ftp_csmiles = fsj.lastTripSummary().miles;
				float ftp_kgH2 = fsj.lastTripSummary().fuelUse;
				
				fsj.setRelSoC(0f);
				fsj.runC(null, hwfetFull, null);
				float hwfet_csmiles = fsj.lastTripSummary().miles;
				float hwfet_kgH2 = fsj.lastTripSummary().fuelUse;
				
				res.city.mpg = (ftp_csmiles*0.7f)/ftp_kgH2;
				res.highway.mpg = (hwfet_csmiles*0.7f)/hwfet_kgH2;
				res.combined.mpg = 1f/(0.55f/res.city.mpg + 0.45f/res.highway.mpg);
			} 
			else 
			{
				//CHARGE SUSTAINING MODE: Same as Hybrids
				//FIRST TEST: Run the "normal temperature FTP"
				fsj.setRelSoC(0f);
				//Bag-1 //Thermal modules would use Ambient Temperature 75F and cold engine start
				fsj.runC(null, uddsBag1, null);
				float ftp75F_Bag1_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag1_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-2 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 75F
				fsj.runC(null, uddsBag2, null);
				float ftp75F_Bag2_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag2_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-3 //Thermal modules would use Ambient Temperature 75F, Vehicle & Engine left to cool off for a period of ftpWaitBag2ToBag3 seconds
				fsj.runC(null, uddsBag1, null);
				float ftp75F_Bag3_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag3_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-4 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 75F
				fsj.runC(null, uddsBag2, null);
				float ftp75F_Bag4_miles = fsj.lastTripSummary().miles;
				float ftp75F_Bag4_gal = fsj.lastTripSummary().fuelUse;
				
				
				
				//SECOND TEST: Run the "Cold temperature FTP"
				fsj.setRelSoC(0f);
				//Bag-1 //Thermal modules would use Ambient Temperature 20F and cold engine start
				fsj.runC(null, uddsBag1, null);
				float ftp20F_Bag1_miles = fsj.lastTripSummary().miles;
				float ftp20F_Bag1_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-2 //(right after, no modifiers to engine cool-off), Thermal modules would use Ambient Temperature 20F
				fsj.runC(null, uddsBag2, null);
				float ftp20F_Bag2_miles = fsj.lastTripSummary().miles;
				float ftp20F_Bag2_gal = fsj.lastTripSummary().fuelUse;
				
				//Bag-3 //Thermal modules would use Ambient Temperature 20F, Vehicle & Engine left to cool off for a period of ftpWaitBag2ToBag3 seconds
				fsj.runC(null, uddsBag1, null);
				float ftp20F_Bag3_miles = fsj.lastTripSummary().miles;
				float ftp20F_Bag3_gal = fsj.lastTripSummary().fuelUse;
				
				
				
				//THIRD TEST: Run US06
				//City Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
				fsj.setRelSoC(0f);
				fsj.runC(null, us06City1, null);
				float us06_city_miles = fsj.lastTripSummary().miles;
				float us06_city_gal = fsj.lastTripSummary().fuelUse;
				
				//Highway Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
				fsj.runC(null, us06Highway, null);
				float us06_highway_miles = fsj.lastTripSummary().miles;
				float us06_highway_gal = fsj.lastTripSummary().fuelUse;
				
				fsj.runC(null, us06City2, null);
				us06_city_miles += fsj.lastTripSummary().miles;
				us06_city_gal += fsj.lastTripSummary().fuelUse;
				
				
				//FOURTH TEST: Run HWFET
				fsj.setRelSoC(0f);
				//City Portion //Thermal modules would use Ambient Temperature 75 and warmed up engine
				fsj.runC(null, hwfetFull, null);
				hwfet_miles = fsj.lastTripSummary().miles;
				float hwfet_gal = fsj.lastTripSummary().fuelUse;
				
				
				
				//FIFTH TEST: Run SC03
				fsj.setRelSoC(0f);
				//City Portion //Thermal modules would use Ambient Temperature 95 (with AC on) and warmed up engine
				fsj.runC(null, sc03Full, null);
				float sc03_miles = fsj.lastTripSummary().miles;
				float sc03_gal = fsj.lastTripSummary().fuelUse;
				
				
				//CITY Rating Calculation
				float startFuel75 = 3.6f*((ftp75F_Bag1_gal/ftp75F_Bag1_miles) - (ftp75F_Bag3_gal/ftp75F_Bag3_miles)) +
						3.9f*((ftp75F_Bag2_gal/ftp75F_Bag2_miles) - (ftp75F_Bag4_gal/ftp75F_Bag4_miles));
				float startFuel20 = 3.6f*((ftp20F_Bag1_gal/ftp20F_Bag1_miles) - (ftp20F_Bag3_gal/ftp20F_Bag3_miles));
				
				float startFC = 0.33f*(0.76f*startFuel75 + 0.24f*startFuel20)/4.1f;
				float runningFC = 0.82f*(0.48f*ftp75F_Bag4_gal/ftp75F_Bag4_miles + 0.41f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.11f*us06_city_gal/us06_city_miles) +
						0.18f*(0.5f*ftp20F_Bag2_gal/ftp20F_Bag2_miles + 0.5f*ftp20F_Bag3_gal/ftp20F_Bag3_miles) + 
						0.133f*1.083f*(sc03_gal/sc03_miles - (0.61f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.39f*ftp75F_Bag4_gal/ftp75F_Bag4_miles));
				
				res.city.mpg = 0.905f/(startFC + runningFC);
				
				
				//HIGHWAY Rating Calculation
				startFC = 0.33f*(0.76f*startFuel75 + 0.24f*startFuel20)/60f;
				runningFC = 1.007f*(0.79f*us06_highway_gal/us06_highway_miles + 0.21f*hwfet_gal/hwfet_miles) + 
						0.133f*0.377f*(sc03_gal/sc03_miles - (0.61f*ftp75F_Bag3_gal/ftp75F_Bag3_miles + 0.39f*ftp75F_Bag4_gal/ftp75F_Bag4_miles));
				
				res.highway.mpg = 0.905f/(startFC + runningFC);
				
				//COMBINED Rating Calculation
				res.combined.mpg = 1f/(0.55f/res.city.mpg + 0.45f/res.highway.mpg);
			}
		}
			break;
		}
		
		return res;
	}
	
	public static float oldDeratingFormula_highwayMPG(float simMPG) {
		float hwyIntercept = 0.001376f;
		float hwySlope = 1.3466f;
		return 1f/(hwyIntercept + (hwySlope/simMPG));
	}
	public static float oldDeratingFormula_highwayKWHPM(float simKWHPM) {
		float kWhPerGGE = 33.7f;
		float simMPG = 1f/(simKWHPM/kWhPerGGE);
		float hwyIntercept = 0.001376f;
		float hwySlope = 1.3466f;
		float deratedMPG = 1f/(hwyIntercept + (hwySlope/simMPG));
		return (1f/deratedMPG)*kWhPerGGE;
	}
	public static float oldDeratingFormula_cityMPG(float simMPG) {
		float cityIntercept = 0.003259f;
		float citySlope = 1.1805f;
		return 1f/(cityIntercept + (citySlope/simMPG)); 
	}
	public static float oldDeratingFormula_cityKWHPM(float simKWHPM) {
		float kWhPerGGE = 33.7f;
		float simMPG = 1f/(simKWHPM/kWhPerGGE);
		float cityIntercept = 0.003259f;
		float citySlope = 1.1805f;
		float deratedMPG = 1f/(cityIntercept + (citySlope/simMPG)); 
		return (1f/deratedMPG)*kWhPerGGE;
	}
	public static float oldDeratingCombFormula(float deratedCityMPG, float deratedHwyMPG) {
		return 1f/((0.55f/deratedCityMPG) + (0.45f/deratedHwyMPG));
	}
	public static float oldDeratingCombFormulaKWHPM(float deratedCityKWHPM, float deratedHwyKWHPM) {
		return 0.55f*deratedCityKWHPM + 0.45f*deratedHwyKWHPM;
	}
	public static float mpg_to_kmPerLitre(float mpg) {
		return 0.001f*mpg*FSJSimConstants.metersPerMile/FSJSimConstants.litrePerGal;
	}
	
	public static class FuelEconomyRating {
		private float mpg, kwhpm;
		
		public float mpg() {return mpg;}
		public float kwhpm() {return kwhpm;}
		
		private FuelEconomyRating() {
			mpg = -1f;
			kwhpm = -1f;
		}
	}
	
	public static class FiveCycleTestResult {
		private FuelEconomyRating city, highway, combined;
		
		public FuelEconomyRating city() {return city;}
		public FuelEconomyRating highway() {return highway;}
		public FuelEconomyRating combined() {return combined;}
		
		private FiveCycleTestResult() {
			city = new FuelEconomyRating();
			highway = new FuelEconomyRating();
			combined = new FuelEconomyRating();
		}
	}
}
