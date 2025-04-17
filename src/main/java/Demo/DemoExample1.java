package Demo;

import QuickCanResolver.Core.CanManager;

public class Demo1 extends MyActivity {
    public static final String TEST_DBC = "testDbc";
    public static final String TEST_DBC2 = "testDbc2";

    // E:\storge\very\code\IntelliJ_IDEA_Project\QuickCanResolver\src\main\resources\DBC\Example.dbc
    public static final String DBC_PATH = "src/main/resources/DBC/Example.dbc" ;
    public static final String DBC_PATH2 = "src/main/resources/DBC/Example2.dbc" ;

    static final int msg1_Id = 0x18AB_AB01 ; // message1
    static final int msg2_Id = 0x18AB_AB02; //
    static final int msg3_Id = 0x18AB_AB03; //
    static byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
    static byte[] data8_2 = new byte[]{7, 8, 9, 10, (byte) 211, 121, (byte) 200, 100};

    McuCan mcuCan ;

    @Override
    public void onCreate() {
        System.out.println("你好，这里是demo1");
        // 1 获取一个管理器
        CanManager canManager = CanManager.getInstance() ;
        // 2 通过管理器，实例化当前的模型,内部完成绑定操作
        CarDataModel oldModel = canManager.bind(CarDataModel.class);

        Demo1 demo1 = new Demo1();
        // 新建一个第三方组件。
        demo1.mcuCan = McuCan.getInstance(); // 第三方组件
        demo1.mcuCan.registerCanListener(new McuCan.CanListener() {
            @Override
            public void onStatus(int canId, byte[] data8) {
                // 解耦：传入解析函数，每次来数据的时候，实现自动解析
                // 3. 解码报文数据，
                canManager.deCode_B(canId,data8);
            }
        });
        CarDataModel newModel = canManager.createNewModel(CarDataModel.class);
        // 到这里，实际上已经实现了一部分解耦，数据实现了自动解析。拿到 oldModel 和 newModel 后，后续更新界面也非常快速。
        // TODO ：只是存在的一个问题是，如果第三方的组件（报文收发实际执行者）发生改动，那么仍然存在耦合
    }
    // 这里模拟一个点击事件，点击后触发报文发送
    public void event1() {
        // TODO:这里仍然存在耦合
        // 模拟数据发送
        mcuCan.sendCanData(123,null);
    }
}
