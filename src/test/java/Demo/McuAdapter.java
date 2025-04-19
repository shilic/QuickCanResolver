package Demo;

import QuickCanResolver.Core.CanListenService;
import QuickCanResolver.Core.McuService;

/** 适配器模式，用于适配不同的底层实现。上层直接使用接口，底层变动，上层不需要变动。 */
public class McuAdapter implements McuService {
    // 最终交给底层的 McuCan 来实现
    McuCan mcuCan = McuCan.getInstance();
    public McuAdapter() {
    }

    @Override
    public void nativeSend(int canId, int[] data8) {
        mcuCan.sendCanData(canId, data8);
    }

    @Override
    public void nativeRegister(CanListenService canListener) {
        //  最终回调了我自己写的监听函数。
        mcuCan.registerCanListener(canListener::listened);
    }
}
