package dk.simplecontentprovider.demo.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import dk.simplecontentprovider.demo.provider.DemoContract;
import dk.simplecontentprovider.demo.R;

public class AddOwnerDialog extends DialogFragment {
    private View mDialogView;

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        mDialogView = inflater.inflate(R.layout.dialog_add_owner, null);
        builder.setView(mDialogView);

        builder.setTitle(R.string.action_add_item);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createNewOwner();
            }
        });

        return builder.create();
    }

    private void createNewOwner() {
        EditText nameView = (EditText) mDialogView.findViewById(R.id.add_owner_name);
        EditText addressView = (EditText) mDialogView.findViewById(R.id.add_owner_address);

        ContentValues values = new ContentValues();
        values.put(DemoContract.Owners.NAME, nameView.getText().toString());
        values.put(DemoContract.Owners.ADDRESS, addressView.getText().toString());
        getActivity().getContentResolver().insert(DemoContract.Owners.CONTENT_URI, values);
    }
}
