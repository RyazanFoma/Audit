/*
 * Created by Eduard Fomin on 16.04.20 12:20
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 16.04.20 12:20
 */

package com.bit.eduardf.audit.TaskList;


import io.reactivex.rxjava3.core.Observable;

/**
 * @author e.matsyuk
 */
public interface PagingListener<T> {
    Observable<T> onNextPage(int offset);
}
//@author e.matsyuk