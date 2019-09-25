package com.bit.eduardf.audit;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.apache.olingo.client.api.communication.request.cud.UpdateType;
import org.apache.olingo.client.api.communication.response.ODataEntityCreateResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.http.HttpClientException;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.core.http.BasicAuthHttpClientFactory;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.jetbrains.annotations.NotNull;

import static com.bit.eduardf.audit.ParcelableUser.USER_ID;
import static com.bit.eduardf.audit.ParcelableUser.USER_NAME;
import static com.bit.eduardf.audit.ParcelableUser.USER_OBJECT;
import static com.bit.eduardf.audit.ParcelableUser.USER_ORGANIZATION;
import static com.bit.eduardf.audit.ParcelableUser.USER_PASSWORD;
import static com.bit.eduardf.audit.ParcelableUser.USER_RESPONSIBLE;
import static com.bit.eduardf.audit.ParcelableUser.USER_TYPE;

/*
 * *
 *  * Created by Eduard Fomin on 05.02.19 9:42
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 04.02.19 16:15
 *
 */

//Класс для доступа в 1С:Аудитор через OData
public class AuditOData {

    static final String EMPTY_KEY = "00000000-0000-0000-0000-000000000000"; //Пустая ссылка

    private static final String WITHOUT_TABLE = "**";
    private static final String DATE_FORMAT_1C = "yyyy-MM-dd'T'HH:mm:ss"; //Шаблон формата даты для 1С

    //Виды иерархии справочников:
    private static final int NOT_HIERARCHY = 0; // нет иерархии
    private static final int FOLDER_HIERARCHY = 1; // папок и элементов
    private static final int ELEMENT_HIERARCHY = 2; // только элеметов

    /**
     * Таблицы 1C
     */
    enum Set {
        AUDITOR("Аудиторы", "Catalog_Аудит_Аудиторы", ELEMENT_HIERARCHY, false),
        TYPE("ВидыАудитов", "Catalog_Аудит_ВидыАудитов", FOLDER_HIERARCHY, false),
        ORGANIZATION("Организации", "Catalog_Аудит_Организации", ELEMENT_HIERARCHY, true),
        OBJECT("Объекты", "Catalog_Аудит_Объекты", ELEMENT_HIERARCHY, true),
        RESPONSIBLE("Ответственные", "Catalog_Аудит_Ответственные", ELEMENT_HIERARCHY, true),
        ANALYTIC("АналитикиОбъектов", "Catalog_Аудит_АналитикиОбъектов", ELEMENT_HIERARCHY, true),
        INDICATOR("ПоказателиАудитов", "Catalog_Аудит_ПоказателиАудитов", FOLDER_HIERARCHY, false),
        SUBJECT("ПредметыАудитов", "Catalog_Аудит_ПредметыАудитов", FOLDER_HIERARCHY, false),
        UNIT("ЕдиницыИзмерения", "Catalog_Аудит_ЕдиницыИзмерения", ELEMENT_HIERARCHY, true),
        INDICATOR_STANDARD("НормативыПоказателей",
                "InformationRegister_Аудит_НормативыЗначенийПоказателей", NOT_HIERARCHY, false),
        TASK("Задания", "Document_Аудит_ЗаданиеНаАудит", NOT_HIERARCHY, true),
        TASK_ANALYTIC("АналитикаЗаданий", "Document_Аудит_ЗаданиеНаАудит_АналитикаОбъекта",
                NOT_HIERARCHY, true),
        TASK_INDICATOR("ПоказателиЗаданий", "Document_Аудит_ЗаданиеНаАудит_Показатели",
                NOT_HIERARCHY, true),
        OBJECT_TYPES("ТипыОбъектаАудита", "InformationRegister_Аудит_ТипыОбъектовПоВидамАудита",
                NOT_HIERARCHY, false),
        ANALYTIC_CORR("СоответствияАналитик",
                "InformationRegister_Аудит_СоответствияАналитикОбъекту", NOT_HIERARCHY, false),
        ANALYTIC_RELAT("СвязьПоТипамАналитик",
                "InformationRegister_Аудит_СвязьПоТипамОбъектаАналитики", NOT_HIERARCHY, false),
        MEDIA_FILES("Файлы",
                "InformationRegister_Аудит_Файлы", NOT_HIERARCHY, true);

        String id;
        String name;
        int hierarchy;
        boolean editable;

        Set(String id, String name, int hierarchy, boolean editable) {
            this.id = id;
            this.name = name;
            this.hierarchy = hierarchy;
            this.editable = editable;
        }

        @Override
        public String toString() {
            return this.id;
        }

        /**
         * Определение значения перечесления по идентификатору
         * @param key - идентификатор
         * @return - значение перечисления
         */
        static public Set toValue(String key) {
            if (key == null) return null;
            switch (key) {
                case "Аудиторы": return AUDITOR;
                case "ВидыАудитов": return TYPE;
                case "Организации": return ORGANIZATION;
                case "Объекты": return OBJECT;
                case "Ответственные": return RESPONSIBLE;
                case "АналитикиОбъектов": return ANALYTIC;
                case "ПоказателиАудитов": return INDICATOR;
                case "ПредметыАудитов": return SUBJECT;
                case "ЕдиницыИзмерения": return UNIT;
                case "НормативыПоказателей": return INDICATOR_STANDARD;
                case "Задания": return TASK;
                case "АналитикаЗаданий": return TASK_ANALYTIC;
                case "ПоказателиЗаданий": return TASK_INDICATOR;
                default: return null;
            }
        }
    }

    //Фильтр по справочнику аудиторов, чтобы отфильтровать пользователей мобильных приложений
    private static final String FILTER_AUDITORS = "ParentType eq 'СправочникСсылка.Аудит_Аудиторы'";

    //Поля справочника аудиторы
    private static final String USERS_PASSWORD = "Пароль";
    private static final String USERS_TYPE = "ВидАудитаПоУмолчанию_Key";
    private static final String USERS_OBJECT = "ОбъектПоУмолчанию_Key";
    private static final String USERS_ORGANIZATION = "ОрганизацияПоУмолчанию_Key";
    private static final String USERS_RESPONSIBLE = "ОтветственныйПоУмолчанию_Key";

    //Порядок соритровки
    private static final String ORDER_ASC = " asc";
    private static final String ORDER_DESC = " desc";

    //Общие поля
    private static final String COMMON_KEY = "Ref_Key";
    private static final String COMMON_LINE = "LineNumber";
    private static final String COMMON_DELETED = "DeletionMark";
    private static final String COMMON_POSTED = "Posted";
    private static final String COMMON_NAME = "Description";
    private static final String COMMON_FOLDER = "IsFolder";
    private static final String COMMON_GROUP = "GroupMark";
    private static final String COMMON_OWNER = "Owner_Key";
    private static final String COMMON_PARENT = "Parent_Key";
//    private static final String COMMON_CODE = "Code";
    private static final String COMMON_PREDEFINED = "Predefined";
    private static final String COMMON_PRENAMED = "PredefinedDataName";
    private static final String COMMON_COMMENT = "Комментарий";
    private static final String COMMON_GOAL = "ЦелевоеЗначение";
    private static final String COMMON_MINIMUM = "МинимальноеЗначение";
    private static final String COMMON_MAXIMUM = "МаксимальноеЗначение";
    private static final String COMMON_ERROR = "Погрешность";
    private static final String COMMON_ORDER = "Приоритет";
    //Суффикс к свойству с типом
    private static final String _TYPE = "_Type";
    //Типы данных в показателях
    private static final String UNDEFINED_TYPE = "StandardODATA.Undefined";
    private static final String DOUBLE_TYPE = "Edm.Double";
    private static final String BOOLEAN_TYPE = "Edm.Boolean";
    private static final String DATE_TYPE = "Edm.DateTime";

    //Поля заданий на аудит
    private static final String TASK_STATUS = "Статус";
    private static final String TASK_DATE = "Date";
    private static final String TASK_NUMBER = "Number";
    private static final String TASK_AUDITOR_KEY = "Аудитор_Key";
    private static final String TASK_TYPE_KEY = "ВидАудита_Key";
    private static final String TASK_TYPE = "ВидАудита";
    private static final String TASK_ORGANIZATION_KEY = "Организация_Key";
    private static final String TASK_OBJECT_KEY = "Объект_Key";
    private static final String TASK_OBJECT = "Объект";
    private static final String TASK_RESPONSIBLE_KEY = "Ответственный_Key";
    private static final String TASK_ANALYTIC_NAMES = "АналитикиСтрокой";
    private static final String TASK_ACHIEVED = "ЦельАудитаДостигнута";
    private static final String TASK_ANALYTICS = "АналитикаОбъекта";
        private static final String TASK_ANALYTIC_VALUE = "Значение_Key";
    private static final String TASK_INDICATORS = "Показатели";
        private static final String TASK_INDICATOR_KEY = "Показатель_Key";
        private static final String TASK_INDICATOR_VALUE = "ФактическоеЗначение";
        private static final String TASK_INDICATOR_ACHIEVED = "ЦельПоказателяДостигнута";

    //Поля видов аудита
    private static final String TYPE_CRITERION = "КритерийДостиженияЦели";
    private static final String TYPE_VALUE = "ЦелевоеЗначение";
    private static final String TYPE_FILL_ACTUAL_VALUE = "ЗаполнятьФактическиеЗначенияПоУмолчанию";
    private static final String TYPE_OPEN_WITH_INDICATORS = "ОткрыватьЗаданияСПоказателей";
    private static final String TYPE_CLEAR_COPY = "ОчищатьЗаданиеПриКопировании";
    private static final String TYPE_SHOW_SUBJECT = "ПоказыватьПредметыПоказателей";
    private static final String TYPE_SELECTION = "ВидОтбораАналитикДляОбъектаАудита";

