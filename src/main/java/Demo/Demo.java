package Demo;

import QuickCanResolver.CanHandle.CanIO;
import QuickCanResolver.CanHandle.CanObjectMapManager;

public class Demo {
    public static void main(String[] args) {
        int num = 1000;
        int id = 0x18AB_AB01 ; // message1
        /* 1.完成DBC和数据模型的绑定。 */
        String path1 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\Example.dbc";
        Model2 model = new Model2();

        CanObjectMapManager manager = CanObjectMapManager.getInstance();
        manager.registerDBC("testDbc",path1); // 绑定DBC文件
        manager.registerData(model); // 绑定数据模型

        /* 2.获取CAN收发对象 */
        CanIO canIO = manager.getCanIo("testDbc");

        long startTime = System.currentTimeMillis();
        for (int i = 0 ;i<num ;i++){
            // 以下代码用于测试报文的  接收
            byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
            canIO.concurrentCanToField(id,data8_);
            System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("程序运行时间: " + timeCost + " 毫秒");
        // 关于多线程的思考。一些改动之后，在一千次时，解析速度确实有了一个明显的提升。但是在一万次时却差不多。
        // 我在思考，线程应该开在什么地方才能最大限度的提高性能
        /* 多线程。第一版程序测试耗时结果，
        一万帧报文，611 、669 、564、 平均一万帧报文只需要600毫秒
        // 一千帧报文，138、149、145、159、238、148、196、268、247、230、205、218
        // 一百帧 60、90、70 */

        /* 多线程 。改动，减少了一次赋值之后，
        * 一万次：657、563、915、661、668、480、750、732、785、754、549
        * 五千次：260、270、468、406、510、423、395、378、411、、281
        * 一千次：119、96、227、105、107、100、124、140、155、141、147、137、97、107、102、101、96、98
        * 一百次：53、39、68、42、46、48、50、51、61、44
        * */

    } // main
}
