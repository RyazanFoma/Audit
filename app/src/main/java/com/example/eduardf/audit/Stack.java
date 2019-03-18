package com.example.eduardf.audit;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 18.01.19 11:49
 *
 */

/**
 * Класс для организации навигации по предкам
 */
abstract class Stack extends Items {
    /**
     * Конструктор
     */
    Stack() {
        super();
    }

    /**
     * Загружает всех предков в стек начиная с корней
     * @param id - quid наследника
     */
    void loadStack(String id) {
        if (!(id == null || AuditOData.EMPTY_KEY.equals(id))) {
            Items.Item item = getItem(id);
            loadStack(item.pater); //Рекурсия!!!
            push(item);
        }
    }

    /**
     * Абстрактный метод получения пункта из таблицы по id
     * @param id - giud пункта
     * @return - пункт
     */
    abstract Items.Item getItem(String id);

    /**
     * Вставляет пункт в стек
     * @param item - предок
     */
    void push(final Items.Item item) {
        add(item);
    }

    /**
     * Укорачивает стек предков
     * @param first - индекс предка, все потомки которого будут удалены
     */
    void clip(final int first) {
        if (first+1 < size()) removeRange(first+1, size());
    }

    /**
     * Укорачивает стек предков
     * @param item - предок, все потомки которого будут удалены
     */
    void clip(final Items.Item item) {
        clip(indexOf(item));
    }

    /**
     * Последний предок
     * @return - последнего добавленного предка / null
     */
    Items.Item peek() {
        if (!isEmpty()) return get(size()-1);
        return null;
    }

    /**
     * Проверка предка на присутствие в стеке
     * @param id - предка
     * @return - true, если есть такой предок, false, в противном случае
     */
    boolean contains(final String id) {
        for (Items.Item item: this) if (id.equals(item.id)) return true;
        return false;
    }

    /**
     * Заполняет LinearLayout предками из стека
     * @param linearLayout - место для вставки
     * @param fragment - фрагмент, в котором находиться linearLayout
     */
    void addTextView(final LinearLayout linearLayout, Fragment fragment) {
        if (!(fragment instanceof View.OnClickListener))
            throw new RuntimeException(fragment.toString()+" must implement View.OnClickListener");
        addTextView(linearLayout, fragment.getActivity(), (View.OnClickListener) fragment);
    }

    /**
     * Заполняет LinearLayout предками из стека
     * @param linearLayout - место для вставки
     * @param activity - активность, в которой находиться linearLayout
     */
    void addTextView(final LinearLayout linearLayout, Activity activity) {
        if (!(activity instanceof View.OnClickListener))
            throw new RuntimeException(activity.toString()+" must implement View.OnClickListener");
        addTextView(linearLayout, activity, (View.OnClickListener) activity);
    }

    /**
     * Заполняет linearLayout предками из стека
     * @param linearLayout - место для предков
     * @param context - контекст
     * @param onClickListener - обработчик нажатия на предка
     */
    private void addTextView(final LinearLayout linearLayout, Context context, View.OnClickListener onClickListener) {
        linearLayout.removeAllViews(); // удаляем предыдущий список
        //Выводим список предков
        for(int i = 0; i < size(); i++) {
            final TextView textView = new TextView(context);
            final Items.Item item = get(i);
            textView.setTag(item);
            textView.setText(item.name);
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_baseline_navigate_next_24px, 0);
            textView.setOnClickListener(onClickListener);
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout.addView(textView);
        }
    }

}
//Фома2018