package QuickCanResolver.Core;

import Demo.McuCan;
import Demo.MyHandler;

/**
 * 将第三方组件彻底的从业务逻辑组件 Demo2 中解耦出来。由于不同的第三方组件，实现的回调函数可能不一样，所以就需要创建新的适配器。
 * TODO ：这里的代码只是一个示例。
 */
public class McuAdapter implements CanOutputService, McuCan.CanListener {
    McuCan mcuCan = McuCan.getInstance();
    CanManager canManager;// 第三方组件
    MyHandler myHandler;

    public McuAdapter(CanManager canManager, MyHandler myHandler) {
        mcuCan.registerCanListener(this);
        this.canManager = canManager;
        this.myHandler = myHandler;
    }

    @Override
    public void onOutput(int canId, int[] data8) {
        // 传入这里，最终数据的发送由 MCU 执行。
        mcuCan.sendCanData(canId, data8);
    }

    @Override
    public void onStatus(int canId, byte[] data8) {
        // 3 解析放到了这里。
        canManager.deCode_B(canId, data8);
        // .... 更多的处理逻辑在这里写
        myHandler.sendMessage(); // 例如，你可以发送消息，发起一次界面更新的请求
    }
}