    //Поля показателей аудита
    private static final String INDICATOR_SUBJECT = "ПредметАудита_Key";
    private static final String INDICATOR_NOT_INVOLVED = "НеУчаствует";
    private static final String INDICATOR_CRITERION = "Критерий";
    private static final String INDICATOR_TYPE = "ТипПоказателя";
    private static final String INDICATOR_UNIT = "ЕдиницаИзмерения_Key";

    //Поля нормативов показателей
    private static final String INDICATOR_STANDARD_TYPE = "ВидАудита_Key";
    private static final String INDICATOR_STANDARD_OBJECT = "Объект_Key";
    private static final String INDICATOR_STANDARD_INDICATOR = "Показатель_Key";

    //Поля объектов аудита
    private static final String OBJECT_TYPE = "ParentType";

    //Поля регистра типов объектов вида аудита
    private static final String OBJECT_TYPES_KEY = "ВидАудита_Key";
    private static final String OBJECT_TYPES_TYPE = "ТипОбъектаАудита";

    //Поля регистра соответствия аналитик виду и объекту аудита
    private static final String CORR_TYPE_KEY = "ВидАудита_Key";
    private static final String CORR_OBJECT_KEY = "Объект_Key";
    private static final String CORR_ANALYTIC_KEY = "Аналитика_Key";
    private static final String CORR_ANALYTIC = "Аналитика";

    //Поля регистра соответсвия типов аналитик, объектов по виду аудита
    private static final String RELAT_TYPE_KEY = "ВидАудита_Key";
    private static final String RELAT_OBJECT_TYPE = "ТипОбъектаАудита";
    private static final String RELAT_ANALYTIC_TYPE = "ТипАналитикиОбъекта";

    //Поля регистра медиафайлов
    private static final String MEDIA_TASK_KEY = "ЗаданиеНаАудит_Key";
    private static final String MEDIA_INDICATOR_KEY = "ПоказательАудита_Key";
    private static final String MEDIA_INDICATOR = "ПоказательАудита";
    private static final String MEDIA_FILE_TYPE = "ТипФайла";
    private static final String MEDIA_FILE_NAME = "ИмяФайла";
    private static final String MEDIA_AUTHOR_KEY = "Автор_Key";
    private static final String MEDIA_AUTHOR = "Автор";
    private static final String MEDIA_FILE_DATE = "ДатаСоздания";
    private static final String MEDIA_COMMENT = "Комментарий";

    //Реквизиты класса
    private final ODataClient client; //Клиент OData
    private final String serviceRootOData; //Путь к oData

    //Конструктор
    AuditOData(Context context) {
        final SharedPreferences pr = PreferenceManager.getDefaultSharedPreferences(context);
        serviceRootOData = pr.getString("odata_path", "");
        client = ODataClientFactory.getClient();
        client.getConfiguration().setDefaultPubFormat(ContentType.JSON_NO_METADATA);
        client.getConfiguration().setHttpClientFactory(new BasicAuthHttpClientFactory(
                pr.getString("odata_user", ""),
                pr.getString("odata_password", "")));
    }

    //Возвращает значение в виде объекта, определенного вида
    private Object getValue(ClientComplexValue complexValue, String name) {
        final String string = complexValue.get(name).getPrimitiveValue().toString();
        switch (complexValue.get(name+_TYPE).getPrimitiveValue().toString()) {
            case BOOLEAN_TYPE:
                return string.equals("true");
            case DOUBLE_TYPE:
                try {
                    return Float.parseFloat(string);
                }
                catch (NumberFormatException e) {
                    break;
                }
            case DATE_TYPE:
                try {
                    return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).parse(string);
                } catch (ParseException e) {
                    break;
                }
            case UNDEFINED_TYPE: default:
                break;
        }
        return null;
    }

    //Возвращает значение в виде объекта, определенного вида
    private Object getValue(ClientEntity entity, String name) {
        final String string = entity.getProperty(name).getPrimitiveValue().toString();
        switch (entity.getProperty(name+_TYPE).getPrimitiveValue().toString()) {
            case BOOLEAN_TYPE:
                return string.equals("true");
            case DOUBLE_TYPE:
                try {
                    return Float.parseFloat(string);
                }
                catch (NumberFormatException e) {
                    break;
                }
            case DATE_TYPE:
                try {
                    return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).parse(string);
                } catch (ParseException e) {
                    break;
                }
            case UNDEFINED_TYPE: default:
                break;
        }
        return null;
    }

    //Добавляет в комплекс значение и тип объекта
    private void addValue(@NonNull ClientComplexValue complexValue, @NonNull String property, Object value) {
        String string = "";
        String type = UNDEFINED_TYPE;
        if (value != null)
            switch (value.getClass().getCanonicalName()) {
                case "java.util.Date":
                    string = (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(value);
                    type = DATE_TYPE;
                    break;
                case "java.lang.Float": {
                    final float f = (Float) value;
                    if (f == (long)f) string = String.format(Locale.US, "%d", (long)f);
                    else string = String.format(Locale.US, "%s", f);
                    type = DOUBLE_TYPE;
                    break;
                }
                case "java.lang.Boolean":
                    string = (Boolean)value?"true":"false";
                    type = BOOLEAN_TYPE;
            }
        complexValue.add(client.getObjectFactory().newPrimitiveProperty(property,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(string)));
        complexValue.add(client.getObjectFactory().newPrimitiveProperty(property+_TYPE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type)));
    }

    //возвращает entity
    private ClientEntity getFullEntity(Set table, String key) {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataRetrieveResponse<ClientEntity> entity = client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
        return entity.getBody();
    }

    //Возвращает true, если ключ - пустая ссылка
//    private boolean noEmptyKey(String key) { return !(key==null || EMPTY_KEY.equals(key)); }

    //Выводит сообщение об ошибке в диалоговом окне
    private boolean sayErrorMessage(@NonNull Exception e) {
        return true;
    }

    /**
     *  Наименование элемента справочника по id
     *  @param table Имя сущности
     *  @param key Guid экземпляра сущности
     *  @return Свойство Description экземпляра сущности или "", если key пустой
     */
    String getName(Set table, String key) {
        if (!(key == null || key.isEmpty() || EMPTY_KEY.equals(key))) {
            final URI entityURI = client.newURIBuilder(serviceRootOData)
                    .appendEntitySetSegment(table.name)
                    .appendKeySegment("guid'"+key+"'")
                    .select(COMMON_NAME)
                    .build();
            try {
                final ODataRetrieveResponse<ClientEntity> entity =
                        client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
                return entity.getBody().getProperty(COMMON_NAME).getValue().toString();
            } catch (ODataRuntimeException e) {
                if (sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.getName('" + table.name + "', guid'" + key +
                            "') Error on requesting of id ." + e.getMessage());
                }
            }
        }
        return "";
    }

    // возвращает guid или null вместо EMPTY_KEY
    @Nullable
    private String getKey(String key) {
        if (key.isEmpty() || EMPTY_KEY.equals(key)) return null;
        return key;
    }

    /**
     * Получить элемент по guid из множества
     * @param key - guid элемента
     * @param set - множество
     * @return - элемент
     */
    Object getObject(String key, Set set) {
        switch (set) {
            case TYPE:
                return getAType(key);
            case OBJECT:
                return getAObject(key);
            default:
                return getItem(set, key);
        }
    }

    //ВСЕ ДЛЯ СПИСКА АУДИТОРОВ
    //возвращает список Entity с аудиторами
    private List<ClientEntity> getAllUsers() throws HttpClientException, ODataRuntimeException {
        final URI userEntitySetURI = client
                .newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.AUDITOR.name)
                .filter(FILTER_AUDITORS)
                .select(COMMON_KEY, COMMON_NAME, USERS_PASSWORD,
                        USERS_TYPE, USERS_OBJECT, USERS_ORGANIZATION, USERS_RESPONSIBLE)
                .build();
        final ODataRetrieveResponse<ClientEntitySet>
                users = client.getRetrieveRequestFactory()
                .getEntitySetRequest(userEntitySetURI).execute();
        return users.getBody().getEntities();
    }

    //возвращает список Map с аудиторами для Spinner
    List<Map<String, Object>> getUsers() throws ODataErrorException {
        List<Map<String, Object>> usersMap = new ArrayList<>();
        try {
            for (ClientEntity clientEntity : getAllUsers()) {
                Map<String, Object> user = new HashMap<>();
                user.put(USER_ID, clientEntity.getProperty(COMMON_KEY).getValue());
                user.put(USER_NAME, clientEntity.getProperty(COMMON_NAME).getValue());
                user.put(USER_PASSWORD, clientEntity.getProperty(USERS_PASSWORD).getValue());
                user.put(USER_TYPE, clientEntity.getProperty(USERS_TYPE).getValue());
                user.put(USER_OBJECT, clientEntity.getProperty(USERS_OBJECT).getValue());
                user.put(USER_ORGANIZATION, clientEntity.getProperty(USERS_ORGANIZATION).getValue());
                user.put(USER_RESPONSIBLE, clientEntity.getProperty(USERS_RESPONSIBLE).getValue());
                usersMap.add(user);
            }
        }
        catch (HttpClientException | ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting user list");
        }
        return usersMap;
    }

    /**
     * Подготовить ентити с индивидуальными настройками пользователя
     * @param type -  giud вида аудита
     * @param organization - giud организации
     * @param object - giud объекта
     * @param responsible - giud ответственного
     * @return ентити с настройками
     */
    private ClientEntity bindUserEntity(@NotNull String type, @NotNull String organization, @NotNull String object, @NotNull String responsible) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        final List<ClientProperty> properties = entity.getProperties();
        properties.add(client.getObjectFactory().newPrimitiveProperty(USERS_TYPE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(type))));
        properties.add(client.getObjectFactory().newPrimitiveProperty(USERS_ORGANIZATION,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(organization))));
        properties.add(client.getObjectFactory().newPrimitiveProperty(USERS_OBJECT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(object))));
        properties.add(client.getObjectFactory().newPrimitiveProperty(USERS_RESPONSIBLE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(responsible))));
        return entity;
    }

    /**
     * Сохранить параметры пользователя
     * @param auditor - giud пользователя
     * @param type -  giud вида аудита
     * @param organization - giud организации
     * @param object - giud объекта
     * @param responsible - giud ответственного
     */
    void saveUser(@NotNull String auditor, @NotNull String type, @NotNull String organization, @NotNull String object, @NotNull String responsible) {
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.AUDITOR.name)
                .appendKeySegment("guid'"+auditor+"'")
                .build();
        try {
            ODataEntityUpdateRequest<ClientEntity> request = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH,
                    bindUserEntity(type, organization, object, responsible));
            request.execute();
