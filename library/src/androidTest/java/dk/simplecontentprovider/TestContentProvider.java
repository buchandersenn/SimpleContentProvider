package dk.simplecontentprovider;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestContentProvider extends AndroidTestCase {

    public void deleteDatabase() {
        mContext.getContentResolver().delete(
                ContractForTests.Values.CONTENT_URI,
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
        String valueType = ContractForTests.AUTHORITY + "." + ContractForTests.Values.TABLE_NAME;

        String dirType = mContext.getContentResolver().getType(ContractForTests.Values.CONTENT_URI);
        assertEquals(ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + valueType, dirType);

        Uri itemUri = ContentUris.withAppendedId(ContractForTests.Values.CONTENT_URI, 1);
        String itemType = mContext.getContentResolver().getType(itemUri);
        assertEquals(ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + valueType, itemType);
    }

    public void testInsertAndReadProvider() {
        ContentValues testValues = createValues("key", 1);

        Uri insertedUri = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues);
        long insertedRowId = ContentUris.parseId(insertedUri);

        // Verify that we got a row back...
        assertTrue(insertedRowId != -1);

        // Verify that we can read the inserted value...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        validateCursor(cursor, testValues);

        // Verify that we can read the inserted value when adding a where clause...
        cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                ContractForTests.Values.KEY + " = ?",
                new String[] {"key"},
                null
        );
        validateCursor(cursor, testValues);

        // Verify that we can read the inserted value when using an Uri with the ID of the inserted row...
        Uri uri = ContentUris.withAppendedId(ContractForTests.Values.CONTENT_URI, insertedRowId);
        cursor = mContext.getContentResolver().query(
                uri,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        validateCursor(cursor, testValues);
    }

    public void testInsertConstraintConflict() {
        ContentValues testValues1 = createValues("key", 1);
        ContentValues testValues2 = createValues("key", 2);

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);

        assertTrue(insertedRowId1 != -1);
        assertTrue(insertedRowId2 != -1);

        // Verify that the first value was replaced with the second value,
        // as specified in the constraint on the table...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        assertEquals(1, cursor.getCount());
        validateCursor(cursor, testValues2);
    }

    public void testUpdate() {
        ContentValues testValues1 = createValues("key1", 1);
        ContentValues testValues2 = createValues("key2", 2);

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);
        assertTrue(insertedRowId2 != -1);

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(ContractForTests.Values.VALUE, 3);

        int count = mContext.getContentResolver().update(
                ContractForTests.Values.CONTENT_URI,
                updatedValues,
                ContractForTests.Values._ID + " = ?",
                new String[] {Long.toString(insertedRowId2)});

        // Verify that one record was affected...
        assertEquals(count, 1);

        // Verify that the value was updated...
        ContentValues expectedValues = new ContentValues(testValues2);
        expectedValues.putAll(updatedValues);
        Uri uri = ContentUris.withAppendedId(ContractForTests.Values.CONTENT_URI, insertedRowId2);
        Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                ContractForTests.Values._ID + " = ?",
                new String[] {Long.toString(insertedRowId2)},
                null
        );
        validateCursor(cursor, expectedValues);
    }

    public void testDelete() {
        ContentValues testValues1 = createValues("key1", 1);
        ContentValues testValues2 = createValues("key2", 2);

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);
        assertTrue(insertedRowId2 != -1);

        // Delete row 2...
        int count = mContext.getContentResolver().delete(
                ContractForTests.Values.CONTENT_URI,
                ContractForTests.Values._ID + " = ?",
                new String[] {Long.toString(insertedRowId2)});

        // Verify that one record was affected...
        assertEquals(count, 1);

        // Verity that the correct row was deleted...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                ContractForTests.Values._ID + " = ?",
                new String[] {Long.toString(insertedRowId1)},
                null
        );
        assertEquals(1, cursor.getCount());
        validateCursor(cursor, testValues1);
    }

    public void testUriNotificationOnInsert() throws InterruptedException {
        // Get a cursor for the table...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<String>();
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

        // Insert value...
        ContentValues testValues = createValues("key", 1);
        mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues);

        // Verity that the table Uri is notified when an item is inserted
        assertEquals(1, notifications.size());
        assertEquals("notification received", notifications.get(0));
    }

    public void testUriNotificationOnUpdateTable() {
        ContentValues testValues1 = createValues("key1", 1);
        ContentValues testValues2 = createValues("key2", 2);

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);
        assertTrue(insertedRowId2 != -1);

        // Get a cursor for the table...
        Cursor tableCursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Get a cursor for item 1...
        Cursor item1Cursor = mContext.getContentResolver().query(
                insertedUri1,
                null,
                null,
                null,
                null
        );

        // Get a cursor for item 2...
        Cursor item2Cursor = mContext.getContentResolver().query(
                insertedUri2,
                null,
                null,
                null,
                null
        );

        // Register observers...
        final List<String> notifications = new ArrayList<String>();
        tableCursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("table notification received");
            }
        });
        item1Cursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("item 1 notification received");
            }
        });
        item2Cursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("item 2 notification received");
            }
        });

        // Update item 1 by updating the table uri,
        // but limiting the effect with a where-clause...
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(ContractForTests.Values.VALUE, 2);
        int count = mContext.getContentResolver().update(
                ContractForTests.Values.CONTENT_URI,
                updatedValues,
                ContractForTests.Values._ID + " = ?",
                new String[] {Long.toString(insertedRowId1)});
        assertEquals(1, count);

        // Verify that all three observers were notified,
        // since the change happened at table level and
        // could therefore in theory affect all the items in the table...
        assertEquals(3, notifications.size());
        assertTrue(notifications.contains("table notification received"));
        assertTrue(notifications.contains("item 1 notification received"));
        assertTrue(notifications.contains("item 2 notification received"));
    }

    public void testUriNotificationOnUpdateItem() {
        ContentValues testValues1 = createValues("key1", 1);
        ContentValues testValues2 = createValues("key2", 2);

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        Uri insertedUri2 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues2);
        long insertedRowId2 = ContentUris.parseId(insertedUri2);
        assertTrue(insertedRowId2 != -1);

        // Get a cursor for the table...
        Cursor tableCursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Get a cursor for item 1...
        Cursor item1Cursor = mContext.getContentResolver().query(
                insertedUri1,
                null,
                null,
                null,
                null
        );

        // Get a cursor for item 2...
        Cursor item2Cursor = mContext.getContentResolver().query(
                insertedUri2,
                null,
                null,
                null,
                null
        );

        // Register observers...
        final List<String> notifications = new ArrayList<String>();
        tableCursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("table notification received");
            }
        });
        item1Cursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("item 1 notification received");
            }
        });
        item2Cursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("item 2 notification received");
            }
        });

        // Update item 1 by updating the item uri directly.
        // No where-clause necessary...
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(ContractForTests.Values.VALUE, 2);
        int count = mContext.getContentResolver().update(
                insertedUri1,
                updatedValues,
                null,
                null);
        assertEquals(1, count);

        // Verify that only the observer for the table and item 1 was notified,
        // and that no notification was received for item 2...
        assertEquals(2, notifications.size());
        assertTrue(notifications.contains("table notification received"));
        assertTrue(notifications.contains("item 1 notification received"));
    }

    public void testUriNotificationOnDelete() {
        ContentValues testValues1 = createValues("key1", 1);

        Uri insertedUri1 = mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues1);
        long insertedRowId1 = ContentUris.parseId(insertedUri1);
        assertTrue(insertedRowId1 != -1);

        // Get a cursor for the table...
        Cursor tableCursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<String>();
        tableCursor.registerContentObserver(new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                notifications.add("table notification received");
            }
        });

        // Delete table content...
        mContext.getContentResolver().delete(
                ContractForTests.Values.CONTENT_URI,
                null,
                null);

        // Verity that the table Uri is notified when an item is deleted
        assertEquals(1, notifications.size());
        assertTrue(notifications.contains("table notification received"));
    }

    public void testUriNotificationOfView() {
        // Get a cursor for the view...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.View.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<String>();
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

        // Insert value...
        ContentValues testValues = createValues("key", 1);
        mContext.getContentResolver().insert(ContractForTests.Values.CONTENT_URI, testValues);

        // Verity that the view Uri is notified when an item is inserted
        // in the table for which the view is registered...
        assertEquals(1, notifications.size());
        assertEquals("notification received", notifications.get(0));
    }

    public void testBulkInsert() {
        // Get a cursor for the table...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<String>();
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
                createValues("key1", 1),
                createValues("key2", 2)
        };
        mContext.getContentResolver().bulkInsert(ContractForTests.Values.CONTENT_URI, testValues);

        // Verity that the Uri is notified only once,
        // despite multiple insert on the same uri...
        assertEquals(1, notifications.size());
        assertEquals("notification received", notifications.get(0));
    }

    public void testBatchOperations() {
        // Get a cursor for the table...
        Cursor cursor = mContext.getContentResolver().query(
                ContractForTests.Values.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Register observer...
        final List<String> notifications = new ArrayList<String>();
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
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            operations.add(ContentProviderOperation.newInsert(ContractForTests.Values.CONTENT_URI).withValues(createValues("key1", 1)).build());
            operations.add(ContentProviderOperation.newInsert(ContractForTests.Values.CONTENT_URI).withValues(createValues("key2", 2)).build());
            operations.add(ContentProviderOperation.newUpdate(ContractForTests.Values.CONTENT_URI).withSelection(ContractForTests.Values.KEY + "=?", new String[]{"key2"}).withValue(ContractForTests.Values.VALUE, 3).build());
            mContext.getContentResolver().applyBatch(ContractForTests.AUTHORITY, operations);
        } catch (RemoteException e) {
            fail("batch operation failed " + e);
        } catch (OperationApplicationException e) {
            fail("batch operation failed " + e);
        }

        // Verity that the uri is notified only once,
        // despite multiple operations on the same uri...
        assertEquals(1, notifications.size());
        assertEquals("notification received", notifications.get(0));
    }

    private ContentValues createValues(String key, int value) {
        ContentValues values = new ContentValues();
        values.put(ContractForTests.Values.KEY, key);
        values.put(ContractForTests.Values.VALUE, value);
        return values;
    }

    private void validateCursor(Cursor valueCursor, ContentValues expectedValues) {
        assertTrue(valueCursor.moveToFirst());

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(idx));
        }
        valueCursor.close();
    }
}