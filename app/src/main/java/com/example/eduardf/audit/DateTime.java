package com.example.eduardf.audit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;

//Фрагмент для ввода даты и времени
public class DateTime extends Fragment{

    //Агрумент с датой
    private static final String ARG_DATETIME = "datetime";
    //Дата
    private Date mDate;
    private TextView dateView;
    private TextView timeView;

    private OnDateTimeInteractionListener mListener;

    public DateTime() {
        // Required empty public constructor
    }

    // возвращает фрагмент
    public static DateTime newInstance(Date date) {
        DateTime fragment = new DateTime();
        Bundle args = new Bundle();
        args.putString(ARG_DATETIME, getDateTimeInstance().format(date));
        fragment.setArguments(args);
        return fragment;
    }

    // получает аргументы
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            try {
                mDate = getDateTimeInstance().parse(getArguments().getString(ARG_DATETIME));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // создает фрагмент
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragment = inflater.inflate(R.layout.fragment_date_time, container, false);

        //Поле с датой
        dateView = (TextView) fragment.findViewById(R.id.date);
        dateView.setText(getDateInstance().format(mDate));
        dateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //При нажатии всплывает констектсное меню
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_date, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Calendar calendar = Calendar.getInstance();
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
                                DatePicker datePicker = new DatePicker();
                                datePicker.setOnDateInteractionListener(new DatePicker.OnDateInteractionListener() {
                                    @Override
                                    public void onDateInteractionListener(Date date) {
                                        dateView.setText(getDateInstance().format(date));
                                        setDate();
                                    }
                                });
                                datePicker.show(getFragmentManager(), "datePicker");
                                return false;
                        }
                        dateView.setText(getDateInstance().format(calendar.getTime()));
                        setDate();
                        return true;
                    }
                });
                popup.show();
            }
        });

        //Поле со временем
        timeView = (TextView) fragment.findViewById(R.id.time);
        timeView.setText(getTimeInstance().format(mDate));
        timeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //При нажатии всплывает констектное меню
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_time, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Calendar calendar = Calendar.getInstance();
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
                                TimePicker timePicker = new TimePicker();
                                timePicker.setOnTimeInteractionListener(new TimePicker.OnTimeInteractionListener() {
                                    @Override
                                    public void onTimeInteractionListener(Date date) {
                                        timeView.setText(getTimeInstance().format(date));
                                        setDate();
                                    }
                                });
                                timePicker.show(getFragmentManager(), "timePicker");
                                return false;
                        }
                        timeView.setText(getTimeInstance().format(calendar.getTime()));
                        setDate();
                        return true;
                    }
                });
                popup.show();
            }
        });

        //Кнопка очистить дату и время - установить текущие
        ((ImageButton) fragment.findViewById(R.id.clear)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();
                dateView.setText(getDateInstance().format(date));
                timeView.setText(getTimeInstance().format(date));
                setDate();
            }
        });

        return fragment;
    }

    // вызывает интеракшин для изменения даты
    private void setDate() {
        if (mListener != null) {
            Date date = null;
            try {
                date = getDateTimeInstance().parse(dateView.getText().toString()+" "+timeView.getText().toString());
            } catch (ParseException e) {
                e.printStackTrace(); //Здесь нужно сообщить о неправильной дате
            }
            mListener.onDateTimeInteraction(date);
        }
    }

    // запоминает интеркашин для изменения даты
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDateTimeInteractionListener) {
            mListener = (OnDateTimeInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDateTimeInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //Интейфейс для обработки изменения даты и/или времени
    public interface OnDateTimeInteractionListener {
        void onDateTimeInteraction(Date date);
    }
}
