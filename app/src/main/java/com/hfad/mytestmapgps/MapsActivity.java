package com.hfad.mytestmapgps;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.DateFormat;
import java.util.Date;

// AppCompatActivity: chứa Fragment API, mà ta sử dụng Map thuộc dạng Fragment 
public class MapsActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener{
	/////////////////////////////
	// private variable  // // //
	/////////////////////////////
	// Using Google Map
    private MapView map;
    private IMapController mapController;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    // Using the Google Play services location APIs, your app can request the last known location of the user's device = user's current location
    private GoogleApiClient mGoogleApiClient; // sd Google Client de? connect den' API
    // Retrieve the latitude and longtitude coordinates of a geographic location of a last known location. 
    private Location mCurrentLocation;
    // To store parameters for requests to the fused location provider, create a LocationRequest. The parameters determine the levels of accuracy requested.
    private LocationRequest mLocationRequest;
    // 3 textView to store latitude and longtitude and time
    private TextView text_lat;
    private TextView text_long;
    private TextView text_time;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    // flag used to track whether the user has turned location updates on or off. 
    private boolean mRequestingLocationUpdates;
    // flag used to track the beginning of the the location to set the map centered at that position
    private boolean firstLocation = true;
    private double firstLat;
    private double firstLong;
    // Time when the location was updated represented as a String.
    private String mLastUpdateTime;

    //////////////////////////////////////////////////////
    // variable to retrieve an address from a location  //
    //////////////////////////////////////////////////////
    ///A ResultReceiver to handle the results of the address lookup.
    private AddressResultReceiver mResultReceiver;
    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    private boolean mAddressRequested; // true la` da~ nhan' nut' lay dia chi 
    // chua' address output
    protected String mAddressOutput;
	protected TextView mLocationAddressTextView; // output address onto the screen 

    /////////////////////////////////////
    // variable to resolve error // // //
    /////////////////////////////////////
    /*
    Now you're ready to safely run your app and connect to Google Play services. How you can perform read and write requests to any of the Google Play services using GoogleApiClient is discussed in the next section.
     */
    // Request code to use when launching the resolution activity
	private static final int REQUEST_RESOLVE_ERROR    = 1001;
	// Unique tag for the error dialog fragment
	private static final String DIALOG_ERROR          = "dialog_error";
	
	private static final String STATE_RESOLVING_ERROR = "resolving_error";
	
	////////////////////////////////////////////////////
	// Keys for storing activity state in the Bundle. //
	////////////////////////////////////////////////////
	protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
	protected final static String LOCATION_KEY                    = "location-key";
	protected final static String LAST_UPDATED_TIME_STRING_KEY    = "last-updated-time-string-key";
	protected static final String LOCATION_ADDRESS_KEY            = "location-address";
	protected static final String ADDRESS_REQUESTED_KEY           = "address-request-pending";
	
