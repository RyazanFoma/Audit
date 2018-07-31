package com.example.eduardf.audit;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, View.OnClickListener, Spinner.OnItemSelectedListener {

    AuditDB db;
    SimpleCursorAdapter scAdapter;
    ProgressDialog pd;
    int iPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Создаем прогресс для ожидания загрузки
        pd = new ProgressDialog(this);
        pd.setTitle(R.string.progress_title);
        pd.setMessage(getResources().getString(R.string.progress_msq));

        //Пока только меню
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Кнопка Вперед
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // открываем подключение к БД
        db = new AuditDB(this);
        db.open();

        //Спиннер для выбора пользователя
        scAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null, new String[] { AuditDB.NAME}, new int[] { android.R.id.text1 }, 0);
        scAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner users = (Spinner) findViewById(R.id.user);
        users.setAdapter(scAdapter);
        users.setOnItemSelectedListener(this);

        // создаем загрузчик для чтения данных
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Закрывает базу при закрытии активити
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
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

            //Текущие значения пароля и идентификатора пользователя
            Cursor c = scAdapter.getCursor();
            String psw = c.getString(c.getColumnIndex(AuditDB.USER_PASSWORD));
            int id = c.getInt(c.getColumnIndex(AuditDB.ID));
            EditText tv = (EditText) findViewById(R.id.password);
            //Пароль верный?
            if (psw.equals(tv.getText().toString())) {
                //Открываем список заданий
                Intent intent = new Intent(this, TaskList.class);
                intent.putExtra("Auditor", id);
                startActivity(intent);
            }
            else {
                //Неверный пароль
                Snackbar.make(v, R.string.msg_password, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                tv.setText("");
            }
        }
    }

    //Очищает пароль после выбора пользователя
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (iPosition != position) {
            iPosition = position;
            ((EditText) findViewById(R.id.password)).setText("");
        }
    }

    //Загрузчик для спиннера
    @Override
    public  Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        pd.show();
        return new MainActivity.MyCursorLoader(this, db);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        pd.dismiss();
        scAdapter.swapCursor(cursor);
        ((Spinner) findViewById(R.id.user)).setSelection(iPosition, true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        scAdapter.swapCursor(null);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //Мой загрузчик для Спинера по таблице пользователей
    static class MyCursorLoader extends CursorLoader {

        AuditDB db;

        public MyCursorLoader(Context context, AuditDB db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {
            return db.getAllData(AuditDB.TBL_USER);
        }

    }
}
//Фома 2018