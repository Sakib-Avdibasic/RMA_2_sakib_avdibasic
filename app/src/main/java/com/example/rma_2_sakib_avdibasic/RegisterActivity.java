package com.example.rma_2_sakib_avdibasic;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import jp.wasabeef.glide.transformations.BlurTransformation;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputName, inputLastName, inputEmail, inputBirthday, inputPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView backgroundImage = findViewById(R.id.backgroundImage);
        View registerContainer = findViewById(R.id.registerContainer);

        int blurRadius = 10;
        int blurSampling = 2;

        MultiTransformation transformation = new MultiTransformation<>(
                new CenterCrop(),
                new BlurTransformation(blurRadius, blurSampling)
        );

        Glide.with(this)
                .load("https://i.pinimg.com/736x/5c/e8/1f/5ce81f4d380d78b7f1758c0e3b5ed8e5.jpg")
                .apply(RequestOptions.bitmapTransform(transformation))
                .into(backgroundImage);

        backgroundImage.setColorFilter(new PorterDuffColorFilter(0x66000000, PorterDuff.Mode.SRC_OVER));

        float radius = getResources().getDisplayMetrics().density * 16;
        registerContainer.setClipToOutline(true);
        registerContainer.setElevation(24f);
        registerContainer.setPadding(32, 32, 32, 32);
        registerContainer.setBackground(getRoundedBackground(Color.parseColor("#121212"), radius));

        inputName = findViewById(R.id.inputName);
        inputLastName = findViewById(R.id.inputLastName);
        inputEmail = findViewById(R.id.inputEmail);
        inputBirthday = findViewById(R.id.inputBirthday);
        inputPassword = findViewById(R.id.inputPassword);

        inputBirthday.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        inputBirthday.setText(selectedDate);
                    },
                    year, month, day);

            Calendar minDate = Calendar.getInstance();
            minDate.set(1900, Calendar.JANUARY, 1);
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.YEAR, -14);

            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
            datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

            datePickerDialog.show();
        });

        findViewById(R.id.btnSubmit).setOnClickListener(v -> registerUser());
    }

    private Drawable getRoundedBackground(int color, float radius) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }
    private void registerUser() {
        String firstName = inputName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String birthday = inputBirthday.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(birthday) || TextUtils.isEmpty(password)) {
            Toast.makeText(RegisterActivity.this, "Sva polja moraju biti popunjena", Toast.LENGTH_SHORT).show();
            return;
        }
        String hashedPassword = PasswordHasher.hashPassword(password);

        User newUser = new User(firstName, lastName, email, birthday, hashedPassword);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .add(newUser)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(RegisterActivity.this, "Uspješna registracija!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    Log.d("Firestore", "Korisnik dodat s ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Greška pri registraciji", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Greška pri dodavanju korisnika", e);
                });
    }

}