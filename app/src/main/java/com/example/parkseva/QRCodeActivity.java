package com.example.parkseva;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parkseva.utils.QRCodeGenerator;

public class QRCodeActivity extends AppCompatActivity {
    
    private ImageView ivQRCode;
    private TextView tvBookingDetails;
    private Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        initViews();
        displayBookingInfo();
        generateQRCode();
    }

    private void initViews() {
        ivQRCode = findViewById(R.id.ivQRCode);
        tvBookingDetails = findViewById(R.id.tvBookingDetails);
        btnDone = findViewById(R.id.btnDone);

        btnDone.setOnClickListener(v -> finish());
    }

    private void displayBookingInfo() {
        String vehicleNumber = getIntent().getStringExtra("vehicleNumber");
        String slotNumber = getIntent().getStringExtra("slotNumber");
        double amount = getIntent().getDoubleExtra("amount", 0.0);

        String details = "Vehicle: " + vehicleNumber + "\n" +
                        "Slot: " + slotNumber + "\n" +
                        "Amount: ₹" + amount;
        
        tvBookingDetails.setText(details);
    }

    private void generateQRCode() {
        String vehicleNumber = getIntent().getStringExtra("vehicleNumber");
        String slotNumber = getIntent().getStringExtra("slotNumber");
        String bookingId = getIntent().getStringExtra("bookingId");

        String qrData = QRCodeGenerator.createBookingQRData(vehicleNumber, slotNumber, bookingId);
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData, 300, 300);
        
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        }
    }
}