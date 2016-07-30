package foocoder.dnd.presentation.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import foocoder.dnd.R;
import foocoder.dnd.domain.Contact;
import foocoder.dnd.presentation.internal.di.components.ContactComponent;
import foocoder.dnd.presentation.presenter.ContactListPresenter;
import foocoder.dnd.presentation.view.ContactListView;
import foocoder.dnd.presentation.view.adapter.ContactAdapter;
import rx.Observable;
import rx.Subscription;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class ContactListActivity extends BaseActivity<ContactComponent> implements ContactListView {

    @BindView(R.id.contacts)
    ListView listView;

    @BindView(R.id.search)
    EditText search;

    @BindView(R.id.cancel)
    Button cancel;

    @BindView(R.id.save)
    Button save;

    @BindView(R.id.progress_wheel)
    ProgressWheel progressWheel;

    @Inject
    ContactAdapter adapter;

    @Inject
    ContactListPresenter presenter;

    public static void start(Activity activity) {
        Intent intent = new Intent(activity, ContactListActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list);

        ButterKnife.bind(this);
        getComponent().inject(this);
        presenter.bindView(this);

        listView.setAdapter(adapter);

        Subscription subscription = RxTextView.textChanges(search)
                .skip(1)
                .debounce(100, MILLISECONDS)
                .map(CharSequence::toString)
                .observeOn(AndroidSchedulers.mainThread())
                .<String>switchMap(s -> Observable.create(subscriber -> {
                    try {
                        presenter.onTextChanged(s.toLowerCase());
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }

                    subscriber.onNext(s);
                }))
                .onErrorReturn(throwable -> "An error occurred!")
                .subscribe(s -> {
                    Timber.d("name to be searched %s", s);
                    adapter.notifyDataSetChanged();
                });

        presenter.start();

        addSubscriptionsForUnbinding(subscription);
    }

    @OnClick(R.id.save)
    void onSaveClick() {
        presenter.saveContactList();

        finish();
    }

    @OnClick(R.id.cancel)
    void onCancelClick() {
        finish();
    }

    @OnItemClick(R.id.contacts)
    void onContactListItemClick(AdapterView<?> parent, View view, int position, long id) {
        CheckBox check = (CheckBox) view.findViewById(R.id.check);
        check.toggle();
        presenter.onContactCheckedChange(check.isChecked(), position);
    }

    @Override
    protected void onDestroy() {
        presenter.unbind();

        super.onDestroy();
    }

    @Override
    public void showContacts(List<Contact> contactList) {
        adapter.changeData(contactList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void startLoading() {
        progressWheel.spin();
    }

    @Override
    public void stopLoading() {
        if (progressWheel.isSpinning()) {
            progressWheel.stopSpinning();
        }
    }

    @Override
    protected ContactComponent getComponent() {
        return getApplicationComponent()
                .contactComponentBuilder()
                .activityModule(getActivityModule())
                .build();
    }
}
