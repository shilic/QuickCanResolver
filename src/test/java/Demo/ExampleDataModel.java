package Demo;


import quickCanResolver.core.CanBinding;

/**
 * CAN报文数据模型类，用于描述车身数据，例如车速、门状态、车窗状态、空调状态等。该类仅作示范作用。使用注解和反射获取该类中的字段，进行绑定。
 */
public class ExampleDataModel {
    /* 按照该DBC构建一个车身数据模型
    BO_ 2560107544 CCS7_CCSToCabin1: 8 CCS
        SG_ test_Signal_14 : 24|8@1+ (0.1,-5.55) [-5|20.5] "" Vector__XXX
        SG_ CCSToCabin1_FanGearReq : 16|4@1+ (1,0) [0|15] "" Vector__XXX
        G_ CCSToCabin1_ColdGearReq : 20|4@1+ (1,0) [0|15] ""  Cabin
        SG_ CCSToCabin1_AirSw : 8|2@1+ (1,0) [0|3] ""  Test,Cabin
        SG_ CCSToCabin1_FactoryID : 0|8@1+ (1,0) [0|255] ""  Cabin
    * */
    //@CanBinding(signalTag = "test_Signal_14")
    public double test_Signal_14 ;
    @CanBinding(signalTag = "CCSToCabin1_FanGearReq")
    public int fanGearReq = 0;
    int coldGearReq = 0;
    int airSw = 0;
    int factoryID = 0;

    @Override
    public String toString() {
        return "ExampleDataModel{" +
                "test_Signal_14=" + test_Signal_14 +
                ", fanGearReq=" + fanGearReq +
                ", coldGearReq=" + coldGearReq +
                ", airSw=" + airSw +
                ", factoryID=" + factoryID +
                '}';
    }
}
