package fastsimjava.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;


public class VehicleSampleMA {
	
	private static final String Header_newVehicleSample = "____VehicleSample__";
	private static final String Header_newTrip = "___Trip_";
	private static final String Header_SampleID = "hhID,vehIDinHH,hhWt,nTrips";
	private static final String Header_TripIDs = "gID,dayID,idInDay,secsFromLastTrip,numRecSteps,miles,year,month,day,hr24,min,sec,numPayloadAdjust";
	private static final String Header_PayloadAdjustment = "tStepID,payloadKg";
	private static final String Header_TripSecbySec = "speedMPH,fltGrade,recAuxKW";
	
	private static final int NumDaysPerYear = 365;
	private static final int NumDaysPerYearLY = 366;
	private static final int[] NumDaysPerMonth = {31,28,31, 30,31,30, 31,31,30, 31,30,31};
	private static final int[] NumDaysPerMonthLY = {31,29,31, 30,31,30, 31,31,30, 31,30,31};
	private static final int NumHoursPerDay = 24;
	private static final int NumMinutesPerHour = 60;
	private static final int NumSecondsPerMinute = 60;
	private static final int NumSecondsPerHour = NumMinutesPerHour*NumSecondsPerMinute;
	private static final int NumSecondsPerDay = NumSecondsPerHour*NumHoursPerDay;
	private static final int daysInYearUntilEndOfPrevMonth(int month, int[] daysPerMonthForYear) {
		int days = 0;
		for (int i=0; i<(month-1); i++) days += daysPerMonthForYear[i];
		return days;
	}

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
	
	
	//Function for reading array from file
	public static VehicleSampleMA[] readArrayFromFile(String fname) {
		ArrayList<VehicleSampleMA> lst = new ArrayList<VehicleSampleMA>();
		
		try {
			BufferedReader fin = new BufferedReader(new FileReader(fname));

			int updatePeriod = 50;
			int samplesCount = 0;

			while (true) {
				try {
					VehicleSampleMA vs = new VehicleSampleMA(fin);
					lst.add(vs);
				} catch (Exception e2) {
					break;
				}
				
				samplesCount++;
				if (samplesCount%updatePeriod == 0) System.out.println(""+samplesCount+" samples read...");
				/*
				if (samplesCount%updatePeriod == 0) stWin.println(""+samplesCount+" samples read...");					
				if (stWin.abortRequested()) {
					fin.close();
					return null;
				} */
			}
			
			fin.close();
		} catch (Exception e) {}
		
		VehicleSampleMA[] arr = new VehicleSampleMA[lst.size()];
		for (int i=0; i<arr.length; i++) arr[i] = lst.get(i);
		return arr;
	}
	
	
	//Default constructor
	private VehicleSampleMA() {}
	//Private constructor via reading from file
	private VehicleSampleMA(BufferedReader fin) throws Exception {
		String readLine = fin.readLine();
		readLine = fin.readLine();
		
		readLine = fin.readLine();
		vehSampleInfo = new SampleID(readLine);
		
		trips = new Trip[vehSampleInfo.nTrips];
		for (int i=0; i<trips.length; i++) trips[i] = new Trip(fin);
	}

	//Static function to write array to file
	public static void writeArrayToFile(String fileName, VehicleSampleMA[] vehSamples) {
		try {
			FileWriter fout = new FileWriter(fileName);
			
			for (int i=0; i<vehSamples.length; i++) {
				vehSamples[i].writeIntoFile(fout);
				fout.flush();
			}
			
			fout.close();
		} catch (Exception e) {}
	}
	public void writeIntoFile(FileWriter fout) throws Exception {
		String lsep = System.getProperty("line.separator");

		fout.append(Header_newVehicleSample+lsep);
		fout.append(Header_SampleID+lsep);
		fout.append(vehSampleInfo.toString()+lsep);

		for (int i=0; i<trips.length; i++) trips[i].writeIntoFile(fout);
	}
	
	
	public static class SampleID {
		//Household ID, Vehicle ID within Household and number of Trips
		public int hhID, vehIDinHH, nTrips;
		//Household Weight
		public float hhWt;

		//Default constructor
		private SampleID() {}
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
	
