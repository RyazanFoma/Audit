package com.example.eduardf.audit;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;

//Фрагмент для ввода даты и времени

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 19.12.18 11:53
 *
 */

/**
 * Редактирование даты и времени
 * Фрагмент может использоваться в статическом варианте {@link DateTime#setDate(java.util.Date)}
 * или динамическом варианте {@link DateTime#newInstance(java.util.Date)}
 */
public class DateTime extends Fragment{

    private Date myDate;
    //Поля даты и времени
    private TextView dateView;
    private TextView timeView;

    private static String ARG_DATETIME = "datetime";

    public DateTime() {}

    /**
     * Установить дату
     * Используется для статического размещения фрагмента
     * @param date - дата
     */
    void setDate(Date date) {
        myDate = date;
        if (!(date.getTime() == 0 || dateView == null || timeView == null)) {
            dateView.setText(getDateInstance().format(date));
            timeView.setText(getTimeInstance().format(date));
        }
    }

    /**
     * Получить выбранную дату
     * @return - дата
     */
    Date getDate() {
        if (!(dateView == null || timeView == null)) {
            try {
                return getDateTimeInstance().parse(dateView.getText().toString()+" "+
                        timeView.getText().toString());
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Создание фрагмента для редактирования даты и времени из активности
     * Используется для динамического размещения фрагмента
     * @param date - исходная дата
     * @return - фрагмент
     */
    public static DateTime newInstance(Date date) {
        final DateTime fragment = new DateTime();
        final Bundle args = new Bundle();
        if (date!=null) args.putLong(ARG_DATETIME, date.getTime());
        fragment.setArguments(args);
        return fragment;
    }

    // получает аргументы
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            final Bundle args = getArguments();
            if (args != null) { //Аргументы возможны только в динамическом варианте
                myDate = new Date();
                myDate.setTime(args.getLong(ARG_DATETIME, 0));
            }
        }
    }

    /**
     * Настройка поля с датой
     * @param container - вью фрагмента
     * @return - вью с датой
     */
    private TextView setDateView(@NonNull View container) {
        final TextView view = container.findViewById(R.id.date);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //При нажатии всплывает констектсное меню
                final PopupMenu popup = new PopupMenu(getActivity(), v);
                final MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_date, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final Calendar calendar = Calendar.getInstance();
                        switch(item.getItemId()) { //Устанавливаем конкретную дату
                            case R.id.today:
                                break;
                            case R.id.tomorrow:
                                calendar.add(Calendar.DAY_OF_YEAR, 1);
                                break;
                            case R.id.nextweek:
                                calendar.add(Calendar.DAY_OF_YEAR, 7);
                                break;
                            case R.id.nextmonth:
                                calendar.add(Calendar.MONTH, 1);
                                break;
                            case R.id.selectday: default: //Или выбираем в диалоге
                                final DatePicker datePicker = new DatePicker();
                                datePicker.setOnDateInteractionListener(new DatePicker.OnDateInteractionListener() {
                                    @Override
                                    public void onDateInteractionListener(Date date) {
                                        view.setText(getDateInstance().format(date));
                                    }
                                });
                                final FragmentManager fragmentManager = getFragmentManager();
                                if (fragmentManager != null)
                                    datePicker.show(fragmentManager, "datePicker");
                                return false;
                        }
                        view.setText(getDateInstance().format(calendar.getTime()));
                        return true;
                    }
                });
                popup.show();
            }
        });
        return view;
    }

    /**
     * Настройка поля со временем
     * @param container - контейнер для вью
     * @return - вью со временем
     */
    private TextView setTimeView(@NonNull View container) {
        final TextView view = container.findViewById(R.id.time);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //При нажатии всплывает констектное меню
                final PopupMenu popup = new PopupMenu(getActivity(), v);
                final MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_time, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final Calendar calendar = Calendar.getInstance();
                        switch(item.getItemId()) { //Устанавливаем конкретное время
                            case R.id.breakfast:
                                calendar.set(0,0,0,9,0,0);
                                break;
                            case R.id.lunch:
                                calendar.set(0,0,0,13,0,0);
                                break;
                            case R.id.snack:
                                calendar.set(0,0,0,17,0,0);
                                break;
                            case R.id.dinner:
                                calendar.set(0,0,0,20,0,0);
                                break;
                            case R.id.selecttime: default: //Или выбыраем в диалоге
                                final TimePicker timePicker = new TimePicker();
                                timePicker.setOnTimeInteractionListener(new TimePicker.OnTimeInteractionListener() {
                                    @Override
                                    public void onTimeInteractionListener(Date date) {
                                        view.setText(getTimeInstance().format(date));
                                    }
                                });
                                final FragmentManager fragmentManager = getFragmentManager();
                                if (fragmentManager != null)
                                    timePicker.show(fragmentManager, "timePicker");
                                return false;
                        }
                        view.setText(getTimeInstance().format(calendar.getTime()));
                        return true;
                    }
                });
                popup.show();
            }
        });
        return view;
    }

    /**
     * Вызывается при создании вью фрагмента
     * @param inflater - выдуватель
     * @param container - контейнер фрагмента
     * @param savedInstanceState - среда хранения
     * @return - вью
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_date_time, container, false);
        dateView = setDateView(view); //Поле с датой
        timeView = setTimeView(view); //Поле со временем
        //Кнопка установить текущие дату и время
        view.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Date date = new Date();
                dateView.setText(getDateInstance().format(date));
                timeView.setText(getTimeInstance().format(date));
            }
        });
        return view;
    }

    /**
     * Заполнить вью фрагмента
     * @param view - вью
     * @param savedInstanceState - среда хранения
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Если места мало, то выведем в одну колонку
        final LinearLayout linearLayout = view.findViewById(R.id.datetime);
        final float limit = getResources().getDimension(R.dimen.min_width_datetime);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (linearLayout.getWidth() < limit)
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
            }
        });
        //Сработает при первом открытии в динамическом варианте размещения фрагмента
        if (savedInstanceState == null && myDate != null) {
            dateView.setText(getDateInstance().format(myDate));
            timeView.setText(getTimeInstance().format(myDate));
        }
    }
}
//Фома2018