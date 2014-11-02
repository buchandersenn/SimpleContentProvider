package dk.simplecontentprovider.demo.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class DemoContract {
    public static final String SCHEME = "content://";
    public static final String AUTHORITY = "dk.simplecontentprovider.demo";

    public static final class Owners implements BaseColumns {
        protected static final String TABLE_NAME = "owners";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/" + TABLE_NAME);

        public static final String NAME = "name";
        public static final String ADDRESS = "address";
    }

    public static final class Pets implements BaseColumns {
        protected static final String TABLE_NAME = "pets";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/" + TABLE_NAME);

        public static final String NAME = "name";
        public static final String TYPE = "type";
        public static final String AGE = "age";
        public static final String OWNER_ID = "owner_id";
    }

    public static final class OwnersAndPetsView implements BaseColumns {
        protected static final String VIEW_NAME = "owners_and_pets";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/" + VIEW_NAME);

        public static final String NAME = "owner_name";
        public static final String ADDRESS = "owner_address";
        public static final String NUMBER_OF_PETS = "number_of_pets";
        public static final String PET_NAMES = "pet_names";
    }
}
