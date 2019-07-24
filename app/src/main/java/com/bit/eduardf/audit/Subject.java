package com.bit.eduardf.audit;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 22.01.19 16:08
 *
 */

/**
 * Предмет аудита
 */
class Subject {
    String id; //Guid предмета
    String name; //Наименование
    String pater; //Guid родителя
    String type; //Guid вида аудита
}