	/////////////////////////////////////
	// Attribute for location updates  //
	/////////////////////////////////////
	private static final String TAG                                   = "location-updates-sample";
    private static final String LOG_TAG                               = "MapsActivity";
	private static final long UPDATE_INTERVAL_IN_MILLISECONDS         = 10000;
	private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * To avoid executing the code in onConnectionFailed() while a previous attempt to resolve an error is ongoing, you need to retain a boolean that tracks whether your app is already attempting to resolve an error.
     * To keep track of the boolean across activity restarts (such as when the user rotates the screen), save the boolean in the activity's saved instance data using onSaveInstanceState():
     * Your app must therefore store any information it needs to recreate the activity. 
     * @param savedInstanceState [description]
     */
    @Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
	    super.onSaveInstanceState(savedInstanceState);
	    savedInstanceState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
	    savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
	    savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
	    // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
	}

	///////////////////////////////
	//////////////////////////////////
	/////////////////////////////////////
	////////////////////////////////////////
	// Life cycle of a activity  // // // //
	////////////////////////////////////////
	/////////////////////////////////////
	//////////////////////////////////
	///////////////////////////////
    /**
     * Run first when you open a "Map" app
     * Called when the activity is first created. This is where you should do all of your normal static set up: create views, bind data to lists, etc. 
     * + Make google api
     * + Set up the "map". Make it appear
     * @param savedInstanceState [description]
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Then recover the saved state during onCreate():
        mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        mResultReceiver = new AddressResultReceiver(new Handler());
        // Locate the UI widget 
		text_lat                 = (TextView) findViewById(R.id.textView);
		text_long                = (TextView) findViewById(R.id.textView2);
		text_time                = (TextView) findViewById(R.id.textView3);
		mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);
        // set variable
		mRequestingLocationUpdates = false;
		mLastUpdateTime            = ""; 
		mAddressOutput             = ""; // at the beginning, there are not any addresses
		mAddressRequested          = false; // at the beginning, user hasn't pressed the button .
        // first lat and long location
        firstLat = 0.0;
        firstLong = 0.0;
        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
        // build google api to get user's location
        buildGoogleApiClient();

    }

	/**
     * Called when the activity is becoming visible to the user.
	 * Followed by onResume() if the activity comes to the foreground, or onStop() if it becomes hidden.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  
            mGoogleApiClient.connect(); // connect to google API when it was build 
        }
    }

    /**
     * Called when the activity will start interacting with the user. At this point your activity is at the top of the activity stack, with user input going to it.
     * Set up the "map". Make it appear. 
     */
    @Override
    protected void onResume() {
        super.onResume();
        // make "Map" again when you open app.
        //setUpMapIfNeeded();
        // check if connect to google api , or track whether location updates are currently turned off. 
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
	        startLocationUpdates();
	    }
    }

    /**
     * Consider whether you want to stop the location updates when the activity is no longer in focus, such as when the user switches to another app or to a different activity in the same app. 
     *  This can be handy to reduce power consumption
     */
    @Override
	protected void onPause() {
	    super.onPause();
	    stopLocationUpdates();
	}

	protected void stopLocationUpdates() {
		mRequestingLocationUpdates = false;
	    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
	}

    /**
     * Called when the activity is no longer visible to the user, because another activity has been resumed and is covering this one.
     */
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect(); // disconnect to google API
        super.onStop();
    }

    ////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    // Updates fields based on data stored in the bundle. // // //
    //////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }
            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }

            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                this.displayAddressOutput();
            }

            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            updateUI(); // output old long and latitude 
        }
    }
  
    //////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    // Connect to Google API , using "Location" API // // //
    ////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    //////////////////////////////////////////////////
    /**
     * Connected to Google Play services and the location services API
     * With the callback interfaces defined (onConnectionSuspended + onConnectionFailed), you're ready to call connect(). 
     * To gracefully manage the lifecyReacle of the connection, you should call connect() during the activity's onStart() (unless you want to connect later), then call disconnect() during the onStop() method. 
     */
    protected synchronized void buildGoogleApiClient() {
    	Log.i(TAG, "Building GoogleApiClient"); // Send an INFO log message.
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * which is called when the client is ready. When your app is successful connecting to google API
     * ->  The good stuff goes here. For example
     * Request user's location .
     * @param connectionHint [description]
     */
    @Override
    public void onConnected(Bundle connectionHint) {
    	// To request the last known location, call the getLastLocation() method, passing it your instance of the GoogleApiClient object.
        // The getLastLocation() method returns a Location object from which you can retrieve the latitude and longitude coordinates of a geographic location.
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //  The location object returned may be null in rare cases when the location is not available. --> check first
        if (firstLocation) {
            firstLocation = false;
            firstLat = mCurrentLocation.getLatitude();
            firstLong = mCurrentLocation.getLongitude();
        }
        // make "Map" again when you open app.
        setUpMapIfNeeded();

        if (mCurrentLocation != null) {
            text_lat.setText(String.valueOf(mCurrentLocation.getLatitude()));
            text_long.setText(String.valueOf(mCurrentLocation.getLongitude()));
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available,
                        Toast.LENGTH_LONG).show();
                return;
            }
            // You must also start the intent service when the connection to Google Play services is established, if the user has already clicked the button on your app's UI.
            if (mAddressRequested) {
                startIntentService();
            }
        }
        createLocationRequest();
        if (!mRequestingLocationUpdates) { // if location update is currently turned off --> turn it on. 
	        startLocationUpdates();
	    }
    }

    /**
     * The last known location of the device provides a handy base from which to start, ensuring that the app has a known location before starting the periodic location updates.
     * The priority of PRIORITY_HIGH_ACCURACY, combined with the ACCESS_FINE_LOCATION permission setting that you've defined in the app manifest, and a fast update interval of 5000 milliseconds (5 seconds), causes the fused location provider to return location updates that are accurate to within a few feet. This approach is appropriate for mapping apps that display the location in real time.
     */
    protected void createLocationRequest() {
	    mLocationRequest = new LocationRequest();
	    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
	    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
	    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	/**
	 * start the regular updates by calling requestLocationUpdates().
	 * shows you how to get the update using the "LocationListener" callback approach. 
	 */
	protected void startLocationUpdates() {
		mRequestingLocationUpdates = true; // it has already turned on location update 
	    LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this); // this = "LocationListener"
	}

	/**
	 * The fused location provider invokes the LocationListener.onLocationChanged() callback method. 
	 * Location object containing the location's latitude and longitude. 
	 * 
	 * @param location [description]
	 */
	@Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        text_lat.setText(String.valueOf(mCurrentLocation.getLatitude()));
        text_long.setText(String.valueOf(mCurrentLocation.getLongitude()));
        text_time.setText(mLastUpdateTime);
        // Xuat' ra dong chu~ len man hinh
        Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    ////////////////
    ///////////////////
    //////////////////////
    /////////////////////////
    // Make a map // // // //
    /////////////////////////
    //////////////////////
    ///////////////////
    ////////////////
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = (MapView) findViewById(R.id.map);
            map.setTileSource(TileSourceFactory.MAPNIK);
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
            // control the map
            mapController = map.getController();
            if (mapController != null) {
                controlMap();
            }

        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     * When the My Location layer is enabled, the My Location button appears in the top right corner of the map.
     * When a user clicks the button, the camera centers the map on the current location of the device, if it is known.
     * The location is indicated on the map by a small blue dot if the device is stationary, or as a chevron if the device is moving.
     */
    private void setUpMap() {
        //map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
    }

    private void controlMap() {
        mapController.setZoom(15);
        GeoPoint firstStartPoint = new GeoPoint(firstLat, firstLong);
        mapController.setCenter(firstStartPoint);
    }

    //////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    // The rest of this code is all about building the error dialog // // //
    ////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////
    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends android.support.v4.app.DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MapsActivity) getActivity()).onDialogDismissed();
        }
    }

    /**
     * Once the user completes the resolution provided by startResolutionForResult() or GoogleApiAvailability.getErrorDialog(), your activity receives the onActivityResult() callback with the RESULT_OK result code. You can then call connect() again. 
     * @param requestCode [description]
     * @param resultCode  [description]
     * @param data        [description]
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }


    ////////////////////////////////////////////////////////
    // Send a location to another class to get addresses  //
    ////////////////////////////////////////////////////////
    /**
     * To create an explicit intent, specify the name of the class to use for the service: FetchAddressIntentService.class.
     * Pass two pieces of information in the intent extras:
     *  + A ResultReceiver to handle the results of the address lookup.
     *  + A Location object containing the latitude and longitude that you want to convert to an address.
     */
    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        startService(intent);
    }

    /**
     * Khi nhan nut' thi` goi. class khac de? lay' address
     * @param view Button
     */
    public void fetchAddressButtonHandler(View view) {
        // Only start the service to fetch the address if GoogleApiClient is
        // connected.
        if (mGoogleApiClient.isConnected() && mCurrentLocation != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, process the user's request by
        // setting mAddressRequested to true. Later, when GoogleApiClient connects,
        // launch the service to fetch the address. As far as the user is
        // concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
        //updateUIWidgets();
    }

    /**
     * Sau khi lay duoc adddress 
     * define an AddressResultReceiver that extends ResultReceiver to handle the response from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        // The result includes a numeric result code (resultCode) as well as a message containing the result data (resultData). 
        // Override the onReceiveResult() method to handle the results delivered to the result receiver
        // If the reverse geocoding process was successful, the resultData contains the address. In the case of a failure, the resultData contains text describing the reason for failure. 
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;

        }
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        mLocationAddressTextView.setText(mAddressOutput);
    }

    /**
     * Shows a toast with the given text.
     */
    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    //////////////////////////////////
    /////////////////////////////////////
    ////////////////////////////////////////
    // show route between 2 places  // // //
    ////////////////////////////////////////
    /////////////////////////////////////
    //////////////////////////////////

}
