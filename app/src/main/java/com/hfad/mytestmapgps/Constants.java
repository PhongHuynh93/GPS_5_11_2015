package com.hfad.mytestmapgps;

/**
 * Created by huynhducthanhphong on 10/25/15.
 */
public final class Constants {
    // The next step is to retrieve the street address from the geocoder, handle any errors that may occur, and send the results back to the activity that requested the address.
    // To report the results of the geocoding process, you need two numeric constants that indicate success or failure.
	public static final int SUCCESS_RESULT         = 0;
	public static final int FAILURE_RESULT         = 1;

	public static final String PACKAGE_NAME        = "com.google.android.gms.location.sample.locationaddress";
	public static final String RECEIVER            = PACKAGE_NAME + ".RECEIVER";
	public static final String RESULT_DATA_KEY     = PACKAGE_NAME + ".RESULT_DATA_KEY";
	public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";


}
