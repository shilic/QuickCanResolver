import QuickCanResolver.CanHandle.CanIO;
import QuickCanResolver.CanHandle.CanObjectMapManager;
import QuickCanResolver.CanHandle.DbcHandle;
import QuickCanResolver.DBC.CanDbc;
import QuickCanResolver.DBC.CanSignal;
import Demo.CanDataModel;
import org.junit.Test;

import java.util.Arrays;

public class DbcTest {
    static int id = 0x18982418;

    /**
     * 测试基本的CAN收发
     */
    @Test
    public void test1(){
        String path1 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\大屏协议（测试版2）GBK编码.dbc";
        CanDbc dbc = null;
        try {
            dbc = DbcHandle.getDbcFromFile(path1);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        assert dbc != null;
        CanIO canIO = new CanIO(dbc);
        id = 0x1898_2418;
        byte[] data8 = new byte[]{10, 3, (byte) 143, 0, 0, 0, 0, 0}; // {20,55, (byte) 0b1000_1111,55,55,55,55,55}
        canIO.concurrentCanDataToDbc(id,data8);
        System.out.println("接收报文 Msg = "+dbc.getMsg(id).getMsgValue());
        /*  测试数据： byte[] data8 = new byte[]{10,3, (byte) 0b10001000,55,55,55,55,55};
        * 测试结果：
        接收报文 Msg = 报文名称：CCS7_CCSToCabin1;
        信号 : CCSToCabin1_ColdGearReq = 8.0;
        信号 : CCSToCabin1_AirSw = 3.0;
        信号 : CCSToCabin1_FactoryID = 10.0;
        信号 : test_Signal_14 = -5.55;
        信号 : CCSToCabin1_FanGearReq = 15.0;
        *  */
        int[] ints = canIO.concurrentDbcToCanData(id);
        System.out.println("发送报文 = "+ Arrays.toString(ints));
        // 至此，报文的接收和发送均无问题。
    }
    /**
     * 测试直接通过 字符串获取信号
     */
    @Test
    public void mappingTest1(){
        //CanObjectMapManager.getAnnotatedFields(CanDataModel.class);
        String path1 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\大屏协议（测试版2）GBK编码.dbc";
        CanDbc dbc = null;
        try {
            dbc = DbcHandle.getDbcFromFile(path1);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (dbc!= null) {
            CanSignal signal = dbc.getSignal("CCSToCabin1_FanGearReq");
            System.out.println("找到信号了。signal = "+signal.getSignalInfo());
        }
    }

    @Test
    public void mapTest(){

    }
}
