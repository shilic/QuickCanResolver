package Demo;


// 第三方组件无法改动，所以需要做适配。
// 模拟第三方组件，用于模拟实车上的CAN收发。
public class McuCanExample {
    public static McuCanExample getInstance(){
        return new McuCanExample();
    }
    private McuCanExample() {
    }
    // 模拟了一个数据发送方法，具体实现由车机大屏的MCU实现。最终交给应用层使用。
    public native void sendCanData(int canId, int[] data8);
    // 模拟了一个数据监听方法的注册，将数据监听接口传给本地方法后，由本地方法回调你写的函数
    public native void registerCanListener(CanListener canListener);

    // 模拟了一个数据监听接口
    public interface CanListener {
        /** 数据监听，当有报文来时触发以下方法。解码CAN报文，进行界面的绘制等操作。*/
        void onStatus(int canId, byte[] data8);
    }
}
