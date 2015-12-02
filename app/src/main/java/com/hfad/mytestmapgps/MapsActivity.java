package com.hfad.mytestmapgps;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.SensorEventListener;
import android.location.Geocoder;
import android.location.Location;

import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.ManifestUtil;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.Manifest;

// AppCompatActivity: chứa Fragment API, mà ta sử dụng Map thuộc dạng Fragment 
/**
 * Bước thưc hiện của app:..
 * 1. Tạo google client cho app xin location API để xin google access nơi ở 
 * 2. Tạo map --> load map tại vị trí hiện tại của ta --> Tìm đường đi giữa 2 điểm bằng cách hỏi Google (update điểm của ta liên tục và vẽ liên tục quãng đường đến đích)
 * 3. Lắng nghe update location --> load map lại tại vị trí của ta. thường xuyên
 *
 * ************************************************************************************************************
 * Activity này:
 * 1. onCreate: build Google API
 * 2. onStart: connect() to Google API 
 *     + onConnect: nếu kết nối thành công, 
 *         - tìm kiếm vị trí hiện tại của ta.
 *         - Tạo Map : 
 *             . Tạo map event overlay để nghe những sự kiện ta tác động lên map 
 *             .zoom , cho nó center tại 1 điểm 
 *         - Xuất vị trí lên màn hình
 *         - Nếu ta nhấn nút chuyển vị trí hiện tại thành address thì xuất address
 *         - Vẽ đường đi từ vị trí hiện tại đến endPoint
 *         - Bắt đầu update vị trí thường xuyên
 *             . Nếu vị trí thay đổi:
 *                 . Xóa marker
 *                 . Update lại vị trí hiện tại
 *                 . Thêm marker ngay tại vị trí hiện tại
 *                 . Thêm maker tại vị trí endpoint
 *                 . set Map tại trung điểm vị trí hiện tai của mình --> Nếu ta chưa bật Location sau đó bật thì hàm này dò ra vị trí thí update lại map 
 *                 . Vẽ đường đi từ vị trí hiện tại đến endPoint 
 *                 . Update lại giao diện:
 *                     - Thay đổi 3 textview chỉ vị tri hiện tại.
 *         --> Nếu ko kết nối được Google (ko bạt wifi và GPS): chương trình chưa crash, hiện chữ "Bật GPS lên"
 *     + onConnectionSuspended: khi chưa kết nối được tới Google.
 *         - connect() lại Google
 *     + onConnectionFailed: Nếu kết nối thất bại
 *         - Xét xem đã giải quyết error chưa.
 *             . Nếu rồi: thì thoát hàm này.
 *             . Nếu chưa: 
 *                 . thì giải quyết lỗi --> set biến đang giải quyết lỗi bằng True
 *                 . connect() lại Google 
 * 3. onResume: bắt đầu lắng nghe sự thay đổi trong location nếu đã kết nối tới Google
 * 4. onPause: ko nghe thay đổi nữa
 * 5. onStop: ngưng kết nối tới Google API
 *
 * ************************************************************************************************************
 * Interface: 
 * 1. ConnectionCallbacks: chưa các method sẽ được gọi thì kết nối thành công hay ko .... tơi google server, gọi interface này khi ta sử dụng connect()
 *     + onConnected(): dc gọi khi kết nối thành công tới Google
 *     + onConnectionSuspended:
 *     + onConnectionFailed: 
 *     
 * *************************************************************************************************************
 * Event
 * 1. singleTapConfirmedHelper
 *     + Khi tap: thì hiện vị trí tap
 * 2. longPressHelper:
 *     + Khi nhấn lâu: 
 *     + Xóa marker
 *     + set marker tại vị tri herePoint, endPoint
 *     + Set lại endPoint
 *     + Tìm đường tại vị tri hiện tại đến endPoint
 */
public class MapsActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, MapEventsReceiver{
	/////////////////////////////
	// private variable  // // //
	//////////////////////////////
    private MapView map;
    private List<Overlay> mapOverlay;
    // compass
    private CompassOverlay mCompassOverlay = null;
    // thanh scale bar
    private ScaleBarOverlay mScaleBarOverlay = null;
    // mini map
    private MinimapOverlay mMinimapOverlay;
    // con duong
    private Polyline roadOverlay;