	public static class TripIDs {
		//Trip ID in Vehicle Sample, Day ID, Trip ID within given Day
		public int gID,dayID,idInDay;
		//Duration (sec) from end of last trip, number of recorded time steps
		public int secsFromLastTrip,numPayloadAdjust,numRecSteps;
		//Trip distance (miles)
		public float miles;
		//Date and Local Time for Trip Start
		public int year,month,day,hr24,min,sec;

		//Default constructor
		private TripIDs() {}
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
			numPayloadAdjust = Integer.parseInt(sp[12]);
		}
		
		//Forming a line String
		@Override public String toString() {
			return ""+gID+","+dayID+","+idInDay+","+secsFromLastTrip+","+numRecSteps+","+miles+","+year+","+month+","+day+","+hr24+","+min+","+sec+","+numPayloadAdjust;
		}		
		
		public int secFrom2k() {
			int daysUpToCurYear = 0;
			for (int i=2000; i<year; i++) {
				if (i%4 == 0) daysUpToCurYear += NumDaysPerYearLY;
				else daysUpToCurYear += NumDaysPerYear;
			}
			
			int numDays = daysUpToCurYear;
			if (year % 4 == 0) numDays += daysInYearUntilEndOfPrevMonth(month, NumDaysPerMonthLY);
			else numDays += daysInYearUntilEndOfPrevMonth(month, NumDaysPerMonth);
			
			numDays += (day-1);
			return numDays*NumSecondsPerDay + hr24*NumSecondsPerHour + min*NumSecondsPerMinute + sec;
		}
		public static int deltaSecStartTime_twoMinusOne(TripIDs t2, TripIDs t1) {
			return t2.secFrom2k() - t1.secFrom2k();
		}
	}
	
	public static class AdditionalPayload {
		public int tStepID;
		public float payloadKg;
		
		private AdditionalPayload() {}
		public AdditionalPayload(String readLine) {
			String[] sp = readLine.split(",");
			tStepID =  Integer.parseInt(sp[0]);
			payloadKg =  Float.parseFloat(sp[1]);
		}
		
		//Forming a line String
		@Override public String toString() {
			return ""+tStepID+","+payloadKg;
		}		
	}
	public static float payloadAtTimeStep(AdditionalPayload[] payloadsInfo, int tID) {
		if (tID < payloadsInfo[0].tStepID) return 0f;

		float lastpayload = payloadsInfo[payloadsInfo.length-1].payloadKg;
		for (int i=payloadsInfo.length-1; i>0; i++) {
			if (tID >= payloadsInfo[i].tStepID) break;
			lastpayload = payloadsInfo[i-1].payloadKg;
		}
		return lastpayload;
	}
	public static float[] payload1HzTimeSeries(AdditionalPayload[] payloadsInfo, int arrLength) {
		if (payloadsInfo == null) return null;
		
		float[] payloadKg = new float[arrLength];
		if (payloadsInfo.length < 1) return payloadKg;
		
		int curPayloadEndID = payloadKg.length - 1;
		int curPayloadID = payloadsInfo.length-1;
		float curPayload = payloadsInfo[curPayloadID].payloadKg;
		int curPayloadStartID = payloadsInfo[curPayloadID].tStepID;
		
		for (int i=curPayloadEndID; i>=curPayloadStartID; i--) payloadKg[i] = curPayload;
		
		while (curPayloadID > 0) {
			curPayloadID--;
			curPayloadEndID = curPayloadStartID - 1;
			curPayload = payloadsInfo[curPayloadID].payloadKg;
			curPayloadStartID = payloadsInfo[curPayloadID].tStepID;
			for (int i=curPayloadEndID; i>=curPayloadStartID; i--) payloadKg[i] = curPayload;
		}			
		
		return payloadKg;
	}
	
	//Class for Trip
	public static class Trip {
		//Trip Identifiers
		private TripIDs tripIDs;
		public TripIDs tripIDs() {return tripIDs;}
		
		//Payload Adjustments
		private AdditionalPayload[] payloadAdjust;
		public AdditionalPayload[] payloadAdjust() {return payloadAdjust;}
		
		//Speed in MPH
		private float[] speedMPH;
		public float[] speedMPH() {return speedMPH;}
		
		//Filtered road grade
		private float[] fltGrade;
		public float[] fltGrade() {return fltGrade;}
		
		//Recorded Auxiliary Power in kW
		private float[] recAuxKW;
		public float[] recAuxKW() {return recAuxKW;}
		
