package dk.simplecontentprovider.demo;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import dk.simplecontentprovider.demo.dialogs.AddOwnerDialog;
import dk.simplecontentprovider.demo.provider.DemoContract;

public class OwnersActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mAdapter = new SimpleCursorAdapter(this, R.layout.item_owner, null,
                new String[]{DemoContract.Owners.NAME, DemoContract.Owners.ADDRESS},
                new int[]{R.id.owner_name, R.id.owner_address}, 0);

        setListAdapter(mAdapter);
        registerForContextMenu(getListView());

        View emptyView = getListView().getEmptyView();
        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText("No data");
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        PetsActivity.startActivity(this, id);
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
            AddOwnerDialog dialog = new AddOwnerDialog();
            dialog.show(getFragmentManager(), "ADD_OWNER");
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
        Uri uri = ContentUris.withAppendedId(DemoContract.Owners.CONTENT_URI, info.id);

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
        return new CursorLoader(this, DemoContract.Owners.CONTENT_URI,
                new String[]{DemoContract.Owners._ID, DemoContract.Owners.NAME, DemoContract.Owners.ADDRESS},
                null, null, DemoContract.Owners._ID);
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