    // icon chi? duong
    private Drawable nodeIcon;
    private Drawable iconContinue;

    // tủ nhét đồ
    private SharedPreferences prefs;

    // đây là marker chỉ vị trí của mình, sẽ tự xoay hình khi ta quẹo.
    private DirectedLocationOverlay myLocationOverlay = null;

    // start: vị trí mà ta muốn bắt đầu đi
    // destination: vi trí mà ta muốn đến
    // prevLocation: điểm GPS trong quá khứ (giúp tính speed và hướng)
    // newLocation: điểm GPS hiện tại  (giúp tính speed và hướng)
    // tương ưng 2 điểm trên ta có 2 markers
    // điểm trung gian nếu ta muốn dẫn đường qua những điểm trung gian này
    private GeoPoint startPoint = null;
    private GeoPoint destinationPoint = null;
    private GeoPoint prevLocation = null;
    private GeoPoint newLocation = null;
    private double mSpeed = 0.0; // speed tính được nhờ quá khứ và hiện tại GPS
    private float mAzimuthAngleSpeed = 0.0f; // tiên đoán hướng xoay dựa vào tốc độ GPS

    private Marker markerStart = null;
    private Marker markerDestination = null;
    private ArrayList<GeoPoint> viaPoints = null;
    private static int START_INDEX = -2;
    private static int DEST_INDEX = -1;

    // các marker hành trình
    private FolderOverlay mItineraryMarkers = null;

    // cửa sổ hướng dẫn đường
    private ViaPointInfoWindow mViaPointInfoWindow = null;

    // thiết lập tham số cho bản đồ
    private IMapController mapController;

    // here: vị trí GPS hiện tại update theo thời gian thực dựa vào tọa độ GPS
    // end: vị trí ta chọn trước 
    private GeoPoint herePoint;
    private GeoPoint endPoint;
    private Marker hereMarker;
    private Marker endMarker;

    // nút track trong bản đồ, và biến theo dõi trang thái nhấn hay ko
    private Button mTrackingModeButton;
    private boolean mTrackingMode = false; // lúc đầu là chế độ ko track

    // overlay sự kiện (touch short và touch long)
    private MapEventsOverlay mapEventsOverlay;
    // Biến App cua mình thành google client (đã xin mẹ chức năng access location)
    private GoogleApiClient mGoogleApiClient; 
    // Retrieve the latitude and longtitude coordinates of a geographic location of a last known location. 
    private Location mCurrentLocation;
    // To store parameters for requests to the fused location provider, create a LocationRequest. The parameters determine the levels of accuracy requested.
    private LocationRequest mLocationRequest;
    // 3 textView to store latitude and longtitude and time
    private TextView text_lat;
    private TextView text_long;
    private TextView text_time;
    // context
    private Context context;

    private boolean mResolvingError = false;    // Bool to track whether the app is already resolving an error

    // flag used to track whether the user has turned location updates on or off. 
    private boolean mRequestingLocationUpdates = false;
    // flag used to track the beginning of the the location to set the map centered at that position
    private boolean firstLocation = true;
    // Time when the location was updated 
    private String mLastUpdateTime;
    // identify GPS turn on or off ?
    private boolean has_GPS_on = false;
    private boolean first_center_map = true; // chi? center map lan dau tien khi ket noi' toi' wifi

    static String graphHopperApiKey;
    static String mapQuestApiKey;
    static String flickrApiKey;
    static String geonamesAccount;

    //////////////////////////////////////////////////////////////////////
    // sử dụng thư viên osmdroid để nghe tín hiệu GPS chứ ko nhờ google //
    //////////////////////////////////////////////////////////////////////
    private LocationManager mLocationManager = null;

    //////////////////////
    // Route giữa 2 máy //
    //////////////////////
    private static final String transportation = "driving"; //walking, bicycling, transit
    String mode = null;
    String urlForDirections = null;


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
    private boolean mAddressRequested = false; // true la` da~ nhan' nut' lay dia chi 
    // chua' address output
    protected String mAddressOutput;
	protected TextView mLocationAddressTextView; // output address onto the screen 

