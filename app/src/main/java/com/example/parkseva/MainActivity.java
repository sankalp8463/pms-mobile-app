package com.example.parkseva;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.parkseva.api.ApiClient;
import com.example.parkseva.fragments.HomeFragment;
import com.example.parkseva.fragments.ParkFragment;
import com.example.parkseva.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply saved language
        applySavedLanguage();
        
        setContentView(R.layout.activity_main_new);
        
        ApiClient.init(this);
        checkLoginStatus();
        
        // Handle system window insets for notch
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            if (item.getItemId() == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_park) {
                fragment = new ParkFragment();
            } else if (item.getItemId() == R.id.nav_map) {
                fragment = new com.example.parkseva.fragments.MapFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }
            
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            }
            return true;
        });
        
        // Load default fragment
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new HomeFragment())
            .commit();
    }

    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("ParkSevaPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        if (token == null || token.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void switchToTab(int tabIndex) {
        bottomNavigation.setSelectedItemId(
            tabIndex == 0 ? R.id.nav_home :
            tabIndex == 1 ? R.id.nav_park :
            tabIndex == 2 ? R.id.nav_map : R.id.nav_profile
        );
    }
    
    private void applySavedLanguage() {
        SharedPreferences prefs = getSharedPreferences("ParkSevaPrefs", MODE_PRIVATE);
        String languageCode = prefs.getString("language", "en");
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
