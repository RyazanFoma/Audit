package com.bit.eduardf.audit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.bit.eduardf.audit.ParcelableUser.USER_ID;
import static com.bit.eduardf.audit.ParcelableUser.USER_NAME;
import static com.bit.eduardf.audit.ParcelableUser.USER_HASH;
import static com.bit.eduardf.audit.ParcelableUser.USER_OBJECT;
import static com.bit.eduardf.audit.ParcelableUser.USER_ORGANIZATION;
import static com.bit.eduardf.audit.ParcelableUser.USER_RESPONSIBLE;
import static com.bit.eduardf.audit.ParcelableUser.USER_TYPE;
import static com.bit.eduardf.audit.ParcelableUser.USER_VERSION;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 07.12.18 15:47
 *
 */

/**
 * User authorization and login
 */
public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        Spinner.OnItemSelectedListener {

    private static final String PREF_LASTUSER = "lastuser";
    private static final String ARG_USERS = "users";
    private static final String ARG_POSITION = "position";

    private AuditOData oData; //OData для доступа в 1С
    private SimpleAdapter usersAdapter; //Адаптер для списка пользователей
    private List<Map<String, Object>> users = new ArrayList<>(); //Список пользователей
    private SharedPreferences preferences;
    private Disposable disposable;

    //All fieds
    private Spinner user;
    private EditText password;
    private ProgressBar progressBar;
    private FloatingActionButton enter;

    //Selected user guid and user position in list
    private String lastUser;
    private int iPosition = 0;

    /**
     * Notification fields that the user list is empty or not
     */
    private void notifyFields() {
        final boolean enabled = !users.isEmpty();
        user.setEnabled(enabled);
        password.setEnabled(enabled);
        enter.setVisibility(enabled ? View.VISIBLE: View.INVISIBLE);
    }

    /**
     * The success load callback of a users list
     */
    final Consumer<List<Map<String, Object>>> onSuccess = new Consumer<List<Map<String, Object>>> () {

        @Override
        public void accept(List<Map<String, Object>> data) {
            users.clear();
            users.addAll(data);
            usersAdapter.notifyDataSetChanged();
            if (lastUser != null) {
                int i = 0;
                for (Map<String, Object> objectMap: users) {
                    if(lastUser.equals(objectMap.get(USER_ID).toString())) {
                        user.setSelection(i);
                        break;
                    }
                    i++;
                }
            }
            progressBar.setVisibility(View.INVISIBLE);
            notifyFields();
        }
    };

    /**
     * The loader error callback
     */
    final Consumer<Throwable> onError = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable e) {
            ((ODataErrorException)e).snackbarShow(MainActivity.this, R.id.toolbar);
            users.clear();
            usersAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.INVISIBLE);
            notifyFields();
        }
    };

    /**
     * Observable for get the auditors list
     * @return single observable
     */
    public Single<List<Map<String, Object>>> getData() {
        return Single.create(new SingleOnSubscribe<List<Map<String, Object>>>() {
            @Override
            public void subscribe(SingleEmitter<List<Map<String, Object>>> emitter) throws ODataErrorException {
                try {
                    final List<Map<String, Object>> data = oData.getUsers();
                    if (!data.isEmpty()) {
                        emitter.onSuccess(data);
                    }
                    else {
                        emitter.onError(new RuntimeException(getString(R.string.msg_is_empty)));
                    }
                }
                catch (ODataErrorException e) {
                    emitter.onError(e);
                }
            }
        });
    }

    /**
     * Start loading user list
     * @param context - activity contecxt
     */
    private void startLoader(Context context) {
        oData = new AuditOData(context);
        progressBar.setVisibility(View.VISIBLE);
        disposable = getData().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess, onError);
    }

    /**
     * Stop loading user list
     */
    private void stopLoader() {
        if (disposable!= null && disposable.isDisposed()) disposable.dispose();
        oData = null;
    }

    /**
     * Initialize all fields
     * @param savedInstanceState - storage place
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Пока только меню
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);

        //Готовим все для спиннера
        usersAdapter = new SimpleAdapter(this, users, android.R.layout.simple_spinner_item,
                new String[]{USER_NAME}, new int[]{android.R.id.text1});
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        lastUser = preferences.getString(PREF_LASTUSER, AuditOData.EMPTY_KEY);

        user = findViewById(R.id.user);
        user.setAdapter(usersAdapter);
        user.setOnItemSelectedListener(this);

        //Кнопка Вперед
        enter = findViewById(R.id.enter);
        enter.setOnClickListener(this);

        password = findViewById(R.id.password);
    }

    /**
     * Load the users list
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (users.isEmpty()) {
            startLoader(this);
        }
        else {
            user.setSelection(iPosition);
        }
        notifyFields();
    }

    /**
     * Stop load the users list
     */
    protected void onDestroy() {
        super.onDestroy();
        stopLoader();
        usersAdapter = null;
    }

    /**
     * Create menu
     * @param menu - menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Callback for selecting setting
     * @param item - setting item
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingAudit.class));
            users.clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Save the users list and list position
     * @param outState - storage place
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!users.isEmpty()) {
            final Parcelable[] parcelables = new Parcelable[users.size()];
            int i = 0;
            for (Map<String, Object> u: users) {
                parcelables[i++] = new ParcelableUser(u);
            }
            outState.putParcelableArray(ARG_USERS, parcelables);
            outState.putInt(ARG_POSITION, iPosition);
        }
    }

    /**
     * Restore the users list and list position
     * @param savedInstanceState - storage place
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final Parcelable[] parcelables = savedInstanceState.getParcelableArray(ARG_USERS);
        if (parcelables != null) {
            for (Parcelable parcelable: parcelables) {
                users.add(((ParcelableUser) parcelable).user);
            }
            usersAdapter.notifyDataSetChanged();
            iPosition = savedInstanceState.getInt(ARG_POSITION);
        }
    }

    /**
     * Callback the enter button
     * @param v - enter button
     */
    @Override
    public void onClick(View v) {
        //Нажата именно Вперед?
        if (v.getId()==R.id.enter) {
            //Текущие значения пароля
            final String hash = users.get(iPosition).get(USER_HASH).toString();

            //Пароль верный?
            if (equalsPassword(hash, password.getText().toString())) {
//                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                final String id = users.get(iPosition).get(USER_ID).toString();
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SettingTask.DEFAULT_TYPE,
                        users.get(iPosition).get(USER_TYPE).toString());
                editor.putString(SettingTask.DEFAULT_ORGANIZATION,
                        users.get(iPosition).get(USER_ORGANIZATION).toString());
                editor.putString(SettingTask.DEFAULT_OBJECT,
                        users.get(iPosition).get(USER_OBJECT).toString());
                editor.putString(SettingTask.DEFAULT_RESPONSIBLE,
                        users.get(iPosition).get(USER_RESPONSIBLE).toString());
                editor.putString(PREF_LASTUSER, id);
                editor.apply();
                //Открываем список заданий
                startActivity(TaskListActivity.intentActivity(this,
                        id,
                        users.get(iPosition).get(USER_NAME).toString(),
                        users.get(iPosition).get(USER_VERSION).toString()));
            }
            else {
                //Неверный пароль
                Snackbar.make(v, R.string.msg_password, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                password.setText("");
            }
        }
    }

    /**
     * Compare password hash with password entry
     * @param passwordText - hash password
     * @param inputText - password entry
     * @return - comparison result
     */
    private boolean equalsPassword(String passwordText, String inputText) {
        char[] password = passwordText.toCharArray();
        char[] input = inputText.toCharArray();
        if (password.length != input.length) return false;
        for (int position = 0; position<password.length; position++ ) {
            if ((int) password[position] != ((int) input[position]*13-(position+1)*7)%873) return false;
        }
        return true;
    }

    /**
     * Clear password after user selection
     * @param parent - parent
     * @param view - view
     * @param position - user position in list
     * @param id - id
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (iPosition != position) {
            iPosition = position;
            ((EditText) findViewById(R.id.password)).setText("");
        }
    }

    /**
     * not used
     * @param parent - parent
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
//Фома 2018