    /////////////////////////////////////
    // variable to resolve error // // //
    /////////////////////////////////////
    // Now you're ready to safely run your app and connect to Google Play services. How you can perform read and write requests to any of the Google Play services using GoogleApiClient is discussed in the next section.
    // Request code to use when launching the resolution activity
	private static final int REQUEST_RESOLVE_ERROR    = 1001;
	// Unique tag for the error dialog fragment
	private static final String DIALOG_ERROR          = "dialog_error";
	
	private static final String STATE_RESOLVING_ERROR = "resolving_error";     // Bool to track whether the app is already resolving an error
	
	////////////////////////////////////////////////////
	// Keys for storing activity state in the Bundle. //
	////////////////////////////////////////////////////
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY                    = "location-key"; // lưu location hiện tại của người dùng 
    protected final static String LAST_UPDATED_TIME_STRING_KEY    = "last-updated-time-string-key";
    protected static final String LOCATION_ADDRESS_KEY            = "location-address";
    protected static final String ADDRESS_REQUESTED_KEY           = "address-request-pending"; // true la` da~ nhan' nut' lay dia chi
    protected static final String START_POINT_KEY                 = "start";
    protected static final String DEST_POINT_KEY                  = "destination";
    protected static final String VIA_POINT_KEY                   = "viapoints";
    protected static final String TRACKING_MODE_KEY               = "tracking_mode";


	
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
     * 
     * Hàm này sẽ được gọi trước khi Activity của ta bị phá hủy, dùng để lưu các thông tin quan trọng để phục hồi lại 1 Activity lúc ban đầu 
     * @param savedInstanceState [description]
     */
    @Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
	    super.onSaveInstanceState(savedInstanceState);

