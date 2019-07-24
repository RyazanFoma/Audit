package com.bit.eduardf.audit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 17.12.18 10:08
 *
 */

/**
 * Фрагмент для просмотра и редактирования (добавления/удаления) списка аналитики объекта
 */
public class Analytics extends Fragment implements
        ReferenceChoice.OnReferenceManagerInteractionMultipleChoice,
        LoadItems.OnLoadItems,
        LoadAnalyticTypes.OnLoadAnalyticTypes {

    private AuditOData oData; //1С:Предприятие
    private AuditOData.Set set; //Таблица справочника
    private ArrayList<String> parentTypes; //Типы родительских справочников
    private String typeKey; //Guid вида аудита для отбора по ручной связи
    private String objectKey; //Guid объекта для отбора по ручной связи
    private String title; //Заголовой поля
    private AnalyticsAdapter recyclerAdapter; //Адаптер для списка
    private boolean enabled = true; //Признак доступности списка для редактирования

    private static String ARG_ENABLED = "enabled";
    private static String ARG_PARENTTYPES = "parenttypes";
    private static String ARG_TYPEKEY = "typekey";
    private static String ARG_OBJECTKEY = "objectkey";

    /**
     * Пустой конструктор. Данные получаем из XML и методов
     */
    public Analytics() {}

    /**
     * Вызывается при раздувании фрагмента из XML
     * @param context - текущий констекст
     * @param attrs - XML-аттрибуты
     * @param savedInstanceState - среда для хранения
     */
    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        final String namespace = "http://schemas.android.com/apk/res-auto";
        final String setAttr = attrs.getAttributeValue(namespace,"set");
        final String titleAttr = attrs.getAttributeValue(namespace,"title");
        if (setAttr != null) {
            set = AuditOData.Set.toValue(setAttr);
        } else {
            throw new RuntimeException("Not found 'set' attribute for Analytics fragment");
        }
        if (titleAttr != null) {
            title = titleAttr;
        } else {
            throw new RuntimeException("Not found 'title' attribute for Analytics fragment");
        }
    }

    /**
     * Загрузка текущего содержания списка
     * @param guids - список guid элементов справочника
     */
    void setKeys(List<String> guids) {
        recyclerAdapter.clear();
        if (!(guids == null || guids.isEmpty())) {
            (new LoadItems(this, oData, set)).execute(guids.toArray(new String[0]));
        }
    }

    /**
     * Получение текущего содержания списка
     * @return - список guid элементов справочника
     */
    @NotNull
    ArrayList<String> getKeys() {
        return recyclerAdapter.getItems();
    }

    /**
     * Очищение списка типов родительских справочников для отбора аналитик по типам
     */
    void setParentTypes() {
        parentTypes = null;
    }

    /**
     * Загрузка типов родительских справочников для отбора аналитики по типам
     * @param type - guid вида аудита
     * @param objectType - тип объекта аудита
     */
    void setParentTypes(@NonNull String type, @NonNull String objectType) {
        (new LoadAnalyticTypes(this, oData)).execute(new String[] {type, objectType});
    }

    /**
     * Очищение типа и объекта аудита для отбора аналитики по ручной связи
     */
    void setTypeObject() {
        setTypeObject(null, null);
    }

    /**
     * Установка типа и объекта аудита для отбора аналитики по ручной связи
     */
    void setTypeObject(String typeKey, String objectKey) {
        this.typeKey = typeKey;
        this.objectKey = objectKey;
    }

    /**
     * Включает/выключает доступность фрагмента для редактирования
     * @param enabled - признак включить/выключить
     */
    void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        final View view = getView();
        if (view != null) {
            if (recyclerAdapter != null) recyclerAdapter.setEnabled(enabled);
            final ImageButton add = view.findViewById(R.id.add);
            add.setEnabled(enabled);
            add.getDrawable().mutate().setColorFilter(enabled? null: new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));
        }
    }

    /**
     * Вызывается при создании фрагмента
     * @param savedInstanceState - среда для сохранения
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recyclerAdapter = new AnalyticsAdapter();
        if (savedInstanceState != null) {
            recyclerAdapter.onRestoreInstanceState(savedInstanceState);
            parentTypes = savedInstanceState.getStringArrayList(ARG_PARENTTYPES);
            typeKey = savedInstanceState.getString(ARG_TYPEKEY);
            objectKey = savedInstanceState.getString(ARG_OBJECTKEY);
        }
    }

    /**
     * Вызывается при создании вью фрагмента
     * @param inflater - для раздувания
     * @param container - группа, где будет размещен фрагмент
     * @param savedInstanceState - среда для хранения
     * @return - вью фрагмента
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_object_list, container, false);
    }

    /**
     * Вызывается после создания вью фрагмента
     * @param view - вью фрагмента
     * @param savedInstanceState - среда хранения
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Кнопка добавить пункты
        view.findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typeKey==null)
                    startActivity(ReferenceChoice.intentActivity(getActivity(), Analytics.this,
                            -1, set, title, null,
                            parentTypes, getKeys()));
                else
                    startActivity(ReferenceChoice.intentActivity(getActivity(), Analytics.this,
                            -1, set, title,null,
                            typeKey, objectKey, getKeys()));
            }
        });
        //Расчитываем кол-во колонок для Grid и создаем GridLayoutManager для рециклервью
        final Activity activity = getActivity();
        int spanCount = 1;
        if (activity != null) {
            final DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            spanCount = Math.max(1, Math.round(((float) metrics.widthPixels) /
                    getResources().getDimension(R.dimen.min_column_reference)));
        }

                //Настраиваем рециклервью
        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setAdapter(recyclerAdapter);
        if (savedInstanceState != null) {
            enabled = savedInstanceState.getBoolean(ARG_ENABLED, true);
            setEnabled(enabled);
        }
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), spanCount));
    }

    /**
     * Вызывается перед поворотом экрана
     * @param outState - среда хранения
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        recyclerAdapter.onSaveInstanceState(outState);
        outState.putBoolean(ARG_ENABLED, enabled);
        outState.putStringArrayList(ARG_PARENTTYPES, parentTypes);
        outState.putString(ARG_TYPEKEY, typeKey);
        outState.putString(ARG_OBJECTKEY, objectKey);
    }

    /**
     * Вызывается при присоединении фрагмента в активности
     * @param context - констекст активности
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        oData = new AuditOData(context);
    }

    /**
     * Вызывается при удалении фрагмента
     */
    @Override
    public void onDetach() {
        super.onDetach();
        oData = null;
    }

    /**
     * Вызывается после множественного выбора пунктов
     * @param requestCode - уникальный код
     * @param items - выбранные пункты
     */
    @Override
    public void onReferenceManagerInteractionMultipleChoice(int requestCode, Items items) {
        recyclerAdapter.addItems(items);
    }

    /**
     * Вызывается перед загрузкой пунктов
     */
    @Override
    public void onPreLoadItems() {
        final View view = getView();
        if (view != null)
            view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     * Вызывается при загрузке каждого пункта
     * @param item - пункт
     */
    @Override
    public void onLoadedItem(Items.Item item) {
        recyclerAdapter.addItem(item);
    }

    /**
     * Вызывается после загрузки всех пунктов
     * @param items - загруженные пункты
     */
    @Override
    public void onPostLoadItems(Items items) {
        final View view = getView();
        if (view != null)
            view.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    /**
     * Вызывается перед загрузкой типов аналитик для отбора по виду аудита и типу объекта
     */
    @Override
    public void onLoadAnalyticTypesPreExecute() {
        final View view = getView();
        if (view != null)
            view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     * Вызывается после загрузки типов аналитик для отбора по виду аудита и типу объекта
     */
    @Override
    public void onLoadAnalyticTypesPostExecute(ArrayList<String> parentTypes) {
        this.parentTypes = parentTypes;
        final View view = getView();
        if (view != null)
            view.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

}
//Фома2018