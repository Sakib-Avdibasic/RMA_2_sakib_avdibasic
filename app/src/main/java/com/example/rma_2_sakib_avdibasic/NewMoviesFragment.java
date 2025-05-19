package com.example.rma_2_sakib_avdibasic;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.rma_2_sakib_avdibasic.models.Movie;
import com.example.rma_2_sakib_avdibasic.models.User;
import com.example.rma_2_sakib_avdibasic.utils.MovieRenderer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NewMoviesFragment extends Fragment implements MovieRenderer.OnTrailerClickListener, MovieRenderer.OnReminderClickListener {
    private static final String CHANNEL_ID = "movie_reminder_channel";
    private static final int PERMISSION_REQUEST_CODE = 789;
    private FirebaseFirestore db;
    private LinearLayout moviesContainer;
    private MovieRenderer movieRenderer;
    private User user;
    private final Map<String, Boolean> userReminders = new HashMap<>();

    public NewMoviesFragment() {
    }

    public static NewMoviesFragment newInstance(User user) {
        NewMoviesFragment fragment = new NewMoviesFragment();
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
        createNotificationChannel();
        checkAndRequestNotificationPermission();
        movieRenderer = new MovieRenderer(requireContext(), this, this);
        ((MoviesActivity) getActivity()).hideTrailer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_movies, container, false);

        moviesContainer = view.findViewById(R.id.moviesContainer);

        fetchUserReminders();
        fetchUpcomingMovies();

        return view;
    }

    private void fetchUserReminders() {
        db.collection("movie_notifications")
                .whereEqualTo("user_id", user.getId())
                .get()
                .addOnSuccessListener(querySnapshot  -> {
                    userReminders.clear();
                    List<DocumentSnapshot> reminderDocs = querySnapshot.getDocuments();
                    List<QueryDocumentSnapshot> queryReminderDocs = new ArrayList<>();
                    for (DocumentSnapshot doc : reminderDocs) {
                        QueryDocumentSnapshot queryDoc = (QueryDocumentSnapshot) doc;
                        String movieId = queryDoc.getString("movie_id");
                        userReminders.put(movieId, true);
                        queryReminderDocs.add(queryDoc);
                    }
                    if (!queryReminderDocs.isEmpty()) {
                        showMovieReminderNotifications(queryReminderDocs);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error fetching user reminders: ", e);
                    fetchUpcomingMovies();
                });
    }

    private void showMovieReminderNotifications(List<QueryDocumentSnapshot> reminderDocs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = Calendar.getInstance().getTime();

        for (QueryDocumentSnapshot reminderDoc : reminderDocs) {
            String movieId = reminderDoc.getString("movie_id");
            if (movieId != null) {
                db.collection("movies").document(movieId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String movieName = documentSnapshot.getString("name");
                                String releaseDateStr = documentSnapshot.getString("release_date");
                                if (movieName != null && releaseDateStr != null) {
                                    try {
                                        Date releaseDate = sdf.parse(releaseDateStr);
                                        if (releaseDate != null && releaseDate.after(today)) {
                                            long timeDiff = releaseDate.getTime() - today.getTime();
                                            long daysLeft = TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS);

                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.baseline_movie_filter_24)
                                                    .setContentTitle(String.format(Locale.getDefault(), "%s stiže u kina za %d dana!", movieName, daysLeft+1))
                                                    .setContentText("Kupi svoje ulaznice na vrijeme!")
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
                                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                                notificationManager.notify(42069, builder.build());
                                            }
                                        }
                                    } catch (ParseException e) {
                                        Log.e("Notification", "Error parsing release date for notification: ", e);
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.e("Firebase", "Error fetching movie details for notification: ", e));
            }
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Notification permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchUpcomingMovies() {
        db.collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> upcomingMovies = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date today = Calendar.getInstance().getTime();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String dateString = document.getString("release_date");
                        String movieId = document.getId();
                        if (dateString != null) {
                            try {
                                Date releaseDate = sdf.parse(dateString);
                                if (releaseDate != null && releaseDate.after(today)) {
                                    String name = document.getString("name");
                                    Long directorIdLong = document.getLong("director_id");
                                    String director = directorIdLong != null ? String.valueOf(directorIdLong) : "";
                                    Long genreIdLong = document.getLong("genre_id");
                                    String genre = genreIdLong != null ? String.valueOf(genreIdLong) : "";
                                    Long yearLong = document.getLong("year");
                                    int year = yearLong != null ? yearLong.intValue() : 0;
                                    Long runtimeLong = document.getLong("runtime");
                                    int runtime = runtimeLong != null ? runtimeLong.intValue() : 0;
                                    String place = document.getString("setting_place");
                                    Long settingYearLong = document.getLong("setting_year");
                                    int settingYear = settingYearLong != null ? settingYearLong.intValue() : 0;
                                    String trailerUrl = document.getString("trailerUrl");
                                    String posterUrl = document.getString("posterUrl");
                                    Movie movie = new Movie(name, director, genre, year, runtime, place, settingYear, posterUrl, trailerUrl);
                                    movie.id = movieId;
                                    movie.releaseDate = releaseDate;
                                    movie.hasReminder = userReminders.containsKey(movie.id);
                                    fetchMovieDetails(movie, upcomingMovies);
                                }
                            } catch (ParseException e) {
                                Log.e("Firebase", "Error parsing release date: ", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error fetching movies: ", e);
                    Toast.makeText(requireContext(), "Failed to load upcoming movies.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchMovieDetails(Movie movie, List<Movie> upcomingMovies) {
        Task<DocumentSnapshot> directorTask = db.collection("directors").document(String.valueOf(movie.director)).get();
        Task<DocumentSnapshot> genreTask = db.collection("genres").document(String.valueOf(movie.genre)).get();

        Tasks.whenAllComplete(directorTask, genreTask)
                .addOnSuccessListener(tasks -> {
                    if (directorTask.isSuccessful() && directorTask.getResult() != null) {
                        DocumentSnapshot directorDoc = directorTask.getResult();
                        String firstName = directorDoc.getString("first_name");
                        String lastName = directorDoc.getString("last_name");
                        if (firstName != null && lastName != null) {
                            movie.director = firstName + " " + lastName;
                        }
                    }
                    if (genreTask.isSuccessful() && genreTask.getResult() != null) {
                        DocumentSnapshot genreDoc = genreTask.getResult();
                        String genreName = genreDoc.getString("name");
                        if (genreName != null) {
                            movie.genre = genreName;
                        }
                    }
                    upcomingMovies.add(movie);
                    renderMovies(upcomingMovies);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error fetching movie details: ", e);
                    upcomingMovies.add(movie);
                    renderMovies(upcomingMovies);
                });
    }

    private void renderMovies(List<Movie> movieList) {
        if (moviesContainer == null) return;
        moviesContainer.removeAllViews();
        for (Movie movie : movieList) {
            movieRenderer.renderMovie(movie, user.getId(), moviesContainer);
        }
    }

    private void addReminder(String movieId, String userId) {
        Map<String, String> notification = new HashMap<>();
        notification.put("movie_id", movieId);
        notification.put("user_id", userId);
        db.collection("movie_notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Podsjetnik postavljen.", Toast.LENGTH_SHORT).show();
                    db.collection("movies").document(movieId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String movieName = documentSnapshot.getString("name");
                                    String releaseDateStr = documentSnapshot.getString("release_date");
                                    if (movieName != null && releaseDateStr != null) {
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                        Date today = Calendar.getInstance().getTime();
                                        try {
                                            Date releaseDate = sdf.parse(releaseDateStr);
                                            if (releaseDate != null && releaseDate.after(today)) {
                                                long timeDiff = releaseDate.getTime() - today.getTime();
                                                long daysLeft = TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS);

                                                NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                                                        .setSmallIcon(R.drawable.baseline_movie_filter_24)
                                                        .setContentTitle(String.format(Locale.getDefault(), "%s stiže u kina za %d dana!", movieName, daysLeft+1))
                                                        .setContentText("Kupi svoje ulaznice na vrijeme!")
                                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
                                                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                                    notificationManager.notify(movieId.hashCode(), builder.build());
                                                }
                                            }
                                        } catch (ParseException e) {
                                            Log.e("Notification", "Error parsing release date for immediate notification: ", e);
                                        }
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error adding reminder: ", e);
                    Toast.makeText(requireContext(), "Greška pri postavljanju podsjetnika.", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeReminder(String movieId, String userId) {
        db.collection("movie_notifications")
                .whereEqualTo("movie_id", movieId)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentIdToDelete = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("movie_notifications").document(documentIdToDelete)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Podsjetnik otkazan.", Toast.LENGTH_SHORT).show();
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
                                    notificationManager.cancel(movieId.hashCode()); // Cancel the notification when reminder is removed
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firebase", "Error deleting reminder: ", e);
                                    Toast.makeText(requireContext(), "Greška pri otkazivanju podsjetnika.", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.d("Firebase", "No reminder found to delete for movie: " + movieId + " and user: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error querying for reminder to delete: ", e);
                    Toast.makeText(requireContext(), "Greška pri otkazivanju podsjetnika.", Toast.LENGTH_SHORT).show();
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
        Button button = (Button) view;
        if (movie.hasReminder) {
            removeReminder(movie.id, userId);
            button.setText("\uD83D\uDD14  Podsjeti me");
            movie.hasReminder = false;
        } else {
            addReminder(movie.id, userId);
            button.setText("\uD83D\uDD14 Otkaži podsjetnik");
            movie.hasReminder = true;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MovieReminderChannel";
            String description = "Channel for notifications about movie release reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        moviesContainer = null;
        movieRenderer = null;
    }
}