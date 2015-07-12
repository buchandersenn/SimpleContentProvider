package dk.simplecontentprovider;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestItemsTable extends AndroidTestCase {

    public void deleteDatabase() {
        mContext.getContentResolver().delete(
                ContractForTests.Items.CONTENT_URI,
                null,
                null
        );
    }

    @Override
    public void setUp() {
        // Since we want each test to start with a clean slate, run deleteDatabase
        // in setUp (called by the test runner before each test).
        deleteDatabase();
    }

    public void testGetType() {
        String valueType = ContractForTests.AUTHORITY + "." + ContractForTests.Items.TABLE_NAME;

        String dirType = mContext.getContentResolver().getType(ContractForTests.Items.CONTENT_URI);
        assertEquals(ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + valueType, dirType);

        Uri itemUri = ContentUris.withAppendedId(ContractForTests.Items.CONTENT_URI, 1);
        String itemType = mContext.getContentResolver().getType(itemUri);
        assertEquals(ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + valueType, itemType);
    }

    public void testInsertAndQuery() {
        ContentValues testValues = createValues("ball", "toy");

        Uri insertedUri = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues);
        long insertedRowId = ContentUris.parseId(insertedUri);

        // Verify that we got a row back...
        assertTrue(insertedRowId != -1);

        // Verify that we can read the inserted value...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        validateCursor(cursor, testValues);

        // Verify that we can read the inserted value when adding a where clause...
        cursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null,
                ContractForTests.Items.NAME + " = ?",
                new String[] {"ball"},
                null
        );
        validateCursor(cursor, testValues);

        // Verify that we can read the inserted value when using an Uri with the ID of the inserted row...
        Uri uri = ContentUris.withAppendedId(ContractForTests.Items.CONTENT_URI, insertedRowId);
        cursor = mContext.getContentResolver().query(
                uri,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        validateCursor(cursor, testValues);
    }

    public void testQueryWithLimit() {
        ContentValues testValues1 = createValues("ball", "toy");
        ContentValues testValues2 = createValues("hammer", "tool");
        ContentValues testValues3 = createValues("car", "vehicle");

        mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues1);
        mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues2);
        mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues3);

        Uri uri = ContractForTests.Items.CONTENT_URI.buildUpon()
                .appendQueryParameter(SimpleContentProvider.QUERY_PARAMETER_LIMIT, "2")
                .build();

        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        // Verify that the limit parameter was respected...
        assertEquals(2, cursor.getCount());

        cursor.close();
    }

    public void testQueryWithLimitAndOffset() {
        ContentValues testValues1 = createValues("ball", "toy");
        ContentValues testValues2 = createValues("hammer", "tool");
        ContentValues testValues3 = createValues("car", "vehicle");

        mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues1);
        mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues2);
        mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues3);

        Uri uri = ContractForTests.Items.CONTENT_URI.buildUpon()
                .appendQueryParameter(SimpleContentProvider.QUERY_PARAMETER_LIMIT, "2")
                .appendQueryParameter(SimpleContentProvider.QUERY_PARAMETER_OFFSET, "1")
                .build();

        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        // Verify that the limit parameter was respected
        // and that the offset resulted in the two last items
        // being returned...
        assertEquals(2, cursor.getCount());
        validateCursor(cursor, testValues2);
        validateCursor(cursor, testValues3);

        cursor.close();
    }

    public void testInsertConflict() {
        ContentValues testValues1 = createValues("ball", "toy");

        Uri insertedUri = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues1);
        long insertedRowId = ContentUris.parseId(insertedUri);


        ContentValues testValues2 = createValues("hammer", "tool");
        testValues2.put(ContractForTests.Items._ID, insertedRowId);

        // Attempt to insert conflicting item...
        Uri uri = ContractForTests.Items.CONTENT_URI;
        Uri insertedUri2 = mContext.getContentResolver().insert(uri, testValues2);

        // Verify that no item was inserted...
        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );
        assertNull(insertedUri2);
        assertEquals(1, cursor.getCount());

        cursor.close();
    }

    public void testInsertWithOnConflict_CONFLICT_REPLACE() {
        ContentValues testValues1 = createValues("ball", "toy");

        Uri insertedUri = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues1);
        long insertedRowId = ContentUris.parseId(insertedUri);


        ContentValues testValues2 = createValues("hammer", "tool");
        testValues2.put(ContractForTests.Items._ID, insertedRowId);

        // Attempt to insert conflicting item using SQLiteDatabase.CONFLICT_REPLACE...
        Uri uri = ContractForTests.Items.CONTENT_URI.buildUpon()
                .appendQueryParameter(SimpleContentProvider.PARAMETER_CONFLICT_ALGORITHM, "" + SQLiteDatabase.CONFLICT_REPLACE)
                .build();
        Uri insertedUri2 = mContext.getContentResolver().insert(uri, testValues2);

        // Verify that the old item was replaced...
        assertEquals(insertedUri, insertedUri2);

        // Verify that the number of items in the database remains the same
        // and that the values of the first item has indeed been replaced...
        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );
        assertEquals(1, cursor.getCount());
        validateCursor(cursor, testValues2);

        cursor.close();
    }

    public void testUpdate() {
        ContentValues testValues1 = createValues("ball", "toy");
        ContentValues testValues2 = createValues("hammer", "tool");

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);
        assertTrue(insertedRowId2 != -1);

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(ContractForTests.Items.NAME, "screwdriver");

        int count = mContext.getContentResolver().update(
                ContractForTests.Items.CONTENT_URI,
                updatedValues,
                ContractForTests.Items._ID + " = ?",
                new String[] {Long.toString(insertedRowId2)});

        // Verify that one record was affected...
        assertEquals(count, 1);

        // Verify that the value was updated...
        ContentValues expectedValues = new ContentValues(testValues2);
        expectedValues.putAll(updatedValues);
        Uri uri = ContentUris.withAppendedId(ContractForTests.Items.CONTENT_URI, insertedRowId2);
        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );
        validateCursor(cursor, expectedValues);
    }

    public void testDelete() {
        ContentValues testValues1 = createValues("ball", "toy");
        ContentValues testValues2 = createValues("hammer", "tool");

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Items.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);
        assertTrue(insertedRowId2 != -1);

        // Delete the second row...
        int count = mContext.getContentResolver().delete(
                ContractForTests.Items.CONTENT_URI,
                ContractForTests.Items._ID + " = ?",
                new String[] {Long.toString(insertedRowId2)});

        // Verify that one record was affected...
        assertEquals(count, 1);

        // Verity that the correct row was deleted...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(1, cursor.getCount());
        validateCursor(cursor, testValues1);
    }

    public void testBulkInsert() {
        // Get a cursor for the table...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<>();
        cursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("notification received");
            }
        });

        ContentValues[] testValues = new ContentValues[] {
                createValues("ball", "toy"),
                createValues("hammer", "tool")
        };
        mContext.getContentResolver().bulkInsert(ContractForTests.Items.CONTENT_URI, testValues);

        // Verity the final number of items in the table...
        Cursor resultCursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(2, resultCursor.getCount());

        // Verity that the Uri is notified only once,
        // despite multiple insert on the same uri...
        assertEquals(1, notifications.size());
        assertEquals("notification received", notifications.get(0));

        cursor.close();
        resultCursor.close();
    }

    public void testBatchOperations() {
        // Get a cursor for the table...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<>();
        cursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("notification received");
            }
        });

        try {
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            operations.add(ContentProviderOperation.newInsert(ContractForTests.Items.CONTENT_URI).withValues(createValues("ball", "toy")).build());
            operations.add(ContentProviderOperation.newInsert(ContractForTests.Items.CONTENT_URI).withValues(createValues("hammer", "tool")).build());
            operations.add(ContentProviderOperation.newUpdate(ContractForTests.Items.CONTENT_URI).withSelection(ContractForTests.Items.NAME + "=?", new String[]{"hammer"}).withValue(ContractForTests.Items.NAME, "screwdriver").build());
            mContext.getContentResolver().applyBatch(ContractForTests.AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException e) {
            fail("batch operation failed " + e);
        }

        // Verity the final number of items in the table...
        Cursor resultCursor = mContext.getContentResolver().query(
                ContractForTests.Items.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(2, resultCursor.getCount());

        // Verity that the uri is notified only once,
        // despite multiple operations on the same uri...
        assertEquals(1, notifications.size());
        assertEquals("notification received", notifications.get(0));

        cursor.close();
        resultCursor.close();
    }

    private ContentValues createValues(String name, String type) {
        ContentValues values = new ContentValues();
        values.put(ContractForTests.Items.NAME, name);
        values.put(ContractForTests.Items.TYPE, type);
        return values;
    }

    private void validateCursor(Cursor valueCursor, ContentValues expectedValues) {
        assertTrue(valueCursor.moveToNext());

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(idx));
        }
    }
}