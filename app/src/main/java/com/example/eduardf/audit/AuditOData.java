package com.example.eduardf.audit;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.client.api.communication.ODataClientErrorException;
import org.apache.olingo.client.api.communication.ODataServerErrorException;
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

//Класс для доступа в 1С:Аудитор через OData
public class AuditOData{

    public static final String EMPTY_KEY = "00000000-0000-0000-0000-000000000000"; //Пустая ссылка

    private static final String WITHOUT_TABLE = "**";
    public static final String DATE_FORMAT_1C = "yyyy-MM-dd'T'HH:mm:ss"; //Шаблон формата даты для 1С

    //Виды иерархии справочников:
    public static final int NOT_HIERARCHY = 0; // нет иерархии
    public static final int FOLDER_HIERARCHY = 1; // папок и элементов
    public static final int ELEMENT_HIERARCHY = 2; // только элеметов

    //Коллекции
    private static final String ENTITY_SET_TASK = "Document_Аудит_ЗаданиеНаАудит";
    private static final String ENTITY_SET_TASK_ANALYTIC = "Document_Аудит_ЗаданиеНаАудит_АналитикаОбъекта";
    private static final String ENTITY_SET_TASK_INDICATOR = "Document_Аудит_ЗаданиеНаАудит_Показатели";
    private static final String ENTITY_SET_AUDITOR = "Catalog_Аудит_Аудиторы";
    public static final String ENTITY_SET_TYPE = "Catalog_Аудит_ВидыАудитов";
    public static final String ENTITY_SET_ORGANIZATION = "Catalog_Аудит_Организации";
    public static final String ENTITY_SET_OBJECT = "Catalog_Аудит_Объекты";
    public static final String ENTITY_SET_RESPONSIBLE = "Catalog_Аудит_Ответственные";
    public static final String ENTITY_SET_ANALYTIC = "Catalog_Аудит_АналитикиОбъектов";
    public static final String ENTITY_SET_INDICATOR = "Catalog_Аудит_ПоказателиАудитов";
    private static final String ENTITY_SET_SUBJECT = "Catalog_Аудит_ПредметыАудитов";
    private static final String ENTITY_SET_INDICATOR_STANDARD = "InformationRegister_Аудит_НормативыЗначенийПоказателей";
    private static final String ENTITY_SET_UNIT = "Catalog_Аудит_ЕдиницыИзмерения";

    //Фильтр по справочнику аудиторов, чтобы отфильтровать пользователей мобильных приложений
    private static final String FILTER_AUDITORS = "ParentType eq 'СправочникСсылка.Аудит_Аудиторы'";

    private static final String USERS_PASSWORD = "Пароль";

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
    private static final String COMMON_CODE = "Code";
    private static final String COMMON_PREDEFINED = "Predefined";
    private static final String COMMON_PRENAMED = "PredefinedDataName";
    private static final String COMMON_COMMENT = "Комментарий";
    private static final String COMMON_GOAL = "ЦелевоеЗначение";
    private static final String COMMON_MINIMUM = "МинимальноеЗначение";
    private static final String COMMON_MAXIMUM = "МаксимальноеЗначение";
    private static final String COMMON_ERROR = "Погрешность";
    private static final String COMMON_ORDER = "Приоритет";
    private static final String _TYPE = "_Type";
    public static final String UNDEFINED_TYPE = "StandardODATA.Undefined";
    public static final String DOUBLE_TYPE = "Edm.Double";
    public static final String BOOLEAN_TYPE = "Edm.Boolean";
    public static final String DATE_TYPE = "Edm.DateTime";

    //Поля заданий на аудит
    private static final String TASK_STATUS = "Статус";
        private static final String ENUM_STATUS_0 = "Утвержден";
        private static final String ENUM_STATUS_1 = "ВРаботе";
        private static final String ENUM_STATUS_2 = "Проведен";
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
    private static final String INDICATOR_CRITERION = "Критерий";
    private static final String INDICATOR_TYPE = "ТипПоказателя";
    private static final String INDICATOR_UNIT = "ЕдиницаИзмерения_Key";

    //Поля нормативов показателей
    private static final String INDICATOR_STANDARD_TYPE = "ВидАудита_Key";
    private static final String INDICATOR_STANDARD_OBJECT = "Объект_Key";
    private static final String INDICATOR_STANDARD_INDICATOR = "Показатель_Key";

    //Реквизиты класса
    private final ODataClient client; //Клиент OData
    private final String serviceRoot; //Путь к oData
    private final FragmentActivity activity; //Контент активности для сообщения об ошибке доступа к 1С с помощью DialogFragment

    //Конструктор
    public AuditOData(Context context) {
        this.activity = (FragmentActivity) context;
        final SharedPreferences pr = PreferenceManager.getDefaultSharedPreferences(context);
        serviceRoot = pr.getString("odata_path", "");
        client = ODataClientFactory.getClient();
        client.getConfiguration().setDefaultPubFormat(ContentType.JSON_NO_METADATA);
        client.getConfiguration().setHttpClientFactory(new BasicAuthHttpClientFactory(pr.getString("odata_user", ""), pr.getString("odata_password", "")));
    }

