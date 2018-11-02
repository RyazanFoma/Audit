package com.example.eduardf.audit;

//Класс - предмет аудита:
public class AuditSubject {
    public int id; // идентификатор
    public int auditType; // вид аудита
    public boolean is_group; // группа?
    public int pater; // родитель
    public String name; // наименование
    public String desc; // описание
    public int criterion; // критерий
    public float value; // значение для критерия

    public static final int NEW_SUBJECT_ID = -1; //Код для нового элемента

    public AuditSubject(int id, int auditType, int pater, boolean is_group, String name, String desc, int criterion, float value) {
        this.id = id;
        this.auditType = auditType;
        this.is_group = is_group;
        this.pater = pater;
        this.name = name;
        this.desc = desc;
        this.criterion = criterion;
        this.value = value;
    }
}
