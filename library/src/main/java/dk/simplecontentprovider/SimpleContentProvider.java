package dk.simplecontentprovider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * General purpose {@link ContentProvider} base class that uses SQLiteDatabase for storage,
 * and works on entities added to the provider.
 *
 * Some of the logic for the applyBatch operation is loosely based on com.android.common.content.SQLiteContentProvider.
 */
public abstract class SimpleContentProvider extends ContentProvider {
    protected String mAuthority;

    protected String mDatabaseName;
    protected int mDatabaseVersion = 1;

    protected List<Entity> mEntities = new ArrayList<Entity>();
    protected List<EntityView> mViews = new ArrayList<EntityView>();
    protected Map<String, Set<String>> mEntityToViewsMap = new HashMap<String, Set<String>>();

    protected SimpleUriMatcher mMatcher;
    protected SQLiteOpenHelper mDatabaseHelper;

    private final ThreadLocal<Boolean> mApplyingBatchOperations = new ThreadLocal<Boolean>();
    private final Set<Uri> mChangedUris = new HashSet<Uri>();

    @Override
    public boolean onCreate() {
        initProvider();

        if (mAuthority == null) {
            throw new IllegalStateException("Authority was not set in initProvider - override initProvider and use setAuthority to set an authority for the provider");
        }

        if (mDatabaseName == null) {
            throw new IllegalStateException("Database name was not set in initProvider - override initProvider and use setDatabaseName to set a database name for the provider");
        }

        if (mEntities.isEmpty()) {
            throw new IllegalStateException("Entities were not created in initProvider - override initProvider and use addEntity to setup entities for the provider");
        }

        for (Entity entity : mEntities) {
            if (entity.columns.isEmpty()) {
                throw new IllegalStateException("No columns for entity '" + entity.name + "' - use addColumn to add columns for each entity in the provider");
            }
        }

        if (mMatcher == null) {
            mMatcher = new SimpleUriMatcher(mAuthority, mEntities, mViews);
        }

        if (mDatabaseHelper == null) {
            mDatabaseHelper = new SimpleDatabaseHelper(getContext(), mDatabaseName, mDatabaseVersion, mEntities);
        }

        return true;
    }

