package fastsimjava.abs;

import fastsimjava.components.FSJVehState;

public abstract class FSJHybridPowerManagerBase {
	//Function for setting the current state of the vehicle,
	// as well as the next required speed and possible power draw for no trace split
	public abstract void setCurState(FSJVehState vehCurState, float mphDesired, float kWDesiredAtWheels, float fcMod, float essChgDischgEffn, float totalAuxKW);


	//Function to report the operating output power from the fuel converter
	public abstract float fuelConvKWOut();
}
