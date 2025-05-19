package com.example.rma_2_sakib_avdibasic;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.rma_2_sakib_avdibasic.models.User;
import com.example.rma_2_sakib_avdibasic.utils.PasswordHasher;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import jp.wasabeef.glide.transformations.BlurTransformation;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        Button btnLogin = findViewById(R.id.button);
        btnLogin.setTextColor(Color.BLACK);
        btnLogin.setOnClickListener((event) -> loginUser());
        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            loginUser();
            return true;
        });

        ImageView backgroundImage = findViewById(R.id.backgroundImage);
        View loginContainer = findViewById(R.id.loginContainer);

        int blurRadius = 10;
        int blurSampling = 2;

        MultiTransformation transformation = new MultiTransformation<>(
                new CenterCrop(),
                new BlurTransformation(blurRadius, blurSampling)
        );

        Glide.with(this)
                .load("https://i.ytimg.com/vi/sAfxBXAQCZM/maxresdefault.jpg")
                .apply(RequestOptions.bitmapTransform(transformation))
                .into(backgroundImage);

        backgroundImage.setColorFilter(new PorterDuffColorFilter(0x66000000, PorterDuff.Mode.SRC_OVER));

        float radius = getResources().getDisplayMetrics().density * 16;
        loginContainer.setClipToOutline(true);
        loginContainer.setElevation(24f);
        loginContainer.setPadding(32, 32, 32, 32);
        loginContainer.setBackground(getRoundedBackground(Color.parseColor("#121212"), radius));
    }

    private Drawable getRoundedBackground(int color, float radius) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void loginUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Unesite email i lozinku", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Korisnik ne postoji", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);

                    String storedHash = doc.getString("password");

                    if (storedHash == null || !PasswordHasher.verifyPassword(password, storedHash)) {
                        Toast.makeText(LoginActivity.this, "Pogrešan email ili lozinka", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(LoginActivity.this, "Uspješna prijava!", Toast.LENGTH_SHORT).show();

                    User loggedInUser = new User(doc.getId(), doc.getString("firstName"), doc.getString("lastName"), doc.getString("email"), doc.getString("birthday"), doc.getString("password"));
                    Intent intent = new Intent(LoginActivity.this, MoviesActivity.class);
                    intent.putExtra("user", loggedInUser);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Greška pri prijavi", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Login error", e);
                });
    }

}