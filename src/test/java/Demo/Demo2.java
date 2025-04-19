package Demo;

import QuickCanResolver.Core.CanManagerImp;
import QuickCanResolver.Core.CanSendService;

import java.lang.ref.WeakReference;

import static Demo.DemoData.msg1_Id;

public class Demo2 extends MyActivity {
    // 1 获取一个管理器
    CanManagerImp canManagerImp = CanManagerImp.getInstance();
    // 2 通过管理器，实例化当前的模型,内部完成绑定操作
    CarDataModel oldModel = canManagerImp.bind(CarDataModel.class);
    MyHandler myHandler = new HandlerImp(this);
    CanSendService service = new McuAdapter0(canManagerImp,myHandler);
    // 这里可以更进一步解耦，把 CanManager 和 service 组装在一起，解耦主活动。
    @Override
    public void onCreate() {
        System.out.println("这里是demo2");
    }

    @Override
    public void onDestroy() {

    }
    // 于是，相比于 Demo1 这里需要再多处一个中间层，用于解耦数据的实际收发和解析。

    /** 这里模拟一个点击事件，点击后触发报文发送。由于这里完成了解耦，故实际的执行器发生变动时，这里的代码并不需要变动。 */
    public void clickEvent() {
        // 调用服务，完成模拟数据发送
        service.send(msg1_Id); // 内部完成了 canManager 封装
        CarDataModel newModel = canManagerImp.createNewModel(CarDataModel.class);
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
