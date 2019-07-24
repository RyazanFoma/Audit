package com.bit.eduardf.audit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 24.01.19 15:34
 *
 */

/**
 * Установка индивидуальных настроек пользователя
 */
public class SettingTask extends AppCompatActivity{

    //Объект OData для доступа к 1С:Аудитор
    private AuditOData oData;

    //giud пользователя
    private String auditiorKey;

    //Поля для выбора настроек
    private Reference typeReference;
    private Reference organizationReference;
    private Reference objectReference;
    private Reference responsibleReference;

    //Ключи предпочтений
    final static String DEFAULT_TYPE = "default_type";
    final static String DEFAULT_ORGANIZATION = "default_organization";
    final static String DEFAULT_OBJECT = "default_object";
    final static String DEFAULT_RESPONSIBLE = "default_responsible";

    //Аргументы интент и поворота экрана
    private final static String ARG_AUDITOR = "auditor";

    /**
     * Создание интент для открытия индивидуальных настроек пользователя
     * @param context - текущий констект
     * @param auditor - guid аудитора
     * @return - интент активности
     */
    public static Intent intentActivity(Context context, String auditor) {
        Intent intent = new Intent(context, SettingTask.class);
        intent.putExtra(ARG_AUDITOR, auditor);
        return intent;
    }

    /**
     * Вызывается при создании активности
     * @param savedInstanceState - среда для хранения
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_task);

        //Лента инструментов
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true); //Отловит onSupportNavigateUp
        }

        //Если экран большой, то ограничим поля по ширине
        if (isLargeTablet(this)) {
            final LinearLayout linearLayout = findViewById(R.id.setting);
            final LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(
                            getResources().getDimensionPixelSize(R.dimen.min_width_grandpaterns),
                            LinearLayout.LayoutParams.MATCH_PARENT);
            linearLayout.setLayoutParams(layoutParams);
        }

        //Создает объект OData
        oData = new AuditOData(this);

        //Обработчик изменения поля
        Reference.OnChangedReferenceKey onChangedReferenceKey = new Reference.OnChangedReferenceKey() {
            @Override
            public void onChangedKey(int id, String key, Object object) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingTask.this);
                final SharedPreferences.Editor editor = preferences.edit();
                switch (id) {
                    case R.id.type:
                        if (key==null || !preferences.getString(SettingTask.DEFAULT_TYPE, AuditOData.EMPTY_KEY).equals(key)) {
                            objectReference.setKey(null);
                            objectReference.setEnabled(key!=null);
                            editor.putString(SettingTask.DEFAULT_OBJECT, AuditOData.EMPTY_KEY);
                        }
                        if(object!=null) {
                            objectReference.setParentTypes(((AType)object).objectTypes);
                        }
                        editor.putString(SettingTask.DEFAULT_TYPE, key);
                        break;
                    case R.id.organization:
                        editor.putString(SettingTask.DEFAULT_ORGANIZATION, key);
                        break;
                    case R.id.object:
                        editor.putString(SettingTask.DEFAULT_OBJECT, key);
                        break;
                    case R.id.responsible:
                        editor.putString(SettingTask.DEFAULT_RESPONSIBLE, key);
                        break;
                    default:
                        throw new RuntimeException("Invalid id of Reference.onReferenceSelectedListener");
                }
                editor.apply();
                new SaveUser(oData).execute(auditiorKey,
                        typeReference.getReferenceKey(),
                        organizationReference.getReferenceKey(),
                        objectReference.getReferenceKey(),
                        responsibleReference.getReferenceKey());
            }
        };

        //Находим ссылки на поля настроек
        typeReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.type);
        typeReference.setOnChangedReferenceKey(onChangedReferenceKey);
        organizationReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.organization);
        organizationReference.setOnChangedReferenceKey(onChangedReferenceKey);
        objectReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.object);
        objectReference.setOnChangedReferenceKey(onChangedReferenceKey);
        responsibleReference = (Reference) getSupportFragmentManager().findFragmentById(R.id.responsible);
        responsibleReference.setOnChangedReferenceKey(onChangedReferenceKey);

        if (savedInstanceState==null) { // открываем форму впервые
            auditiorKey = getIntent().getStringExtra(ARG_AUDITOR);
            //Загружаем guid из предпочтений. Настраиваем фрагменты
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            typeReference.setKey(preferences.getString(DEFAULT_TYPE, AuditOData.EMPTY_KEY));
            organizationReference.setKey(preferences.getString(DEFAULT_ORGANIZATION, AuditOData.EMPTY_KEY));
            objectReference.setKey(preferences.getString(DEFAULT_OBJECT, AuditOData.EMPTY_KEY));
            responsibleReference.setKey(preferences.getString(DEFAULT_RESPONSIBLE, AuditOData.EMPTY_KEY));
        } else { // открываем после поворота
            auditiorKey = savedInstanceState.getString(ARG_AUDITOR);
        }
    }

    /**
     * Определение размера экрана устройства
     * @param context - текущий контекст актисности
     * @return - true - если экран большой или еще больше
     */
    private static boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Вызывается перед поворотом экрана
     * @param outState - среда для хранения
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_AUDITOR, auditiorKey);
    }

    //Обработчик возврата назад
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Вызывается при уничтожении активности
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        oData = null;
    }

}
//Фома2018