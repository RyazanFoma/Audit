package com.example.eduardf.audit;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;

//Диалог для изменения наименования группы
public class DialogsReferenceManager extends DialogFragment {

    public static final String TAG_EDIT = "edit"; //Режим редактирования группы
    public static final String TAG_CREATE = "create"; //Режим создания группы
    public static final String TAG_DELETE = "delete"; //Режим удаления

    private static final String ARG_NAME = "name"; //Аргумент Наименование
    private static final String ARG_COUNT = "count"; //Аргумент Количество
    private static final String ARG_START = "start"; //Аргумент Начало выделения
    private static final String ARG_END = "end"; //Аргумент Конец выделения
    private static DialogInteractionListener mListener; //Обработчик нажатия Изменить
    private EditText name; //Текстовое поле с наименованием

    //Создает диалог для редактирвоания группы
    static DialogsReferenceManager newInstance(Context context, String name) {
        instanceOf(context);
        DialogsReferenceManager f = new DialogsReferenceManager();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name); //Старое имя группы
        f.setArguments(args);
        return f;
    }

    //Создает диалог для создания группы
    static DialogsReferenceManager newInstance(Context context) {
        instanceOf(context);
        DialogsReferenceManager f = new DialogsReferenceManager();
        return f;
    }

    //Создает диалог для удаления помеченных объектов
    static DialogsReferenceManager newInstance(Context context, int count) {
        instanceOf(context);
        DialogsReferenceManager f = new DialogsReferenceManager();
        Bundle args = new Bundle();
        args.putInt(ARG_COUNT, count); //Количество объектов для удаления
        f.setArguments(args);
        return f;
    }

    //Проверяем наличие интеракшин в родительском классе
    static private void instanceOf(Context context) {
        if (context instanceof DialogsReferenceManager.DialogInteractionListener) {
            mListener = (DialogsReferenceManager.DialogInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DialogsReferenceManager.DialogInteractionListener");
        }
    }

    //Интерфейс для нажатия кнопки Изменить. Должен присутствовать в родительском классе
    public interface DialogInteractionListener {
        public void onEditGroupPositiveClick(String name);
        public void onCreatGroupPositiveClick(String name);
        public void onDeletePositiveClick();
        public void onDeleteNegativeClick();
    }

    //Строит диалог
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        switch (getTag()) {
            case TAG_DELETE:
                String message = getString(R.string.msg_delete_n);
                builder.setTitle(R.string.ttl_delete)
                        .setIcon(R.drawable.ic_black_delete_sweep_24px)
                        .setMessage(message.replaceFirst("…", ""+getArguments().getInt(ARG_COUNT)))
                        .setNegativeButton(R.string.btn_unmark_delete, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == Dialog.BUTTON_NEGATIVE) {
                                    mListener.onDeleteNegativeClick();
                                }
                            }
                        })
                        .setPositiveButton(R.string.btn_mark_delete, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == Dialog.BUTTON_POSITIVE) {
                                    mListener.onDeletePositiveClick();
                                }
                            }
                        });
                break;
            case TAG_CREATE:
                //Поле для ввода наименования
                name = new EditText(getActivity());
                name.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
                name.setHint(R.string.msg_enter_name);
                if (savedInstanceState!=null) { //после поворота экрна
                    name.setText(savedInstanceState.getString(ARG_NAME));
                    name.setSelection(savedInstanceState.getInt(ARG_START), savedInstanceState.getInt(ARG_END));
                }
                builder.setTitle(R.string.ttl_create_group)
                        .setIcon(R.drawable.ic_black_add_circle_outline_24px)
                        .setView(name)
                        .setPositiveButton(R.string.btn_addition, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == Dialog.BUTTON_POSITIVE && name.getText().length() > 0) {
                                    mListener.onCreatGroupPositiveClick(name.getText().toString());
                                }
                            }
                        });
                break;
            case TAG_EDIT:
                //Поле для ввода наименования
                name = new EditText(getActivity());
                name.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
                name.setHint(R.string.msg_enter_name);
                if (savedInstanceState==null) { //при первом создании
                    name.setText(getArguments().getString(ARG_NAME));
                }
                else { //после поворота экрна
                    name.setText(savedInstanceState.getString(ARG_NAME));
                    name.setSelection(savedInstanceState.getInt(ARG_START), savedInstanceState.getInt(ARG_END));
                }
                builder.setTitle(R.string.ttl_edit_group)
                        .setIcon(R.drawable.ic_black_edit_24px)
                        .setView(name)
                        .setPositiveButton(R.string.btn_change, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == Dialog.BUTTON_POSITIVE && name.getText().length() > 0) {
                                    mListener.onEditGroupPositiveClick(name.getText().toString());
                                }
                            }
                        });
                break;
        }
        builder.setNeutralButton(R.string.btn_cancel, null);
        return builder.create();
    }

    //Перед поворотом экрана
    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_NAME, name.getText().toString());
        outState.putInt(ARG_START, name.getSelectionStart());
        outState.putInt(ARG_END, name.getSelectionEnd());
    }
}

