package com.firebase.sfvehicles;

import android.app.AlertDialog;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EditText;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.sfvehicles.model.CarInfo;
import com.firebase.sfvehicles.model.CarPos;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.studio.rai.live2d2.Live2DRender;
import com.studio.rai.live2d2.live2d.L2DModelSetting;
import com.studio.rai.live2d2.live2d.MyL2DModel;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jp.live2d.Live2D;

public class SFVehiclesActivity extends FragmentActivity implements GeoQueryEventListener, GoogleMap.OnCameraChangeListener {

    private final String TAG = SFVehiclesActivity.class.getSimpleName();

    private static final GeoLocation INITIAL_CENTER = new GeoLocation(25.072844, 121.5210583);
    private static final int INITIAL_ZOOM_LEVEL = 14;
    private static final String GEO_FIRE_DB = "https://crazytaxi-b3f28.firebaseio.com";
    private static final String GEO_FIRE_REF = GEO_FIRE_DB + "/cars_pos";

    private GoogleMap map;
    //private Circle searchCircle;
    private GeoFire geoFire;
    private GeoQuery geoQuery;

    private Map<String,Marker> markers;

    private List<CarInfo> carInfoList = new ArrayList<>();
    private List<CarPos> carPosList = new ArrayList<>();

    private Live2DRender mLive2DRender;
    private L2DModelSetting mModelSetting;
    private MyL2DModel mModel;
    private GLSurfaceView mGlSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //setContentView(R.layout.activity_sfvehicles);
        setContentView(R.layout.activity_main);

