	void phevSimTest() {
		//Path to FASTSim vehicle model file (Prius Prime)
		String pathVehModelFile = "~~~ Full Path to the File~~~ofvmPriusPrime_2021_03_26.csv";
		
		//Path to Trip File
		String pathVehSampleTripFile = "~~~ Full Path to the File~~~compositeTrip01.csv";
		int tripID = 0;
		
		//Output Directory
		String outputDirectory = "~~~ Full Path to Output Directory including forward (or backward) slash~~~";
		
		//Read Trip
		VehicleSampleMA[] vehSamplesData = VehicleSampleMA.readArrayFromFile(pathVehSampleTripFile);
		float[] mph = vehSamplesData[0].trips()[tripID].speedMPH();
		float[] roadGrade = vehSamplesData[0].trips()[tripID].fltGrade();
		
		//Read Vehicle Model and set it into FASTSim-Java
		FSJOneFileVehModel ofvModel = new FSJOneFileVehModel(pathVehModelFile);
		FASTSimJ3c fsm = new FASTSimJ3c();		
		fsm.setVehModel(ofvModel.vehModelParam, ofvModel.curveMan, ofvModel.addMassKg, ofvModel.addAuxKW, ofvModel.adjDEMult);
		
		//Outputs to be recorded from simulations
		FASTSimJ3c.TripRecordOutput[] outReq = {FASTSimJ3c.TripRecordOutput.Time_sec, FASTSimJ3c.TripRecordOutput.Speed_mph, 
				FASTSimJ3c.TripRecordOutput.Distance_mi, FASTSimJ3c.TripRecordOutput.RelSOC, FASTSimJ3c.TripRecordOutput.FuelUse, 
				FASTSimJ3c.TripRecordOutput.FuelConverterKW, FASTSimJ3c.TripRecordOutput.MotorKW, FASTSimJ3c.TripRecordOutput.BatteryKW};

		
		//Test simulation #1: EV Mode Starting with battery full
		String outputFile1 = outputDirectory+"ctOutput_startFull_evMode.csv";
		float iSoC = 1f;
		
		fsm.setRelSoC(iSoC);
		fsm.runTR(null, mph, roadGrade);
		fsm.extractTimeRecord(outputFile1, outReq);
		
		
		//Test simulation #2: Charge Sustaining Mode Starting with empty full
		String outputFile2 = outputDirectory+"ctOutput_startEmpty_csMode.csv";
		iSoC = 0f;
		
		fsm.setRelSoC(iSoC);
		fsm.runTR(null, mph, roadGrade);
		fsm.extractTimeRecord(outputFile2, outReq);

		
		//Test simulation #3: Starting with battery full, utilizing hold mode until just before Geo-Fence
		String outputFile3 = outputDirectory+"ctOutput_startFull_holdMode.csv";
		iSoC = 1f;
		float holdUntilMile = 103f;
		
		FSJHybridPowerManagerAdvPHEV pwrMan = new FSJHybridPowerManagerAdvPHEV();
		pwrMan.addChargeMangementSegment_chgHold(0);
		pwrMan.addChargeMangementSegment_normal(holdUntilMile);
		
		fsm.setRelSoC(iSoC);
		fsm.runTR(null, mph, roadGrade, null, null, pwrMan, -1f);		
		fsm.extractTimeRecord(outputFile3, outReq);

		
		//Test simulation #4: Starting with battery empty, charge up mode until battery is full, then hold until just before Geo-Fence
		String outputFile4 = outputDirectory+"ctOutput_startEmpty_chargeMode.csv";
		iSoC = 0f;
		float chargeTillSOC = 1f;
		float milesToChargeSOC = 20f;
		
		pwrMan.resetModeIntervals();
		pwrMan.addChargeMangementSegment_chgUp(0, milesToChargeSOC, chargeTillSOC);
		pwrMan.addChargeMangementSegment_normal(holdUntilMile);
		
		fsm.setRelSoC(iSoC);
		fsm.runTR(null, mph, roadGrade, null, null, pwrMan, -1f);		
		fsm.extractTimeRecord(outputFile4, outReq);

		
		//Test simulation #4: Starting with battery full, drive in EV mode and charge sustaining mode when empty, but re-charge and hold before Geo-Fence
		String outputFile5 = outputDirectory+"ctOutput_startFull_evMode_chargeMode.csv";
		iSoC = 1f;
		float mileToBeginReCharge = 70f;
		
		pwrMan.resetModeIntervals();
		pwrMan.addChargeMangementSegment_chgUp(mileToBeginReCharge, milesToChargeSOC, chargeTillSOC);
		pwrMan.addChargeMangementSegment_normal(holdUntilMile);
		
		fsm.setRelSoC(iSoC);
		fsm.runTR(null, mph, roadGrade, null, null, pwrMan, -1f);		
		fsm.extractTimeRecord(outputFile5, outReq);
	}
