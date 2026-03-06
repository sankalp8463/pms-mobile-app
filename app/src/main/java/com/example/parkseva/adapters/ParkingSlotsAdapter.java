package com.example.parkseva.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkseva.R;
import com.example.parkseva.models.ParkingSlot;

import java.util.List;

public class ParkingSlotsAdapter extends RecyclerView.Adapter<ParkingSlotsAdapter.ViewHolder> {
    
    private List<ParkingSlot> slots;
    private OnSlotBookListener listener;

    public interface OnSlotBookListener {
        void onSlotBook(ParkingSlot slot);
    }

    public ParkingSlotsAdapter(List<ParkingSlot> slots, OnSlotBookListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingSlot slot = slots.get(position);
        holder.bind(slot);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public void updateSlots(List<ParkingSlot> newSlots) {
        this.slots = newSlots;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotNumber, tvStatus, tvVehicleType, tvHourlyRate;
        Button btnBookSlot;

        ViewHolder(View itemView) {
            super(itemView);
            tvSlotNumber = itemView.findViewById(R.id.tvSlotNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            tvHourlyRate = itemView.findViewById(R.id.tvHourlyRate);
            btnBookSlot = itemView.findViewById(R.id.btnBookSlot);
        }

        void bind(ParkingSlot slot) {
            tvSlotNumber.setText(slot.getSlotNumber());
            tvStatus.setText(slot.getStatus().toUpperCase());
            tvVehicleType.setText(slot.getVehicleType().toUpperCase());
            tvHourlyRate.setText("₹" + slot.getHourlyRate() + "/hour");

            boolean isAvailable = "available".equals(slot.getStatus());
            btnBookSlot.setEnabled(isAvailable);
            btnBookSlot.setText(isAvailable ? "🎫 Book This Slot" : "Not Available");
            
            // Update status badge color
            if (isAvailable) {
                tvStatus.setBackgroundColor(0xFFE8F5E8);
                tvStatus.setTextColor(0xFF2E7D32);
            } else {
                tvStatus.setBackgroundColor(0xFFFFEBEE);
                tvStatus.setTextColor(0xFFC62828);
            }

            btnBookSlot.setOnClickListener(v -> {
                if (listener != null && isAvailable) {
                    listener.onSlotBook(slot);
                }
            });
        }
    }
}