package fm.mrc.suivibudgetapp;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import java.util.*;

public class AddTransactionActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1001;

    private EditText editAmount, editDescription, editDate;
    private AutoCompleteTextView editCategory;
    private RadioGroup radioGroupType;
    private RadioButton radioRevenu, radioDepense;
    private Button btnSave, btnAttachReceipt;
    private ImageView imageReceiptPreview;

    private DatabaseHelper db;
    private Calendar selectedDate;
    private Uri receiptUri = null;

    private final Set<String> suggestedRevenueCategories = new HashSet<>(Arrays.asList(
            "Salaire", "Prime", "Vente", "Cadeau", "Investissement", "Remboursement"));
    private final Set<String> suggestedExpenseCategories = new HashSet<>(Arrays.asList(
            "Alimentation", "Transport", "Logement", "Loisirs", "Santé", "Éducation", "Vêtements", "Restaurant", "Factures", "Autres"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialisation des vues
        editAmount = findViewById(R.id.editAmount);
        editDescription = findViewById(R.id.editDescription);
        editCategory = findViewById(R.id.editCategory);
        editDate = findViewById(R.id.editDate);
        radioGroupType = findViewById(R.id.radioGroupType);
        radioRevenu = findViewById(R.id.radioRevenu);
        radioDepense = findViewById(R.id.radioDepense);
        btnSave = findViewById(R.id.btnSave);
        btnAttachReceipt = findViewById(R.id.btnAttachReceipt);
        imageReceiptPreview = findViewById(R.id.imageReceiptPreview);

        db = new DatabaseHelper(this);

        radioDepense.setChecked(true);
        showCategorySuggestions(false);

        selectedDate = Calendar.getInstance();
        updateDateField();

        editDate.setOnClickListener(v -> showDatePickerDialog());
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            editCategory.setText("");
            showCategorySuggestions(checkedId == R.id.radioRevenu);
        });

        btnAttachReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnSave.setOnClickListener(v -> saveTransaction());

        editAmount.addTextChangedListener(createTextWatcher());
        editDescription.addTextChangedListener(createTextWatcher());
        editCategory.addTextChangedListener(createTextWatcher());

        validateForm();
    }

    private void showCategorySuggestions(boolean isRevenue) {
        Set<String> categories = isRevenue ? suggestedRevenueCategories : suggestedExpenseCategories;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(categories));
        editCategory.setAdapter(adapter);
        editCategory.setThreshold(1);
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

    private void saveTransaction() {
        String amountStr = editAmount.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String dateStr = editDate.getText().toString().trim();

        if (amountStr.isEmpty() || description.isEmpty() || category.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            String type = radioRevenu.isChecked() ? "Revenu" : "Dépense";
            long dateMillis = selectedDate.getTimeInMillis();
            String receiptUriStr = (receiptUri != null) ? receiptUri.toString() : null;

            long result = db.insertTransaction(amount, description, type, category, dateMillis, receiptUriStr);
            if (result != -1) {
                Snackbar.make(findViewById(android.R.id.content), "Transaction ajoutée avec succès", Snackbar.LENGTH_LONG)
                        .setAction("OK", v -> finish()).show();
                clearFields();
                findViewById(android.R.id.content).postDelayed(this::finish, 1500);
            } else {
                Toast.makeText(this, "Erreur lors de l'ajout de la transaction", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            editAmount.setError("Format de montant invalide");
            editAmount.requestFocus();
        }
    }

    private void clearFields() {
        editAmount.setText("");
        editDescription.setText("");
        editCategory.setText("");
        radioDepense.setChecked(true);
        selectedDate = Calendar.getInstance();
        updateDateField();
        showCategorySuggestions(false);
        receiptUri = null;
        imageReceiptPreview.setImageResource(R.drawable.ic_other); // Met une icône par défaut
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
        btnSave.setEnabled(isValid);
    }
}