package com.example.parkseva.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkseva.R;
import com.example.parkseva.models.BookingResponse;

import java.util.List;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.ViewHolder> {

    private List<BookingResponse> bookings;
    private OnBookingClickListener listener;

    public interface OnBookingClickListener {
        void onBookingClick(BookingResponse booking);
        void onDeleteClick(int position);
    }

    public BookingsAdapter(List<BookingResponse> bookings, OnBookingClickListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponse booking = bookings.get(position);
        holder.tvVehicleNumber.setText(booking.getVehicleNumber());
        holder.tvSlotNumber.setText("Slot: " + booking.getSlotNumber());
        
        holder.itemView.setOnClickListener(v -> listener.onBookingClick(booking));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(position));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleNumber, tvSlotNumber;
        Button btnDelete;

        ViewHolder(View view) {
            super(view);
            tvVehicleNumber = view.findViewById(R.id.tvVehicleNumber);
            tvSlotNumber = view.findViewById(R.id.tvSlotNumber);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }
}
