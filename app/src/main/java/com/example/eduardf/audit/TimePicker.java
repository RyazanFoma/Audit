package com.example.eduardf.audit;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 03.08.18 14:28
 *
 */

//Класс для выбора даты через DatePickerDialog
public class TimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    //Обработчик выбора времени
    private OnTimeInteractionListener mListener = null;

    //Пустой констуктор
    public TimePicker() {}

    //создает диалог с TimePickerDialog
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // определяем текущее время
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);

        // создаем TimePickerDialog и возвращаем его
        return new TimePickerDialog(getActivity(), this, hour, minute, false);
    }

    //Запоминаем обработчик изменения времени
    public void setOnTimeInteractionListener(OnTimeInteractionListener listener) { mListener = listener; }

    // вызывается при выборе времени
    @Override
    public void onTimeSet(android.widget.TimePicker timePicker, int hour, int minute) {
        if (mListener!=null) {
            final Calendar c = Calendar.getInstance();
            c.set(0, 0, 0, hour, minute, 0);
            mListener.onTimeInteractionListener(c.getTime());
        }
    }

    //Интейфейс обработчика изменения даты
    public interface OnTimeInteractionListener { void onTimeInteractionListener(Date date); }
}
