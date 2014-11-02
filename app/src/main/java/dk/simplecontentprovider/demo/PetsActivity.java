package dk.simplecontentprovider.demo;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import dk.simplecontentprovider.demo.dialogs.AddPetDialog;
import dk.simplecontentprovider.demo.provider.DemoContract;

public class PetsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_OWNER_ID = "extra_owner_id";

    private long mOwnerId;
    private SimpleCursorAdapter mAdapter;

    public static void startActivity(Context context, long owner) {
        Intent intent = new Intent(context, PetsActivity.class);
        intent.putExtra(EXTRA_OWNER_ID, owner);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        mOwnerId = intent.getLongExtra(EXTRA_OWNER_ID, -1);

        mAdapter = new SimpleCursorAdapter(this, R.layout.item_pet, null,
                new String[]{DemoContract.Pets.NAME, DemoContract.Pets.TYPE, DemoContract.Pets.AGE},
                new int[]{R.id.pet_name, R.id.pet_type, R.id.pet_age}, 0);

        setListAdapter(mAdapter);
        registerForContextMenu(getListView());

        View emptyView = getListView().getEmptyView();
        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText("No data");
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_item) {
            DialogFragment dialog = AddPetDialog.newInstance(mOwnerId);
            dialog.show(getFragmentManager(), "ADD_PET");
            return true;
        } else if (id == R.id.action_open_overview) {
            OverviewActivity.startActivity(this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = new MenuInflater(v.getContext());
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Uri uri = ContentUris.withAppendedId(DemoContract.Pets.CONTENT_URI, info.id);

        switch (item.getItemId()) {
            case R.id.delete:
                getContentResolver().delete(uri, null, null);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, DemoContract.Pets.CONTENT_URI,
                new String[]{DemoContract.Pets._ID, DemoContract.Pets.NAME, DemoContract.Pets.TYPE, DemoContract.Pets.AGE, DemoContract.Pets.OWNER_ID},
                DemoContract.Pets.OWNER_ID + "=?", new String[]{"" + mOwnerId}, DemoContract.Pets._ID);
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