//            client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH,
//                    bindUserEntity(type, organization, object, responsible)).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.saveUser('"+auditor+
                        "', ...) Error on update of user. "+e.getMessage());
            }
        }
    }

    //ВСЕ ДЛЯ ЗАДАНИЙ НА АУДИТ
    //возвращает список Entity с заданиями с обором по аудитору, статусу и наименованию
    private List<ClientEntity> getAllTasks(String auditor, Tasks.Task.Status status, String like, int... skip) {
        String filter = TASK_AUDITOR_KEY+" eq guid'"+auditor+"' and "+TASK_STATUS+" eq '"+status.id+"'";
        if (!(like==null||like.isEmpty())) filter += " and substringof('"+like+"',"+TASK_OBJECT+"/"+COMMON_NAME+")";
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        uriBuilder.appendEntitySetSegment(Set.TASK.name)
                .select(COMMON_KEY, TASK_DATE, COMMON_DELETED, COMMON_POSTED, TASK_ACHIEVED, TASK_NUMBER,
                        TASK_TYPE_KEY, TASK_OBJECT_KEY, COMMON_COMMENT, TASK_ANALYTIC_NAMES,
                        TASK_TYPE+"/"+COMMON_NAME, TASK_OBJECT+"/"+COMMON_NAME, WITHOUT_TABLE)
                .expand(TASK_TYPE, TASK_OBJECT)
                .filter(filter)
                .orderBy(TASK_DATE +ORDER_DESC);
        if (skip.length == 2) uriBuilder.skip(skip[0]).top(skip[1]);
        final URI entitySetURI = uriBuilder.build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    //извлекает дату из строки
    private Date parseDate(String string) {
        try {
            return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).parse(string);
        } catch (ParseException e) {
            return null;
        }
    }

    // возвращает строку с датой
    private String bindDate(Date date) {
        if (date != null)
            return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(date);
        else {
            Date empty = new Date();
            empty.setTime(0);
            return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(empty);
        }
    }

    // возвращает коллекцию аналитикой для табличной части задания
    private ClientCollectionValue<ClientValue> bindTaskAnalytics(List<String> analytics) {
        int line = 1;
        final ClientCollectionValue<ClientValue> collectionValue = client.getObjectFactory().newCollectionValue(null);
        for (String key: analytics) {
            final ClientComplexValue complexValue = client.getObjectFactory().newComplexValue(null);
            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_LINE,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(String.valueOf(line++))));
            complexValue.add(client.getObjectFactory().newPrimitiveProperty(TASK_ANALYTIC_VALUE,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(key)));
            collectionValue.add(complexValue);
        }
        return collectionValue;
    }

    // возвращает коллекцию показателей для табличной части задания
    private ClientCollectionValue<ClientValue> bindTaskIndicators(List<Tasks.Task.IndicatorRow> indicators) {
        int line = 1;
        final ClientCollectionValue<ClientValue> collectionValue = client.getObjectFactory().newCollectionValue(null);
        for (Tasks.Task.IndicatorRow row: indicators) {
            final ClientComplexValue complexValue = client.getObjectFactory().newComplexValue(null);
            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_LINE,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(String.valueOf(line++))));
            complexValue.add(client.getObjectFactory().newPrimitiveProperty(TASK_INDICATOR_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.indicator)));
            addValue(complexValue, COMMON_GOAL, row.goal);
            addValue(complexValue, COMMON_MINIMUM, row.minimum);
            addValue(complexValue, COMMON_MAXIMUM, row.maximum);
            addValue(complexValue, TASK_INDICATOR_VALUE, row.value);
            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_COMMENT,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.comment)));
            complexValue.add(client.getObjectFactory().newPrimitiveProperty(TASK_INDICATOR_ACHIEVED,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(row.achived)));
            collectionValue.add(complexValue);
        }
        return collectionValue;
    }

    // возвращает ентити, заполненную по заданию
    private ClientEntity bindTaskEntity(Tasks.Task task) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        if (task!= null) {
            final List<ClientProperty> properties = entity.getProperties();
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_DATE,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(bindDate(task.date))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(task.status.id)));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_AUDITOR_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(bindGuid(task.auditor_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_TYPE_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(bindGuid(task.type_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_ORGANIZATION_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(bindGuid(task.organization_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_OBJECT_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(bindGuid(task.object_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_RESPONSIBLE_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(bindGuid(task.responsible_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(COMMON_COMMENT,
                    client.getObjectFactory().newPrimitiveValueBuilder().
                            buildString(task.comment)));
            //Аналитика объекта
            if (task.analytics != null) {
                properties.add(client.getObjectFactory().newCollectionProperty(TASK_ANALYTICS,
                        bindTaskAnalytics(task.analytics)));
            }
            //Показатели аудита
            if (task.indicators != null) {
                properties.add(client.getObjectFactory().newCollectionProperty(TASK_INDICATORS,
                        bindTaskIndicators(task.indicators)));
            }
        }
        return entity;
    }

    //Возвращает задачу, заполненную данными из ентити
    private Tasks.Task parseFullTask(ClientEntity entity, boolean checked, boolean expand) {
        Tasks.Task task = null;
        if (entity!=null) {
            task = new Tasks.Task();
            task.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
            task.date = parseDate(entity.getProperty(TASK_DATE).getPrimitiveValue().toString());
            task.number = entity.getProperty(TASK_NUMBER).getPrimitiveValue().toString();
            task.status = Tasks.Task.Status.toValue(entity.getProperty(TASK_STATUS).getPrimitiveValue().toString());
            task.auditor_key = getKey(entity.getProperty(TASK_AUDITOR_KEY).getValue().toString());
            task.type_key = getKey(entity.getProperty(TASK_TYPE_KEY).getValue().toString());
            task.object_key = getKey(entity.getProperty(TASK_OBJECT_KEY).getValue().toString());
            task.organization_key = getKey(entity.getProperty(TASK_ORGANIZATION_KEY).getValue().toString());
            task.responsible_key = getKey(entity.getProperty(TASK_RESPONSIBLE_KEY).getValue().toString());
            task.achieved = (boolean) entity.getProperty(TASK_ACHIEVED).getPrimitiveValue().toValue();
            task.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
            task.posted = (boolean) entity.getProperty(COMMON_POSTED).getPrimitiveValue().toValue();
            task.analytic_names = entity.getProperty(TASK_ANALYTIC_NAMES).getPrimitiveValue().toString();
            task.comment = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
            task.checked = checked;
            task.expand = expand;
            //Таблица с аналитикой
            for(Object e: entity.getProperty(TASK_ANALYTICS).getCollectionValue().asCollection())
                task.analytics.add(((ClientComplexValue)e).get(TASK_ANALYTIC_VALUE).getPrimitiveValue().toString());
            //Таблица показателей
            for(Object e: entity.getProperty(TASK_INDICATORS).getCollectionValue().asCollection()) {
                Tasks.Task.IndicatorRow row = task.new IndicatorRow();
                row.indicator = getKey(((ClientComplexValue)e).get(TASK_INDICATOR_KEY).getPrimitiveValue().toString());
                row.goal = getValue((ClientComplexValue)e, COMMON_GOAL);
                row.minimum = getValue((ClientComplexValue)e, COMMON_MINIMUM);
                row.maximum = getValue((ClientComplexValue)e, COMMON_MAXIMUM);
                row.value = getValue((ClientComplexValue)e, TASK_INDICATOR_VALUE);
                row.error = Float.valueOf(((ClientComplexValue)e).get(COMMON_ERROR).getPrimitiveValue().toString());
                row.comment = ((ClientComplexValue)e).get(COMMON_COMMENT).getPrimitiveValue().toString();
                row.achived = (boolean) ((ClientComplexValue)e).get(TASK_INDICATOR_ACHIEVED).getPrimitiveValue().toValue();
                task.indicators.add(row);
            }
            //Список медиафайлов из регистра
            getMediaFiles(task.mediaFiles, task.id);
        }
        return task;
    }

    //Возвращает задачу, частично заполненную данными из ентити
    private Tasks.Task parseShortTask(ClientEntity entity, boolean checked, boolean expand) {
        Tasks.Task task = null;
        if (entity!=null) {
            task = new Tasks.Task();
            task.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();
            task.date = parseDate(entity.getProperty(TASK_DATE).getPrimitiveValue().toString());
            task.status = Tasks.Task.Status.toValue(entity.getProperty(TASK_STATUS).getPrimitiveValue().toString());
            task.type_key = getKey(entity.getProperty(TASK_TYPE_KEY).getValue().toString());
            if (task.type_key!=null) {
                if (entity.getProperties().contains(entity.getProperty(TASK_TYPE)))
                    task.type_name = entity.getProperty(TASK_TYPE).getComplexValue().get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    task.type_name = getName(Set.TYPE, task.type_key);
            }
            else task.type_name = "";
            task.object_key = getKey(entity.getProperty(TASK_OBJECT_KEY).getValue().toString());
            if (task.object_key!= null) {
                if (entity.getProperties().contains(entity.getProperty(TASK_OBJECT)))
                    task.object_name = entity.getProperty(TASK_OBJECT).getComplexValue().get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    task.object_name = getName(Set.OBJECT, task.object_key);
            }
            else task.object_name = "";
            task.achieved = (boolean) entity.getProperty(TASK_ACHIEVED).getPrimitiveValue().toValue();
            task.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
            task.posted = (boolean) entity.getProperty(COMMON_POSTED).getPrimitiveValue().toValue();
            task.number = entity.getProperty(TASK_NUMBER).getPrimitiveValue().toString();
            task.analytic_names = entity.getProperty(TASK_ANALYTIC_NAMES).getPrimitiveValue().toString();
            task.comment = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
            task.checked = checked;
            task.expand = expand;
        }
        return task;
    }

    //Возвращает строку с guid или EMPTY_KEY, если аргумент пустой
    private String bindGuid(String key) {
        if (key == null || key.isEmpty()) return EMPTY_KEY;
        return key;
    }

    /**
     * Получить список заданий
     * @param auditor - guid аудитора
     * @param status - статус задания
     * @param like - подстрока для отбора
     * @return - возвращает список заданий для рециклервью и не только
     */
    @NonNull Tasks getTasks(String auditor, Tasks.Task.Status status, String like, int... skip)
            throws ODataErrorException {
        Tasks tasks = new Tasks();
        try {
            for (ClientEntity clientEntity: getAllTasks(auditor, status, like, skip))
                tasks.add(parseShortTask(clientEntity, false, false)); //Не отмечен и свернут
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting task list");
            }
        return tasks;
    }

    /**
     * Получить задание
     * @param key - giud задания
     * @return - задание
     */
    Tasks.Task getTask(String key) {
        Tasks.Task task = null;
        try {
            task = parseFullTask(getFullEntity(Set.TASK, key), false, false);
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getTask(guid'"+key+"') Error on requesting of task." + e.getMessage());
            }
        }
        return task;
    }

    /**
     * Создание нового задания
     * @param task объект с новым заданием
     */
    void createTask(Tasks.Task task) {
        //Запрос на создание задания
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .build();
        try {
            final ODataEntityCreateResponse<ClientEntity> response =
                    client.getCUDRequestFactory().getEntityCreateRequest(entityURI, bindTaskEntity(task)).execute();
            //guid нового задания получаем в ответе на запрос создания
            task.id = response.getBody().getProperty(COMMON_KEY).getPrimitiveValue().toString();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.createTask('" + task.toString() +
                        "') Error creating new task. " + e.getMessage());
            }
        }
//        MediaHttps.updateMediaFiles(task.id, task.mediaFiles);
    }

    /**
     * Изменение реквизитов задания - сохранение
     * @param task задание с новыми значениями реквизитов
     */
    void updateTask(Tasks.Task task) {
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .appendKeySegment("guid'"+task.id+"'")
                .build();
        try {
            client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, bindTaskEntity(task)).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.updateTask('"+task.toString()+
                        "') Error on update of task. "+e.getMessage());
            }
        }
//        updateMediaFiles(task.id, task.mediaFiles);
    }

    /**
     * Перемещение задания в другую папку - изменение статуса.
     * @param key - giud задания
     * @param status - новое значение статуса задания
     * @return - измененное задание с реквизитами, достаточными для отображения в списке.
     */
    Tasks.Task moveTask(String key, Tasks.Task.Status status) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(status.id)));
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.moveTask(guid'"+key+"', "+status+
                        ") Error on movening of task. "+e.getMessage());
            }
            return null;
        }
        return parseShortTask(response.getBody(),true, false); //Помечен в списке, свернут
    }

    /**
     * Копирование задания в другую папку - с изменением статуса
     * @param key - giud старого задания
     * @param status - нзначение статуса нового задания
     * @return - новое задание с реквизитами, достаточными для отображения в списке.
     */
    Tasks.Task copyTask(String key, Tasks.Task.Status status) {
        final ODataEntityCreateResponse<ClientEntity> response;
        try {
            //Исходное задание
            ClientEntity entity = getFullEntity(Set.TASK, key);
            //Изменяем entity
            //Удаляем свойство с идентификатором - 1С присвоит новый
            entity.getProperties().remove(entity.getProperty(COMMON_KEY));
            //Удаляем свойство с номером задания - 1С присвоит новый
            entity.getProperties().remove(entity.getProperty(TASK_NUMBER));
            //Устанавливаем текущую дату и время
            entity.getProperties().remove(entity.getProperty(TASK_DATE));
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_DATE,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindDate(new Date()))));
            //Меняем статус
            entity.getProperties().remove(entity.getProperty(TASK_STATUS));
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(status.id)));
            //Отметка на удаление должна быть снята
            entity.getProperties().remove(entity.getProperty(COMMON_DELETED));
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(false)));
            //Запрос на создание
            final URI entityURI = client.newURIBuilder(serviceRootOData)
                    .appendEntitySetSegment(Set.TASK.name)
                    .build();
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.copyTask(guid'"+key+"', "+status+
                        ") Error on copy of task. "+e.getMessage());
            }
            return null;
        }
        return parseShortTask(response.getBody(), false, false); //Новое задание пункт будет в списке свернуто и не отмечено
    }

    /**
     * Изменение пометки на удаление у задания
     * @param key - giud задания
     * @param delete - значение пометки на удаление
     * @return - измененное задание с реквизитами, достаточными для отображения в списке.
     */
    Tasks.Task deleteTask(String key, boolean delete) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //Устанавливаем значение пометки на удаление
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(delete)));
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.deleteTask(guid'"+key+"', "+delete+
                        ") Error on deleting of task. "+e.getMessage());
            }
            return new Tasks.Task();
        }
        return parseShortTask(response.getBody(), true, false); //Помечен в списке, свернут
    }

    //ВСЕ ДЛЯ ВИДОВ АУДИТА
    //возвращает вид аудита
    private AType parseType(ClientEntity entity) {
        AType type = null;
        if (entity != null) {
            type = new AType();
            type.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();;
            type.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
//            type.code = entity.getProperty(COMMON_CODE).getPrimitiveValue().toString();
//            type.pater = entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString();
//            type.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
//            type.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
//            type.predefined = (boolean) entity.getProperty(COMMON_PREDEFINED).getPrimitiveValue().toValue();
//            type.prenamed = entity.getProperty(COMMON_PRENAMED).getPrimitiveValue().toString();
//            type.criterion = parseATypeCriterion(entity.getProperty(TYPE_CRITERION).getPrimitiveValue().toString());
//            type.value = Float.valueOf(entity.getProperty(TYPE_VALUE).getPrimitiveValue().toString());
            type.fillActualValue = (boolean) entity.getProperty(TYPE_FILL_ACTUAL_VALUE).getPrimitiveValue().toValue();
            type.openWithIndicators = (boolean) entity.getProperty(TYPE_OPEN_WITH_INDICATORS).getPrimitiveValue().toValue();
            type.clearCopy = (boolean) entity.getProperty(TYPE_CLEAR_COPY).getPrimitiveValue().toValue();
            type.showSubject = (boolean) entity.getProperty(TYPE_SHOW_SUBJECT).getPrimitiveValue().toValue();
            type.selection = AType.Selections.toValue(entity.getProperty(TYPE_SELECTION).getPrimitiveValue().toString());
        }
        return type;
    }

    /**
     * Получить все типы объектов вида аудита
     * @param type - guid вида аудита
     * @return - список ентити
     */
    private List<ClientEntity> getAllObjectTypes(String type) {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.OBJECT_TYPES.name)
                .filter(OBJECT_TYPES_KEY+" eq guid'"+type+"'")
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    private String parseObjectType(ClientEntity entity) {
        String objectType = null;
        if (entity != null)
            objectType =  entity.getProperty(OBJECT_TYPES_TYPE).getPrimitiveValue().toString();
        return objectType;
    }

    /**
     * Подичить список типов объекта по виду аудита
     * @param type - guid вида аудита
     * @return - список типов объектов
     */
    private ArrayList<String> getObjectTypes(String type) {
        final ArrayList<String> list = new ArrayList<>();
        try {
            for(ClientEntity entity: getAllObjectTypes(type)) {
                list.add(parseObjectType(entity));
            }
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getObjectTypes() Error on requesting of object types." + e.getMessage());
            }
        }
        return list;
    }

    /**
     * Получить вид аудита
     * @param key guid вида аудита
     * @return - вид аудита
     */
    AType getAType(String key) {
        AType type = null;
        try {
            type = parseType(getFullEntity(Set.TYPE, key));
            type.objectTypes = getObjectTypes(key);
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getAType() Error on requesting of type." + e.getMessage());
            }
        }
        return type;
    }

    /**
     * Изменение вида аудита.
     * @param type - измененний вид аудита
     */
    void updateType(AType type) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //новые значения:
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.name)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_CRITERION,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.criterion.id)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_VALUE,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildSingle(type.value)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_SELECTION,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.selection.id)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_FILL_ACTUAL_VALUE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.fillActualValue)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_OPEN_WITH_INDICATORS,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.openWithIndicators)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_CLEAR_COPY,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.clearCopy)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_SHOW_SUBJECT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.showSubject)));
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TYPE.name)
                .appendKeySegment("guid'"+type.id+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.updateType('"+type.name+
                        "') Error on update of type. "+e.getMessage());
            }
        }
    }

    /**
     * Создание нового вида аудита
     * @param type - новый вида аудита
     */
    void createType(AType type) {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TYPE.name)
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //новые значения:
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.name)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_CRITERION,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.criterion.id)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_VALUE,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildSingle(type.value)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_SELECTION,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.selection.id)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_FILL_ACTUAL_VALUE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.fillActualValue)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_OPEN_WITH_INDICATORS,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.openWithIndicators)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_CLEAR_COPY,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.clearCopy)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_SHOW_SUBJECT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.showSubject)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_FOLDER,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.folder)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.pater)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PREDEFINED,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.predefined)));
//        ODataEntityCreateResponse<ClientEntity> response;
        try {
            /*response =*/
            client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.createType('"+type.name+
                        "') Error on create of type. "+e.getMessage());
            }
        }
    }

    //ВСЕ ДЛЯ ОБЪЕКТОВ АУДИТА
    /**
     * Рапаковать объект аудита
     * @param entity - ентити
     * @return - объект аудита
     */
    private AObject parseObject(ClientEntity entity) {
        AObject object = null;
        if (entity != null) {
            object = new AObject();
            object.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();;
            object.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            object.objectType = entity.getProperty(OBJECT_TYPE).getPrimitiveValue().toString();
        }
        return object;
    }

    /**
     * Получить объект аудита
     * @param key - guid объекта
     * @return - объект
     */
    AObject getAObject(String key) {
        AObject object = null;
        try {
            object = parseObject(getFullEntity(Set.OBJECT, key));
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getAObject() Error on requesting of object." + e.getMessage());
            }
        }
        return object;
    }

    //ВСЕ ДЛЯ СПРАВОЧНИКОВ
