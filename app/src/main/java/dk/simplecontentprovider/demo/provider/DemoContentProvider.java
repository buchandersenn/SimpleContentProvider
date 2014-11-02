package dk.simplecontentprovider.demo.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Build;

import java.util.List;

import dk.simplecontentprovider.SimpleContentProvider;
import dk.simplecontentprovider.SimpleDatabaseHelper;

public class DemoContentProvider extends SimpleContentProvider {
    public DemoContentProvider() {
    }

    @Override
    public void initProvider() {
        // Set authority...
        setAuthority(DemoContract.AUTHORITY);

        // Set database name and version...
        setDatabaseName("DemoContentProvider.db");
        setDatabaseVersion(1);

        // Add entities from the contract...
        addEntity(DemoContract.Owners.TABLE_NAME)
            .addColumn(DemoContract.Owners._ID, "INTEGER PRIMARY KEY AUTOINCREMENT")
            .addColumn(DemoContract.Owners.NAME, "TEXT")
            .addColumn(DemoContract.Owners.ADDRESS, "TEXT");

        addEntity(DemoContract.Pets.TABLE_NAME)
                .addColumn(DemoContract.Pets._ID, "INTEGER PRIMARY KEY AUTOINCREMENT")
                .addColumn(DemoContract.Pets.NAME, "TEXT")
                .addColumn(DemoContract.Pets.TYPE, "TEXT")
                .addColumn(DemoContract.Pets.AGE, "INTEGER")
                .addColumn(DemoContract.Pets.OWNER_ID, "INTEGER")

                // Optionally add some constraints...
                .addConstraint("FOREIGN KEY (" + DemoContract.Pets.OWNER_ID + ") " +
                                "REFERENCES " + DemoContract.Owners.TABLE_NAME + " (" + DemoContract.Owners._ID + ") ON DELETE CASCADE")
                .addConstraint("UNIQUE (" + DemoContract.Pets.NAME + ", " + DemoContract.Pets.OWNER_ID + ") ON CONFLICT REPLACE");

        // Optionally add one or more views...
        addView(DemoContract.OwnersAndPetsView.VIEW_NAME, createQueryBuilderForOwnersAndPets())
            .onEntity(DemoContract.Owners.TABLE_NAME)
            .onEntity(DemoContract.Pets.TABLE_NAME);

        // Use a custom SQLiteOpenHelper to enable foreign key constraints.
        // You could use a similar technique to provide onUpgrade behaviour
        // to the database, if necessary...
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            setDatabaseHelper(new LegacyDatabaseHelper(getContext(), mDatabaseName, mDatabaseVersion, mEntities));
        } else {
            setDatabaseHelper(new DemoDatabaseHelper(getContext(), mDatabaseName, mDatabaseVersion, mEntities));
        }
    }

    private SQLiteQueryBuilder createQueryBuilderForOwnersAndPets() {
        // Create a query builder for joining an owner with a summary of the owners pets...
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        String table = "(SELECT " +
                            DemoContract.Owners.TABLE_NAME + "." + DemoContract.Owners._ID + " AS " + DemoContract.OwnersAndPetsView._ID + "," +
                            DemoContract.Owners.TABLE_NAME + "." + DemoContract.Owners.NAME + " AS " + DemoContract.OwnersAndPetsView.NAME + "," +
                            DemoContract.Owners.TABLE_NAME + "." + DemoContract.Owners.ADDRESS + " AS " + DemoContract.OwnersAndPetsView.ADDRESS + "," +
                            "count(" + DemoContract.Pets.TABLE_NAME + "." + DemoContract.Pets._ID + ") AS " + DemoContract.OwnersAndPetsView.NUMBER_OF_PETS + "," +
                            "group_concat(" + DemoContract.Pets.TABLE_NAME + "." + DemoContract.Pets.NAME + ",', ')" + " AS " + DemoContract.OwnersAndPetsView.PET_NAMES + " " +
                        "FROM " + DemoContract.Owners.TABLE_NAME + " " +
                            "LEFT JOIN " + DemoContract.Pets.TABLE_NAME + " ON (" + DemoContract.Owners.TABLE_NAME + "." + DemoContract.Owners._ID + "=" + DemoContract.Pets.TABLE_NAME + "." + DemoContract.Pets.OWNER_ID + ") " +
                        "GROUP BY " + DemoContract.OwnersAndPetsView._ID + ") AS t";
        builder.setTables(table);

        return builder;
    }

    public static class LegacyDatabaseHelper extends SimpleDatabaseHelper {
        public LegacyDatabaseHelper(Context context, String databaseName, int databaseVersion, List<Entity> entities) {
            super(context, databaseName, databaseVersion, entities);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            if (!db.isReadOnly()) {
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON;");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static class DemoDatabaseHelper extends SimpleDatabaseHelper {
        public DemoDatabaseHelper(Context context, String databaseName, int databaseVersion, List<Entity> entities) {
            super(context, databaseName, databaseVersion, entities);
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            db.setForeignKeyConstraintsEnabled(true);
        }
    }
}
