package fastsimjava.components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FSJSimConstants {
	public float airDensity,gravity,h2KWhPerKg,kWhPerGGE,kWhPerGalDiesel,gasKWhPerKg,dieselKWhPerKg,cngKWhPerM3;
	public float refAmbTempC,refAtmPressureBar;
	
	public static final float mphPerMps = 2.2369f; 		//Miles per hour / meters per second
	public static final float metersPerMile = 1609;		//Meters per mile
	public static final float inchesPerMeter = 39.37f;	//Inches per meter
	public static final float litrePerGal = 3.78541f;	//Liters per Gallon
	
	public static float litrePer100km(float milesPerGal) {
		float gpm = 1f/milesPerGal;
		return gpm*litrePerGal*100000f/metersPerMile;
	}
	

	//Default constructor
	public FSJSimConstants() {
		airDensity = 1.2f;			//kg/m3 (slightly different from exact air density of 1.2041 at standard temperature of 20c and 1atm = 101.325 kPa)
		gravity = 9.81f;			//m/s2
		
		h2KWhPerKg = FuelCalcConstants.Default_h2KWhPerKg;
		kWhPerGGE = FuelCalcConstants.Default_kWhPerGGE;
		kWhPerGalDiesel = FuelCalcConstants.Default_kWhPerGalDiesel;
		cngKWhPerM3 = FuelCalcConstants.Default_cngKWhPerM3;
		
		gasKWhPerKg = 13.1f;		//kWh per kilogram of Gasoline
		dieselKWhPerKg = 12.61f;	//kWh per kilogram of Diesel
		
		refAmbTempC = 20f;				//Degrees Celsius
		refAtmPressureBar = 1.0098f;	//Bar
	}
	//Constructor via CSV file
	public FSJSimConstants(String fname) {
		readFromCSVFile(fname);
	}
	//Function to write contents to CSV file
	public void writeToCSVFile(String fname) {
		try {
			String lsep = System.getProperty("line.separator");
			FileWriter fWriter = new FileWriter(fname);
		
			fWriter.append(headerString());
			fWriter.append(lsep);
			
			fWriter.append(toString());
			fWriter.append(lsep);
			
			fWriter.flush();
			fWriter.close();
		} catch (IOException e) {}
	}
	//Function to read contents from CSV file
	public void readFromCSVFile(String fname) {
		try {
			BufferedReader readingBuffer=new BufferedReader(new FileReader(fname));
			String readLine = readingBuffer.readLine();
			readLine = readingBuffer.readLine();
			parseFromString(readLine);		
			readingBuffer.close();
		} catch (IOException e) {}
	}
	//Header string
	public static String headerString() {
		return "airDensity,gravity,h2KWhPerKg,kWhPerGGE,kWhPerGalDiesel,gasKWhPerKg,dieselKWhPerKg,refAmbTempC,refAtmPressureBar";
	}
	//Form Equivalent string
	@Override public String toString() {
		return ""+airDensity+","+gravity+","+h2KWhPerKg+","+kWhPerGGE+","+kWhPerGalDiesel+","+
				gasKWhPerKg+","+dieselKWhPerKg+","+refAmbTempC+","+refAtmPressureBar;
	}
	//Function to parse values from equivalent string
	public void parseFromString(String readLine) {
		String[] strSplit = readLine.split(",");
		
		airDensity = Float.parseFloat(strSplit[0]);
		gravity = Float.parseFloat(strSplit[1]);
		h2KWhPerKg = Float.parseFloat(strSplit[2]);
		kWhPerGGE = Float.parseFloat(strSplit[3]);
		kWhPerGalDiesel = Float.parseFloat(strSplit[4]);
		gasKWhPerKg = Float.parseFloat(strSplit[5]);
		dieselKWhPerKg = Float.parseFloat(strSplit[6]);
		refAmbTempC = Float.parseFloat(strSplit[7]);
		refAtmPressureBar = Float.parseFloat(strSplit[8]);
	}
}
