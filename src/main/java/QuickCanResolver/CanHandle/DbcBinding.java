package QuickCanResolver.CanHandle;

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
     * dbc标签名
     */
    String dbcTag() ;
    /**
     * dbc文件路径
     * */
    String dbcPath() ;
}
