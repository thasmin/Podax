package com.axelby.podax.ui;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.axelby.gpodder.AuthenticatorActivity;
import com.axelby.podax.Constants;
import com.axelby.podax.PodaxApplication;
import com.axelby.podax.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class LogViewerFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.logviewer, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		view.findViewById(R.id.clear_log).setOnClickListener((v) -> {
			try {
				File file = new File(getActivity().getExternalFilesDir(null), "podax.log");
				new FileOutputStream(file, false).close();
				loadLog();
			} catch (IOException ignored) {
			}
		});

		view.findViewById(R.id.gpodder).setOnClickListener((v) -> handleGPodder());

		loadLog();
	}

	private void loadLog() {
		File file = new File(getActivity().getExternalFilesDir(null), "podax.log");
		StringBuilder text = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.insert(0, '\n');
				text.insert(0, line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		TextView tv = (TextView) getActivity().findViewById(R.id.textView);
		tv.setText(text);
	}

	private void handleGPodder() {
		if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
			// TODO: explain permission usage with ActivityCompat.shouldShowRequestPermissionRationale
			FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, Constants.PERMISSION_GPODDER);
			return;
		}

		AccountManager am = AccountManager.get(getActivity());
		Account[] gpodder_accounts = am.getAccountsByType(Constants.GPODDER_ACCOUNT_TYPE);
		if (gpodder_accounts.length == 0) {
			startActivity(new Intent(getActivity(), AuthenticatorActivity.class));
		} else if (getView() != null) {
			Snackbar.make(getView(), "Refreshing from gpodder.net as " + gpodder_accounts[0].name, Snackbar.LENGTH_SHORT).show();
			ContentResolver.requestSync(gpodder_accounts[0], PodaxApplication.GPODDER_AUTHORITY, new Bundle());
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == Constants.PERMISSION_GPODDER) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				handleGPodder();
		}
	}
}
