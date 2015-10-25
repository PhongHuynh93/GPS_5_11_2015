package com.hfad.mytestmapgps;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The getFromLocation() method provided by the Geocoder class accepts a latitude and longitude, and returns a list of addresses. 
 * The method is synchronous, and may take a long time to do its work, so you should not call it from the main, user interface (UI) thread of your app.
 * The IntentService class provides a structure for running a task on a background thread. Using this class, you can handle a long-running operation without affecting your UI's responsiveness.
 */
public class FetchAddressIntentService extends IntentService {
    ///////////////
    //////////////////
    /////////////////////
    // variable  // // //
    /////////////////////
    //////////////////
    ///////////////
    private static final String TAG = "FetchAddressIS";
    protected ResultReceiver mReceiver;
    

    /////////////////
    ////////////////////
    ///////////////////////
    // Constructor // // //
    ///////////////////////
    ////////////////////
    /////////////////
    public FetchAddressIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    /////////////////
    ////////////////////
    ///////////////////////
    // Main method // // //
    ///////////////////////
    ////////////////////
    /////////////////
    /**
     * reverse geocoding: to convert a geographic location to an address
     * @param intent Location object 
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Using the Geocoder class in the Android framework location APIs, you can convert an address to the corresponding geographic coordinates.
        // Locale: dai. dien. 1 vung. pass nó cho Geocoder để address trả về chỉ tập trung tại vị trí người dùng
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        String errorMessage = "";
        
        ////////////////////////////////////////////
        // get data through an extra from Intent  //
        ////////////////////////////////////////////
        // get mReceiver through an extra
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }
        // get a location passed to this service through an extra.
        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
        // Make sure that the location data was really sent over through an extra. If it wasn't,
        // send an error error message and return.
        if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            Log.wtf(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            return;
        }

        //////////////////////////////
        // get address for location //
        //////////////////////////////
        try {
            // To get a street address corresponding to a geographical location, call getFromLocation(), passing it the latitude and longitude from the location object, and the maximum number of addresses you want returned.
            // The geocoder returns an array of addresses.
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } 
        ///////////////////////////////////////////////////////////////////////
        // Check for the following errors as shown in the code sample below. //
        ///////////////////////////////////////////////////////////////////////
        //  If an error occurs, place the corresponding error message in the errorMessage variable, so you can send it back to the requesting activity:
        catch (IOException ioException) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } 
        catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage
                    + ". "
                    + "Latitude = "
                    + location.getLatitude() 
                    + ", Longitude = "
                    + location.getLongitude(), illegalArgumentException);
        }
        // Handle case where no address was found.
        //  If no addresses were found to match the given location, it returns an empty list. If there is no backend geocoding service available, the geocoder returns null.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            // Send error back 
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } 
        //////////////////////////////////
        // if success in get addresses  //
        //////////////////////////////////
        else { // chi lay' address dau tien cua array thoi
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, getString(R.string.address_found));
            //////////////////////////////////////////////////////
            // Send the results back to the requesting activity //
            //////////////////////////////////////////////////////
            /// In the case of a successful reverse geocoding, the string contains the address.
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }


    ////////////////////////////////////
    ///////////////////////////////////////
    //////////////////////////////////////////
    // Private method, helper method  // // //
    //////////////////////////////////////////
    ///////////////////////////////////////
    ////////////////////////////////////
    /**
     * sends a result code and message bundle to the result receiver.
     * @param resultCode error (cant file address) or success 
     * @param message    [description]
     */
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }


}
