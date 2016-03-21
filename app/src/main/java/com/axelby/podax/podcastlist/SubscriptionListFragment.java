package com.axelby.podax.podcastlist;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.Subscriptions;
import com.axelby.podax.UpdateService;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.List;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.schedulers.Schedulers;

public class SubscriptionListFragment extends RxFragment {

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	public static class AddSubscriptionDialog extends DialogFragment {
		public AddSubscriptionDialog() { }

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.subscription_list_add, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			EditText url = (EditText) view.findViewById(R.id.url);
			url.setOnEditorActionListener((url1, actionId, event) -> {
				SubscriptionProvider.addNewSubscription(url1.getContext(), url1.getText().toString());
				dismiss();
				return true;
			});

			view.findViewById(R.id.subscribe).setOnClickListener(button -> {
				EditText url1 = (EditText) ((ViewGroup)button.getParent()).findViewById(R.id.url);
				SubscriptionProvider.addNewSubscription(button.getContext(), url1.getText().toString());
				dismiss();
			});
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		View.OnClickListener addListener = view1 -> {
			AddSubscriptionDialog dialog = new AddSubscriptionDialog();
			dialog.show(getFragmentManager(), "addSubscription");
		};
		getActivity().findViewById(R.id.add).setOnClickListener(addListener);

		Observable<List<ItemModel>> models = Subscriptions.getAll(getActivity())
			.map(sub -> ItemModel.fromSubscriptionId(sub.getTitle(), sub.getThumbnail(), sub.getId()))
			.toList()
			.subscribeOn(Schedulers.io())
			.compose(RxLifecycle.bindFragment(lifecycle()));

		RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
		list.setLayoutManager(new GridLayoutManager(getActivity(), 3));
		list.setAdapter(new ListAdapter(models));
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.subscription_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            UpdateService.updateSubscriptions(getActivity());
            return true;
        }
        return false;
    }
}
