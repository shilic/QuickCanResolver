package quickCanResolver.core;

import quickCanResolver.dbc.CanSignal;
import quickCanResolver.dbc.CanDbc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CAN编解码器的管理器。实现了 dbc 绑定、报文编解码等方法。
 */
public class CanManagerImp implements CanManagerService {
    /** 绑定的dbc */
    protected Map<String, CanDbc> dbcMap;
    /** 编解码器 */
    protected Map<String, CanCoder> canCoderMap;
    private static volatile CanManagerImp canManagerImp;
    /** 绑定数据模型 */
    protected Map<Class<?>,Object> modelMap ;
    protected DbcInputInterface dbcInputInterface;


    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型，省略手动调用的步骤。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param clazz 定义好绑定关系的数据模型 的 Class
     * @param <T> 实现了拷贝接口的数据模型的类型
     * @return 返回绑定好的数据模型，如果绑定失败，返回null
     */
    @Override
    public <T extends CanCopyable<T>> T bind(Class<T> clazz) {
        // 查找注解
        boolean isAnno =  findAnnotation(clazz);
        if (! isAnno) {
            return null;
        }
        T instance = createInstance(clazz);
        modelMap.putIfAbsent(clazz,instance);
        // 给 dbc 中的 CanSignal 绑定字段 ，以及模型
        bindModelAndField(clazz,instance);
        System.out.println("Manager：绑定完成, class = " + clazz.getName());
        return instance; // 返回实例化之后的数据模型
    }
    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型，省略手动调用的步骤。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param model 定义好绑定关系的数据模型
     * @param <T> 实现了拷贝接口的数据模型的类型
     * @return  返回传入的对象，如果绑定失败，那么返回空
     */
    @Override
    public <T extends CanCopyable<T>> T bind(T model) {
        Class<T> clazz = (Class<T>) model.getClass();
        boolean isAnno =  findAnnotation(clazz);
        if (! isAnno) {
            return null;
        }
        //T instance = createInstance(clazz);
        modelMap.putIfAbsent(clazz,model);
        // 给 dbc 中的 CanSignal 绑定字段 ，以及模型
        bindModelAndField(clazz,model);
        System.out.println("Manager：绑定完成, class = " + clazz.getName());
        return model;
    }

    /**
     * 查找注解
     * @param clazz 定义好绑定关系的数据模型 的 Class
     * @param <T> 实现了拷贝接口的数据模型的类型
     * @return 返回是否绑定成功
     */
    private  <T extends CanCopyable<T>> boolean findAnnotation(Class<T> clazz) {
        // 获取类上的注解
        if (! clazz.isAnnotationPresent(DbcBinding.class)) {
            return false;
        }
        // 拿到注解
        DbcBinding dbcBinding = clazz.getAnnotation(DbcBinding.class);
        if(dbcBinding == null) {
            return false;
        }
        // 现在可以一次性给一个数据模型绑定多个DBC
        DbcBinding.Dbc[] rawDbcArray = dbcBinding.value();
        if (rawDbcArray.length == 0) {
            return false;
        }
        // 循环，遍历多个DBC文件，绑定DBC
        for (DbcBinding.Dbc rawDbc : rawDbcArray) {
            String dbcTag = rawDbc.dbcTag();
            String dbcFilePath = rawDbc.dbcPath();
            // 增加校验，避免反复创建dbc。
            if (dbcMap.containsKey(dbcTag)) {
                continue;
            }
            // 生成 dbc ，并添加到map中。即初始化一个Dbc文件 //addDbcToMap(dbcTag,dbcFilePath);
            addDbcToMap(dbcTag,dbcFilePath);
            System.out.println("Manager：DBC绑定成功，dbcTag = " + dbcTag + ", dbcFilePath = " + dbcFilePath);
        } // 循环，遍历多个DBC文件
        return true;
    }

