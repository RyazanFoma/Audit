package com.example.eduardf.audit;

import java.util.ArrayList;

//Список пунктов
public class Items {

    private ArrayList<Item> list;

    public Items(int count) {
        list = new ArrayList<Item>(count);
    }

    public Items() {
        list = new ArrayList<Item>();
    }

    // добавить пункт
    public void add(Item item) { list.add(item); }

    public ArrayList<Item> getItems() { return list; }

    public Item get(int index) { return list.get(index); }

    public int size() { return list.size(); }

    public boolean isEmpty() { return list.isEmpty(); }

    public Item remove(int index) { return list.remove(index); }

    public boolean remove(Object object) { return list.remove(object); }

    public void clear() {list.clear();}

    public void addAll(Items items) {list.addAll(items.getItems());}

    public int indexOf(Item item) { return list.indexOf(item); }

    //Пункт списка
    public static class Item {
        int id; //Идентификатор
        int pater; //Родитель или 0, если корень
        boolean folder; //Признак группы
        int folders; //число папок в группе
        int files; //число файлов в группе
        String name; //Наименование
        String desc; //Описание
        boolean checked; //Отмеченные пункты

        //Конструктор пункта
        public Item( int id, boolean folder, int folders, int files, int pater, String name, String desc) {
            this.id = id;
            this.folder = folder;
            this.folders = folders;
            this.files = files;
            this.pater = pater;
            this.name = name;
            this.desc = desc;
            this.checked = false;
        }
    }
}
