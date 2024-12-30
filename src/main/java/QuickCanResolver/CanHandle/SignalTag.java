package QuickCanResolver.CanHandle;


import java.lang.annotation.*;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignalTag {
    int Default = -1;
    int messageName() default Default;
    String signalName() ;
}