    /**
     * 使用反射创建对象实例
     * @param clazz 定义好绑定关系的数据模型 的 Class
     * @return 返回实例
     * @param <T> 泛型
     */
    private static <T> T createInstance(Class<T> clazz) {
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

    /**
     * 获取绑定的初始对象
     * @param clazz 定义好绑定关系的数据模型 的 Class
     * @param <T> 对象需实现拷贝接口
     * @return 返回绑定的初始对象，如果没有查询到，可能返回空
     */
    @Override
    public <T extends CanCopyable<T>> T getModel(Class<T> clazz) {
        // 如果没有查询到，那么会返回空
        return (T) modelMap.get(clazz);
    }



    /**
     * 注册DBC文件，即初始化一个Dbc文件
     * @param dbcTag DBC标签
     * @param dbcFilePath DBC文件地址
     * @return 返回一个生成的  dbc
     */
    private CanDbc addDbcToMap(String dbcTag, String dbcFilePath) {
        // 如果没有定义接口，就直接从路径获取。
        if (dbcInputInterface == null) {
            CanDbc dbc = DbcParse.getDbcFromFilePath(dbcTag,dbcFilePath);
            dbcMap.put(dbcTag,dbc);
            return dbc;
        }
        // 如果定义了接口，就从接口获取 inputStream
        try ( InputStream inputStream = dbcInputInterface.getInputStream(dbcFilePath) ) {
            CanDbc dbc = DbcParse.getDbcFromInputStream(dbcTag, inputStream);
            dbcMap.put(dbcTag, dbc);
            return dbc;
        } catch (IOException e) {
            throw new RuntimeException("获取DBC文件发生错误，IO异常");
        }
    }

    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * @param clazz 定义好绑定关系的数据模型 的 Class
     * @param <T> 对象需实现拷贝接口
     * @return 新的对象。
     */
    @Override
    public  <T extends CanCopyable<T>> T createNewModel(Class<T> clazz) {
        T oldDataModel = getModel(clazz);
        // 拷贝一个新的对象。
        return oldDataModel.copyNew();
    }
    /**
     * 封装  deCode_B() 方法。接收数据，解码报文。 将接收到的CAN报文，解析后存入绑定好的数据模型中
     * @param canId  canId
     * @param data8 报文数组
     */
    @Override
    public void deCode_B(int canId, byte[] data8) {
        // 根据 canId 确定要写入哪一个 DBC
        String dbcTag = findDbcTagByCanId(canId);
        // 根据 DbcTag 获取处理者
        CanCoder canCoder = getCanCoder(dbcTag);
        // 更新数据到 模型中
        canCoder.deCode_B(canId,data8);
    }
    /**
     * 编码数据，发送报文。
     * @param canId canId
     * @return 返回解码后的报文
     */
    @Override
    public byte[] enCode_B(int canId) {
        // 根据 canId 确定要写入哪一个 DBC
        String dbcTag = findDbcTagByCanId(canId);
        // 根据 DbcTag 获取处理者
        CanCoder canCoder = getCanCoder(dbcTag);
        return canCoder.enCode_B(canId);
    }
    /**
     * 使用一个新的对象，编码数据，发送报文。
     * @param canId canId
     * @param model 使用一个数据模型用来生成报文，而不是之前的模型
     * @return 返回解码后的报文
     */
    @Override
    public  byte[] enCode_B(int canId,Object model) {
        // 根据 canId 确定要写入哪一个 DBC
        String dbcTag = findDbcTagByCanId(canId);
        // 根据 DbcTag 获取处理者
        CanCoder canCoder = getCanCoder(dbcTag);
        return canCoder.enCode_B(canId, model);
    }

    /**
     * 根据canID查询dbcTag
     * @param canId  canId
     * @return 返回查询结果
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

    /**
     * 根据类型查找dbc
     * @param clazz 定义好绑定关系的数据模型 的 Class
     * @return 返回找到的 dbcTag
     */
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

    /**
     * 根据数据模型查找dbc
     * @param model 定义好绑定关系的数据模型
     * @return 返回绑定的dbc
     */

    private static String[] findDbcTagsFromModel(Object model) {
        Class<?> clazz = model.getClass();
        return findDbcTagsFromClass(clazz);
    }

    /**
     * 绑定字段和数据模型 <br>
     * 该方法必须要在绑定了dbc文件之后使用（也就是 addDbcToMap() 方法）。否则会无法绑定。<br>
     * 步骤也很简单，从已绑定的dbc中查询 CanSignal ,查找是否和字段的注解一致，一致则绑定到 dbc 中。一般在初始化时使用，只会调用一次。
     * @param dataModelClass 定义好绑定关系的数据模型 的 Class
     * @param model 定义好绑定关系的数据模型
     */
    private void bindModelAndField(Class<?> dataModelClass, Object model) {
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
    private CanCoder getCanCoder(String dbcTag) {
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
     * 添加DBC输入接口
     * @param dbcInputInterface  DBC输入接口
     * @return 返回自身，便于链式调用
     */
    @Override
    public CanManagerService addDbcInputInterface(DbcInputInterface dbcInputInterface) {
        this.dbcInputInterface = dbcInputInterface;
        System.out.println("Manager：DBC输入接口 DbcInputInterface 绑定完成");
        return this;
    }
    /**
     * 取消注册DBC
     * @param dbcTag  对应的DBC标签
     */
    @Override
    public void clearDBC(String dbcTag){
        dbcMap.remove(dbcTag);
        canCoderMap.remove(dbcTag);
        System.out.println("Manager：清理完成DBC："+dbcTag);
    }

    /**
     * 取消所有DBC的注册
     */
    @Override
    public void clearAllDbc(){
        dbcMap.clear();
        canCoderMap.clear();
        System.out.println("Manager：清理完成所有DBC");
    }
    /**
     * 清理所有注册项
     */
    @Override
    public void clear() {
        clearAllDbc();
        modelMap.clear();
        System.out.println("Manager：清理完成所有绑定关系");
    }

}
