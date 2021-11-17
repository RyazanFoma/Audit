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
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
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
import static com.bit.eduardf.audit.ParcelableUser.USER_HASH;
import static com.bit.eduardf.audit.ParcelableUser.USER_RESPONSIBLE;
import static com.bit.eduardf.audit.ParcelableUser.USER_TYPE;
import static com.bit.eduardf.audit.ParcelableUser.USER_VERSION;

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
    private static final String USERS_VERSION = "DataVersion";

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
    private static final String COMMON_VERSION = "DataVersion";
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

    private static final String CONNECTION_ERROR = "Connection error";
    
    //Реквизиты класса
    private final ODataClient client; //Клиент OData
    private final String serviceRootOData; //Путь к oData

    //Конструктор
    public AuditOData(Context context) {
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

    /**
     * Get record from table of database 1C by guid
     * @param table - table name of 1C date base
     * @param key - record guid
     * @return - client entity
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of request failure
     */
    private ClientEntity getFullEntity(Set table, String key)
            throws HttpClientException, ODataRuntimeException {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataRetrieveResponse<ClientEntity> entity = client.getRetrieveRequestFactory().
                getEntityRequest(entityURI).execute();
        return entity.getBody();
    }

//    //Выводит сообщение об ошибке в диалоговом окне
//    private boolean sayErrorMessage(@NonNull Exception e) {
//        return true;
//    }
//
    /**
     * Get the description of the item from the database table 1C
     * @param table - table name
     * @param key - item guid or null
     * @return - item description or "" if key is null or empty
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of request failure
     */
    String getName(Set table, String key) throws HttpClientException, ODataRuntimeException {
        if (!(key == null || key.isEmpty() || EMPTY_KEY.equals(key))) {
            final URI entityURI = client.newURIBuilder(serviceRootOData)
                    .appendEntitySetSegment(table.name)
                    .appendKeySegment("guid'"+key+"'")
                    .select(COMMON_NAME)
                    .build();
            final ODataRetrieveResponse<ClientEntity> entity =
                    client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
            return entity.getBody().getProperty(COMMON_NAME).getValue().toString();
        }
        return "";
    }

    /**
     * Get key from guid string or null if guid is empty
     * @param key - guid string
     * @return guid or null
     */
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

    //EVERYTHING FOR USER LIST
    /**
     * Get a list of client entities with users from 1C database
     * @return - list with users
     * @throws HttpClientException - in case of connection error
     * @throws ODataRuntimeException - in case of request error
     */
    private List<ClientEntity> getAllUsers() throws HttpClientException, ODataRuntimeException {
        final URI userEntitySetURI = client
                .newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.AUDITOR.name)
                .filter(FILTER_AUDITORS)
                .select(COMMON_KEY, COMMON_NAME, COMMON_VERSION, USERS_PASSWORD,
                        USERS_TYPE, USERS_OBJECT, USERS_ORGANIZATION, USERS_RESPONSIBLE)
                .build();
//        final ODataRetrieveResponse<ClientEntitySet>
//                users = client.getRetrieveRequestFactory()
//                .getEntitySetRequest(userEntitySetURI).execute();
        final ODataEntitySetRequest<ClientEntitySet> setRequest = client.getRetrieveRequestFactory()
                .getEntitySetRequest(userEntitySetURI);
        final ODataRetrieveResponse<ClientEntitySet> users = setRequest.execute();
        return users.getBody().getEntities();
    }

    /**
     * Get a list of users for the spinner
     * @return - list of users
     * @throws ODataErrorException - in case of request failure
     */
    List<Map<String, Object>> getUsers() throws ODataErrorException {
        List<Map<String, Object>> usersMap = new ArrayList<>();
        try {
            for (ClientEntity clientEntity : getAllUsers()) {
                Map<String, Object> user = new HashMap<>();
                user.put(USER_ID, clientEntity.getProperty(COMMON_KEY).getValue());
                user.put(USER_NAME, clientEntity.getProperty(COMMON_NAME).getValue());
                user.put(USER_HASH, clientEntity.getProperty(USERS_PASSWORD).getValue());
                user.put(USER_TYPE, clientEntity.getProperty(USERS_TYPE).getValue());
                user.put(USER_OBJECT, clientEntity.getProperty(USERS_OBJECT).getValue());
                user.put(USER_ORGANIZATION, clientEntity.getProperty(USERS_ORGANIZATION).getValue());
                user.put(USER_RESPONSIBLE, clientEntity.getProperty(USERS_RESPONSIBLE).getValue());
                user.put(USER_VERSION, clientEntity.getProperty(COMMON_VERSION).getValue());
                usersMap.add(user);
            }
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting user list");
        }
        return usersMap;
    }

    /**
     * Build a client entity with user parameters
     * @param type - audit type giud
     * @param organization - organization giud
     * @param object - object giud
     * @param responsible - responsible giud
     * @param version - DataVersion
     * @return - client entity with user parameters
     */
    private ClientEntity buildUserEntity(@NotNull String type, @NotNull String organization,
                                         @NotNull String object, @NotNull String responsible,
                                         @NotNull String version) {
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
        properties.add(client.getObjectFactory().newPrimitiveProperty(USERS_VERSION,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(version))));
        return entity;
    }

    /**
     * Save user parameters in 1C database
     * @param auditor - user giud
     * @param type - audit type giud
     * @param organization - organization giud
     * @param object - object giud
     * @param responsible - responsible giud
     * @param version - DataVersion
     * @throws ODataErrorException - in case of request failure
     * @return new DataVersion
     */
    String saveUser(@NotNull String auditor, @NotNull String type, @NotNull String organization,
                  @NotNull String object, @NotNull String responsible, @NotNull String version
                  ) throws ODataErrorException {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.AUDITOR.name)
                .appendKeySegment("guid'"+auditor+"'")
                .build();

        try {
            final ODataEntityUpdateRequest<ClientEntity> request = client.getCUDRequestFactory()
                    .getEntityUpdateRequest(entityURI, UpdateType.PATCH,
                        buildUserEntity(type, organization, object, responsible, version));
            request.setIfMatch(version);
            return request.execute().getBody().getProperty(COMMON_VERSION).getPrimitiveValue().toString();
//            final ODataEntityUpdateResponse<ClientEntity> response = request.execute();
//            final String newVersion = response.getBody().getProperty(COMMON_VERSION).getPrimitiveValue().toString();
//            return newVersion;
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e,"Error updating user" );
        }
    }

    //EVERYTHING FOR AUDIT TASKS
    /**
     * Get a list of client entity with a audit tasks from 1C database
     * @param auditor - auditor guid
     * @param status - audit status
     * @param like - substring for selection a tasks by a object name
     * @param skip - load portion parameters:
     *            skip[0] - how many items to skip,
     *            skip[1] - how many items to load
     * @return - loaded part of client entity with a audit task
     * @throws HttpClientException - in case of connection failure
     * @throws ODataErrorException - in case of request failure
     */
    private List<ClientEntity> getAllTasks(String auditor, Tasks.Task.Status status, String like, int... skip)
            throws HttpClientException, ODataRuntimeException {
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

    /**
     * Parsing dates from a string in 1c format
     * @param string - string with a date
     * @return - date from string
     * @throws ODataErrorException - in case of data failure
     */
    private Date parseDate(String string) throws ODataErrorException {
        try {
            return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).parse(string);
        } catch (ParseException e) {
            throw new ODataErrorException(e, "Date parsing error");
        }
    }

    /**
     * Building a string with a date in 1C format
     * @param date - date or null
     * @return - date string
     */
    private String bindDate(Date date) {
        if (date != null)
            return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(date);
        else {
            final Date empty = new Date();
            empty.setTime(0);
            return (new SimpleDateFormat(DATE_FORMAT_1C, Locale.US)).format(empty);
        }
    }

    /**
     * Building a client collection from a list of audit task analytics
     * @param analytics - analitics list
     * @return - client collection
     */
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

    /**
     * Building a client collection from a list of audit task indicators
     * @param indicators - indicators list
     * @return - client collection
     */
    private ClientCollectionValue<ClientValue> buildTaskIndicators(List<Tasks.Task.IndicatorRow> indicators) {
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

    /**
     * Building a client entity from an audit task
     * @param task - audit task
     * @return - client entity
     */
    private ClientEntity buildTaskEntity(Tasks.Task task) {
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
            //Object analytics
            if (task.analytics != null) {
                properties.add(client.getObjectFactory().newCollectionProperty(TASK_ANALYTICS,
                        bindTaskAnalytics(task.analytics)));
            }
            //Audit indicators
            if (task.indicators != null) {
                properties.add(client.getObjectFactory().newCollectionProperty(TASK_INDICATORS,
                        buildTaskIndicators(task.indicators)));
            }
        }
        return entity;
    }

    /**
     * Full parse a task from an client entity
     * @param entity - cliect entity
     * @return - audit task
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of parse failure
     * @throws ODataErrorException - in case of request failure
     */
    private Tasks.Task parseFullTask(ClientEntity entity)
            throws HttpClientException, ODataRuntimeException, ODataErrorException {
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
            //Table with analytics
            for(Object e: entity.getProperty(TASK_ANALYTICS).getCollectionValue().asCollection())
                task.analytics.add(((ClientComplexValue)e).get(TASK_ANALYTIC_VALUE).getPrimitiveValue().toString());
            //Table with indicators
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
            //Media files list
            getMediaFiles(task.mediaFiles, task.id);
        }
        return task;
    }

    /**
     * Selective parse a task from an client entity
     * @param entity - client entity of task
     * @param checked - default flag value with name is the checked
     * @return - audit task
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of parse failure
     */
    private Tasks.Task parseShortTask(ClientEntity entity, boolean checked)
            throws HttpClientException, ODataRuntimeException {
        Tasks.Task task = null;
        if (entity!=null) {
            task = new Tasks.Task();
            task.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();
            task.date = parseDate(entity.getProperty(TASK_DATE).getPrimitiveValue().toString());
            task.status = Tasks.Task.Status.toValue(entity.getProperty(TASK_STATUS).
                    getPrimitiveValue().toString());
            task.type_key = getKey(entity.getProperty(TASK_TYPE_KEY).getValue().toString());
            if (task.type_key!=null) {
                if (entity.getProperties().contains(entity.getProperty(TASK_TYPE)))
                    task.type_name = entity.getProperty(TASK_TYPE).getComplexValue().
                            get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    task.type_name = getName(Set.TYPE, task.type_key);
            }
            else task.type_name = "";
            task.object_key = getKey(entity.getProperty(TASK_OBJECT_KEY).getValue().toString());
            if (task.object_key!= null) {
                if (entity.getProperties().contains(entity.getProperty(TASK_OBJECT)))
                    task.object_name = entity.getProperty(TASK_OBJECT).getComplexValue().
                            get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    task.object_name = getName(Set.OBJECT, task.object_key);
            }
            else {
                task.object_name = "";
            }
            task.achieved = (boolean) entity.getProperty(TASK_ACHIEVED).getPrimitiveValue().toValue();
            task.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
            task.posted = (boolean) entity.getProperty(COMMON_POSTED).getPrimitiveValue().toValue();
            task.number = entity.getProperty(TASK_NUMBER).getPrimitiveValue().toString();
            task.analytic_names = entity.getProperty(TASK_ANALYTIC_NAMES).getPrimitiveValue().toString();
            task.comment = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
            task.checked = checked;
        }
        return task;
    }

    /**
     * Returns 1С guid on a string with a key, which may be empty or null
     * @param key - string with key
     * @return - 1C guid
     */
    private String bindGuid(String key) {
        if (key == null || key.isEmpty()) return EMPTY_KEY;
        return key;
    }

    /**
     * Get a tasks list of auditor from 1C database
     * @param auditor - auditor guid
     * @param status - tasks status for selection
     * @param like - substring for selecting tasks by object name
     * @param skip - load portion parameters:
     *            skip[0] - how many items to skip,
     *            skip[1] - how many items to load
     * @return - loaded part of tasks list
     * @throws ODataErrorException - in case of request failure
     */
    @NonNull
    public Tasks getTasks(String auditor, Tasks.Task.Status status, String like, int... skip)
            throws ODataErrorException {
        Tasks tasks = new Tasks();
        try {
            for (ClientEntity clientEntity: getAllTasks(auditor, status, like, skip))
                tasks.add(parseShortTask(clientEntity, false)); //No checked
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting task list");
        }
        return tasks;
    }

    /**
     * Get audit task
     * @param key - task giud
     * @return - received task
     * @throws ODataErrorException - in case of request failure
     */
    Tasks.Task getTask(String key) throws ODataErrorException {
        Tasks.Task task = null;
        try {
            task = parseFullTask(getFullEntity(Set.TASK, key));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting of task");
        }
        catch (ODataErrorException e) {
            throw new ODataErrorException(e, "Error requesting of media files");
        }
        return task;
    }

    /**
     * Create new task
     * @param task - new task
     * @throws ODataErrorException - in case of request failure
     */
    void createTask(Tasks.Task task) throws ODataErrorException {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .build();
        try {
            final ODataEntityCreateResponse<ClientEntity> response =
                    client.getCUDRequestFactory().getEntityCreateRequest(entityURI, buildTaskEntity(task)).
                            execute();
            //guid of the new task obtained from the response to the request for creation
            task.id = response.getBody().getProperty(COMMON_KEY).getPrimitiveValue().toString();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error creating new task");
        }
    }

    /**
     * Update task
     * @param task - changed task
     * @throws ODataErrorException - in case of request failure
     */
    void updateTask(Tasks.Task task) throws ODataErrorException {
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .appendKeySegment("guid'"+task.id+"'")
                .build();
        try {
            client.getCUDRequestFactory().
                    getEntityUpdateRequest(entityURI, UpdateType.PATCH, buildTaskEntity(task)).
                    execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error updating task");
        }
    }

    /**
     * Change task status
     * @param key - task giud
     * @param status - new status vaalue
     * @return - changed task
     * @throws ODataErrorException - in case of request failure
     */
    Tasks.Task moveTask(String key, Tasks.Task.Status status) throws ODataErrorException {
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(status.id)));
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().
                    getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
            return parseShortTask(response.getBody(),true); //Checked
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error changing task status");
        }
    }

    /**
     * Copy task with status change
     * @param key - giud of old task
     * @param status - new status value
     * @return - new task
     * @throws ODataErrorException - in case of request failure
     */
    Tasks.Task copyTask(String key, Tasks.Task.Status status) throws ODataErrorException {
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
            return parseShortTask(response.getBody(), false); //No checked
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error copying task");
        }
    }

    /**
     * Change the value of the delete task flag
     * @param key - task giud
     * @param delete - value of the delete flag
     * @return - changed task
     * @throws ODataErrorException - in case of request error
     */
    Tasks.Task deleteTask(String key, boolean delete) throws ODataErrorException {
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(delete)));
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.TASK.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().
                    getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
            return parseShortTask(response.getBody(), true); //Checked
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error deleting task");
        }
    }

    //ВСЕ ДЛЯ ВИДОВ АУДИТА
    //возвращает вид аудита

    /**
     * Parse audit type from client entity
     * @param entity - client entity
     * @return - audit type
     */
    private AType parseType(ClientEntity entity) {
        AType type = null;
        if (entity != null) {
            type = new AType();
            type.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();;
            type.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            type.fillActualValue = (boolean) entity.getProperty(TYPE_FILL_ACTUAL_VALUE).getPrimitiveValue().toValue();
            type.openWithIndicators = (boolean) entity.getProperty(TYPE_OPEN_WITH_INDICATORS).getPrimitiveValue().toValue();
            type.clearCopy = (boolean) entity.getProperty(TYPE_CLEAR_COPY).getPrimitiveValue().toValue();
            type.showSubject = (boolean) entity.getProperty(TYPE_SHOW_SUBJECT).getPrimitiveValue().toValue();
            type.selection = AType.Selections.toValue(entity.getProperty(TYPE_SELECTION).getPrimitiveValue().toString());
        }
        return type;
    }

    /**
     * Get a list of client entity with all object types of audit type
     * @param type - audit type guid
     * @return - list of client entity with object types
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private List<ClientEntity> getAllObjectTypes(String type)
            throws HttpClientException, ODataRuntimeException {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.OBJECT_TYPES.name)
                .filter(OBJECT_TYPES_KEY+" eq guid'"+type+"'")
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().
                getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Parse object type from client entity
     * @param entity - client entity with object type
     * @return - object type or null if client entity is null
     */
    private String parseObjectType(ClientEntity entity) {
        String objectType = null;
        if (entity != null)
            objectType =  entity.getProperty(OBJECT_TYPES_TYPE).getPrimitiveValue().toString();
        return objectType;
    }

    /**
     * Get a guid list of all object types of audit type from 1C database
     * @param type - audit type guid
     * @return - guid list of object types
     * @throws ODataErrorException - in case of request failure
     */
    private ArrayList<String> getObjectTypes(String type) throws ODataErrorException {
        final ArrayList<String> list = new ArrayList<>();
        try {
            for(ClientEntity entity: getAllObjectTypes(type)) {
                list.add(parseObjectType(entity));
            }
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting object type");
        }
        return list;
    }

    /**
     * Get the type of audit with all types of objects from the 1C database
     * @param key - audit type guid
     * @return - audit type
     * @throws ODataErrorException - in case of request failure
     */
    AType getAType(String key) throws ODataErrorException {
        AType type = null;
        try {
            type = parseType(getFullEntity(Set.TYPE, key));
            type.objectTypes = getObjectTypes(key);
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting audit type");
        }
        catch (ODataErrorException e) {
            throw new ODataErrorException(e, "Error requesting object types");
        }
        return type;
    }

    /**
     * Updating the type of audit in the 1C database
     * @param type - new audit type
     * @throws ODataErrorException - in case of request failure
     */
    void updateType(AType type) throws ODataErrorException {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //новые значения:
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.name)));
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
        try {
            client.getCUDRequestFactory().
                    getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error updating audit type");
        }
    }

    /**
     * Create a new type of audit in the 1C database
     * @param type - new audit type
     * @throws ODataErrorException - in case of request failure
     */
    void createType(AType type) throws ODataErrorException {
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
//                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.parent)));
//        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PREDEFINED,
//                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.predefined)));
//        ODataEntityCreateResponse<ClientEntity> response;
        try {
            /*response =*/
            client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error creating audit type");
        }
    }

    //EVERYTHING FOR AUDIT OBJECT
    /**
     * Parse an audit object from an client entity
     * @param entity - client entity
     * @return - audit object
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
     * Get the audit object from the 1C database
     * @param key - object guid
     * @return - audit object
     * @throws ODataErrorException - in case of request failure
     */
    private AObject getAObject(String key) throws ODataErrorException {
        AObject object = null;
        try {
            object = parseObject(getFullEntity(Set.OBJECT, key));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting object");
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
//        final String parent = getPater(uriBuilder, children);
//        if (!(EMPTY_KEY.equals(parent) || paters.contains(parent))) {
//            paters.add(parent);
//            addPaters(uriBuilder, parent, paters);
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

    /**
     * Get a table items from 1C database from a specific owner and parent
     * @param table - table name
     * @param owner - owner guid
     * @param parent - parent guid
     * @param like - substring for selection by item name
     * @param parentTypes - parent types list for selection
     * @param skip - load portion parameters:
     *            skip[0] - how many items to skip,
     *            skip[1] - how many items to load
     * @return - loaded part of table items
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of request failure
     */
    private List<ClientEntity> getAllItems(Set table, String owner, String parent, String like,
                                           ArrayList<String> parentTypes, int... skip)
            throws HttpClientException, ODataRuntimeException {
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
                filter.append(COMMON_PARENT).append(" eq guid'").append(parent).append("'");
                if (!(like==null||like.isEmpty()))
                    filter.append("and(substringof('").append(like).append("',").append(COMMON_NAME).append(")or ").append(COMMON_FOLDER).append(")");
                break;
            case ELEMENT_HIERARCHY:
                select.add(COMMON_GROUP);
                filter.append(COMMON_PARENT).append(" eq guid'").append(parent).append("'");
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

    /**
     * Parse a item from client entity
     * @param entity - client entity
     * @param hierarchy - hierarchy type
     * @param checked - checked flag of item
     * @return - item
     */
    private Items.Item parseItem(ClientEntity entity, int hierarchy, boolean checked) {
        Items.Item item = null;
        if (entity!=null) {
            item = new Items.Item();
            item.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();
            item.parent = entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString();
            item.folder = isFolder(entity, hierarchy);
            item.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            item.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
            item.predefined = (boolean) entity.getProperty(COMMON_PREDEFINED).getPrimitiveValue().toValue();
            item.prenamed = entity.getProperty(COMMON_PRENAMED).getPrimitiveValue().toString();
            item.checked = checked;
        }
        return item;
    }

    /**
     * Get table item from 1C database for list
     * @param table - table name
     * @param key - item guid
     * @return - client entity
     * @throws HttpClientException in case of connection failure
     * @throws ODataRuntimeException in case of request failure
     */
    private ClientEntity getShortItem(Set table, String key)
            throws HttpClientException, ODataRuntimeException {
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

    /**
     * Return true if the item is a folder
     * @param entity - client entity with item
     * @param hierarchy - hirerachy type
     * @return - folder flag
     */
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
     *  Get a part of table items from 1C database from a specific owner and parent
     * @param table - table name
     * @param owner - owner guid
     * @param parent - parent guid
     * @param like - substring for selection by item name
     * @param parentTypes - parent types list for selection
     * @param skip - load portion parameters:
     *            skip[0] - how many items to skip,
     *            skip[1] - how many items to load
     * @return - loaded part of table items
     * @throws ODataRuntimeException - in case of request failure
     */
    Items getItems(Set table, String owner, String parent, String like, ArrayList<String> parentTypes,
                   int... skip) throws ODataErrorException {
        Items items = new Items();
        try {
            for (ClientEntity entity: getAllItems(table, owner, parent, like, parentTypes, skip))
                items.add(parseItem(entity, table.hierarchy,false)); //No checked
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting items");
        }
        return items;
    }

    /**
     * Get table item from 1C database
     * @param table - table name
     * @param key - item guid
     * @return - table item
     * @throws ODataErrorException - in case of request failure
     */
    Items.Item getItem(Set table, String key) throws ODataErrorException {
        Items.Item item = new Items.Item();
        try {
            item = parseItem(getShortItem(table, key), table.hierarchy, false);
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting item");
        }
        return item;
    }

    /**
     * Copy table item to new parent in 1C database
     * @param table - table name
     * @param key - old item guid
     * @param parent - new parent guid
     * @return - new item
     * @throws ODataErrorException - in case of request failure
     */
    Items.Item copyItem(Set table, String key, String parent) throws ODataErrorException {
        final ODataEntityCreateResponse<ClientEntity> response;
        try {
            final ClientEntity entity = getFullEntity(table, key);
            //Удаляем свойство с идентификатором - 1С присвоит новый
            entity.getProperties().remove(entity.getProperty(COMMON_KEY));
            //Меняем родителя
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(parent)));
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
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error copying item");
        }
        return parseItem(response.getBody(), table.hierarchy,false);
    }

    /**
     * Move table item to new parent in 1C database.
     * Cyclic error control will be carried out externally in ReferenceChoice
     * @param table - table name
     * @param key - old item guid
     * @param parent - new parent guid
     * @return - moved item
     * @throws ODataErrorException - in case of request failure
     */
    Items.Item moveItem(Set table, String key, String parent) throws ODataErrorException {
        if (table.hierarchy==NOT_HIERARCHY) { //Если набор не является иерархией!!!
            throw new RuntimeException("AuditOData.moveItem('"+table+"', guid'"+key+"', guid'"+parent+"') Entity set '"+table+"' is not the hierarchy.");
        }
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //Изменяем родителя
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(parent)));
        ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI,
                    UpdateType.PATCH, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error moving item");
        }
        return parseItem(response.getBody(), table.hierarchy, true);
    }

    /**
     * Update the delete label of a table in 1C database
     * @param table - table name
     * @param key - item guid
     * @param delete - delete label value
     * @return - updated item
     * @throws ODataErrorException - in case of connection failure
     */
    Items.Item deleteItem(Set table, String key, boolean delete) throws ODataErrorException {
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
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI,
                    UpdateType.PATCH, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error updating delete label");
        }
        return parseItem(response.getBody(), table.hierarchy, true);
    }

    /**
     * Update the description item of a table in 1C database
     * @param table - table name
     * @param item - table item with new description
     * @return - updated item
     * @throws ODataErrorException - in case of request failure
     */
    Items.Item updateItem(Set table, Items.Item item) throws ODataErrorException {
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
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI,
                    UpdateType.PATCH, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error updating item");
        }
        return parseItem(response.getBody(), table.hierarchy, true);
    }

    /**
     * Create a new item or group in the 1C database table
     * It is suitable for creating folders
     * @param table - table name
     * @param parent - parent guid
     * @param name - desription
     * @param isGroup - group creation flag
     * @return - new item group
     * @throws RuntimeException - in case of data or request failure
     */
    Items.Item createItem(Set table, String parent, String name, boolean isGroup)
            throws RuntimeException {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(table.name)
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        final List<ClientProperty> propertys = entity.getProperties();
        //Наименование
        propertys.add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(name)));
        //Устанавливаем признак группы
        switch (table.hierarchy) {
            case FOLDER_HIERARCHY:
                propertys.add(client.getObjectFactory().newPrimitiveProperty(COMMON_FOLDER,
                        client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(isGroup)));
                break;
            case ELEMENT_HIERARCHY:
                propertys.add(client.getObjectFactory().newPrimitiveProperty(COMMON_GROUP,
                        client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(isGroup)));
                break;
            case NOT_HIERARCHY: default:
                if (isGroup) {
                    throw new RuntimeException("Error on create of item group. The '\"+table+\"' set do not have hierarchy.");
                }
        }
        //Родитель
        propertys.add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(parent)));
        ODataEntityCreateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error creating item");
        }
        return parseItem(response.getBody(), table.hierarchy, false);
    }

    //EVERYTHING FOR ANALYTIC TYPES
    /**
     * Get all analytical types of any object and type from 1C database
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @return - list of client entity with analytics types
     * @throws HttpClientException - in case of connect failure
     * @throws ODataRuntimeException - in case of request failure
     */
    private List<ClientEntity> getAllAnalyticTypes (@NonNull String typeKey, @NonNull String objectKey)
            throws HttpClientException, ODataRuntimeException {
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        final URI entitySetURI = uriBuilder
                .appendEntitySetSegment(Set.ANALYTIC_RELAT.name)
                .filter(RELAT_TYPE_KEY+" eq guid'"+typeKey+"' and "+
                        RELAT_OBJECT_TYPE+" eq '"+objectKey+"'")
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Parse a analytic type from client entity
     * @param entity - client entity with analytics type
     * @return - analytic type or null if client entity is null
     */
    private String parseAnalyticTypes(ClientEntity entity) {
        String analyticType = null;
        if (entity!=null)
            analyticType = entity.getProperty(RELAT_ANALYTIC_TYPE).getPrimitiveValue().toString();
        return analyticType;
    }

    /**
     * Get all analytical types of any object and type of audit from 1C database
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @return - guid list with analytics types
     * @throws ODataErrorException - in case of request failure
     */
    ArrayList<String> getAnalyticTypes(@NonNull String typeKey, @NonNull String objectKey) {
        ArrayList<String> parentTypes = new ArrayList<>();
        try {
            for (ClientEntity entity: getAllAnalyticTypes(typeKey, objectKey))
                parentTypes.add(parseAnalyticTypes(entity));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting analytics types");
        }
        return parentTypes;
    }

    /**
     * Get a analytics of any object and type of audit from 1C database
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @param like - substring for selection by analytics name
     * @param skip - load portion parameters:
     *            skip[0] - how many items to skip,
     *            skip[1] - how many items to load
     * @return - loaded part of client entities with analytics
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private List<ClientEntity> getAllAnalytics (@NonNull String typeKey, @NonNull String objectKey,
                                                String like, int... skip)
            throws HttpClientException, ODataRuntimeException {
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRootOData);
        final StringBuilder filter = new StringBuilder();
        filter.append(CORR_TYPE_KEY).append(" eq guid'").append(typeKey).append("' and ")
                .append(CORR_OBJECT_KEY).append(" eq guid'").append(objectKey).append("'");
        if (!(like==null||like.isEmpty()))
            filter.append(" and substringof('").append(like).append("',")
                    .append(CORR_ANALYTIC+"/"+COMMON_NAME).append(")");
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
     * Parse a analytic item from client entity
     * @param entity - client entity with analytic
     * @return - analytic or null if client entity is null
     */
    private Items.Item parseAnalytic(ClientEntity entity) {
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
                }
            }
        }
        return item;
    }

    /**
     * Get a analytics of any object and type of audit from 1C database
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @param like - substring for selection by analytics name
     * @param skip - load portion parameters:
     *            skip[0] - how many items to skip,
     *            skip[1] - how many items to load
     * @return - loaded part of items with a analytics
     * @throws ODataErrorException - in case of request failure
     */
    Items getAnalytics(String typeKey, String objectKey, String like, int... skip) {
        Items items = new Items();
        try {
            for (ClientEntity entity: getAllAnalytics(typeKey, objectKey, like, skip))
                items.add(parseAnalytic(entity));
        }
        catch (ODataErrorException e) {
            throw new ODataErrorException(e, "Error requesting analytics");
        }
        return items;
    }

    /**
     * Create a new analytical link in the 1C database table
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @param analyticKey - object analytic giud
     * @throws RuntimeException - in case of data or request failure
     */
    void createAnalytic(String typeKey, String objectKey, String analyticKey)
            throws RuntimeException {
        final URI entityURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.ANALYTIC_CORR.name)
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        final List<ClientProperty> propertys = entity.getProperties();
        propertys.add(client.getObjectFactory().newPrimitiveProperty(CORR_TYPE_KEY,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(typeKey)));
        propertys.add(client.getObjectFactory().newPrimitiveProperty(CORR_OBJECT_KEY,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(objectKey)));
        propertys.add(client.getObjectFactory().newPrimitiveProperty(CORR_ANALYTIC_KEY,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(analyticKey)));

        ODataEntityCreateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error creating analytic link");
        }
    }

    //EVERYTHING FOR AUDIT INDICATOR STANDARDS
    /**
     * Get a all standards of indicators of any object and type of audit from the 1C database
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @return list of client entity with standards of indicators
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private List<ClientEntity> getAllIndicatorStandard(String typeKey, String objectKey)
            throws HttpClientException, ODataRuntimeException {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.INDICATOR_STANDARD.name)
                .filter(INDICATOR_STANDARD_TYPE+" eq guid'"+typeKey+"' and "+INDICATOR_STANDARD_OBJECT+
                        " eq guid'"+objectKey+"'")
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet =
                client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Parse a standard of indicator
     * @param entity - client entity
     * @return - standard of indicator
     */
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
     * Get a all standards of indicators of any object and type of audit from the 1C database
     * @param typeKey - audit type giud
     * @param objectKey - audit object giud
     * @return - standarts of indicators
     * @throws ODataErrorException - in case of request failure
     */
    ArrayList<Tasks.Task.IndicatorRow> getStandardIndicatorRows(String typeKey, String objectKey)
            throws ODataErrorException {
        final ArrayList<Tasks.Task.IndicatorRow> indicators = new ArrayList<>();
        try {
            for (ClientEntity entity : getAllIndicatorStandard(typeKey != null? typeKey: EMPTY_KEY,
                    objectKey != null? objectKey: EMPTY_KEY)) {
                indicators.add(parseIndicatorStandard(entity));
            }
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting standards of indicators");
        }
        return indicators;
    }

    //EVERYTHING FOR INDICATORS OF TASK
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

    //EVERYTHING FOR AUDIT INDICATORS
