package com.example.eduardf.audit;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 01.02.19 13:29
 *
 */

/**
 * Фрагмент с полем для выбора элемента справочника
 */
public class Reference extends Fragment implements
        ReferenceChoice.OnReferenceManagerInteractionSingleChoice,
        LoadObject.OnLoadObject {

    //Аргументы для поворота экрана
    private static final String ARG_OWNER = "owner";
    private static final String ARG_KEY = "key";
    private static final String ARG_OBJECT = "object";
    private static final String ARG_ENABLED = "enabled";

    private AuditOData.Set set; //Таблица справочника
    private String title; //Заголовой поля
    private String owner = null; //Giud собственника
    private String key = null; //Guid выбранного элемента
    private Object object = null; //Выбранный элемент
    private boolean enabled = true; //Признак доступности редактирования
    private EditText viewName; //Поле с наименованием выбранного элемента
    private boolean afterRotation; //true - признак прошедшего поворота, false - после фокуса на поле с наименованием
    private OnChangedReferenceKey onChangedReferenceKey; //Обработчик изменения значения поля

    private AuditOData oData;

    //Пустой конструктор. Данные получаем из XML и методов
    public Reference() {}

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
            throw new RuntimeException("Not found 'set' attribute for Reference fragment");
        }
        if (titleAttr != null) {
            title = titleAttr;
        } else {
            throw new RuntimeException("Not found 'title' attribute for Reference fragment");
        }
    }

    /**
     * Установка текущего выбранного элемента справочника по guid
     * @param guid - guid элемента справочника
     */
    void setKey(String guid) {
        if (guid == null) {
            key = null;
            object = null;
            setText("");
            if (onChangedReferenceKey != null) onChangedReferenceKey.onChangedKey(getId(), key, object);
        } else if (!guid.equals(key))
            new LoadObject(this, oData, set).execute(key = guid);
    }

    /**
     * Установка текста в поле
     * @param name - наименование элемента справочника
     */
    void setText(String name) {
        if (viewName != null && name != null) viewName.setText(name);
    }

    /**
     * Получение текущего выбранного элемента справочника
     * @return - guid элемента справочника
     */
    String getReferenceKey() {
        return key;
    }

//    /**
//     * Установка владельца справочника
//     * @param guid - guid владельца
//     */
//    void setReferenceOwner(String guid) {
//        owner = guid;
//    }

    /**
     * Включает/выключает доступность фрагмента фрагмент
     * @param enabled - признак включить/выключить
     */
    void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        final View view = getView();
        if (view != null) {
            view.findViewById(R.id.input_layout_name).setEnabled(enabled);
            view.findViewById(R.id.input_name).setEnabled(enabled);
            view.findViewById(R.id.clear_name).setEnabled(enabled);
        }
    }

    /**
     * Устанавливает обработчик изменения значения поля
     * @param onChangedReferenceKey - обработчик
     */
    void setOnChangedReferenceKey(OnChangedReferenceKey onChangedReferenceKey) {
        this.onChangedReferenceKey = onChangedReferenceKey;
    }

    @Override
    public void onPreLoadObject() {
        if (getView() != null) getView().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostLoadObject(Object object) {
        this.object = object;
        String name;
        switch (set) {
            case TYPE:
                key = ((AType) object).id;
                name = ((AType) object).name;
                break;
            case OBJECT:
                key = ((AObject) object).id;
                name = ((AObject) object).name;
                break;
            default:
                key = ((Items.Item) object).id;
                name = ((Items.Item) object).name;
        }
        setText(name);
        if (onChangedReferenceKey != null) onChangedReferenceKey.onChangedKey(getId(), key, object);
        if (getView() != null) getView().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    /**
     * Интерфейс обработчика изменения значения поля
     */
    interface OnChangedReferenceKey{
        /**
         * Обработчик
         * @param id - идентификатор поля
         * @param key - новое значение поля
         * @param object - новое значение объекта
         */
        void onChangedKey(int id, String key, Object object);
    }

    /**
     * Вызывается при создании фрагмента
     * @param savedInstanceState - среда для хранения
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            key = savedInstanceState.getString(ARG_KEY);
            owner = savedInstanceState.getString(ARG_OWNER);
            enabled = savedInstanceState.getBoolean(ARG_ENABLED, true);
            if (savedInstanceState.containsKey(ARG_OBJECT))
                switch (set) {
                    case TYPE: {
                        final AType object = new AType();
                        object.onRestoreInstanceState(savedInstanceState, ARG_OBJECT);
                        this.object = object;
                        break;
                    }
                    case OBJECT: {
                        final AObject object = new AObject();
                        object.onRestoreInstanceState(savedInstanceState, ARG_OBJECT);
                        this.object = object;
                        break;
                    }
                    default: {
                        ParcelableItem parcelable = savedInstanceState.getParcelable(ARG_OBJECT);
                        if (parcelable != null) object = parcelable.item;
                    }
                }
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
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reference_field, container, false);
    }

    /**
     * Вызывается после создания вью фрагмента для его заполнения
     * @param view - вью фрагмента
     * @param savedInstanceState - среда для хранения
     */
    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        afterRotation = savedInstanceState!=null;
        ((TextInputLayout) view.findViewById(R.id.input_layout_name)).setHint(title);
        viewName = view.findViewById(R.id.input_name);
        viewName.setKeyListener(null);
        //Обработчики открытия выбора значения из справочника
        viewName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(ReferenceChoice.intentActivity(Reference.this,
                        set, title, owner, key));
            }
        });
        viewName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    if (!afterRotation) //Чтобы после поворота не открывалась активность выбора
                        startActivity(ReferenceChoice.intentActivity(Reference.this,
                                set, title, owner, key));
                    else
                        afterRotation = false;
            }
        });
        //Обработчик кнопки очистить
        (view.findViewById(R.id.clear_name)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setKey(null);
            }
        });
        setEnabled(enabled);
    }

    /**
     * Сохранение переменных перед поворотом экрана
     * @param outState - среда хранения
     */
    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_KEY, key);
        outState.putString(ARG_OWNER, owner);
        outState.putBoolean(ARG_ENABLED, enabled);
        if (object != null)
            switch (set) {
                case TYPE:
                    ((AType) object).onRestoreInstanceState(outState, ARG_OBJECT);
                    break;
                case OBJECT:
                    ((AObject) object).onRestoreInstanceState(outState, ARG_OBJECT);
                    break;
                default:
                    outState.putParcelable(ARG_OBJECT, new ParcelableItem((Items.Item) object));
            }
    }

    //Обработчик выбора элемента в активности ReferenceChoice

    /**
     * Обрабатывает выбор элемента справочника
     * @param code - уникальный код для идентификации активности
     * @param item - выбранный элемент
     */
    @Override
    public void onReferenceManagerInteractionListenerSingleChoice(int code, Items.Item item) {
        //При вызове из фрагмента code будет всегда -1. Скрипач не нужен
        switch (set) {
            case TYPE: case OBJECT:
                setText(item.name);
                setKey(item.id);
                break;
            default:
                object = item;
                key = item.id;
                setText(item.name);
                if (onChangedReferenceKey != null) onChangedReferenceKey.onChangedKey(getId(), key, object);
        }
    }

    //вызывается при присоединении фрагмента
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
        onChangedReferenceKey = null;
    }

}
//Фома2018