//    /**
//     * Получить родителя ребенка
//     * @param uriBuilder - построитель запросов
//     * @param children - guid ребенка
//     * @return - guid родителя
//     */
//    @NonNull
//    private String getPater(@NonNull final URIBuilder uriBuilder, @NonNull final String children) {
//        final URI entityURI = uriBuilder
//                .appendKeySegment("guid'"+children+"'")
//                .build();
//        final ODataRetrieveResponse<ClientEntity> response =
//                client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
//        return response.getBody().getProperty(COMMON_PARENT).getPrimitiveValue().toString();
//    }

//    /**
//     * Добавить в список guid всех предков ребенка
//     * @param uriBuilder - построитель запросов
//     * @param children - guid ребенка
//     * @param paters - список guid предков
//     */
//    private void addPaters(@NonNull final URIBuilder uriBuilder, @NonNull final String children, @NonNull final ArrayList<String> paters) {
//        final String pater = getPater(uriBuilder, children);
//        if (!(EMPTY_KEY.equals(pater) || paters.contains(pater))) {
//            paters.add(pater);
//            addPaters(uriBuilder, pater, paters);
//        }
//    }

//    /**
//     * Получить список всех предков по списку детей
//     * @param set - таблица
//     * @param childrens - список guid детей
//     * @return - список guid предков
//     */
//    @NonNull
//    ArrayList<String> getAllPaters(@NonNull final Set set, @NonNull final ArrayList<String> childrens) {
//        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
//        final ArrayList<String> paters = new ArrayList<>();
//        uriBuilder.select(COMMON_KEY, COMMON_DELETED, COMMON_PARENT)
//                .appendEntitySetSegment(set.id);
//        for(String children: childrens) {
//            addPaters(uriBuilder, children, paters);
//        }
//        return paters;
//    }

    //возвращает все пункты указанного родителя с отбором по наименованию
    private List<ClientEntity> getAllItems(Set table, String owner, String pater, String like,
                                           ArrayList<String> parentTypes, int... skip) {
        final StringBuilder filter = new StringBuilder();
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        final ArrayList<String> select = new ArrayList<>();
        select.add(COMMON_KEY);
        select.add(COMMON_DELETED);
        select.add(COMMON_NAME);
        select.add(COMMON_PARENT);
        select.add(COMMON_PREDEFINED);
        select.add(COMMON_PRENAMED);
        switch (table.hierarchy) {
            case FOLDER_HIERARCHY:
                select.add(COMMON_FOLDER);
                filter.append(COMMON_PARENT).append(" eq guid'").append(pater).append("'");
                if (!(like==null||like.isEmpty()))
                    filter.append("and(substringof('").append(like).append("',").append(COMMON_NAME).append(")or ").append(COMMON_FOLDER).append(")");
                break;
            case ELEMENT_HIERARCHY:
                select.add(COMMON_GROUP);
                filter.append(COMMON_PARENT).append(" eq guid'").append(pater).append("'");
                if (!(like==null||like.isEmpty()))
                    filter.append("and(substringof('").append(like).append("',").append(COMMON_NAME).append(")or ").append(COMMON_GROUP).append(")");
                break;
            case NOT_HIERARCHY: default:
                if (!(like==null||like.isEmpty()))
                    filter.append("substringof('").append(like).append("',").append(COMMON_NAME).append(")");
        }
        //Добавляем отбор по владельцу
        if (!(owner==null||owner.isEmpty())) {
            if (filter.length()>0) filter.append(" and ");
            filter.append(COMMON_OWNER).append(" eq guid'").append(owner).append("'");
        }
        //Добавляем отбор по типу родительского справочника
        if (!(parentTypes==null || parentTypes.isEmpty())) {
            select.add(OBJECT_TYPE);
            if (filter.length()>0) filter.append(" and ");
            filter.append("(");
            boolean next = false;
            for(String objectType: parentTypes) {
                if (next) filter.append(" or ");
                filter.append(OBJECT_TYPE).append(" eq '").append(objectType).append("'");
                next = true;
            }
            filter.append(")");
        }
        //Для порционной загрузки
        if (skip.length == 2) uriBuilder.skip(skip[0]).top(skip[1]);
        final URI entitySetURI = uriBuilder
                .select(select.toArray(new String[0])) //добавить массив из select
                .appendEntitySetSegment(table.name)
                .filter(filter.toString())
                .orderBy(COMMON_NAME+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    //возвращает пункт - элемент справочника из ентити
    private Items.Item parseItem(ClientEntity entity, int hierarchy, boolean checked, boolean expand) {
        Items.Item item = null;
        if (entity!=null) {
            item = new Items.Item();
            item.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();
            item.pater = entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString();
            item.folder = isFolder(entity, hierarchy);
            item.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            item.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
            item.predefined = (boolean) entity.getProperty(COMMON_PREDEFINED).getPrimitiveValue().toValue();
            item.prenamed = entity.getProperty(COMMON_PRENAMED).getPrimitiveValue().toString();
            item.checked = checked;
            item.expand = expand;
        }
        return item;
    }

    //возвращает пункт справочника для списка
    private ClientEntity getShortItem(Set table, String key) {
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        switch (table.hierarchy) {
            case FOLDER_HIERARCHY:
                uriBuilder.select(COMMON_FOLDER, COMMON_KEY, COMMON_DELETED, COMMON_NAME,
                        COMMON_PARENT, COMMON_PREDEFINED, COMMON_PRENAMED);
                break;
            case ELEMENT_HIERARCHY:
                uriBuilder.select(COMMON_GROUP, COMMON_KEY, COMMON_DELETED, COMMON_NAME,
                        COMMON_PARENT, COMMON_PREDEFINED, COMMON_PRENAMED);
                break;
            case NOT_HIERARCHY: default:
                uriBuilder.select(COMMON_KEY, COMMON_DELETED, COMMON_NAME,
                        COMMON_PARENT, COMMON_PREDEFINED, COMMON_PRENAMED);
        }
        final URI entityURI = uriBuilder
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataRetrieveResponse<ClientEntity> entity =
                client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
        return entity.getBody();
    }

    //Проверяет, является ли объект папкой?
    private boolean isFolder(ClientEntity entity, int hierarchy) {
        switch (hierarchy) {
            case FOLDER_HIERARCHY:
                return (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
            case ELEMENT_HIERARCHY:
                return (boolean) entity.getProperty(COMMON_GROUP).getPrimitiveValue().toValue();
        }
        return false;
    }

    /**
     * Извлечение списка элементов справочника
     * @param table - имя сущности
     * @param owner - guid владелеца / null
     * @param pater - guid нового родителя
     * @param like - строка для отбора по наименованию / null
     * @param parentTypes - родительские справочники для отбора / null
     * @param skip - массив из 2х элементов: 0 - сколько пропустить, 1 - сколько загрузить пунктов
     * @return - список пунтов
     */
    Items getItems(Set table, String owner, String pater, String like, ArrayList<String> parentTypes,
                   int... skip) {
        Items items = new Items();
        try {
            for (ClientEntity entity: getAllItems(table, owner, pater, like, parentTypes, skip))
                items.add(parseItem(entity, table.hierarchy,false,false));
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getItems() Error on requesting of items." + e.getMessage());
            }
        }
        return items;
    }

    /**
     * Извлечение записей из регистра СвязьПоТипамОбъектаАналитики по виду аудита и типу объекта
     * @param typeKey - giud вида аудита
     * @param objectType - тип объекта аудита
     * @return - список ентити
     */
    private List<ClientEntity> getAllAnalyticTypes (@NonNull String typeKey, @NonNull String objectType) {
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        //Отбор по наименованию аналитики
        final URI entitySetURI = uriBuilder
                .appendEntitySetSegment(Set.ANALYTIC_RELAT.name)
                .filter(RELAT_TYPE_KEY+" eq guid'"+typeKey+"' and "+
                        RELAT_OBJECT_TYPE+" eq '"+objectType+"'")
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Извлечение типа аудита из ентити
     * @param entity - ентити
     * @return - тип аудита / null
     */
    private String parseAnalyticTypes(ClientEntity entity) {
        String analyticType = null;
        if (entity!=null)
            analyticType = entity.getProperty(RELAT_ANALYTIC_TYPE).getPrimitiveValue().toString();
        return analyticType;
    }

    /**
     * Получение списка типов аналитик для отбора по виду аудита и типу объекта
     * @param typeKey - guid вида аудита
     * @param objectType - тип объекта аудита
     * @return - список типов аналитик
     */
    ArrayList<String> getAnalyticTypes(@NonNull String typeKey, @NonNull String objectType) {
        ArrayList<String> parentTypes = new ArrayList<>();
        try {
            for (ClientEntity entity: getAllAnalyticTypes(typeKey, objectType))
                parentTypes.add(parseAnalyticTypes(entity));
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getAnalyticTypes() Error on requesting of analytic types." +
                        e.getMessage());
            }
        }
        return parentTypes;
    }

    /**
     * Получение списка ентити соответствующих виду и объекту аудита
     * @param type - guid вида аудита
     * @param object - guid объекта аудита
     * @param like - строка отбора по наименованию аналитики
     * @param skip - массив из 2х элементов: 0 - сколько пропустить, 1 - сколько загрузить пунктов
     * @return список ентити
     */
    private List<ClientEntity> getAllAnalytics (@NonNull String type, @NonNull String object, String like, int... skip) {
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        final StringBuilder filter = new StringBuilder();
        //Отбор по виду и объекту аудита
        filter.append(CORR_TYPE_KEY).append(" eq guid'").append(type).append("' and ")
                .append(CORR_OBJECT_KEY).append(" eq guid'").append(object).append("'");
        //Отбор по наименованию аналитики
        if (!(like==null||like.isEmpty()))
            filter.append(" and substringof('").append(like).append("',")
                    .append(CORR_ANALYTIC+"/"+COMMON_NAME).append(")");
        //Для порционной загрузки
        if (skip.length == 2) uriBuilder.skip(skip[0]).top(skip[1]);
        final URI entitySetURI = uriBuilder
                .appendEntitySetSegment(Set.ANALYTIC_CORR.name)
                .expand(CORR_ANALYTIC)
                .select(CORR_TYPE_KEY, CORR_OBJECT_KEY, CORR_ANALYTIC_KEY,
                        CORR_ANALYTIC+"/"+COMMON_NAME,
                        CORR_ANALYTIC+"/"+COMMON_DELETED,
                        CORR_ANALYTIC+"/"+COMMON_PREDEFINED,
                        CORR_ANALYTIC+"/"+COMMON_PRENAMED)
                .filter(filter.toString())
                .orderBy(CORR_ANALYTIC+"/"+COMMON_NAME+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Извлекает пункт из ентити с аналитикой
     * @param entity - ентити
     * @param checked - признак отметки пункта
     * @param expand - признак раскрытия пункта
     * @return - пункт с аналитикой
     */
    private Items.Item parseAnalytic(ClientEntity entity, boolean checked, boolean expand) {
        Items.Item item = null;
        if (entity!=null) {
            item = new Items.Item();
            item.id = entity.getProperty(CORR_ANALYTIC_KEY).getPrimitiveValue().toString();
            if (item.id!=null) {
                final ClientProperty property = entity.getProperty(CORR_ANALYTIC);
                if (entity.getProperties().contains(property)) {
                    final ClientComplexValue value = property.getComplexValue();
                    item.name = value.get(COMMON_NAME).getPrimitiveValue().toString();
                    item.deleted = (boolean) value.get(COMMON_DELETED).getPrimitiveValue().toValue();
                    item.predefined = (boolean) value.get(COMMON_PREDEFINED).getPrimitiveValue().toValue();
                    item.prenamed = value.get(COMMON_PRENAMED).getPrimitiveValue().toString();
                    item.checked = checked;
                    item.expand = expand;
                }
            }
        }
        return item;
    }

    /**
     * Извлечение списка аналитик соответствующих виду и объекту аудита
     * @param type - guid вида аудита
     * @param object - guid объекта аудита
     * @param like - строка отбора по наименованию аналитики
     * @param skip - массив из 2х элементов: 0 - сколько пропустить, 1 - сколько загрузить пунктов
     * @return - список пунктов
     */
    Items getAnalytics(String type, String object, String like, int... skip) {
        Items items = new Items();
        try {
            for (ClientEntity entity: getAllAnalytics(type, object, like, skip))
                items.add(parseAnalytic(entity, false, false));
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getAnalytics() Error on requesting of items." + e.getMessage());
            }
        }
        return items;
    }

    /**
     * Извлечение элемента справочника
     * @param table - имя сущности
     * @param key - guid пункта
     * @return - пункт справочника
     */
    Items.Item getItem(Set table, String key) {
        Items.Item item = new Items.Item();
        try {
            item = parseItem(getShortItem(table, key), table.hierarchy, false, false);
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("Error on requesting of item." + e.getMessage());
            }
        }
        return item;
    }

    /**
     * Копирование пункта справочника в эту же или другую папку
     * @param table - имя сущности
     * @param key - guid пункта
     * @param pater - guid нового родителя
     * @return - копию пункта
     */
    Items.Item copyItem(Set table, String key, String pater) {
        final ODataEntityCreateResponse<ClientEntity> response;
        try {
            final ClientEntity entity = getFullEntity(table, key);
            //Удаляем свойство с идентификатором - 1С присвоит новый
            entity.getProperties().remove(entity.getProperty(COMMON_KEY));
            //Меняем родителя
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(pater)));
            //Отметка на удаление должна быть снята
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(false)));
            //Предопределенность должна быть снята
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PREDEFINED,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(false)));
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PRENAMED,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString("")));
            final URI entityURI = client.newURIBuilder(serviceRootOData)
                    .appendEntitySetSegment(table.name)
                    .build();
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.copyItem('"+table+"', guid'"+key+"', guid'"+pater+
                        "') Error on copy of item. "+e.getMessage());
            }
            return new Items.Item();
        }
        return parseItem(response.getBody(), table.hierarchy,false, false); //Новый пункт будет в списке свернут и не отмечен
    }

    /**
     * Перемещение пункта справочника в эту же или другую папку - изменение родителя
     * Контроль зацикливания ссылок осуществляется в ReferenceChoice!!!
     * @param table - имя сущности
     * @param key - guid пункта
     * @param pater - guid нового родителя
     * @return - измененный пукнт / null, если была попытка переместить папку, когда not_folders == true
     */
    Items.Item moveItem(Set table, String key, String pater) {
        if (table.hierarchy==NOT_HIERARCHY) { //Если набор не является иерархией!!!
            throw new RuntimeException("AuditOData.moveItem('"+table+"', guid'"+key+"', guid'"+pater+"') Entity set '"+table+"' is not the hierarchy.");
        }
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //Изменяем родителя
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(pater)));
        ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.moveItem('"+table+"', guid'"+key+"', guid'"+pater+
                        "') Error on move of item. "+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), table.hierarchy, true, false); //Помечен в списке, свернут
    }

    /**
     * Изменение отметки на удаление у пункта справочнка
     * @param table - имя сущности
     * @param key - guid пункта справочника
     * @param delete - значение пометки на удаление
     * @return - измененный пункт
     */
    Items.Item deleteItem(Set table, String key, boolean delete) {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //Устанавливаем пометку на удаление
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(delete)));
        ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.deleteItem('"+table+"', guid'"+key+"', "+delete+
                        ") Error on delete of item ."+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), table.hierarchy, true, false); //Помечен в списке, свернут
    }

    /**
     * Изменение пункта справочника (только наименование). Подходит для изменения наименования группы
     * @param table - имя сущности
     * @param item - объект пункт справочника, с новым наименованием
     * @return - измененный пункт
     */
    Items.Item updateItem(Set table, Items.Item item) {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+item.id+"'")
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //новое наименование
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(item.name)));
        ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.updateItem('"+table+"', '"+item.toString()+
                        "') Error on update of item. "+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), table.hierarchy, true, false); //Помечен в списке, свернут
    }

    /**
     * Создание пункта справочника (только наименование и родитель)
     * Подходит для создания группы
     * @param table - имя сущности
     * @param pater - guid нового родителя
     * @param name - наименование
     * @param isFolder - признак группы
     * @return - созданный пункт
     */
    Items.Item createItem(Set table, String pater, String name, boolean isFolder) {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(name)));
        //Устанавливаем признак группы
        switch (table.hierarchy) {
            case FOLDER_HIERARCHY:
                entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_FOLDER,
                        client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(isFolder)));
                break;
            case ELEMENT_HIERARCHY:
                entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_GROUP,
                        client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(isFolder)));
                break;
            case NOT_HIERARCHY: default:
                if (isFolder)
                    throw new RuntimeException("AuditOData.createItem('"+table+"', '"+pater+"', '"+name+
                            "', "+isFolder+"') Error on create of item group. The '"+table+"' set do not have hierarchy.");
        }
        //Родитель
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(pater)));
        //Новое наименование
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(name)));
        ODataEntityCreateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.createItem('"+table+"', '"+pater+"', '"+name+
                        "') Error on create of item group ."+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), table.hierarchy, false, false); //Не помечен в списке, свернут
    }

    //ВСЕ ДЛЯ НОРМАТИВОВ ПОКАЗАТЕЛЕЙ АУДИТА
    //Возвращает список entity с нормативными значениями
    private List<ClientEntity> getAllIndicatorStandard(String type, String object) {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.INDICATOR_STANDARD.name)
                .filter(INDICATOR_STANDARD_TYPE+" eq guid'"+type+"' and "+INDICATOR_STANDARD_OBJECT+" eq guid'"+object+"'")
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    //Возвращает нормативы показателя
    private Tasks.Task.IndicatorRow parseIndicatorStandard(ClientEntity entity) {
        final Tasks.Task.IndicatorRow row = new Tasks.Task(). new IndicatorRow();
        if (entity != null) {
            row.indicator = getKey(entity.getProperty(INDICATOR_STANDARD_INDICATOR).getPrimitiveValue().toString());
            row.goal = getValue(entity, COMMON_GOAL);
            row.minimum = getValue(entity, COMMON_MINIMUM);
            row.maximum = getValue(entity, COMMON_MAXIMUM);
            row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
        }
        return row;
    }

    /**
     * Получить нормативы показателей вида аудита по объекту
     * @param type - guid вида аудита
     * @param object - guid объекта аудита
     * @return - список нормативов показателей
     */
    ArrayList<Tasks.Task.IndicatorRow> getStandardIndicatorRows(String type, String object) {
        final ArrayList<Tasks.Task.IndicatorRow> indicators = new ArrayList<>();
        try {
            for (ClientEntity entity : getAllIndicatorStandard(type != null? type: EMPTY_KEY,
                    object != null? object: EMPTY_KEY)) {
                indicators.add(parseIndicatorStandard(entity));
            }
        }
        catch(ODataRuntimeException e) {
                if (sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.getTaskIndicators() Error on requesting of indicator standard. " + e.getMessage());
                }
            }
        return indicators;
    }

    //ВСЕ ДЛЯ ТАБЛИЧНОЙ ЧАСТИ ЗАДАНИЯ "ПОКАЗАТЕЛИ"
