package fastsimjava.components;

public class FuelCalcConstants {
	//Default values for calculation of equivalent GHG -- change as needed -- last updated September 25th, 2020
	public static final float Default_gmCO2perGalGas = 10778f;		// 2.20 kg-CO2/gal for Well-to-tank (as per GREET 2019), plus 18.9 pounds CO2 (as per https://www.eia.gov/tools/faqs/faq.php?id=307&t=11) = 8573 grams from combustion of E10 gasoline
	public static final float Default_gmCO2perGalDiesel = 11981f;	// 1.82 kg-CO2/gal for Well-to-tank (as per GREET 2019), plus 22.4 pounds CO2 (as per https://www.eia.gov/tools/faqs/faq.php?id=307&t=11) = 10161 grams from combustion of gal of Diesel
	public static final float Default_gmCO2perKgH2 = 8500f;			// Most-likely reasonable value for 2016 in CA -- see TRI1702-324
	public static final float Default_gmCO2perM3CNG = 2486f;		// 0.61 kg-CO2/m^3 for Well-to-tank (as per GREET 2019), plus 0.1171 pounds CO2 per cubic foot (as per https://www.eia.gov/environment/emissions/co2_vol_mass.php)
	public static final float Default_gmCO2perKWhElect = 310f;		// Model for ~2018 CA grid
	
	//Energy intensity per unit fuel
	public static final float Default_h2KWhPerKg = 32.72f;			//kWhr per kilogram of hydrogen
	public static final float Default_kWhPerGGE = 33.7f;			//kWhr per gallon gasoline equivalent
	public static final float Default_kWhPerGalDiesel = 37.9527f;	//kWhr per gallon Diesel
	public static final float Default_cngKWhPerM3 = 10.395f;		//kWh per m^3 Natural gas (https://www.uniongas.com/business/save-money-and-energy/analyze-your-energy/energy-insights-information/conversion-factors)
	
	//Variables
	public float gmCO2perGalGas, gmCO2perGalDiesel, gmCO2perKgH2, gmCO2perM3CNG, gmCO2perKWhElect;

	public FuelCalcConstants() {
		gmCO2perGalGas = Default_gmCO2perGalGas;
		gmCO2perGalDiesel = Default_gmCO2perGalDiesel;
		gmCO2perKgH2 = Default_gmCO2perKgH2;
		gmCO2perM3CNG = Default_gmCO2perM3CNG;
		gmCO2perKWhElect = Default_gmCO2perKWhElect;
	}
}
