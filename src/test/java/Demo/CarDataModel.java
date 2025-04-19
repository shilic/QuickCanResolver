package Demo;

import QuickCanResolver.Core.CanBinding;
import QuickCanResolver.Core.CanCopyable;
import QuickCanResolver.Core.DbcBinding;

/**
 * 定义一个数据模型类，仅用于测试，除了最基本的数据模型外，不进行额外操作。
 *
 */
//@DbcBinding(dbcTag = "testDbc", dbcPath = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\Example.dbc")
    // 嵌套注解，实现更为复杂的数据的注解，现在可以传入多个DBC给一个数据模型了。可以使用静态常量
@DbcBinding({
        @DbcBinding.Dbc(dbcTag = DemoData.TEST_DBC, dbcPath = DemoData.DBC_PATH),
        @DbcBinding.Dbc(dbcTag = DemoData.TEST_DBC2, dbcPath = DemoData.DBC_PATH2)
})
public class CarDataModel implements CanCopyable<CarDataModel> , Cloneable {
    @CanBinding(signalTag = "CabinToCCS1_FactoryID")
    int CabinToCCS1_FactoryID ;
    // 属性的名称可以不和dbc中的信号名称一致，名称定义取决于你。但，注解中标注的信号名称需要和DBC文件中定义的一致。
    /** 空调温度 */
    @CanBinding(signalTag = "CabinToCCS1_CabinTemp")
    int airTemperature;
    /** 空调制冷级别 */
    @CanBinding(signalTag = "CabinToCCS1_ColdGearSts")
    int coldGear;
    /**  鼓风机档位 */
    @CanBinding(signalTag = "CabinToCCS1_FanGearSts")
    int fanGear;
    /** 鼓风机故障状态 */
    @CanBinding(signalTag = "CabinToCCS1_FanMotFlt")
    int fanMotFault;
    /** 冷机故障状态 */
    @CanBinding(signalTag = "CabinToCCS1_ColdMotFlt")
    int coldMotFault;
    /** 空调运行状态 */
    @CanBinding(signalTag = "CabinToCCS1_AirSts")
    int airState;
    /** rollingCounter，滚动计数，用于校验信号是否正确。 */
    @CanBinding(signalTag = "CabinToCCS1_RollCnt")
    int rollingCounter;

    @CanBinding(signalTag = "msg1_sig1")
    int msg1Sig1;
    @CanBinding(signalTag = "msg1_sig2")
    int msg1Sig2;
    @CanBinding(signalTag = "msg1_sig3")
    int msg1Sig3;
    @CanBinding(signalTag = "msg1_sig4")
    int msg1Sig4;
    @CanBinding(signalTag = "msg1_sig5")
    double msg1Sig5;
    @CanBinding(signalTag = "msg1_sig6")
    double msg1Sig6;
    @CanBinding(signalTag = "msg1_sig7")
    double msg1Sig7;
    @CanBinding(signalTag = "msg1_sig8")
    double msg1Sig8;

    @CanBinding(signalTag = "msg2_sig1")
    int msg2_sig1;
    @CanBinding(signalTag = "msg2_sig2")
    int msg2_sig2;
    @CanBinding(signalTag = "msg2_sig3")
    int msg2_sig3;
    @CanBinding(signalTag = "msg2_sig4")
    int msg2_sig4;
    @CanBinding(signalTag = "msg2_sig5")
    int msg2_sig5;
    @CanBinding(signalTag = "msg2_sig6")
    int msg2_sig6;
    @CanBinding(signalTag = "msg2_sig7")
    int msg2_sig7;
    @CanBinding(signalTag = "msg2_sig8")
    int msg2_sig8;

    @CanBinding(signalTag = "msg3_sig1")
    int msg3_sig1;
    @CanBinding(signalTag = "msg3_sig2")
    int msg3_sig2;
    @CanBinding(signalTag = "msg3_sig3")
    int msg3_sig3;
    @CanBinding(signalTag = "msg3_sig4")
    int msg3_sig4;
    @CanBinding(signalTag = "msg3_sig5")
    double msg3_sig5;
    @CanBinding(signalTag = "msg3_sig6")
    double msg3_sig6;
    @CanBinding(signalTag = "msg3_sig7")
    double msg3_sig7;
    @CanBinding(signalTag = "msg3_sig8")
    double msg3_sig8;


    int msg4_sig1;
    int msg4_sig2;
    int msg4_sig3;
    int msg4_sig4;
    int msg4_sig5;
    int msg4_sig6;
    int msg4_sig7;
    int msg4_sig8;

    // 这里是第二个DBC中的数据，框架实现了可以多个模型对应多个DBC的复杂映射关系。
    @CanBinding(signalTag = "msg9_sig1")
    int msg9_sig1;
    @CanBinding(signalTag = "msg9_sig2")
    int msg9_sig2;
    @CanBinding(signalTag = "msg9_sig3")
    int msg9_sig3;
    @CanBinding(signalTag = "msg9_sig4")
    int msg9_sig4;
    @CanBinding(signalTag = "msg9_sig5")
    int msg9_sig5;
    @CanBinding(signalTag = "msg9_sig6")
    int msg9_sig6;
    @CanBinding(signalTag = "msg9_sig7")
    int msg9_sig7;
    @CanBinding(signalTag = "msg9_sig8")
    int msg9_sig8;



    public String getMsg1Value(){
        return "Msg1 = {msg1_sig1 :"+ msg1Sig1 +", msg1_sig2 :"+ msg1Sig2 +", msg1_sig3 = "+ msg1Sig3 +", msg1_sig4 = "+ msg1Sig4
                +" , msg1_sig5: "+ msg1Sig5 +", msg1_sig6 :"+ msg1Sig6 +", msg1_sig7 = "+ msg1Sig7 +", msg1_sig8 = "+ msg1Sig8
                +"}";
    }

    /**
     * 测试刷新数据，仅用于测试。
     */
    public void updateValue(){
        msg1Sig1 = 11;
        msg1Sig2 = 12;
        msg1Sig3 = 13;
        msg1Sig4 = 14 ;
        msg1Sig5 = 23.45;
        msg1Sig6 = 10.7;
        msg1Sig7 = -81.1;
        msg1Sig8 = 111.0;
    }


    @Override
    public CarDataModel copyNew() {
        // TODO:(需要你自己实现 CanCopyable<T> 接口，并需要自己实现 拷贝方法，并返回自身 。下边的代码只是一个示例，你也可以采用其他拷贝方式。)
        return clone();
    }

    @Override
    public CarDataModel clone() {
        try {
            return (CarDataModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆失败", e);
        }
    }
}
