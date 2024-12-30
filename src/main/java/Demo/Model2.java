package Demo;

import QuickCanResolver.CanHandle.SignalTag;

public class Model2 {
    @SignalTag(signalName = "CabinToCCS1_FactoryID")
    int CabinToCCS1_FactoryID ;
    @SignalTag(signalName = "CabinToCCS1_CabinTemp")
    int CabinToCCS1_CabinTemp ;
    @SignalTag(signalName = "CabinToCCS1_ColdGearSts")
    int CabinToCCS1_ColdGearSts;
    @SignalTag(signalName = "CabinToCCS1_FanGearSts")
    int CabinToCCS1_FanGearSts;
    @SignalTag(signalName = "CabinToCCS1_FanMotFlt")
    int CabinToCCS1_FanMotFlt ;
    @SignalTag(signalName = "CabinToCCS1_ColdMotFlt")
    int CabinToCCS1_ColdMotFlt;
    @SignalTag(signalName = "CabinToCCS1_AirSts")
    int CabinToCCS1_AirSts;
    @SignalTag(signalName = "CabinToCCS1_RollCnt")
    int CabinToCCS1_RollCnt;

    @SignalTag(signalName = "msg1_sig1")
    int msg1_sig1;
    @SignalTag(signalName = "msg1_sig2")
    int msg1_sig2;
    @SignalTag(signalName = "msg1_sig3")
    int msg1_sig3;
    @SignalTag(signalName = "msg1_sig4")
    int msg1_sig4;
    @SignalTag(signalName = "msg1_sig5")
    double msg1_sig5;
    @SignalTag(signalName = "msg1_sig6")
    double msg1_sig6;
    @SignalTag(signalName = "msg1_sig7")
    double msg1_sig7;
    @SignalTag(signalName = "msg1_sig8")
    double msg1_sig8;

    @SignalTag(signalName = "msg2_sig1")
    int msg2_sig1;
    @SignalTag(signalName = "msg2_sig2")
    int msg2_sig2;
    @SignalTag(signalName = "msg2_sig3")
    int msg2_sig3;
    @SignalTag(signalName = "msg2_sig4")
    int msg2_sig4;
    @SignalTag(signalName = "msg2_sig5")
    int msg2_sig5;
    @SignalTag(signalName = "msg2_sig6")
    int msg2_sig6;
    @SignalTag(signalName = "msg2_sig7")
    int msg2_sig7;
    @SignalTag(signalName = "msg2_sig8")
    int msg2_sig8;

    @SignalTag(signalName = "msg3_sig1")
    int msg3_sig1;
    @SignalTag(signalName = "msg3_sig2")
    int msg3_sig2;
    @SignalTag(signalName = "msg3_sig3")
    int msg3_sig3;
    @SignalTag(signalName = "msg3_sig4")
    int msg3_sig4;
    @SignalTag(signalName = "msg3_sig5")
    double msg3_sig5;
    @SignalTag(signalName = "msg3_sig6")
    double msg3_sig6;
    @SignalTag(signalName = "msg3_sig7")
    double msg3_sig7;
    @SignalTag(signalName = "msg3_sig8")
    double msg3_sig8;

    int msg4_sig1;
    int msg4_sig2;
    int msg4_sig3;
    int msg4_sig4;
    int msg4_sig5;
    int msg4_sig6;
    int msg4_sig7;
    int msg4_sig8;



    public String getMsg1Value(){
        return "Msg1 = {msg1_sig1 :"+msg1_sig1+", msg1_sig2 :"+msg1_sig2+", msg1_sig3 = "+msg1_sig3+", msg1_sig4 = "+msg1_sig4
                +" , msg1_sig5: "+msg1_sig5+", msg1_sig6 :"+msg1_sig6+", msg1_sig7 = "+msg1_sig7+", msg1_sig8 = "+msg1_sig8
                +"}";
    }

}
