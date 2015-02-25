package com.axelby.podax.ui;

import android.app.Fragment;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.axelby.podax.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogViewerFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.logviewer, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

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
}
