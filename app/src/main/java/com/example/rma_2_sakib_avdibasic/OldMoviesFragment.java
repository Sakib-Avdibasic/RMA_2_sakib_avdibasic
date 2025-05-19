package com.example.rma_2_sakib_avdibasic;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.rma_2_sakib_avdibasic.models.Movie;
import com.example.rma_2_sakib_avdibasic.models.User;
import com.example.rma_2_sakib_avdibasic.utils.MovieRenderer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OldMoviesFragment extends Fragment implements MovieRenderer.OnTrailerClickListener, MovieRenderer.OnReminderClickListener {
    private static final String TMDB_API_KEY = "7adaeb9096ca6330821e0f3d1e2a3a20";
    private final Handler handler = new Handler();
    private final Map<AutoCompleteTextView, Runnable> debounceRunnables = new HashMap<>();
    private AutoCompleteTextView actorInput;
    private AutoCompleteTextView directorInput;
    private Spinner genreSpinner;
    private EditText startYearInput;
    private EditText endYearInput;
    private AutoCompleteTextView placeInput;
    Button searchButton;
    ScrollView scrollView;
    private FirebaseFirestore db;
    private User user;
    private TextView welcomeView;
    private LinearLayout moviesContainer;
    private MovieRenderer movieRenderer;
    private LinearLayout topGrossingMoviesLayout;
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public OldMoviesFragment() {
    }

    public static OldMoviesFragment newInstance(User user) {
        OldMoviesFragment fragment = new OldMoviesFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = (User) getArguments().getSerializable("user");
        }


        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_old_movies, container, false);

        LinearLayout searchContainer = view.findViewById(R.id.searchContainer);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(24f);
        drawable.setStroke(1, Color.WHITE);
        searchContainer.setBackground(drawable);

        ((MoviesActivity) requireActivity()).hideTrailer();

        welcomeView = view.findViewById(R.id.welcomeText);
        topGrossingMoviesLayout = view.findViewById(R.id.topGrossingMoviesLayout);
        if (user != null) {
            welcomeView.setText("\uD83D\uDD75\uFE0F Detaljnija pretraga");
            fetchTopGrossingMoviesByBirthYear(getBirthYear(new Date(user.getBirthday())));
        }

        moviesContainer = view.findViewById(R.id.moviesContainer);
        scrollView = view.findViewById(R.id.scrollView1);
        movieRenderer = new MovieRenderer(requireContext(), this, this);

        genreSpinner = view.findViewById(R.id.genreInput);
        startYearInput = view.findViewById(R.id.startYearInput);
        endYearInput = view.findViewById(R.id.endYearInput);
        actorInput = view.findViewById(R.id.actorInput);
        directorInput = view.findViewById(R.id.directorInput);
        placeInput = view.findViewById(R.id.locationInput);
        searchButton = view.findViewById(R.id.btnSearch);

        populateSpinnerFromFirestore(genreSpinner, "genres");
        setupAutoComplete(actorInput, "actors");
        setupAutoComplete(directorInput, "directors");
        setupAutoComplete(actorInput, "actors");
        setupAutoComplete(directorInput, "directors");
        setupAutoComplete(placeInput, "movies");

        searchButton.setOnClickListener(v -> performSearch());

        return view;
    }

    private int getBirthYear(java.util.Date birthDate) {
        if (birthDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(birthDate);
            return calendar.get(Calendar.YEAR)-1;
        }
        return -1;
    }

    private void fetchTopGrossingMoviesByBirthYear(int birthYear) {
        if (birthYear <= 0 || TMDB_API_KEY.equals("YOUR_TMDB_API_KEY")) {
            Log.w("TMDB", "Invalid birth year or TMDB API key not set.");
            return;
        }

        executorService.execute(() -> {
            String discoverUrl = String.format(Locale.US,
                    "https://api.themoviedb.org/3/discover/movie?api_key=%s&sort_by=revenue.desc&primary_release_year=%d",
                    TMDB_API_KEY, birthYear);
            Request request = new Request.Builder().url(discoverUrl).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray results = jsonResponse.getJSONArray("results");
                    List<TMDBMovie> topMovies = new ArrayList<>();
                    for (int i = 0; i < Math.min(3, results.length()); i++) {
                        JSONObject movieJson = results.getJSONObject(i);
                        String title = movieJson.getString("title");
                        String posterPath = movieJson.getString("poster_path");
                        int movieId = movieJson.getInt("id");
                        topMovies.add(new TMDBMovie(movieId, title, posterPath));
                    }

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> displayTopGrossingMovies(topMovies));
                    }

                } else {
                    Log.e("TMDB", "Failed to fetch top grossing movies: " + response);
                }
            } catch (IOException e) {
                Log.e("TMDB", "Network error while fetching top grossing movies: ", e);
            } catch (JSONException e) {
                Log.e("TMDB", "JSON parsing error for top grossing movies: ", e);
            }
        });
    }

    private void displayTopGrossingMovies(List<TMDBMovie> movies) {
        if (topGrossingMoviesLayout != null) {
            topGrossingMoviesLayout.removeAllViews();
            if (!movies.isEmpty()) {
                TextView titleTextView = new TextView(requireContext());
                titleTextView.setText("\uD83D\uDCA1 Naša preporuka");
                titleTextView.setTypeface(titleTextView.getTypeface(), Typeface.BOLD);
                titleTextView.setTextSize(20);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                titleParams.setMargins(32, 32, 32, 8);
                topGrossingMoviesLayout.addView(titleTextView, titleParams);

                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);
                GradientDrawable containerBg = new GradientDrawable();
                containerBg.setColor(Color.parseColor("#303030"));
                containerBg.setStroke(1, Color.parseColor("#FFD700"));
                containerBg.setCornerRadius(24);
                container.setBackground(containerBg);
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                containerParams.setMargins(32, 8, 32, 8);
                container.setLayoutParams(containerParams);

                TextView titleTextView2 = new TextView(requireContext());
                titleTextView2.setText("Najpopularniji filmovi iz " + getBirthYear(new Date(user.getBirthday())) + ".");
                titleTextView2.setTextColor(Color.WHITE);
                titleTextView2.setTextSize(16);
                titleTextView2.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams titleParams2 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                titleParams2.setMargins(32, 24, 32, 8);
                container.addView(titleTextView2, titleParams2);

                for (int i = 0; i < movies.size(); i++) {
                    TMDBMovie movie = movies.get(i);
                    LinearLayout movieItem = new LinearLayout(requireContext());
                    movieItem.setOrientation(LinearLayout.HORIZONTAL);
                    movieItem.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    itemParams.setMargins(24, 8, 24, 16);
                    movieItem.setLayoutParams(itemParams);

                    TextView orderTextView = new TextView(requireContext());
                    orderTextView.setText(String.valueOf(i + 1));
                    orderTextView.setTextColor(Color.WHITE);
                    orderTextView.setGravity(Gravity.CENTER);
                    orderTextView.setTypeface(Typeface.MONOSPACE);
                    orderTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    orderTextView.setPadding(8, 8, 8, 8);
                    GradientDrawable circle = new GradientDrawable();
                    circle.setShape(GradientDrawable.RECTANGLE);
                    circle.setColor(Color.TRANSPARENT);
                    circle.setStroke(1, Color.WHITE);
                    circle.setCornerRadius(18);
                    orderTextView.setBackground(circle);
                    LinearLayout.LayoutParams orderParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    orderParams.setMarginEnd(16);
                    orderTextView.setLayoutParams(orderParams);
                    movieItem.addView(orderTextView);

                    TextView movieTitleTextView = new TextView(requireContext());
                    movieTitleTextView.setText(movie.getTitle());
                    movieTitleTextView.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams titleTextParams = new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );
                    movieTitleTextView.setLayoutParams(titleTextParams);
                    movieItem.addView(movieTitleTextView);

                    Button trailerButton = new Button(requireContext());
                    trailerButton.setText("▶ Pogledaj trailer");
                    trailerButton.setTextColor(Color.WHITE);
                    trailerButton.setBackgroundColor(Color.parseColor("#301934"));
                    trailerButton.setPadding(20, 10, 20, 10);
                    trailerButton.setMinWidth(200);
                    trailerButton.setAllCaps(false);
                    GradientDrawable shape = new GradientDrawable();
                    shape.setShape(GradientDrawable.RECTANGLE);
                    shape.setCornerRadius(24);
                    shape.setColor(Color.parseColor("#301934"));
                    trailerButton.setBackground(shape);
                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    trailerButton.setLayoutParams(buttonParams);
                    trailerButton.setOnClickListener(v -> fetchAndShowTrailer(movie.getId()));
                    movieItem.addView(trailerButton);

                    container.addView(movieItem);
                }

                topGrossingMoviesLayout.addView(container);
            }
        }
    }

    private void fetchAndShowTrailer(int movieId) {
        if (TMDB_API_KEY.equals("YOUR_TMDB_API_KEY")) {
            Toast.makeText(requireContext(), "TMDB API key not set.", Toast.LENGTH_SHORT).show();
            return;
        }
        executorService.execute(() -> {
            String videosUrl = String.format(Locale.US,
                    "https://api.themoviedb.org/3/movie/%d/videos?api_key=%s",
                    movieId, TMDB_API_KEY);
            Request request = new Request.Builder().url(videosUrl).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray results = jsonResponse.getJSONArray("results");
                    String trailerKey = null;
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject video = results.getJSONObject(i);
                        if (video.getString("type").equalsIgnoreCase("Trailer") && video.getString("site").equalsIgnoreCase("YouTube")) {
                            trailerKey = video.getString("key");
                            break;
                        }
                    }
                    if (trailerKey != null && isAdded()) {
                        String finalTrailerKey = trailerKey;
                        requireActivity().runOnUiThread(() -> ((MoviesActivity) requireActivity()).showTrailer(finalTrailerKey));
                    } else {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Trailer not found.", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    Log.e("TMDB", "Failed to fetch videos for movie " + movieId + ": " + response);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch trailer.", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (IOException e) {
                Log.e("TMDB", "Network error while fetching videos: ", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Network error fetching trailer.", Toast.LENGTH_SHORT).show());
                }
            } catch (JSONException e) {
                Log.e("TMDB", "JSON parsing error for videos: ", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error parsing trailer info.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    private void performSearch() {
        hideKeyboard();

        String actorName = actorInput.getText().toString().trim();
        String directorName = directorInput.getText().toString().trim();
        String genreName = (genreSpinner.getSelectedItemPosition() > 0)
                ? genreSpinner.getSelectedItem().toString()
                : "";
        String startYearStr = startYearInput.getText().toString().trim();
        String endYearStr = endYearInput.getText().toString().trim();
        String settingPlace = placeInput.getText().toString().trim();

        Integer startYear = null;
        Integer endYear = null;

        if (!startYearStr.isEmpty()) {
            try {
                startYear = Integer.parseInt(startYearStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Neispravan format početne godine.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!endYearStr.isEmpty()) {
            try {
                endYear = Integer.parseInt(endYearStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Neispravan format krajnje godine.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Task<DocumentSnapshot> actorTask = findPersonByName("actors", actorName);
        Task<DocumentSnapshot> directorTask = findPersonByName("directors", directorName);
        Task<QueryDocumentSnapshot> genreTask = (genreName != "")
                ? findGenreByName(genreName)
                : Tasks.forResult(null);


        Integer finalStartYear = startYear;
        Integer finalEndYear = endYear;

        Tasks.whenAllComplete(actorTask, directorTask, genreTask).addOnSuccessListener(tasks -> {
            String actorId = (actorTask.isSuccessful() && actorTask.getResult() != null) ? actorTask.getResult().getId() : null;
            String directorId = (directorTask.isSuccessful() && directorTask.getResult() != null) ? directorTask.getResult().getId() : null;
            String genreId = (genreTask.isSuccessful() && genreTask.getResult() != null) ? genreTask.getResult().getId() : null;

            fetchFilteredMovies(actorId, directorId, genreId, finalStartYear, finalEndYear, settingPlace);
        });
    }

    private void fetchFilteredMovies(String actorIdStr, String directorId, String genreId, Integer startYear, Integer endYear, String settingPlaceFilter) {
        if (actorIdStr != null && !actorIdStr.isEmpty()) {
            try {
                int actorId = Integer.parseInt(actorIdStr);
                db.collection("movie_casts")
                        .whereEqualTo("actor_id", actorId)
                        .get()
                        .addOnSuccessListener(castSnapshot -> {
                            List<String> movieIds = new ArrayList<>();
                            if (!castSnapshot.isEmpty()) {
                                for (QueryDocumentSnapshot castDoc : castSnapshot) {
                                    Number movieIdNumber = castDoc.getLong("movie_id");
                                    if (movieIdNumber != null) {
                                        movieIds.add(String.valueOf(movieIdNumber.intValue()));
                                    }
                                }
                                if (!movieIds.isEmpty()) {
                                    fetchMoviesByIdsAndFilters(movieIds, directorId, genreId, startYear, endYear, settingPlaceFilter);
                                } else {
                                    renderMovies(new ArrayList<>());
                                }
                            } else {
                                renderMovies(new ArrayList<>());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firebase", "Error fetching movie casts: ", e);
                            renderMovies(new ArrayList<>());
                        });
            } catch (NumberFormatException e) {
                Log.w("Filter", "Invalid actor ID format : " + actorIdStr);
                fetchMoviesRegular(directorId, genreId, startYear, endYear, settingPlaceFilter);
            }
        } else {
            fetchMoviesRegular(directorId, genreId, startYear, endYear, settingPlaceFilter);
        }
    }

    private void fetchMoviesByIdsAndFilters(List<String> movieIds, String directorId, String genreId, Integer startYear, Integer endYear, String settingPlaceFilter) {
        db.collection("movies")
                .whereIn(FieldPath.documentId(), movieIds)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Movie> movieList = new ArrayList<>();
                    for (DocumentSnapshot movieDoc : snapshot.getDocuments()) {
                        if(movieDoc.contains("release_date")) continue;
                        String mDirectorId = String.valueOf(movieDoc.get("director_id"));
                        String mGenreId = String.valueOf(movieDoc.get("genre_id"));
                        String mSettingPlace = movieDoc.getString("setting_place");
                        Long settingYearLong = movieDoc.getLong("setting_year");
                        int settingYear = settingYearLong != null ? settingYearLong.intValue() : 0;

                        boolean matchesDirector = (directorId == null || directorId.equals(mDirectorId));
                        boolean matchesGenre = (genreId == null || genreId.equals(mGenreId));
                        boolean matchesYear = true;
                        boolean matchesPlace = (settingPlaceFilter == null || settingPlaceFilter.isEmpty() || (mSettingPlace != null && mSettingPlace.toLowerCase().contains(settingPlaceFilter.toLowerCase())));

                        if (startYear != null && settingYear < startYear) {
                            matchesYear = false;
                        }
                        if (endYear != null && settingYear > endYear) {
                            matchesYear = false;
                        }
                        if (matchesDirector && matchesGenre && matchesYear && matchesPlace) {
                            processAndAddMovie(movieDoc, movieList);
                        }
                    }
                    renderMovies(movieList);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error fetching movies by IDs: ", e);
                    renderMovies(new ArrayList<>());
                });
    }

    private void fetchMoviesRegular(String directorId, String genreId, Integer startYear, Integer endYear, String settingPlaceFilter) {
        db.collection("movies").get().addOnSuccessListener(snapshot -> {
            List<Movie> movieList = new ArrayList<>();
            for (DocumentSnapshot movieDoc : snapshot.getDocuments()) {
                if(movieDoc.contains("release_date")) continue;
                String mDirectorId = String.valueOf(movieDoc.get("director_id"));
                String mGenreId = String.valueOf(movieDoc.get("genre_id"));
                String mSettingPlace = movieDoc.getString("setting_place");
                Long settingYearLong = movieDoc.getLong("setting_year");
                int settingYear = settingYearLong != null ? settingYearLong.intValue() : 0;

                boolean matchesDirector = (directorId == null || directorId.equals(mDirectorId));
                boolean matchesGenre = (genreId == null || genreId.equals(mGenreId));
                boolean matchesYear = true;
                boolean matchesPlace = (settingPlaceFilter == null || settingPlaceFilter.isEmpty() || (mSettingPlace != null && mSettingPlace.toLowerCase().contains(settingPlaceFilter.toLowerCase())));

                if (startYear != null && settingYear < startYear) {
                    matchesYear = false;
                }
                if (endYear != null && settingYear > endYear) {
                    matchesYear = false;
                }

                if (matchesDirector && matchesGenre && matchesYear && matchesPlace) {
                    processAndAddMovie(movieDoc, movieList);
                }
            }
            renderMovies(movieList);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Error fetching movies: ", e);
            renderMovies(new ArrayList<>());
        });
    }

    private void processAndAddMovie(DocumentSnapshot movieDoc, List<Movie> movieList) {
        String mDirectorId = String.valueOf(movieDoc.get("director_id"));
        String mGenreId = String.valueOf(movieDoc.get("genre_id"));

        String name = movieDoc.getString("name");
        Long runtimeLong = movieDoc.getLong("runtime");
        int runtime = runtimeLong != null ? runtimeLong.intValue() : 0;
        Long movieYearLong = movieDoc.getLong("year");
        int movieYear = movieYearLong != null ? movieYearLong.intValue() : 0;
        Long settingYearLong = movieDoc.getLong("setting_year");
        int settingYear = settingYearLong != null ? settingYearLong.intValue() : 0;
        String settingPlace = movieDoc.getString("setting_place");
        String posterUrl = movieDoc.getString("posterUrl");
        String trailerUrl = movieDoc.getString("trailerUrl");

        Task<DocumentSnapshot> dirRef = db.collection("directors").document(mDirectorId).get();
        Task<DocumentSnapshot> genRef = db.collection("genres").document(mGenreId).get();

        Tasks.whenAllComplete(dirRef, genRef).addOnSuccessListener(details -> {
            String directorName = "", genreName = "";
            if (dirRef.isSuccessful()) {
                DocumentSnapshot d = dirRef.getResult();
                directorName = d.getString("first_name") + " " + d.getString("last_name");
            }
            if (genRef.isSuccessful()) {
                DocumentSnapshot g = genRef.getResult();
                genreName = g.getString("name");
            }

            Movie movie = new Movie(name, directorName, genreName, movieYear, runtime, settingPlace, settingYear, posterUrl, trailerUrl);
            movie.id = movieDoc.getString("id");
            movieList.add(movie);
            String dateString = movieDoc.getString("release_date");
            if (dateString != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    movie.releaseDate = sdf.parse(dateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            renderMovies(movieList);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Error fetching director/genre: ", e);
        });
    }

    private Task<DocumentSnapshot> findPersonByName(String collection, String name) {
        TaskCompletionSource<DocumentSnapshot> source = new TaskCompletionSource<>();
        if (name.isEmpty()) {
            source.setResult(null);
        } else {
            db.collection(collection).get().addOnSuccessListener(snap -> {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String fullName = (doc.getString("first_name") + " " + doc.getString("last_name")).trim();
                    if (fullName.equalsIgnoreCase(name)) {
                        source.setResult(doc);
                        return;
                    }
                }
                source.setResult(null);
            });
        }
        return source.getTask();
    }

    private Task<QueryDocumentSnapshot> findGenreByName(String name) {
        TaskCompletionSource<QueryDocumentSnapshot> source = new TaskCompletionSource<>();
        if (name.isEmpty()) {
            source.setResult(null);
        } else {
            db.collection("genres").get().addOnSuccessListener(snap -> {
                for (QueryDocumentSnapshot doc : snap) {
                    if (name.equalsIgnoreCase(doc.getString("name"))) {
                        source.setResult(doc);
                        return;
                    }
                }
                source.setResult(null);
            });
        }
        return source.getTask();
    }

    private void renderMovies(List<Movie> movieList) {
        if (moviesContainer == null) return;
        moviesContainer.removeAllViews();

        for (Movie movie : movieList) {
            movieRenderer.renderMovie(movie, user.getId(), moviesContainer);
        }

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                int targetY = moviesContainer.getTop();

                ObjectAnimator animator = ObjectAnimator.ofInt(scrollView, "scrollY", targetY);
                animator.setDuration(250);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.start();
            }
        });
    }

    private void addReminder(String movieId, String userId) {
        Map<String, String> notification = new HashMap<>();
        notification.put("movie_id", movieId);
        notification.put("user_id", userId);

        db.collection("movie_notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> Toast.makeText(requireContext(), "Podsjetnik postavljen!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Greška pri postavljanju podsjetnika.", Toast.LENGTH_SHORT).show());
    }

    private void setupAutoComplete(AutoCompleteTextView inputField, String collectionName) {
        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Runnable previousRunnable = debounceRunnables.get(inputField);
                if (previousRunnable != null) handler.removeCallbacks(previousRunnable);

                Runnable newRunnable = () -> fetchSuggestions(inputField, collectionName, s.toString().trim());
                debounceRunnables.put(inputField, newRunnable);
                handler.postDelayed(newRunnable, 500);
            }
        });
    }

    private void fetchSuggestions(AutoCompleteTextView inputField, String collectionName, String query) {
        if (query.isEmpty()) return;

        db.collection(collectionName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<String> suggestions = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    if (collectionName == "movies") {
                        String location = doc.getString("setting_place");
                        if (location.toLowerCase().startsWith(query.toLowerCase()) && !suggestions.contains(location)) {
                            suggestions.add(location);
                        }
                    } else {
                        String firstName = doc.getString("first_name");
                        String lastName = doc.getString("last_name");
                        if (firstName == null) firstName = "";
                        if (lastName == null) lastName = "";

                        String fullName = (firstName + " " + lastName).trim();
                        if (fullName.toLowerCase().startsWith(query.toLowerCase())) {
                            suggestions.add(fullName);
                        }
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions
                );
                inputField.setAdapter(adapter);
                inputField.showDropDown();
            }
        });
    }

    private void populateSpinnerFromFirestore(Spinner spinner, String collectionName) {
        db.collection(collectionName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<String> items = new ArrayList<>();
                items.add(collectionName.equals("genres") ? "Bilo koji žanr" : "Bilo koji osjećaj");
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String name = doc.getString("name");
                    if (name != null) items.add(name);
                }

                if (items.size() > 1) {
                    Collections.sort(items.subList(1, items.size()), String::compareToIgnoreCase);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item, items) {
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        return view;
                    }
                };

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }
        });
    }

    @Override
    public void onTrailerClick(String videoId) {
        if (getActivity() instanceof MoviesActivity) {
            ((MoviesActivity) getActivity()).showTrailer(videoId);
        }
    }

    @Override
    public void onReminderClick(View view, Movie movie, String userId) {
        addReminder(movie.id, userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        welcomeView = null;
        actorInput = null;
        directorInput = null;
        genreSpinner = null;
        startYearInput = null;
        endYearInput = null;
        moviesContainer = null;
        movieRenderer = null;
        topGrossingMoviesLayout = null;
    }

    private static class TMDBMovie {
        private final int id;
        private final String title;
        private final String posterPath;

        public TMDBMovie(int id, String title, String posterPath) {
            this.id = id;
            this.title = title;
            this.posterPath = posterPath;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getPosterPath() {
            return posterPath;
        }
    }
    private void hideKeyboard() {
        View view = getView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}