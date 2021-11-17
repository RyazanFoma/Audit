/*
 * Created by Eduard Fomin on 25.05.20 17:04
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 25.05.20 17:04
 */

package com.bit.eduardf.audit.TaskList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PaginationList<T> {
    public Observable<T> getObservable() {
        return getScrolling()
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .observeOn(Schedulers.from(BackgroundExecutor.getSafeBackgroundExecutor()))
                .switchMap();

    }

    private Observable<Integer> getScrolling() {
        return Observable.create(source -> {

        });
    }
}
