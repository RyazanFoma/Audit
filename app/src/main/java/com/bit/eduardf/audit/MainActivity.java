package com.bit.eduardf.audit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bit.eduardf.audit.ParcelableUser.USER_ID;
import static com.bit.eduardf.audit.ParcelableUser.USER_NAME;
import static com.bit.eduardf.audit.ParcelableUser.USER_OBJECT;
import static com.bit.eduardf.audit.ParcelableUser.USER_ORGANIZATION;
import static com.bit.eduardf.audit.ParcelableUser.USER_PASSWORD;
import static com.bit.eduardf.audit.ParcelableUser.USER_RESPONSIBLE;
import static com.bit.eduardf.audit.ParcelableUser.USER_TYPE;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 07.12.18 15:47
 *
 */

//Главная активность - аутентификация пользователя и установка параметров приложения
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<List<Map<String, Object>>>,
        View.OnClickListener,
        Spinner.OnItemSelectedListener {

//    private static final String ARG_USER = "user";
    private static final String PREF_LASTUSER = "lastuser";
    private static final String ARG_USERS = "users";

    private AuditOData oData; //OData для доступа в 1С
    private SimpleAdapter usersAdapter; //Адаптер для списка пользователей
    private List<Map<String, Object>> users = new ArrayList<>(); //Список пользователей
    private SharedPreferences preferences;

    private Spinner user;
    private ProgressBar progressBar;
    private FloatingActionButton enter;
    private String lastUser;
    private boolean loaded = false;

    private int iPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Пока только меню
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Кнопка Вперед
        enter = findViewById(R.id.enter);
        enter.setOnClickListener(this);

        //Готовим все для спиннера
        usersAdapter = new SimpleAdapter(this, users, android.R.layout.simple_spinner_item,
                new String[]{USER_NAME}, new int[]{android.R.id.text1});
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        lastUser = preferences.getString(PREF_LASTUSER, AuditOData.EMPTY_KEY);

        user = findViewById(R.id.user);
        user.setAdapter(usersAdapter);
        user.setOnItemSelectedListener(this);

        //Неопределенный прогресс-бар движется во время загрузки (см. onCreateLoader, onLoadFinished, onLoaderReset)
        progressBar = findViewById(R.id.progressBar);

        //Открываем клиента
        oData = new AuditOData(this);

//        // ATTENTION: This was auto-generated to handle app links.
//        Intent appLinkIntent = getIntent();
//        String appLinkAction = appLinkIntent.getAction();
//        Uri appLinkData = appLinkIntent.getData();
    }

    /**
     * Для вывода служебного сообщения в спиннере на время загрузки и т.п.
     * @param message - текст служебного сообщения
     */
    private void setMessage(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put(USER_NAME, message);
        users.add(map);
        usersAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (loaded) {
            user.setSelection(iPosition);
            enter.setVisibility(View.VISIBLE);
        }
        else {
            //Создаем загрузчик для чтения данных
            final Loader loader = getSupportLoaderManager().getLoader(0);
            if (loader != null && !loader.isReset()) {
                getSupportLoaderManager().restartLoader(0, null, this);
            } else {
                getSupportLoaderManager().initLoader(0, null, this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsAudit.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Закрывает базу при закрытии активити
    protected void onDestroy() {
        super.onDestroy();
        oData = null;
        usersAdapter = null;
    }

    //Сохраняет текущее значение пароля перед поворотом экрана
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (loaded) {
//            outState.putParcelable(ARG_USER, user.onSaveInstanceState());
            final Parcelable[] parcelables = new Parcelable[users.size()];
            int i = 0;
            for (Map<String, Object> u: users) {
                parcelables[i++] = new ParcelableUser(u);
            }
            outState.putParcelableArray(ARG_USERS, parcelables);
        }

        outState.putInt("position", iPosition);
        outState.putString("password", ((EditText) findViewById(R.id.password)).getText().toString());
    }

    //Восстанавливает значение пароля после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final Parcelable[] parcelables = savedInstanceState.getParcelableArray(ARG_USERS);
        if (parcelables != null) {
            for (Parcelable parcelable: parcelables) {
                users.add(((ParcelableUser) parcelable).user);
            }
//            user.onRestoreInstanceState(savedInstanceState.getParcelable(ARG_USER));
            loaded = true;
            usersAdapter.notifyDataSetChanged();
        }

        iPosition = savedInstanceState.getInt("position");
        ((EditText) findViewById(R.id.password)).setText(savedInstanceState.getString("password", ""));
    }

    //Обработчик кнопки Вперед
    @Override
    public void onClick(View v) {
        //Нажата именно Вперед?
        if (v.getId()==R.id.enter) {
            //Текущие значения пароля
            final String password = users.get(iPosition).get(USER_PASSWORD).toString();

            EditText input = findViewById(R.id.password);
            //Пароль верный?
            if (equalsPassword(password, input.getText().toString())) {
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
                        users.get(iPosition).get(USER_NAME).toString()));
            }
            else {
                //Неверный пароль
                Snackbar.make(v, R.string.msg_password, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                input.setText("");
            }
        }
    }

    //Сравнивает пароли
    private boolean equalsPassword(String passwordText, String inputText) {
        char[] password = passwordText.toCharArray();
        char[] input = inputText.toCharArray();
        if (password.length != input.length) return false;
        for (int position = 0; position<password.length; position++ ) {
            if ((int) password[position] != ((int) input[position]*13-(position+1)*7)%873) return false;
        }
        return true;
    }

    //Очищает пароль после выбора пользователя
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (iPosition != position) {
            iPosition = position;
            ((EditText) findViewById(R.id.password)).setText("");

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @NonNull
    @Override
    public Loader<List<Map<String, Object>>> onCreateLoader(int id, @Nullable Bundle args) {
        progressBar.setVisibility(View.VISIBLE);
        setMessage("Идет загрузка...");
        return new MainActivity.MyLoader(this, oData);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Map<String, Object>>> loader, List<Map<String, Object>> data) {
        users.clear();
        users.addAll(data);
        if (users.isEmpty()) {
            setMessage("Список пуст...");
            usersAdapter.notifyDataSetChanged();
        }
        else {
            loaded = true;
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
            enter.setVisibility(View.VISIBLE);
        }
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Map<String, Object>>> loader) {
        users.clear();
        progressBar.setVisibility(View.INVISIBLE);
    }

    //Загрузчик списка пользователей
    static class MyLoader extends AsyncTaskLoader<List<Map<String, Object>>> {

        final AuditOData oData;
        final Activity activity;

        MyLoader(@NonNull Context context, @NonNull AuditOData oData) {
            super(context);
            this.activity = (Activity) context;
            this.oData = oData;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public List<Map<String, Object>> loadInBackground() {
            List<Map<String, Object>> usersMap = new ArrayList<>();
            try {
                usersMap.addAll(oData.getUsers());
            }
            catch (RuntimeException e) {
                if (!(activity == null || activity.isFinishing())) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //We find any view that has a CoordinatorLayout at the root.
                            // Example - toolbar
                            final View view = activity.findViewById(R.id.toolbar);
                            if (view != null) {
                                Snackbar.make(view, R.string.msg_odata_error, Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
            return usersMap;
        }
    }
}
//Фома 2018