//    //Возвращает список entity показателей задания
//    private List<ClientEntity> getAllTaskIndicators(String task) {
//        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
//                .appendEntitySetSegment(Set.TASK_INDICATOR.id)
//                .filter(COMMON_KEY+" eq guid'"+task+"'")
//                .orderBy(COMMON_LINE+ORDER_ASC)
//                .build();
//        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
//        return entitySet.getBody().getEntities();
//    }

//    //Возвращает строку таблицы с показателем
//    private Tasks.Task.IndicatorRow parseTaskIndicator(ClientEntity entity) {
//        final Tasks.Task.IndicatorRow row = new Tasks.Task(). new IndicatorRow();
//        if (entity != null) {
//            row.indicator = getKey(entity.getProperty(TASK_INDICATOR_KEY).getPrimitiveValue().toString());
//            row.goal = getValue(entity, COMMON_GOAL);
//            row.minimum = getValue(entity, COMMON_MINIMUM);
//            row.maximum = getValue(entity, COMMON_MAXIMUM);
//            row.value = getValue(entity, TASK_INDICATOR_VALUE);
//            row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
//            row.comment = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
//            row.achived = (boolean) entity.getProperty(TASK_INDICATOR_ACHIEVED).getPrimitiveValue().toValue();
//        }
//        return row;
//    }

