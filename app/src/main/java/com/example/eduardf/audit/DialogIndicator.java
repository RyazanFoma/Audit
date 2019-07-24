package com.example.eduardf.audit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Locale;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 10.01.19 15:57
 *
 */

/**
 * Ввод значений: числового показателя, показателя с датой и комментария к показателю
 */
public class DialogIndicator extends DialogFragment {

    /**
     * Тэги диалога
     */
    public static final String TAG_NUMBER = "number"; //Режим редактирования числа
    public static final String TAG_DATE = "date"; //Режим редактирования даты
    public static final String TAG_COMMENT = "comment"; //Режим редактирования комментария

    //Аргументы
    private static final String ARG_POSITION = "position"; //Аргумент Позиция показателя
    private static final String ARG_TITLE = "title"; //Аргумент Заголовок
    private static final String ARG_NUMBER = "number"; //Аргумент Число
    private static final String ARG_UNIT = "unit"; //Аргумент ед. измерения
    private static final String ARG_DATE = "date"; //Аргумент Дата
    private static final String ARG_COMMENT = "comment"; //Аргумент комментарий

    private static DialogInteractionListener mListener; //Обработчик нажатия Изменить

    private int position; //Позиция показателя, возращается в обработчик нажатия Изменить

    //Переменные для заполнения полей при создании фрагмента
    private String title;
    private float number;
    private String comment;
    private String unit;
    private Date date;

    /**
     * Признак начала процесса поворота экрана.
     * Устанавливается в true в {@link DialogIndicator#onSaveInstanceState(android.os.Bundle)}.
     * Измользуется в {@link DialogIndicator#onDestroyView()}
     */
    private boolean afterRotate = false;

