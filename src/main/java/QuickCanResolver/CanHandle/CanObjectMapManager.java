package QuickCanResolver.CanHandle;

import QuickCanResolver.DBC.SignalIOService;
import QuickCanResolver.DBC.CanSignal;
import QuickCanResolver.DBC.CanDbc;
import QuickCanResolver.DBC.CanMessage;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanObjectMapManager {
    protected Map<String, CanDbc> dbcMap;
    protected Map<String, CanIOHandler> canIOMap;
    protected static CanObjectMapManager manager;


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
    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型，省略手动调用的步骤。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param dataModel 数据模型
     */
    public void bindDbc(Object dataModel) {
        Class<?> clazz = dataModel.getClass();
        // 获取类上的所有注解
        if (clazz.isAnnotationPresent(DbcBinding.class)) {
            // 拿到注解
            DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
            String dbcTag = dbcBinding.dbcTag();
            String dbcFilePath = dbcBinding.dbcPath();

            // 增加校验，避免反复创建。避免一个 dbc 绑定多个模型。
            if (dbcMap.containsKey(dbcTag)) {
                return;
            }

            // 生成 dbc ，并添加到map中。
            CanDbc mDbc = addDbcToMap(dbcTag,dbcFilePath);
            // 给 dbc 单独绑定数据模型
            mDbc.bindModel(dataModel);
            // 给 dbc 中的 CanSignal 绑定字段
            bindField(dataModel);

            System.out.println("DBC绑定成功，dbcTag = " + dbcTag + ", dbcFilePath = " + dbcFilePath);
        }
    }

    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * 拷贝的同时，也会重新绑定
     * @param canId canId
     * @param data8 8位数组
     * @param oldDataModel 旧的对象
     * @return 新的对象。
     */
    public  <T extends CanCopyable<T>> T createNewDataModel(int canId, byte[] data8, T oldDataModel) {
        // 拷贝一个新的对象。
        T newObj = oldDataModel.copyNew();
        // 然后重新绑定 Model，用于生成新的数据模型
        String dbcTag = reBindModel(newObj);
        // 再写入新数据
        getCanIo(dbcTag).update_B(canId,data8);
        return newObj;
    }
    /**
     * 不使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData。
     * @param oldDataModel 旧的对象
     * @return 新的对象。
     */
    public <T extends CanCopyable<T>> T createNewDataModel(T oldDataModel) {
        // 拷贝一个新的对象。
        T newObj = oldDataModel.copyNew();
        // 然后重新绑定 Model，用于生成新的数据模型
        reBindModel(newObj);
        return newObj;
    }

    /**
     * TODO 封装 CanIOHandler 的 update_B() 方法
     */
    public void update_B(int canId, byte[] data8){
        // 1. 找到 dbcTag,生成 CanIOHandler
        String dbcTag = findDbcTagFromMap(canId);
        CanIOHandler canIOHandler = getCanIo(dbcTag);
        canIOHandler.update_B(canId,data8);

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
    private String findDbcTagFromClass(Class<?> clazz){
        String dbcTag = null ;
        if (clazz.isAnnotationPresent(DbcBinding.class)) {
            // 拿到注解
            DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
            dbcTag = dbcBinding.dbcTag();
        }
        return dbcTag;
    }
    private String findDbcTagFromModel(Object model) {
        Class<?> clazz = model.getClass();
        return findDbcTagFromClass(clazz);
    }
    /**
     * 再次绑定数据模型，这一次只绑定模型，不绑定字段。<br>
     * 因为字段已经在首次绑定时绑定好了，并且是一对一绑定。故这次只需要绑定数据模型即可。<br>
     * 该方式设计时用于和 viewModel 的互动。动态生成一个新的对象。<br>
     * @param newDataModel 新的数据模型。
     * @return dbcTag
     */
    public String reBindModel(Object newDataModel) {
        String dbcTag = findDbcTagFromModel(newDataModel);
        dbcMap.get(dbcTag).bindModel(newDataModel); // 再次绑定数据模型
        return dbcTag;
    }
    /**
     * 同时绑定 '数据模型' 和 '字段'，到信号中 <br>
     * 该方法必须要在绑定了dbc文件之后使用（也就是 addDbcToMap() 方法）。否则会无法将数据模型绑定到DBC中。<br>
     * @param dataModel 数据模型
     * @deprecated 注意：该方法随时准备弃用，因为存在用户调用的时序问题，容易出错，建议直接使用 @DbcBinding 注解数据模型。<br>
     * 使用 manager.bindDbc(dataModel) 绑定注解好的数据模型 。<br>
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
     * 步骤也很简单，从已绑定的dbc中查询 CanSignal ,查找是否和字段的注解一致，一致则绑定到 dbc 中。一般只会调用一次。
     * @param dataModel 数据模型
     */
    private void bindField(Object dataModel){
        Class<?> dataModelClazz = dataModel.getClass();
        // 循环，查找所有字段
        for (Field field : dataModelClazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (! field.isAnnotationPresent(CanBinding.class)) {
                continue;
            }
            CanSignal signal = findSignalByBind(field);
            if (signal != null){
                signal.setField(field);
            }
            else {
                throw new RuntimeException("字段: '"+field.getName()+
                        "' 中 注解 的信号名称{"+field.getAnnotation(CanBinding.class).signalTag()+"} 在DBC中未找到相关信息。");
            }
        } //查找所有字段
    }

    /**
     * 从 dbcMap 中查询dbc ,并传入字段，查找该字段是否含有相关注解。<br>
     * 注意，该方法必须在 dbcMap 不为空使用，即初始化之后使用。<br>
     * 一般会在绑定DBC时，也就是初始化时被反复调用。
     * @param field 字段
     * @return 返回找到的CAN信号
     */
    private CanSignal findSignalByBind(Field field) {
        CanSignal signal = null;
        if (! field.isAnnotationPresent(CanBinding.class)) {
            return signal;
        }
        // 查找到 被注解的字段
        CanBinding canBinding = field.getAnnotation(CanBinding.class);
        String signalTag = canBinding.signalTag();
        int msgTag = canBinding.messageId(); // 该注解可有可无
        // 需要查找DBC中是否含有对应信号，有才加入 map 中。
        if (msgTag == CanBinding.Default){
            // 用户没有标记 msgTag ，只能通过signal查询，多一层循环。
            for (CanDbc canDbc : dbcMap.values() ){
                CanSignal temp1 = canDbc.getSignal(signalTag);
                if (temp1 != null){
                    signal = temp1;
                    break;
                }
            }
        }
        else {
            // 标记了 msgTag ,可快速查询
            for (CanDbc mDbc : dbcMap.values() ){
                CanSignal temp2 = mDbc.getSignal(signalTag,msgTag);
                if (temp2 != null){
                    signal = temp2;
                    break;
                }
            }
        }  // 需要查找DBC中是否含有对应信号，有才加入 map 中。
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
