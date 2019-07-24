package com.bit.eduardf.audit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 03.08.18 14:22
 *
 */

//Класс для выбора даты через DatePickerDialog
public class DatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private OnDateInteractionListener mListener = null;

    //Пустой констуктор
    public DatePicker() {}

    //создает диалог в DatePickerDialog
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // определяем текущую дату
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // создаем DatePickerDialog и возвращаем его
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    //Запоминаем обработчик изменения даты
    public void setOnDateInteractionListener(OnDateInteractionListener listener) {
        mListener = listener;
    }

    // вызывается при выборе даты
    @Override
    public void onDateSet(android.widget.DatePicker datePicker, int year, int month, int day) {
        if (mListener!=null) {
            final Calendar c = Calendar.getInstance();
            c.set(year, month, day);
            mListener.onDateInteractionListener(c.getTime());
        }
    }

    //Интейфейс обработчика изменения даты
    public interface OnDateInteractionListener {
        void onDateInteractionListener(Date date);
    }
}
