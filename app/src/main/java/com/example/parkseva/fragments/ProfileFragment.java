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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.parkseva.LoginActivity;
import com.example.parkseva.R;
import com.example.parkseva.api.ApiClient;
import com.example.parkseva.models.User;
import com.example.parkseva.utils.WalletManager;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvUserPhone, tvUserEmail, tvUserRole, tvWalletBalance, tvWalletActivity;
    private Button btnLogout, btnSettings, btnTopUpWallet;
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
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvWalletActivity = view.findViewById(R.id.tvWalletActivity);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnTopUpWallet = view.findViewById(R.id.btnTopUpWallet);
        
        sharedPreferences = requireContext().getSharedPreferences("ParkSevaPrefs", MODE_PRIVATE);
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> logout());
        btnSettings.setOnClickListener(v -> showLanguageDialog());
        btnTopUpWallet.setOnClickListener(v -> showTopUpDialog());
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
        refreshWalletSection();
    }

    private void loadFromCache() {
        // Load from SharedPreferences as fallback
        String userName = sharedPreferences.getString("user_name", "Unknown");
        tvUserName.setText(userName);
        tvUserPhone.setText("Loading...");
        tvUserEmail.setText("Loading...");
        tvUserRole.setText("Loading...");
        refreshWalletSection();
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

    @Override
    public void onResume() {
        super.onResume();
        refreshWalletSection();
    }

    private void refreshWalletSection() {
        if (tvWalletBalance == null || tvWalletActivity == null) {
            return;
        }

        tvWalletBalance.setText(WalletManager.formatCurrency(WalletManager.getBalance(requireContext())));

        List<String> transactions = WalletManager.getRecentTransactions(requireContext(), 3);
        if (transactions.isEmpty()) {
            tvWalletActivity.setText(getString(R.string.no_wallet_activity_yet));
        } else {
            tvWalletActivity.setText(android.text.TextUtils.join("\n\n", transactions));
        }
    }

    private void showTopUpDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wallet_top_up, null, false);
        TextView tvTopUpBalance = dialogView.findViewById(R.id.tvTopUpBalance);
        EditText etCustomAmount = dialogView.findViewById(R.id.etCustomAmount);
        Button btnAdd100 = dialogView.findViewById(R.id.btnAdd100);
        Button btnAdd250 = dialogView.findViewById(R.id.btnAdd250);
        Button btnAdd500 = dialogView.findViewById(R.id.btnAdd500);
        Button btnAddCustomAmount = dialogView.findViewById(R.id.btnAddCustomAmount);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();

        tvTopUpBalance.setText(getString(R.string.current_wallet_balance) + ": "
            + WalletManager.formatCurrency(WalletManager.getBalance(requireContext())));

        View.OnClickListener quickAddListener = v -> {
            double amount = v.getId() == R.id.btnAdd100 ? 100 : v.getId() == R.id.btnAdd250 ? 250 : 500;
            handleTopUp(amount, dialog);
        };

        btnAdd100.setOnClickListener(quickAddListener);
        btnAdd250.setOnClickListener(quickAddListener);
        btnAdd500.setOnClickListener(quickAddListener);
        btnAddCustomAmount.setOnClickListener(v -> {
            String value = etCustomAmount.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(requireContext(), R.string.wallet_top_up_invalid, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                handleTopUp(Double.parseDouble(value), dialog);
            } catch (NumberFormatException exception) {
                Toast.makeText(requireContext(), R.string.wallet_top_up_invalid, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void handleTopUp(double amount, AlertDialog dialog) {
        boolean success = WalletManager.addFunds(requireContext(), amount, getString(R.string.wallet_top_up_title));
        if (!success) {
            Toast.makeText(requireContext(), R.string.wallet_top_up_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), R.string.wallet_top_up_success, Toast.LENGTH_SHORT).show();
        refreshWalletSection();
        dialog.dismiss();
    }
}
