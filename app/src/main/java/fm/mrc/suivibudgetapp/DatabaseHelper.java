package fm.mrc.suivibudgetapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    // Database Info
    private static final String DATABASE_NAME = "SuiviBudgetDatabase";
    private static final int DATABASE_VERSION = 2; // Version augmentée pour receipt_uri

    // Table Names
    private static final String TABLE_TRANSACTIONS = "transactions";

    // Transaction Table Columns
    public static final String KEY_TRANS_ID = "_id";
    public static final String KEY_TRANS_AMOUNT = "amount";
    public static final String KEY_TRANS_DESCRIPTION = "description";
    public static final String KEY_TRANS_TYPE = "type";
    public static final String KEY_TRANS_CATEGORY = "category";
    public static final String KEY_TRANS_DATE = "date";
    public static final String KEY_TRANS_RECEIPT_URI = "receipt_uri";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TRANSACTIONS_TABLE =
                "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                        + KEY_TRANS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + KEY_TRANS_AMOUNT + " REAL NOT NULL,"
                        + KEY_TRANS_DESCRIPTION + " TEXT NOT NULL,"
                        + KEY_TRANS_TYPE + " TEXT NOT NULL,"
                        + KEY_TRANS_CATEGORY + " TEXT NOT NULL,"
                        + KEY_TRANS_DATE + " INTEGER NOT NULL,"
                        + KEY_TRANS_RECEIPT_URI + " TEXT"
                        + ")";
        db.execSQL(CREATE_TRANSACTIONS_TABLE);
        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migration pour ajouter le champ pièce jointe
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + KEY_TRANS_RECEIPT_URI + " TEXT");
        }
    }

    // CRUD Operations
    public long insertTransaction(double amount, String description, String type, String category, long date, String receiptUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TRANS_AMOUNT, amount);
        values.put(KEY_TRANS_DESCRIPTION, description);
        values.put(KEY_TRANS_TYPE, type);
        values.put(KEY_TRANS_CATEGORY, category);
        values.put(KEY_TRANS_DATE, date);
        values.put(KEY_TRANS_RECEIPT_URI, receiptUri);
        return db.insert(TABLE_TRANSACTIONS, null, values);
    }

    public boolean updateTransaction(int id, double amount, String description, String type, String category, long date, String receiptUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TRANS_AMOUNT, amount);
        values.put(KEY_TRANS_DESCRIPTION, description);
        values.put(KEY_TRANS_TYPE, type);
        values.put(KEY_TRANS_CATEGORY, category);
        values.put(KEY_TRANS_DATE, date);
        values.put(KEY_TRANS_RECEIPT_URI, receiptUri);
        int rowsAffected = db.update(TABLE_TRANSACTIONS, values, KEY_TRANS_ID + " = ?", new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    public boolean deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TRANSACTIONS, KEY_TRANS_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // Query Methods
    public Cursor getAllTransactions() {
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + KEY_TRANS_DATE + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(query, null);
    }

    public Cursor getTransactionById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_TRANS_ID + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(id)});
    }

    public Cursor searchTransactions(String keyword) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS
                + " WHERE " + KEY_TRANS_DESCRIPTION + " LIKE ?"
                + " OR " + KEY_TRANS_CATEGORY + " LIKE ?"
                + " ORDER BY " + KEY_TRANS_DATE + " DESC";
        String[] selectionArgs = new String[]{"%" + keyword + "%", "%" + keyword + "%"};
        return db.rawQuery(query, selectionArgs);
    }

    public Cursor getFilteredTransactions(String type, String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM ").append(TABLE_TRANSACTIONS);
        boolean hasWhereClause = false;
        if (type != null && !type.isEmpty()) {
            queryBuilder.append(" WHERE ").append(KEY_TRANS_TYPE).append(" = ?");
            hasWhereClause = true;
        }
        if (category != null && !category.isEmpty()) {
            if (hasWhereClause) {
                queryBuilder.append(" AND ");
            } else {
                queryBuilder.append(" WHERE ");
            }
            queryBuilder.append(KEY_TRANS_CATEGORY).append(" = ?");
        }
        queryBuilder.append(" ORDER BY ").append(KEY_TRANS_DATE).append(" DESC");
        String[] selectionArgs;
        if (type != null && !type.isEmpty() && category != null && !category.isEmpty()) {
            selectionArgs = new String[]{type, category};
        } else if (type != null && !type.isEmpty()) {
            selectionArgs = new String[]{type};
        } else if (category != null && !category.isEmpty()) {
            selectionArgs = new String[]{category};
        } else {
            selectionArgs = null;
        }
        return db.rawQuery(queryBuilder.toString(), selectionArgs);
    }

    public double getTotalByType(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + KEY_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS
                + " WHERE " + KEY_TRANS_TYPE + " = ?";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{type});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total for type: " + type, e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return total;
    }
}