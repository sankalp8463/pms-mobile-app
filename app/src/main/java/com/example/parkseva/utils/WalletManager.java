package com.example.parkseva.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class WalletManager {

    public static final String PREF_NAME = "ParkSevaPrefs";
    private static final String KEY_WALLET_BALANCE = "wallet_balance";
    private static final String KEY_WALLET_TRANSACTIONS = "wallet_transactions";
    private static final double MIN_TOP_UP_AMOUNT = 10.0;

    private WalletManager() {
    }

    public static double getBalance(Context context) {
        return getPrefs(context).getFloat(KEY_WALLET_BALANCE, 0f);
    }

    public static boolean hasSufficientBalance(Context context, double amount) {
        return getBalance(context) >= amount;
    }

    public static boolean addFunds(Context context, double amount, String title) {
        if (amount < MIN_TOP_UP_AMOUNT) {
            return false;
        }

        double updatedBalance = getBalance(context) + amount;
        getPrefs(context).edit().putFloat(KEY_WALLET_BALANCE, (float) updatedBalance).apply();
        appendTransaction(context, title, amount, "credit");
        return true;
    }

    public static boolean charge(Context context, double amount, String title) {
        double balance = getBalance(context);
        if (amount <= 0 || balance < amount) {
            return false;
        }

        double updatedBalance = balance - amount;
        getPrefs(context).edit().putFloat(KEY_WALLET_BALANCE, (float) updatedBalance).apply();
        appendTransaction(context, title, amount, "debit");
        return true;
    }

    public static List<String> getRecentTransactions(Context context, int limit) {
        List<String> transactions = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(getPrefs(context).getString(KEY_WALLET_TRANSACTIONS, "[]"));
            int max = Math.min(limit, array.length());
            for (int i = 0; i < max; i++) {
                JSONObject item = array.getJSONObject(i);
                String type = item.optString("type", "debit");
                String title = item.optString("title", "Wallet activity");
                double amount = item.optDouble("amount", 0);
                long timestamp = item.optLong("timestamp", 0);
                String sign = "credit".equalsIgnoreCase(type) ? "+" : "-";
                String formattedTime = formatTimestamp(timestamp);
                transactions.add(title + "  " + sign + formatCurrency(amount) + "\n" + formattedTime);
            }
        } catch (Exception ignored) {
        }

        return transactions;
    }

    public static String formatCurrency(double amount) {
        return String.format(Locale.US, "Rs %.2f", amount);
    }

    private static void appendTransaction(Context context, String title, double amount, String type) {
        try {
            SharedPreferences prefs = getPrefs(context);
            JSONArray existing = new JSONArray(prefs.getString(KEY_WALLET_TRANSACTIONS, "[]"));
            JSONArray updated = new JSONArray();

            JSONObject entry = new JSONObject();
            entry.put("title", title);
            entry.put("amount", amount);
            entry.put("type", type);
            entry.put("timestamp", System.currentTimeMillis());
            updated.put(entry);

            for (int i = 0; i < existing.length() && i < 19; i++) {
                updated.put(existing.getJSONObject(i));
            }

            prefs.edit().putString(KEY_WALLET_TRANSACTIONS, updated.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "Just now";
        }
        return new SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(new Date(timestamp));
    }
}
