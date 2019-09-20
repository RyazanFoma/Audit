package com.bit.eduardf.audit;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 04.10.18 13:01
 *
 */

/**
 * Класс - сообщение об ошибке
 */
public class DialogODataError extends DialogFragment {

    private static final String ARG_MESSAGE = "message"; //Аргумент сообщение
    private static OnSelected onSelected; //Контекст с обработчиком кнопок

    /**
     * Создает новый экземпляр объекта с диалогом
     * @param oData - контекст с обработчиками кнопок диалога
     * @param message - текстовое описание ошибки
     * @return - новый объект с диалога для последующего .show()
     */
    static DialogODataError newInstance(@NonNull AuditOData oData, String message) {
        onSelected = oData;
        DialogODataError fragment = new DialogODataError();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    //Строит диалог
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) /*dismiss();*/ dismissAllowingStateLoss();
        final FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            final Bundle arguments = getArguments();
            if (arguments != null) {
                final String message = arguments.getString(ARG_MESSAGE, "");
                final AlertDialog.Builder builder = new AlertDialog.Builder(fragmentActivity);
                builder.setTitle(R.string.ttl_odata_error)
                        .setIcon(R.drawable.ic_black_error_24px)
                        .setMessage(message+"\n\n"+getString(R.string.msg_repeat))
                        .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onSelected.onCancelSelected();
                            }
                        })
                        .setPositiveButton(R.string.btn_repeat, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onSelected.onRepeatSelected();
                            }
                        });
                return builder.create();
            }
        }
        throw new RuntimeException("Incorrect DialogODataError parameters");
    }

    /**
     * Интерфейс обработчиков кнопок
     */
    public interface OnSelected {
        void onCancelSelected(); //Прервать
        void onRepeatSelected(); //Повторить
    }

//    public void onAttach (Context context) {
//        super.onAttach(context);
//    }
//
//    public void onDestroy () {
//        super.onDestroy();
//    }

}
//Фома2018