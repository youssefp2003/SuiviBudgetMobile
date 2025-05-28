package fm.mrc.suivibudgetapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PinSetupActivity extends AppCompatActivity {
    private EditText editPin, editPinConfirm;
    private Button btnSetPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        editPin = findViewById(R.id.editPin);
        editPinConfirm = findViewById(R.id.editPinConfirm);
        btnSetPin = findViewById(R.id.btnSetPin);

        btnSetPin.setOnClickListener(v -> {
            String pin = editPin.getText().toString();
            String pinConfirm = editPinConfirm.getText().toString();

            if (pin.length() < 4) {
                editPin.setError("Le code doit contenir au moins 4 chiffres");
            } else if (!pin.equals(pinConfirm)) {
                editPinConfirm.setError("Les codes ne correspondent pas");
            } else {
                SharedPreferences prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                prefs.edit().putString("pin_code", pin).apply();
                Toast.makeText(this, "Code PIN défini", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}