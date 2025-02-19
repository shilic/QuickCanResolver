package QuickCanResolver.CanHandle;

import QuickCanResolver.DBC.SignalIOService;
import QuickCanResolver.DBC.CanSignal;
import QuickCanResolver.DBC.CanDbc;
import QuickCanResolver.DBC.CanMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanObjectMapManager {
    protected Map<String, CanDbc> dbcMap;
    protected Map<String, CanIOHandler> canIOMap;
    protected static CanObjectMapManager manager;



    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型，省略手动调用的步骤。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param clazz 数据模型Class
     */
    public <T> T bind(Class<T> clazz) {
        T instance = creatInstance(clazz);
        // 获取类上的注解
        if (! clazz.isAnnotationPresent(DbcBinding.class)) {
            return null; // 没有则直接返回null
        }
        // 拿到注解
        DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
        // 现在可以一次性给一个数据模型绑定多个DBC
        DbcBinding.Dbc[] rawDbcArray = dbcBinding.value();
        // 循环，遍历多个DBC文件
        for (DbcBinding.Dbc rawDbc : rawDbcArray){
            String dbcTag = rawDbc.dbcTag();
            String dbcFilePath = rawDbc.dbcPath();
            // 增加校验，避免反复创建dbc。
            if (dbcMap.containsKey(dbcTag)) {
                continue;
            }
            // 生成 dbc ，并添加到map中。即初始化一个Dbc文件
            addDbcToMap(dbcTag,dbcFilePath);
            // 给 dbc 中的 CanSignal 绑定字段
            bindField(clazz);
            System.out.println("DBC绑定成功，dbcTag = " + dbcTag + ", dbcFilePath = " + dbcFilePath);
        } // 循环，遍历多个DBC文件

        // 给 dbc 单独绑定数据模型，由于已经绑定了字段，故只需要到dbcMap中查找即可
        // TODO 绑定 instance

        return instance; // 返回实例化之后的数据模型
    }
    public static <T> T creatInstance(Class<T> clazz) {
        try {
            // 获取无参构造函数
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            // 允许访问私有构造函数
            constructor.setAccessible(true);
            // 创建实例并返回
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("反射实例化数据模型时出错"+e.getMessage(), e);
        }
    }
    /**
     * 注册DBC文件，即初始化一个Dbc文件
     * @param dbcTag DBC标签
     * @param dbcFilePath DBC文件地址
     */
    public CanDbc addDbcToMap(String dbcTag, String dbcFilePath) {
        //生成一个 DBC
        CanDbc dbc = DbcHandle.getDbcFromFile(dbcTag,dbcFilePath);
        dbcMap.put(dbcTag,dbc);
        return dbc;
    }

    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * @param canId canId
     * @param data8 8位数组
     * @param oldDataModel 旧的对象
     * @return 新的对象。
     */
    public  <T extends CanCopyable<T>> T createNewDataModel(int canId, byte[] data8, T oldDataModel) {
        // 拷贝一个新的对象。
        T newObj = oldDataModel.copyNew();
        // 然后重新绑定 Model，用于生成新的数据模型 TODO

        // 再写入新数据 TODO

        return newObj;
    }

    /**
     * TODO 封装 CanIOHandler 的 update_B() 方法
     */
    public void update_B(int canId, byte[] data8){


    }

    /**
     * TODO 反向操作，封装 CanIOHandler 类的 fieldToCan_I() 方法
     */
    public int[] createCanFrameI(){


        return null;
    }

    /**
     * 根据canID查询dbcTag
     */
    private String findDbcTagFromMap(int canId) {
        String dbcTag = null;
        for (CanDbc dbc : dbcMap.values()){
            if (dbc.getIntMsgMap().containsKey(canId)){
                dbcTag = dbc.dbcTag ;
                break;
            }
        }
        return dbcTag;
    }
    private static String[] findDbcTagsFromClass(Class<?> clazz){
        String[] dbcTags = null ;
        if (clazz.isAnnotationPresent(DbcBinding.class)) {
            // 拿到注解
            DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
            DbcBinding.Dbc[] rawDbcArray = dbcBinding.value();
            int size = rawDbcArray.length;
            dbcTags = new String[size];
            for (int i = 0 ; i < size ; i++){
                dbcTags[i] = rawDbcArray[i].dbcTag();
            }
        }
        return dbcTags;
    }
    private static String[] findDbcTagsFromModel(Object model) {
        Class<?> clazz = model.getClass();
        return findDbcTagsFromClass(clazz);
    }
    /**
     * 绑定数据模型，这一次只绑定模型，不绑定字段。<br>
     * 该方式设计时可用于和 viewModel 的互动。动态生成一个新的对象。<br>
     * 故我单独把这一部分抽象了出来。<br>
     * @param dataModel 新的数据模型。
     */
    public void bindModel(Object dataModel) {




        // TODO
    }

    /**
     * 同时绑定 '数据模型' 和 '字段'，到信号中 <br>
     * 该方法必须要在绑定了dbc文件之后使用（也就是 addDbcToMap() 方法）。否则会无法将数据模型绑定到DBC中。<br>
     * @param dataModel 数据模型
     * @deprecated 注意：该方法随时准备弃用，因为存在用户调用的时序问题，容易出错，建议直接使用 @DbcBinding 注解数据模型。<br>
     * 使用 manager.bind() 绑定注解好的数据模型 。<br>
     * 然后使用 newObj = manager.createNewDataModel(id, data8, model); 生成一个新的对象。<br>
     * 又或者使用 update_B() 刷新数据。
     */
    @Deprecated
    public void bindDataModelAndField(Object dataModel) {
        Class<?> dataModelClazz = dataModel.getClass();
        // 循环，查找所有字段
        for (Field field : dataModelClazz.getDeclaredFields()) {
            /* 设置setAccessible为true，绕过访问控制检查*/
            field.setAccessible(true);
            // 查找到 没有被我注释的字段就执行下一次循环。使用“卫语句”减少 if-else 的嵌套次数。
            if (! field.isAnnotationPresent(CanBinding.class)) {
                continue;
            }
            CanSignal signal = findSignalByBind(field);

            if (signal != null){
                // 标记该信号属于哪一个数据模型
                signal.setDataModel(dataModel);
                //System.out.println("字段名:"+field.getName()+", 注解值: {"+signalTag+"} 和DBC匹配, 添加到map中。");
                signal.setField(field);
            }
            else {
                throw new RuntimeException("字段: '"+field.getName()+
                        "' 中 注解 的信号名称{"+field.getAnnotation(CanBinding.class).signalTag()+"} 在DBC中未找到相关信息。");
            }
        } //查找所有字段
    } // 从对象中获取字段

    /**
     * 相比于上面的方法，该方法只绑定字段，不绑定数据模型。<br>
     * 该方法必须要在绑定了dbc文件之后使用（也就是 addDbcToMap() 方法）。否则会无法绑定。<br>
     * 步骤也很简单，从已绑定的dbc中查询 CanSignal ,查找是否和字段的注解一致，一致则绑定到 dbc 中。一般在初始化时使用，只会调用一次。
     * @param dataModelClass 数据模型的类
     */
    private void bindField(Class<?> dataModelClass){
        // 循环，查找所有字段
        for (Field field : dataModelClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (! field.isAnnotationPresent(CanBinding.class)) {
                continue; // 不含有 CanBinding 注解，则忽略，执行下一次循环。
            }
            // 故以下代码都默认字段包含了 CanBinding 注解。
            CanSignal signal = findSignalByBind(field);
            // 这里的意思就是在包含 CanBinding 注解的情况下，找到了相关信号。
            if (signal != null){
                signal.setField(field); // 绑定字段
            }
            else {
                // 这里抛出异常的意思就是，在包含 CanBinding 注解的情况下，绑定的信息有误，字段绑定的信号在dbc中没有找到，DBC实际上不含这个信号。
                throw new RuntimeException("字段: '"+field.getName()+
                        "' 绑定信息有误; 实际上未在DBC中未找到你想要绑定的信号{" + field.getAnnotation(CanBinding.class).signalTag() + "} 。");
            }
        } //查找所有字段
    }

    /**
     * 从字段中解析相关注解 CanBinding ；<br>
     * 再从 dbcMap 中查询dbc ,查找该字段是否含有相关注解 CanBinding 。<br>
     * 注意，该方法必须在 dbcMap 不为空使用，即初始化之后使用。<br>
     * 一般会在绑定DBC时，也就是初始化时被反复调用。<br>
     * 实际调用SignalMap查询，如果未从 SignalMap 查找到，仍会返回null，表示未找到。<br>
     * @param field 要查找的字段
     * @return 返回找到的CAN信号
     */
    private CanSignal findSignalByBind(Field field) {
        CanSignal signal = null;
        if (! field.isAnnotationPresent(CanBinding.class)) {
            return signal;// 不含相关注解 CanBinding 直接返回空
        }
        // 查找到 被 CanBinding 注解的字段
        CanBinding canBinding = field.getAnnotation(CanBinding.class);
        String signalTag = canBinding.signalTag();
        int msgTag = canBinding.messageId();
        // 需要查找DBC中是否含有对应信号，有才加入 map 中。
        if (msgTag == CanBinding.Default){
            // 用户没有标记 msgTag ，只能通过signal查询，多一层循环。
            for (CanDbc canDbc : dbcMap.values() ){
                // 这里如果在map中没有查询到，仍然有可能返回一个null
                CanSignal temp1 = canDbc.getSignal(signalTag);
                // 不为空则跳出循环，因为已经找到了，前提是你的DBC的信号不会重复，当然，这也是DBC文件的规范。
                if (temp1 != null){
                    signal = temp1;
                    break;
                }
            }
        }
        else {
            // 标记了 msgTag ,可快速查询
            for (CanDbc canDbc : dbcMap.values() ){
                // 这里如果在map中没有查询到，仍然有可能返回一个null
                CanSignal temp2 = canDbc.getSignal(signalTag,msgTag);
                if (temp2 != null){
                    signal = temp2;
                    break;
                }
            }
        }  // 需要查找DBC中是否含有对应信号，有才加入 map 中。
        // 在上边的循环中，实际从 SignalMap 中查找相关信号，map没有查询到对应的key，当然会返回null，故这里仍可能返回null值的signal，表示未从 SignalMap 中查询到信号。
        return signal;
    }


    /**
     * 获取某一个DBC的IO组件
     * @param dbcTag DBC的标签
     * @return 返回一个CAN收发器
     */
    public CanIOHandler getCanIo(String dbcTag) {
        CanDbc dbc = dbcMap.get(dbcTag);
        CanIOHandler canIOHandler = canIOMap.get(dbcTag); // 如果查到了并且不为空，则直接返回
        if (canIOHandler == null) { // 为空则重新创建，并加入到表中
            canIOHandler = new CanIOHandler(dbc);
            canIOMap.put(dbcTag, canIOHandler);
        }
        return canIOHandler;
    }

    /**
     * 使用单例模式获取一个 “CAN对象映射管理器”
     * @return “CAN对象映射管理器”
     */
    public static CanObjectMapManager getInstance() {
        if (manager == null){
            synchronized (CanObjectMapManager.class){
                if (manager == null){
                    return manager = new CanObjectMapManager();
                }
            }
        }
        return manager;
    }
    private CanObjectMapManager() {
        dbcMap = new ConcurrentHashMap<>();
        canIOMap = new ConcurrentHashMap<>();

    }

    /**
     * 取消注册DBC
     * @param dbcTag  对应的DBC标签
     */
    public void clearDBC(String dbcTag){
        dbcMap.remove(dbcTag);
    }

    /**
     * 取消所有DBC的注册
     */
    public void clearAllDbc(){
        dbcMap.clear();
    }

    /**
     * 清理指定 canIO
     */
    public void clearCanIO(String dbcTag){
        canIOMap.remove(dbcTag);
    }

    /**
     * 清理所有 canIO
     */
    public void clearAllCanIO(){
        canIOMap.clear();
    }

    /**
     * 清理所有注册项
     */
    public void clear() {
        clearAllCanIO();
        clearAllDbc();
    }






    /**
     * 使用并发流，获取一个CAN消息
     * @deprecated 任务过小，并发流实际上会增加线程开销，导致负优化
     */
    @Deprecated
    private CanMessage getMsgFromDbcMap(int canId) {
        final CanMessage[] msgs = new CanMessage[1];
        msgs[0] = null ;
        dbcMap.values().parallelStream().filter(dbc -> dbc.getMsg(canId) != null).forEach(dbc -> msgs[0] = dbc.getMsg(canId));
        return msgs[0];
    }
    /**
     * 更新数据模型中的数据至DBC中
     * @param canId CanId
     * @deprecated 任务过小，并发流实际上会增加线程开销，导致负优化
     */
    @Deprecated
    public void updateModelToDbc(int canId) {
        /* 1.  找到交换数据的双方 。字段和DBC*/
        CanMessage msg = getMsgFromDbcMap(canId);
        if (msg == null){
            return;
        }
        Map<String, CanSignal> signalMap = msg.getSignalMap();
        signalMap.values().parallelStream().filter(CanSignal::isFieldBind).forEach(signal -> {
            Field field = signal.getField();/* 2. 拿到字段 */
            Object model = signal.getDataModel(); // 原始对象
            //double sigValue = signal.currentValue ; /* 3. 拿到DBC的数据 */
            /* 4. 将 字段中的数据刷写到DBC中 */
            signal.currentValue = SignalIOService.getFieldValue(field,model); // 弃用
        });
    } // updateModelToDbc

    /**
     * 更新DBC的数据至数据模型中
     * @param canId canId
     * @deprecated 任务过小，并发流实际上会增加线程开销，导致负优化
     */
    @Deprecated
    public void updateDbcToModel(int canId) {
        /* 1.  找到交换数据的双方 。字段和DBC*/
        CanMessage msg = getMsgFromDbcMap(canId);
        if (msg == null){
            return;
        }
        Map<String, CanSignal> signalMap = msg.getSignalMap();
        signalMap.values().parallelStream().filter(CanSignal::isFieldBind).forEach(signal -> {
            Field field = signal.getField();/* 2. 拿到字段 */
            Object model = signal.getDataModel(); // 原始对象
            double sigValue = signal.currentValue ; /* 3. 拿到DBC的数据(弃用) */
            /* 4. 将 DBC中数据 刷写道 字段中 */
            SignalIOService.setFieldValue(field, model, sigValue);
        });
    } // 更新DBC的数据至数据模型中
}
