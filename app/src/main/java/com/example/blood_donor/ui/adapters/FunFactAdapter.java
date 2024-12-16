package com.example.blood_donor.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;

import java.util.List;

public class FunFactAdapter extends RecyclerView.Adapter<FunFactAdapter.FunFactViewHolder> {
    private final List<String> funFacts;

    public FunFactAdapter(List<String> funFacts) {
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
        private final TextView contentView;

        FunFactViewHolder(@NonNull View itemView) {
            super(itemView);
            contentView = itemView.findViewById(R.id.funFactContent);
        }

        void bind(String funFact) {
            contentView.setText(funFact);
        }
    }
}
