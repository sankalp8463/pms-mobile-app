package com.example.parkseva.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.parkseva.R;
import com.example.parkseva.api.ApiClient;
import com.example.parkseva.models.Location;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final int REQ_LOCATION = 1001;
    private static final GeoPoint DEFAULT_CENTER = new GeoPoint(19.0760, 72.8777);

    private MapView mapView;
    private TextView tvMapStatus;
    private Button btnMyLocation;
    private Button btnRefresh;
    private MyLocationNewOverlay myLocationOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        tvMapStatus = view.findViewById(R.id.tvMapStatus);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        btnRefresh = view.findViewById(R.id.btnRefreshLocations);

        setupMap();
        setupActions();
        ensureLocationOverlay();
        loadLocations();
        return view;
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(DEFAULT_CENTER);
    }

    private void setupActions() {
        btnMyLocation.setOnClickListener(v -> centerToMyLocation());
        btnRefresh.setOnClickListener(v -> loadLocations());
    }

    private void ensureLocationOverlay() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initLocationOverlay();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
    }

    private void initLocationOverlay() {
        if (myLocationOverlay != null) {
            return;
        }
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
        mapView.invalidate();
    }

    private void centerToMyLocation() {
        if (myLocationOverlay == null) {
            Toast.makeText(requireContext(), getString(R.string.location_not_ready), Toast.LENGTH_SHORT).show();
            ensureLocationOverlay();
            return;
        }

        GeoPoint current = myLocationOverlay.getMyLocation();
        if (current == null) {
            Toast.makeText(requireContext(), getString(R.string.location_not_ready), Toast.LENGTH_SHORT).show();
            return;
        }

        mapView.getController().animateTo(current);
        mapView.getController().setZoom(15.0);
    }

    private void loadLocations() {
        setStatus(getString(R.string.loading_parking_locations), false);
        clearMarkersOnly();

        ApiClient.getParkingApi().getAllLocations().enqueue(new Callback<List<Location>>() {
            @Override
            public void onResponse(Call<List<Location>> call, Response<List<Location>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setStatus(getString(R.string.failed_load_locations), true);
                    return;
                }

                List<Location> locations = response.body();
                if (locations.isEmpty()) {
                    setStatus(getString(R.string.no_locations_found), true);
                    return;
                }

                double minLat = Double.MAX_VALUE;
                double maxLat = -Double.MAX_VALUE;
                double minLon = Double.MAX_VALUE;
                double maxLon = -Double.MAX_VALUE;

                int markerCount = 0;
                for (Location location : locations) {
                    if (location.getCoordinates() == null) {
                        continue;
                    }

                    double lat = location.getCoordinates().getLatitude();
                    double lon = location.getCoordinates().getLongitude();

                    minLat = Math.min(minLat, lat);
                    maxLat = Math.max(maxLat, lat);
                    minLon = Math.min(minLon, lon);
                    maxLon = Math.max(maxLon, lon);

                    addParkingMarker(location);
                    markerCount++;
                }

                if (markerCount == 0) {
                    setStatus(getString(R.string.no_locations_found), true);
                    return;
                }

                double centerLat = (minLat + maxLat) / 2;
                double centerLon = (minLon + maxLon) / 2;
                mapView.getController().setCenter(new GeoPoint(centerLat, centerLon));
                mapView.getController().setZoom(13.5);
                mapView.invalidate();

                String status = String.format(Locale.US, "%d parking locations loaded", markerCount);
                setStatus(status, false);
            }

            @Override
            public void onFailure(Call<List<Location>> call, Throwable t) {
                Log.e(TAG, "Error fetching locations", t);
                setStatus(getString(R.string.network_error_locations), true);
            }
        });
    }

    private void clearMarkersOnly() {
        for (int i = mapView.getOverlays().size() - 1; i >= 0; i--) {
            Object overlay = mapView.getOverlays().get(i);
            if (overlay instanceof Marker) {
                mapView.getOverlays().remove(i);
            }
        }
    }

    private void addParkingMarker(Location location) {
        GeoPoint point = new GeoPoint(location.getCoordinates().getLatitude(), location.getCoordinates().getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("P - " + location.getLocationName());
        marker.setSnippet(buildMarkerSnippet(location));
        marker.setTextIcon("P");
        marker.setOnMarkerClickListener((clickedMarker, ignoredMapView) -> {
            showLocationActions(location);
            return true;
        });
        mapView.getOverlays().add(marker);
    }

    private String buildMarkerSnippet(Location location) {
        String address = location.getAddress() != null ? location.getAddress() : "Address unavailable";
        return address + "\nSlots: " + location.getTotalSlots();
    }

    private void showLocationActions(Location location) {
        if (location.getCoordinates() == null) {
            return;
        }

        String name = location.getLocationName() != null ? location.getLocationName() : "Parking Location";
        String address = location.getAddress() != null ? location.getAddress() : "Address unavailable";
        String message = address + "\nTotal Slots: " + location.getTotalSlots() + "\n" + getDistanceLine(location);

        new AlertDialog.Builder(requireContext())
                .setTitle(name)
                .setMessage(message)
                .setPositiveButton(R.string.navigate, (dialog, which) ->
                        openNavigation(location.getCoordinates().getLatitude(), location.getCoordinates().getLongitude(), name))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private String getDistanceLine(Location location) {
        if (myLocationOverlay == null || myLocationOverlay.getMyLocation() == null || location.getCoordinates() == null) {
            return "Distance: unknown";
        }

        GeoPoint me = myLocationOverlay.getMyLocation();
        GeoPoint target = new GeoPoint(location.getCoordinates().getLatitude(), location.getCoordinates().getLongitude());
        double km = me.distanceToAsDouble(target) / 1000.0;
        return String.format(Locale.US, "Distance: %.2f km", km);
    }

    private void openNavigation(double lat, double lon, String label) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
        Intent googleMapsIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        googleMapsIntent.setPackage("com.google.android.apps.maps");

        if (googleMapsIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(googleMapsIntent);
            return;
        }

        Uri geoUri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + Uri.encode(label) + ")");
        Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        if (geoIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(geoIntent);
            return;
        }

        Toast.makeText(requireContext(), getString(R.string.no_navigation_app), Toast.LENGTH_SHORT).show();
    }

    private void setStatus(String text, boolean isError) {
        tvMapStatus.setText(text);
        tvMapStatus.setTextColor(isError ? 0xFFB91C1C : 0xFF166534);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLocationOverlay();
            centerToMyLocation();
        } else {
            Toast.makeText(requireContext(), getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
