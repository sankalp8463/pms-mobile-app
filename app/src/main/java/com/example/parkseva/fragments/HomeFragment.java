package com.example.parkseva.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import com.example.parkseva.MainActivity;
import com.example.parkseva.QRCodeActivity;
import com.example.parkseva.R;
import com.example.parkseva.adapters.BookingsAdapter;
import com.example.parkseva.models.BookingResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_home, container, false);
            
            // Set time-based greeting
            TextView tvGreeting = view.findViewById(R.id.tvGreeting);
            if (tvGreeting != null) {
                tvGreeting.setText(getTimeBasedGreeting());
            }
            
            MaterialCardView quickAction1 = view.findViewById(R.id.quickAction1);
            MaterialCardView quickAction2 = view.findViewById(R.id.quickAction2);
            MaterialCardView quickAction4 = view.findViewById(R.id.quickAction4);
            Button btnNavigate = view.findViewById(R.id.btnNavigate);
            
            if (quickAction1 != null) {
                quickAction1.setOnClickListener(v -> {
                    try {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).switchToTab(1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            if (quickAction2 != null) {
                quickAction2.setOnClickListener(v -> showBookings());
            }
            
            if (quickAction4 != null) {
                quickAction4.setOnClickListener(v -> showHistory());
            }

            if (btnNavigate != null) {
                btnNavigate.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).switchToTab(2);
                    }
                });
            }
            
            return view;
        } catch (Exception e) {
            e.printStackTrace();
            return new View(requireContext());
        }
    }
    
    private void showBookings() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
        String bookingsJson = prefs.getString("bookings", "[]");
        
        try {
            org.json.JSONArray bookingsArray = new org.json.JSONArray(bookingsJson);
            
            if (bookingsArray.length() == 0) {
                Toast.makeText(requireContext(), getString(R.string.no_bookings_found), Toast.LENGTH_SHORT).show();
                return;
            }
            
            List<BookingResponse> bookings = new ArrayList<>();
            for (int i = 0; i < bookingsArray.length(); i++) {
                org.json.JSONObject obj = bookingsArray.getJSONObject(i);
                BookingResponse booking = new BookingResponse();
                booking.setId(obj.optString("id", ""));
                booking.setVehicleNumber(obj.optString("vehicleNumber", ""));
                booking.setSlotNumber(obj.optString("slotNumber", ""));
                bookings.add(booking);
            }
            
            showBookingsDialog(bookings);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
        String historyJson = prefs.getString("history", "[]");
        
        try {
            org.json.JSONArray historyArray = new org.json.JSONArray(historyJson);
            
            if (historyArray.length() == 0) {
                Toast.makeText(requireContext(), getString(R.string.no_history_found), Toast.LENGTH_SHORT).show();
                return;
            }
            
            List<BookingResponse> history = new ArrayList<>();
            for (int i = 0; i < historyArray.length(); i++) {
                org.json.JSONObject obj = historyArray.getJSONObject(i);
                BookingResponse booking = new BookingResponse();
                booking.setId(obj.optString("id", ""));
                booking.setVehicleNumber(obj.optString("vehicleNumber", ""));
                booking.setSlotNumber(obj.optString("slotNumber", ""));
                history.add(booking);
            }
            
            showHistoryDialog(history);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showBookingsDialog(List<BookingResponse> bookings) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_bookings);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerBookings);
        Button btnClose = dialog.findViewById(R.id.btnClose);
        
        BookingsAdapter adapter = new BookingsAdapter(bookings, new BookingsAdapter.OnBookingClickListener() {
            @Override
            public void onBookingClick(BookingResponse booking) {
                Intent intent = new Intent(requireContext(), QRCodeActivity.class);
                intent.putExtra("vehicleNumber", booking.getVehicleNumber());
                intent.putExtra("slotNumber", booking.getSlotNumber());
                intent.putExtra("bookingId", booking.getId());
                intent.putExtra("amount", 0.0);
                startActivity(intent);
                dialog.dismiss();
            }

            @Override
            public void onDeleteClick(int position) {
                BookingResponse booking = bookings.get(position);
                bookings.remove(position);
                saveBookings(bookings);
                saveToHistory(booking);
                recyclerView.getAdapter().notifyItemRemoved(position);
                Toast.makeText(requireContext(), getString(R.string.booking_moved_to_history), Toast.LENGTH_SHORT).show();
                if (bookings.isEmpty()) {
                    dialog.dismiss();
                }
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void showHistoryDialog(List<BookingResponse> history) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_bookings);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerBookings);
        Button btnClose = dialog.findViewById(R.id.btnClose);
        
        BookingsAdapter adapter = new BookingsAdapter(history, new BookingsAdapter.OnBookingClickListener() {
            @Override
            public void onBookingClick(BookingResponse booking) {
                Toast.makeText(requireContext(), "History item: " + booking.getVehicleNumber(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(int position) {
                Toast.makeText(requireContext(), getString(R.string.cannot_delete_history), Toast.LENGTH_SHORT).show();
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void saveBookings(List<BookingResponse> bookings) {
        try {
            org.json.JSONArray array = new org.json.JSONArray();
            for (BookingResponse booking : bookings) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", booking.getId());
                obj.put("vehicleNumber", booking.getVehicleNumber());
                obj.put("slotNumber", booking.getSlotNumber());
                array.put(obj);
            }
            SharedPreferences prefs = requireContext().getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("bookings", array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveToHistory(BookingResponse booking) {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
            String historyJson = prefs.getString("history", "[]");
            org.json.JSONArray historyArray = new org.json.JSONArray(historyJson);
            
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("id", booking.getId());
            obj.put("vehicleNumber", booking.getVehicleNumber());
            obj.put("slotNumber", booking.getSlotNumber());
            obj.put("timestamp", System.currentTimeMillis());
            
            historyArray.put(obj);
            prefs.edit().putString("history", historyArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getTimeBasedGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return getString(R.string.good_morning);
        } else if (hour >= 12 && hour < 17) {
            return getString(R.string.good_afternoon);
        } else if (hour >= 17 && hour < 21) {
            return getString(R.string.good_evening);
        } else {
            return getString(R.string.good_night);
        }
    }
}