    /**
     * Создание диалога для ввода числа
     * @param context - текущй контекст с обработчиком нажатия Изменить
     * @param position - позиция показателя в задании
     * @param title - наименование показателя
     * @param number - значение числа
     * @param unit - единица изменения
     * @return - экземпляр диалогового фрагмента
     */
    static DialogIndicator newInstance(Fragment context, int position, String title, float number, String unit) {
        instanceOf(context);
        final DialogIndicator f = new DialogIndicator();
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position); //Позиция показателя
        args.putString(ARG_TITLE, title); //Наименование показателя
        args.putFloat(ARG_NUMBER, number); //Число
        if (!(unit==null || unit.isEmpty()))
            args.putString(ARG_UNIT, unit); //Наименование показателя
        f.setArguments(args);
        return f;
    }

    /**
     * Создание диалога для ввода комментария
     * @param context - текущй контекст с обработчиком нажатия Изменить
     * @param position - позиция показателя в задании
     * @param title - наименование показателя
     * @param comment - комментарий
     * @return - экземпляр диалогового фрагмента
     */
    static DialogIndicator newInstance(Fragment context, int position, String title, String comment) {
        instanceOf(context);
        final DialogIndicator f = new DialogIndicator();
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position); //Позиция показателя
        args.putString(ARG_TITLE, title); //Наименование показателя
        args.putString(ARG_COMMENT, comment); //комментарий
        f.setArguments(args);
        return f;
    }

    /**
     * Создание диалога для редактирования даты
     * @param context - текущй контекст с обработчиком нажатия Изменить
     * @param position - позиция показателя в задании
     * @param title - наименование показателя
     * @param date - значение даты
     * @return - экземпляр диалогового фрагмента
     */
    static DialogIndicator newInstance(Fragment context, int position, String title, Date date) {
        instanceOf(context);
        final DialogIndicator f = new DialogIndicator();
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position); //Позиция показателя
        args.putString(ARG_TITLE, title); //Наименование показателя
        if (date!=null)
            args.putLong(ARG_DATE, date.getTime());
        f.setArguments(args);
        return f;
    }

    /**
     * Проверка наличия обработчика кнопки Изменить в контексте
     * @param context - текущй контекст с обработчиком нажатия Изменить
     */
    static private void instanceOf(Fragment context) {
        if (context instanceof DialogInteractionListener) {
            mListener = (DialogInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DialogsIndicator.DialogInteractionListener");
        }
    }

    /**
     * Интерфейс обработчика кнопки Изменить, должен присутствовать в текущем контектсе
     */
    public interface DialogInteractionListener {
        void onChangedIndicatorValue(int position, Object value);
        void onChangedIndicatorComment(int position, String value);
    }

    /**
     * Создание диалога
     * @param savedInstanceState - среда для хранения
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final  Bundle args = savedInstanceState == null? getArguments(): savedInstanceState;
        if (args != null) {
            position = args.getInt(ARG_POSITION);
            title = args.getString(ARG_TITLE);
            number = args.getFloat(ARG_NUMBER, 0);
            comment = args.getString(ARG_COMMENT, "");
            unit = args.getString(ARG_UNIT, "");
            date = new Date();
            date.setTime(args.getLong(ARG_DATE, 0));
        }
    }

    /**
     * Создание вью диалога
     * @param inflater - выдуватель
     * @param container - контейтер для вью
     * @param savedInstanceState - среда для хранения
     * @return - вию диалога
     */
    @SuppressLint("ClickableViewAccessibility") //Чтобы для onTouch не возникало предупреждение о performClick
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        if (getTag() == null) throw new RuntimeException("Tag for DialogIndicator not installed");
        switch (getTag()) {
            case TAG_NUMBER: {
                view = inflater.inflate(R.layout.dialog_indicator_number, container, false);
                final TextInputLayout layoutValue = view.findViewById(R.id.layout_value);
                final TextInputEditText textValue = view.findViewById(R.id.text_value);
                //The listener of  a drawableEnd button for clear a TextInputEditText
                textValue.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(event.getAction() == MotionEvent.ACTION_UP) {
                            final TextView textView = (TextView)v;
                            if(event.getX() >= textView.getWidth() - textView.getCompoundPaddingEnd()) {
                                textView.setText(""); //Clear a view, example: EditText or TextView
                                return true;
                            }
                        }
                        return false;
                    }
                });
                textValue.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        layoutValue.setErrorEnabled(false);
                        layoutValue.setError(null);
                        return false;
                    }
                });
                if (!unit.isEmpty()) layoutValue.setHint(unit);
                layoutValue.setCounterMaxLength(getResources().getInteger(R.integer.max_length_number));
                //Обработчик нажатия на кнопку Изменить
                view.findViewById(R.id.positive).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        //Добавить проверку и сообщение об ошибке!!!
                        try {
                            mListener.onChangedIndicatorValue(position,
                                    Float.parseFloat(textValue.getText().toString()));
                            dismiss();
                        }
                        catch (NumberFormatException e) {
                            layoutValue.setErrorEnabled(true);
                            layoutValue.setError(getResources().getString(R.string.msg_input_number));
                        }
                    }
                });
                break;
            }
            case TAG_COMMENT: {
                view = inflater.inflate(R.layout.dialog_indicator_comment, container, false);
                final TextInputLayout layoutComment = view.findViewById(R.id.layout_comment);
                final TextInputEditText textComment = view.findViewById(R.id.text_comment);
                // Реализация кнопки очистить
                textComment.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(event.getAction() == MotionEvent.ACTION_UP) {
                            final TextView textView = (TextView)v;
                            if(event.getX() >= textView.getWidth() - textView.getCompoundPaddingEnd()) {
                                textView.setText("");
                                return true;
                            }
                        }
                        return false;
                    }
                });
                layoutComment.setCounterMaxLength(getResources().getInteger(R.integer.max_length_comment));
                view.findViewById(R.id.positive).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mListener.onChangedIndicatorComment(position, textComment.getText().toString());
                        dismiss();
                    }
                });
                break;
            }
            case TAG_DATE: {
                view = inflater.inflate(R.layout.dialog_indicator_date, container, false);
                view.findViewById(R.id.positive).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (getActivity() != null) {
                            final DateTime dateTime = (DateTime) getActivity().
                                    getSupportFragmentManager().findFragmentById(R.id.fragment_date);
                            mListener.onChangedIndicatorValue(position, dateTime.getDate());
                        }
                        dismiss();
                    }
                });
                break;
            }
            default:
                throw new RuntimeException("The is unknown tag for a DialogIndicator");
        }
        view.findViewById(R.id.negative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        afterRotate = false;
        return view;
    }

    /**
     * Заполнение вью диалога значениями
     * @param view - вью диалога
     * @param savedInstanceState - среда для хранения
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.title)).setText(title);
        final String tag = getTag();
        if (tag != null) {
            switch (tag) {
                case TAG_NUMBER: {
                    final TextInputEditText textValue = view.findViewById(R.id.text_value);
                    final String text = (number == (long)number)?
                            String.format(Locale.US,"%d", (long) number):
                            String.format(Locale.US,"%s", number);
                    textValue.setText(text);
                    textValue.setSelection(text.length());
                    break;
                }
                case TAG_COMMENT: {
                    final TextInputEditText textComment = view.findViewById(R.id.text_comment);
                    textComment.setText(comment);
                    textComment.setSelection(comment.length());
                    break;
                }
                case TAG_DATE: {
                    if (getActivity() != null) {
                        final DateTime dateTime = (DateTime) getActivity().
                                getSupportFragmentManager().findFragmentById(R.id.fragment_date);
                        dateTime.setDate(date);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Сохранение значений полей перед поворотом экрана
     * @param outState - среда для хранения
     */
    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_POSITION, position);
        outState.putString(ARG_TITLE, title);
        final String tag = getTag();
        if (!(tag == null || getView() == null))
            switch (tag) {
                case TAG_NUMBER: {
                    outState.putFloat(ARG_NUMBER, Float.parseFloat(
                            ((TextInputEditText) getView().findViewById(R.id.text_value)).
                                    getText().toString()));
                    outState.putString(ARG_UNIT, unit);
                    break;
                }
                case TAG_COMMENT: {
                    outState.putString(ARG_COMMENT,
                            ((TextInputEditText) getView().findViewById(R.id.text_comment)).
                                    getText().toString());
                    break;
                }
                case TAG_DATE: {
                    if (getActivity() != null) {
                        final DateTime dateTime = (DateTime) getActivity().
                                getSupportFragmentManager().findFragmentById(R.id.fragment_date);
                        outState.putLong(ARG_DATE, dateTime.getDate().getTime());
                    }
                    break;
                }
            }
        afterRotate = true; //Поворот начался
    }

    /**
     * Удаление фрагмента, чтобы при последующем открытии не было ошибок вдувания
     */
    @Override
    public void onDestroyView () {
        if (!(afterRotate || getActivity() == null)) {
            final FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            final String tag = getTag();
            int id = -1;
            if (tag != null) {
                switch (tag) {
                    case TAG_NUMBER:
                        id = R.id.text_value;
                        break;
                    case TAG_COMMENT:
                        id = R.id.text_comment;
                        break;
                    case TAG_DATE:
                        id = R.id.fragment_date;
                        break;
                }
            }
            final Fragment fragment = fragmentManager.findFragmentById(id);
            if (fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
        super.onDestroyView();
    }
}
//Фома2018
