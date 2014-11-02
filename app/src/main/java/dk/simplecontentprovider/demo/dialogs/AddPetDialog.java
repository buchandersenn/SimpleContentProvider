package dk.simplecontentprovider.demo.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import dk.simplecontentprovider.demo.provider.DemoContract;
import dk.simplecontentprovider.demo.R;

public class AddPetDialog extends DialogFragment {
    public static final String ARG_OWNER_ID = "arg_owner_id";

    private View mDialogView;

    public static DialogFragment newInstance(long ownerId) {
        Bundle args = new Bundle();
        args.putLong(ARG_OWNER_ID, ownerId);

        AddPetDialog dialog = new AddPetDialog();
        dialog.setArguments(args);


        return dialog;
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        mDialogView = inflater.inflate(R.layout.dialog_add_pet, null);
        builder.setView(mDialogView);

        builder.setTitle(R.string.action_add_item);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createNewPet();
            }
        });

        return builder.create();
    }

    private void createNewPet() {
        Bundle args = getArguments();
        long ownerId = args.getLong(ARG_OWNER_ID, -1);

        EditText nameView = (EditText) mDialogView.findViewById(R.id.add_pet_name);
        EditText typeView = (EditText) mDialogView.findViewById(R.id.add_pet_type);
        EditText ageView = (EditText) mDialogView.findViewById(R.id.add_pet_age);

        int age = 1;
        if (ageView.getText().toString() != null && !ageView.getText().toString().isEmpty()) {
            age = Integer.parseInt(ageView.getText().toString());
        }

        ContentValues values = new ContentValues();
        values.put(DemoContract.Pets.OWNER_ID, ownerId);
        values.put(DemoContract.Pets.NAME, nameView.getText().toString());
        values.put(DemoContract.Pets.TYPE, typeView.getText().toString());
        values.put(DemoContract.Pets.AGE, age);
        getActivity().getContentResolver().insert(DemoContract.Pets.CONTENT_URI, values);
    }
}
