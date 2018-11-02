package com.example.eduardf.audit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Date;
import java.util.Locale;

//Диалог для изменения наименования группы
public class DialogIndicator extends DialogFragment
        implements DateTime.OnDateTimeInteractionListener {

    public static final String TAG_NUMBER = "number"; //Режим редактирования числа
    public static final String TAG_DATE = "date"; //Режим редактирования даты
    public static final String TAG_COMMENT = "comment"; //Режим редактирования комментария

    private static final String ARG_POSITION = "position"; //Аргумент Позиция показателя
    private static final String ARG_TITLE = "title"; //Аргумент Число
    private static final String ARG_NUMBER = "number"; //Аргумент Число
    private static final String ARG_UNIT = "unit"; //Аргумент ед. измерения
    private static final String ARG_DATE = "date"; //Аргумент Дата
    private static final String ARG_COMMENT = "comment"; //Аргумент комментарий
    private static DialogInteractionListener mListener; //Обработчик нажатия Изменить

    private int position;
    private String title;
    private float number;
    private String comment;
    private String unit;
    private Date date;

    //Создает диалог для редактирвоания числа
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

    //Создает диалог для редактирвоания комментария
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

    //Создает диалог для редактирвоания даты
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

    //Проверяем наличие интеракшин в родительском классе
    static private void instanceOf(Fragment context) {
        if (context instanceof DialogIndicator.DialogInteractionListener) {
            mListener = (DialogIndicator.DialogInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DialogsIndicator.DialogInteractionListener");
        }
    }

    //Обработчик изменения даты
    @Override
    public void onDateTimeInteraction(Date date) {
        this.date = date;
    }

    //Интерфейс для нажатия кнопки Изменить. Должен присутствовать в родительском классе
    public interface DialogInteractionListener {
        public void onChangedIndicatorValue(int position, Object value);
        public void onChangedIndicatorComment(int position, String value);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final  Bundle args = savedInstanceState == null? getArguments(): savedInstanceState;
        position = args.getInt(ARG_POSITION);
        title = args.getString(ARG_TITLE);
        number = args.getFloat(ARG_NUMBER, 0);
        comment = args.getString(ARG_COMMENT, "");
        unit = args.getString(ARG_UNIT, "");
        date = new Date();
        date.setTime(args.getLong(ARG_DATE, 0));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_indicator, container, false);
    }

    @SuppressLint("ClickableViewAccessibility") //Чтобы для onTouch не возникало предупреждение о performClick
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LinearLayout layoutNumber = view.findViewById(R.id.layout_number);
        final TextInputLayout layoutComment = view.findViewById(R.id.layout_comment);
        final FrameLayout layoutDate = view.findViewById(R.id.datetime);

        ((TextView) view.findViewById(R.id.title)).setText(title);
        switch (getTag()) {
            case TAG_NUMBER: {
                final TextInputLayout layoutValue = view.findViewById(R.id.layout_value);
                final TextInputEditText textValue = view.findViewById(R.id.text_value);
                final String text = (number == (long)number)?
                        String.format(Locale.US,"%d", (long) number):
                        String.format(Locale.US,"%s", number);
                textValue.setText(text);
                textValue.setSelection(text.length());
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
                //Остальное скрываем
                layoutComment.setVisibility(View.GONE);
                layoutDate.setVisibility(View.GONE);
                //Обработчик нажатия на кнопку Изменить
                ((Button) view.findViewById(R.id.positive)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        //Добавить проверку и сообщение об ошибке!!!
                        try {
                            mListener.onChangedIndicatorValue(position, Float.parseFloat(textValue.getText().toString()));
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
                final TextInputEditText textComment = view.findViewById(R.id.text_comment);
                textComment.setText(comment);
                textComment.setSelection(comment.length());
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
                layoutNumber.setVisibility(View.GONE);
                layoutDate.setVisibility(View.GONE);
                ((Button) view.findViewById(R.id.positive)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mListener.onChangedIndicatorComment(position, textComment.getText().toString());
                        dismiss();
                    }
                });
                break;
            }
            case TAG_DATE:
                layoutNumber.setVisibility(View.GONE);
                layoutComment.setVisibility(View.GONE);
                getChildFragmentManager().
                        beginTransaction().
                        add(R.id.datetime, DateTime.newInstance(date)).
                        commit();
                ((Button) view.findViewById(R.id.positive)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mListener.onChangedIndicatorValue(position, date);
                        dismiss();
                    }
                });
                break;
        }
        ((Button) view.findViewById(R.id.negative)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    //Перед поворотом экрана
    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ARG_POSITION, position);
        outState.putString(ARG_TITLE, title);
        outState.putFloat(ARG_NUMBER, number);
        outState.putString(ARG_COMMENT, comment);
        outState.putString(ARG_UNIT, unit);
        outState.putLong(ARG_DATE, date.getTime());
    }
}

