package Demo;

import quickCanResolver.core.CanIo;
import quickCanResolver.core.CanListenService;
import org.junit.Test;
import quickCanResolver.core.DbcInputInterface;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import static Demo.DemoDataTest.data8_;
import static Demo.DemoDataTest.msg1_Id;

public class Demo3Test extends MyActivityTest {
    // 1. 初始化兼容层框架
    CanIo canIo = CanIo.getInstance();
    // 2. 完成 数据模型的初始绑定
    CarDataModelTest oldModel = canIo.manager.addDbcInputInterface(new DbcInputInterface() {
        @Override
        public InputStream getInputStream(String dbcFilePath) {
            try {
                return new FileInputStream(dbcFilePath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }).bind(CarDataModelTest.class);
    {
        // 初始化数据，也可以不初始化
        canIo.manager.deCode_B(msg1_Id, data8_);
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

    }
    @Test
    @Override
    public void onCreate() {
        System.out.println("这里是demo3");


        // 从Demo1 到Demo2 ，再到Demo3，这里更进一步实现了解耦。有了兼容层 CanIo之后，和底层的交互只需要一个适配器即可。
        canIo.register(McuAdapterTest.class , new CanListenService() {
            @Override
            public void listened(int canId , byte[] data8) {
                // 3. 解析数据后执行后续操作。
                // 在适配器中完成了数据的解析，这里只需也要关注 canid 即可
                // 实际的CAN接收监听在这里。根据不同的CANID，拿到数据 model 进行界面的刷新。
                // CarDataModel oldModel = canIo.manager.getModel(CarDataModel.class); // 获取原来绑定的初始对象
                if (canId == msg1_Id){
                    // 或者获取一个新的对象。（首先你需要根据ID判断数据是否有刷新）
                    CarDataModelTest newModel = canIo.manager.createNewModel(CarDataModelTest.class);
                    System.out.println("Model Value = " + newModel.getMsg1Value());  //打印值
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
                    System.out.println("newModel  = " + newModel );
                    // 打印 对象的 hash 码 Demo.CarDataModel@6ec8211c

                    System.out.println("拿到了数据，界面完成了刷新\n");
                }
            } // listened
        }); // register()

        // 延长JVM时间，让本地方法运行起来
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    } //  onCreate()

    @Override
    public void onDestroy() {
        canIo.unRegisterAll();
    }

    /** 模拟一个事件 */
    @Test
    public void event() {
        canIo.unRegisterCanListener();
        // 模拟在子线程中，手动修改了模型的值。
        oldModel.updateValue();
        // 调用服务，完成模拟数据发送
        canIo.send(msg1_Id);
        // 产生报文 = [11, 12, 13, 14, 217, 121, 194, 110] 正确
        CarDataModelTest dataModel = canIo.manager.getModel(CarDataModelTest.class);
        // ...省略更改界面的代码
    }

    public static class HandlerImp extends MyHandlerTest {
        WeakReference<Demo3Test> ref ;
        public HandlerImp(Demo3Test myActivity){
            ref = new WeakReference<>(myActivity) ;
        }
        @Override
        public void handleMessage() {
            Demo3Test activity = ref.get();
            // 在这里接收 子线程 发来的事件请求，并处理，例如更新界面，发送报文等操作。
            activity.event();
            CanIo canIo = CanIo.getInstance();
            CarDataModelTest newModel = canIo.manager.createNewModel(CarDataModelTest.class);
        }
    }
}