	    savedInstanceState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError); // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);  // true la` da~ nhan' nut' lay dia chi 

	    savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);

	    savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime); // thời gian lưu lại 
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput); // Save the address string.
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
		mLastUpdateTime            = ""; 
		mAddressOutput             = ""; // at the beginning, there are not any addresses
        
        // endPoint to route between my location to there
        endPoint = new GeoPoint(10.758097, 106.659147);
        
        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // context
        context = this.getApplicationContext();

        // api key
        graphHopperApiKey = ManifestUtil.retrieveKey(context,  "GRAPHHOPPER_API_KEY");
        mapQuestApiKey = ManifestUtil.retrieveKey(this, "MAPQUEST_API_KEY");
        flickrApiKey = ManifestUtil.retrieveKey(this, "FLICKR_API_KEY");
        geonamesAccount = ManifestUtil.retrieveKey(this, "GEONAMES_ACCOUNT");

        // cái tủ nhét đồ
        prefs = getSharedPreferences("OSMNAVIGATOR", MODE_PRIVATE);

        // xây dựng Google Client có chức năng access vị trí 
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
            // hành động google client xin phép mẹ sử dụng các chức năng của mẹ 
            // xin mẹ cho: con thực hiện "onConnected()"
            // xin mẹ ko cho: con thực hiện "onConnectionFailed()"
            mGoogleApiClient.connect(); 
        }
    }

    /**
     * Called when the activity will start interacting with the user. At this point your activity is at the top of the activity stack, with user input going to it.
     * 1. update location khi bật lại app
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
	        startLocationUpdates();
	    }
        // bat compass
        if (mCompassOverlay != null)
            mCompassOverlay.enableCompass();
    }

    /**
     * 1. Consider whether you want to stop the location updates when the activity is no longer in focus --> reduce power 
     */
    @Override
	protected void onPause() {
	    super.onPause();
	    stopLocationUpdates();
        // tat' compass
        if (mCompassOverlay != null)
            mCompassOverlay.disableCompass();
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
        mGoogleApiClient.disconnect(); // xài xong phải trả lại mẹ , để ko kết nối tới google nữa, ko định vị nữa --> tiết kiệm pin 
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

            // lấy tọa độ điểm đi
            if (savedInstanceState.keySet().contains(START_POINT_KEY)) {
                startPoint = savedInstanceState.getParcelable(START_POINT_KEY);
            }

            // lấy tọa độ điểm đến
            if (savedInstanceState.keySet().contains(DEST_POINT_KEY)) {
                destinationPoint = savedInstanceState.getParcelable(DEST_POINT_KEY);
            }

            // lấy tọa độ các điểm trung gian
            if (savedInstanceState.keySet().contains(VIA_POINT_KEY)) {
                viaPoints = savedInstanceState.getParcelableArrayList(VIA_POINT_KEY);
            }

            // lấy tracking mode
            if (savedInstanceState.keySet().contains(TRACKING_MODE_KEY)) {
                mTrackingMode = savedInstanceState.getBoolean(TRACKING_MODE_KEY);
                updateUIWithTrackingMode();
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
     * SỬ dụng để access vị tri của người dùng , kiểm tra address dựa vào vị trí của người dùng 
     */
    protected synchronized void buildGoogleApiClient() {
    	Log.i(TAG, "Building GoogleApiClient"); // Send an INFO log message.
        // LocationServices.API:  chứa FusedLocationProviderApi mà trong API có hàm: public abstract Location getLastLocation (GoogleApiClient client)
        // biến máy của mình thành Client xin Google , xin chứa năng gì add vô "addApi". "Builder" = xây nhà (xây Google con)
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Called when your app is successful connecting to google API -> là GOogle client(con) đã xin được mẹ thành công
     * ->  Kết nối xong làm gì thì muốn xin Google cái j thì xin tại đây
     * 
     * 1. Request first user's location .
     * 2. Make a map 
     * 3. Lấy địa chỉ ngay tại vị trí nếu người dùng nhấn nút 
     * 4. TÌm đường đi giữa điểm tại đây và 1 điểm chọn đại
     * 5. Bắt đầu update vị trí người dùng thường xuyên 
     * @param connectionHint [description]
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mCurrentLocation == null) {
            showToast("Bạn vui lòng bật GPS để xác định vị trí");
            has_GPS_on = false;
            // do chưa có GPS nên ko hiện marker vị trí hiện tại của mình
            myLocationOverlay.setEnabled(false);
        }

        //  The location object returned may be null in rare cases when the location is not available. --> check first
        if (firstLocation && mCurrentLocation != null) {
            firstLocation = false;
            herePoint = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        }

        // make "Map" appear
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
            // // Đây là TH khi người dùng nhấn phím lấy address mà chưa kết nối được google, thì sau khi kết nối được thì mới lấy
            if (mAddressRequested) {
                startIntentService();
            }

            // make url to retrieve route between my location and another location
            mode = transportation; // driving, walking, bicycling, transit
            
            if (mode != null) {
                urlForDirections = makeURL(
                        mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude(),
                        endPoint.getLatitude(),
                        endPoint.getLongitude(), mode);
            }
            if (urlForDirections != null) {
                new connectAsyncTask(urlForDirections).execute();
            }
        }

        createLocationRequest();
        if (!mRequestingLocationUpdates) { // if location update is currently turned off --> turn it on. 
	        startLocationUpdates();
	    }
    }

    /**
     * LocationRequest: chứa tham số cho FusedLocationProviderApi.
     * 
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

        if (mCurrentLocation != null){
            has_GPS_on = true;
            newLocation = new GeoPoint(mCurrentLocation);
        }

        // nếu mà myLocation chưa bật lên thì bật nó lên
        if (!myLocationOverlay.isEnabled()) {
            myLocationOverlay.setEnabled(true);
        }

        // nếu myLocation khác null
        if (myLocationOverlay != null)
            prevLocation = myLocationOverlay.getLocation();

        myLocationOverlay.setLocation(newLocation); // dịch marker tương ứng
        myLocationOverlay.setAccuracy((int)mCurrentLocation.getAccuracy());

        // prevL = null khi lúc đầu mới biết địa chỉ GPS --> lúc đầu speed = 0 tên khỏi tính
        if (prevLocation != null && mCurrentLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            mSpeed = mCurrentLocation.getSpeed() * 3.6;
            //long speedInt = Math.round(mSpeed);
            // xuất ra màn hình speed của xe

            if (mSpeed >= 0.1) {
                mAzimuthAngleSpeed = mCurrentLocation.getBearing();
                myLocationOverlay.setBearing(mAzimuthAngleSpeed);
            }
        }

        // bật và ko bật chế độ Tracking
        if (mTrackingMode) {
            map.setMapOrientation(-mAzimuthAngleSpeed); // xoay map dựa vào góc
            mapController.animateTo(newLocation);
        }
        // ko bật thì ko làm gì hết

        // marker, if location change then update marker + draw route again
        herePoint = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        // dat. ban do` tai vi. tri' hien. tai cua? minh` chỉ lần đầu tiên mới center, các lần sau ko dc tự ý center
        if (has_GPS_on && first_center_map) {
            first_center_map = false;
            mapController.setCenter(herePoint);
        }

        hereMarker.setPosition(herePoint);
        hereMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // dat. ngon' tay tai. ngay vi. tri diem startpoint, nam` tren diem? do'
        hereMarker.setTitle("My location"); // click vao startMaker se~ hien. chu~ nay`
        hereMarker.setIcon(ContextCompat.getDrawable(this, R.mipmap.here3));


        if (mapOverlay.contains(hereMarker))
            mapOverlay.remove(hereMarker); // xóa cái here marker cũ
        //mapOverlay.add(hereMarker); // dán cái here marker mới 

        
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        // update route between my location and end point
        if (mode != null) {
            urlForDirections = makeURL(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(),
                    endPoint.getLatitude(),
                    endPoint.getLongitude(), mode);
            //showToast("Ve lai quang duong di giua 2 diem" + urlForDirections);
        }
        if (urlForDirections != null) {
            new connectAsyncTask(urlForDirections).execute();
        }

        map.invalidate();
        updateUI();
    }

    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // phải thêm vào vẽ các marker start, end, via point nữa do ta đã phục hồi được giá trị của nó 
    private void updateUI() {
        text_lat.setText(String.valueOf(mCurrentLocation.getLatitude()));
        text_long.setText(String.valueOf(mCurrentLocation.getLongitude()));
        text_time.setText(mLastUpdateTime);
        // Xuat' ra dong chu~ len man hinh
        //Toast.makeText(this, getResources().getString(R.string.location_updated_message),Toast.LENGTH_SHORT).show();
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
     * 1. Make a map
     * 2. Set multitouch to zoom map
     * 3. Set map center at my Location 
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            final DisplayMetrics dm = context.getResources().getDisplayMetrics();
            // Khai bao' thong^ so' cho Map
            map = (MapView) findViewById(R.id.map);// 2 dòng sau sẽ thấy dc world map
            map.setTileSource(TileSourceFactory.MAPNIK);
            // Khai báo overlay cho map
            mapOverlay = map.getOverlays();

            mapEventsOverlay = new MapEventsOverlay(this, this);// tạo map event overlay
            if (map != null) {// add thêm la bàn vào
                mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), map);
                mCompassOverlay.enableCompass();
            }
            mScaleBarOverlay = new ScaleBarOverlay(context); // thanh scale trên bản đồ
            mScaleBarOverlay.setCentred(true);
            mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
            if (map != null) { // tao ban do nho?
                mMinimapOverlay = new MinimapOverlay(context, map.getTileRequestCompleteHandler());
                mMinimapOverlay.setWidth(dm.widthPixels / 5);
                mMinimapOverlay.setHeight(dm.heightPixels / 5);
            }

            // icon huon'g dan~
            if (nodeIcon == null) {
                nodeIcon = ContextCompat.getDrawable(context, R.drawable.marker_node);
            }

            if (iconContinue == null) {
                iconContinue = ContextCompat.getDrawable(context, R.drawable.ic_continue);
            }

            // chỉ đường
            if (myLocationOverlay == null) {
                myLocationOverlay = new DirectedLocationOverlay(context);

            }

            // diem tung gian
            if (viaPoints == null) {
                viaPoints = new ArrayList<GeoPoint>();
            }

            // marker hành trình
            if (mItineraryMarkers == null) {
                mItineraryMarkers = new FolderOverlay(context);
                mItineraryMarkers.setName(getString(R.string.itinerary_markers_title));
            }

            // cửa sổ marker hành trình
            if (mViaPointInfoWindow == null) {
                mViaPointInfoWindow = new ViaPointInfoWindow(R.layout.itinerary_bubble, map);
            }

            // nút tracking
            if (mTrackingModeButton == null) {
                mTrackingModeButton = (Button)findViewById(R.id.buttonTrackingMode);
            }

            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }

            // control the map
            mapController = map.getController();
            if (mapController != null) {
                controlMap();
            }

            // location manager
            if (mLocationManager == null) {
                mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            }

            // gọi hàm này để vẽ 3 marker là startPoint, destPoint, va viaPoint nếu như đã có rồi
            updateUIWithItineraryMarkers();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     */
    private void setUpMap() {
        //map.setBuiltInZoomControls((true)); // thêm thanh zoom cho map
        map.setMultiTouchControls(true);
        hereMarker = new Marker(map); // khai bao' marker xac dinh. vi tri' hien. tai. cua chung ta
        endMarker = new Marker(map); // khai bao' marker xac dinh. vi tri' hien. tai. cua chung ta

        // add overlay vào map: event, chỉ đường, marker hành trình, la bàn, thanh scale, bản đồ mini, here map, endmap
        mapOverlay.add(0, mapEventsOverlay); // add cái event này vào overlay trên map ở vị trí cuối cùng
        if (myLocationOverlay != null) {
            mapOverlay.add(myLocationOverlay);
            //myLocationOverlay.setEnabled(false); // không bật chỉ đường theo thời gian thực
        }
        if (mItineraryMarkers != null) {
            mapOverlay.add(mItineraryMarkers);
        }
        if (mCompassOverlay != null)
            mapOverlay.add(mCompassOverlay);
        mapOverlay.add(mScaleBarOverlay);
        mapOverlay.add(mMinimapOverlay);

        hereMarker.setPosition(herePoint);
        hereMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // dat. ngon' tay tai. ngay vi. tri diem startpoint, nam` tren diem? do'
        hereMarker.setTitle("My location"); // click vao startMaker se~ hien. chu~ nay`
        hereMarker.setIcon(ContextCompat.getDrawable(this, R.mipmap.here3));
        //mapOverlay.add(hereMarker); // dan' vao map

        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // dat. ngon' tay tai. ngay vi. tri diem startpoint, nam` tren diem? do'
        endMarker.setTitle("Destination"); // click vao startMaker se~ hien. chu~ nay`
        endMarker.setIcon(ContextCompat.getDrawable(this, R.mipmap.here2));
        mapOverlay.add(endMarker);

        // listen cái nút tracking xem nó có được nhấn ko
        if (mTrackingModeButton != null) {
            mTrackingModeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    mTrackingMode = !mTrackingMode; // khi nhấn thì thay đổi giữa co nhấn hay ko
                    updateUIWithTrackingMode();
                }
            });
        }

    }

    /**
     * Nếu chưa tìm được thì nó zoom 15 và set tọa độ 0, 0 để load map 
     */
    private void controlMap() { 
        mapController.setZoom(prefs.getInt("MAP_ZOOM_LEVEL", 15)); // lấy giá trị trong tủ để làm giá tr zoom
        //mapController.setCenter(herePoint); // lấy giá trị trong tủ là vị trí center của map trước khi Application bị phá hủy
        mapController.setCenter(new GeoPoint( (double)prefs.getFloat("MAP_CENTER_LAT", (float)herePoint.getLatitude()),
                (double)prefs.getFloat("MAP_CENTER_LON", (float)herePoint.getLongitude()) ));
    }

    /**
     * Xuất ra dòng chữ "Vị trí mà mình tap"
     * @param  p Tọa độ click trên bản đồ
     * @return   true nếu ta có lập trình tap
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        //Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Tap on ("+p.getLatitude()+","+p.getLongitude()+")", Toast.LENGTH_SHORT).show();
        return true; // return true la sử dụng event này
    }

    /**
     * Khi nhấn lâu, thì vẽ marker, tìm đường từ vị trí hiện tại đến marker
     * @param  p [description]
     * @return   [description]
     */
    @Override
    public boolean longPressHelper(GeoPoint p) {
        endMarker.setPosition(p);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // dat. ngon' tay tai. ngay vi. tri diem startpoint, nam` tren diem? do'
        endMarker.setTitle("Destination"); // click vao startMaker se~ hien. chu~ nay`
        endMarker.setIcon(ContextCompat.getDrawable(this, R.mipmap.here2));

        if (mapOverlay.contains(endMarker))
            mapOverlay.remove(endMarker); // xóa endmarker cũ
        mapOverlay.add(endMarker); // add endmarker mới

        endPoint = p;

        // update route between my location and end point
        if (mode != null) {
            urlForDirections = makeURL(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(),
                    endPoint.getLatitude(),
                    endPoint.getLongitude(), mode);
            //showToast("Ve lai quang duong di giua 2 diem" + urlForDirections);
        }
        if (urlForDirections != null) {
            new connectAsyncTask(urlForDirections).execute();
        }
        map.invalidate();
        return true;
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
        mAddressRequested = true;
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
    public String makeURL(double sourcelat, double sourcelog, double destlat, double destlog, String travelMode) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString.append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString(destlog));
        urlString.append("&sensor=false&mode="+travelMode+"&alternatives=true"); //  phương tiện gì
        return urlString.toString();
    }

    // class connectAsyncTask
    public class connectAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        String url;

        /////////////////
        ////////////////////
        // Constructor //////
        ////////////////////
        /////////////////

        /**
         * pass URL cho class này để nó kiếm JSON
         * pass Map để nó vẽ lên map đó
         */
        connectAsyncTask(String urlPass) {
            url = urlPass;
        }


        //////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////
        // Method chạy nền, để hỏi google route giữa 2 điểm sau đó trả về đường đi  // // //
        ////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////

        /**
         * Xuất ra bảng thông báo là đang tìm đường đi ra màn hình
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //progressDialog = new ProgressDialog(MapsActivity.this);
            //progressDialog.setMessage("Fetching route, Please wait...");
            //progressDialog.setIndeterminate(true);
            //progressDialog.show();
        }

        /**
         * Từ URL ta sẽ kiếm được json chứa data về đường đi dạng "string"
         *
         * @param params địa chỉ URL chứa thông tin route giưa 2 điểm
         * @return JSON string data chứa route giữa 2 điểm
         */
        @Override
        protected String doInBackground(Void... params) {
            String json = getJSONFromUrl(url); // lay json từ url này
            // json: là 1 dạng dự liệu dc lưu trên CSDL (dạng key+value)
            return json;
        }

        /**
         * Vẽ đường đi khi ta tìm được data giữa 2 điểm rồi
         *
         * @param result JSON string data chứa route giữa 2 điểm
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //progressDialog.hide();
            if (result != null) {
                drawPath(result); // khi được thông tin(JSON) dựa vào server rồi thì vẽ đường đi
            }
        }
    }


    /////////////////////////////////////
    ////////////////////////////////////////
    ///////////////////////////////////////////
    // Method phụ do method chính gọi  // // //
    ///////////////////////////////////////////
    ////////////////////////////////////////
    /////////////////////////////////////
    /**
     * Lấy dư liệu từ URL mà srever trả về
     * @param  url địa chỉ web server cần lấy jason
     * @return     JSON đường đi giữa 2 điểm
     */
    public String getJSONFromUrl(String url) {
        StringBuilder stringBuilder = new StringBuilder();
        Long startTime = System.currentTimeMillis();
        // lay address
        URL url1 = null;
        try {
            url1 = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // ket noi den address do
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url1.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // phuong thuc ket noi
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        // read the response
        // noi cai ma server tra ve vao 1 string
        try{
            if (connection.getResponseCode() == 201 || connection.getResponseCode() == 200){
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null){
                    stringBuilder.append(line);
                }
                inputStream.close();
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error in request");
            // e.printStackTrace();
        }
        // This returns a String object representing the value of this Integer.
        Long timeTaken = System.currentTimeMillis() - startTime;
        Log.d("RoadTask.doTask", "time taken: " + timeTaken);
        return stringBuilder.toString();
    }


    /**
     * Vẽ đường đi giữa 2 điểm trên bản đồ
     * @param result JASON đã nhận được từ server
     */
    public void drawPath(String result) {
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>(); // tao 1 array cac toạ dộ
        GeoPoint startPoint = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        waypoints.add(startPoint); // thêm điểm đầu

        try {
            final JSONObject json = new JSONObject(result); // lưu JSON mà server trả
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes
                    .getJSONObject("overview_polyline"); // duong di cua google
            String duration = routes.getJSONArray("legs").getJSONObject(0)
                    .getJSONObject("duration").getString("text"); // thời gian
            String distance = routes.getJSONArray("legs").getJSONObject(0)
                    .getJSONObject("distance").getString("text"); // khoảng cách

            //Toast.makeText(MapsActivity.this, ("Total Road Duration: " + duration + "  / Total Distance: " + distance), Toast.LENGTH_LONG).show(); // thời gian + khoảng cách
            String encodedString = overviewPolylines.getString("points"); // lấy value với kye là "point"
            List<GeoPoint> list = decodePoly(encodedString); // hàm này return 1 list Geopoint doc  đường đi

            for (int z = 0; z < list.size() - 1; z++) {
                GeoPoint src = list.get(z);
                waypoints.add(src);
                /*
                // ve~ cac marker giua~ cac node
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(src);
                nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                nodeMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.marker_node));
                mapOverlay.add(nodeMarker);
                */
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        waypoints.add(endPoint); // add điểm cuối


        RoadManager roadManager = new OSRMRoadManager();
        Road road = roadManager.getRoad(waypoints);  // vẽ đường đi nối các điểm lại với nhau  --> cũng nhờ server vẽ hộ
        //Polyline roadOverlay = RoadManager.buildRoadOverlay(road, MapsActivity.this); // hàm vẽ mình tự lập trình

        if (roadOverlay != null)
            mapOverlay.remove(roadOverlay);

        roadOverlay = RoadManager.buildRoadOverlay(road, 0x80000080, 11.0f, context); // hàm vẽ mình tự lập trình
        mapOverlay.add(roadOverlay);
        if (mapOverlay.contains(mMinimapOverlay))
            mapOverlay.remove(mMinimapOverlay);
        mapOverlay.add(mMinimapOverlay);

        map.invalidate();
    }

    /**
     * Trả về rất nhiều điểm từ đây đến đích để ta có thể vẽ được 1 đường
     * @param  encoded [description]
     * @return         [description]
     */
    private List<GeoPoint> decodePoly(String encoded) {
        List<GeoPoint> poly = new ArrayList<GeoPoint>(); // list geopoint
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint p = new GeoPoint((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


    /**
     * Hàm xóa cái bảng thông báo dẫn đường đi 
     * @param index [description]
     */
    public void removePoint(int index) {

    }

    /**
     * hiển thị các marker nếu ta đã biết tọa độ đầu và tọa độ cuối 
     */
    public void updateUIWithItineraryMarkers() {
        // start marker
        if (startPoint != null) {

        }

        // viapoint
        
        // destination marker
        if (destinationPoint != null) {

        }
    }

    /**
     * Hàm này sẽ update map dựa vào ta có nhấn nút tracking hay ko
     */
    public void updateUIWithTrackingMode() {
        // nếu co nhấn
        if (mTrackingMode) {
            // load hình có nhấn
            mTrackingModeButton.setBackgroundResource(R.drawable.btn_tracking_on);
            mTrackingModeButton.setKeepScreenOn(true); // trong quá trình tìm đường thì không tắt màn hình
            // nếu như có vi trí hiện tại của ta mới center ngay tại map
            if (myLocationOverlay.isEnabled() && myLocationOverlay.getLocation() != null) {
                mapController.animateTo(myLocationOverlay.getLocation());
            }
            map.setMapOrientation(-mAzimuthAngleSpeed);
        }
        // nếu ko nhấn, load hình, xoay map lại ban đầu, tắt màn hình
        else {
            mTrackingModeButton.setBackgroundResource(R.drawable.btn_tracking_off);
            map.setMapOrientation(0.0f);
            mTrackingModeButton.setKeepScreenOn(false);
        }
    }

}
