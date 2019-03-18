package com.example.eduardf.audit;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import static com.example.eduardf.audit.ReferenceChoice.ACTION_BAR;
import static com.example.eduardf.audit.ReferenceChoice.ACTION_MODE;
import static com.example.eduardf.audit.ReferenceChoice.ACTION_COPY;
import static com.example.eduardf.audit.ReferenceChoice.ACTION_MOVE;
import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 31.01.19 11:07
 *
 */

//Адаптер для списка. Для заполнения списка используется загрузчик LoaderItems
class TaskListAdapter extends RecyclerView.Adapter<ViewHolderTasks> {

    private Tasks tasks; //Список заданий

    //Аргументы для сохранения/восстановления данных при повороте
    private static final String ARG_LIST = "list";
    private static final String ARG_MODE_MENU = "menu_mode"; //Режим меню

    //Ссылки на аргументы конструктора
    private AuditOData oData; //Объект OData для доступа к 1С:Аудитор
    private Activity activity; //Активность
    private View.OnClickListener onClickListener; //Обработчик короткого нажатия
    private View.OnLongClickListener onLongClickListener; //Обработчик длинного нажатия
    private OnInvalidateActivity onInvalidateActivityListener; //Обработчие обновления активности после копирования или переноса
    private int modeMenu; //Режим меню

    //Состояния отмеченных заданий
    final static int CHECKED_STATUS_NULL = 0; //Нет отмеченных пунктов
    private final static int CHECKED_STATUS_SOME = 2; //Помечено несколько пунктов
    final static int CHECKED_STATUS_ALL = 3; //Помечены все пункты

    TaskListAdapter(Context context, AuditOData oData, int modeMenu) {

        if (context instanceof View.OnClickListener &&
                context instanceof View.OnLongClickListener &&
                context instanceof OnInvalidateActivity) {
            onClickListener = (View.OnClickListener) context;
            onLongClickListener = (View.OnLongClickListener) context;
            onInvalidateActivityListener = (OnInvalidateActivity) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnClickListener, OnLongClickListener and OnInvalidateActivity");
        }
        this.oData = oData;
        this.activity = (Activity) context;
        this.modeMenu = modeMenu;
        //Создаем пустой список заданий
        tasks = new Tasks();
    }

    // восстанавливает все, что нужно адаптеру, после поворота экрана
    void onRestoreInstanceState(Bundle savedInstanceState) {
        modeMenu = savedInstanceState.getInt(ARG_MODE_MENU);
        tasks.onRestoreInstanceState(savedInstanceState, ARG_LIST);
    }

