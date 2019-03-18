package com.example.eduardf.audit;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;

import static com.example.eduardf.audit.ReferenceChoice.ACTION_BAR;
import static com.example.eduardf.audit.ReferenceChoice.ACTION_COPY;
import static com.example.eduardf.audit.ReferenceChoice.ACTION_MODE;
import static com.example.eduardf.audit.ReferenceChoice.ACTION_MOVE;
import static com.example.eduardf.audit.ReferenceChoice.MODE_MULTIPLE_CHOICE;
import static com.example.eduardf.audit.ReferenceChoice.MODE_SINGLE_CHOICE;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 27.11.18 11:19
 *
 */

/**
 * Адаптер для списка элементов/групп
 */
class ReferenceAdapter extends RecyclerView.Adapter<ViewHolderRefs> {

    private final Items items; //Пункты списка

    //Аргументы для сохранения/восстановления данных при повороте
    private static final String ARG_LIST = "list";
    private static final String ARG_CHECKED_STATUS = "status";

    //Ссылки на аргументы конструктора
    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private Activity activity; //Активность
    private AuditOData.Set table; //Таблица
    private View.OnClickListener onClickListener; //Обработчик короткого нажатия
    private View.OnLongClickListener onLongClickListener; //Обработчик длинного нажатия
    private ArrayList<String> ids; //Текущие выбранные элементы для подсветки
    private Stack stack; //Стек предков
    private int modeChoice; //Режим выбора
    private int modeMenu; //Режим меню

