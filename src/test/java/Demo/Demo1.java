package Demo;

import quickCanResolver.core.CanManagerImp;

public class Demo1 extends MyActivity {

    McuCan mcuCan;

    @Override
    public void onCreate() {
        System.out.println("你好，这里是demo1");
        // 1 获取一个管理器
        CanManagerImp canManagerImp = CanManagerImp.getInstance() ;
        // 2 通过管理器，实例化当前的模型,内部完成绑定操作
        CarDataModel oldModel = canManagerImp.bind(CarDataModel.class);

        Demo1 demo1 = new Demo1();
        // 新建一个第三方组件。
        demo1.mcuCan = McuCan.getInstance(); // 第三方组件
        demo1.mcuCan.nativeRegisterCanListener(new McuCan.McuCanListener() {
            @Override
            public void onStatus(int canId, byte[] data8) {
                // 解耦：传入解析函数，每次来数据的时候，实现自动解析
                // 3. 解码报文数据，
                canManagerImp.deCode_B(canId,data8);
            }
        });
        CarDataModel newModel = canManagerImp.createNewModel(CarDataModel.class);
        // 到这里，实际上已经实现了一部分解耦，数据实现了自动解析。拿到 oldModel 和 newModel 后，后续更新界面也非常快速。
        // TODO ：只是存在的一个问题是，如果第三方的组件（报文收发实际执行者）发生改动，那么仍然存在耦合
    }

    @Override
    public void onDestroy() {

    }

    // 这里模拟一个点击事件，点击后触发报文发送
    public void event1() {
        // TODO:这里仍然存在耦合
        // 模拟数据发送
        mcuCan.nativeSendCanData(123,null);
    }
}