		//Default constructor
		private Trip() {}
		//Constructor via reading a chunk from a file
		private Trip (BufferedReader fin) throws Exception {
			String readLine = fin.readLine();		//Skip header of Trip	
			
			readLine = fin.readLine();	//Skip header of Trip IDs
			readLine = fin.readLine();	//Data of Trip IDs
			
			tripIDs = new TripIDs(readLine);
			
			payloadAdjust = new AdditionalPayload[tripIDs.numPayloadAdjust];
			speedMPH = new float[tripIDs.numRecSteps];
			fltGrade = new float[tripIDs.numRecSteps];
			recAuxKW = new float[tripIDs.numRecSteps];
						
			readLine = fin.readLine();	//Skip header of payload adjustment
			for (int i=0; i<payloadAdjust.length; i++) {
				readLine = fin.readLine();
				payloadAdjust[i] = new AdditionalPayload(readLine);
			}
			
			readLine = fin.readLine();	//Skip header of sec-by-sec			
			for (int i=0; i<speedMPH.length; i++) {
				readLine = fin.readLine();
				String[] sp = readLine.split(",");
				speedMPH[i] = Float.parseFloat(sp[0]);
				fltGrade[i] = Float.parseFloat(sp[1]);
				recAuxKW[i] = Float.parseFloat(sp[2]);
			}
		}
		//Function for writing the trip to file
		private void writeIntoFile(FileWriter fout) throws Exception {
			String lsep = System.getProperty("line.separator");
			
			fout.append(Header_newTrip+lsep);
			fout.append(Header_TripIDs+lsep);
			fout.append(tripIDs.toString()+lsep);
			
			fout.append(Header_PayloadAdjustment+lsep);
			for (int i=0; i<payloadAdjust.length; i++) {
				fout.append(payloadAdjust[i].toString()+lsep);
			}
			
			fout.append(Header_TripSecbySec+lsep);
			for (int i=0; i<speedMPH.length; i++) {
				fout.append(""+speedMPH[i]+","+fltGrade[i]+","+recAuxKW[i]+lsep);
			}
		}
	}
	
	public static VehicleSampleMA utl_formFromLimitedInfo_type1(int hhID, int vehIDinHH, float hhWt, float[][] speedMPH, String[] start_dayHrMin, float payloadKg) {
		VehicleSampleMA vs = new VehicleSampleMA();
		
		vs.vehSampleInfo = new SampleID();
		vs.vehSampleInfo.hhID = hhID;
		vs.vehSampleInfo.vehIDinHH = vehIDinHH;
		vs.vehSampleInfo.nTrips = speedMPH.length;	//First index on number of trips
		vs.vehSampleInfo.hhWt = hhWt;
		
		vs.trips = new Trip[vs.vehSampleInfo.nTrips];
		
		int refYear = 2001;
		long prevTripStart_secFrom2001 = 0;

		for (int i=0; i<vs.trips.length; i++) {
			String[] spDT = start_dayHrMin[i].split(" ");
			String[] spD = spDT[0].split("/");
			String[] spT = spDT[1].split(":");
			
			int month = Integer.parseInt(spD[0]);
			int day = Integer.parseInt(spD[1]);
			int year = Integer.parseInt(spD[2]);

			int h24 = Integer.parseInt(spT[0]);
			int min = Integer.parseInt(spT[1]);
			int sec = 0;

			int daysFrom2001 = ((year - refYear)/4)*(NumDaysPerYear*3 + NumDaysPerYearLY) + ((year - refYear)%4)*NumDaysPerYear;
			if (year % 4 == 0) {
				for (int j=0; j<month; j++) daysFrom2001 += NumDaysPerMonthLY[j];
			} else {
				for (int j=0; j<month; j++) daysFrom2001 += NumDaysPerMonth[j];
			}
			daysFrom2001 += (day - 1);
			long curTripStart_secFrom2001 = daysFrom2001*NumSecondsPerDay + h24*NumSecondsPerHour + min*NumSecondsPerMinute + sec;
			
			vs.trips[i] = new Trip();
			vs.trips[i].tripIDs = new TripIDs();
			
			vs.trips[i].tripIDs.gID = i;
			vs.trips[i].tripIDs.dayID = 0;
			vs.trips[i].tripIDs.idInDay = i;

			if (i< 1) vs.trips[i].tripIDs.secsFromLastTrip = 12*NumSecondsPerHour;
			else {
				vs.trips[i].tripIDs.secsFromLastTrip = (int)(curTripStart_secFrom2001 - prevTripStart_secFrom2001) - vs.trips[i-1].tripIDs.numRecSteps;
			}
			
			float[] mph = speedMPH[i];
			float miles = 0f;

			vs.trips[i].speedMPH = new float[mph.length];
			vs.trips[i].fltGrade = new float[mph.length];
			vs.trips[i].recAuxKW = new float[mph.length];
			
			for (int j=0; j<mph.length; j++) vs.trips[i].speedMPH[j] = mph[j];
			for (int j=1; j<mph.length; j++) miles += 0.5f*(mph[j]+mph[j-1])/(float)NumSecondsPerHour;

			vs.trips[i].tripIDs.numRecSteps = mph.length;
			vs.trips[i].tripIDs.miles = miles;
			
			vs.trips[i].tripIDs.year = year;
			vs.trips[i].tripIDs.month = month;
			vs.trips[i].tripIDs.day = day;
			
			vs.trips[i].tripIDs.hr24 = h24;
			vs.trips[i].tripIDs.min = min;
			vs.trips[i].tripIDs.sec = sec;
			
			vs.trips[i].tripIDs.numPayloadAdjust = 1;
			vs.trips[i].payloadAdjust = new AdditionalPayload[vs.trips[i].tripIDs.numPayloadAdjust];
			vs.trips[i].payloadAdjust[0] = new AdditionalPayload();
			vs.trips[i].payloadAdjust[0].payloadKg = payloadKg;
			
			prevTripStart_secFrom2001 = curTripStart_secFrom2001;
		}
		
		return vs;
	}
	
