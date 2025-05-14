package quickCanResolver.dbc;

import java.lang.reflect.Field;

public class FieldChanger {
    private static final String ERR_INFO = "注解的字段类型出错，数据类型必须是 int,byte,short,float,double 中的一个";
    /**
     * 通过反射的方式，将字段值写入到传入的模型中。
     * @param field 字段
     * @param obj 对象
     * @param sigValue 要设置的值
     */
    public static void setFieldValue(Field field, Object obj, double sigValue) {
        if (obj == null){
            return;
        }
        Class<?> fieldType = field.getType();
        try {
            if (fieldType.equals(Integer.TYPE)){
                field.setInt(obj, (int) sigValue);
            }
            else if (fieldType.equals(Double.TYPE)){
                field.setDouble(obj,sigValue);
            }
            else if (fieldType.equals(Byte.TYPE)){
                field.setByte(obj, (byte) sigValue);
            }
            else if (fieldType.equals(Short.TYPE)){
                field.setShort(obj, (short) sigValue);
            }
            else if (fieldType.equals(Float.TYPE)){
                field.setFloat(obj, (float) sigValue);
            }
            else {
                throw new RuntimeException(ERR_INFO);
            }
        } catch (IllegalAccessException e){
            throw new RuntimeException(ERR_INFO);
        }

    }

    /**
     * 使用反射从对象获取值
     * @param field 对应字段
     * @param obj 对象
     * @return 返回对象相应的值
     */
    public static double getFieldValue(Field field,Object obj){
        double mCurrentValue;
        Class<?> fieldType = field.getType();
        /*有9个预先定义好的Class对象代表8个基本类型和void,它们被java虚拟机创建,和基本类型有相同的名字boolean, byte, char, short, int, long, float, and double.
        这8个基本类型的Class对象可以通过java.lang.Boolean.TYPE,java.lang.Integer.TYPE等来访问,同样可以通过int.class,boolean.class等来访问.
        int.class与Integer.TYPE是等价的,但是与Integer.class是不相等的,int.class指的是int的Class对象,Integer.class是Integer的Class的类对象 */
        try {
            if (fieldType.equals(Integer.TYPE)){
                mCurrentValue = field.getInt(obj);
            }
            else if (fieldType.equals(Double.TYPE)){
                mCurrentValue = field.getDouble(obj);
            }
            else if (fieldType.equals(Byte.TYPE)){
                mCurrentValue = field.getByte(obj);
            }
            else if (fieldType.equals(Short.TYPE)){
                mCurrentValue = field.getShort(obj);
            }
            else if (fieldType.equals(Float.TYPE)){
                mCurrentValue = field.getFloat(obj);
            }else {
                throw new RuntimeException(ERR_INFO);
            }
        }catch (IllegalAccessException e){
            throw new RuntimeException(ERR_INFO);
        }
        return mCurrentValue;
    }
}
