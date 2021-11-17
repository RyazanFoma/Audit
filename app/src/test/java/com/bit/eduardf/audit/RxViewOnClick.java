/*
 * Created by Eduard Fomin on 03.02.20 14:38
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 03.02.20 14:38
 */

package com.bit.eduardf.audit;

import android.support.annotation.NonNull;
import android.view.View;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class RxViewOnClick {

    public static Observable<String> getTextWatcherObservable(@NonNull final View view) {

        final PublishSubject<String> subject = PublishSubject.create();

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subject.onNext(v.toString());
            }
        });
        return subject;
    }

}
