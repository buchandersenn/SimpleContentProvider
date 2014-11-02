package dk.simplecontentprovider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class SimpleDatabaseHelper extends SQLiteOpenHelper {
    private final List<SimpleContentProvider.Entity> mEntities;

    public SimpleDatabaseHelper(Context context, String databaseName, int databaseVersion, List<SimpleContentProvider.Entity> entities) {
        super(context, databaseName, null, databaseVersion);
        this.mEntities = entities;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (SimpleContentProvider.Entity entity : mEntities) {
            String entitySql = null;

            for (SimpleContentProvider.EntityColumn column : entity.columns) {
                if (entitySql == null) {
                    entitySql = column.name + " " + column.definition;
                } else {
                    entitySql += "," + column.name + " " + column.definition;
                }
            }

            for (String constraint : entity.constraints) {
                if (entitySql == null) {
                    entitySql = constraint;
                } else {
                    entitySql += "," + constraint;
                }
            }

            db.execSQL("CREATE TABLE " + entity.name + " (" + entitySql + ")");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (SimpleContentProvider.Entity entity : mEntities) {
            db.execSQL("DROP TABLE IF EXISTS " + entity.name);
        }
        onCreate(db);
    }
}