    //Возвращает значение в виде объекта, определенного вида
    private Object getValue(@NonNull ClientProperty value, @NonNull ClientProperty type) {
        final String string = value.getPrimitiveValue().toString();
        switch (type.getPrimitiveValue().toString()) {
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
        String type1 = value.getClass().getCanonicalName();
        switch (type1) {
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
    private ClientEntity getFullEntity(String table, String key) {
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(table)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataRetrieveResponse<ClientEntity> entity = client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
        return entity.getBody();
    }

    //Возвращает true, если ключ - пустая ссылка
    private boolean isEmptyKey(String key) { return key==null || EMPTY_KEY.equals(key); }

    /**
     * Для вывода служебного сообщения в спиннере на время загрузки и т.п.
     * @param message - текст служебного сообщения
     * @return - список с единственным пунктом служебного сообщения
     */
    public static List<Map<String, Object>> newDataSpinner(String message) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", EMPTY_KEY);
        map.put("name", message);
        list.add(map);
        return list;
    }

    //Выводит сообщение об ошибке в диалоговом окне
    private boolean sayErrorMessage(@NonNull Exception e) {
        //ДОБАВИТЬ ДИАГНОСТИКУ ПО ЧЕЛОВЕЧЕСКИ!!!
        (DialogODataError.newInstance("666",
                e.getLocalizedMessage())).show(activity.getSupportFragmentManager(), "");
        return true;
    }

    /**
     *  Наименование элемента справочника по id
     *  @param set Имя сущности
     *  @param key Guid экземпляра сущности
     *  @return Свойство Description экземпляра сущности или "", если key пустой
     */
    public String getName(String set, String key) {
        if (!(key == null || EMPTY_KEY.equals(key))) {
            final URI entityURI = client.newURIBuilder(serviceRoot)
                    .appendEntitySetSegment(set)
                    .appendKeySegment("guid'"+key+"'")
                    .select(COMMON_NAME)
                    .build();
            final ODataRetrieveResponse<ClientEntity> entity;
            try {
                entity = client.getRetrieveRequestFactory().getEntityRequest(entityURI).execute();
            } catch (ODataRuntimeException e) {
                if (!sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.getName('" + set + "', guid'" + key +
                            "') Error on requesting of name ." + e.getMessage());
                }
                return null;
            }
            return entity.getBody().getProperty(COMMON_NAME).getValue().toString();
        }
        return "";
    }

    // возвращает guid или null вместо EMPTY_KEY
    @Nullable
    private String getKey(String key) {
        if (EMPTY_KEY.equals(key)) return null;
        return key;
    }

    //ВСЕ ДЛЯ СПИСКА АУДИТОРОВ
    //возвращает список Entity с аудиторами
    private List<ClientEntity> getAllUsers() {
        final URI userEntitySetURI = client
                .newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_AUDITOR)
                .filter(FILTER_AUDITORS)
                .select(COMMON_KEY, COMMON_NAME, USERS_PASSWORD)
                .build();
        final ODataRetrieveResponse<ClientEntitySet>
            users = client.getRetrieveRequestFactory()
                .getEntitySetRequest(userEntitySetURI).execute();
        return users.getBody().getEntities();
    }

    //возвращает список Map с аудиторами для Spinner
    public List<Map<String, Object>> getUsers() {
        List<Map<String, Object>> usersMap = new ArrayList<Map<String, Object>>();
        try {
            for (ClientEntity clientEntity : getAllUsers()) {
                Map<String, Object> user = new HashMap<String, Object>();
                user.put("id", clientEntity.getProperty(COMMON_KEY).getValue());
                user.put("name", clientEntity.getProperty(COMMON_NAME).getValue());
                user.put("password", clientEntity.getProperty(USERS_PASSWORD).getValue());
                usersMap.add(user);
            }
        }
        catch (HttpClientException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getAllUsers() Error on requesting of users." + e.getMessage());
            }
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getAllUsers() Error on requesting of users." + e.getMessage());
            }
        }
        return usersMap;
    }

    //ВСЕ ДЛЯ ЗАДАНИЙ НА АУДИТ
    //возвращает список Entity с заданиями с обором по аудитору, статусу и наименованию
    private List<ClientEntity> getAllTasks(String auditor, int status, String like) {
        String filter = TASK_AUDITOR_KEY+" eq guid'"+auditor+"' and "+TASK_STATUS+" eq '"+ bindStatus(status)+"'";
        if (!(like==null||like.isEmpty())) filter += " and substringof('"+like+"',"+TASK_OBJECT+"/"+COMMON_NAME+")";
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                    .appendEntitySetSegment(ENTITY_SET_TASK)
                    .select(COMMON_KEY, TASK_DATE, COMMON_DELETED, COMMON_POSTED, TASK_ACHIEVED, TASK_NUMBER,
                            TASK_TYPE_KEY, TASK_OBJECT_KEY, COMMON_COMMENT, TASK_ANALYTIC_NAMES,
                            TASK_TYPE+"/"+COMMON_NAME, TASK_OBJECT+"/"+COMMON_NAME, WITHOUT_TABLE)
                    .expand(TASK_TYPE, TASK_OBJECT)
                    .filter(filter)
                    .orderBy(TASK_DATE +ORDER_DESC)
                    .build();
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
//            e.printStackTrace();
//            throw new RuntimeException("AuditOData.parseDate('"+string+"') Error on parsing of date.");
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

    // извлекает статус из строки
    private int parseStatus(String status) {
        switch (status) {
            case ENUM_STATUS_0: return 0;
            case ENUM_STATUS_1: return 1;
            case ENUM_STATUS_2: return 2;
            default: throw new RuntimeException("AuditOData.parseStatus('"+status+"') Error on parsing of status.");
        }
    }

    // возвращает строку со статусом по номеру
    private String bindStatus(int status) {
        switch (status) {
            case 0: return ENUM_STATUS_0;
            case 1: return ENUM_STATUS_1;
            case 2: return ENUM_STATUS_2;
            default:
                throw new RuntimeException("AuditOData.getTasks(auditor, status): The Status parameter must have values: 0, 1 or 2");
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
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_GOAL,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.valueToString(row.goal))));
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_GOAL+_TYPE,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.toEdmType())));
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_MINIMUM,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.valueToString(row.minimum))));
/*
 * 1С воспринимает значения минимум и максимум только с типом Edm.Double!!! Возможно это связано недоделанной конфигурацией?
 * Поэтому, пока, будем ставить Edm.Double.
 */
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_MINIMUM+_TYPE,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.toEdmType())));
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString("Edm.Double")));
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_MAXIMUM,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.valueToString(row.maximum))));
/*
 * 1С воспринимает значения минимум и максимум только с типом Edm.Double!!! Возможно это связано недоделанной конфигурацией?
 * Поэтому, пока, будем ставить Edm.Double.
 */
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_MAXIMUM+_TYPE,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.toEdmType())));
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString("Edm.Double")));
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(COMMON_ERROR,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(
//                            (row.error == (long)row.error)?
//                                    String.format(Locale.US, "%d", (long)row.error):
//                                    String.format(Locale.US, "%s", row.error)
//                    )));
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(TASK_INDICATOR_VALUE,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.valueToString(row.value))));
//            complexValue.add(client.getObjectFactory().newPrimitiveProperty(TASK_INDICATOR_VALUE+_TYPE,
//                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(row.type.toEdmType())));
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
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindDate(task.date))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindStatus(task.status))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_AUDITOR_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(task.auditor_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_TYPE_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(task.type_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_ORGANIZATION_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(task.organization_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_OBJECT_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(task.object_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(TASK_RESPONSIBLE_KEY,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindGuid(task.responsible_key))));
            properties.add(client.getObjectFactory().newPrimitiveProperty(COMMON_COMMENT,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(task.comment)));
            //Аналитика объекта
            if (task.analytics != null) {
                properties.add(client.getObjectFactory().newCollectionProperty(TASK_ANALYTICS, bindTaskAnalytics(task.analytics)));
            }
            //Показатели аудита
            if (task.indicators != null) {
                properties.add(client.getObjectFactory().newCollectionProperty(TASK_INDICATORS, bindTaskIndicators(task.indicators)));
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
            task.status = parseStatus(entity.getProperty(TASK_STATUS).getPrimitiveValue().toString());
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
                row.type = getIndicator(row.indicator).type; //Неоптимально!!! Только из-за типа приходится запрашивать ПОЛНЫЙ вид аудита
                row.goal = getValue(((ClientComplexValue)e).get(COMMON_GOAL), ((ClientComplexValue)e).get(COMMON_GOAL+_TYPE));
                row.minimum = getValue(((ClientComplexValue)e).get(COMMON_MINIMUM), ((ClientComplexValue)e).get(COMMON_MINIMUM+_TYPE));
                row.maximum = getValue(((ClientComplexValue)e).get(COMMON_MAXIMUM), ((ClientComplexValue)e).get(COMMON_MAXIMUM+_TYPE));
                row.value = getValue(((ClientComplexValue)e).get(TASK_INDICATOR_VALUE), ((ClientComplexValue)e).get(TASK_INDICATOR_VALUE+_TYPE));
                row.error = Float.valueOf(((ClientComplexValue)e).get(COMMON_ERROR).getPrimitiveValue().toString());
                row.comment = ((ClientComplexValue)e).get(COMMON_COMMENT).getPrimitiveValue().toString();
                row.achived = (boolean) ((ClientComplexValue)e).get(TASK_INDICATOR_ACHIEVED).getPrimitiveValue().toValue();
                task.indicators.add(row);
            }
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
            task.type_key = getKey(entity.getProperty(TASK_TYPE_KEY).getValue().toString());
            if (task.type_key!=null) {
                if (entity.getProperties().contains(entity.getProperty(TASK_TYPE)))
                    task.type_name = entity.getProperty(TASK_TYPE).getComplexValue().get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    task.type_name = getName(ENTITY_SET_TYPE, task.type_key);
            }
            else task.type_name = "";
            task.object_key = getKey(entity.getProperty(TASK_OBJECT_KEY).getValue().toString());
            if (task.object_key!= null) {
                if (entity.getProperties().contains(entity.getProperty(TASK_OBJECT)))
                    task.object_name = entity.getProperty(TASK_OBJECT).getComplexValue().get(COMMON_NAME).getPrimitiveValue().toString();
                else
                    task.object_name = getName(ENTITY_SET_OBJECT, task.object_key);
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
    public Tasks getTasks(String auditor, int status, String like) {
        Tasks tasks = new Tasks();
        try {
            for (ClientEntity clientEntity: getAllTasks(auditor, status, like))
                tasks.add(parseShortTask(clientEntity, false, false)); //Не отмечен и свернут
        }
        catch (ODataRuntimeException e) {
                if (!sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.getTasks() Error on requesting of tasks ." + e.getMessage());
                }
            }
        return tasks;
    }

    /**
     * Получить задание
     * @param key - giud задания
     * @return - задание
     */
    public Tasks.Task getTask(String key) {
        Tasks.Task task = null;
        try {
            task = parseFullTask(getFullEntity(ENTITY_SET_TASK, key), false, false);
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
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
    public void createTask(Tasks.Task task) {
        //Запрос на создание задания
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TASK)
                .build();
        try {
            client.getCUDRequestFactory().getEntityCreateRequest(entityURI, bindTaskEntity(task)).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.createTask('" + task.toString() +
                        "') Error creating new task. " + e.getMessage());
            }
        }
    }

    /**
     * Изменение реквизитов задания - сохранение
     * @param task задание с новыми значениями реквизитов
     */
    public void updateTask(Tasks.Task task) {
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TASK)
                .appendKeySegment("guid'"+task.id+"'")
                .build();
        try {
            client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, bindTaskEntity(task)).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.updateTask('"+task.toString()+
                        "') Error on update of task. "+e.getMessage());
            }
        }
    }

    /**
     * Перемещение задания в другую папку - изменение статуса.
     * @param key - giud задания
     * @param status - новое значение статуса задания
     * @return - измененное задание с реквизитами, достаточными для отображения в списке.
     */
    public Tasks.Task moveTask(String key, int status) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindStatus(status))));
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TASK)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
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
    public Tasks.Task copyTask(String key, int status) {
        final ODataEntityCreateResponse<ClientEntity> response;
        try {
            //Исходное задание
            ClientEntity entity = getFullEntity(ENTITY_SET_TASK, key);
            //Изменяем entity
            //Удаляем свойство с идентификатором - 1С присвоит новый
            entity.getProperties().remove(entity.getProperty(COMMON_KEY));
            //Удаляем свойство с номером задания - 1С присвоит новый
            entity.getProperties().remove(entity.getProperty(TASK_NUMBER));
            //Устанавливаем текущую дату и время
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_DATE,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindDate(new Date()))));
            //Меняем статус
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TASK_STATUS,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildString(bindStatus(status))));
            //Отметка на удаление должна быть снята
            entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                    client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(false)));
            //Запрос на создание
            final URI entityURI = client.newURIBuilder(serviceRoot)
                    .appendEntitySetSegment(ENTITY_SET_TASK)
                    .build();
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
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
    public Tasks.Task deleteTask(String key, boolean delete) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //Устанавливаем значение пометки на удаление
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_DELETED,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(delete)));
        //Запрос на изменение
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TASK)
                .appendKeySegment("guid'"+key+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.deleteTask(guid'"+key+"', "+delete+
                        ") Error on deleting of task. "+e.getMessage());
            }
            return new Tasks.Task();
        }
        return parseShortTask(response.getBody(), true, false); //Помечен в списке, свернут
    }

    //ВСЕ ДЛЯ ВИДОВ АУДИТА
    //возвращает критерий достижения целей по виду аудита
    private AType.Criterions parseATypeCriterion(String key) {
        for (AType.Criterions criterion: AType.Criterions.values())
            if (criterion.id.equals(key)) return criterion;
        return null;
    }

    //возвращает вид отбора аналитик по объекту
    private AType.Selections parseATypeSelection(String key) {
        for (AType.Selections selection: AType.Selections.values())
            if (selection.id.equals(key)) return selection;
        return null;
    }

    //возвращает вид аудита
    private AType parseType(ClientEntity entity) {
        AType type = null;
        if (entity != null) {
            type = new AType();
            type.id = entity.getProperty(COMMON_KEY).getPrimitiveValue().toString();;
            type.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
            type.code = entity.getProperty(COMMON_CODE).getPrimitiveValue().toString();
            type.pater = entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString();
            type.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
            type.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
            type.predefined = (boolean) entity.getProperty(COMMON_PREDEFINED).getPrimitiveValue().toValue();
            type.prenamed = entity.getProperty(COMMON_PRENAMED).getPrimitiveValue().toString();
            type.criterion = parseATypeCriterion(entity.getProperty(TYPE_CRITERION).getPrimitiveValue().toString());
            type.value = Float.valueOf(entity.getProperty(TYPE_VALUE).getPrimitiveValue().toString());
            type.fillActualValue = (boolean) entity.getProperty(TYPE_FILL_ACTUAL_VALUE).getPrimitiveValue().toValue();
            type.openWithIndicators = (boolean) entity.getProperty(TYPE_OPEN_WITH_INDICATORS).getPrimitiveValue().toValue();
            type.clearCopy = (boolean) entity.getProperty(TYPE_CLEAR_COPY).getPrimitiveValue().toValue();
            type.showSubject = (boolean) entity.getProperty(TYPE_SHOW_SUBJECT).getPrimitiveValue().toValue();
            type.selection = parseATypeSelection(entity.getProperty(TYPE_SELECTION).getPrimitiveValue().toString());
        }
        return type;
    }

    /**
     * Получить вид аудита
     * @param key guid вида аудита
     * @return - вид аудита
     */
    public AType getType(String key) {
        AType type = null;
        try {
            type = parseType(getFullEntity(ENTITY_SET_TYPE, key));
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getType() Error on requesting of type." + e.getMessage());
            }
        }
        return type;
    }

    /**
     * Изменение вида аудита.
     * @param type - измененний вид аудита
     */
    public void updateType(AType type) {
        //Создаем entity
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //новые значения:
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.name)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_CRITERION,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.criterion.id)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_VALUE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildSingle(type.value)));
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
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TYPE)
                .appendKeySegment("guid'"+type.id+"'")
                .build();
        final ODataEntityUpdateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityUpdateRequest(entityURI, UpdateType.PATCH, entity).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
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
    public void createType(AType type) {
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TYPE)
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        //новые значения:
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.name)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_CRITERION,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.criterion.id)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(TYPE_VALUE,
                client.getObjectFactory().newPrimitiveValueBuilder().buildSingle(type.value)));
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
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_FOLDER,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.folder)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PARENT,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(type.pater)));
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_PREDEFINED,
                client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(type.predefined)));
        ODataEntityCreateResponse<ClientEntity> response;
        try {
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.createType('"+type.name+
                        "') Error on create of type. "+e.getMessage());
            }
        }
    }

    //ВСЕ ДЛЯ СПРАВОЧНИКОВ
    //возвращает все пункты указанного родителя с отбором по наименованию
    private List<ClientEntity> getAllItems(String table, int hierarchy, String owner, String pater, String like) {
        StringBuilder filter = new StringBuilder();
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRoot);
        switch (hierarchy) {
            case FOLDER_HIERARCHY:
                uriBuilder.select(COMMON_FOLDER, COMMON_KEY, COMMON_DELETED, COMMON_NAME,
                                COMMON_PARENT, COMMON_PREDEFINED, COMMON_PRENAMED);
                filter.append(COMMON_PARENT).append(" eq guid'").append(pater).append("'");
                if (!(like==null||like.isEmpty()))
                    filter.append("and(substringof('").append(like).append("',").append(COMMON_NAME).append(")or ").append(COMMON_FOLDER).append(")");
                break;
            case ELEMENT_HIERARCHY:
                uriBuilder.select(COMMON_GROUP, COMMON_KEY, COMMON_DELETED, COMMON_NAME,
                                COMMON_PARENT, COMMON_PREDEFINED, COMMON_PRENAMED);
                filter.append(COMMON_PARENT).append(" eq guid'").append(pater).append("'");
                if (!(like==null||like.isEmpty()))
                    filter.append("and(substringof('").append(like).append("',").append(COMMON_NAME).append(")or ").append(COMMON_GROUP).append(")");
                break;
            case NOT_HIERARCHY: default:
                uriBuilder.select(COMMON_KEY, COMMON_DELETED, COMMON_NAME,
                                COMMON_PARENT, COMMON_PREDEFINED, COMMON_PRENAMED);
                if (!(like==null||like.isEmpty()))
                    filter.append("substringof('").append(like).append("',").append(COMMON_NAME).append(")");
        }
        //Добавляем отбор по владельцу
        if (!(owner==null||owner.isEmpty())) {
            if (filter.length()>0) filter.append(" and ");
            filter.append(COMMON_OWNER).append(" eq guid'").append(owner).append("'");
        }
        final URI entitySetURI = uriBuilder
                .appendEntitySetSegment(table)
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
    private ClientEntity getShortItem(String table, int hierarchy, String key) {
        final URIBuilder uriBuilder = client.newURIBuilder(serviceRoot);
        switch (hierarchy) {
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
                .appendEntitySetSegment(table)
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
     * @param hierarchy - вид иерархии справочника
     * @param owner - guid владелеца / null
     * @param pater - guid нового родителя
     * @param like - строка для отбора по наименованию / null
     * @return - список пунтов
     */
    public Items getItems(String table, int hierarchy, String owner, String pater, String like) {
        Items items = new Items();
        try {
            for (ClientEntity entity: getAllItems(table, hierarchy, owner, pater, like))
                items.add(parseItem(entity, hierarchy,false, false));
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getItems() Error on requesting of items." + e.getMessage());
            }
        }
        return items;
    }

    /**
     * Извлечение списка элементов справочника
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param owner - guid владелеца / null
     * @param pater - guid нового родителя
     * @param like - строка для отбора по наименованию / null
     * @param in - список guid папок первого уровня для отбора
     * @return - список пунтов
     */
    public Items getItems(String table, int hierarchy, String owner, String pater, String like, List<String> in) {
        Items items = new Items();
        try {
            if (!(in == null || in.isEmpty()))
                for (ClientEntity entity: getAllItems(table, hierarchy, owner, pater, like))
                    if (in.contains(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString()))
                        items.add(parseItem(entity, hierarchy, false, false));
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getItems() Error on requesting of items." + e.getMessage());
            }
        }
        return items;
    }

    /**
     * Извлечение элемента справочника
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param key - guid пункта
     * @return - пункт справочника
     */
    public Items.Item getItem(String table, int hierarchy, String key) {
        Items.Item item = new Items.Item();
        try {
            item = parseItem(getShortItem(table, hierarchy, key), hierarchy, false, false);
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getItem() Error on requesting of item." + e.getMessage());
            }
        }
        return item;
    }

    /**
     * Копирование пункта справочника в эту же или другую папку
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param key - guid пункта
     * @param pater - guid нового родителя
     * @return - копию пункта
     */
    public Items.Item copyItem(String table, int hierarchy, String key, String pater) {
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
            final URI entityURI = client.newURIBuilder(serviceRoot)
                    .appendEntitySetSegment(table)
                    .build();
            response = client.getCUDRequestFactory().getEntityCreateRequest(entityURI, entity).execute();
        } catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.copyItem('"+table+"', "+hierarchy+", guid'"+key+"', guid'"+pater+
                        "') Error on copy of item. "+e.getMessage());
            }
            return new Items.Item();
        }
        return parseItem(response.getBody(), hierarchy,false, false); //Новый пункт будет в списке свернут и не отмечен
    }

    /**
     * Перемещение пункта справочника в эту же или другую папку - изменение родителя
     * Контроль зацикливания ссылок осуществляется в ReferenceChoice!!!
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param key - guid пункта
     * @param pater - guid нового родителя
     * @return - измененный пукнт / null, если была попытка переместить папку, когда not_folders == true
     */
    public Items.Item moveItem(String table, int hierarchy, String key, String pater) {
        if (hierarchy==NOT_HIERARCHY) { //Если набор не является иерархией!!!
            throw new RuntimeException("AuditOData.moveItem('"+table+"', "+hierarchy+", guid'"+key+"', guid'"+pater+"') Entity set '"+table+"' is not the hierarchy.");
        }
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(table)
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
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.moveItem('"+table+"', "+hierarchy+", guid'"+key+"', guid'"+pater+
                        "') Error on move of item. "+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), hierarchy, true, false); //Помечен в списке, свернут
    }

    /**
     * Изменение отметки на удаление у пункта справочнка
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param key - guid пункта справочника
     * @param delete - значение пометки на удаление
     * @return - измененный пункт
     */
    public Items.Item deleteItem(String table, int hierarchy, String key, boolean delete) {
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(table)
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
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.deleteItem('"+table+"', "+hierarchy+", guid'"+key+"', "+delete+
                        ") Error on delete of item ."+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), hierarchy, true, false); //Помечен в списке, свернут
    }

    /**
     * Изменение пункта справочника (только наименование). Подходит для изменения наименования группы
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param item - объект пункт справочника, с новым наименованием
     * @return - измененный пункт
     */
    public Items.Item updateItem(String table, int hierarchy, Items.Item item) {
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(table)
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
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.updateItem('"+table+"', "+hierarchy+", '"+item.toString()+
                        "') Error on update of item. "+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), hierarchy, true, false); //Помечен в списке, свернут
    }

    /**
     * Создание пункта справочника (только наименование и родитель)
     * Подходит для создания группы
     * @param table - имя сущности
     * @param hierarchy - вид иерархии справочника
     * @param pater - guid нового родителя
     * @param name - наименование группы
     * @return - созданный пункт
     */
    public Items.Item createItem(String table, int hierarchy, String pater, String name) {
        final URI entityURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(table)
                .build();
        final ClientEntity entity = client.getObjectFactory().newEntity(null);
        entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_NAME,
                client.getObjectFactory().newPrimitiveValueBuilder().buildString(name)));
        //Устанавливаем признак группы
        switch (hierarchy) {
            case FOLDER_HIERARCHY:
                entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_FOLDER,
                        client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(true)));
                break;
            case ELEMENT_HIERARCHY:
                entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty(COMMON_GROUP,
                        client.getObjectFactory().newPrimitiveValueBuilder().buildBoolean(true)));
                break;
            case NOT_HIERARCHY: default:
                throw new RuntimeException("AuditOData.createItem('"+table+"', "+hierarchy+", '"+pater+"', '"+name+
                        "') Error on create of item group. The '"+table+"' set do not have hierarchy.");
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
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.createItem('"+table+"', "+hierarchy+", '"+pater+"', '"+name+
                        "') Error on create of item group ."+e.getMessage());
            }
            return null;
        }
        return parseItem(response.getBody(), hierarchy, false, false); //Не помечен в списке, свернут
    }

    //ВСЕ ДЛЯ ТАБЛИЧНОЙ ЧАСТИ ЗАДАНИЯ "АНАЛИТИКА ОБЪЕКТА"
   /**
     * Получить аналитику задания
     * @param task - giud задания
     * @return - возвращает список entity
     */
    private List<ClientEntity> getAllTaskAnalytics(String task) {
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TASK_ANALYTIC)
                .filter(COMMON_KEY+" eq guid'"+task+"'")
                .orderBy(COMMON_LINE+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet;
        try {
            entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        } catch (ODataRuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("AuditOData.getAllTaskAnalytics('"+task+
                    "'): Error on execute request of task analytics. "+e.getMessage());
        }
        return entitySet.getBody().getEntities();
    }

    /**
     * Разбор entity с аналитикой
     * @param entity - со ссылкой на аналитику обхекта
     * @return - возвращает пункт с аналитикой для рециклервью
     */
    private Items.Item parseTaskAnalytic(ClientEntity entity) {
        Items.Item item = null;
        if (entity!=null) {
            item = new Items.Item();
            item.id = getKey(entity.getProperty(TASK_ANALYTIC_VALUE).getPrimitiveValue().toString());
            item.name = getName(ENTITY_SET_ANALYTIC, item.id);
        }
        return item;
    }

    /**
     * Получить аналитику задания
     * @param task - guid задания
     * @return - список пунктов с аналитикой для рециклервью
     */
    public Items getTaskAnalytics(String task) {
        Items items = new Items();
        for (ClientEntity clientEntity: getAllTaskAnalytics(task)) {
            items.add(parseTaskAnalytic(clientEntity));
        }
        return items;
    }

    //ВСЕ ДЛЯ НОРМАТИВОВ ПОКАЗАТЕЛЕЙ АУДИТА
    //Возвращает список entity с нормативными значениями
    private List<ClientEntity> getAllIndicatorStandard(String type, String object) {
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_INDICATOR_STANDARD)
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
            row.goal = getValue(entity.getProperty(COMMON_GOAL), entity.getProperty(COMMON_GOAL+_TYPE));
            row.minimum = getValue(entity.getProperty(COMMON_MINIMUM), entity.getProperty(COMMON_MINIMUM+_TYPE));
            row.maximum = getValue(entity.getProperty(COMMON_MAXIMUM), entity.getProperty(COMMON_MAXIMUM+_TYPE));
            row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
        }
        return row;
    }

    /**
     *
     * @param type - guid вида аудита
     * @param object - guid объекта аудита
     * @return - список нормативов показателей
     */
    public Map<String, Tasks.Task.IndicatorRow> getIndicatorStandard(String type, String object) {
        final Map<String, Tasks.Task.IndicatorRow> list = new HashMap<>();
        try {
            for (ClientEntity entity : getAllIndicatorStandard(type, object)) {
                Tasks.Task.IndicatorRow row = parseIndicatorStandard(entity);
                list.put(row.indicator, row);
            }
        }
        catch(ODataRuntimeException e) {
                if (!sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.getTaskIndicators() Error on requesting of indicator standard. " + e.getMessage());
                }
            }
        return list;
    }

    //ВСЕ ДЛЯ ТАБЛИЧНОЙ ЧАСТИ ЗАДАНИЯ "ПОКАЗАТЕЛИ"
    //Возвращает список entity показателей задания
    private List<ClientEntity> getAllTaskIndicators(String task) {
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_TASK_INDICATOR)
                .filter(COMMON_KEY+" eq guid'"+task+"'")
                .orderBy(COMMON_LINE+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    //Возвращает строку таблицы с показателем
    private Tasks.Task.IndicatorRow parseTaskIndicator(ClientEntity entity) {
        final Tasks.Task.IndicatorRow row = new Tasks.Task(). new IndicatorRow();
        if (entity != null) {
            row.indicator = getKey(entity.getProperty(TASK_INDICATOR_KEY).getPrimitiveValue().toString());
            row.goal = getValue(entity.getProperty(COMMON_GOAL), entity.getProperty(COMMON_GOAL+_TYPE));
            row.minimum = getValue(entity.getProperty(COMMON_MINIMUM), entity.getProperty(COMMON_MINIMUM+_TYPE));
            row.maximum = getValue(entity.getProperty(COMMON_MAXIMUM), entity.getProperty(COMMON_MAXIMUM+_TYPE));
            row.value = getValue(entity.getProperty(TASK_INDICATOR_VALUE), entity.getProperty(TASK_INDICATOR_VALUE+_TYPE));
            row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
            row.comment = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
            row.achived = (boolean) entity.getProperty(TASK_INDICATOR_ACHIEVED).getPrimitiveValue().toValue();
        }
        return row;
    }

    /**
     * Подучить показатели задания
     * @param task guid задания
     * @return - список строк показателей
     */
    public List<Tasks.Task.IndicatorRow> getTaskIndicators(String task) {
        final List<Tasks.Task.IndicatorRow> list = new ArrayList<>();
        try {
            for (ClientEntity clientEntity: getAllTaskIndicators(task))
                list.add(parseTaskIndicator(clientEntity));
        }
        catch(ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getTaskIndicators() Error on requesting of task indicators. " + e.getMessage());
            }
        }
        return list;
    }

    /**
     * Получить показатели с нормативными значениями
     * @param type - guid вида аудита
     * @param object - [guid объекта аудита]
     * @return - спосок показателей аудита для табличной части задания
     */
    public ArrayList<Tasks.Task.IndicatorRow> getStandardIndicators(String type, String object) {
        final ArrayList<Tasks.Task.IndicatorRow> rows = new ArrayList<Tasks.Task.IndicatorRow>();
        //заполняем показателями из справочника
        if (!isEmptyKey(type)) {
            for (ClientEntity entity: getAllIndicators(type))
                rows.add(parseIndicatorRow(entity));
            //уточняем значения из регистра нормативов
            if (!isEmptyKey(object)) {
                final Map<String, Tasks.Task.IndicatorRow> map = getIndicatorStandard(type, object);
                for (Tasks.Task.IndicatorRow row: rows)
                    if (map.containsKey(row.indicator)) {
                        final Tasks.Task.IndicatorRow standard = map.get(row.indicator);
                        row.goal = standard.goal;
                        row.minimum = standard.minimum;
                        row.maximum = standard.maximum;
                        row.error = standard.error;
                    }
            }
        }
        return rows;
    }

    //ВСЕ ДЛЯ СПРАВОЧНИКА ПОКАЗАТЕЛЕЙ АУДИТА
    //Возвращает показатель аудита по entity
    private Indicators.Indicator parseFullIndicator(ClientEntity entity) {
        final Indicators.Indicator indicator = new Indicators(). new Indicator();;
        if (entity != null) {
            try {
                indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
                indicator.code = entity.getProperty(COMMON_CODE).getPrimitiveValue().toString();
                indicator.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
                indicator.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
                indicator.owner = getKey(entity.getProperty(COMMON_OWNER).getPrimitiveValue().toString());
                indicator.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
                indicator.desc = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
                indicator.type = Indicators.Types.toValue(entity.getProperty(INDICATOR_TYPE).getPrimitiveValue().toString());
                indicator.subject = getKey(entity.getProperty(INDICATOR_SUBJECT).getPrimitiveValue().toString());
                indicator.criterion = Indicators.Criterions.toValue(entity.getProperty(INDICATOR_CRITERION).getPrimitiveValue().toString());
                indicator.unit = getKey(entity.getProperty(INDICATOR_UNIT).getPrimitiveValue().toString());
                indicator.goal = getValue(entity.getProperty(COMMON_GOAL), entity.getProperty(COMMON_GOAL+_TYPE));
                indicator.minimum = getValue(entity.getProperty(COMMON_MINIMUM), entity.getProperty(COMMON_MINIMUM+_TYPE));
                indicator.maximum = getValue(entity.getProperty(COMMON_MAXIMUM), entity.getProperty(COMMON_MAXIMUM+_TYPE));
                indicator.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
                indicator.deleted = (boolean) entity.getProperty(COMMON_DELETED).getPrimitiveValue().toValue();
                indicator.predefined = (boolean) entity.getProperty(COMMON_PREDEFINED).getPrimitiveValue().toValue();
                if (indicator.predefined)
                    indicator.prenamed = entity.getProperty(COMMON_PRENAMED).getPrimitiveValue().toString();
            }
            catch (ODataRuntimeException e) {
                if (!sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.parseFullIndicator() Error on parsing of indicator ." + e.getMessage());
                }
            }
        }
        return indicator;
    }

    //Возвращает строку таблицы показателей
    private Tasks.Task.IndicatorRow parseIndicatorRow(ClientEntity entity) {
        final Tasks.Task.IndicatorRow row = new Tasks.Task().new IndicatorRow();
        if (entity != null) {
            try {
                row.indicator = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
                row.type = Indicators.Types.toValue(entity.getProperty(INDICATOR_TYPE).getPrimitiveValue().toString());
                row.goal = getValue(entity.getProperty(COMMON_GOAL), entity.getProperty(COMMON_GOAL+_TYPE));
                row.minimum = getValue(entity.getProperty(COMMON_MINIMUM), entity.getProperty(COMMON_MINIMUM+_TYPE));
                row.maximum = getValue(entity.getProperty(COMMON_MAXIMUM), entity.getProperty(COMMON_MAXIMUM+_TYPE));
                row.error = Float.valueOf(entity.getProperty(COMMON_ERROR).getPrimitiveValue().toString());
                row.value = row.type.stringToValue(""); //Здесь можно присваивать факт норме!!!
            }
            catch (ODataRuntimeException e) {
                if (!sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.parseIndicatorRow() Error on parsing of indicator ." + e.getMessage());
                }
            }
        }
        return row;
    }

    //Возвращает показатель аудита по entity
    private Indicators.Indicator parseShortIndicator(ClientEntity entity) {
        final Indicators.Indicator indicator = new Indicators(). new Indicator();
        if (entity != null) {
            try {
                indicator.id = getKey(entity.getProperty(COMMON_KEY).getPrimitiveValue().toString());
                indicator.name = entity.getProperty(COMMON_NAME).getPrimitiveValue().toString();
                indicator.pater = getKey(entity.getProperty(COMMON_PARENT).getPrimitiveValue().toString());
                indicator.folder = (boolean) entity.getProperty(COMMON_FOLDER).getPrimitiveValue().toValue();
                if (!indicator.folder) {
                    String key;
                    indicator.desc = entity.getProperty(COMMON_COMMENT).getPrimitiveValue().toString();
                    indicator.type = Indicators.Types.toValue(entity.getProperty(INDICATOR_TYPE).getPrimitiveValue().toString());
                    //Вместо guid наименование!!!
                    indicator.subject = getName(ENTITY_SET_SUBJECT, getKey(entity.getProperty(INDICATOR_SUBJECT).getPrimitiveValue().toString()));
                    indicator.criterion = Indicators.Criterions.toValue(entity.getProperty(INDICATOR_CRITERION).getPrimitiveValue().toString());
                    //Вместо guid наименование!!!
                    indicator.unit = getName(ENTITY_SET_UNIT, getKey(entity.getProperty(INDICATOR_UNIT).getPrimitiveValue().toString()));
                }
            }
            catch (ODataRuntimeException e) {
                if (!sayErrorMessage(e)) {
                    e.printStackTrace();
                    throw new RuntimeException("AuditOData.parseShortIndicator() Error on requesting of indicator ." + e.getMessage());
                }
            }
        }
        return indicator;
    }

    /**
     * Получить показатель аудита
     * @param key - giud показателя
     * @return - показатель аудита
     */
    public Indicators.Indicator getIndicator(String key) {
        try {
            return parseFullIndicator(getFullEntity(ENTITY_SET_INDICATOR, key));
        }
        catch (ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getIndicator() Error on requesting of indicator. " + e.getMessage());
            }
        }
        return null;
    }

    //Возвращает список entity с показателями аудита
    private List<ClientEntity> getAllIndicators(String type) {
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_INDICATOR)
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

    //Возвращает список entity с показателями аудита
    private List<ClientEntity> getAllIndicators(String type, String pater, String subject) {
        final StringBuilder filter = new StringBuilder();
        filter.append(COMMON_OWNER).append(" eq guid'").append(type).append("'");
        filter.append(" and ").append(COMMON_PARENT).append(" eq guid'").append(pater).append("'");
//        filter.append(" and ").append(COMMON_DELETED).append(" eq false");
        if (!isEmptyKey(subject))
            filter.append(" and ").append(INDICATOR_SUBJECT).append(" eq guid'").append(subject).append("'");
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_INDICATOR)
                .filter(filter.toString())
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Получить показатели аудита
     * @param type - guid вида аудита
     * @param pater - guid родителя (папка)
     * @param subject - guid предмета аудита для отбора показателей / null
     * @return - список показателей аудита
     */
    public Indicators getIndicators(String type, String pater, String subject) {
        final Indicators indicators = new Indicators();
        try {
            for (ClientEntity clientEntity: getAllIndicators(type, pater, subject))
                indicators.add(parseShortIndicator(clientEntity));
        }
        catch(ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getIndicators(guid'"+type+"', guid'"+pater+"', guid'"+subject+
                        "') Error on requesting of indicators. "+e.getMessage());
            }
        }
        return indicators;
    }

    //Возвращает список entity с папками Показателей 1го уровня
    private List<ClientEntity> getAllIndicatorFolders(String type) {
        final URI entitySetURI = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment(ENTITY_SET_INDICATOR)
                .filter(COMMON_OWNER+" eq guid'"+type+"' and "+
                        COMMON_PARENT+" eq guid'"+EMPTY_KEY+"' and "+
                        COMMON_FOLDER+" eq true")
                .orderBy(COMMON_ORDER+ORDER_ASC)
                .build();
        final ODataRetrieveResponse<ClientEntitySet> entitySet = client.getRetrieveRequestFactory().getEntitySetRequest(entitySetURI).execute();
        return entitySet.getBody().getEntities();
    }

    /**
     * Получить все папки Показателей 1го уровня
     * @param type - guid вида аудита
     * @return - список пунктов с папками
     */
    public Items getIndicatorFolders(String type) {
        final Items items = new Items();
        try {
            for (ClientEntity clientEntity: getAllIndicatorFolders(type))
                items.add(parseItem(clientEntity, FOLDER_HIERARCHY, false, false));
        }
        catch(ODataRuntimeException e) {
            if (!sayErrorMessage(e)) {
                e.printStackTrace();
                throw new RuntimeException("AuditOData.getIndicatorFolders() Error on requesting of indicator folders. " + e.getMessage());
            }
        }
        return items;
    }
}
//Фома2018