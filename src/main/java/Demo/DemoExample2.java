package Demo;

import QuickCanResolver.Core.CanManager;
import QuickCanResolver.Core.CanOutputService;
import QuickCanResolver.Core.McuAdapter;

import java.lang.ref.WeakReference;

public class Demo2 extends MyActivity {
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


    // 1 获取一个管理器
    CanManager canManager = CanManager.getInstance();
    // 2 通过管理器，实例化当前的模型,内部完成绑定操作
    CarDataModel oldModel = canManager.bind(CarDataModel.class);
    MyHandler myHandler = new HandlerImp(this);
    CanOutputService out = new McuAdapter(canManager,myHandler);
    @Override
    public void onCreate() {
        System.out.println("你好，这里是demo1");
        CarDataModel newModel = canManager.createNewModel(CarDataModel.class);
    }
    // 于是，相比于 Demo1 这里需要再多处一个中间层，用于解耦数据的实际收发和解析。

    /** 这里模拟一个点击事件，点击后触发报文发送。由于这里完成了解耦，故实际的执行器发生变动时，这里的代码并不需要变动。 */
    public void clickEvent() {
        // 调用服务，完成模拟数据发送
        out.onOutput(msg1_Id,canManager.enCode_I(msg1_Id));
    }
    public static class HandlerImp extends MyHandler{
        WeakReference<Demo2> ref ;
        public HandlerImp(Demo2 myActivity){
            ref = new WeakReference<>(myActivity) ;
        }
        @Override
        public void handleMessage() {
            Demo2 activity = ref.get();
            // 在这里接收 子线程 发来的事件请求，并处理，例如更新界面，发送报文等操作。
            activity.clickEvent();
        }
    }
}
