package com.example.blood_donor.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;

public class IntroSliderAdapter extends RecyclerView.Adapter<IntroSliderAdapter.SlideViewHolder> {

    private static final int[] SLIDE_IMAGES = new int[]{
            R.drawable.intro_3,
            R.drawable.intro_2,
            R.drawable.intro_3
    };

    private static final int[] SLIDE_HEADINGS = new int[]{
            R.string.intro_heading_1,
            R.string.intro_heading_2,
            R.string.intro_heading_3
    };

    private static final int[] SLIDE_DESCRIPTIONS = new int[]{
            R.string.intro_desc_1,
            R.string.intro_desc_2,
            R.string.intro_desc_3
    };

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SlideViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.slide_item,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        holder.imageView.setImageResource(SLIDE_IMAGES[position]);
        holder.textTitle.setText(SLIDE_HEADINGS[position]);
        holder.textDescription.setText(SLIDE_DESCRIPTIONS[position]);
    }

    @Override
    public int getItemCount() {
        return SLIDE_IMAGES.length;
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textTitle;
        TextView textDescription;

        SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageSlide);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
        }
    }
}