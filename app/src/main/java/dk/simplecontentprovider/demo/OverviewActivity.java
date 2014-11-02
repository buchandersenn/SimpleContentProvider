package dk.simplecontentprovider.demo;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import dk.simplecontentprovider.demo.provider.DemoContract;

public class OverviewActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private SimpleCursorAdapter mAdapter;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, OverviewActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mAdapter = new SimpleCursorAdapter(this, R.layout.item_overview, null,
                new String[]{DemoContract.OwnersAndPetsView.NAME, DemoContract.OwnersAndPetsView.NUMBER_OF_PETS, DemoContract.OwnersAndPetsView.PET_NAMES},
                new int[]{R.id.owner_name, R.id.pet_count, R.id.pet_names}, 0);

        setListAdapter(mAdapter);

        View emptyView = getListView().getEmptyView();
        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText("No data");
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, DemoContract.OwnersAndPetsView.CONTENT_URI,
                new String[]{DemoContract.OwnersAndPetsView._ID, DemoContract.OwnersAndPetsView.NAME, DemoContract.OwnersAndPetsView.NUMBER_OF_PETS, DemoContract.OwnersAndPetsView.PET_NAMES}, null, null,
                DemoContract.OwnersAndPetsView.NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
