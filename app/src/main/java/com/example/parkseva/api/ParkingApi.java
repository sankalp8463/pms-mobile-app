package com.example.parkseva.api;

import com.example.parkseva.models.BookingRequest;
import com.example.parkseva.models.BookingResponse;
import com.example.parkseva.models.LoginRequest;
import com.example.parkseva.models.LoginResponse;
import com.example.parkseva.models.ParkingEntryStatusResponse;
import com.example.parkseva.models.ParkingSlot;
import com.example.parkseva.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ParkingApi {
    
    // User authentication
    @POST("api/users/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    // User profile
    @GET("api/users/{id}")
    Call<User> getUserProfile(@Path("id") String userId);
    
    // Parking slots
    @GET("api/parking-slots")
    Call<List<ParkingSlot>> getAllParkingSlots();
    
    @GET("api/parking-slots/status/available")
    Call<List<ParkingSlot>> getAvailableSlots();
    
    // Vehicle registration
    @POST("api/vehicles")
    Call<Void> registerVehicle(@Body BookingRequest request);
    
    // Parking entries
    @POST("api/admin-parking-entries/park")
    Call<BookingResponse> bookSlot(@Body BookingRequest request);

    @GET("api/parking-entries")
    Call<List<ParkingEntryStatusResponse>> getParkingEntries();
    
    // Get user bookings
    @GET("api/parking-entries/user/{userId}")
    Call<List<BookingResponse>> getUserBookings(@Path("userId") String userId);
    
    // Get all locations
    @GET("api/admin-locations/all")
    Call<List<com.example.parkseva.models.Location>> getAllLocations();
}
