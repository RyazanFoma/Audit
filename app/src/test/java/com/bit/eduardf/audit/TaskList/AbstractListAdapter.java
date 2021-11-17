/*
 * Created by Eduard Fomin on 06.04.20 15:45
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 06.04.20 15:45
 */

package com.bit.eduardf.audit.TaskList;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractListAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<T> items = new ArrayList<>();
    private boolean allLoaded;

    public final static int CHECKED_STATUS_NULL = 0;
    public final static int CHECKED_STATUS_SOME = 2;
    public final static int CHECKED_STATUS_ALL = 3;

    public boolean isAllLoaded() {
        return allLoaded;
    }

    public void addNewItems(List<T> items) {
        if (items.size() == 0) {
            allLoaded = true;
            return;
        }
        this.items.addAll(items);
    }

    public void addNewItem(T item) {
        items.add(item);
    }

    public List<T> getItems() {
        return items;
    }

    public T getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void notifyItemChanged(T item) {
        notifyItemChanged(items.indexOf(item));
    }

    public int getStatus() {
        int count = itemCount();
        int checked = checkedCount();
        if (checked==0 || count==0) return CHECKED_STATUS_NULL;
        else if (checked==count) return CHECKED_STATUS_ALL;
        return CHECKED_STATUS_SOME;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public abstract int checkedCount();

    public abstract int itemCount();

    public abstract void setChecked(ArrayList<String> checked);

    public abstract ArrayList<String> getChecked();

    public abstract void setCheckedAll(boolean checked);

    public abstract void onSaveInstanceState(Bundle outState);

    public abstract void onRestoreInstanceState(Bundle savedInstanceState);

}
//Фома2020