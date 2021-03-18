package fastsimjava.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;


public class SVehicleSample {
	//Constants
	private static final float metersPerMile = 1609f;
	private static final float secondsPerHour = 3600f;
	
	//Vehicle Sample Identification Info
	private SampleID vehSampleInfo;
	public SampleID vehSampleInfo() {return vehSampleInfo;}
	
	//Array of Trips
	private Trip[] trips;
	public Trip[] trips() {return trips;}
	
	//Utility function to return the number of drive days
	public int numDriveDays() {return trips[trips.length-1].tripIDs.dayID + 1;}
	//Utility function to extract an array of trips on a given day
	public Trip[] getDayTrips(int dayID) {
		ArrayList<Trip> lst = new ArrayList<Trip>();
		for (int i=0; i<trips.length; i++) {
			if (trips[i].tripIDs.dayID == dayID) lst.add(trips[i]);
		}
		Trip[] arr = new Trip[lst.size()];
		for (int i=0; i<arr.length; i++) arr[i] = lst.get(i);
		return arr;
	}
	
	//Static functions to read from file and return instance(s) of this class
	public static SVehicleSample[] readFromFile(String fileName) {
		try {
			ArrayList<SVehicleSample> lst = new ArrayList<SVehicleSample>();
			BufferedReader fin = new BufferedReader(new FileReader(fileName));
			
			SVehicleSample curSample = new SVehicleSample(fin);
			lst.add(curSample);
			
			int sampleWinUpdatePeriod = 50;
			int samplesCount = 0;
			
			while (curSample!=null) {
				curSample = null;
				try {
					curSample = new SVehicleSample(fin);
					lst.add(curSample);
				} catch (Exception e) {}
				
				samplesCount++;
				if (samplesCount%sampleWinUpdatePeriod == 0) System.out.println(""+samplesCount+" samples read...");
				/*
				if (samplesCount%sampleWinUpdatePeriod == 0) stWin.println(""+samplesCount+" samples read...");					
				if (stWin.abortRequested()) {
					fin.close();
					return null;
				} */
			}
					
			fin.close();
			
			SVehicleSample[] arr = new SVehicleSample[lst.size()];
			for (int i=0; i<arr.length; i++) arr[i] = lst.get(i);
			
			return arr;
		} catch (Exception e) {
			return null;
		}
	}

	//Private constructor to prevent direct instantiation -- Use one of the static functions to read from file
	private SVehicleSample(BufferedReader fin) throws Exception {
		String readLine = fin.readLine();			
		readLine = fin.readLine();
		vehSampleInfo = new SampleID(readLine);
		
		trips = new Trip[vehSampleInfo.nTrips];
		for (int i=0; i<trips.length; i++) {
			trips[i] = new Trip(fin);
		}
	}
	
	
	public static SVehicleSample readFromFile(String fileName, int sampleIDinFile) {
		try {
			BufferedReader fin = new BufferedReader(new FileReader(fileName));

			SVehicleSample curSample = null;
			for (int i=0; i<=sampleIDinFile; i++) {
				curSample = new SVehicleSample(fin);
			}
			
			fin.close();
			return curSample;

		} catch (Exception e) {
			return null;
		}
	}
	public static SVehicleSample readFromFile(String fileName, int householdID, int vehIDinHousehold) {
		try {
			BufferedReader fin = new BufferedReader(new FileReader(fileName));
			SVehicleSample curSample = new SVehicleSample(fin);
			
			while (!curSample.vehSampleInfo.isSameSample(householdID, vehIDinHousehold)) {
				curSample = new SVehicleSample(fin);
			}
			
			fin.close();
			return curSample;
			
		} catch (Exception e) {
			return null;
		}
	}
	
	
	//Function to Write Vehicle Sample into a file
	public void writeToFile(String fileName) {
		try {
			FileWriter fout = new FileWriter(fileName);
			String lsep = System.getProperty("line.separator");
			
			fout.append("VehicleSample__hhID_vehIDinHH_hhWt_nTrips"+lsep);
			fout.append(vehSampleInfo.toString()+lsep);
			
			for (int i=0; i<trips.length; i++) {
				trips[i].writeIntoFile(fout);
			}
			
			fout.flush();
			fout.close();
		} catch (Exception e) {}
	}
	
	//Class for Sample Identification
	public static class SampleID {
		//Household ID, Vehicle ID within Household and number of Trips
		public int hhID, vehIDinHH, nTrips;
		//Household Weight
		public float hhWt;
		
		//Function for checking if ID matches some input
		public boolean isSameSample(int householdID, int vehIDinHousehold) {
			if (hhID!=householdID) return false;
			if (vehIDinHH!=vehIDinHousehold) return false;
			return true;
		}
		//Constructor from reading a line from CSV file
		private SampleID(String readLine) {
			String[] sp = readLine.split(",");
			hhID = Integer.parseInt(sp[0]);
			vehIDinHH = Integer.parseInt(sp[1]);
			hhWt = Float.parseFloat(sp[2]);
			nTrips = Integer.parseInt(sp[3]);
		}	
		//Forming a line String
		@Override public String toString() {
			return ""+hhID+","+vehIDinHH+","+hhWt+","+nTrips;
		}
	}

