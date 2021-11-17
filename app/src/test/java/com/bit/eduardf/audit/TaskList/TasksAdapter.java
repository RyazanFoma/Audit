/*
 * Created by Eduard Fomin on 08.04.20 12:56
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 08.04.20 12:56
 */

package com.bit.eduardf.audit.TaskList;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bit.eduardf.audit.R;
import com.bit.eduardf.audit.Tasks;
import com.bit.eduardf.audit.ViewHolderTasks;

import java.util.ArrayList;

import static com.bit.eduardf.audit.ReferenceChoice.ACTION_BAR;
import static com.bit.eduardf.audit.ReferenceChoice.ACTION_COPY;
import static com.bit.eduardf.audit.ReferenceChoice.ACTION_MODE;
import static com.bit.eduardf.audit.ReferenceChoice.ACTION_MOVE;
import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

/**
 * Task List Adapter for RecyclerView and Download Pagination
 */
public class TasksAdapter extends AbstractListAdapter<Tasks.Task> {

    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;
    private int menuMode;

    private static final String ARG_LIST = "list";
    private static final String ARG_MODE_MENU = "menu_mode";

    /**
     * Constructor
     * @param fragment - contains OnClickListener, OnLongClickListener callbacks
     * @param menuMode - fragment menu mode
     */
    public TasksAdapter(Fragment fragment, int menuMode) {
        if (fragment instanceof View.OnClickListener &&
                fragment instanceof View.OnLongClickListener) {
            onClickListener = (View.OnClickListener) fragment;
            onLongClickListener = (View.OnLongClickListener) fragment;
        }
        else {
            throw new RuntimeException(fragment.toString()
                    + " must implement OnClickListener, OnLongClickListener and OnInvalidateActivity");
        }
        this.menuMode = menuMode;
    }

//    @Override
//    public long getItemId(int position) {
//        return getItem(position).getId();
//    }

    /**
     * Notify of a change in the data set and the new menu mode
     * @param menuMode - the new menu mode
     */
    public void notifyDataSetChanged(int menuMode) {
        this.menuMode = menuMode;
        notifyDataSetChanged();
    }

    /**
     * Save menu mode and task list
     * @param outState - place of conservation
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_MODE_MENU, menuMode);
        ((Tasks)getItems()).onSaveInstanceState(outState, ARG_LIST);
    }

    /**
     * Restore menu mode and task list
     * @param savedInstanceState - place of conservation
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        menuMode = savedInstanceState.getInt(ARG_MODE_MENU);
        final Tasks tasks = new Tasks();
        tasks.onRestoreInstanceState(savedInstanceState, ARG_LIST);
        addNewItems(tasks);
    }

    @Override
    public int checkedCount() {
        return ((Tasks) getItems()).checkedCount();
    }

    @Override
    public int itemCount() {
        return ((Tasks) getItems()).getCount();
    }

    @Override
    public void setChecked(ArrayList<String> checked) {
        ((Tasks) getItems()).setChecked(checked);
    }

    @Override
    public ArrayList<String> getChecked() {
        return ((Tasks) getItems()).getChecked();
    }

    @Override
    public void setCheckedAll(boolean checked) {
        ((Tasks) getItems()).setCheckedAll(checked);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolderTasks(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        final ViewHolderTasks holder = (ViewHolderTasks) viewHolder;

        holder.task = getItem(position);
        if (holder.task.group) {
            // The first task in the group is a dummy task that spans all columns.
            onBindViewHolderGroup(holder);
        }
        else {
            // All subsequent tasks in a group are normal tasks that span multiple columns.
            onBindViewHolderItem(holder);
        }
    }

    /**
     * Binding a group card to the first task in the group
     * @param holder - view holder
     */
    private void onBindViewHolderGroup(ViewHolderTasks holder) {
        ((StaggeredGridLayoutManager.LayoutParams) holder.mView.getLayoutParams()).
                setFullSpan(true);
        holder.groupView.setVisibility(View.VISIBLE);
        holder.elementView.setVisibility(View.GONE);
        holder.nameView.setText(getDateInstance().format(holder.task.date));
        holder.itemView.setBackgroundResource(R.color.colorBackgroundItem);
    }

    /**
     * Binding an item card to subsequent tasks in a group
     * @param holder - view holder
     */
    private void onBindViewHolderItem(ViewHolderTasks holder) {

        final Tasks.Task task = holder.task;

        ((StaggeredGridLayoutManager.LayoutParams) holder.mView.getLayoutParams()).
                setFullSpan(false);
        holder.elementView.setVisibility(View.VISIBLE);
        holder.groupView.setVisibility(View.GONE);
        holder.numberView.setText(task.number);
        holder.dateView.setText(getDateInstance().format(task.date));
        holder.objectView.setText(task.object_name);
        holder.typeView.setText(task.type_name);

        onBindViewHolderStatus(holder);
        onBindViewHolderExpand(holder);
        onBindViewHolderMenuMode(holder);
    }

