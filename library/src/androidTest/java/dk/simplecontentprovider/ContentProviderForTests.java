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
        addEntity(ContractForTests.Items.TABLE_NAME)
                .addColumn(ContractForTests.Items._ID, "INTEGER PRIMARY KEY")
                .addColumn(ContractForTests.Items.NAME, "TEXT")
                .addColumn(ContractForTests.Items.TYPE, "TEXT");

        // Add entities from the contract...
        addEntity(ContractForTests.UniqueValues.TABLE_NAME)
                .addColumn(ContractForTests.UniqueValues._ID, "INTEGER PRIMARY KEY AUTOINCREMENT")
                .addColumn(ContractForTests.UniqueValues.KEY, "TEXT")
                .addColumn(ContractForTests.UniqueValues.VALUE, "INTEGER")
                .addConstraint("UNIQUE (" + ContractForTests.UniqueValues.KEY + ") ON CONFLICT REPLACE");

        // Optionally add one or more views...
        addView(ContractForTests.View.VIEW_NAME, createQueryBuilderForView())
                .onEntity(ContractForTests.UniqueValues.TABLE_NAME);
    }

    private SQLiteQueryBuilder createQueryBuilderForView() {
        // Create a query builder for joining an owner with a summary of the owners pets...
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        String table = "(SELECT " +
                            "COUNT(" + ContractForTests.UniqueValues.KEY + ") AS " + ContractForTests.View.NUMBER_OF_KEYS + "," +
                            "MIN(" + ContractForTests.UniqueValues.VALUE + ") AS " + ContractForTests.View.MIN_VALUE + "," +
                            "MAX(" + ContractForTests.UniqueValues.VALUE + ") AS " + ContractForTests.View.MAX_VALUE + " " +
                       "FROM " + ContractForTests.UniqueValues.TABLE_NAME + ")";
        builder.setTables(table);

        return builder;
    }
}
