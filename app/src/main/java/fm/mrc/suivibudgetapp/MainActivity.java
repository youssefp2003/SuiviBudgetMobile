package fm.mrc.suivibudgetapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int ADD_TRANSACTION_REQUEST = 1;

    private TextView soldeText, revenusText, depensesText;
    private Button btnAdd, btnViewAll;
    private DatabaseHelper db;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soldeText = findViewById(R.id.soldeText);
        revenusText = findViewById(R.id.revenusText);
        depensesText = findViewById(R.id.depensesText);
        btnAdd = findViewById(R.id.btnAddTransaction);
        btnViewAll = findViewById(R.id.btnViewTransactions);

        db = new DatabaseHelper(this);
        currencyFormat.setCurrency(java.util.Currency.getInstance("MAD"));

        updateFinancialInfo();

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST);
        });

        btnViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TransactionListActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFinancialInfo(); // Met à jour les infos chaque fois que l'activité reprend
    }

    @SuppressLint("SetTextI18n")
    public void updateFinancialInfo() {
        try {
            double revenus = db.getTotalByType("Revenu");
            double depenses = db.getTotalByType("Dépense");
            double solde = revenus - depenses;

            String formattedSolde = String.format(Locale.getDefault(), "%.2f MAD", solde);
            String formattedRevenus = String.format(Locale.getDefault(), "%.2f MAD", revenus);
            String formattedDepenses = String.format(Locale.getDefault(), "%.2f MAD", depenses);

            soldeText.setText("Solde actuel : " + formattedSolde);
            revenusText.setText("Revenus totaux : " + formattedRevenus);
            depensesText.setText("Dépenses totales : " + formattedDepenses);

            Log.d(TAG, "Financial info updated - Solde: " + solde + ", Revenus: " + revenus + ", Dépenses: " + depenses);
        } catch (Exception e) {
            Log.e(TAG, "Error updating financial information", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_TRANSACTION_REQUEST && resultCode == RESULT_OK) {
            updateFinancialInfo(); // Met à jour les informations après ajout d'une transaction
        }
    }
}