//    /**
//     * Подучить показатели задания
//     * @param task guid задания
//     * @return - список строк показателей
//     */
//    List<Tasks.Task.IndicatorRow> getTaskIndicators(String task) {
//        final List<Tasks.Task.IndicatorRow> list = new ArrayList<>();
//        try {
//            for (ClientEntity clientEntity: getAllTaskIndicators(task))
//                list.add(parseTaskIndicator(clientEntity));
//        }
//        catch(ODataRuntimeException e) {
//            if (sayErrorMessage(e)) {
//                e.printStackTrace();
//                throw new RuntimeException("AuditOData.getTaskIndicators() Error on requesting of task indicators. " + e.getMessage());
//            }
//        }
//        return list;
//    }

//    /**
//     * Получить показатели с нормативными значениями
//     * @param type - guid вида аудита
//     * @param object - [guid объекта аудита]
//     * @return - спосок показателей аудита для табличной части задания
//     */
//    ArrayList<Tasks.Task.IndicatorRow> getStandardIndicators(String type, String object) {
//        final ArrayList<Tasks.Task.IndicatorRow> rows = new ArrayList<Tasks.Task.IndicatorRow>();
//        //заполняем показателями из справочника
//        if (noEmptyKey(type)) {
//            for (ClientEntity entity: getAllIndicators(type))
//                rows.add(parseIndicatorRow(entity));
//            //уточняем значения из регистра нормативов
//            if (noEmptyKey(object)) {
//                final Map<String, Tasks.Task.IndicatorRow> map = getIndicatorStandard(type, object);
//                for (Tasks.Task.IndicatorRow row: rows)
//                    if (map.containsKey(row.indicator)) {
//                        final Tasks.Task.IndicatorRow standard = map.get(row.indicator);
//                        row.goal = standard.goal;
//                        row.minimum = standard.minimum;
//                        row.maximum = standard.maximum;
//                        row.error = standard.error;
//                    }
//            }
//        }
//        return rows;
//    }

    //ВСЕ ДЛЯ СПРАВОЧНИКА ПОКАЗАТЕЛЕЙ АУДИТА