    @Override
    public String getType(Uri uri) {
        SimpleUriMatcher.Match match = mMatcher.match(uri);
        if (match == null) {
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        String matchName;
        if (match.entity != null) {
            matchName = match.entity.name;
        } else if (match.view != null) {
            matchName = match.view.name;
        } else {
            throw new IllegalStateException("Invalid match on Uri: " + uri);
        }

        if (match.isItem()) {
            return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + mAuthority + "." + matchName;
        } else {
            return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + mAuthority + "." + matchName;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        SimpleUriMatcher.Match match = mMatcher.match(uri);
        if (match == null) {
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        // Query an entity...
        if (match.entity != null) {
            SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
            String table = match.entity.name;
            String idColumn = match.entity.idColumn;

            if (match.isItem) {
                long id  = ContentUris.parseId(uri);
                String where = table + "." + idColumn + "=?";
                String[] whereArgs = new String[]{"" + id};
                selection = DatabaseUtils.concatenateWhere(selection, where);
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, whereArgs);
                cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
            } else {
                cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
            }
        }

        // Query a view...
        else if (match.view != null) {
            SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
            SQLiteQueryBuilder builder = match.view.queryBuilder;
            String idColumn = match.view.idColumn;

            if (match.isItem) {
                long id  = ContentUris.parseId(uri);
                String where = idColumn + "=?";
                String[] whereArgs = new String[]{"" + id};
                selection = DatabaseUtils.concatenateWhere(selection, where);
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, whereArgs);
                cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
            } else {
                cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
            }
        }

        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SimpleUriMatcher.Match match = mMatcher.match(uri);

        if (match == null) {
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        if (match.view != null) {
            throw new IllegalArgumentException("Cannot use insert with a view Uri: " + uri);
        }

        if (match.isItem) {
            throw new IllegalArgumentException("Cannot use insert with an item Uri: " + uri);
        }

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        String table = match.entity.name;
        String nullColumnHack = match.entity.nullColumnHack;
        long insertedId = db.insert(table, nullColumnHack, values);

        Uri insertedUri = null;
        if (insertedId != -1) {
            insertedUri = ContentUris.withAppendedId(uri, insertedId);
            postNotifyChangedUri(uri);
            postNotifyChangedViews(match.entity.name);
            notifyChangedUris();
        }

        return insertedUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SimpleUriMatcher.Match match = mMatcher.match(uri);

        if (match == null) {
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        if (match.view != null) {
            throw new IllegalArgumentException("Cannot use update with a view Uri: " + uri);
        }

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        String table = match.entity.name;
        String idColumn = match.entity.idColumn;

        int rowCount;
        if (match.isItem) {
            long id  = ContentUris.parseId(uri);
            String where = table + "." + idColumn + "=?";
            String[] whereArgs = new String[]{"" + id};
            selection = DatabaseUtils.concatenateWhere(selection, where);
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, whereArgs);
            rowCount = db.update(table, values, selection, selectionArgs);
        } else {
            rowCount = db.update(table, values, selection, selectionArgs);
        }

        if (rowCount > 0) {
            postNotifyChangedUri(uri);
            postNotifyChangedViews(match.entity.name);
            notifyChangedUris();
        }

        return rowCount;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SimpleUriMatcher.Match match = mMatcher.match(uri);

        if (match == null) {
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        if (match.view != null) {
            throw new IllegalArgumentException("Cannot use delete with a view Uri: " + uri);
        }

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        String table = match.entity.name;
        String idColumn = match.entity.idColumn;

        int rowCount;
        if (match.isItem) {
            long id  = ContentUris.parseId(uri);
            String where = table + "." + idColumn + "=?";
            String[] whereArgs = new String[]{"" + id};
            selection = DatabaseUtils.concatenateWhere(selection, where);
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, whereArgs);
            rowCount = db.delete(table, selection, selectionArgs);
        } else {
            rowCount = db.delete(table, selection, selectionArgs);
        }

        // If the selection is null then all the rows in the table are deleted,
        // but the delete method will not return a row count. Yet we still
        // want to notify listeners...
        if (selection == null || rowCount > 0) {
            postNotifyChangedUri(uri);
            postNotifyChangedViews(match.entity.name);
            notifyChangedUris();
        }

        return rowCount;
    }

    /**
     * Inserts a list of values in a single batch. The batch is running in a transaction.
     * If one of the insert operations fails, then all of them are rolled back.
     *
     * A single notifications of change is sent out only when all the inserts are done.
     *
     * @param uri The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database. This must not be {@code null}.
     * @return The number of values that were inserted.
     */
    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values) {
        SimpleUriMatcher.Match match = mMatcher.match(uri);

        if (match == null) {
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        if (match.view != null) {
            throw new IllegalArgumentException("Cannot use insert with a view Uri: " + uri);
        }

        if (match.isItem) {
            throw new IllegalArgumentException("Cannot use insert with an item Uri: " + uri);
        }

        String table = match.entity.name;
        String nullColumnHack = match.entity.nullColumnHack;
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        int rows = 0;
        try {
            db.beginTransaction();
            for (ContentValues value : values) {
                long id = db.insert(table, nullColumnHack, value);
                if (id != -1) {
                    rows += 1;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (rows > 0) {
            postNotifyChangedUri(uri);
            postNotifyChangedViews(match.entity.name);
            notifyChangedUris();
        }

        return rows;
    }

    /**
     * Applies a list of operations in a single batch. The batch is running in a transaction.
     * If one of the operations fails, then all of them are rolled back.
     *
     * Notifications of changes are batched and sent out only when all the operations are done.
     * The content provider always sends a maximum of one notification per URI,
     * even if multiple operations were applied to the same URI.
     *
     * @param operations the operations to apply
     * @return the results of the applications
     * @throws OperationApplicationException thrown if any operation fails.
     * @see ContentProvider#applyBatch(java.util.ArrayList)
     * @see ContentProviderOperation#apply
     */
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        ContentProviderResult[] results = null;
        try {
            mApplyingBatchOperations.set(true);
            db.beginTransaction();
            results = super.applyBatch(operations);
            db.setTransactionSuccessful();
        } finally {
            mApplyingBatchOperations.set(false);
            db.endTransaction();
            notifyChangedUris();
        }

        return results;
    }

    private void postNotifyChangedUri(Uri uri) {
        synchronized (mChangedUris) {
            mChangedUris.add(uri);
        }
    }

    protected void postNotifyChangedViews(String entityName) {
        Set<String> viewNames = mEntityToViewsMap.get(entityName);
        if (viewNames == null) {
            return;
        }

        for (String view : viewNames) {
            Uri uri = Uri.parse("content://" + mAuthority + "/" + view);
            postNotifyChangedUri(uri);
        }
    }

    private void notifyChangedUris() {
        boolean isApplyingBatch = mApplyingBatchOperations.get() != null && mApplyingBatchOperations.get();
        if (isApplyingBatch) {
            return;
        }

        Set<Uri> changed;
        synchronized (mChangedUris) {
            changed = new HashSet<Uri>(mChangedUris);
            mChangedUris.clear();
        }

        ContentResolver resolver = getContext().getContentResolver();
        for (Uri uri : changed) {
            resolver.notifyChange(uri, null);
        }
    }

    /**
     * Override this method to set up the content provider.
     *
     * Set the authority, database name and version using
     * setAuthority, setDatabaseName and setDatabaseVersion,
     * respectively.
     *
     * Then add entities with the addEntity and addColumn
     * methods. Optionally add constraints to the entities
     * using the addConstraint method.
     *
     * Optionally add views using the addView method.
     * For each view, use the onEntity method to
     * specify entities, which must automatically
     * propagate changes to cursors on the view.
     */
    protected abstract void initProvider();

    /**
     * Set the authority of the content provider.
     * It must match the provider tag in the manifest.
     *
     * This method must be called in initProvider.
     * Failure to do so will result in an
     * IllegalStateException.
     *
     * @param authority the authority for the provider
     */
    protected void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    /**
     * Set the name of the database. This is used by
     * the database helper implementation to create
     * the database file.
     *
     * This method must be called in initProvider.
     * Failure to do so will result in an
     * IllegalStateException.
     *
     * @param databaseName the name of the database used by the provider
     */
    protected void setDatabaseName(String databaseName) {
        this.mDatabaseName = databaseName;
    }

    /**
     * Set the version of the database. This is used by
     * the database helper implementation to create
     * or upgrade the database file.
     *
     * If this method is not called in initProvider then
     * the database version is assumed to be 1.
     *
     * @param databaseVersion the version of the database used by the provider
     */
    protected void setDatabaseVersion(int databaseVersion) {
        this.mDatabaseVersion = databaseVersion;
    }

    /**
     * Optionally set a custom database helper. If no custom helper
     * is specified then the default SimpleDatabaseHelper
     * implementation is used.
     *
     * The most common use case for this method is to replace
     * the default implementation with a helper extending
     * SimpleDatabaseHelper, in order to override onUpgrade
     * and perform additional steps when upgrading the database.
     *
     * @param helper a custom SQLiteOpenHelper implementation
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void setDatabaseHelper(SQLiteOpenHelper helper) {
        this.mDatabaseHelper = helper;
    }

    /**
     * Optionally set a custom Uri matcher implementation.
     * If no custom implementation is specified then the
     * default @{Link SimpleUriMatcher} implementation
     * is used.
     *
     * It will probably never be necessary, or advisable,
     * to replace the Uri matcher, but it is possible.
     *
     * @param matcher a custom matcher implementation
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void setUriMatcher(SimpleUriMatcher matcher) {
        this.mMatcher = matcher;
    }

    protected Entity addEntity(String name) {
        return addEntity(name, BaseColumns._ID, null);
    }

    protected Entity addEntity(String name, String idColumn, String nullColumnHack) {
        Entity newEntity = new Entity(name, idColumn, nullColumnHack);
        mEntities.add(newEntity);
        return newEntity;
    }

    protected EntityView addView(String name, SQLiteQueryBuilder queryBuilder) {
        return addView(name, BaseColumns._ID, queryBuilder);
    }

    protected EntityView addView(String name, String idColumn, SQLiteQueryBuilder queryBuilder) {
        EntityView newView = new EntityView(name, idColumn, queryBuilder);
        mViews.add(newView);
        return newView;
    }

    protected static class Entity {
        protected final String name;
        protected final String idColumn;
        protected final String nullColumnHack;
        protected final List<EntityColumn> columns;
        protected final List<String> constraints;

        public Entity(String name, String idColumn, String nullColumnHack) {
            this.name = name;
            this.idColumn = idColumn;
            this.nullColumnHack = nullColumnHack;
            this.columns = new ArrayList<EntityColumn>();
            this.constraints = new ArrayList<String>();
        }

        // TODO : JavaDoc - explain about syntax, incl. row constraint
        public Entity addColumn(String name, String definition) {
            columns.add(new EntityColumn(name, definition));
            return this;
        }

        // TODO : JavaDoc - these are table constraints - can be named?
        public Entity addConstraint(String sql) {
            constraints.add(sql);
            return this;
        }
    }

    protected static class EntityColumn {
        protected final String name;
        protected final String definition;

        public EntityColumn(String name, String definition) {
            this.name = name;
            this.definition = definition;
        }
    }

    protected class EntityView {
        protected final String name;
        protected final String idColumn;
        protected final SQLiteQueryBuilder queryBuilder;

        public EntityView(String name, String idColumn, SQLiteQueryBuilder queryBuilder) {
            this.name = name;
            this.idColumn = idColumn;
            this.queryBuilder = queryBuilder;
        }

        public EntityView onEntity(String entityName) {
            Set<String> views = mEntityToViewsMap.get(entityName);
            if (views == null) {
                views = new HashSet<String>();
                mEntityToViewsMap.put(entityName, views);
            }

            views.add(this.name);

            return this;
        }
    }
}
