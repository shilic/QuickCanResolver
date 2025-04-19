package Demo;

import QuickCanResolver.Core.CanListenService;
import QuickCanResolver.Core.McuService;

public class McuAdapter2 implements McuService {
    // 最终交给底层的 McuCan 来实现
    McuCan mcuCan = McuCan.getInstance();
    public McuAdapter2() {
    }

    @Override
    public void nativeSend(int canId, int[] data8) {
        mcuCan.sendCanData(canId, data8);
    }

    @Override
    public void nativeRegister(CanListenService canListener) {
        // 在这里给底层的 报文监听 的 回调函数
        mcuCan.registerCanListener(new McuCan.CanListener() {
            // 最外层是 底层 的 回调方法
            @Override
            public void onStatus(int canId, byte[] data8) {
                // 而最里边 最终回调了我自己写的监听函数。
                canListener.onListen(canId, data8);
            }
        });
    }
}
