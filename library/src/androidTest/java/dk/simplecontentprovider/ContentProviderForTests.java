package dk.simplecontentprovider;

import android.database.sqlite.SQLiteQueryBuilder;

public class ContentProviderForTests extends SimpleContentProvider {
    @Override
    protected void initProvider() {
        // Set authority...
        setAuthority(ContractForTests.AUTHORITY);

        // Set database name and version...
        setDatabaseName("ContentProviderForTests.db");
        setDatabaseVersion(1);

        // Add entities from the contract...
        addEntity(ContractForTests.Values.TABLE_NAME)
                .addColumn(ContractForTests.Values._ID, "INTEGER PRIMARY KEY AUTOINCREMENT")
                .addColumn(ContractForTests.Values.KEY, "TEXT")
                .addColumn(ContractForTests.Values.VALUE, "INTEGER")
                .addConstraint("UNIQUE (" + ContractForTests.Values.KEY + ") ON CONFLICT REPLACE");

        // Optionally add one or more views...
        addView(ContractForTests.View.VIEW_NAME, createQueryBuilderForView())
                .onEntity(ContractForTests.Values.TABLE_NAME);
    }

    private SQLiteQueryBuilder createQueryBuilderForView() {
        // Create a query builder for joining an owner with a summary of the owners pets...
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        String table = "(SELECT " +
                            "COUNT(" + ContractForTests.Values.KEY + ") AS " + ContractForTests.View.NUMBER_OF_KEYS + "," +
                            "MIN(" + ContractForTests.Values.VALUE + ") AS " + ContractForTests.View.MIN_VALUE + "," +
                            "MAX(" + ContractForTests.Values.VALUE + ") AS " + ContractForTests.View.MAX_VALUE + " " +
                       "FROM " + ContractForTests.Values.TABLE_NAME + ")";
        builder.setTables(table);

        return builder;
    }
}