//    //Возвращает показатель аудита по entity
//    private Indicators.Indicator parseFullIndicator(ClientEntity entity) {
//        final Indicators.Indicator indicator = new Indicators(). new Indicator();;
//        if (entity != null) {
//            try {
//                indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
//                indicator.code = entity.getProperty(COMMON_CODE).getPrimitiveValue().toString();
//                indicator.id = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
//                indicator.parent = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
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

    /**
     * Parse a indicators row of audit task from a client entity
     * @param entity - client entity with indicators row
     * @return - indicators row of audit task
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private Tasks.Task.IndicatorRow parseIndicatorRow(ClientEntity entity)
            throws ODataRuntimeException {
        final Tasks.Task.IndicatorRow row = new Tasks.Task().new IndicatorRow();
        if (entity != null) {
            row.indicator = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
            row.goal = getValue(entity, COMMON_GOAL);
            row.minimum = getValue(entity, COMMON_MINIMUM);
            row.maximum = getValue(entity, COMMON_MAXIMUM);
            row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
        }
        return row;
    }

//    //Возвращает показатель аудита по entity
//    private Indicators.Indicator parseShortIndicator(ClientEntity entity) {
//        final Indicators.Indicator indicator = new Indicators(). new Indicator();
//        if (entity != null) {
//            indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
//            indicator.id = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
//            indicator.parent = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
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
     * Parse a audit indicators from a client entity
     * @param entity - client entity with indicator
     * @return - audit indicator
     * @throws ODataErrorException - in case of request failure
     */
    private IndList.Ind parseInd(ClientEntity entity)
            throws ODataRuntimeException {
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
//    private List<ClientEntity> getAllIndicators(@NonNull String type) {
//        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
//                .appendEntitySetSegment(Set.INDICATOR.name)
//                .filter(COMMON_OWNER+" eq guid'"+type+"' and "+
//                        COMMON_DELETED+" eq false and "+
//                        COMMON_FOLDER+" eq false")
//                .select(COMMON_OWNER, COMMON_DELETED, COMMON_FOLDER,
//                        COMMON_KEY, INDICATOR_TYPE,
//                        COMMON_GOAL, COMMON_GOAL+_TYPE,
//                        COMMON_MINIMUM, COMMON_MINIMUM+_TYPE,
//                        COMMON_MAXIMUM, COMMON_MAXIMUM+_TYPE,
//                        COMMON_ERROR, COMMON_ORDER)
//                .orderBy(COMMON_ORDER+ORDER_ASC)
//                .build();
//        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().
//                getEntitySetRequest(entitySetURI).execute();
//        return entitySet.getBody().getEntities();
//    }

    /**
     * Get all indicators for any type of audit from 1C database
     * @param typeKey - audit type guid
     * @param onlyElements - flag for selecting only elements without groups
     * @param withoutDeleted - flag for selecting not deleted items
     * @return - list of client entity
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private List<ClientEntity> getAllIndicators(@NonNull String typeKey, boolean onlyElements,
                                                boolean withoutDeleted )
            throws HttpClientException, ODataRuntimeException {
        final StringBuilder filter = new StringBuilder();
        filter.append(COMMON_OWNER).append(" eq guid'").append(typeKey).append("'");
        if (onlyElements) filter.append(" and ").append(COMMON_FOLDER).append(" eq false");
        if (withoutDeleted) filter.append(" and ").append(COMMON_DELETED).append(" eq false");
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.INDICATOR.name)
                .filter(filter.toString())
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().
                getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

//    //Возвращает список entity с показателями аудита
//    private List<ClientEntity> getAllIndicators(String type, String parent, String subject) {
//        final StringBuilder filter = new StringBuilder();
//        filter.append(COMMON_OWNER).append(" eq guid'").append(type).append("'");
//        if (parent != null)
//            filter.append(" and ").append(COMMON_PARENT).append(" eq guid'").append(parent).append("'");
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
     * Get all indicators for any type of audit from 1C database
     * @param typeKey - audit type guid
     * @return - list of indicators rows for the audit task
     * @throws ODataErrorException - in case of requesting failure
     */
    ArrayList<Tasks.Task.IndicatorRow> getIndicatorRows(String typeKey) throws ODataErrorException {
        final ArrayList<Tasks.Task.IndicatorRow> indicators = new ArrayList<>();
        try {
            for (ClientEntity entity: getAllIndicators(typeKey != null? typeKey: EMPTY_KEY,
                    true, true))
                indicators.add(parseIndicatorRow(entity));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting indicators");
        }
        return indicators;
    }

    /**
     * Add indicators to the list for any type of audit from the 1C database
     * @param indList - list of indicators for additions
     * @param typeKey - audit type guid
     * @param onlyElements - flag for selecting only elements without groups
     * @throws ODataErrorException - in case of requesting failure
     */
    void addInd(@NonNull IndList indList, @NonNull String typeKey, boolean onlyElements)
            throws ODataErrorException {
        try {
            for (ClientEntity clientEntity: getAllIndicators(typeKey, onlyElements, false))
                indList.add(parseInd(clientEntity));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting indicators");
        }
    }

    //EVERYTHING FOR A AUDIT SUBJECTS
//    /**
//     * Получить все предметы по виду аудита и родителю
//     * @param type - guid вида аудита
//     * @param parent- guid родителя предмета
//     * @return - список ентити с предметами
//     */
//    private List<ClientEntity> getAllSubjects(@NonNull String type, @NonNull String parent) {
//        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
//                .appendEntitySetSegment(Set.SUBJECT.id)
//                .filter(COMMON_OWNER+" eq guid'"+type+"' and "+
//                        COMMON_PARENT+" eq guid'"+parent+"'")
//                .orderBy(COMMON_ORDER+ORDER_ASC)
//                .build();
//        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
//        return entitySet.getBody().getEntities();
//    }

    /**
     * Get all subjects for any type of audit from 1C database
     * @param typeKey - audit type guid
     * @return - list of clients entity with audit subjects
     * @throws HttpClientException - in case of connection failure
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private List<ClientEntity> getAllSubjects(@NonNull String typeKey)
            throws HttpClientException, ODataRuntimeException {
        final URI entitySetURI = client.newURIBuilder(serviceRootOData)
                .appendEntitySetSegment(Set.SUBJECT.name)
                .filter(COMMON_OWNER+" eq guid'"+typeKey+"'")
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().
                getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Parse a subject from client entity
     * @param entity - client entity with audit subject
     * @return - audit subject for list of indicators.
     * This is done to show audit indicators by subject
     * @throws ODataRuntimeException - in case of requesting failure
     */
    private IndList.Ind parseSubject(ClientEntity entity) throws ODataRuntimeException {
        final IndList.Ind subject = new IndList.Ind();
        if (entity != null) {
            subject.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
            subject.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            subject.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
            subject.folder = true; //All subjects must be groups for audit indicators.
            subject.type = Indicators.Types.IS_BOOLEAN;
            subject.criterion = Indicators.Criteria.EQUALLY;
        }
        return subject;
    }

    /**
     * Add subjects as indicators groups to the list for any type of audit from 1C database.
     * This is done to show audit indicators by subject
     * @param indList - list of indicators for addition
     * @param typeKey - audit type guid
     * @throws ODataErrorException - in case of requesting failure
     */
    void addSubjects(@NonNull IndList indList, @NonNull String typeKey) throws ODataErrorException {
        try {
            for (ClientEntity clientEntity: getAllSubjects(typeKey))
                indList.add(parseSubject(clientEntity));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting subjects");
        }
    }

    //EVERYTHING FOR A LIST OF MEDIA FILES
    /**
     * Get a list of client entities this task
     * @param task_key - task guid
     * @return - list of client entities
     * @throws HttpClientException - in case of connection error
     * @throws ODataRuntimeException - in case of request error
     */
    private List<ClientEntity> getAllMediaFiles(@NonNull String task_key)
            throws HttpClientException, ODataRuntimeException {
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
     * Parse a media file by a client entity
     * @param entity - client entity with media file
     * @return - media file
     * @throws HttpClientException - in case of connection failure
     * @throws ODataErrorException - in case of request failure
     */
    private MediaFiles.MediaFile parseMediaFile(ClientEntity entity)
            throws HttpClientException, ODataRuntimeException {
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
     * Add media files list of task
     * @param mediaFiles - media files list
     * @param task_key - task guid
     * @throws HttpClientException - in case of connection error
     * @throws ODataErrorException - in case of request error
     */
    private void getMediaFiles(@NonNull MediaFiles mediaFiles, @NonNull String task_key)
            throws HttpClientException, ODataErrorException {
        try {
            for (ClientEntity clientEntity: getAllMediaFiles(task_key))
                mediaFiles.add(parseMediaFile(clientEntity));
        }
        catch (HttpClientException e) {
            throw new ODataErrorException(e, CONNECTION_ERROR);
        }
        catch (ODataRuntimeException e) {
            throw new ODataErrorException(e, "Error requesting media files list");
        }
    }

}
//Фома2018