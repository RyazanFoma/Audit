package com.bit.eduardf.audit;

/*
 * *
 *  * Created by Eduard Fomin on 06.02.19 15:28
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 06.02.19 15:28
 *
 */

import android.os.Bundle;
import android.support.annotation.NonNull;

class AObject {
    String id; //Идентификатор
    String name; //Наименование
    String objectType; //Тип объекта

    void onSaveInstanceState(@NonNull Bundle outState, String argName) {
        outState.putParcelable(argName, new ParcelableObject(this));
    }

    void onRestoreInstanceState(Bundle savedInstanceState, String argName) {
        if (savedInstanceState.containsKey(argName)) {
            final ParcelableObject parcelable = savedInstanceState.getParcelable(argName);
            if (parcelable != null) {
                final AObject object = parcelable.object;
                id = object.id;
                name = object.name;
                objectType = object.objectType;
            }
        }
    }

}
//Фома2019