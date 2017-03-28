package com.kimeeo.kAndroid.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RawRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kimeeo.kAndroid.listViews.BaseListDataView;
import com.kimeeo.kAndroid.listViews.dataProvider.DataProvider;
import com.kimeeo.kAndroid.listViews.dataProvider.MonitorList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bhavinpadhiyar on 2/3/16.
 */
abstract public class BaseMapView extends BaseListDataView implements MonitorList.OnChangeWatcher {
    public static int VIEW_TYPE = GoogleMap.MAP_TYPE_NORMAL;
    protected SupportMapFragment mapFragment;
    protected GoogleMap googleMap;
    protected HashMap<String, Object> markerInfo = new HashMap<String, Object>();
    GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(final Marker marker) {
            return onMarkerTouch(markerInfo.get(marker.getId()));
        }
    };
    GoogleMap.OnInfoWindowClickListener onInfoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            Object data = markerInfo.get(marker.getId());
            onInfoWindowTouch(data);
        }
    };
    GoogleMap.OnMarkerDragListener onMarkerDragListener = new GoogleMap.OnMarkerDragListener() {
        @Override
        public void onMarkerDragStart(Marker marker) {

        }

        @Override
        public void onMarkerDragEnd(Marker marker) {

        }

        @Override
        public void onMarkerDrag(Marker marker) {

        }
    };
    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void onGlobalLayout() {
            try {
                LatLngBounds.Builder bc = new LatLngBounds.Builder();
                List<Object> dataList = getDataProvider();
                IPOI poi;
                for (Object item : dataList) {
                    if (item instanceof IPOI) {
                        poi = (IPOI) item;
                        bc.include(poi.getMarker().getPosition());
                    } else {
                        poi = getPOIForObject(item);
                        if (poi != null)
                            bc.include(poi.getMarker().getPosition());
                    }
                }
                if (dataList.size() > 0) {
                    LatLngBounds bounds = bc.build();
                    clearOnGlobalLayoutListener();
                    if (googleMap != null)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, getBoundZoom()));
                }
            } catch (Exception e) {
            }
        }


    };

    protected int getBoundZoom() {
        return 80;
    }

    private Location myLocation;
    GoogleMap.OnMyLocationChangeListener onMyLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            if (googleMap != null) {
                myLocation = googleMap.getMyLocation();
                myLocationChange(myLocation);
                List<AddressVO> list = MapUtils.getAddress(getActivity(), myLocation, 2);
                System.out.println(list);
            }
        }
    };
    private View mProgressBar;
    private List<?> updatePending;
    private boolean firstItemIn = false;

    public HashMap<String, Object> getMarkers() {
        return markerInfo;
    }

    public List<Marker> getMarkersList() {

        List<Marker> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : markerInfo.entrySet()) {
            if (entry.getValue() instanceof IPOI)
                list.add(((IPOI) entry.getValue()).getMarker());
        }
        return list;
    }

    public void updateLatLng(Marker marker, LatLng latLng) {
        marker.setPosition(latLng);
        marker.notifyAll();
    }

    public void updateLatLng(Marker marker, Double latitude, Double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        marker.setPosition(latLng);
        marker.notifyAll();
    }

    protected void configDataManager(DataProvider dataProvider) {
        if (dataProvider != null) {
            dataProvider.addFatchingObserve(this);
            dataProvider.addDataChangeWatcher(this);
            if (dataProvider.size() != 0)
                itemsAdded(0, dataProvider);
        }
    }

    protected void garbageCollectorCall() {
        super.garbageCollectorCall();
        if (googleMap != null) {
            googleMap.setOnMarkerClickListener(null);
            googleMap.setOnInfoWindowClickListener(null);
            googleMap.setOnMyLocationChangeListener(null);
            googleMap.setOnMarkerDragListener(null);

        }
        clearOnGlobalLayoutListener();
        mapFragment = null;
        googleMap = null;
        myLocation = null;
        updatePending = null;
        markerInfo = null;
    }

    public void drawLine(IPOI src, IPOI dest, int color) {
        LatLng latLng1 = new LatLng(src.getLatitude(), src.getLongitude());
        LatLng latLng2 = new LatLng(dest.getLatitude(), dest.getLongitude());
        getGoogleMap().addPolyline((new PolylineOptions()).add(latLng1).add(latLng2).width(2).color(color).geodesic(true));
    }

    public void drawLine(LatLng src, LatLng dest, int color) {
        getGoogleMap().addPolyline((new PolylineOptions()).add(src).add(dest).width(2).color(color).geodesic(true));
    }

    public void drawLine(Location src, Location dest, int color) {
        LatLng latLng1 = new LatLng(src.getLatitude(), src.getLongitude());
        LatLng latLng2 = new LatLng(dest.getLatitude(), dest.getLongitude());
        getGoogleMap().addPolyline((new PolylineOptions()).add(latLng1).add(latLng2).width(2).color(color).geodesic(true));
    }

    public void drawLine(IPOI src, Location dest, int color) {
        LatLng latLng1 = new LatLng(src.getLatitude(), src.getLongitude());
        LatLng latLng2 = new LatLng(dest.getLatitude(), dest.getLongitude());
        getGoogleMap().addPolyline((new PolylineOptions()).add(latLng1).add(latLng2).width(2).color(color).geodesic(true));
    }

    public void drawLine(Location src, IPOI dest, int color) {
        LatLng latLng1 = new LatLng(src.getLatitude(), src.getLongitude());
        LatLng latLng2 = new LatLng(dest.getLatitude(), dest.getLongitude());
        getGoogleMap().addPolyline((new PolylineOptions()).add(latLng1).add(latLng2).width(2).color(color).geodesic(true));
    }

    public GoogleMap getGoogleMap() {
        return googleMap;
    }

    public boolean showMenu() {
        return true;
    }

    protected View createRootView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout._fregment_map_fragment, container, false);
        return rootView;
    }

    protected View createRootMapNotSupportedView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, boolean debug) {
        View rootView;
        if (debug)
            rootView = inflater.inflate(R.layout._fregment_map_fragment_not_support_debug, container, false);
        else
            rootView = inflater.inflate(R.layout._fregment_map_fragment_not_support, container, false);
        return rootView;
    }

    protected SupportMapFragment getSupportMapFragment(View rootView, FragmentManager fragmentManager) {
        return (SupportMapFragment) fragmentManager.findFragmentById(R.id.mapFragment);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    final public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        try {
            configViewParam();
            final View rootView = createRootView(inflater, container, savedInstanceState);
            FragmentManager fragmentManager = getChildFragmentManager();
            mapFragment = getSupportMapFragment(rootView, fragmentManager);
            mapFragment.getMapAsync(new OnMapReadyCallback()
            {
                @Override
                public void onMapReady(GoogleMap map) {
                    googleMap =map;

                    if(getDefaultMapStyle()!=-1)
                        setMapStyle(getDefaultMapStyle());

                    MapsInitializer.initialize(getActivity());
                    setHasOptionsMenu(showMenu());
                    viewCreated(rootView);
                    if (rootView.findViewById(R.id.progressBar) != null)
                        mProgressBar = rootView.findViewById(R.id.progressBar);

                    configMapView(googleMap, mapFragment, getDataProvider());
                    next();
                }
            });


            return rootView;
        } catch (Exception e) {
            View rootView = createRootMapNotSupportedView(inflater, container, savedInstanceState, BuildConfig.DEBUG);
            viewCreated(rootView);
            return rootView;
        }
    }


    private int mapStyle=-1;
    @RawRes
    public int getMapStyle() {
        return mapStyle;
    }

    @RawRes
    protected int getDefaultMapStyle() {
        return -1;
    }

    public void setMapStyle(@RawRes int value) {

        mapStyle=value;
        if(googleMap!=null)
        {
            if(getMapStyle()!=-1)
            {
                try {
                    boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), getMapStyle()));
                }catch (Resources.NotFoundException e){}
            }
        }
    }

    @Override
    protected String[] requirePermissions() {
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, "com.google.android.providers.gsf.permission.READ_GSERVICES"};
    }

    @Override
    public String[] getFriendlyPermissionsMeaning() {
        return new String[]{"Location"};
    }

    protected void configMapView(GoogleMap googleMap, SupportMapFragment mapFragment, DataProvider dataProvider) {

    }

    protected void viewCreated(View rootView) {

    }

    public void navigateTo(IPOI poi) {
        String locationURL = "http://maps.google.com/maps?saddr=" + myLocation.getLatitude() + "," + myLocation.getLongitude() + "&daddr=" + poi.getLatitude() + "," + poi.getLongitude() + "";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(locationURL));
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map_menu_options, menu);
        setMenuIcons(menu, inflater);


        MenuItem item = null;
        if (VIEW_TYPE == GoogleMap.MAP_TYPE_NORMAL)
            item = menu.findItem(R.id.normal);
        else if (VIEW_TYPE == GoogleMap.MAP_TYPE_SATELLITE)
            item = menu.findItem(R.id.satellite);
        else if (VIEW_TYPE == GoogleMap.MAP_TYPE_TERRAIN)
            item = menu.findItem(R.id.terrain);
        else if (VIEW_TYPE == GoogleMap.MAP_TYPE_HYBRID)
            item = menu.findItem(R.id.hybrid);
        if (item != null)
            item.setChecked(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(true);

        if (item.getItemId() == R.id.normal) {
            VIEW_TYPE = GoogleMap.MAP_TYPE_NORMAL;
            updateMapView(VIEW_TYPE);
            return true;
        } else if (item.getItemId() == R.id.satellite) {
            VIEW_TYPE = GoogleMap.MAP_TYPE_SATELLITE;
            updateMapView(VIEW_TYPE);
            return true;
        } else if (item.getItemId() == R.id.terrain) {
            VIEW_TYPE = GoogleMap.MAP_TYPE_TERRAIN;
            updateMapView(VIEW_TYPE);

            return true;
        } else if (item.getItemId() == R.id.hybrid) {
            VIEW_TYPE = GoogleMap.MAP_TYPE_HYBRID;
            updateMapView(VIEW_TYPE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setMenuIcons(Menu menu, MenuInflater inflater) {

    }

    public void updateMapView(int type) {
        if (googleMap != null)
            googleMap.setMapType(type);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (googleMap != null)
            setUpMap(googleMap);
    }
    @Override
    protected void permissionGranted() {
        try {
            if (googleMap!=null && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                googleMap.setMyLocationEnabled(true);
        }catch (Exception e){}
    }



    protected void setUpMap(GoogleMap googleMap) {
        try {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                googleMap.setMyLocationEnabled(true);
        }catch (Exception e){}


        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setAllGesturesEnabled(true);

        googleMap.setOnMarkerClickListener(onMarkerClickListener);
        googleMap.setOnInfoWindowClickListener(onInfoWindowClickListener);
        googleMap.setOnMyLocationChangeListener(onMyLocationChangeListener);
        googleMap.setOnMarkerDragListener(onMarkerDragListener);

        updateMapView(VIEW_TYPE);

        if (updatePending != null)
        {
            itemsAdded(0, updatePending);
            updatePending = null;
        }

    }

    public void myLocationChange(Location location) {

    }

    public Distance getDistanceFromMyLocation(Location loc){
        return MapUtils.getDistance(myLocation,loc);
    }

    public Distance getDistanceFromMyLocation(IPOI loc){
        return MapUtils.getDistance(myLocation,loc);
    }

    public void moveCameraToLocation( IPOI newPOI) {
        if(newPOI!=null && googleMap!=null) {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(newPOI.getLatitude(), newPOI.getLongitude())).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void moveCameraToLocation( Location location) {
        if(location !=null && googleMap!=null) {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void moveCameraToMyLocation() {
        Location location = myLocation;
        if(location !=null && googleMap!=null) {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void onInfoWindowTouch(Object data) {

    }

    public boolean onMarkerTouch(Object data)
    {
        return false;
    }

    public void fitMapToPins() {
        try
        {
            if (mapFragment.getView().getViewTreeObserver().isAlive()) {
                mapFragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
            }
        }catch(Exception e){}

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    protected boolean removePOIMarker(IPOI poi) {
        try {
            if(googleMap!=null) {
                poi.getMarker().remove();
                return true;
            }
        }catch (Exception e){}
        return false;
    }

    protected void addPOIMarker(IPOI poi) {
        try
        {
            if(googleMap!=null) {
                MarkerOptions markerOptions = getMarkerOptions(poi);
                if (markerOptions != null) {
                    Bitmap iconBitmap = getPOIIcon(poi);
                    BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(iconBitmap);
                    markerOptions.icon(bitmapDescriptor);
                    markerOptions.snippet(poi.getSnippet());
                    markerOptions.title(poi.getTitle());
                    Marker marker = googleMap.addMarker(markerOptions);
                    marker.setSnippet(poi.getSnippet());
                    poi.setMarker(marker);

                    markerInfo.put(marker.getId(), poi);
                }
            }
        }catch(Exception e)
        {
            System.out.println(e);
        }


    }

    protected Bitmap getPOIIcon(IPOI poi) {

        Drawable pin = getActivity().getResources().getDrawable(R.drawable._vector_icon_map_pin);
        /*
        com.kimeeo.library.listDataView.mapView.TextDrawable d= new com.kimeeo.library.listDataView.mapView.TextDrawable(getActivity());
        d.setText(poi.getTitle().substring(0,1));
        d.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        d.setTextSize(12);

        LayerDrawable layerDrawable= new LayerDrawable(new Drawable[]{d,pin});
        */
        Bitmap icon = drawableToBitmap(pin);
        return icon;
    }

    public Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    protected IPOI getPOIForObject(Object data)
    {
        return null;
    }

    protected MarkerOptions getMarkerOptions(IPOI poi) {
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(poi.getLatitude(), poi.getLongitude())).title(poi.getTitle()).snippet(poi.getSnippet());
        return markerOptions;
    }
    public void clearOnGlobalLayoutListener() {
        if(mapFragment!=null && mapFragment.getView()!=null)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                mapFragment.getView().getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
            else
                mapFragment.getView().getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
    }




    public void onFetchingStart(boolean var1)
    {
        if(mProgressBar!=null && firstItemIn==false)
            mProgressBar.setVisibility(View.VISIBLE);
    }

    public void onFetchingFinish(boolean var1)
    {

    }

    public void onFetchingEnd(List<?> var1, boolean var2)
    {

    }

    public void onFetchingError(Object var1)
    {

    }



    public void itemsChanged(int var1, List list)
    {

    }


    public void itemsAdded(int pos,List dataList) {
        if(googleMap==null)
        {
            updatePending=dataList;
        }
        else
        {
            for (Object item : dataList)
            {
                if(item instanceof IPOI)
                    addPOIMarker((IPOI)item);
                else
                {
                    IPOI poi =getPOIForObject(item);
                    if(poi!=null)
                        addPOIMarker(poi);
                }
            }
            fitMapToPins();
        }

        if(mProgressBar!=null)
            mProgressBar.setVisibility(View.GONE);

        firstItemIn = true;
    }
    public void removedItem(int index,Object item) {

    }
    public void itemsRemoved(int pos,List dataList) {
        if(googleMap!=null) {
            for (Object item : dataList)
            {
                if(item instanceof IPOI)
                    removePOIMarker((IPOI) item);
                else
                    removedItem(pos,item);
            }
            fitMapToPins();
        }
        if(mProgressBar!=null)
            mProgressBar.setVisibility(View.GONE);
    }
}
