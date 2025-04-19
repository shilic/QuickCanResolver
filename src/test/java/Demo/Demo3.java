package Demo;

import QuickCanResolver.Core.CanIo;
import QuickCanResolver.Core.CanListenService;

import java.lang.ref.WeakReference;

public class Demo3 extends MyActivity {

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


    CanIo canIo = CanIo.getInstance();
    @Override
    public void onCreate() {
        System.out.println("这里是demo3");
        canIo.register(new McuAdapter(), new CanListenService() {
            @Override
            public void listened(int canId, byte[] data8) {
                // 实际的CAN接收监听在这里。
            }
        });
    }
    public void clickEvent() {
        // 调用服务，完成模拟数据发送
        canIo.send(msg1_Id);
        CarDataModel dataModel = canIo.manager.getModel(CarDataModel.class);
    }

    public static class HandlerImp extends MyHandler{
        WeakReference<Demo3> ref ;
        public HandlerImp(Demo3 myActivity){
            ref = new WeakReference<>(myActivity) ;
        }
        @Override
        public void handleMessage() {
            Demo3 activity = ref.get();
            // 在这里接收 子线程 发来的事件请求，并处理，例如更新界面，发送报文等操作。
            activity.clickEvent();
            CanIo canIo = CanIo.getInstance();
            CarDataModel newModel = canIo.manager.createNewModel(CarDataModel.class);
        }
    }
}
