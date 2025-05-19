package com.example.rma_2_sakib_avdibasic.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.example.rma_2_sakib_avdibasic.R;
import com.example.rma_2_sakib_avdibasic.models.Movie;
import java.util.concurrent.TimeUnit;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;

public class MovieRenderer {

    private final Context context;
    private final OnTrailerClickListener trailerClickListener;
    private final OnReminderClickListener reminderClickListener;

    public interface OnTrailerClickListener {
        void onTrailerClick(String videoId);
    }

    public interface OnReminderClickListener {
        void onReminderClick(View view, Movie movie, String userId);
    }

    public MovieRenderer(Context context, OnTrailerClickListener trailerClickListener, OnReminderClickListener reminderClickListener) {
        this.context = context;
        this.trailerClickListener = trailerClickListener;
        this.reminderClickListener = reminderClickListener;
    }

    public LinearLayout renderMovie(Movie movie, String userId, LinearLayout movieLayout) {
        movieLayout.setOrientation(LinearLayout.VERTICAL);
        movieLayout.setPadding(32, 0, 32, 32);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;

        int imageHeight = (int) (screenHeight * 0.22);

        FrameLayout imageFrame = new FrameLayout(context);
        imageFrame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, imageHeight
        ));

        ImageView imageView = new ImageView(context);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.rounded_corners);
        imageView.setClipToOutline(true);

        RequestBuilder<Drawable> backupRequest = Glide.with(context)
                .load("https://kompaskomerc.co.rs/wp-content/uploads/2021/01/Nedostupna-10.jpg")
                .apply(new RequestOptions().override(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight))
                .centerCrop()
                .transform(new ColorFilterTransformation(Color.argb(110, 0, 0, 0)));

        Glide.with(context)
                .load("https://a.ltrbxd.com/resized/sm/upload/" + movie.posterUrl)
                .apply(new RequestOptions().override(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight))
                .centerCrop()
                .transform(new ColorFilterTransformation(Color.argb(110, 0, 0, 0)))
                .error(backupRequest)
                .into(imageView);

        TextView titleOverlay = new TextView(context);FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START
        );
        titleParams.setMargins(24, 16, 0, 24);
        titleOverlay.setLayoutParams(titleParams);
        SpannableString spannableString = new SpannableString(movie.name + getYearOrDaysRemaining(movie));
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, movie.name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titleOverlay.setText(spannableString);
        titleOverlay.setTextColor(Color.WHITE);
        titleOverlay.setTextSize(20f);

        imageView.setOnClickListener(v -> trailerClickListener.onTrailerClick(movie.trailerUrl));

        imageFrame.addView(imageView);
        imageFrame.addView(titleOverlay);
        imageFrame.setPadding(0, 24, 0, 12);
        movieLayout.addView(imageFrame);

        LinearLayout infoRow = new LinearLayout(context);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams infoRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoRow.setLayoutParams(infoRowParams);

        LinearLayout movieInfoLayout = new LinearLayout(context);
        movieInfoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams movieInfoLayoutParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT);
        movieInfoLayoutParams.weight = 1;
        movieInfoLayout.setLayoutParams(movieInfoLayoutParams);

        movieInfoLayout.addView(createMovieInfoText("🎬 " + movie.director));
        movieInfoLayout.addView(createMovieInfoText("🎭 " + movie.genre));
        movieInfoLayout.addView(createMovieInfoText("⏱️ " + movie.runtime + " min"));
        movieInfoLayout.addView(createMovieInfoText("📍 " + movie.settingPlace + " · " + movie.settingYear));

        infoRow.addView(movieInfoLayout);

        if (movie.releaseDate != null) {
            Button reminderButton = new Button(context);
            reminderButton.setAllCaps(false);
            reminderButton.setTextColor(Color.parseColor("#FFFFFF"));
            reminderButton.setTypeface(reminderButton.getTypeface(), Typeface.BOLD);
            reminderButton.setPadding(24, 0, 24, 0);
            GradientDrawable goldPill = new GradientDrawable();
            goldPill.setCornerRadius(12);
            goldPill.setStroke(1, Color.parseColor("#FFD700"));
            reminderButton.setBackground(goldPill);

            setReminderButtonText(reminderButton, movie.hasReminder);

            LinearLayout.LayoutParams reminderLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            reminderLayoutParams.gravity = Gravity.END;
            reminderButton.setLayoutParams(reminderLayoutParams);
            reminderButton.setOnClickListener(v -> reminderClickListener.onReminderClick(v, movie, userId));
            infoRow.addView(reminderButton);
        }

        movieLayout.addView(infoRow);

        View divider = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#CCCCCC"));

        movieLayout.addView(divider);
        return movieLayout;
    }

    private void setReminderButtonText(Button button, boolean hasReminder) {
        if (hasReminder) {
            button.setText("\uD83D\uDD14 Otkaži podsjetnik");
        } else {
            button.setText("\uD83D\uDD14 Podsjeti me");
        }
    }

    private TextView createMovieInfoText(String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(16f);
        textView.setPadding(0, 8, 0, 8);
        return textView;
    }

    private String getYearOrDaysRemaining(Movie movie) {
        if (movie.releaseDate != null) {
            long diffInMillis = movie.releaseDate.getTime() - System.currentTimeMillis();
            if (diffInMillis > 0) {
                long days = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS) + 1;
                return " (za " + days + " dana)";
            } else {
                return " (" + movie.year + ")";
            }
        } else {
            return " (" + movie.year + ")";
        }
    }
}