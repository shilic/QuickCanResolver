package Demo;

import quickCanResolver.core.CanIo;
import quickCanResolver.core.CanListenService;
import quickCanResolver.core.McuService;

/** 因为为了适配第三方组件McuCan。故采用适配器模式，用于适配不同的底层实现。上层直接使用接口，底层变动，上层不需要变动。 */
public class McuAdapterTest implements McuService {
    // 最终交给底层的 McuCan 来实现。
    McuCanTest mcuCan = McuCanTest.getInstance(); //第三方组件。不可变动。 同时，第三方的组件和现有的接口有所不同，故采用适配器模式，适配现有接口。
    McuCanListenerEvent listenerEvent ;
    public McuAdapterTest() {
    }

    @Override
    public void nativeSend(int canId, byte[] data8) {
        // 最终调用了本地的方法来实现报文的发送
        mcuCan.nativeSendCanData(canId, data8);
    }

    @Override
    public void nativeRegister(CanListenService canListener) {
        listenerEvent = McuCanListenerEvent.getInstance().addListenerEvent(canListener);
        // 这里会首先回调本地的方法调用。先调用第三方组件的方法。最后调用自己的方法
        mcuCan.nativeRegisterCanListener(listenerEvent);
    }

    @Override
    public void nativeUnRegister(CanListenService canListener) {
        if (listenerEvent != null && canListener != null) {
            mcuCan.unregisterCan(listenerEvent);
        }
    }
    public static class McuCanListenerEvent implements McuCanTest.McuCanListener {
        private static volatile McuCanListenerEvent event;
        CanListenService canListener;

        @Override
        public void onStatus(int canId, byte[] data8) {
            // 拿到第三方的数据后， 最终回调了我自己写的监听函数。
            // 首先进行数据的解析。
            CanIo.getInstance().manager.deCode_B(canId, data8);
            // 再回调从 Activity 传入的回调函数
            canListener.listened(canId, data8);
        }

        public static McuCanListenerEvent getInstance() {
            if (event == null){
                synchronized (McuCanListenerEvent.class){
                    if (event == null){
                        return event = new McuCanListenerEvent();
                    }
                }
            }
            return event;
        }
        private McuCanListenerEvent(){

        }
        public McuCanListenerEvent addListenerEvent(CanListenService canListener){
            this.canListener = canListener;
            return this;
        }
    }
}
