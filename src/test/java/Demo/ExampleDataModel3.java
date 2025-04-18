package Demo;

import QuickCanResolver.Core.CanBinding;
import QuickCanResolver.Core.CanCopyable;
import QuickCanResolver.Core.DbcBinding;

@DbcBinding({
        @DbcBinding.Dbc(dbcTag = DemoExample1.TEST_DBC, dbcPath = DemoExample1.DBC_PATH),
})
public class ExampleDataModel3  implements CanCopyable<ExampleDataModel3>, Cloneable {
    @CanBinding(signalTag = "msg6_sig1")
    int msg6_sig1;
    @CanBinding(signalTag = "msg6_sig2")
    int msg6_sig2;
    @CanBinding(signalTag = "msg6_sig3")
    int msg6_sig3;
    @CanBinding(signalTag = "msg6_sig4")
    int msg6_sig4;
    @CanBinding(signalTag = "msg6_sig5")
    int msg6_sig5;
    @CanBinding(signalTag = "msg6_sig6")
    int msg6_sig6;
    @CanBinding(signalTag = "msg6_sig7")
    int msg6_sig7;
    @CanBinding(signalTag = "msg6_sig8")
    int msg6_sig8;

    @Override
    public ExampleDataModel3 copyNew() {
        return clone();
    }

    @Override
    public ExampleDataModel3 clone() {
        try {
            return (ExampleDataModel3) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆失败", e);
        }
    }
}