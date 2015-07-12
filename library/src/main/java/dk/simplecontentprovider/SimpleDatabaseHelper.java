package dk.simplecontentprovider;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class SimpleDatabaseHelper extends SQLiteOpenHelper {
    private final SimpleContentProvider simpleContentProvider;

    public SimpleDatabaseHelper(SimpleContentProvider simpleContentProvider, String databaseName, int databaseVersion) {
        super(simpleContentProvider.getContext(), databaseName, null, databaseVersion);
        this.simpleContentProvider = simpleContentProvider;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        simpleContentProvider.onCreateDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        simpleContentProvider.onUpgradeDatabase(db, oldVersion, newVersion);
    }

    public static class ForeignKeyConstraintDatabaseHelper extends SimpleDatabaseHelper {
        public ForeignKeyConstraintDatabaseHelper(SimpleContentProvider simpleContentProvider, String databaseName, int databaseVersion) {
            super(simpleContentProvider, databaseName, databaseVersion);
        }

        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                db.setForeignKeyConstraintsEnabled(true);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            if (!db.isReadOnly() && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON;");
            }
        }
    }
}