	//Class for Trip Identifiers
	public static class TripIDs {
		//Trip ID in Vehicle Sample, Day ID, Trip ID within given Day
		public int gID,dayID,idInDay;
		//Duration (sec) from end of last trip, number of recorded time steps
		public int secsFromLastTrip,numRecSteps;
		//Trip distance (miles)
		public float miles;
		//Date and Local Time for Trip Start
		public int year,month,day,hr24,min,sec;
		//Trip identification flag
		public TripIsPublic isTripPublic;

		//Constructor from reading a line from CSV file
		private TripIDs(String readLine) {
			String[] sp = readLine.split(",");
			
			gID = Integer.parseInt(sp[0]);
			dayID = Integer.parseInt(sp[1]);
			idInDay = Integer.parseInt(sp[2]);
			
			secsFromLastTrip = Integer.parseInt(sp[3]);
			numRecSteps = Integer.parseInt(sp[4]);
			miles = Float.parseFloat(sp[5]);
			
			year = Integer.parseInt(sp[6]);
			month = Integer.parseInt(sp[7]);
			day = Integer.parseInt(sp[8]);
			
			hr24 = Integer.parseInt(sp[9]);
			min = Integer.parseInt(sp[10]);
			sec = Integer.parseInt(sp[11]);
			
			if (sp.length > 12) {
				isTripPublic = TripIsPublic.decode(sp[12]);
			} else {
				isTripPublic = TripIsPublic.unknown;
			}
		}
		
		//Forming a line String
		@Override public String toString() {
			return ""+gID+","+dayID+","+idInDay+","+secsFromLastTrip+","+numRecSteps+","+miles+","+year+","+month+","+day+","+hr24+","+min+","+sec+","+isTripPublic.toString();
		}		
	}
	
	//Class for Trip
	public static class Trip {
		//Trip Identifiers
		private TripIDs tripIDs;
		public TripIDs tripIDs() {return tripIDs;}
		
		//Speed in MPH
		private float[] speedMPH;
		public float[] speedMPH() {return speedMPH;}
		
		//Filtered road grade
		private float[] fltGrade;
		public float[] fltGrade() {return fltGrade;}

		//Constructor via reading a chunk from a file
		private Trip (BufferedReader fin) throws Exception {
			String readLine = fin.readLine();			
			readLine = fin.readLine();
			tripIDs = new TripIDs(readLine);
			readLine = fin.readLine();
			
			speedMPH = new float[tripIDs.numRecSteps];
			fltGrade = new float[tripIDs.numRecSteps];
			for (int i=0; i<speedMPH.length; i++) {
				readLine = fin.readLine();
				String[] sp = readLine.split(",");
				speedMPH[i] = Float.parseFloat(sp[0]);
				fltGrade[i] = Float.parseFloat(sp[1]);
			}
		}
		//Function for writing the trip to file
		private void writeIntoFile(FileWriter fout) throws Exception {
			String lsep = System.getProperty("line.separator");
			fout.append("Trip__gID_dayID_idInDay_secsFromLastTrip_numRecSteps_miles_year_month_day_hr24_min_sec"+lsep);
			fout.append(tripIDs.toString()+lsep);
			
			fout.append("Trip__speedMPH_grade"+lsep);
			for (int i=0; i<speedMPH.length; i++) {
				fout.append(""+speedMPH[i]+","+fltGrade[i]+lsep);
			}
		}

		public void saveFormattedToFile(String outputFileName) {
			try {
				FileWriter fout = new FileWriter(outputFileName);
				String lsep = System.getProperty("line.separator");
				
				fout.append("timeSec,speedMPH,roadGrade,altChangeM"+lsep);
				
				float prevSpeed = speedMPH[0];
				float prevGrade = fltGrade[0];
				float curAlt = 0f;
				String stOut = ""+0+","+prevSpeed+","+prevGrade+","+curAlt+lsep;
				fout.append(stOut);
				
				for (int i=1; i<speedMPH.length; i++) {
					float curSpeed = speedMPH[i];
					float curGrade = fltGrade[i];
					
					float deltaDistMeters = 0.5f*(prevSpeed+curSpeed)*metersPerMile/secondsPerHour;
					float avGrade = 0.5f*(prevGrade + curGrade);
					curAlt += avGrade*deltaDistMeters;
					
					stOut = ""+i+","+curSpeed+","+curGrade+","+curAlt+lsep;
					fout.append(stOut);
					
					prevSpeed = curSpeed;
					prevGrade = curGrade;
				}				
				
				fout.flush();
				fout.close();
			} catch (Exception e) {}			
		}
	}
	
	//Enum for public identifyability flag of a trip
	public static enum TripIsPublic {
		isPublic, notPublic, unknown;
		private TripIsPublic() {}
		public static TripIsPublic decode(String spItem) {
			if (spItem==null) return unknown;
			if (spItem.equalsIgnoreCase(isPublic.toString())) return isPublic;
			if (spItem.equalsIgnoreCase(notPublic.toString())) return notPublic;
			return unknown;
		}
	}
}