//    //Возвращает показатель аудита по entity
//    private Indicators.Indicator parseFullIndicator(ClientEntity entity) {
//        final Indicators.Indicator indicator = new Indicators(). new Indicator();;
//        if (entity != null) {
//            try {
//                indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
//                indicator.code = entity.getProperty(COMMON_CODE).getPrimitiveValue().toString();
//                indicator.id = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
//                indicator.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
//                indicator.owner = getKey(entity.getProperty(COMMON_OWNER).getPrimitiveValue().toString());
//                indicator.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
//                indicator.desc = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
//                indicator.type = Indicators.Types.toValue(entity.getProperty(INDICATOR_TYPE).getPrimitiveValue().toString());
//                indicator.subject = getKey(entity.getProperty(INDICATOR_SUBJECT).getPrimitiveValue().toString());
//                indicator.criterion = Indicators.Criteria.toValue(entity.getProperty(INDICATOR_CRITERION).getPrimitiveValue().toString());
//                indicator.unit = getName(Set.UNIT, getKey(entity.getProperty(INDICATOR_UNIT).getPrimitiveValue().toString()));
//                indicator.goal = getValue(entity, COMMON_GOAL);
//                indicator.minimum = getValue(entity, COMMON_MINIMUM);
//                indicator.maximum = getValue(entity, COMMON_MAXIMUM);
//                indicator.error = getFloat(entity, COMMON_ERROR);
//                indicator.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
//                indicator.predefined = (boolean) entity.getProperty(COMMON_PREDEFINED).getPrimitiveValue().toValue();
//                if (indicator.predefined)
//                    indicator.prenamed = entity.getProperty(COMMON_PRENAMED).getPrimitiveValue().toString();
//            }
//            catch (ODataRuntimeException e) {
//                if (sayErrorMessage(e)) {
//                    e.printStackTrace();
//                    throw new RuntimeException("AuditOData.parseFullIndicator() Error on parsing of indicator ." + e.getMessage());
//                }
//            }
//        }
//        return indicator;
//    }

    //Возвращает строку таблицы показателей
    private Tasks.Task.IndicatorRow parseIndicatorRow(ClientEntity entity) {
        final Tasks.Task.IndicatorRow row = new Tasks.Task().new IndicatorRow();
        if (entity != null) {
            try {
                row.indicator = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
                row.goal = getValue(entity, COMMON_GOAL);
                row.minimum = getValue(entity, COMMON_MINIMUM);
                row.maximum = getValue(entity, COMMON_MAXIMUM);
                row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
            }
            catch (ODataRuntimeException e) {
                if (sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.parseIndicatorRow() Error on parsing of indicator ." + e.getMessage());
                }
            }
        }
        return row;
    }

//    //Возвращает показатель аудита по entity
//    private Indicators.Indicator parseShortIndicator(ClientEntity entity) {
//        final Indicators.Indicator indicator = new Indicators(). new Indicator();
//        if (entity != null) {
//            indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
//            indicator.id = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
//            indicator.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
//            indicator.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
//            if (!indicator.folder) {
//                indicator.desc = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
//                indicator.type = Indicators.Types.toValue(entity.getProperty(INDICATOR_TYPE).getPrimitiveValue().toString());
////                    indicator.subject = getKey(entity.getProperty(INDICATOR_SUBJECT).getPrimitiveValue().toString());
//                indicator.criterion = Indicators.Criteria.toValue(entity.getProperty(INDICATOR_CRITERION).getPrimitiveValue().toString());
//                indicator.unit = getName(Set.UNIT, getKey(entity.getProperty(INDICATOR_UNIT).getPrimitiveValue().toString()));
//            }
//        }
//        return indicator;
//    }

    /**
     * Распаковать показатель аудита
     * @param entity - ентити
     * @return - показатель аудита
     */
    private IndList.Ind parseInd(ClientEntity entity) {
        final IndList.Ind indicator = new IndList.Ind();
        if (entity != null) {
            indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
            indicator.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            indicator.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
            indicator.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
            if (!indicator.folder) {
                indicator.desc = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
                indicator.type = Indicators.Types.toValue(entity.getProperty(INDICATOR_TYPE).getPrimitiveValue().toString());
                indicator.subject = getKey(entity.getProperty(INDICATOR_SUBJECT).getPrimitiveValue().toString());
                indicator.not_involved = (boolean) entity.getProperty(INDICATOR_NOT_INVOLVED).getPrimitiveValue().toValue();
                indicator.criterion = Indicators.Criteria.toValue(entity.getProperty(INDICATOR_CRITERION).getPrimitiveValue().toString());
                indicator.unit = getName(Set.UNIT, getKey(entity.getProperty(INDICATOR_UNIT).getPrimitiveValue().toString()));
            }
            else {
                //Заполним, чтобы не пустовало и не возникала ошибка при упаковке/распаковке
                indicator.type = Indicators.Types.IS_BOOLEAN;
                indicator.criterion = Indicators.Criteria.EQUALLY;
            }
        }
        return indicator;
    }