    //Статус состояния отмеченных объектов
    private int checkedStatus;
    final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    final static int CHECKED_STATUS_ONE = 1; //Помечен один пункт
    private final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    /**
     * Конструктор адаптера
     * @param context - контекст активности
     * @param oData - доступ в 1С
     * @param table - таблица 1С
     * @param ids - список guid выбранных ранее пунктов
     * @param stack - стек предков
     * @param modeChoice - режим выбора: одиночный / множественный
     * @param modeMenu - режим меню: обычное для выбора, контектное для редактирования, копирование/перемещение
     */
    ReferenceAdapter(Context context, AuditOData oData, AuditOData.Set table,
                     ArrayList<String> ids, Stack stack, int modeChoice, int modeMenu) {

        if (context instanceof View.OnClickListener && context instanceof View.OnLongClickListener) {
            this.onClickListener = (View.OnClickListener) context;
            this.onLongClickListener = (View.OnLongClickListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnClickListener and OnLongClickListener");
        }
        this.activity = (Activity) context;
        this.oData = oData;
        this.table = table;
        this.ids = ids;
        this.stack = stack;
        this.modeChoice = modeChoice;
        this.modeMenu = modeMenu;
        //Создаем пустой список пунктов
        items = new Items();
    }

    /**
     * Восстанавливает состояние адаптера
     * @param savedInstanceState - среда для хранения
     */
    void onRestoreInstanceState(Bundle savedInstanceState) {
        checkedStatus = savedInstanceState.getInt(ARG_CHECKED_STATUS);
        items.onRestoreInstanceState(savedInstanceState, ARG_LIST);
    }

    /**
     * Сохраняет состояние адаптера
     * @param outState - среда для хранения
     */
    void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_CHECKED_STATUS, checkedStatus);
        items.onSaveInstanceState(outState, ARG_LIST);
    }

    /**
     * Побуждает обновить содержимое списка
     * @param modeMenu - режим меню
     */
    void notifyDataSetChanged(int modeMenu) {
        setModeMenu(modeMenu);
        notifyDataSetChanged();
    }

    /**
     * Устанавливает режим меню
     * @param modeMenu - новое значение режима
     */
    void setModeMenu(int modeMenu) {
        this.modeMenu = modeMenu;
    }


    /**
     * Пустой список?
     * @return true - если пустой, иначе - false
     */
    boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * @return - количество пунктов списка
     */
    int checkedCount() {
        return items.checkedCount();
    }

    /**
     * Первый пункт списка
     * @return - ссылку на первый пункт в списке
     */
    Items.Item checkedItemFirst() {
        return items.checkedItemFirst();
    }

    /**
     * Статус отметок пунктов в списке
     * @return - текущее значение статуса
     */
    int checkedStatus() {
        return checkedStatus;
    }

    /**
     * Очищает список пунктов
     */
    void clearList() {
        if (!items.isEmpty()) items.clear();
        notifyDataSetChanged();
    }

    /**
     * Добавляет в список новые пункты
     * @param data - новые пункты
     */
    void loadList(Items data) {
        final int start = items.size();
        if (data!=null) items.addAll(data);
        notifyItemRangeInserted(start, data.size());
    }

    /**
     * Устанавливает отметки пунктов
     * @param checked - значение отметки
     * @param only_child - только элементов без папок
     */
    void setCheckedAll(boolean checked, boolean only_child) {
        items.setCheckedAll(checked, only_child);
        notifyDataSetChanged();
        if (checked) updateStatus(only_child);
        else checkedStatus = CHECKED_STATUS_NULL;
    }

    //Проверяет количество отмеченных пунктов

    /**
     * Обновляет значение статуса отметок списка
     * @param only_child - только элементов без папок
     */
    void updateStatus(boolean only_child) {
        int count = 0; //Общее количество пунктов
        int checked = 0; //Из них отмеченных
        //Подсчитываем количество отмеченных и всех
        if (only_child) {
            for(Items.Item item: items) {
                if (!item.folder) {
                    count++;
                    if (item.checked) checked++;
                }
            }
        }
        else {
            count = items.size();
            checked = items.checkedCount();
        }
        //Сравниваем результат
        if (checked==0 || count==0) checkedStatus = CHECKED_STATUS_NULL;
        else if (checked==1) checkedStatus = CHECKED_STATUS_ONE;
        else if (checked==count) checkedStatus = CHECKED_STATUS_ALL;
        else checkedStatus = CHECKED_STATUS_SOME;
    }

    /**
     * Отмеченные пункты
     * @return - список guid отмеченныъ элементов
     */
    ArrayList<String> getChecked() {
        return items.getChecked();
    }

    /**
     * Отмеченные пункты
     * @return - список отмеченных пунктов
     */
    Items getCheckedItems() {
        Items rezult = new Items();
        for (Items.Item item: items) if (item.checked) rezult.add(item);
        return rezult;
    }

    /**
     * Класс для операций копирования отмеченных строк
     * Входные параметры: guid папки, в которую будем перемещать
     */
    private class copyRowsAsyncTask extends AsyncTask<String, Void, Void> {
        private ArrayList<String> ids;
        private int start;
        private copyRowsAsyncTask(ArrayList<String> ids) {
            this.ids = ids;
        }
        protected void onPreExecute() {
            activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            start = items.size();
        }
        protected Void doInBackground(String... pater) {
            for (String id : ids) items.add(oData.copyItem(table, id, pater[0]));
            return null;
        }
        protected void onPostExecute(Void voids) {
            ids = null;
            updateStatus(false);
            notifyItemRangeInserted(start, items.size()-start);
            activity.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Копирует отмеченные строки
     */
    void copyRows(ArrayList<String> ids, String checkedTo) {
        new copyRowsAsyncTask(ids).execute(checkedTo);
    }

    /**
     * Класс операций перемещения отмеченных строк в другую папку
     * Входные параметры: guid - родителя
     * Возвращает: false - если не все папки были перемещены, чтобы не нарушать целостность
     */
    private class moveRowsAsyncTask extends AsyncTask<String, Void, Boolean> {
        private ArrayList<String> ids;
        private int start;
        private moveRowsAsyncTask(ArrayList<String> ids) {
            this.ids = ids;
        }
        protected void onPreExecute() {
            activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            start = items.size();
        }
        protected Boolean doInBackground(String... pater) {
            boolean notMoved = false;
            for (String id : ids) {
                Items.Item item = oData.getItem(table, id);
                if ((item.folder && stack.contains(id)) || item.predefined)
                    notMoved = true; //Перемещения в своих потомков или предопределенный элемент/группа
                else
                    items.add(oData.moveItem(table, id, pater[0]));
            }
            return notMoved;
        }
        protected void onPostExecute(Boolean notMoved) {
            ids = null;
            updateStatus(false);
            notifyItemRangeInserted(start, items.size()-start);
            activity.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            if (notMoved)
                Snackbar.make((View) activity.findViewById(R.id.list), R.string.msg_move_error, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
        }
    }

    /**
     * Перемещение отмеченных строк в другую папку
     */
    void moveRows(ArrayList<String> ids, String checkedTo) {
        new moveRowsAsyncTask(ids).execute(checkedTo);
    }

    /**
     * Класс для операций установки/снятия пометки на удаление в отмеченных строках
     */
    private class deleteRowsAsyncTask extends AsyncTask<Boolean, Void, Void> {
        protected void onPreExecute() {
            activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
        protected Void doInBackground(Boolean... delete) {
            int position = 0;
            for(Items.Item item: items) {
                if (item.checked && !item.predefined) { //Отмеченные и не предопределенные
                    items.set(position, oData.deleteItem(table, item.id, delete[0]));
                }
                position++;
            }
            return null;
        }
        protected void onPostExecute(Void voids) {
            notifyDataSetChanged();
            activity.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Установка/снятие пометки на удаление в отмеченных строках
     * @param delete - значение пометки на удаление
     */
    void deleteRows(boolean delete) {
        new deleteRowsAsyncTask().execute(delete);
    }

    /**
     * Класс операции добавления группы
     * Входные параметры:
     * guid родителя
     * наименование группы
     */
    private class createRowAsyncTask extends AsyncTask<String, Void, Items.Item> {
        final private boolean isFolder;
        private createRowAsyncTask(boolean isFolder) {
            this.isFolder = isFolder;
        }
        protected void onPreExecute() {
            ((ProgressBar) activity.findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
        }
        protected Items.Item doInBackground(String... name) {
            return oData.createItem(table, name[0], name[1], isFolder);
        }
        protected void onPostExecute(Items.Item item) {
            items.add(item);
            notifyItemInserted(items.size()-1);
            ((ProgressBar) activity.findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Добавляет новый элемент/группу
     * @param name - наименование
     * @param isFolder - признак группы
     */
    void addRow(String name, boolean isFolder) {
        new createRowAsyncTask(isFolder).execute(stack.peek().id, name);
    }

    /**
     * Класс для выполнения операций изменение наименования
     * Входные параметры: пункт с новым наименованием
     * Возвращает: измененный пункт
     */
    private class updateRowAsyncTask extends AsyncTask<Items.Item, Void, Items.Item> {
        int position;
        private updateRowAsyncTask(int position) {
            super();
            this.position = position;
        }
        protected void onPreExecute() {
            ((ProgressBar) activity.findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
        }
        protected Items.Item doInBackground(Items.Item... items) {
            return oData.updateItem(table, items[0]);
        }
        protected void onPostExecute(Items.Item item) {
            notifyItemChanged(position, item);
            ((ProgressBar) activity.findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Изменяет наименование элемента/группы
     * @param name - новое наименование элемента/группы
     */
    void editRow(String name) {
        Items.Item item = items.checkedItemFirst();
        item.name = name;
        new updateRowAsyncTask(items.indexOf(item)).execute(item);
    }

    /**
     * Создает хранилище пункта
     * @param parent - родительский вью
     * @param viewType - тип вью
     * @return - хранилище пункта
     */
    @NonNull
    @Override
    public ViewHolderRefs onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reference, parent, false);
        return new ViewHolderRefs(view);
    }

    /**
     * Заполняет вью пункта данными
     * @param holder - хранилище пункта
     * @param position - позиция пункта в списке
     */
    @Override
    public void onBindViewHolder(@NonNull final ViewHolderRefs holder, int position) {
        //Текущий пункт
        holder.item = items.get(position);

        //Иконка + / -
        holder.imageView.setImageResource(holder.item.folder?
                R.drawable.ic_black_add_circle_outline_24px :
                R.drawable.ic_baseline_remove_circle_outline_24px);

        // наименование и описание (нужно добавить комментарий хотя бы для видов аудита)
        holder.nameView.setText(holder.item.name);
        holder.deletedView.setVisibility(holder.item.deleted? View.VISIBLE: View.INVISIBLE);
        if (!holder.item.predefined) {
            holder.predefinedView.setVisibility(View.INVISIBLE);
            holder.descView.setText("");
        }
        else {
            holder.predefinedView.setVisibility(View.VISIBLE);
            holder.descView.setText(holder.item.prenamed);
        }

        holder.itemView.setTag(holder.item); // в теге храним пункт

        // Раскрашиваем карточки разными цветами
        if (!ids.contains(holder.item.id)) { //Пункта нет в списке выбранных элементов
            if (holder.item.deleted) //Помеченные на удаление - серым
                holder.cardView.setBackgroundResource(R.color.colorBackgroundGrey);
            else if (holder.item.predefined) //Предопределенные - желтым
                holder.cardView.setBackgroundResource(R.color.colorBackgroundYellow);
            else //Остальное белым
                holder.cardView.setBackgroundResource(R.color.cardview_light_background);
        }
        else { //Пунт был выбран ранее
            if (holder.item.deleted) //Помеченные на удаление - сине-серым
                holder.cardView.setBackgroundResource(R.color.colorBackgroundDarkGrey);
            else if (holder.item.predefined) //Предопределенные - сине-желтым (зеленым)
                holder.cardView.setBackgroundResource(R.color.colorBackgroundDarkYellow);
            else //Остальное синим
                holder.cardView.setBackgroundResource(R.color.colorBackgroundItem);
        }

        switch (modeMenu) {
            case ACTION_BAR:
                switch (modeChoice) {
                    case MODE_SINGLE_CHOICE:
                        holder.checkedView.setVisibility(View.GONE);
                        if (holder.item.folder) {
                            holder.forwardView.setVisibility(View.VISIBLE);
                            holder.itemView.setOnClickListener(onClickListener); //Папки открываем
                        }
                        else {
                            holder.forwardView.setVisibility(View.GONE);
                            holder.itemView.setOnClickListener(onClickListener);
                        }
                        break;
                    case MODE_MULTIPLE_CHOICE:
                        if (holder.item.folder) {
                            holder.itemView.setOnClickListener(onClickListener); //Папки открываем
                            holder.forwardView.setVisibility(View.VISIBLE);
                            holder.checkedView.setVisibility(View.GONE);
                            holder.checkedView.setTag(null);
                            holder.checkedView.setOnClickListener(null);
                        }
                        else {
                            holder.itemView.setOnClickListener(null); //Только чек-бокс
                            holder.forwardView.setVisibility(View.GONE);
                            holder.checkedView.setVisibility(View.VISIBLE);
                            holder.checkedView.setChecked(holder.item.checked);
                            holder.checkedView.setTag(holder.item);
                            holder.checkedView.setOnClickListener(onClickListener);
                        }
                        break;
                }

                holder.itemView.setOnLongClickListener(onLongClickListener);
                break;
            case ACTION_MODE:
                holder.forwardView.setVisibility(View.GONE);
                holder.checkedView.setVisibility(View.VISIBLE);
                holder.checkedView.setChecked(holder.item.checked);
                holder.checkedView.setEnabled(true);
                holder.checkedView.setTag(holder.item);
                holder.checkedView.setOnClickListener(onClickListener);
                if (holder.item.folder) holder.itemView.setOnClickListener(onClickListener); //Папки открываем
                else holder.itemView.setOnClickListener(null); //Только чек-бокс
                // недоступны длинные щелчки
                holder.itemView.setOnLongClickListener(null);
                break;
            case ACTION_COPY | ACTION_MOVE:
                // чекбокс видим, но недоступен для отмеченных заданий, для остальных чекбокс невидим
                if (holder.checkedView.isChecked()) {
                    holder.checkedView.setVisibility(View.VISIBLE);
                    holder.checkedView.setEnabled(false);
                }
                else {
                    holder.checkedView.setVisibility(View.INVISIBLE);
                }
                if (holder.item.folder) {
                    holder.forwardView.setVisibility(View.VISIBLE);
                    holder.itemView.setOnClickListener(onClickListener); //Папки открываем
                }
                else {
                    holder.forwardView.setVisibility(View.INVISIBLE);
                    holder.itemView.setOnClickListener(null); //Файлы не трогаем
                }
                // недоступны длинные щелчки
                holder.itemView.setOnLongClickListener(null);
                break;
        }
    }

    /**
     * Количество пунктов
     * @return - количество пунктов
     */
    @Override
    public int getItemCount() {
        return items.size();
    }
}
//Фома2018