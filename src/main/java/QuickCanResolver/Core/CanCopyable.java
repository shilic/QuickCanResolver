package QuickCanResolver.Core;

/* 为什么选择自定义拷贝接口的方式？
 * 1. 刷新的字段不确定。
 * 2. 刷新的数据模型不确定。
 * 3. clone 方法的实现通常在 Object 类中声明为 protected。因为类型不确定，故我们无法直接使用Object 类的 clone 方法，故只有通过自定义 克隆接口 。
 * 4. 反射包中定义的 Field.setValue(Object,value) 方法，是使用的的 Object 作为接受者，而不是 泛型。
 *  */
/**
 * 自定义的 copy 接口，用于拷贝数据模型。 <br>
 * 自定义数据模型需要实现该接口，否则无法生成新对象。 <br>
 */
public interface CanCopyable<T> {
    /**
     * 自定义的 copy 接口，用于拷贝数据模型，请返回拷贝之后的数据模型。
     * @return 返回拷贝之后的数据模型
     */
    T copyNew();
}
