package com.bit.eduardf.audit;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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

    private static final String ARG_MESSAGE = "message"; //Аргумент Наименование

    /**
     * Создает новый объект с диалогом
     * @param code - код ошибки, можно использовать для анализа
     * @param message - текстовое описание ошибки
     * @return - новый объект с диалога для последующего .show()
     */
    static DialogODataError newInstance(String code, String message) {
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

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.ttl_odata_error)
                .setIcon(R.drawable.ic_black_error_24px)
                .setMessage(getArguments().getString(ARG_MESSAGE))
                .setPositiveButton(R.string.btn_ok, null);
        return builder.create();
    }
}
//Фома2018