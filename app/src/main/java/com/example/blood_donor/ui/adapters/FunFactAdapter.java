package com.example.blood_donor.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;

import java.util.List;

public class FunFactAdapter extends RecyclerView.Adapter<FunFactAdapter.FunFactViewHolder> {
    private final List<FunFactItem> funFacts;
    public static class FunFactItem {
        final String title;
        final String content;
        final int illustrationResId;

        public FunFactItem(String title, String content, int illustrationResId) {
            this.title = title;
            this.content = content;
            this.illustrationResId = illustrationResId;
        }
    }
    public FunFactAdapter(List<FunFactItem> funFacts) {
        this.funFacts = funFacts;
    }

    @NonNull
    @Override
    public FunFactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_fun_fact, parent, false);
        return new FunFactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FunFactViewHolder holder, int position) {
        holder.bind(funFacts.get(position));
    }

    @Override
    public int getItemCount() {
        return funFacts.size();
    }

    static class FunFactViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView contentView;
        private final ImageView imageView;

        FunFactViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.funFactTitle);
            contentView = itemView.findViewById(R.id.funFactContent);
            imageView = itemView.findViewById(R.id.funFactImage);
        }

        void bind(FunFactItem funFact) {
            titleView.setText(funFact.title);
            contentView.setText(funFact.content);
            imageView.setImageResource(funFact.illustrationResId);
        }
    }
}