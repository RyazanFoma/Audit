package com.example.eduardf.audit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

/*
 * *
 *  * Created by Eduard Fomin on 17.07.19 9:05
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 19.12.18 11:53
 *
 */

/**
 * Ввод комментария к медиафайлу
 */
public class DialogGallery extends DialogFragment {

    /**
     * Тэги диалога
     */
    public static final String TAG_COMMENT = "comment"; //Режим редактирования комментария

    //Аргументы
    private static final String ARG_POSITION = "position"; //Аргумент Позиция показателя
    private static final String ARG_TITLE = "title"; //Аргумент Заголовок
    private static final String ARG_COMMENT = "comment"; //Аргумент комментарий

    private static OnDialogGalleryListener mListener; //Обработчик нажатия Изменить

    private int position; //Позиция показателя, возращается в обработчик нажатия Изменить

    //Переменные для заполнения полей при создании фрагмента
    private String title;
    private String comment;

    /**
     * Признак начала процесса поворота экрана.
     * Устанавливается в true в {@link DialogIndicator#onSaveInstanceState(android.os.Bundle)}.
     * Измользуется в {@link DialogIndicator#onDestroyView()}
     */
    private boolean afterRotate = false;

    /**
     * Создание диалога для ввода комментария
     * @param context - текущй контекст с обработчиком нажатия Изменить
     * @param position - позиция показателя в задании
     * @param title - наименование показателя
     * @param comment - комментарий
     * @return - экземпляр диалогового фрагмента
     */
    static DialogGallery newInstance(Context context, int position, String title, String comment) {
        instanceOf(context);
        final DialogGallery f = new DialogGallery();
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position); //Позиция записи медиафайла
        args.putString(ARG_TITLE, title); //Наименование показателя
        args.putString(ARG_COMMENT, comment); //комментарий
        f.setArguments(args);
        return f;
    }

    /**
     * Проверка наличия обработчика кнопки Изменить в контексте
     * @param context - текущй контекст с обработчиком нажатия Изменить
     */
    static private void instanceOf(Context context) {
        if (context instanceof OnDialogGalleryListener) {
            mListener = (OnDialogGalleryListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DialogGallery.OnDialogGalleryListener");
        }
    }

    /**
     * Интерфейс обработчика кнопки Изменить, должен присутствовать в текущем контектсе
     */
    public interface OnDialogGalleryListener {
        /**
         * Вызывается при изменении комментария к медиафайлу в диалоге
         * @param position - позиция записи медиафайла
         * @param comment - комментарий
         */
        void onChangedComment(int position, String comment);
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
            comment = args.getString(ARG_COMMENT, "");
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
        final String tag = getTag();
        if (tag == null) throw new RuntimeException("Tag for DialogGallery not installed");
        switch (tag) {
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
                        mListener.onChangedComment(position, textComment.getText().toString());
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
                case TAG_COMMENT: {
                    final TextInputEditText textComment = view.findViewById(R.id.text_comment);
                    textComment.setText(comment);
                    textComment.setSelection(comment.length());
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
                case TAG_COMMENT: {
                    outState.putString(ARG_COMMENT,
                            ((TextInputEditText) getView().findViewById(R.id.text_comment)).
                                    getText().toString());
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
            final Fragment fragment = fragmentManager.findFragmentById(R.id.text_comment);
            if (fragment != null)
                fragmentManager.beginTransaction().remove(fragment).commit();
        }
        super.onDestroyView();
    }
}
//Фома2019
