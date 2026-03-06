package com.example.parkseva.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.parkseva.QRCodeActivity;
import com.example.parkseva.R;
import com.example.parkseva.api.ApiClient;
import com.example.parkseva.models.BookingRequest;
import com.example.parkseva.models.BookingResponse;
import com.example.parkseva.models.ParkingEntryStatusResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParkFragment extends Fragment {

    private static final String TAG = "ParkFragment";
    private static final String OFFLINE_SMS_NUMBER = "7498301509";
    private static final int REQUEST_SEND_SMS = 1201;
    private EditText etVehicleNumber;
    private Spinner spinnerVehicleType;
    private Button btnParkNow;
    private Button btnCheckEntryStatus;
    private TextView tvEntryStatus;
    private String pendingOfflineSmsBody;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_park, container, false);
        
        etVehicleNumber = view.findViewById(R.id.etVehicleNumber);
        spinnerVehicleType = view.findViewById(R.id.spinnerVehicleType);
        btnParkNow = view.findViewById(R.id.btnParkNow);
        btnCheckEntryStatus = view.findViewById(R.id.btnCheckEntryStatus);
        tvEntryStatus = view.findViewById(R.id.tvEntryStatus);
        
        String[] vehicleTypes = {"car", "bike", "truck"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, vehicleTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);
        
        btnCheckEntryStatus.setOnClickListener(v -> checkVehicleEntryStatus());
        btnParkNow.setOnClickListener(v -> parkVehicle());
        
        return view;
    }

    private void parkVehicle() {
        String vehicleNumber = normalizeVehicleNumber(etVehicleNumber.getText().toString());
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();
        
        if (vehicleNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Enter vehicle number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isInternetConnected()) {
            sendOfflineParkingSms(vehicleNumber);
            showEntryStatus("No internet. Parking data sent via SMS.", true);
            return;
        }
        
        SharedPreferences prefs = requireContext().getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);
        checkActiveEntryForVehicle(vehicleNumber, new ActiveEntryCheckCallback() {
            @Override
            public void onResult(boolean exists, ParkingEntryStatusResponse entry) {
                if (exists && entry != null) {
                    showEntryStatus("Vehicle already parked in slot " + entry.getResolvedSlotNumber()
                            + " since " + formatEntryTime(entry.getEntryTime()), false);
                    setLoadingState(false);
                    return;
                }

                showEntryStatus("No active entry found. You can park now.", true);
                BookingRequest request = new BookingRequest(vehicleNumber, vehicleType, userId, 1);

                ApiClient.getParkingApi().registerVehicle(request).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d(TAG, "Vehicle registration: " + response.code());
                        bookSlot(request);
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.d(TAG, "Vehicle already registered, proceeding to book");
                        bookSlot(request);
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoadingState(false);
                showEntryStatus(message, false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void bookSlot(BookingRequest request) {
        Log.d(TAG, "Booking: " + request.getVehicleNumber() + ", " + request.getVehicleType());
        
        ApiClient.getParkingApi().bookSlot(request).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                Log.d(TAG, "Booking response: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    BookingResponse booking = response.body();
                    Log.d(TAG, "Booking success: " + booking.getId());
                    setLoadingState(false);
                    
                    saveBookingLocally(booking);
                    showEntryStatus("Parking booked successfully.", true);
                    
                    Intent intent = new Intent(requireContext(), QRCodeActivity.class);
                    intent.putExtra("vehicleNumber", booking.getVehicleNumber());
                    intent.putExtra("slotNumber", booking.getSlotNumber());
                    intent.putExtra("bookingId", booking.getId());
                    intent.putExtra("amount", 0.0);
                    startActivity(intent);
                    etVehicleNumber.setText("");
                } else {
                    setLoadingState(false);
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Booking failed: " + response.code() + " - " + errorBody);
                        Toast.makeText(requireContext(), "Parking failed: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                        Toast.makeText(requireContext(), "Parking failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {
                setLoadingState(false);
                Log.e(TAG, "Network error booking", t);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkVehicleEntryStatus() {
        String vehicleNumber = normalizeVehicleNumber(etVehicleNumber.getText().toString());
        if (vehicleNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Enter vehicle number", Toast.LENGTH_SHORT).show();
            return;
        }

        showEntryStatus("Checking database for active entry...", true);
        setLoadingState(true);
        checkActiveEntryForVehicle(vehicleNumber, new ActiveEntryCheckCallback() {
            @Override
            public void onResult(boolean exists, ParkingEntryStatusResponse entry) {
                setLoadingState(false);
                if (exists && entry != null) {
                    showEntryStatus("Active entry exists in slot " + entry.getResolvedSlotNumber()
                            + " since " + formatEntryTime(entry.getEntryTime()), false);
                } else {
                    showEntryStatus("No active entry found for this vehicle.", true);
                }
            }

            @Override
            public void onError(String message) {
                setLoadingState(false);
                showEntryStatus(message, false);
            }
        });
    }

    private void checkActiveEntryForVehicle(String vehicleNumber, ActiveEntryCheckCallback callback) {
        final String normalizedInput = normalizeVehicleNumber(vehicleNumber);
        ApiClient.getParkingApi().getParkingEntries().enqueue(new Callback<List<ParkingEntryStatusResponse>>() {
            @Override
            public void onResponse(Call<List<ParkingEntryStatusResponse>> call, Response<List<ParkingEntryStatusResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Could not verify parking entry right now.");
                    return;
                }

                for (ParkingEntryStatusResponse entry : response.body()) {
                    String status = entry.getStatus() != null ? entry.getStatus() : "";
                    String number = normalizeVehicleNumber(entry.getResolvedVehicleNumber());
                    if ("active".equalsIgnoreCase(status) && normalizedInput.equals(number)) {
                        callback.onResult(true, entry);
                        return;
                    }
                }

                callback.onResult(false, null);
            }

            @Override
            public void onFailure(Call<List<ParkingEntryStatusResponse>> call, Throwable t) {
                Log.e(TAG, "Entry status check failed", t);
                callback.onError("Network error while checking entry.");
            }
        });
    }

    private void setLoadingState(boolean loading) {
        btnParkNow.setEnabled(!loading);
        btnCheckEntryStatus.setEnabled(!loading);
    }

    private void showEntryStatus(String message, boolean positive) {
        tvEntryStatus.setVisibility(View.VISIBLE);
        tvEntryStatus.setText(message);
        tvEntryStatus.setTextColor(positive ? 0xFF166534 : 0xFFB91C1C);
    }

    private String normalizeVehicleNumber(String value) {
        if (value == null) return "";
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
    }

    private String formatEntryTime(String isoTime) {
        if (isoTime == null || isoTime.trim().isEmpty()) return "unknown time";
        try {
            SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            source.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = source.parse(isoTime);
            if (parsed == null) return isoTime;
            return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(parsed);
        } catch (Exception e) {
            return isoTime;
        }
    }

    private interface ActiveEntryCheckCallback {
        void onResult(boolean exists, ParkingEntryStatusResponse entry);
        void onError(String message);
    }

    @SuppressLint("MissingPermission")
    private void sendOfflineParkingSms(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Invalid vehicle number for SMS", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingOfflineSmsBody = vehicleNumber;
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(OFFLINE_SMS_NUMBER, null, vehicleNumber, null, null);
            Toast.makeText(requireContext(), "SMS sent to " + OFFLINE_SMS_NUMBER, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send offline SMS", e);
            Toast.makeText(requireContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_SEND_SMS) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingOfflineSmsBody != null) {
            String smsBody = pendingOfflineSmsBody;
            pendingOfflineSmsBody = null;
            sendOfflineParkingSms(smsBody);
            return;
        }

        pendingOfflineSmsBody = null;
        Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show();
    }
    
    private void saveBookingLocally(BookingResponse booking) {
        SharedPreferences prefs = requireContext().getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
        String bookingsJson = prefs.getString("bookings", "[]");
        
        try {
            org.json.JSONArray bookings = new org.json.JSONArray(bookingsJson);
            org.json.JSONObject bookingObj = new org.json.JSONObject();
            bookingObj.put("id", booking.getId());
            bookingObj.put("vehicleNumber", booking.getVehicleNumber());
            bookingObj.put("slotNumber", booking.getSlotNumber());
            bookings.put(bookingObj);
            
            prefs.edit().putString("bookings", bookings.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving booking", e);
        }
    }
}
