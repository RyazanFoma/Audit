package com.example.eduardf.audit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Фрагмент для организации поля с выбором элемента справочника
 */
public class ReferenceField extends Fragment implements ReferenceChoice.OnReferenceManagerInteractionSingleChoice {

    //Аргументы для передачи параметров поля
    private static final String ARG_REQUESTCODE = "requestcode";
    private static final String ARG_TABLE = "table";
    private static final String ARG_TITLE = "title";
    private static final String ARG_HIERARCHY = "hierarchy";
    private static final String ARG_OWNER = "owner";
    private static final String ARG_ID = "id";
    private static final String ARG_IN = "in";

    private int requestCode; //Код для идентификации фрагмента. Присваиваем при создании. Возвращаем при выборе
    private String table; //Наименование таблицы справочника
    private String title; //Заголовой поля
    private int hierarchy; //Вид иерархии справочника
    private String owner; //Giud собственника
    private ArrayList<String> in; //Список guid папок первого уровня для отбора
    private String id; //Guid выбранного элемента
    private EditText viewName; //Поле с наименованием выбранного элемента
    private boolean afterRotation; //true - признак прошедшего поворота, false - после фокуса на поле с наименованием

    private OnFragmentChoiceListener mListener;

    private AuditOData oData;

    public ReferenceField() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param requestCode Уникальный код, для идентификации фрагментов
     * @param table Таблица в oData
     * @param title Наименование поля для hint и заголовка активности
     * @param hierarchy Вид иерархии справочника
     * @param owner Guid владельца справочника
     * @param id Guid текущего выбранного элемента
     * @param in Список giud папок первого уровня для отбора
     * @return A new instance of fragment ReferenceField.
     */
    public static ReferenceField newInstance(int requestCode, String table, String title, int hierarchy, String owner, String id, ArrayList<String> in) {
        ReferenceField fragment = new ReferenceField();
        Bundle args = new Bundle();
        args.putInt(ARG_REQUESTCODE, requestCode);
        args.putString(ARG_TABLE, table);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_HIERARCHY, hierarchy);
        args.putString(ARG_OWNER, owner);
        args.putString(ARG_ID, id);
        if (in != null) args.putStringArrayList(ARG_IN, in);
        fragment.setArguments(args);
        return fragment;
    }
    // То-же, но без возможносои отбора по список guid папок первого уровня
    public static ReferenceField newInstance(int requestCode, String table, String title, int hierarchy, String owner, String id) {
        return newInstance(requestCode, table, title, hierarchy, owner, id, null);
    }

    //создает фрагмент
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            requestCode = args.getInt(ARG_REQUESTCODE);
            table = args.getString(ARG_TABLE);
            title = args.getString(ARG_TITLE);
            hierarchy = args.getInt(ARG_HIERARCHY);
            owner = args.getString(ARG_OWNER);
            if (args.containsKey(ARG_IN)) in = args.getStringArrayList(ARG_IN);
            if (savedInstanceState != null && savedInstanceState.containsKey(ARG_ID))
                id = savedInstanceState.getString(ARG_ID);
            else
                id = args.getString(ARG_ID);
        }
        else {
            throw new RuntimeException(
                    "At the time of the creation of the fragment, there are no arguments ");
        }
    }

    //возвращает View фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reference_field, container, false);
    }

    //заполняет View фрагмента
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        afterRotation = savedInstanceState!=null;
        ((TextInputLayout) view.findViewById(R.id.input_layout_name)).setHint(title);
        viewName = view.findViewById(R.id.input_name);
        if (id != null) new setName(viewName).execute(id);
        viewName.setKeyListener(null);
        viewName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(ReferenceChoice.intentActivity(ReferenceField.this, table, title, hierarchy, owner, id, in));
            }
        });
        viewName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    if (!afterRotation) //Чтобы после поворота не открывалась активность выбора
                        startActivity(ReferenceChoice.intentActivity(ReferenceField.this, table, title, hierarchy, owner, id, in));
                    else
                        afterRotation = false;
            }
        });
        final ImageView clear_name = view.findViewById(R.id.clear_name);
        clear_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                id = null;
                viewName.setText("");
                mListener.onFragmentChoiceListener(requestCode, id);
            }
        });
    }

    //Сохраняет выбранный id перед поворотом экрана. Остальное сохраниться в агрументах
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_ID, id);
    }

    //Обработчик выбора элемента в активности ReferenceChoice
    @Override
    public void onReferenceManagerInteractionListenerSingleChoice(int code, Items.Item item) {
        //При вызове из фрагмента code будет всегда -1. Скрипач не нужен
        id = item.id;
        viewName.setText(item.name);
        if (mListener != null) mListener.onFragmentChoiceListener(requestCode, id);
    }

    //Класс для заполнения поля с наименованием в парралельном потоке
    private class setName extends AsyncTask<String, Void, String> {
        final EditText editText;
        private setName(EditText editText) { this.editText = editText; }
        protected void onPreExecute() { editText.setText(R.string.progress_msq); }
        protected String doInBackground(String... id) { return oData.getName(table, id[0]); }
        protected void onPostExecute(String name) {
            editText.setText(name);
        }
    }

    //вызывается при присоединении фрагмента
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentChoiceListener) {
            mListener = (OnFragmentChoiceListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentChoiceListener");
        }
        oData = new AuditOData(context);
    }

    //вызывается при удалении фрагмента
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        in = null;
        viewName = null;
        oData = null;
    }

    //Интерфейс для передачи выбранного элемента
    public interface OnFragmentChoiceListener {
        void onFragmentChoiceListener(int requestCode, String id);
    }
}
//Фома2018