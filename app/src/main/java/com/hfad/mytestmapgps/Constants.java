package com.hfad.mytestmapgps;

/**
 * Created by huynhducthanhphong on 10/25/15.
 */
public final class Constants {
    // The next step is to retrieve the street address from the geocoder, handle any errors that may occur, and send the results back to the activity that requested the address.
    // To report the results of the geocoding process, you need two numeric constants that indicate success or failure.
	public static final int SUCCESS_RESULT                     = 0;
	public static final int FAILURE_RESULT                     = 1;
	
	// 
	public static final String PACKAGE_NAME                    = "com.google.android.gms.location.sample.locationaddress";
	public static final String RECEIVER                        = PACKAGE_NAME + ".RECEIVER";
	public static final String RESULT_DATA_KEY                 = PACKAGE_NAME + ".RESULT_DATA_KEY";
	public static final String LOCATION_DATA_EXTRA             = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
	
	// ////////////////////////////////////////////////////
	// Keys for storing activity state in the Bundle. //
	////////////////////////////////////////////////////
	public static final String STATE_RESOLVING_ERROR           = "resolving_error";     // Bool to track whether the app is already resolving an error
	public static final String LAST_UPDATED_TIME_STRING_KEY    = "last-updated-time-string-key";
	public static final String LOCATION_ADDRESS_KEY            = "location-address";
	public static final String ADDRESS_REQUESTED_KEY           = "address-request-pending"; // true la` da~ nhan' nut' lay dia chi
	public static final String START_POINT_KEY                 = "start";
	public static final String DEST_POINT_KEY                  = "destination";
	public static final String VIA_POINT_KEY                   = "viapoints";
	public static final String TRACKING_MODE_KEY               = "tracking_mode";
	public static final String MY_LOCATION_KEY = "location"; // myLocationOverlay

	public static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
	public static final String LOCATION_KEY                    = "location-key"; // lưu location hiện tại của người dùng 

	// address của endpoint
	public static final double END_POINT_LATITUDE = 10.758097;
	public static final double END_POINT_LONGTITUDE = 106.659147;

	// icon cho map
	public static final int NODE_ICON = R.drawable.marker_node;
	public static final int CONTINUE_ICON = R.drawable.ic_continue;
	public static final int HERE_MARKER_ICON = R.mipmap.here3;
	public static final int DES_MARKER_ICON = R.mipmap.here2;
	public static final int BUTTON_TRACKING_ON_ICON = R.drawable.btn_tracking_on;
	public static final int BUTTON_TRACKING_OFF_ICON = R.drawable.btn_tracking_off;
	public static final int DEPARTURE_ICON = R.drawable.marker_departure;
	public static final int VIA_POINT_ICON = R.drawable.marker_via;
	public static final int DESTINATION_ICON = R.drawable.marker_destination;
	public static final int POI_ICON = R.drawable.marker_poi_cluster;
	

	// dòng chữ cho heremarker và endmarker
	public static final String MESSAGE_DEPARTURE = "Điểm đi";
	public static final String MESSAGE_DESTINATION = "Điểm đến";

	// phương tiện di chuyển
	public static final String TRANSPORTATION = "driving"; // //walking, bicycling, transit

	// requestCode của app
	public static final int REQUEST_RESOLVE_ERROR = 1001; // // Request code to use when launching the resolution activity
	public static final int ROUTE_REQUEST = 1; // // Request code to use when launching the resolution activity
	public static final int POIS_REQUEST = 2; // // Request code to use when launching the resolution activity
}