    /**
     * Binding card color and icon visibility to task status
     * @param holder - view holder
     */
    private void onBindViewHolderStatus(ViewHolderTasks holder) {

        final Tasks.Task task = holder.task;

        holder.deletedView.setVisibility(task.deleted ? View.VISIBLE : View.INVISIBLE);
        holder.postedView.setVisibility(task.posted ? View.VISIBLE : View.INVISIBLE);
        holder.thumbView.setVisibility(View.INVISIBLE);
        holder.itemView.setBackgroundResource(R.color.cardview_light_background);
        switch (task.status) {
            case POSTED:
                if (task.deleted) //Помеченные на удаление - серым
                    holder.itemView.setBackgroundResource(R.color.colorBackgroundGrey);
                else if (task.posted) {
                    holder.thumbView.setVisibility(View.VISIBLE);
                    if (task.achieved) {
                        holder.itemView.setBackgroundResource(R.color.colorBackgroundGreen);
                        holder.thumbView.setImageResource(R.drawable.ic_black_thumb_up_alt_24px);
                    } else {
                        holder.itemView.setBackgroundResource(R.color.colorBackgroundRed);
                        holder.thumbView.setImageResource(R.drawable.ic_black_thumb_down_alt_24px);
                    }
                } else
                    holder.itemView.setBackgroundResource(R.color.cardview_light_background);
                break;
            case APPROVED:
            case IN_WORK:
            default:
                if (task.deleted)
                    holder.itemView.setBackgroundResource(R.color.colorBackgroundGrey);
                else if (task.posted)
                    holder.itemView.setBackgroundResource(R.color.colorBackgroundYellow);
                else
                    holder.itemView.setBackgroundResource(R.color.cardview_light_background);
        }
    }

    /**
     * Binding extended or shorted view of the task card
     * @param holder - view holder
     */
    private void onBindViewHolderExpand(ViewHolderTasks holder) {

        final Tasks.Task task = holder.task;

        if (task.expand) {
            holder.expandImage.setImageResource(R.drawable.ic_black_expand_less_24px);
            holder.timeView.setVisibility(View.VISIBLE);
            holder.timeView.setText(getTimeInstance().format(task.date));
            holder.analyticsView.setVisibility(View.VISIBLE);
            holder.analyticsView.setText(task.analytic_names);
            holder.commentView.setVisibility(View.VISIBLE);
            holder.commentView.setText(task.comment);
        } else {
            holder.expandImage.setImageResource(R.drawable.ic_black_expand_more_24px);
            holder.timeView.setVisibility(View.GONE);
            holder.analyticsView.setVisibility(View.GONE);
            holder.commentView.setVisibility(View.GONE);
        }
        holder.expandImage.setTag(task);
        holder.expandImage.setOnClickListener(onClickListener);
    }

    /**
     * Binding task check box and task card to task data depending on menu mode
     * @param holder - view holder
     */
    private void onBindViewHolderMenuMode(ViewHolderTasks holder) {

        final Tasks.Task task = holder.task;

        switch (menuMode) {
            case ACTION_MODE: //context menu mode
                holder.checkedView.setVisibility(View.VISIBLE);
                holder.checkedView.setEnabled(true);
                holder.checkedView.setChecked(task.checked);
                holder.checkedView.setTag(task);
                holder.checkedView.setOnClickListener(onClickListener);
                holder.itemView.setTag(null);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                break;
            case ACTION_COPY:
            case ACTION_MOVE: //copy or move mode
                holder.checkedView.setVisibility(task.checked ? View.VISIBLE : View.INVISIBLE);
                holder.checkedView.setChecked(true);
                holder.checkedView.setEnabled(false);
                holder.checkedView.setTag(null);
                holder.checkedView.setOnClickListener(null);
                holder.itemView.setTag(null);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                break;
            case ACTION_BAR: default: //normal menu mode
                holder.checkedView.setVisibility(View.INVISIBLE);
                holder.checkedView.setTag(null);
                holder.checkedView.setOnClickListener(null);
                holder.itemView.setTag(task);
                holder.itemView.setOnClickListener(onClickListener);
                holder.itemView.setOnLongClickListener(onLongClickListener);
                break;
        }
    }

}
//Фома2020