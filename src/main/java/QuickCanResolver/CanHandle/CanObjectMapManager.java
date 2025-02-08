package QuickCanResolver.CanHandle;

import QuickCanResolver.CanTool.MyReflect;
import QuickCanResolver.DBC.CanDbc;
import QuickCanResolver.DBC.CanMessage;
import QuickCanResolver.DBC.CanSignal;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanObjectMapManager {
    Map<String, CanDbc> dbcMap;
    Map<String,CanIO> canIOMap;
    protected static volatile CanObjectMapManager manager;


    /**
     * 绑定数据模型，从对象中获取字段
     * @param dataModel 数据模型
     */
    public void registerData(Object dataModel) {
        Class<?> dataModelClazz = dataModel.getClass();
        //查找所有字段
        for (Field field : dataModelClazz.getDeclaredFields()) {
            field.setAccessible(true); /* 设置setAccessible为true，绕过访问控制检查*/
            if (! field.isAnnotationPresent(SignalTag.class)) { // 查找到 没有被我注释的字段就
               continue;
            }
            // 查找到 被注解的字段
            SignalTag annotation = field.getAnnotation(SignalTag.class); // 获取注解
            String signalTag = annotation.signalName();
            int msgTag = annotation.messageName(); // 该注解可有可无
            // 需要查找DBC中是否含有对应信号，有才加入 map 中。
            //final CanSignal[] signals = {null};
            CanSignal mSig = null;
            if (msgTag == SignalTag.Default){ // 用户没有标记 msgTag ，只能通过signal查询
                for (CanDbc mDbc : dbcMap.values() ){
                    CanSignal temp1 = mDbc.getSignal(signalTag);
                    if (temp1 != null){
                        mSig = temp1;
                        break;
                    }
                }
            }
            else { // 标记了 msgTag ,可快速查询
                for (CanDbc mDbc : dbcMap.values() ){
                    CanSignal temp2 = mDbc.getSignal(signalTag,msgTag);
                    if (temp2 != null){
                        mSig = temp2;
                        break;
                    }
                }
            }  // 需要查找DBC中是否含有对应信号，有才加入 map 中。

            if (mSig != null){ // signals[0] != null
                mSig.setTarget(dataModel); // 标记该信号属于哪一个数据模型
                //System.out.println("字段名:"+field.getName()+", 注解值: {"+signalTag+"} 和DBC匹配, 添加到map中。");
                mSig.setField(field);
            }
            else {
                throw new RuntimeException("字段: "+field.getName()+" 中注解的信号名称{"+signalTag+"} 在DBC中未找到相关信息。");
            }
        }//查找所有字段
    } // 从对象中获取字段


    /**
     * 获取某一个DBC的IO组件
     * @param dbcTag DBC的标签
     * @return 返回一个CAN收发器
     */
    public CanIO getCanIo(String dbcTag ) {
        CanDbc dbc = dbcMap.get(dbcTag);
        CanIO canIO = canIOMap.get(dbcTag); // 如果查到了并且不为空，则直接返回
        if (canIO == null) { // 为空则重新创建，并加入到表中
            canIO = new CanIO(dbc);
            canIOMap.put(dbcTag,canIO);
        }
        return canIO;
    }
    public static CanObjectMapManager getInstance(){
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
     * 注册DBC文件
     * @param dbcTag DBC标签
     * @param dbcFilePath DBC文件地址
     */
    public void registerDBC(String dbcTag, String dbcFilePath) {
        CanDbc dbc = DbcHandle.getDbcFromFile(dbcFilePath);
        dbcMap.put(dbcTag,dbc);
    }
    /**
     * 取消注册DBC
     * @param dbcTag  DBC标签
     */
    public void unRegisterDBC(String dbcTag){
        dbcMap.remove(dbcTag);
    }
    public void unRegisterDbcAll(){
        dbcMap.clear();
    }

    public void clearCanIO(String dbcTag){
        canIOMap.remove(dbcTag);
    }
    public void clearAllCanIO(){
        canIOMap.clear();
    }
    public void unRegisterAll(){
        clearAllCanIO();
        unRegisterDbcAll();
    }





    @Deprecated
    private CanMessage getMsgFromDbcMap(int canId){
        final CanMessage[] msgs = new CanMessage[1];
        msgs[0] = null ;
        dbcMap.values().parallelStream().filter(dbc -> dbc.getMsg(canId) != null).forEach(dbc -> msgs[0] = dbc.getMsg(canId));
        return msgs[0];
    }
    /**
     * 更新数据模型中的数据至DBC中
     * @param canId CanId
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
            Object model = signal.getTarget(); // 原始对象
            //double sigValue = signal.currentValue ; /* 3. 拿到DBC的数据 */
            /* 4. 将 字段中的数据刷写到DBC中 */
            try {
                signal.currentValue = MyReflect.getFieldValue(field,model);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    } // updateModelToDbc

    /**
     * 更新DBC的数据至数据模型中
     * @param canId canId
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
            Object model = signal.getTarget(); // 原始对象
            double sigValue = signal.currentValue ; /* 3. 拿到DBC的数据 */
            /* 4. 将 DBC中数据 刷写道 字段中 */
            try {
                MyReflect.setFieldValue(field, model, sigValue);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    } // 更新DBC的数据至数据模型中
}
