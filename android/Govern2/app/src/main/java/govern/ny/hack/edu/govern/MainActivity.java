package govern.ny.hack.edu.govern;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.github.clans.fab.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import govern.ny.hack.edu.govern.models.GovModel;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ReportDialogFragment.ReportEditTextListener {



    private FirebaseAuth mFirebaseauth;
    private FirebaseAuth.AuthStateListener mAuthstateListener;
    private static final int RC_SIGN_IN = 123;
    private UserModel mUserModel;

    private GoogleMap mMap;
    GeoDataClient mGeoDataClient;
    PlaceDetectionClient mPlaceDetectionClient;
    FusedLocationProviderClient mFusedLocationProviderClient;
    boolean mLocationPermissionGranted = false;
    static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    Location mLastKnownLocation;
    LatLng mDefaultLocation;
    float DEFAULT_ZOOM = 17.0f;
    List<LatLng> latLngList = Collections.synchronizedList(new ArrayList<LatLng>());
    private Unbinder bind;

    private DatabaseReference mDatabase;

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirebaseauth = FirebaseAuth.getInstance();
        mUserModel = new UserModel();




        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // Boilerplate code to get the location from the phone
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Create a reference to the firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();


        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        mAuthstateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mFirebaseauth.getCurrentUser();
                if (user != null) {
                    //user is signed in
                    mUserModel.setmPhoneNumber(user.getPhoneNumber());
                    if (user.getPhotoUrl() != null) {
                        mUserModel.setmPhotourl(user.getPhotoUrl().toString());
                    }
                    mUserModel.setmUid(user.getUid());
                    mUserModel.setmUserName(user.getDisplayName());
                } else {
                    //user is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        bind = ButterKnife.bind(this);


        mDatabase.child("GovernanceLocations").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                GovModel newPost = dataSnapshot.getValue(GovModel.class);
                latLngList.add(new LatLng(newPost.getLatitude(), newPost.getLongitude()));
                //updateMapUI();
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(newPost.getLatitude(),
                                newPost.getLongitude()))
                        .radius(50)
                        .strokeColor(Color.argb(180, 0, 0, 255))
                        .fillColor(Color.argb(70, 0, 0, 255)));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sign_out_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                //Toast.makeText(MainActivity.this, "User Signed In!", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(MainActivity.this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseauth.removeAuthStateListener(mAuthstateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseauth.addAuthStateListener(mAuthstateListener);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        mDefaultLocation = new LatLng(-34, 151);


        getLocationPermission();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        mMap.getUiSettings().setMyLocationButtonEnabled(true);

    }


    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataHeader.add("Upper East Manhattan");
        listDataHeader.add("Central Park");
        listDataHeader.add("Columbia University");

        // Adding child data
        List<String> top250 = new ArrayList<String>();
        top250.add("Lampost on West 120th St");
        top250.add("Ensure consruction safety on sidewalk");



        List<String> nowShowing = new ArrayList<String>();
        nowShowing.add("Park is not clean");
        nowShowing.add("Eve teasing");
        nowShowing.add("Poor condition of benches");

        List<String> comingSoon = new ArrayList<String>();
        comingSoon.add("Street lamp issue");
        comingSoon.add("Navigation signs are not clear");
        comingSoon.add("Premises are not clean");

        listDataChild.put(listDataHeader.get(0), top250); // Header, Child data
        listDataChild.put(listDataHeader.get(1), nowShowing);
        listDataChild.put(listDataHeader.get(2), comingSoon);
    }

    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                getDeviceLocation();
                //getGovernorList();
            } else {
                mMap.setMyLocationEnabled(false);
                //mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            LatLng currentLatLng = new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude());
                            zoomInAddMarker(mMap, currentLatLng);
                            getGovernorList();
                        } else {
                            Log.d("Map Activity", "Current location is null. Using defaults.");
                            Log.e("Map Activity", "Exception: %s", task.getException());
                            zoomInAddMarker(mMap, mDefaultLocation);
                            //mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void zoomInAddMarker(GoogleMap map, LatLng latLng) {
        map.addMarker(new MarkerOptions().position(latLng)
                .title("Your Position"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }


    private void getGovernorList() {
        // TODO HTTP get request here
        // call back should call populateGovernorList with the new list here
        if (this.mLastKnownLocation != null)
            populateGovernorList(new ArrayList<LatLng>());
    }

    private void populateGovernorList(List<LatLng> fetchedList) {
        // Using a mock list of governor lat longs
        // start Mock
        fetchedList = new ArrayList<LatLng>();
        fetchedList.add(new LatLng(40.812256, -73.962573));
        fetchedList.add(new LatLng(40.806655, -73.965684));
        fetchedList.add(new LatLng(40.806616, -73.959974));
        fetchedList.add(new LatLng(40.804679, -73.965597));
        fetchedList.add(new LatLng(40.804565, -73.965297));
        // end Mock

        this.latLngList = fetchedList;

        updateMapUI();
    }


    private void updateMapUI() {
        //Toast.makeText(MainActivity.this, "Hello : "+ this.latLngList, Toast.LENGTH_SHORT).show();
        for (LatLng latLng : this.latLngList) {
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(latLng.latitude,latLng.longitude))
                    .radius(100)
                    .strokeColor(Color.argb(180, 0, 0, 255))
                    .fillColor(Color.argb(70, 0, 0, 255)));
        }
    }


    private void addAsNewGoverner(LatLng latLng){
        addNewGoverner(mUserModel.getmUid(), latLng);
    }

    private void addNewGoverner(String userID, LatLng latLng){
        GovModel gm = new GovModel();
        gm.setIssues(new ArrayList<String>());
        gm.setLatitude(latLng.latitude);
        gm.setLongitude(latLng.longitude);
        gm.setOwnerID(userID);
        mDatabase.child("GovernanceLocations").child(latToBat(latLng)).setValue(gm);
    }

    private String latToBat(LatLng latLng){
        if(latLng == null)
            return "";
        String s1[] = (Double.toString(latLng.latitude)+".").split("\\.");
        String s2[] = (Double.toString(latLng.longitude)+".").split("\\.");
        String res = "BAT";
        for(String s : s1)
            res += s+"BAT";
        for(String s : s2)
            res += s+"BAT";
        return res;
    }

    private LatLng batToLat (String bat){
        if(bat == null)
            return mDefaultLocation;

        String s[] = bat.split("BAT");

        double d1 = Double.parseDouble(s[1]+"."+s[2]);
        double d2 = Double.parseDouble(s[3]+"."+s[4]);

        LatLng res = new LatLng(d1, d2);
        return res;
    }

    @Override
    public void onFinishReportDialog(String value, LatLng location) {
        //Toast.makeText(MainActivity.this, "Submit clicked", Toast.LENGTH_SHORT).show();
        submitReport(value, location);
        Toast.makeText(MainActivity.this, "Report submitted", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.material_design_floating_action_menu_item2)
    public void onReportSelected(View view) {
        //Toast.makeText(MainActivity.this, "Report Text", Toast.LENGTH_SHORT).show();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        LatLng govLoc = isAnyGovernerNearby();
        if(govLoc!=null){
            // Create and show the dialog.
            ReportDialogFragment newFragment = ReportDialogFragment.newInstance(govLoc.latitude, govLoc.longitude);
            newFragment.show(ft,"dialog");
        }
        else{
            Toast.makeText(MainActivity.this, "Place Not governed by anyone, report not submitted!", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.material_design_floating_action_menu_item1)
    public void onGovernClicked(View view){
        //Toast.makeText(MainActivity.this, "Govern Clicked", Toast.LENGTH_SHORT).show();
        if(mLastKnownLocation!=null) {
            //Toast.makeText(MainActivity.this, "trying to add new governor at : "+ mLastKnownLocation.getLatitude()+","+
                    //mLastKnownLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            addAsNewGoverner(new LatLng(mLastKnownLocation.getLatitude(),
                    mLastKnownLocation.getLongitude()));
        }
        else{
            //Toast.makeText(MainActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
        }
    }

    public synchronized LatLng isAnyGovernerNearby(){
        for(LatLng ll : this.latLngList){
            if(diff(ll, new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude())) < 0.002){
                return ll;
            }
        }
        return null;
    }

    public double diff (LatLng l1, LatLng l2){
        return Math.sqrt((l1.latitude - l2.latitude) * (l1.latitude - l2.latitude) +
                (l1.longitude - l2.longitude) * (l1.longitude - l2.longitude));
    }

    public void submitReport(final String report, LatLng location){
        mDatabase.child("GovernanceLocations").child(latToBat(location)).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                GovModel govModel = mutableData.getValue(GovModel.class);
                if(govModel.getIssues() == null){
                    govModel.setIssues(new ArrayList<String>());
                }
                govModel.getIssues().add(report);

                mutableData.setValue(govModel);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
            }
        });
    }

}

