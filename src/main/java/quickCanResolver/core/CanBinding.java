package quickCanResolver.core;


import java.lang.annotation.*;

/**
 * 用于绑定信号。<br>
 *  messageId : int = CAN报文的ID，如0x18ABAB01 <br>
 *  signalTag : String = CAN信号在dbc文件中的名称，需要保持一致。 <br>
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CanBinding {
    int Default = -1 ;

    /**
     * CAN报文的ID，如0x18ABAB01 。存在默认值，可不填写。（填写了之后可提高程序效率，这取决于你）
     * @return CAN报文的ID，如0x18ABAB01
     */
    int messageId() default Default ;

    /**
     * CAN信号在dbc文件中的名称，需要保持一致。
     */
    String signalTag() ;
}
