package com.bit.eduardf.audit;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 22.11.18 13:32
 *
 */

/**
 * Вспомогательный класс для отслеживания загрузочных страниц
 */
class CurrentPage {

    private int currentPage; //Текущая страница
    private final int top; //Порция загрузки записей (время загрузки д.б. < 1 сек.)
    private int skip; //Количество пропущенных записей при загрузке
    private boolean lastPage; //Признак последней страницы

    /**
     * Порция загрузки записей
     * @return - Количество записей в одной загрузке
     */
    int top() {
        return top;
    }

    /**
     * Количество пропускаемых записей
     * @return - Количество записей, пропускаемых перед загрузкой
     */
    int skip() {
        return skip;
    }

    /**
     * Конструктор
     */
    CurrentPage(int top) {
        currentPage = 0;
        this.top = top;
        skip = 0;
        lastPage = false;
    }

    /**
     * Последняя страница?
     * @return true - если страница последняя
     */
    boolean isLastPage() {
        return lastPage;
    }

    /**
     * Установка признака последней страницы
     * @param lastPage - true - если страница последняя
     */
    void setLastPage(boolean lastPage) {
        this.lastPage = lastPage;
    }

    /**
     * Переход на следущую страницу
     * @param portion - порция загруженных данных
     */
    void nextPage(int portion) {
        currentPage++;
        skip = currentPage*top;
        lastPage = portion < top; //Если загрузили меньше чем хотели
    }

    /**
     * Установка текущей страницы по позиции записи
     * @param position - позиция записи
     */
    void setPageByPosition(int position) {
        currentPage = position/top;
        skip = currentPage*top;
        lastPage = false;
    }
}
//Фома2018