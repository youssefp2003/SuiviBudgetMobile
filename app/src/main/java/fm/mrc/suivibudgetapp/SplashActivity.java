package fm.mrc.suivibudgetapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String pin = prefs.getString("pin_code", null);

        if (pin == null) {
            startActivity(new Intent(this, PinSetupActivity.class));
        } else {
            startActivity(new Intent(this, LockActivity.class));
        }
        finish();
    }
}