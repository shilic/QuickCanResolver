package quickCanResolver.core;

import java.lang.annotation.*;

/**
 * 用于绑定一个dbc文件。<br>
 *  dbcTag : String = dbc标签名 <br>
 *  dbcPath : String = dbc文件路径 <br>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DbcBinding {
    /**
     * dbc的数组集合
     * @return dbc的数组集合
     */
    Dbc[] value();

    // 注解中只可以注解编译时常量，不可注解运行时常量，这一点和 switch 的用法一致，只能是编译时常量。
    // 故如果想要更复杂的数据，只能是嵌套注解或者枚举或者基本数据类型的数组。
    /**
     *  单个dbc文件。<br>
     *  dbcTag : String = dbc标签名 <br>
     *  dbcPath : String = dbc文件路径 <br>
     */
    @interface Dbc{
        /**
         * dbc标签名
         */
        String dbcTag() ;
        /**
         * dbc文件路径
         * */
        String dbcPath() ;
    }
}
