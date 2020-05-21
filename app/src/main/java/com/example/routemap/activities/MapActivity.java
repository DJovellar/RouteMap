package com.example.routemap.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.routemap.R;
import com.example.routemap.domain.DirectionsParser;
import com.example.routemap.domain.InfoMarker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.*;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener, OnInfoWindowClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private Toolbar toolbar;
    private GoogleMap map;

    private View v1;
    private View v2;

    private List<Marker> markers;
    private List<String> documentsId;

    private SharedPreferences preferences;
    private WifiManager wifiManager;

    private Location location;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private boolean showCurrentLocation = false;
    private boolean markerRoute = false;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        createLocationRequest();

        markers = new ArrayList<>();
        documentsId = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        firebaseFirestore = FirebaseFirestore.getInstance();

    }

    public void calculateRoute(double destination_latitude, double destination_longitude) {
        String url=
                "https://maps.googleapis.com/maps/api/directions/json?origin="
                        + location.getLatitude() + "," + location.getLongitude() +"&destination="
                        + destination_latitude + "," + destination_longitude + "&sensor=false" + "&alternatives=true" + "&key=" + getString(R.string.google_api_key);

        TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
        taskRequestDirections.execute(url);
    }

    private String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            //Get the response result
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    @Override
    protected void onResume() {
        super.onResume();

        startLocationUpdates();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String networkConnectionType = preferences.getString("networkType", "Wifi");
        if(networkConnectionType.equals("Wifi")) {
            wifiManager.setWifiEnabled(true);
        }
        else {
            wifiManager.setWifiEnabled(false);
        }
        showCurrentLocation = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.filterButton:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater inflater = getLayoutInflater();

                v2 = inflater.inflate(R.layout.filter_options, null);

                builder.setView(v2).setPositiveButton("Filtrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String type = ((Spinner) v2.findViewById(R.id.filterType)).getSelectedItem().toString();
                        String level = ((Spinner) v2.findViewById(R.id.filterLevel)).getSelectedItem().toString();
                        String author = ((TextView) v2.findViewById(R.id.filterAuthor)).getText().toString();

                        filterResults(type, level, author);
                    }
                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
                return true;

            case R.id.routeButton:
                markerRoute = true;
                Toast.makeText(this, "Seleccione el destino en el mapa", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.settingsButton:
                Intent in = new Intent(this, PreferencesActivity.class);
                startActivity(in);
                return true;

            case R.id.logoutButton:
                firebaseAuth.signOut();
                Toast.makeText(this, "Sesion cerrada", Toast.LENGTH_SHORT).show();
                finish();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        //Init configuration
        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        String mapType = preferences.getString("defaultTypeMap", "Normal");
        switch (mapType) {
            case "Normal":
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case "Satelite":
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case "Terreno":
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case "Hibrido":
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
        }

        map.setInfoWindowAdapter(new CustomInfoWindowAdapter(getLayoutInflater().from(this)));

        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnInfoWindowClickListener(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onMapClick(LatLng latLng) {

        if(markerRoute) {
            calculateRoute(latLng.latitude, latLng.longitude);
            addDestinationMarker(latLng);
            markerRoute = false;
        } else {
            addPersonalizedMarker(latLng);
        }
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                location = locationResult.getLastLocation();
                if (location != null) {
                    if (!showCurrentLocation) {
                        String zoomString = preferences.getString("defaultZoomMap", "16");
                        int zoom = Integer.parseInt(zoomString);

                        goToMyCurrentLocation(zoom);
                        showCurrentLocation = true;
                    }
                    updateAvaliableInfoMarkers();
                }
            }
        };
    }

    public void updateAvaliableInfoMarkers() {

        String days_string = preferences.getString("daysMarkers", "1");
        int days = Integer.parseInt(days_string);

        Date currentDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, -days);
        currentDate = c.getTime();

        final CollectionReference collectionReference = firebaseFirestore.collection("Markers");
        collectionReference.whereGreaterThanOrEqualTo("date", currentDate).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document: task.getResult()) {
                        InfoMarker infoMarker = document.toObject(InfoMarker.class);

                        if(!documentsId.contains(document.getId())) {
                            documentsId.add(document.getId());
                            if(calculVisibilityMarker(infoMarker.getLatitude(), infoMarker.getLongitude())) {
                                showPersonalizedMarker(infoMarker);
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(MapActivity.this, "Error al obtener los marcadores", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void goToMyCurrentLocation (int zoom){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (location != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10));
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(zoom)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            else {
                Toast.makeText(this, "Localización desconocida", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Falta permiso para obtener la localización", Toast.LENGTH_SHORT).show();
        }
    }

    public void addPersonalizedMarker(final LatLng latLng) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        v1 = inflater.inflate(R.layout.input_info_marker, null);

        builder.setView(v1).setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InfoMarker infoMarker = new InfoMarker();
                infoMarker.setType(((Spinner) v1.findViewById(R.id.input_info_type)).getSelectedItem().toString());
                infoMarker.setDescription(((TextView) v1.findViewById(R.id.input_info_description)).getText().toString());
                infoMarker.setLevel(((Spinner) v1.findViewById(R.id.input_info_level)).getSelectedItem().toString());

                Date currentDate = new Date();
                infoMarker.setDate(currentDate);
                infoMarker.setAuthor(currentUser.getDisplayName());
                infoMarker.setLatitude(latLng.latitude);
                infoMarker.setLongitude(latLng.longitude);

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                switch (infoMarker.getLevel()) {
                    case "Leve":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        break;
                    case "Moderado":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                        break;
                    case "Grave":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        break;
                }

                Marker marker = map.addMarker(markerOptions);
                marker.setTag(infoMarker);
                markers.add(marker);

                firebaseFirestore.collection("Markers").add(infoMarker).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MapActivity.this, "Marcador compartido correctamente", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapActivity.this, "Error al compartir el marcador", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    public void addDestinationMarker(final LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        map.addMarker(markerOptions);
    }

    public boolean calculVisibilityMarker(double latitude, double longitude) {
        double MAX_LATITUDE = location.getLatitude() + 0.01;
        double MIN_LATITUDE = location.getLatitude() - 0.01;
        double MAX_LONGITUDE = location.getLongitude() + 0.01;
        double MIN_LONGITUDE = location.getLongitude() - 0.01;

        return (latitude <= MAX_LATITUDE)
                && (longitude <= MAX_LONGITUDE)
                && (latitude >= MIN_LATITUDE)
                && (longitude >= MIN_LONGITUDE);
    }

    public void showPersonalizedMarker(InfoMarker infoMarker) {

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(infoMarker.getLatitude(), infoMarker.getLongitude()));

        switch (infoMarker.getLevel()) {
            case "Leve":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            case "Moderado":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                break;
            case "Grave":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
        }

        Marker marker = map.addMarker(markerOptions);
        marker.setTag(infoMarker);
        markers.add(marker);
    }

    public void filterResults(String type, String level, String author) {
        if(!markers.isEmpty()) {
            for (Marker marker : markers) {
                InfoMarker infoMarker = (InfoMarker) marker.getTag();
                if ((infoMarker.getType().equals(type) || type.equals("Todos"))
                        && (infoMarker.getLevel().equals(level) || level.equals("Todos"))
                        && (infoMarker.getAuthor().equals(author) || author.isEmpty())) {
                            marker.setVisible(true);
                }
                else {
                    marker.setVisible(false);
                }
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if(marker.getTag() != null) {
            marker.showInfoWindow();
        }
        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("defaultTypeMap")) {
            String mapType = sharedPreferences.getString("defaultTypeMap", "Normal");
            switch (mapType) {
                case "Normal":
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case "Satelite":
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case "Terreno":
                    map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    break;
                case "Hibrido":
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
            }
        }

        if(key.equals("defaultZoomMap")) {
            String zoomString = sharedPreferences.getString("defaultZoomMap", "16");
            int zoom =  Integer.parseInt(zoomString);
            map.animateCamera(CameraUpdateFactory.zoomTo(zoom));
        }

        if(key.equals("networkType")) {
            String networkConnectionType = sharedPreferences.getString("networkType", "Wifi");
            if(networkConnectionType.equals("Wifi")) {
                wifiManager.setWifiEnabled(true);
            }
            else {
                wifiManager.setWifiEnabled(false);
            }
        }

        if(key.equals("changePassword")) {
            String newPasword = sharedPreferences.getString("changePassword", "0");
            currentUser.updatePassword(newPasword).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful()) {
                        Toast.makeText(MapActivity.this, "Contraseña modificada", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        FirebaseAuthException exception = (FirebaseAuthException) task.getException();
                        String errorCode = exception.getErrorCode();

                        switch (errorCode) {
                            case "ERROR_WEAK_PASSWORD":
                                Toast.makeText(MapActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                            default:
                                Toast.makeText(MapActivity.this, errorCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        if (key.equals("changeAlias")) {
            String newAlias = sharedPreferences.getString("changeAlias", "");
            if(newAlias.isEmpty()) {
                Toast.makeText(this, "El alias no puede estar vacio", Toast.LENGTH_SHORT).show();
            } else {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(newAlias).build();
                currentUser.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MapActivity.this, "Alias modificado", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(MapActivity.this, "Error al modificar el alias", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    //Custom Info Window for the markers
    public static class CustomInfoWindowAdapter implements InfoWindowAdapter {

        private LayoutInflater inflater;

        CustomInfoWindowAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");

            View v = inflater.inflate(R.layout.info_window_marker, null);

            //Details of the marker
            InfoMarker infoMarker = (InfoMarker) marker.getTag();
            ((TextView)v.findViewById(R.id.info_window_type)).setText("Tipo: " + infoMarker.getType());
            ((TextView)v.findViewById(R.id.info_window_level)).setText("Nivel:  " + infoMarker.getLevel());
            ((TextView)v.findViewById(R.id.info_window_description)).setText("Descripción: " + infoMarker.getDescription());
            ((TextView)v.findViewById(R.id.info_window_date)).setText("Fecha: " + sdf.format(infoMarker.getDate()));
            ((TextView)v.findViewById(R.id.info_window_author)).setText("Creado por: " + infoMarker.getAuthor());

            return v;
        }
    }

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //Parse JSON here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            }catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            // Get list route and display it into the map

            ArrayList points = null;
            PolylineOptions polylineOptions = null;

            for (List<HashMap<String, String>> path: lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat, lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(8);
                polylineOptions.color(Color.CYAN);
                polylineOptions.geodesic(true);

                map.addPolyline(polylineOptions);
            }
            if(polylineOptions == null) {
                Toast.makeText(MapActivity.this, "Error al calcular la ruta, intentelo de nuevo", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