	public static VehicleSampleMA utl_convertFrom_SVehicleSample(SVehicleSample svs) {
		VehicleSampleMA vs = new VehicleSampleMA();
		
		vs.vehSampleInfo = new SampleID();
		vs.vehSampleInfo.hhID = svs.vehSampleInfo().hhID;
		vs.vehSampleInfo.vehIDinHH = svs.vehSampleInfo().vehIDinHH;
		vs.vehSampleInfo.nTrips = svs.vehSampleInfo().nTrips;
		vs.vehSampleInfo.hhWt = svs.vehSampleInfo().hhWt;
		
		SVehicleSample.Trip[] sTrips = svs.trips();
		vs.trips = new Trip[sTrips.length];
		
		for (int i=0; i<vs.trips.length; i++) {
			SVehicleSample.TripIDs sTripIDs = sTrips[i].tripIDs();
			
			vs.trips[i] = new Trip();
			vs.trips[i].tripIDs = new TripIDs();
			
			vs.trips[i].tripIDs.gID = sTripIDs.gID;
			vs.trips[i].tripIDs.dayID = sTripIDs.dayID;
			vs.trips[i].tripIDs.idInDay = sTripIDs.idInDay;

			vs.trips[i].tripIDs.secsFromLastTrip = sTripIDs.secsFromLastTrip;
			vs.trips[i].tripIDs.numRecSteps = sTripIDs.numRecSteps;

			vs.trips[i].tripIDs.miles = sTripIDs.miles;

			vs.trips[i].tripIDs.year = sTripIDs.year;
			vs.trips[i].tripIDs.month = sTripIDs.month;
			vs.trips[i].tripIDs.day = sTripIDs.day;
			
			vs.trips[i].tripIDs.hr24 = sTripIDs.hr24;
			vs.trips[i].tripIDs.min = sTripIDs.min;
			vs.trips[i].tripIDs.sec = sTripIDs.sec;
			
			vs.trips[i].tripIDs.numPayloadAdjust = 1;
			vs.trips[i].payloadAdjust = new AdditionalPayload[vs.trips[i].tripIDs.numPayloadAdjust];
			vs.trips[i].payloadAdjust[0] = new AdditionalPayload();

			float[] mph = sTrips[i].speedMPH();
			float[] fltGrade = sTrips[i].fltGrade();
			
			vs.trips[i].speedMPH = new float[mph.length];
			vs.trips[i].fltGrade = new float[mph.length];
			vs.trips[i].recAuxKW = new float[mph.length];
			
			for (int j=0; j<mph.length; j++) {
				vs.trips[i].speedMPH[j] = mph[j];
				vs.trips[i].fltGrade[j] = fltGrade[j];
			}
		}
		
		return vs;
	}
}
