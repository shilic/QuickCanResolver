import Demo.CarDataModel;
import Demo.ExampleDataModel3;
import QuickCanResolver.Core.CanFrameData;
import QuickCanResolver.Core.CanManagerImp;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BindDbcTest {
    static int num = 10000;
    static final int msg1_Id = 0x18AB_AB01 ; // message1
    static final int msg2_Id = 0x18AB_AB02; //
    static final int msg3_Id = 0x18AB_AB03; //
    static byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
    static byte[] data8_2 = new byte[]{7, 8, 9, 10, (byte) 211, 121, (byte) 200, 100};

    long bindTimeCost;

    List<CanFrameData> canFrameDataList;
    List<CanFrameData> canFrameDataList2;
    {
        // 模拟一些数据
        canFrameDataList = new ArrayList<>();
        CanFrameData canFrameData1 = new CanFrameData(msg1_Id,data8_);
        CanFrameData canFrameData2 = new CanFrameData(msg2_Id,data8_);
        CanFrameData canFrameData3 = new CanFrameData(msg3_Id,data8_);

        CanFrameData canFrameData4 = new CanFrameData(msg1_Id,data8_);
        CanFrameData canFrameData5 = new CanFrameData(msg2_Id,data8_);
        CanFrameData canFrameData6 = new CanFrameData(msg3_Id,data8_);

        CanFrameData canFrameData7 = new CanFrameData(msg1_Id,data8_);
        CanFrameData canFrameData8 = new CanFrameData(msg2_Id,data8_);
        CanFrameData canFrameData9 = new CanFrameData(msg3_Id,data8_);

        canFrameDataList.add(canFrameData1);
        canFrameDataList.add(canFrameData2);
        canFrameDataList.add(canFrameData3);
        canFrameDataList.add(canFrameData4);
        canFrameDataList.add(canFrameData5);
        canFrameDataList.add(canFrameData6);
        canFrameDataList.add(canFrameData7);
        canFrameDataList.add(canFrameData8);
        canFrameDataList.add(canFrameData9);

        canFrameDataList2 = new ArrayList<>();
        canFrameDataList2.add(canFrameData1);
        canFrameDataList2.add(canFrameData2);
        canFrameDataList2.add(canFrameData3);
    }

    /* 实验耗时记录 (毫秒)
     * 一千次：37,34,41,35,36,35
     * 一万次：88,67,99,77,96,80,83
     * 十万次：320,486,305,323,335,422,347
     *
     */
    @Test
    public void test1() {
        System.out.println("test1");
        int num = 10 ;
        // 1 获取一个管理器
        CanManagerImp canManagerImp = CanManagerImp.getInstance();

        // 2 通过管理器，实例化当前的模型,内部完成绑定操作
        CarDataModel model = canManagerImp.bind(CarDataModel.class);

        long startTime = System.currentTimeMillis();
        for (int i = 0 ; i < num ; i++){
            CanFrameData canFrameData = getRandomData();
            int canId = canFrameData.getMsgId();
            byte[] data = canFrameData.getBytes8();

            // 使用时，只需要一行代码即可更新数据到绑定的 model 中
            canManagerImp.deCode_B(canId,data);

            System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用单线程处理报文，并且不加锁，使用随机数据,运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
    }

    /* 实验耗时记录 (毫秒)
     * 一千次：21,23,24,21,33,27
     * 一万次：75,89,72,67,96,43,73,70,63
     * 十万次：416,375,353,331,432,424
     */
    /**
     * 测试生成新的模型
     */
    @Test
    public void test2() {
        System.out.println("test2");
        int num = 10;

        // 1 获取一个管理器
        CanManagerImp canManagerImp = CanManagerImp.getInstance() ;
        // 2 通过管理器，实例化当前的模型,内部完成绑定操作
        CarDataModel oldModel = canManagerImp.bind(CarDataModel.class);
        ExampleDataModel3 model3 = canManagerImp.bind(ExampleDataModel3.class);

        // 初始化数据，也可以不初始化
        canManagerImp.deCode_B(msg1_Id,data8_);
        System.out.println("oldModel Value = " + oldModel.getMsg1Value());  //打印值
        /* 初始化数据打印如下
         * oldModel Value =
         * Msg1 = {msg1_sig1 :30,
         * msg1_sig2 :29,
         * msg1_sig3 = 28,
         * msg1_sig4 = 20 ,
         * msg1_sig5: 22.200000000000003,
         * msg1_sig6 :10.5,
         * msg1_sig7 = -80.5,
         * msg1_sig8 = 110.0}
         */
        System.out.println("oldModel  = " + oldModel + "\n");
        // 打印 原始对象的 hash 码 Demo.CarDataModel@6ec8211c

        long startTime = System.currentTimeMillis();
        for (int i = 0 ; i < num ; i++) {
            // 用新数据和旧的模型生成一个新的模型
            //CarDataModel newModel = canCoder.createNewModel(msg1_Id,data8_2,oldModel);
            canManagerImp.deCode_B(msg1_Id,data8_2);
            CarDataModel newModel = canManagerImp.createNewModel(CarDataModel.class);
            // 多个数据模型也是可以的
            ExampleDataModel3 model3new = canManagerImp.createNewModel(ExampleDataModel3.class);

            System.out.println("newModel Value = "+ newModel.getMsg1Value());
            /* 更新数据打印如下
             *  newModel Value =
             * Msg1 = {msg1_sig1 :7,
             * msg1_sig2 :8,
             * msg1_sig3 = 9,
             * msg1_sig4 = 10 ,
             * msg1_sig5: 22.200000000000003,
             * msg1_sig6 :10.5,
             * msg1_sig7 = -80.5,
             * msg1_sig8 = 110.0}
             */
            System.out.println("newModel  = " + newModel);
            // 打印 新对象 的 hash 码 Demo.CarDataModel@7276c8cd
            // 可见，对象的hash码发生了变化，数据得到了更新
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用单线程处理报文，并且不加锁，测试生成新对象，运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
    }


    public CanFrameData getRandomData(){
        // 在3种报文中，挑一个，生成一个随机数据。
        int randomIndex = new Random().nextInt(canFrameDataList2.size());  //范围在 [0, list.size()) 的随机整数。
        return canFrameDataList2.get(randomIndex);
    }


}
