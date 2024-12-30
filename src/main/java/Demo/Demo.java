package Demo;

import QuickCanResolver.CanHandle.CanIO;
import QuickCanResolver.CanHandle.CanObjectMapManager;
import org.apache.poi.ooxml.util.IdentifierManager;

import java.util.Arrays;

public class Demo {
    public static void main(String[] args) {
        int num = 100;
        int id = 0x18AB_AB01 ; // message1
        /* 1.完成DBC和数据模型的绑定。 */
        String path1 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\Example.dbc";
        Model2 model = new Model2();

        CanObjectMapManager manager = CanObjectMapManager.getInstance();
        manager.registerDBC("testDbc",path1);
        manager.registerData(model);

        /* 2.获取CAN收发对象 */
        CanIO canIO = manager.getCanIo("testDbc");



        /* 3. 以下代码用于测试报文的  接收 */
//        byte[] data8 = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
//        canIO.canDataToModel(id,data8);
//        System.out.println("model = "+ model.getMsg1Value());
        // 输出 model = Msg1 = {msg1_sig1 :30, msg1_sig2 :29, msg1_sig3 = 28, msg1_sig4 = 20 ,
        // msg1_sig5: 22.200000000000003, msg1_sig6 :10.5, msg1_sig7 = -80.5, msg1_sig8 = 110.0}

        /* 4. 以下代码用于测试报文的  发送 */
//        model.msg1_sig1 = 30;
//        model.msg1_sig2 = 29;
//        model.msg1_sig3 = 28;
//        model.msg1_sig4 = 20;
//        model.msg1_sig5 = 22.2;
//        model.msg1_sig6 = 10.5;
//        model.msg1_sig7 = -80.5;
//        model.msg1_sig8 = 110;
//        int[] canData = canIO.modelToCanData(id); // message1
//        System.out.println("canData = "+ Arrays.toString(canData)); // 输出 {30, 29, 28, 20, 211, 121, 200, 100}


        long startTime = System.currentTimeMillis();
        for (int i = 0 ;i<num ;i++){
            // 以下代码用于测试报文的  接收
            byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
            canIO.canDataToModel(id,data8_);
            System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("程序运行时间: " + timeCost + " 毫秒");
        // 一万帧报文，611 、669 、564、 平均一万帧报文只需要600毫秒
        // 一千帧报文，138、149、145、159、238、148、196、268、247、230、205、218
        // 一百帧
    } // main
}
