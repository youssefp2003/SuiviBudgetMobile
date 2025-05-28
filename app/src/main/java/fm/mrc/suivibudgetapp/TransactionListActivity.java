package fm.mrc.suivibudgetapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TransactionListActivity extends AppCompatActivity {
    private static final String TAG = "TransactionListActivity";
    private ListView transactionListView;
    private DatabaseHelper db;
    private SimpleCursorAdapter adapter;
    private Spinner typeFilterSpinner;
    private Spinner categoryFilterSpinner;
    private SearchView searchView;
    private String currentTypeFilter = null;
    private String currentCategoryFilter = null;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);
        db = new DatabaseHelper(this);
        initializeViews();

        try {
            setupFilterSpinners();
            loadTransactions();
        } catch (Exception e) {
            Log.e(TAG, "Error during onCreate", e);
            Toast.makeText(this, "Erreur lors du chargement des transactions", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        transactionListView = findViewById(R.id.transactionListView);
        typeFilterSpinner = findViewById(R.id.typeFilterSpinner);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);

        if (transactionListView == null) {
            Log.e(TAG, "ListView not found in layout!");
            Toast.makeText(this, "Erreur: ListView introuvable", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (typeFilterSpinner == null || categoryFilterSpinner == null) {
            Log.e(TAG, "Filter spinners not found in layout!");
            Toast.makeText(this, "Erreur: Filtres introuvables", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.transaction_list_menu, menu);
            setupSearchView(menu);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating menu", e);
        }
        return true;
    }

    private void setupSearchView(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        currentSearchQuery = query;
                        refreshTransactionList();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (newText.length() > 2 || newText.isEmpty()) {
                            currentSearchQuery = newText;
                            refreshTransactionList();
                        }
                        return true;
                    }
                });
            }
        }
    }

    private void setupFilterSpinners() {
        String[] typeOptions = new String[]{"Tous", "Revenu", "Dépense"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeOptions);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (typeFilterSpinner != null) {
            typeFilterSpinner.setAdapter(typeAdapter);
        }

        updateCategorySpinner();

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (parent == typeFilterSpinner) {
                        String selected = (String) parent.getItemAtPosition(position);
                        currentTypeFilter = "Tous".equals(selected) ? null : selected;
                    } else if (parent == categoryFilterSpinner) {
                        String selected = (String) parent.getItemAtPosition(position);
                        currentCategoryFilter = "Toutes".equals(selected) ? null : selected;
                    }
                    refreshTransactionList();
                } catch (Exception e) {
                    Log.e(TAG, "Error applying filters", e);
                    Toast.makeText(TransactionListActivity.this, "Erreur lors de l'application des filtres", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        };

        if (typeFilterSpinner != null) {
            typeFilterSpinner.setOnItemSelectedListener(filterListener);
        }
        if (categoryFilterSpinner != null) {
            categoryFilterSpinner.setOnItemSelectedListener(filterListener);
        }
    }

    private void updateCategorySpinner() {
        if (categoryFilterSpinner == null) return;

        Set<String> categories = new HashSet<>();
        categories.add("Toutes");

        try (Cursor cursor = db.getAllTransactions()) {
            if (cursor != null && cursor.moveToFirst()) {
                int categoryColIdx = cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_CATEGORY);
                if (categoryColIdx != -1) {
                    do {
                        String category = cursor.getString(categoryColIdx);
                        if (category != null && !category.isEmpty()) {
                            categories.add(category);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching categories", e);
        }

        ArrayList<String> categoryList = new ArrayList<>(categories);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);
    }

    private void loadTransactions() {
        try {
            Cursor cursor = db.getAllTransactions();
            if (cursor == null) {
                Log.e(TAG, "Cursor is null when retrieving transactions");
                Toast.makeText(this, "Erreur: Impossible de récupérer les transactions", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cursor.getCount() == 0) {
                Log.d(TAG, "No transactions found in database");
                Toast.makeText(this, "Aucune transaction trouvée", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Found " + cursor.getCount() + " transactions");
            }

            String[] fromColumns = {
                    DatabaseHelper.KEY_TRANS_DESCRIPTION,
                    DatabaseHelper.KEY_TRANS_CATEGORY,
                    DatabaseHelper.KEY_TRANS_AMOUNT,
                    DatabaseHelper.KEY_TRANS_DATE
            };

            int[] toViews = {
                    R.id.textDescription,
                    R.id.textCategory,
                    R.id.textAmount,
                    R.id.textDate
            };

            adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.list_item_transaction,
                    cursor,
                    fromColumns,
                    toViews,
                    0
            ) {
                @Override
                public View getView(int position, View convertView, android.view.ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    Cursor cursor = (Cursor) getItem(position);
                    @SuppressLint("Range") final int transactionId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_ID));
                    setupRowButtons(view, transactionId);
                    return view;
                }
            };

            adapter.setViewBinder(new TransactionViewBinder());
            transactionListView.setAdapter(adapter);
            refreshTransactionList();
        } catch (Exception e) {
            Log.e(TAG, "Error loading transactions", e);
            Toast.makeText(this, "Erreur lors du chargement des transactions: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRowButtons(View view, final int transactionId) {
        ImageButton btnEdit = view.findViewById(R.id.btnEdit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(TransactionListActivity.this, EditTransactionActivity.class);
                intent.putExtra("TRANSACTION_ID", transactionId);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "btnEdit not found in list item layout");
        }

        ImageButton btnDelete = view.findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteConfirmation(transactionId));
        } else {
            Log.e(TAG, "btnDelete not found in list item layout");
        }
    }

    private void showDeleteConfirmation(final int transactionId) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la transaction")
                .setMessage("Êtes-vous sûr de vouloir supprimer cette transaction ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    if (db.deleteTransaction(transactionId)) {
                        Toast.makeText(TransactionListActivity.this, "Transaction supprimée avec succès", Toast.LENGTH_SHORT).show();
                        refreshTransactionList();
                    } else {
                        Toast.makeText(TransactionListActivity.this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void refreshTransactionList() {
        if (adapter == null) return;

        Cursor newCursor = null;
        try {
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                newCursor = db.searchTransactions(currentSearchQuery);
            } else {
                newCursor = db.getFilteredTransactions(currentTypeFilter, currentCategoryFilter);
            }

            if (newCursor != null) {
                Cursor oldCursor = adapter.swapCursor(newCursor);
                if (oldCursor != null && oldCursor != newCursor) {
                    oldCursor.close();
                }

                if (newCursor.getCount() == 0) {
                    if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                        Toast.makeText(this, "Aucune transaction ne correspond à la recherche", Toast.LENGTH_SHORT).show();
                    } else if (currentTypeFilter != null || currentCategoryFilter != null) {
                        Toast.makeText(this, "Aucune transaction ne correspond aux filtres", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.e(TAG, "Cursor is null after filtering");
                Toast.makeText(this, "Erreur lors du filtrage des transactions", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing transaction list", e);
            Toast.makeText(this, "Erreur lors du rafraîchissement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (newCursor != null && !newCursor.isClosed()) {
                newCursor.close();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateCategorySpinner();
            refreshTransactionList();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onDestroy() {
        if (adapter != null && adapter.getCursor() != null && !adapter.getCursor().isClosed()) {
            adapter.getCursor().close();
        }
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    private class TransactionViewBinder implements SimpleCursorAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            try {
                String columnName = cursor.getColumnName(columnIndex);
                if (DatabaseHelper.KEY_TRANS_AMOUNT.equals(columnName)) {
                    return formatAmount(view, cursor, columnIndex);
                } else if (DatabaseHelper.KEY_TRANS_DATE.equals(columnName)) {
                    return formatDate(view, cursor, columnIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in ViewBinder", e);
            }
            return false;
        }

        private boolean formatAmount(View view, Cursor cursor, int columnIndex) {
            double amount = cursor.getDouble(columnIndex);
            int typeColIdx = cursor.getColumnIndex(DatabaseHelper.KEY_TRANS_TYPE);
            String type = "Dépense";
            if (typeColIdx != -1) {
                type = cursor.getString(typeColIdx);
            }

            TextView amountView = (TextView) view;
            String formattedAmount;
            int colorRes;

            if ("Revenu".equals(type)) {
                formattedAmount = String.format(Locale.getDefault(), "+%.2f MAD", amount);
                colorRes = android.R.color.holo_green_dark;
            } else {
                formattedAmount = String.format(Locale.getDefault(), "-%.2f MAD", amount);
                colorRes = android.R.color.holo_red_dark;
            }

            amountView.setText(formattedAmount);
            amountView.setTextColor(getResources().getColor(colorRes));
            return true;
        }

        private boolean formatDate(View view, Cursor cursor, int columnIndex) {
            long dateMillis = cursor.getLong(columnIndex);
            TextView dateView = (TextView) view;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(dateMillis));
            dateView.setText(formattedDate);
            return true;
        }
    }
}