    // сохраняет все, что нужно адаптеру, перед поворотом экрана
    void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_MODE_MENU, modeMenu);
        tasks.onSaveInstanceState(outState, ARG_LIST);
    }

    /**
     * Побуждает обновить содержимое списка
     * @param modeMenu - режим меню
     */
    void notifyDataSetChanged(int modeMenu) {
        this.modeMenu = modeMenu;
        notifyDataSetChanged();
    }

    //Загружает список заданий в адаптер
    void load(Tasks data) {
        if (!tasks.isEmpty()) tasks.clear();
        if (data!=null) tasks.addAll(data);
        notifyDataSetChanged();
    }

    //обновляет статус отмеченных заданий
    int getStatus() {
        int checkedStatus = CHECKED_STATUS_SOME;
        int count = tasks.size(); //Общее количество заданий
        int checked = tasks.checkedCount(); //Из них отмеченных
        //Сравниваем результат
        if (checked==0 || count==0) checkedStatus = CHECKED_STATUS_NULL;
        else if (checked==count) checkedStatus = CHECKED_STATUS_ALL;
        return checkedStatus;
    }

    /**
     * Пустой список?
     * @return true - если пустой, иначе - false
     */
    boolean isEmpty() {
        return tasks.isEmpty();
    }

    //Помечает/отменяет отметки всех видимых элементов
    void setCheckedAll(boolean checked) {
        tasks.setCheckedAll(checked);
        notifyDataSetChanged();
    }

    /**
     * Отмеченные пункты
     * @return - список guid отмеченныъ элементов
     */
    ArrayList<String> getChecked() {
        return tasks.getChecked();
    }

    void setChecked(ArrayList<String> checked) {
        tasks.setChecked(checked);
    }

    /**
     * @return - количество пунктов списка
     */
    int checkedCount() {
        return tasks.checkedCount();
    }

    /**
     * Уведомляет о необходимости обновления задания в списоке
     * @param task - измененное задание
     */
    void notifyItemChanged(Tasks.Task task) {
        notifyItemChanged(tasks.indexOf(task));
    }

    /**
     * Интерфейс для выполнения операций обновления после копирования или переноса
     */
    public interface OnInvalidateActivity {
        void onInvalidateActivity();
    }

    //Класс для выполнения операций копирования заданий в новом потоке с последующим обновлением рециклервью
    class copyTasksAsync extends AsyncTask<Tasks.Task.Status, Integer, Void> {
        final private ArrayList<String> checkedIds;
        private int start;
        private copyTasksAsync(ArrayList<String> checkedIds) {
            this.checkedIds = checkedIds;
        }
        protected void onPreExecute() {
            activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            start = tasks.size();
        }
        protected Void doInBackground(Tasks.Task.Status... status) {
            for (String id : checkedIds) {
                tasks.add(oData.copyTask(id, status[0]));
                publishProgress((int) start++);
            }
            return null;
        }
        protected void onProgressUpdate(Integer... position) {
            notifyItemChanged(position[0]);
        }
        protected void onPostExecute(Void voids) {
            onInvalidateActivityListener.onInvalidateActivity();
            activity.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }

    //Копирует отмеченные задания с возможным с изменением статуса
    void copyRows(ArrayList<String> checkedIds, Tasks.Task.Status status) {
        new copyTasksAsync(checkedIds).execute(status);
    }

    //Класс для выполнения операций перемещения заданий в новом потоке с последующим обновлением рециклервью
    private class moveTasksAsync extends AsyncTask<Tasks.Task.Status, Integer, Void> {
        final private ArrayList<String> checkedIds;
        private int start;
        private moveTasksAsync(ArrayList<String> checkedIds) {
            this.checkedIds = checkedIds;
        }
        protected void onPreExecute() {
            activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            start = tasks.size();
        }
        protected Void doInBackground(Tasks.Task.Status... status) {
            for (String id : checkedIds) {
                tasks.add(oData.moveTask(id, status[0]));
                publishProgress((int) start++);
            }
            return null;
        }
        protected void onProgressUpdate(Integer... position) {
            notifyItemChanged(position[0]);
        }
        protected void onPostExecute(Void voids) {
            onInvalidateActivityListener.onInvalidateActivity();
            activity.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }

    //Перемещает отмеченные - меняем статус заданий
    void moveRows(ArrayList<String> checkedIds, Tasks.Task.Status status) {
        new moveTasksAsync(checkedIds).execute(status);
    }

    //Класс для выполнения операций пометки на удаление заданий в новом потоке с последующим обновлением рециклервью
    private class deleteTasksAsync extends AsyncTask<Boolean, Integer, Void> {
        protected void onPreExecute() {
            activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
        protected Void doInBackground(Boolean... delete) {
            int position = 0;
            for (Object task: tasks) {
                if (((Tasks.Task) task).checked) {
                    tasks.set(position, oData.deleteTask(((Tasks.Task) task).id, delete[0]));
                    publishProgress((int) position);
                }
                position++;
            }
            return null;
        }
        protected void onProgressUpdate(Integer... position) {
            notifyItemChanged(position[0]);
        }
        protected void onPostExecute(Void voids) {
            activity.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }

    // Помечает на удаление помеченные строки
    void deleteRows(boolean delete) {
        new deleteTasksAsync().execute(delete);
    }

    //Возвращает вью пункта списка
    @NonNull
    @Override
    public ViewHolderTasks onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_list, parent, false);
        return new ViewHolderTasks(view);
    }

    //Заполняет вью пунта списка
    @Override
    public void onBindViewHolder(@NonNull ViewHolderTasks holder, final int position) {
        //Текущий пункт
        holder.task = tasks.get(position);

        //Иконки
        holder.deletedView.setVisibility(holder.task.deleted? View.VISIBLE: View.INVISIBLE);
        holder.postedView.setVisibility(holder.task.posted? View.VISIBLE: View.INVISIBLE);
        holder.thumbView.setVisibility(View.INVISIBLE); //Пока невидима, когда будет окончательно проведенная, выведим картинку

        //Выделяем карточку цветом фона цветом в зависимости от статуса и состояния
        switch (holder.task.status) {
            case POSTED: //Проведен
                if (holder.task.deleted) //Помеченные на удаление - серым
                    holder.itemView.setBackgroundResource(R.color.colorBackgroundGrey);
                else if (holder.task.posted) { //Проведенные
                    holder.thumbView.setVisibility(View.VISIBLE);
                    if (holder.task.achieved) { //Достигшие цели - зеленым + иконка
                        holder.itemView.setBackgroundResource(R.color.colorBackgroundGreen);
                        holder.thumbView.setImageResource(R.drawable.ic_black_thumb_up_alt_24px);
                    } else {//Не достигшие цели - красным
                        holder.itemView.setBackgroundResource(R.color.colorBackgroundRed);
                        holder.thumbView.setImageResource(R.drawable.ic_black_thumb_down_alt_24px);
                    }
                }
                else //Остальное - белым
                    holder.itemView.setBackgroundResource(R.color.cardview_light_background);
                break;
            case APPROVED: //Утвержден
            case IN_WORK: //В работе
            default:
                if (holder.task.deleted) //Помеченные на удаление - серым
                    holder.itemView.setBackgroundResource(R.color.colorBackgroundGrey);
                else if (holder.task.posted) //Проведенные - желтым
                    holder.itemView.setBackgroundResource(R.color.colorBackgroundYellow);
                else //Остальное - белым
                    holder.itemView.setBackgroundResource(R.color.cardview_light_background);
        }
        //Дата и время задания, вид и объект аудита
        holder.dateView.setText(getDateInstance().format(holder.task.date));
        holder.objectView.setText(holder.task.object_name);
        holder.typeView.setText(holder.task.type_name);
        //Номер задания, организация, строка с аналитикой, комментарий, появляются в развернутом пункте
        if (holder.task.expand) { //Развернутый пукнт
            holder.expandImage.setImageResource(R.drawable.ic_black_expand_less_24px);
            holder.numberView.setVisibility(View.VISIBLE);
            holder.numberView.setText(holder.task.number);
            holder.timeView.setVisibility(View.VISIBLE);
            holder.timeView.setText(getTimeInstance().format(holder.task.date));
            holder.analyticsView.setVisibility(View.VISIBLE);
            holder.analyticsView.setText(holder.task.analytic_names);
            holder.commentView.setVisibility(View.VISIBLE);
            holder.commentView.setText(holder.task.comment);
        }
        else { //Свернутый пукнт
            holder.expandImage.setImageResource(R.drawable.ic_black_expand_more_24px);
            holder.numberView.setVisibility(View.GONE);
            holder.timeView.setVisibility(View.GONE);
            holder.analyticsView.setVisibility(View.GONE);
            holder.commentView.setVisibility(View.GONE);
        }
        //Первое задание в группировке по датам занимает все колонки в списке
        ((StaggeredGridLayoutManager.LayoutParams) holder.mView.getLayoutParams()).setFullSpan(holder.task.full);

        //Чекбокс и щелчки на задании:
        switch (modeMenu) {
            case ACTION_BAR: //Обычный режим:
                // чекбокс невидим, можно щелкнуть на задании для его редактирования, можно долго щелкнуть для перехода в режим редактирования списка заданий
                holder.checkedView.setVisibility(View.INVISIBLE);
                holder.checkedView.setTag(null);
                holder.checkedView.setOnClickListener(null);
                // короткий щелчок на задании - открытие формы задания
                holder.itemView.setTag(holder.task);
                holder.itemView.setOnClickListener(onClickListener);
                // длинный щелчок - переход в режим редактирования списка заданий
                holder.itemView.setOnLongClickListener(onLongClickListener);
                break;
            case ACTION_MODE: //Режим контектного меню:
                // чекбокс доступен
                holder.checkedView.setVisibility(View.VISIBLE);
                holder.checkedView.setEnabled(true);
                holder.checkedView.setChecked(holder.task.checked);
                holder.checkedView.setTag(holder.task);
                holder.checkedView.setOnClickListener(onClickListener);
                // недоступны щелчки на задании
                holder.itemView.setTag(null);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                break;
            case ACTION_COPY:
            case ACTION_MOVE: //Режим копирования или переноса
                // чекбокс видим, но недоступен для отмеченных заданий, для остальных чекбокс невидим
                holder.checkedView.setVisibility(holder.task.checked? View.VISIBLE: View.INVISIBLE);
                holder.checkedView.setChecked(true);
                holder.checkedView.setEnabled(false);
                holder.checkedView.setTag(null);
                holder.checkedView.setOnClickListener(null);
                // недоступны щелчки на задании
                holder.itemView.setTag(null);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                break;
        }
        //Область, свернуть/развернуть
        holder.expandView.setTag(holder.task);
        holder.expandView.setOnClickListener(onClickListener);
    }

    //Возвращает количество пунктов
    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
//Фома2018