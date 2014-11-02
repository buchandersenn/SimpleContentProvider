#SimpleContentProvider

SimpleContentProvider is a library to simplify the implementation of android content providers built on top of a SQLite database.
The library implements most the bothersome boilerplate code for you, so you no longer have to duplicate it for every project.

##Include the library in your project

The library is not (yet) available from Maven Central, so you'll need to clone the project and install it into your local Maven
repository by invoking

    $ ./gradlew clean build publishToMavenLocal


and then add the library as a dependency in your 'build.gradle' file:

    dependencies {
        compile 'dk.simplecontentprovider:simplecontentprovider:1.0.+'
    }

Alternatively, you can just to copy the three classes SimpleContentProvider, SimpleDatabaseHelper and
SimpleUriMatcher into your project.

<!--
The library is available from Maven Central, so you just need to add the following dependency to your `build.gradle` file:

    dependencies {
        compile 'dk.simplecontentprovider:simplecontentprovider:1.0.+'
    }
-->
##Usage

You need to write your contract class as usual, for example:

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
    }

The content provider, on the other hand, is hugely simplified. You no longer need to implement
query, insert, update and delete methods yourself. Instead you just have to extend
SimpleContentProvider and implement the initProvider method. In initProvider, you populate
the provider with the entity from your contract and define the authority, database name and
version.

Here's a simple - but adequate - implementation of a content provider for the contract above:

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
        }
    }

The code above will initialize a content provider with default implementations of 
the query, insert, update and delete methods, as well as implementations of
bulkInsert and applyBatch. The default implementation will uses transactions
to optimize bulk inserts and batch operations out of the box.

## Advanced usage 

### Adding views

The entity mechanism is simple and probably good enough for many applications.
But often you need to query for data from more than one entity at a time.

The example above has two entities, Owners and Pets. To combine data from both entities
you can define a view as follows:

    public final class DemoContract {
        public static final String SCHEME = "content://";
        public static final String AUTHORITY = "dk.simplecontentprovider.demo";

        public static final class Owners implements BaseColumns {
            ...
        }

        public static final class Pets implements BaseColumns {
            ...
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

And then add it to the content provider by defining a SQLiteQueryBuilder that lets the
content provider know how to populate the view, typically in the form of some kind of joint:

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
            ...

            // Add view
            addView(DemoContract.OwnersAndPetsView.VIEW_NAME, createQueryBuilderForOwnersAndPets())
                .onEntity(DemoContract.Owners.TABLE_NAME)
                .onEntity(DemoContract.Pets.TABLE_NAME);
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
    }

The you can query the view just as if it was a regular table:

        getContentResolver().query(
                DemoContract.OwnersAndPetsView.CONTENT_URI,
                new String []{DemoContract.OwnersAndPetsView._ID, DemoContract.OwnersAndPetsView.NAME, DemoContract.OwnersAndPetsView.NUMBER_OF_PETS, DemoContract.OwnersAndPetsView.PET_NAMES},
                null, null, null);

### Adding constraints

You can add constraints to your entities as shown below:

    @Override
    public void initProvider() {
        
        ...
        
        addEntity(DemoContract.Pets.TABLE_NAME)
                .addColumn(DemoContract.Pets._ID, "INTEGER PRIMARY KEY AUTOINCREMENT")
                .addColumn(DemoContract.Pets.NAME, "TEXT")
                .addColumn(DemoContract.Pets.TYPE, "TEXT")
                .addColumn(DemoContract.Pets.AGE, "INTEGER")
                .addColumn(DemoContract.Pets.OWNER_ID, "INTEGER")
                
                .addConstraint("FOREIGN KEY (" + DemoContract.Pets.OWNER_ID + ") " +
                                "REFERENCES " + DemoContract.Owners.TABLE_NAME + " (" + DemoContract.Owners._ID + ") ON DELETE CASCADE")
                .addConstraint("UNIQUE (" + DemoContract.Pets.NAME + ", " + DemoContract.Pets.OWNER_ID + ") ON CONFLICT REPLACE");
    }

Just remember that you need to enable support for foreign key constraints, if you are going to
use them. See the demo app for an example of how to enable such constraints. For other type of
constraints, see the SQLite documentation.

### Overriding default behaviour

SimpleContentProvider use a couple of helper classes, SimpleDatabaseHelper and SimpleUriMatcher,
to create the SQLite database and match Uri's. You can replace these helper classes with your own 
implementations as follows:

    @Override
    public void initProvider() {
        // Set authority...
        setAuthority(DemoContract.AUTHORITY);

        // Set database name and version...
        setDatabaseName("DemoContentProvider.db");
        setDatabaseVersion(1);
        
        // Use a custom database helper...
        setDatabaseHelper(new CustomDatabaseHelper(this));
        
        // Use a custom uri matcher...
        setUriMatcher(new CustomUriMatcher());
    }

The most common use case for this would be to extend SimpleDatabaseHelper and override the 
onUpgrade method in order to implement a better strategy for handling database changes. 
The default implementation just drops and re-create all the tables.

    public static class CustomDatabaseHelper extends SimpleDatabaseHelper {
        public DemoDatabaseHelper(Context context, String databaseName, int databaseVersion, List<Entity> entities) {
            super(context, databaseName, databaseVersion, entities);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            ...
        }
    }


#Developed By

Nicolai Buch-Andersen, Google+ <https://google.com/+NicolaiBuchAndersen>, Email: <nicolai.buch.andersen@gmail.com>

#License

    Copyright 2014 Nicolai Buch-Andersen

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.