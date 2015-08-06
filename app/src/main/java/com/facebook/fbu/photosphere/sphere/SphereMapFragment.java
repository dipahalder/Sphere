// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;

import java.util.List;

import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.SpheresLoadedCallback;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.fetchAllSpheres;
import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Map view with photospheres loaded onto it, loads onto users current position
 */
public class SphereMapFragment extends SupportMapFragment implements
        SpheresLoadedCallback,
        OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = SphereMapFragment.class.getSimpleName();
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final String LOCATION_SHARED_PREFERENCES = "LOCATION";
    private static final String LONGITUDE = "Longitude";
    private static final String LATITUDE = "Latitude";
    private static final LatLng sMenloParkLatLng = new LatLng(37.48, -122.14);
  
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private LatLng mCurrentLocation;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        getLocation();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        getMapAsync(new OnMapReadyCallback() {
          @Override
          public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            mMap.setOnMarkerClickListener(SphereMapFragment.this);
            fetchAllSpheres(SphereMapFragment.this);
            updateUI();
          }
        });
    }

    private void setLastKnownLocation() {
      SharedPreferences settings = getActivity()
          .getSharedPreferences(LOCATION_SHARED_PREFERENCES, 0);
      //Keep Mountain View coordinates as Default
      double longitude = settings.getFloat(LONGITUDE, (float) sMenloParkLatLng.longitude);
      double latitude = settings.getFloat(LATITUDE, (float) sMenloParkLatLng.latitude);
      mCurrentLocation = new LatLng(latitude, longitude);
      Log.v(TAG, "Setting last known location to: "+latitude+""+longitude);
      updateUI();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String photoID = marker.getSnippet();
        if (photoID == null) {
            return false;
        }
        //Replace container with detail view fragment
        DetailViewFragment fragment = DetailViewFragment.newInstance(photoID);
        FragmentTransaction fragmentTransaction = getActivity()
            .getSupportFragmentManager()
            .beginTransaction();
        fragmentTransaction.add(R.id.realtabcontent, fragment);
        fragmentTransaction.addToBackStack(null).commit();

        return true;
    }

    // creates location request, on location updated sets current location and calls the method to
    // load spheres
    private void getLocation() {
      LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
        Log.i(TAG, "Request: " + request);
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, request, new LocationListener() {
                  @Override
                  public void onLocationChanged(Location loc) {
                    Log.i(TAG, "Got a fix: " + loc);
                    mCurrentLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                    updateUI();
                  }
                });
    }


    /**
     * handles the loading of the spheres, adding them as markers to the map, and to the hashmap
     * @param photoSpheres- list of PhotoOrbs returned from parse query
     */
    @Override
    public void onSpheresLoaded(List<Sphere> photoSpheres) {
        Resources resources = getResources();
        int iconSize = resources.getDimensionPixelSize(R.dimen.pin_icon_size);
        int roundedCorner = resources.getDimensionPixelSize(R.dimen.corner_round_factor);
        int borderThickness = resources.getDimensionPixelSize(R.dimen.border_thickness);
        mMap.clear();
        for(Sphere sphere : photoSpheres) {
            ParseFile thumbnailFile = sphere.getThumbnail();
            try {
                byte[] byteData = new byte[0];
                byteData = thumbnailFile.getData();
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteData, 0, byteData.length);
                ParseGeoPoint sphereLoc = sphere.getCoordinates();
                // creates marker with thumbnail as icon and ObjectId as snippet
                MarkerOptions sphereMarker = new MarkerOptions()
                    .position(new LatLng(sphereLoc.getLatitude(), sphereLoc.getLongitude()))
                    .icon(BitmapDescriptorFactory
                            .fromBitmap(getRoundedRectBitmap(
                            bitmap,
                            Color.WHITE,
                            iconSize,
                            borderThickness,
                            roundedCorner)))
                    .snippet(sphere.getObjectId());
                mMap.addMarker(sphereMarker);

            } catch (ParseException e) {
                Log.e(TAG, "error when handling loaded spheres: " + e);
            }
        }
        updateUI();
    }

    @Override
    public void onSphereLoadFailed() {
        //TODO: handle failure of loading of spheres in an area
    }

    /**
     * updates the UI with current location, moving the map accordingly
     */
    private void updateUI() {
        if (mMap == null) {
            return;
        }
        if (mCurrentLocation == null) {
            mCurrentLocation = sMenloParkLatLng;
        }
        MarkerOptions myMarker = new MarkerOptions()
            .position(mCurrentLocation);
        mMap.addMarker(myMarker).setVisible(false);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 10.0f);
        mMap.animateCamera(update);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, location.toString());
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();
        mCurrentLocation = new LatLng(lat, lng);
        updateUI();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // starts an activity to resolve the error
                connectionResult.startResolutionForResult(
                        getActivity(),
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "error in onConnectionFailed: " + e);
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " +
                    connectionResult.getErrorCode());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setLastKnownLocation();
        fetchAllSpheres(SphereMapFragment.this);
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Store last known location for load speed purposes
        double longitude = mCurrentLocation.longitude;
        double latitude = mCurrentLocation.latitude;
        SharedPreferences settings = getActivity()
            .getSharedPreferences(LOCATION_SHARED_PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(LONGITUDE, (float) longitude);
        editor.putFloat(LATITUDE, (float) latitude);
        editor.commit();

        if (mGoogleApiClient.isConnected()) {
            FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

  //Customized rectangle with rounded corners pin bitmap function
  private static Bitmap getRoundedRectBitmap(
      Bitmap bitmap,
      int strokeColor,
      int iconSize,
      int borderThickness,
      int roundedEdgeFactor) {

    if (bitmap == null) {
      return null;
    }
    // Crop original 2:1 bitmap into a square
    bitmap = ThumbnailUtils.extractThumbnail(bitmap, iconSize, iconSize);
    // Create plain bitmap
    int srcHeight = bitmap.getHeight();
    int srcWidth = bitmap.getWidth();
    Bitmap canvasBitmap = Bitmap.createBitmap(
        srcWidth,
        srcHeight,
        Bitmap.Config.ARGB_8888);

    Canvas canvas = new Canvas(canvasBitmap);
    canvas.drawARGB(0, 0, 0, 0);

    Paint paint = new Paint();
    paint.setAntiAlias(true);

    Rect rect = new Rect(0, 0, srcWidth, srcHeight);
    RectF rectF = new RectF(rect);
    canvas.drawRoundRect(rectF, roundedEdgeFactor, roundedEdgeFactor, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rect, paint);

    //Draw border
    Paint borderStroke = new Paint();
    borderStroke.setAntiAlias(true);
    borderStroke.setColor(strokeColor);
    borderStroke.setStyle(Paint.Style.STROKE);
    borderStroke.setStrokeWidth(borderThickness);
    borderStroke.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawRoundRect(rectF, roundedEdgeFactor, roundedEdgeFactor, borderStroke);

    return canvasBitmap;
  }
}
