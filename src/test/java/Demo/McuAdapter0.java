package Demo;

import quickCanResolver.core.CanManagerImp;
import quickCanResolver.core.CanSendService;

/**
 * 将第三方组件彻底的从业务逻辑组件 Demo2 中解耦出来。由于不同的第三方组件，实现的回调函数可能不一样，所以就需要创建新的适配器。
 * TODO ：这里的代码只是一个示例。
 */
public class McuAdapter0 implements CanSendService, McuCan.McuCanListener {
    McuCan mcuCan = McuCan.getInstance();
    CanManagerImp canManagerImp;// 第三方组件
    MyHandler myHandler;

    public McuAdapter0(CanManagerImp canManagerImp, MyHandler myHandler) {
        mcuCan.nativeRegisterCanListener(this);
        this.canManagerImp = canManagerImp;
        this.myHandler = myHandler;
    }

    @Override
    public void send(int canId, int[] data8) {
        // 传入这里，最终数据的发送由 MCU 执行。应对不同的地层，可以在这里做相对应的适配，下边的代码同样只是举例。根据底层实现不同，这里的代码也会有区别。
        mcuCan.nativeSendCanData(canId, data8);
    }
    @Override
    public void send(int canId) {

        // 传入这里，最终数据的发送由 MCU 执行。应对不同的地层，可以在这里做相对应的适配，下边的代码同样只是举例。根据底层实现不同，这里的代码也会有区别。
        mcuCan.nativeSendCanData(canId, canManagerImp.enCode_I(canId));
    }


    @Override
    public void onStatus(int canId, byte[] data8) {
        // 3 解析放到了这里。
        canManagerImp.deCode_B(canId, data8);
        // .... 更多的处理逻辑在这里写
        myHandler.sendMessage(); // 例如，你可以发送消息，发起一次界面更新的请求
    }
}
