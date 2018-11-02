package com.example.eduardf.audit;

import java.util.ArrayList;
import java.util.List;

//Список пунктов - элементы справочников
public class Items extends ArrayList<Items.Item> {

    //Пункт списка - элемент справочника
    public static class Item {
        String id; //Идентификатор
        String pater; //Родитель или 0, если корень
        boolean folder; //Признак группы
        String name; //Наименование
        boolean deleted; //Пометка на удаление
        boolean predefined; //Предопределенный элемент
        String prenamed; //Предопределенное наименование
        boolean checked; //Отмеченные пункт
        boolean expand; //Развернутый пункт
    }

    //возвращает количество отмеченных пунтов
    public int checkedCount() {
        int checked = 0;
        for(Item item: this) if (item.checked) checked++;
        return checked;
    }

    //Возвращает список идентификаторов отмеченных заданий
    public ArrayList<String> getChecked() {
        ArrayList<String> checked = new ArrayList<String>();
        for(Item item: this) if (item.checked) checked.add(item.id);
        return checked;
    }

    //Отмечает пункты по списку
    public void setChecked(ArrayList<String> checked) {
        if (!(this == null || checked == null || checked.isEmpty()))
            for(Item item: this) item.checked = checked.contains(item.id);
    }

    //Возвращает список идентификаторов развернутых пунтов
    public ArrayList<String> getExpand() {
        ArrayList<String> expand = new ArrayList<String>();
        for(Item item: this) if (item.expand) expand.add(item.id);
        return expand;
    }

    //Разворачивает пунты по списку
    public void setExpand(ArrayList<String> expand) {
        if (!(this == null || expand == null || expand.isEmpty()))
            for(Item item: this) item.expand = expand.contains(item.id);
    }

    //Помечает/отменяет все задания
    public void setCheckedAll(boolean checked, boolean only_child) {
        if (only_child) {
            for(Item item: this) if (!item.folder) item.checked=checked;
        }
        else {
            for(Item item: this) item.checked=checked;
        }
    }

    //Возвращает первый попавшийся отчеченный пункт
    public Items.Item checkedItemFirst() {
        for (Items.Item item: this) if (item.checked) return item;
        return null;
    }

    //Возвращает позицию пункта по id, если не найден 0
    public int getPosition(String id) {
        int i=0;
        for(Items.Item item: this) if (id.equals(item.id)) return i; else i++;
        return 0;
    }

}