//    /**
//     * Получить показатель аудита
//     * @param key - giud показателя
//     * @return - показатель аудита
//     */
//    Indicators.Indicator getIndicator(String key) {
//        try {
//            return parseFullIndicator(getFullEntity(Set.INDICATOR, key));
//        }
//        catch (ODataRuntimeException e) {
//            if (sayErrorMessage(e)) {
//                e.printStackTrace();
//                throw new RuntimeException("AuditOData.getIndicator() Error on requesting of indicator. " + e.getMessage());
//            }
//        }
//        return null;
//    }

    //Возвращает список entity с показателями аудита
    private List<ClientEntity> getAllIndicators(@NonNull String type) {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.INDICATOR.name)
                .filter(COMMON_OWNER+" eq guid'"+type+"' and "+
                        COMMON_DELETED+" eq false and "+
                        COMMON_FOLDER+" eq false")
                .select(COMMON_OWNER, COMMON_DELETED, COMMON_FOLDER,
                        COMMON_KEY, INDICATOR_TYPE,
                        COMMON_GOAL, COMMON_GOAL+_TYPE,
                        COMMON_MINIMUM, COMMON_MINIMUM+_TYPE,
                        COMMON_MAXIMUM, COMMON_MAXIMUM+_TYPE,
                        COMMON_ERROR, COMMON_ORDER)
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Получить все показатели по виду аудита
     * @param type - guid вида аудита
     * @param onlyElements - признак, только элементы
     * @param withoutDeleted - без удаленных элементов
     * @return - список ентити
     */
    private List<ClientEntity> getAllIndicators(@NonNull String type, boolean onlyElements, boolean withoutDeleted ) {
        final StringBuilder filter = new StringBuilder();
        filter.append(COMMON_OWNER).append(" eq guid'").append(type).append("'");
        if (onlyElements) filter.append(" and ").append(COMMON_FOLDER).append(" eq false");
        if (withoutDeleted) filter.append(" and ").append(COMMON_DELETED).append(" eq false");
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.INDICATOR.name)
                .filter(filter.toString())
//                .select(COMMON_OWNER, COMMON_FOLDER,
//                        COMMON_KEY, COMMON_NAME,
//                        COMMON_PARENT, COMMON_COMMENT,
//                        INDICATOR_TYPE, INDICATOR_CRITERION,
//                        INDICATOR_UNIT, INDICATOR_SUBJECT,
//                        COMMON_GOAL, COMMON_GOAL+_TYPE,
//                        COMMON_MAXIMUM, COMMON_MAXIMUM+_TYPE,
//                        COMMON_MINIMUM, COMMON_MINIMUM+_TYPE,
//                        COMMON_ERROR, COMMON_ERROR+_TYPE,
//                        COMMON_ORDER)
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

//    //Возвращает список entity с показателями аудита
//    private List<ClientEntity> getAllIndicators(String type, String pater, String subject) {
//        final StringBuilder filter = new StringBuilder();
//        filter.append(COMMON_OWNER).append(" eq guid'").append(type).append("'");
//        if (pater != null)
//            filter.append(" and ").append(COMMON_PARENT).append(" eq guid'").append(pater).append("'");
//        if (subject != null) {
//            filter.append(" and ").append(INDICATOR_SUBJECT).append(" eq guid'").append(subject).append("'");
//            filter.append(" and ").append(COMMON_FOLDER).append(" eq false");
//        }
//        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
//                .appendEntitySetSegment(Set.INDICATOR.id)
//                .filter(filter.toString())
//                .orderBy(COMMON_ORDER+ORDER_ASC)
//                .build();
//        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
//        return entitySet.getBody().getEntities();
//    }

    /**
     * Получить показатели вида аудита
     * @param type - guid вида аудита
     * @return - список показателей аудита
     */
    ArrayList<Tasks.Task.IndicatorRow> getIndicatorRows(String type) {
        final ArrayList<Tasks.Task.IndicatorRow> indicators = new ArrayList<>();
        try {
            for (ClientEntity entity: getAllIndicators(type != null? type: EMPTY_KEY,
                    true, true))
                indicators.add(parseIndicatorRow(entity));
        }
        catch(ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getIndicatorRows(guid'"+type+
                        "') Error on requesting of indicators. "+e.getMessage());
            }
        }
        return indicators;
    }

    /**
     * Добавить все показатели в дерево показателей по виду аудита
     * @param indList - дерево показателей
     * @param type - guid вида аудита
     * @param onlyElements - признак загрузки только элементов
     */
    void addInd(@NonNull IndList indList, @NonNull String type, boolean onlyElements) {
        try {
            for (ClientEntity clientEntity: getAllIndicators(type, onlyElements, false))
                indList.add(parseInd(clientEntity));
        }
        catch(ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.addInd(..., guid'"+type+"', "+onlyElements+
                        ") Error on requesting of indicators. "+e.getMessage());
            }
        }
    }

    //ВСЕ ДЛЯ ПРЕДМЕТОВ
//    /**
//     * Получить все предметы по виду аудита и родителю
//     * @param type - guid вида аудита
//     * @param pater- guid родителя предмета
//     * @return - список ентити с предметами
//     */
//    private List<ClientEntity> getAllSubjects(@NonNull String type, @NonNull String pater) {
//        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
//                .appendEntitySetSegment(Set.SUBJECT.id)
//                .filter(COMMON_OWNER+" eq guid'"+type+"' and "+
//                        COMMON_PARENT+" eq guid'"+pater+"'")
//                .orderBy(COMMON_ORDER+ORDER_ASC)
//                .build();
//        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
//        return entitySet.getBody().getEntities();
//    }

    /**
     * Получить все предметы по виду аудита
     * @param type - guid вида аудита
     * @return - список ентити с предметами
     */
    private List<ClientEntity> getAllSubjects(@NonNull String type) {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.SUBJECT.name)
                .filter(COMMON_OWNER+" eq guid'"+type+"'")
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Распковать предмет аудита
     * @param entity - ответ на запрос
     * @return - предмет аудита
     */
    private IndList.Ind parseSubject(ClientEntity entity) {
        final IndList.Ind subject = new IndList.Ind();
        if (entity != null) {
            subject.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
            subject.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            subject.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
            subject.folder = true; //Все будет папками
            //Заполним, чтобы не пустовало и не возникала ошибка при упаковке/распаковке
            subject.type = Indicators.Types.IS_BOOLEAN;
            subject.criterion = Indicators.Criteria.EQUALLY;
        }
        return subject;
    }

    /**
     * Добавить предметы в виде папок в дерево показателей
     * @param indList - дерево показателей
     * @param type - guid вида аудита
     */
    void addSubjects(@NonNull IndList indList, @NonNull String type) {
        try {
            for (ClientEntity clientEntity: getAllSubjects(type))
                indList.add(parseSubject(clientEntity));
        }
        catch(ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.addSubjects( ..., guid'"+type+"') Error on requesting of subjects. "+e.getMessage());
            }
        }
    }

    //ВСЕ ДЛЯ СПИСКА МЕДИАФАЙЛОВ

    /**
     * Получить список ентити с медиафайлами задания
     * @param task_key - guid задания
     * @return - список ентити
     */
    private List<ClientEntity> getAllMediaFiles(@NonNull String task_key) {
        String filter = MEDIA_TASK_KEY+" eq guid'"+task_key+"'";
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        uriBuilder.appendEntitySetSegment(Set.MEDIA_FILES.name)
                .select(MEDIA_TASK_KEY, MEDIA_FILE_NAME, MEDIA_FILE_TYPE,
                        MEDIA_INDICATOR_KEY,
                        MEDIA_AUTHOR_KEY,
                        MEDIA_FILE_DATE, MEDIA_COMMENT,
                        MEDIA_INDICATOR+"/"+COMMON_NAME, MEDIA_AUTHOR +"/"+COMMON_NAME)
                .expand(MEDIA_INDICATOR, MEDIA_AUTHOR)
                .filter(filter)
                .orderBy(MEDIA_FILE_DATE +ORDER_DESC);
        final URI entitySetURI = uriBuilder.build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Создание медиафайла по ентити
     * @param entity - ентити
     * @return - медиафайл
     */
    private MediaFiles.MediaFile parseMediaFile(ClientEntity entity) {
        MediaFiles.MediaFile mediaFile = null;
        if (entity!=null) {
            mediaFile = new MediaFiles.MediaFile();
            mediaFile.name = entity.getProperty(MEDIA_FILE_NAME).getPrimitiveValue().toString();
            mediaFile.type = MediaFiles.MediaType.toValue(entity.getProperty(MEDIA_FILE_TYPE).getPrimitiveValue().toString());
            mediaFile.indicator_key = getKey(entity.getProperty(MEDIA_INDICATOR_KEY).getPrimitiveValue().toString());
            if (mediaFile.indicator_key!=null) {
                if (entity.getProperties().contains(entity.getProperty(MEDIA_INDICATOR)))
                    mediaFile.indicator_name = entity.getProperty(MEDIA_INDICATOR).getComplexValue().get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    mediaFile.indicator_name = getName(Set.INDICATOR, mediaFile.indicator_key);
            }
            else mediaFile.indicator_name = "";
            mediaFile.author_key = getKey(entity.getProperty(MEDIA_AUTHOR_KEY).getPrimitiveValue().toString());
            if (mediaFile.author_key !=null) {
                if (entity.getProperties().contains(entity.getProperty(MEDIA_AUTHOR)))
                    mediaFile.author_name = entity.getProperty(MEDIA_AUTHOR).getComplexValue().get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    mediaFile.author_name = getName(Set.AUDITOR, mediaFile.author_key);
            }
            else mediaFile.author_name = "";
            mediaFile.date = parseDate(entity.getProperty(MEDIA_FILE_DATE).getPrimitiveValue().toString());
            mediaFile.comment = entity.getProperty(MEDIA_COMMENT).getPrimitiveValue().toString();
            mediaFile.loaded = false;
            mediaFile.act = MediaFiles.Act.NoAction;
        }
        return mediaFile;
    }

    /**
     * Пополнить список медиафайлов задания
     * @param mediaFiles - список медиафайлов
     * @param task_key - guid задания
     */
    private void getMediaFiles(@NonNull MediaFiles mediaFiles, @NonNull String task_key) {
        try {
            for (ClientEntity clientEntity: getAllMediaFiles(task_key))
                mediaFiles.add(parseMediaFile(clientEntity));
        }
        catch (ODataRuntimeException e) {
            if (sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("Error on requesting of media files." + e.getMessage());
            }
        }
    }

}
//Фома2018