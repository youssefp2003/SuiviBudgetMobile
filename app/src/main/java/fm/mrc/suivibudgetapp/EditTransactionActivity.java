package fm.mrc.suivibudgetapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTransactionActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1002;

    private EditText editAmount, editDescription, editCategory, editDate;
    private RadioGroup radioGroupType;
    private RadioButton radioRevenu, radioDepense;
    private Button btnUpdate, btnCancel, btnAttachReceipt;
    private ImageView imageReceiptPreview;

    private DatabaseHelper db;
    private int transactionId;
    private Calendar selectedDate;
    private Uri receiptUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);

        // Récupérer l'ID de la transaction à éditer
        transactionId = getIntent().getIntExtra("TRANSACTION_ID", -1);
        if (transactionId == -1) {
            Toast.makeText(this, "Erreur: Impossible d'éditer cette transaction", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialisation des vues
        editAmount = findViewById(R.id.editAmount);
        editDescription = findViewById(R.id.editDescription);
        editCategory = findViewById(R.id.editCategory);
        editDate = findViewById(R.id.editDate);
        radioGroupType = findViewById(R.id.radioGroupType);
        radioRevenu = findViewById(R.id.radioRevenu);
        radioDepense = findViewById(R.id.radioDepense);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnCancel = findViewById(R.id.btnCancel);
        btnAttachReceipt = findViewById(R.id.btnAttachReceipt);
        imageReceiptPreview = findViewById(R.id.imageReceiptPreview);

        db = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();

        loadTransactionData();

        editDate.setOnClickListener(v -> showDatePickerDialog());

        btnAttachReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnUpdate.setOnClickListener(v -> updateTransaction());
        btnCancel.setOnClickListener(v -> finish());

        editAmount.addTextChangedListener(createTextWatcher());
        editDescription.addTextChangedListener(createTextWatcher());
        editCategory.addTextChangedListener(createTextWatcher());
        editDate.addTextChangedListener(createTextWatcher());

        validateForm();
    }

    private void loadTransactionData() {
        try (Cursor cursor = db.getTransactionById(transactionId)) {
            if (cursor != null && cursor.moveToFirst()) {
                double amount = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_AMOUNT));
                String description = cursor.getString(cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_DESCRIPTION));
                String type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_TYPE));
                String category = cursor.getString(cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_CATEGORY));
                long dateMillis = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_DATE));
                String receiptUriString = null;
                int receiptUriCol = cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_RECEIPT_URI);
                if (receiptUriCol != -1) {
                    receiptUriString = cursor.getString(receiptUriCol);
                }

                editAmount.setText(String.valueOf(amount));
                editDescription.setText(description);
                editCategory.setText(category);

                // Type
                if ("Revenu".equals(type)) {
                    radioRevenu.setChecked(true);
                } else {
                    radioDepense.setChecked(true);
                }

                // Date
                selectedDate.setTimeInMillis(dateMillis);
                updateDateField();

                // Pièce jointe
                if (receiptUriString != null) {
                    receiptUri = Uri.parse(receiptUriString);
                    imageReceiptPreview.setImageURI(receiptUri);
                } else {
                    imageReceiptPreview.setImageResource(R.drawable.ic_other);
                }
            } else {
                Toast.makeText(this, "Erreur: Transaction introuvable", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors du chargement des données", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showDatePickerDialog() {
        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH);
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year1);
            selectedDate.set(Calendar.MONTH, month1);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateField();
        }, year, month, day);
        dialog.show();
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        editDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void updateTransaction() {
        String amountStr = editAmount.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String dateStr = editDate.getText().toString().trim();
        String type = radioRevenu.isChecked() ? "Revenu" : "Dépense";
        long dateMillis = selectedDate.getTimeInMillis();
        String receiptUriString = (receiptUri != null) ? receiptUri.toString() : null;

        if (amountStr.isEmpty() || description.isEmpty() || category.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            boolean success = db.updateTransaction(transactionId, amount, description, type, category, dateMillis, receiptUriString);
            if (success) {
                Snackbar.make(findViewById(android.R.id.content), "Transaction mise à jour avec succès", Snackbar.LENGTH_LONG)
                        .setAction("OK", v -> finish()).show();
                findViewById(android.R.id.content).postDelayed(this::finish, 1200);
            } else {
                Toast.makeText(this, "Erreur lors de la mise à jour de la transaction", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            editAmount.setError("Format de montant invalide");
            editAmount.requestFocus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            receiptUri = data.getData();
            imageReceiptPreview.setImageURI(receiptUri);
        }
    }

    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateForm(); }
        };
    }

    private void validateForm() {
        boolean isValid = true;
        String amountStr = editAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            editAmount.setError("Montant requis");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    editAmount.setError("Le montant doit être supérieur à zéro");
                    isValid = false;
                } else {
                    editAmount.setError(null);
                }
            } catch (NumberFormatException e) {
                editAmount.setError("Montant invalide");
                isValid = false;
            }
        }
        String description = editDescription.getText().toString().trim();
        if (description.isEmpty()) {
            editDescription.setError("Description requise");
            isValid = false;
        } else {
            editDescription.setError(null);
        }
        String category = editCategory.getText().toString().trim();
        if (category.isEmpty()) {
            editCategory.setError("Catégorie requise");
            isValid = false;
        } else {
            editCategory.setError(null);
        }
        String date = editDate.getText().toString().trim();
        if (date.isEmpty()) {
            editDate.setError("Date requise");
            isValid = false;
        } else {
            editDate.setError(null);
        }
        btnUpdate.setEnabled(isValid);
    }
}