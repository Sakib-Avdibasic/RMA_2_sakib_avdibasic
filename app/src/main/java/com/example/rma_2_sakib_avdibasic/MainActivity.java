package com.example.rma_2_sakib_avdibasic;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView title = findViewById(R.id.appTitle);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView divider = findViewById(R.id.tvOr);
        Typeface montserrat = ResourcesCompat.getFont(this, R.font.montserrat);
        btnLogin.setTypeface(montserrat, Typeface.BOLD);
        btnRegister.setTypeface(montserrat, Typeface.BOLD);
        title.setAlpha(0f);
        divider.setAlpha(0f);
        btnLogin.setAlpha(0f);
        btnRegister.setAlpha(0f);

        new Handler().post(() -> {
            title.animate().alpha(1f).setDuration(500).start();
        });

        new Handler().postDelayed(() -> {
            btnLogin.animate().alpha(1f).setDuration(500).start();
            btnRegister.animate().alpha(1f).setDuration(500).start();
        }, 500);

        float dp = 25f;
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());

        ImageView imageView1 = findViewById(R.id.imageView);
        ImageView imageView2 = findViewById(R.id.imageView2);

        imageView1.setTranslationX(px + 300f);
        imageView1.setTranslationY(px + 300f);

        imageView2.setTranslationX(-px - 300f);
        imageView2.setTranslationY(px + 300f);

        new Handler().postDelayed(() -> {
            divider.animate().alpha(1f).setDuration(500).start();
            imageView1.animate()
                    .translationX(px)
                    .translationY(px)
                    .setDuration(500)
                    .start();

            imageView2.animate()
                    .translationX(-px)
                    .translationY(px)
                    .setDuration(500)
                    .start();
        }, 1000);

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}