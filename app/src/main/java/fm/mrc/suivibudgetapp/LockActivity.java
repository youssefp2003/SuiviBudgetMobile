package fm.mrc.suivibudgetapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LockActivity extends AppCompatActivity {
    private EditText editPin;
    private Button btnUnlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        editPin = findViewById(R.id.editPin);
        btnUnlock = findViewById(R.id.btnUnlock);

        btnUnlock.setOnClickListener(v -> {
            String enteredPin = editPin.getText().toString();
            SharedPreferences prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            String savedPin = prefs.getString("pin_code", null);
            if (savedPin != null && savedPin.equals(enteredPin)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Code PIN incorrect", Toast.LENGTH_SHORT).show();
            }
        });
    }
}