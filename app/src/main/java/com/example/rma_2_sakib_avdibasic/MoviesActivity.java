package com.example.rma_2_sakib_avdibasic;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.View;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import com.example.rma_2_sakib_avdibasic.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class MoviesActivity extends AppCompatActivity {

    private User user;
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private FrameLayout trailerOverlay;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer myYouTubePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movies);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomInset = insets.getSystemWindowInsetBottom();
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset);
            return insets.consumeSystemWindowInsets();
        });

        user = (User) getIntent().getSerializableExtra("user");

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            loadFragment(OldMoviesFragment.newInstance(user));
        }

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_old_movies) {
                    fragment = OldMoviesFragment.newInstance(user);
                } else if (itemId == R.id.nav_new_movies) {
                    fragment = NewMoviesFragment.newInstance(user);
                }

                if (fragment != null) {
                    loadFragment(fragment);
                    return true;
                }
                return false;
            }
        });

        trailerOverlay = findViewById(R.id.trailerOverlay);
        youTubePlayerView = findViewById(R.id.youtube_player_view);

        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                myYouTubePlayer = youTubePlayer;
            }
        });

        trailerOverlay.setOnClickListener(v -> {
            hideTrailer();
        });

    }

    public void hideTrailer() {
        if (myYouTubePlayer != null) {
            myYouTubePlayer.pause();
        }
        youTubePlayerView.setVisibility(View.GONE);
        trailerOverlay.setVisibility(View.GONE);
    }
    public void showTrailer(String videoId) {
        trailerOverlay.setVisibility(View.VISIBLE);
        youTubePlayerView.setVisibility(View.VISIBLE);
        if (myYouTubePlayer != null) {
            myYouTubePlayer.loadVideo(videoId, 0);
        } else {
            Log.w("YouTube", "YouTubePlayer is not ready.");
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }
}

