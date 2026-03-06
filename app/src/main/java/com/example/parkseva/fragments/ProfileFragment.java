package com.example.parkseva.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import com.example.parkseva.LoginActivity;
import com.example.parkseva.R;
import com.example.parkseva.api.ApiClient;
import com.example.parkseva.models.User;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvUserPhone, tvUserEmail, tvUserRole;
    private Button btnLogout, btnSettings;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        initViews(view);
        setupClickListeners();
        loadUserProfile();
        
        return view;
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSettings = view.findViewById(R.id.btnSettings);
        
        sharedPreferences = requireContext().getSharedPreferences("ParkSevaPrefs", MODE_PRIVATE);
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> logout());
        btnSettings.setOnClickListener(v -> showLanguageDialog());
    }

    private void loadUserProfile() {
        String userId = sharedPreferences.getString("user_id", null);
        if (userId == null) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getParkingApi().getUserProfile(userId)
            .enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        updateUI(user);
                    } else {
                        Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                        loadFromCache();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                    loadFromCache();
                }
            });
    }

    private void updateUI(User user) {
        tvUserName.setText(user.getName());
        tvUserPhone.setText(user.getPhoneNumber());
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "Not provided");
        tvUserRole.setText(user.getRole().toUpperCase());
    }

    private void loadFromCache() {
        // Load from SharedPreferences as fallback
        String userName = sharedPreferences.getString("user_name", "Unknown");
        tvUserName.setText(userName);
        tvUserPhone.setText("Loading...");
        tvUserEmail.setText("Loading...");
        tvUserRole.setText("Loading...");
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    
    private void showLanguageDialog() {
        String[] languages = {getString(R.string.english), getString(R.string.marathi), getString(R.string.hindi)};
        String[] languageCodes = {"en", "mr", "hi"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.select_language));
        builder.setItems(languages, (dialog, which) -> {
            setLanguage(languageCodes[which]);
        });
        builder.show();
    }
    
    private void setLanguage(String languageCode) {
        sharedPreferences.edit().putString("language", languageCode).apply();
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());
        
        requireActivity().recreate();
    }
}