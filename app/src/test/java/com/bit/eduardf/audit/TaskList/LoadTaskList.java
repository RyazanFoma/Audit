/*
 * Created by Eduard Fomin on 20.04.20 16:35
 * Copyright (c) 2020 Eduard Fomin. All rights reserved.
 * Last modified 20.04.20 16:35
 */

package com.bit.eduardf.audit.TaskList;

import com.bit.eduardf.audit.AuditOData;
import com.bit.eduardf.audit.Tasks;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;

/**
 * @author e.matsyuk
 */
class LoadTaskList {

    private AuditOData oData;
    private static volatile LoadTaskList client;

    static LoadTaskList getInstance(AuditOData oData) {
        if (client == null) {
            synchronized (LoadTaskList.class) {
                if (client == null) {
                    client = new LoadTaskList(oData);
                }
            }
        }
        return client;
    }

    private LoadTaskList(AuditOData oData) {
        this.oData = oData;
    }

    Observable<List<Tasks.Task>> getResponse(final String auditor,
                                             final Tasks.Task.Status status,
                                             final String like,
                                             final int skip,
                                             final int load) {
            return Observable.defer(() -> Observable.just(oData.getTasks(auditor, status, like, skip, load)));
    }
}
//Recoding Фома2020
