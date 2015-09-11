package com.axelby.podax.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {
    private String _message;

	public static ProgressDialogFragment newInstance() {
		return new ProgressDialogFragment();
	}

    public void setMessage(String message) {
        _message = message;
    }

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final ProgressDialog dialog = new ProgressDialog(getActivity());
        if (_message != null)
            dialog.setMessage(_message);
        else
            dialog.setMessage("Logging into gpodder.net...");
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);

		// Disable the back button
		DialogInterface.OnKeyListener keyListener = (dialog1, keyCode, event) ->
			keyCode == KeyEvent.KEYCODE_BACK;
		dialog.setOnKeyListener(keyListener);
		return dialog;
	}

}
