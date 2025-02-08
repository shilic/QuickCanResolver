package QuickCanResolver.CanTool;

import java.lang.reflect.Field;

public class MyReflect {
    /**
     * 设置字段值
     * @param field 字段
     * @param model 对象
     * @param sigValue 要设置的值
     * @throws IllegalAccessException 抛出参数异常
     */
    public static void setFieldValue(Field field, Object model, double sigValue) throws IllegalAccessException {
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
    }
    public static double getFieldValue(Field field,Object target) throws IllegalAccessException {
        double mCurrentValue;
        Class<?> fieldType = field.getType();
        /*有9个预先定义好的Class对象代表8个基本类型和void,它们被java虚拟机创建,和基本类型有相同的名字boolean, byte, char, short, int, long, float, and double.
        这8个基本类型的Class对象可以通过java.lang.Boolean.TYPE,java.lang.Integer.TYPE等来访问,同样可以通过int.class,boolean.class等来访问.
        int.class与Integer.TYPE是等价的,但是与Integer.class是不相等的,int.class指的是int的Class对象,Integer.class是Integer的Class的类对象 */
        if (fieldType.equals(Integer.TYPE)){ // fieldValue instanceof Integer 和 fieldType == int.class 都可以判断。但不能用 Integer.class,注意。
            mCurrentValue = field.getInt(target);
        }
        else if (fieldType.equals(Double.TYPE)){
            mCurrentValue = field.getDouble(target);
        }
        else if (fieldType.equals(Byte.TYPE)){
            mCurrentValue = field.getByte(target);
        }
        else if (fieldType.equals(Short.TYPE)){
            mCurrentValue = field.getShort(target);
        }
        else if (fieldType.equals(Float.TYPE)){
            mCurrentValue = field.getFloat(target);
        }else {
            throw new RuntimeException("注解的字段类型出错，数据类型必须是 int,byte,short,float,double 中的一个");
        }
        return mCurrentValue;
    }
}
