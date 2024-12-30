package QuickCanResolver.CanHandle;

import QuickCanResolver.DBC.CanChannel;
import QuickCanResolver.DBC.CanMessage;
import QuickCanResolver.DBC.CanSignal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CanObjectMapManager {
    Map<String,CanChannel> dbcMap = new HashMap<>();
    Map<String,CanIO> canIOMap = new HashMap<>();
    protected static CanObjectMapManager manager;
    /**
     * 从对象中获取字段
     * @param dataModel 数据模型的对象
     */
    private void getAnnotatedFields(Object dataModel) {
        Class<?> dataModelClazz = dataModel.getClass();
        //查找所有字段
        for (Field field : dataModelClazz.getDeclaredFields()) {
            field.setAccessible(true); /* 设置setAccessible为true，绕过访问控制检查*/
            if (field.isAnnotationPresent(SignalTag.class)) { // 查找注解
                SignalTag annotation = field.getAnnotation(SignalTag.class); // 获取注解
                String signalTag = annotation.signalName(); // Field: fanGearReq has annotation with value: CCSToCabin1_FanGearReq
                int msgTag = annotation.messageName(); // 该注解可有可无
                // 需要查找DBC中是否含有对应信号，有才加入 map 中。
                final CanSignal[] signals = {null};
                if (msgTag == SignalTag.Default){ // 用户没有标记报文ID，只能通过signal查询
//                    for (Map.Entry<String,CanChannel> entry:dbcMap.entrySet()) {
//                        CanChannel dbc = entry.getValue();
//                        signal = dbc.getSignal(signalTag);
//                        if (signal != null){
//                            break;
//                        }
//                    }
                    dbcMap.values().parallelStream().forEach(dbc -> {
                        if (dbc.getSignal(signalTag) != null){
                            signals[0] = dbc.getSignal(signalTag);
                        }
                    });
                }
                else { // 标记了id,可快速查询
//                    for (Map.Entry<String,CanChannel> entry:dbcMap.entrySet()) {
//                        CanChannel dbc = entry.getValue();
//                        signal = dbc.getSignal(signalTag,msgTag);
//                        if (signal != null){
//                            break;
//                        }
//                    }
                    dbcMap.values().parallelStream().forEach(dbc -> {
                        if (dbc.getSignal(signalTag,msgTag) != null){
                            signals[0] = dbc.getSignal(signalTag,msgTag);
                        }
                    });
                }  // 需要查找DBC中是否含有对应信号，有才加入 map 中。
                if (signals[0] != null){
                    signals[0].setTarget(dataModel); // 标记该信号属于哪一个数据模型
                    //System.out.println("字段名:"+field.getName()+", 注解值: {"+signalTag+"} 和DBC匹配, 添加到map中。");
                    signals[0].setField(field);
                }
                else {
                    System.out.println("字段名:"+field.getName()+", 注解值{"+signalTag+"} 在DBC中未找到相关信息。");
                }
            } // 查找字段注解
        }
        //查找所有字段
    }

    /**
     * 更新数据模型中的数据至DBC中
     * @param canId CanId
     */

    public void updateModelToDbc(int canId) {
        /* 1.  找到交换数据的双方 。字段和DBC*/
//        final CanMessage[] msgs = new CanMessage[1];
//        dbcMap.values().parallelStream().forEach(dbc -> {
//            if (dbc.getMsg(canId) != null){
//                msgs[0] = dbc.getMsg(canId);
//            }
//        });
        CanMessage msg = getMsgFromDbcMap(canId);
        if (msg == null){
            return;
        }
        Map<String, CanSignal> signalMap = msg.getSignalMap();
        signalMap.values().parallelStream().filter(signal -> signal.getField() != null).forEach(signal -> {
            Field field = signal.getField();/* 2. 拿到字段 */
            Object model = signal.getTarget(); // 原始对象
            //double sigValue = signal.currentValue ; /* 3. 拿到DBC的数据 */
            /* 4. 将 字段中的数据刷写到DBC中 */
            try {
                //signal.currentValue = field.getInt(model); // 这行代码没有问题
                Class<?> fieldType = field.getType();
                /*有9个预先定义好的Class对象代表8个基本类型和void,它们被java虚拟机创建,和基本类型有相同的名字boolean, byte, char, short, int, long, float, and double.
                这8个基本类型的Class对象可以通过java.lang.Boolean.TYPE,java.lang.Integer.TYPE等来访问,同样可以通过int.class,boolean.class等来访问.
                int.class与Integer.TYPE是等价的,但是与Integer.class是不相等的,int.class指的是int的Class对象,Integer.class是Integer的Class的类对象 */
                if (fieldType.equals(Integer.TYPE)){ // fieldValue instanceof Integer 和 fieldType == int.class 都可以判断。但不能用 Integer.class,注意。
                    signal.currentValue = field.getInt(model);
                }
                else if (fieldType.equals(Double.TYPE)){
                    signal.currentValue = field.getDouble(model);
                }
                else if (fieldType.equals(Byte.TYPE)){
                    signal.currentValue = field.getByte(model);
                }
                else if (fieldType.equals(Short.TYPE)){
                    signal.currentValue = field.getShort(model);
                }
                else if (fieldType.equals(Float.TYPE)){
                    signal.currentValue = field.getFloat(model);
                }else {
                    throw new RuntimeException("注解的字段类型出错，数据类型必须是 int,byte,short,float,double 中的一个");
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    } // updateModelToDbc

    /**
     * 更新DBC的数据至数据模型中
     * @param canId canId
     */
    public void updateDbcToModel(int canId) {
        /* 1.  找到交换数据的双方 。字段和DBC*/
//        final CanMessage[] msgs = new CanMessage[1];
//        dbcMap.values().parallelStream().forEach(dbc -> {
//            if (dbc.getMsg(canId) != null){
//                msgs[0] = dbc.getMsg(canId);
//            }
//        });
        CanMessage msg = getMsgFromDbcMap(canId);
        if (msg == null){
            return;
        }
        Map<String, CanSignal> signalMap = msg.getSignalMap();
        signalMap.values().parallelStream().filter(signal -> signal.getField() != null).forEach(signal -> {
            Field field = signal.getField();/* 2. 拿到字段 */
            Object model = signal.getTarget(); // 原始对象
            double sigValue = signal.currentValue ; /* 3. 拿到DBC的数据 */
            /* 4. 将 DBC中数据 刷写道 字段中 */
            try {
                field.setInt(model, (int) sigValue);
                Class<?> fieldType = field.getType();
                if (fieldType.equals(Integer.TYPE)){
                    field.setInt(model, (int) sigValue);
                }
                else if (fieldType.equals(Double.TYPE)){
                    field.setDouble(model,sigValue);
                }
                else if (fieldType.equals(Byte.TYPE)){
                    field.setByte(model, (byte) sigValue);
                }
                else if (fieldType.equals(Short.TYPE)){
                    field.setShort(model, (short) sigValue);
                }
                else if (fieldType.equals(Float.TYPE)){
                    field.setFloat(model, (float) sigValue);
                }
                else {
                    throw new RuntimeException("注解的字段类型出错，数据类型必须是 int,byte,short,float,double 中的一个");
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }
    private CanMessage getMsgFromDbcMap(int canId){
        final CanMessage[] msgs = new CanMessage[1];
        msgs[0] = null ;
        dbcMap.values().parallelStream().filter(dbc -> dbc.getMsg(canId) != null).forEach(dbc -> {
            msgs[0] = dbc.getMsg(canId);
        });
        return msgs[0];
    }
    /**
     * 获取某一个DBC的IO组件
     * @param dbcTag DBC的标签
     * @return 返回一个CAN收发器
     */
    public CanIO getCanIo(String dbcTag ){
        CanChannel dbc = dbcMap.get(dbcTag);
        CanIO canIO = canIOMap.get(dbcTag); // 如果查到了并且不为空，则直接返回
        if (canIO == null){ // 为空则重新创建，并加入到表中
            canIO = new CanIO(dbc,this);
            canIOMap.put(dbcTag,canIO);
        }
        return canIO;
    }
    public static CanObjectMapManager getInstance(){
        if (manager == null){
            return manager = new CanObjectMapManager();
        }
        else {
            return manager;
        }
    }
    private CanObjectMapManager(){

    }
    /**
     * 注册DBC文件
     * @param dbcTag DBC标签
     * @param dbcFilePath DBC文件地址
     * @return 返回是否添加成功
     */
    public boolean registerDBC(String dbcTag,String dbcFilePath) {
        CanChannel dbc = DbcHandle.getDbcFromFile(dbcFilePath);
        if (dbc != null){
            dbcMap.put(dbcTag,dbc);
            return true;
        }
        else {
            return false;
        }
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
    public void registerData(Object dataModel){
        getAnnotatedFields(dataModel);
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
}