        Live2D.init();
        initView();

/*

        // setup map and camera position
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        this.map = mapFragment.getMap();
        LatLng latLngCenter = new LatLng(INITIAL_CENTER.latitude, INITIAL_CENTER.longitude);
        //this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
        //this.searchCircle.setFillColor(Color.argb(125, 255, 255, 255));
        //this.searchCircle.setStrokeColor(Color.argb(255, 0, 0, 0));
        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        this.map.setOnCameraChangeListener(this);

        FirebaseOptions options = new FirebaseOptions.Builder().setApplicationId("geofire").setDatabaseUrl(GEO_FIRE_DB).build();
        FirebaseApp app = FirebaseApp.initializeApp(this, options);

        // setup GeoFire
        this.geoFire = new GeoFire(FirebaseDatabase.getInstance(app).getReferenceFromUrl(GEO_FIRE_REF));
        // radius in km
        this.geoQuery = this.geoFire.queryAtLocation(INITIAL_CENTER, 1);

        // setup markers
        this.markers = new HashMap<String, Marker>();
*/


    }

    private void initView() {



        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.main_glSurface);
        //mGlSurfaceView.setZOrderOnTop(true);

        //et = (EditText) findViewById(R.id.main_et);

        setupLive2DModels();
        mGlSurfaceView.setRenderer(mLive2DRender);


        //initButton();
    }

    private void setupLive2DModels() {
        try {
            //String modelName = "tsumiki";
            String modelName = "Epsilon_free";
            //String modelName = "izumi_illust";
            //String modelName = "hibiki";
            mModelSetting = new L2DModelSetting(this, modelName);
            mModel = new MyL2DModel(this, mModelSetting);

            mLive2DRender = new Live2DRender();
            mLive2DRender.setModel(mModel);


        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/*
    @Override
    protected void onStop() {
        super.onStop();
        // remove all event listeners to stop updating in the background
        this.geoQuery.removeAllListeners();
        for (Marker marker: this.markers.values()) {
            marker.remove();
        }
        this.markers.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // add an event listener to start updating locations again
        this.geoQuery.addGeoQueryEventListener(this);
    }
*/
    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        // Add a new marker to the map


        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.taxi2);

        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(location.latitude, location.longitude))
                .title("Current Location")
                .snippet("Thinking of finding some thing...")
                .icon(icon)
                .rotation(0.0f);

        Marker marker = this.map.addMarker(markerOptions);

        //Marker marker = this.map.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)));

        this.markers.put(key, marker);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        carsInfoListener(database);
        carsPosListener(database);
    }

    private void carsInfoListener(final FirebaseDatabase database){
        DatabaseReference myRef = database.getReference("cars_info");

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                CarInfo carInfo = dataSnapshot.getValue(CarInfo.class);
                carInfo.setKey(dataSnapshot.getKey());
                if (carInfoList.contains(carInfo)) {

                } else {
                    carInfoList.add(carInfo);
                }

//                String aaa = String.valueOf(dataSnapshot.child("driver_name").getValue());
//                String key  = dataSnapshot.getKey();
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

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                CarInfo carInfo = dataSnapshot.getValue(CarInfo.class);
                carInfo.setKey(dataSnapshot.getKey());
                if (carInfoList.contains(carInfo)) {

                } else {
                    carInfoList.add(carInfo);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void carsPosListener(FirebaseDatabase database){
        final DatabaseReference carsPosDataBase = database.getReference("cars_pos");

        carsPosDataBase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                CarPos carPos = dataSnapshot.getValue(CarPos.class);
                carPos.setKey(dataSnapshot.getKey());
                if (carPosList.contains(carPos)) {

                } else {
                    carPosList.add(carPos);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                CarPos carPos = dataSnapshot.getValue(CarPos.class);
                carPos.setKey(dataSnapshot.getKey());
                if (carPosList.contains(carPos)) {

                    CarPos prevCarPos = carPosList.get(carPosList.indexOf(carPos));
                    Float prevLat = prevCarPos.getL().get(0);
                    Float prevLong = prevCarPos.getL().get(1);

                    Float newLat = carPos.getL().get(0);
                    Float newLong = carPos.getL().get(1);

                    Location prevLocation = new android.location.Location(LocationManager.GPS_PROVIDER);
                    prevLocation.setLatitude(prevLat);
                    prevLocation.setLongitude(prevLong);

                    Location newLocation = new android.location.Location(LocationManager.GPS_PROVIDER);
                    newLocation.setLatitude(newLat);
                    newLocation.setLongitude(newLong);

                    float bearing = prevLocation.bearingTo(newLocation) ;
                    Marker marker = SFVehiclesActivity.this.markers.get(carPos.getKey());

                    if(marker != null && bearing != 0.0) {
                        marker.setRotation(bearing);
                    }

                    carPosList.remove(carPosList.indexOf(carPos));
                    carPosList.add(carPos);

                } else {
                    carPosList.add(carPos);
                }
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

        carsPosDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }


    @Override
    public void onKeyExited(String key) {
        // Remove any old marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            marker.remove();
            this.markers.remove(key);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        // Move the marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            this.animateMarkerTo(marker, location.latitude, location.longitude);
        }
    }

    @Override
    public void onGeoQueryReady() {
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("There was an unexpected error querying GeoFire: " + error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Animation handler for old APIs without animation support
    private void animateMarkerTo(final Marker marker, final double lat, final double lng) {
//        final Handler handler = new Handler();
//        final long start = SystemClock.uptimeMillis();
//        final long DURATION_MS = 10000;
//        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
//        final LatLng startPosition = marker.getPosition();
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                float elapsed = SystemClock.uptimeMillis() - start;
//                float t = elapsed/DURATION_MS;
//                float v = interpolator.getInterpolation(t);
//
//                double currentLat = (lat - startPosition.latitude) * v + startPosition.latitude;
//                double currentLng = (lng - startPosition.longitude) * v + startPosition.longitude;
//                marker.setPosition(new LatLng(currentLat, currentLng));
//
//                // if animation is not finished yet, repeat
//                if (t < 1) {
//                    handler.postDelayed(this, 16);
//                }
//            }
//        });

        marker.setPosition(new LatLng(lat, lng));
    }

    private double zoomLevelToRadius(double zoomLevel) {
        // Approximation to fit circle into view
        return 16384000/Math.pow(2, zoomLevel-1);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // Update the search criteria for this geoQuery and the circle on the map
        LatLng center = cameraPosition.target;
        double radius = zoomLevelToRadius(cameraPosition.zoom);
        //this.searchCircle.setCenter(center);
        //this.searchCircle.setRadius(radius);
        this.geoQuery.setCenter(new GeoLocation(center.latitude, center.longitude));
        // radius in km
        this.geoQuery.setRadius(radius/1000);
    }
}
