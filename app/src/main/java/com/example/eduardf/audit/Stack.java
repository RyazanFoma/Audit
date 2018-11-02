package com.example.eduardf.audit;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

//Класс стек для вывода текущего положения в дереве
public class Stack {
    //Используем список для организации стека, чтобы была возможность получить toString с наименованиями пунктов
    private Items items;
    private Context context;
    private View.OnClickListener onClickListener;

    //Конструктор из фрагмента
    public Stack(Fragment fragment) {
        if (!(fragment instanceof View.OnClickListener))
            throw new RuntimeException(context.toString()+" must implement View.OnClickListener");
        else onClickListener = (View.OnClickListener) fragment;
        this.context = fragment.getActivity();
        items = new Items();
    }

    //Конструктор из активности
    public Stack(Context context) {
        if (!(context instanceof View.OnClickListener))
            throw new RuntimeException(context.toString()+" must implement View.OnClickListener");
        else onClickListener = (View.OnClickListener) context;
        this.context = context;
        items = new Items();
    }

    // добавляет пункт в конец стека
    public void push(Items.Item item) {
        items.add(item);
    }

    // заполняет вью списком предков с ограничением по длине наименования
    public void addTextView(LinearLayout linearLayout, int limit) {
        linearLayout.removeAllViews(); // удаляем предыдущий список
        // все родители их стека
        for (Items.Item item:items) {
            TextView textView = new TextView(context);
            textView.setTag(item);
            textView.setText((item.name.length()<=limit+5)?
                    item.name:
                    item.name.substring(0, limit/2)+" ... "+item.name.substring(item.name.length()-limit/2, item.name.length()));
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_navigate_next_24px, 0);
            textView.setOnClickListener((View.OnClickListener) onClickListener);
            linearLayout.addView(textView);
        }
    }

    //Обрезаем стек до указанного пункта
    public void clip(int first) {
        int last = items.size();
        if (first >= 0) for (int i = first+1; i<last; i++) pop();
    }

    //Обрезаем стек до указанного пункта
    public void clip(Items.Item item) {
        clip(items.indexOf(item));
    }

    //Извлекает верхний пунт стека
    private Items.Item pop() {
        return items.remove(items.size()-1);
    }

    //Возвращает ссылку на верхний пункт стека
    public Items.Item peek() {
        Items.Item item = null;
        if (! items.isEmpty()) item = items.get(items.size()-1);
        return item;
    }

    //Возвращает true, если указанный элемент является родителем текущего
    public boolean contains(String id) {
        for (Items.Item item:items) if (id.equals(item.id)) return true;
        return false;
    }

}
//Фома2018