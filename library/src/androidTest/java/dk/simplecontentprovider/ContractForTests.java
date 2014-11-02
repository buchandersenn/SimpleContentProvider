package dk.simplecontentprovider;

import android.net.Uri;
import android.provider.BaseColumns;

public class ContractForTests {
    public static final String SCHEME = "content://";
    public static final String AUTHORITY = "dk.simplecontentprovider.test";

    public static final class Values implements BaseColumns {
        protected static final String TABLE_NAME = "test_table";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/" + TABLE_NAME);

        public static final String KEY = "key";
        public static final String VALUE = "value";
    }

    public static final class View implements BaseColumns {
        protected static final String VIEW_NAME = "view";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/" + VIEW_NAME);

        public static final String NUMBER_OF_KEYS = "number_of_keys";
        public static final String MIN_VALUE = "min_value";
        public static final String MAX_VALUE = "max_value";
    }

}
