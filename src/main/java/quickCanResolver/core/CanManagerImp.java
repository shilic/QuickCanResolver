package quickCanResolver.core;

import quickCanResolver.dbc.FieldChanger;
import quickCanResolver.dbc.CanSignal;
import quickCanResolver.dbc.CanDbc;
import quickCanResolver.dbc.CanMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanManagerImp implements CanManagerService {
    protected Map<String, CanDbc> dbcMap;
    protected Map<String, CanCoder> canCoderMap;
    protected static volatile CanManagerImp canManagerImp;
    protected Map<Class<?>,Object> modelMap ;


    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型，省略手动调用的步骤。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param clazz 数据模型Class
     */

    public <T extends CanCopyable<T>> T bind(Class<T> clazz) {
        // 获取类上的注解
        if (! clazz.isAnnotationPresent(DbcBinding.class)) {
            return null; // 没有则直接返回null
        }
        // 拿到注解
        DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
        // 现在可以一次性给一个数据模型绑定多个DBC
        DbcBinding.Dbc[] rawDbcArray = dbcBinding.value();
        // 循环，遍历多个DBC文件，绑定DBC
        for (DbcBinding.Dbc rawDbc : rawDbcArray) {
            String dbcTag = rawDbc.dbcTag();
            String dbcFilePath = rawDbc.dbcPath();
            // 增加校验，避免反复创建dbc。
            if (dbcMap.containsKey(dbcTag)) {
                continue;
            }
            // 生成 dbc ，并添加到map中。即初始化一个Dbc文件
            addDbcToMap(dbcTag,dbcFilePath);
            System.out.println("DBC绑定成功，dbcTag = " + dbcTag + ", dbcFilePath = " + dbcFilePath);
        } // 循环，遍历多个DBC文件
        T instance = createInstance(clazz);
        modelMap.putIfAbsent(clazz,instance);
        // 给 dbc 中的 CanSignal 绑定字段 ，以及模型
        bindModelAndField(clazz,instance);
        return instance; // 返回实例化之后的数据模型
    }
    public static <T> T createInstance(Class<T> clazz) {
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
    // 新增一个方法，返回绑定的模型。当多个模型和多个报文进行绑定时，接收一个报文可能返回多个数据模型。故需要单独做一个接收

    public <T extends CanCopyable<T>> T getModel(Class<T> clazz) {
        return (T) modelMap.get(clazz); // 如果没有查询到，那么会返回空
    }



    /**
     * 注册DBC文件，即初始化一个Dbc文件
     * @param dbcTag DBC标签
     * @param dbcFilePath DBC文件地址
     */
    public CanDbc addDbcToMap(String dbcTag, String dbcFilePath) {
        //生成一个 DBC
        CanDbc dbc = DbcParse.getDbcFromFile(dbcTag,dbcFilePath);
        dbcMap.put(dbcTag,dbc);
        return dbc;
    }

    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * @return 新的对象。
     */

    public  <T extends CanCopyable<T>> T createNewModel(Class<T> clazz) {
        T oldDataModel = getModel(clazz);
        // 拷贝一个新的对象。
        return oldDataModel.copyNew();
    }

    /**
     * 封装  deCode_B() 方法。接收数据，解码报文。 将接收到的CAN报文，解析后存入绑定好的数据模型中
     */

    public void deCode_B(int canId, byte[] data8) {
        // 根据 canId 确定要写入哪一个 DBC
        String dbcTag = findDbcTagByCanId(canId);
        // 根据 DbcTag 获取处理者
        CanCoder canCoder = getCanCoder(dbcTag);
        // 更新数据到 模型中
        canCoder.deCode_B(canId,data8);
    }

    /**
     * 封装  enCode_I() 方法。编码数据，发送报文。
     */

    public int[] enCode_I(int canId) {
        // 根据 canId 确定要写入哪一个 DBC
        String dbcTag = findDbcTagByCanId(canId);
        // 根据 DbcTag 获取处理者
        CanCoder canCoder = getCanCoder(dbcTag);
        return canCoder.enCode_I(canId);
    }





    /**
     * 根据canID查询dbcTag
     */
    private String findDbcTagByCanId(int canId) {
        String dbcTag = null;
        for (CanDbc dbc : dbcMap.values()){
            if (dbc.getIntMsgMap().containsKey(canId)){
                dbcTag = dbc.dbcTag ;
                break;
            }
        }
        return dbcTag;
    }
    private static String[] findDbcTagsFromClass(Class<?> clazz) {
        String[] dbcTags = null ;
        if (clazz.isAnnotationPresent(DbcBinding.class)) {
            // 拿到注解
            DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
            DbcBinding.Dbc[] rawDbcArray = dbcBinding.value();
            int size = rawDbcArray.length;
            dbcTags = new String[size];
            for (int i = 0 ; i < size ; i++) {
                dbcTags[i] = rawDbcArray[i].dbcTag();
            }
        }
        return dbcTags;
    }
    @SuppressWarnings("unused")
    private static String[] findDbcTagsFromModel(Object model) {
        Class<?> clazz = model.getClass();
        return findDbcTagsFromClass(clazz);
    }

    /**
     * 相比于上面的方法，该方法只绑定字段，不绑定数据模型。<br>
     * 该方法必须要在绑定了dbc文件之后使用（也就是 addDbcToMap() 方法）。否则会无法绑定。<br>
     * 步骤也很简单，从已绑定的dbc中查询 CanSignal ,查找是否和字段的注解一致，一致则绑定到 dbc 中。一般在初始化时使用，只会调用一次。
     * @param dataModelClass 数据模型的类
     */
    public void bindModelAndField(Class<?> dataModelClass, Object model){
        // 循环，查找所有字段
        for (Field field : dataModelClass.getDeclaredFields()) {
            // 设置setAccessible为true，绕过访问控制检查
            field.setAccessible(true);
            // 不含有 CanBinding 注解，则忽略，执行下一次循环。
            if (! field.isAnnotationPresent(CanBinding.class)) {
                continue;
            }
            // 故以下代码都默认字段包含了 CanBinding 注解。
            CanSignal signal = findSignalByBind(field);
            // 这里的意思就是在包含 CanBinding 注解的情况下，找到了相关信号。
            /*PS:代码大量的使用了卫语句来减少if-else的嵌套。用人话来讲就是，如果一个if条件不满足需要返回或者退出，
            你就不需要继续使用else来嵌套的写满足后的逻辑了。
            你可以直接在if语句中 return ,break,continue 或者抛出异常。这样子代码更优雅，可读性更好。
            * */
            if (signal == null){
                // 这里抛出异常的意思就是，在包含 CanBinding 注解的情况下，绑定的信息有误，字段绑定的信号在dbc中没有找到，DBC实际上不含这个信号。
                throw new RuntimeException("字段: '"+field.getName()+
                        "' 绑定信息有误; 实际上未在DBC中未找到你想要绑定的信号{" + field.getAnnotation(CanBinding.class).signalTag() + "} 。");
            }
            // 绑定字段
            signal.setField(field);
            // 绑定模型
            signal.setDataModel(model);
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
     * 获取某一个DBC的CAN解编码器
     * @param dbcTag DBC的标签
     * @return 返回一个CAN收发器
     */
    public CanCoder getCanCoder(String dbcTag) {
        CanCoder canCoder = canCoderMap.get(dbcTag); // 如果查到了并且不为空，则直接返回
        if (canCoder == null) { // 为空则重新创建，并加入到表中
            CanDbc dbc = dbcMap.get(dbcTag);
            canCoder = new CanCoder(dbc);
            canCoderMap.put(dbcTag, canCoder);
        }
        return canCoder;
    }

    /**
     * 使用单例模式获取一个 “CAN对象映射管理器”
     * @return “CAN对象映射管理器”
     */
    public static CanManagerImp getInstance() {
        if (canManagerImp == null){
            synchronized (CanManagerImp.class){
                if (canManagerImp == null){
                    return canManagerImp = new CanManagerImp();
                }
            }
        }
        return canManagerImp;
    }
    private CanManagerImp() {
        dbcMap = new ConcurrentHashMap<>();
        canCoderMap = new ConcurrentHashMap<>();
        modelMap = new ConcurrentHashMap<>();
        System.out.println("Manager：数据管理层 Manager 初始化完成 ");
    }

    /**
     * 取消注册DBC
     * @param dbcTag  对应的DBC标签
     */

    public void clearDBC(String dbcTag){
        dbcMap.remove(dbcTag);
        canCoderMap.remove(dbcTag);
    }

    /**
     * 取消所有DBC的注册
     */

    public void clearAllDbc(){
        dbcMap.clear();
        canCoderMap.clear();
    }
    /**
     * 清理所有注册项
     */

    public void clear() {
        clearAllDbc();
        modelMap.clear();
        System.out.println("清理完成所有绑定关系");
    }






    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * @param canId canId
     * @param data8 8位数组
     * @param oldDataModel 旧的对象
     * @return 新的对象。
     */
    @Deprecated
    public  <T extends CanCopyable<T>> T createNewModel(int canId, byte[] data8, T oldDataModel) {
        // 根据 canId 确定要写入哪一个 DBC
        String dbcTag = findDbcTagByCanId(canId);
        // 根据 DbcTag 获取处理者
        CanCoder canCoder = getCanCoder(dbcTag);
        // 更新数据到 模型中
        canCoder.updateObj_B(canId,data8,oldDataModel);

        // 拷贝一个新的对象。
        return oldDataModel.copyNew();
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
            signal.currentValue = FieldChanger.getFieldValue(field,model); // 弃用
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
            FieldChanger.setFieldValue(field, model, sigValue);
        });
    } // 更新DBC的数据至数据模型中
}
