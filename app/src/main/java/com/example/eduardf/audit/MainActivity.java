package com.example.eduardf.audit;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
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

import java.util.List;
import java.util.Map;

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

    AuditOData oData; //OData для доступа в 1С
    SimpleAdapter usersAdapter; //Адаптер для списка пользователей
    List<Map<String, Object>> usersMap; //Список пользователей для адаптера

    int iPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Пока только меню
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Кнопка Вперед
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        //Готовим все для спиннера
        usersMap = AuditOData.newDataSpinner("Идет загрузка...");
        usersAdapter = new SimpleAdapter(this, usersMap, android.R.layout.simple_spinner_item, new String[]{"name"}, new int[]{android.R.id.text1});
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner users = (Spinner) findViewById(R.id.user);
        users.setAdapter(usersAdapter);
        users.setOnItemSelectedListener(this);

        //Неопределенный прогресс-бар движется во время загрузки (см. onCreateLoader, onLoadFinished, onLoaderReset)
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Открываем клиента
        oData = new AuditOData(this);

        //Создаем загрузчик для чтения данных
        Loader loader = getSupportLoaderManager().getLoader(0);
        if (loader != null && !loader.isReset()) {
            getSupportLoaderManager().restartLoader(0, null, this);
        } else {
            getSupportLoaderManager().initLoader(0, null, this);
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
        outState.putInt("user", iPosition);
        outState.putString("password", ((EditText) findViewById(R.id.password)).getText().toString());
    }

    //Восстанавливает значение пароля после поворота экрана
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        iPosition = savedInstanceState.getInt("user");
        ((EditText) findViewById(R.id.password)).setText(savedInstanceState.getString("password", ""));
    }

    //Обработчик кнопки Вперед
    @Override
    public void onClick(View v) {
        //Нажата именно Вперед?
        if (v.getId()==R.id.fab) {
            //Текущие значения пароля
            final String password = usersMap.get(iPosition).get("password").toString();

            EditText input = (EditText) findViewById(R.id.password);
            //Пароль верный?
            if (equalsPassword(password, input.getText().toString())) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SettingTask.DEFAULT_TYPE,
                        usersMap.get(iPosition).get("type").toString());
                editor.putString(SettingTask.DEFAULT_ORGANIZATION,
                        usersMap.get(iPosition).get("organization").toString());
                editor.putString(SettingTask.DEFAULT_OBJECT,
                        usersMap.get(iPosition).get("object").toString());
                editor.putString(SettingTask.DEFAULT_RESPONSIBLE,
                        usersMap.get(iPosition).get("responsible").toString());
                editor.apply();
                //Открываем список заданий
                startActivity(TaskListActivity.intentActivity(this,
                        usersMap.get(iPosition).get("id").toString(),
                        usersMap.get(iPosition).get("name").toString()));
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
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
        return new MainActivity.MyLoader(this, oData);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Map<String, Object>>> loader, List<Map<String, Object>> data) {
        usersMap.clear();
        usersMap.addAll(data);
        usersAdapter.notifyDataSetChanged();
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Map<String, Object>>> loader) {
        usersMap.clear();
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
    }

    //Загрузчик списка пользователей
    static class MyLoader extends AsyncTaskLoader<List<Map<String, Object>>> {

        AuditOData oData;

        public MyLoader(@NonNull Context context, AuditOData oData) {
            super(context);
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
            return oData.getUsers();
        }
    }
}
//Фома 2018