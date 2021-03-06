package pranav.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.util.ArrayList;
import java.util.Arrays;

import static pranav.utilities.Log.TAG;


/**
 * Created on 06-08-2017 at 19:39 by Pranav Raut.
 * For QRCodeProtection
 *
 * @author Pranav
 * @version 0
 */

@SuppressWarnings("ALL")
public abstract class DataBaseHelper<E> extends SQLiteOpenHelper {

    private final SQLiteQuery query;
    private final Context context;

    public DataBaseHelper(Context context, SQLiteQuery query) {
        super(context, query.getDBName(), null, 1);
        this.query = query;
        this.context = context;
//        onUpgrade(getReadableDatabase(), 0, 0);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(query.getCreateQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(query.getExistsQuery());
        onCreate(db);
    }

    /**
     * @return Contents of database as string[row][col]
     */
    public String[][] getEverythingAsString() {
        String[] strings = query.getNamesToArray();
        Cursor cursor = getReadableDatabase().rawQuery(query.getSelectTableQuery(), null);
        String[][] result = new String[strings.length][(int) getRowCount()];
        if (cursor.moveToFirst())
            while (!cursor.isAfterLast()) {
                for (int i = 0; i < strings.length; i++)
                    result[cursor.getPosition()][i] = cursor.getString(i);
                cursor.moveToNext();
            }
        cursor.close();
        return result;
    }

    protected int getID(String col, Object object) {
        Cursor cursor = this.getCursor(query.getSelectItemQuery(col, object));
        if (cursor != null && cursor.moveToFirst()) {
            int i = cursor.getInt(0);
            cursor.close();
            return i;
        }
        return -1;
    }

    public int getID(String col, String object) {
        return getID(col, (Object) object);
    }

    @Nullable
    public Cursor getCursor(String query, String... values) {
        return getReadableDatabase().rawQuery(query, values);
    }

    public int removeAll() {
        return getWritableDatabase().delete(query.getDBName(), null, null);
    }

    public long getRowCount() {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), query.getDBName());
    }

    @Nullable
    public abstract ArrayList<E> getEverything();

    @Nullable
    public abstract E getItem(int index);

    public int deleteItem(int id) {
        Log.d(TAG, "deleteItem() called with: id = [" + id + "]");
        return getWritableDatabase().delete(query.getDBName(),
                "id =? ", new String[]{Integer.toString(id)});
    }

    public int updateItem(int id, E item) {
        return getWritableDatabase().update(query.getDBName(),
                contentVal(item), "id =? ", new String[]{String.valueOf(id)});
    }

    public long insertItem(E item) {
        return getWritableDatabase().insert(query.getDBName(), null, contentVal(item));
    }

    protected abstract ContentValues contentVal(E item);

    public Context getContext() {
        return context;
    }

    public SQLiteQuery getQuery() {
        return query;
    }

    /**
     * Created on 06-08-2017 at 19:49 by Pranav Raut.
     * For QRCodeProtection
     *
     * @author Pranav
     * @version 0
     */
    @SuppressWarnings("unused")
    public static class SQLiteQuery {
        public static final String TAG = "preons";
        private final static String[] VALUES = {
                "BLOB",
                "BOOLEAN",
                "DATETIME",
                "INT",
                "MEDIUMINT",
                "BIGINT",
                "FLOAT",
                "DOUBLE",
                "CHARACTER",
                "TEXT",
                "TIMESTAMP"
        };
        private final String name;
        private final ArrayList<String> names = new ArrayList<>();
        private final ArrayList<String> types = new ArrayList<>();
        private final ArrayList<String> defaultValues = new ArrayList<>();

        public SQLiteQuery(String name) {
            this.name = name;
        }

        public final void addCol(@Size(min = 1) String[] names, @dataType String[] types) {
            String[] defaults = new String[name.length()];
            Arrays.fill(defaults, "NONE");

            addCol(names, types, defaults);
        }

        public final void addCol(@Size(min = 1) String[] names, @dataType String[] types, String[] defaults) {
            if (names.length == types.length) for (int i = 0; i < names.length; i++)
                addCol(names[i], types[i], defaults[i]);
            else throw new IllegalArgumentException("size of name and type don't match " +
                    "name: [" + names.length + "] : type [" + types.length + "]");
        }

        public final void addCol(@Size(min = 1) String name, @dataType String type) {
            addCol(name, type, "NONE");
        }

        public final void addCol(@Size(min = 1) String name, @dataType String type, String defaultValue) {
            if (Arrays.binarySearch(VALUES, name.toUpperCase()) > 0)
                throw new IllegalAccessError("\"" + name + "\" is reserved");
            if (names.contains(name))
                throw new IllegalArgumentException("\"" + name + "\" exists try using different one");

            this.types.add(type);
            this.names.add(name);
            this.defaultValues.add(defaultValue);
        }

        @NonNull
        final String getCreateQuery() {
            StringBuilder builder = new StringBuilder(200);
            builder.append("CREATE TABLE ")
                    .append(name).append(" (").append("`id` INTEGER PRIMARY KEY AUTOINCREMENT, ");

            for (int i = 0; i < names.size(); i++) {
                builder.append('`').append(names.get(i)).append("` ").append(types.get(i))
                        .append(" DEFAULT ").append(defaultValues.get(i))
                        .append(", ");
            }

            builder = new StringBuilder(builder.substring(0, builder.length() - 2));
            builder.append(")");

            Log.d(TAG, "getCreateQuery: " + builder.toString());
            return builder.toString();
        }

        final String getDBName() {
            return name;
        }

        /**
         * @return the expression for which the class @{@link SQLiteOpenHelper} will drop table creation if the table
         * has last column existing in the table else will create new table
         */
        @NonNull
        final String getExistsQuery() {
            return "DROP TABLE IF EXISTS " + names.get(names.size() - 1);
        }

        public final ArrayList<String> getNames() {
            return names;
        }

        @NonNull
        final String getSelectItemQuery(String colName, Object value) {
            return getSelectTableQuery() + " WHERE `" + colName + "` = '" + value + "'";
        }

        @NonNull
        public final String getSelectItemQuery(String colName, String value) {
            return getSelectTableQuery() + " WHERE `" + colName + "` = '" + value + "'";
        }

        @NonNull
        public final String getSelectItemQuery(int colIndex, Object value) {
            return getSelectTableQuery() + " WHERE `" + names.get(colIndex) + "` = '" + value + "'";
        }

        @NonNull
        public final String getSelectItemQuery(int colIndex, String value) {
            return getSelectTableQuery() + " WHERE `" + names.get(colIndex) + "` = '" + value + "'";
        }

        @NonNull
        final String[] getNamesToArray() {
            return names.toArray(new String[0]);
        }

        @NonNull
        public final String getSelectTableQuery() {
            return "SELECT * FROM `" + name + "`";
        }
    }
}
