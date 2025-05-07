package Demo;


// 第三方组件无法改动，所以需要做适配。
// 模拟第三方组件，用于模拟实车上的CAN收发。

import quickCanResolver.tool.SLCTool;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static Demo.DemoData.*;

/** 模拟底层的组件。第三方组件。不可变动。 */
public final class McuCan {
    private static volatile McuCan mcuCan;
    McuCanListener mcuCanListener;
    boolean register = false;
    public static McuCan getInstance(){
        if (mcuCan == null){
            synchronized (McuCan.class){
                if (mcuCan == null){
                    return mcuCan = new McuCan();
                }
            }
        }
        return mcuCan;
    }
    private McuCan() {
        // 模拟数据周期刷新
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (! register){
                    return;
                }
                if (mcuCanListener == null){
                    return;
                }
                // 模拟接收到报文，回调函数最终在这里被本地方法调用。
                mcuCanListener.onStatus(msg1_Id,data8_2);
                System.out.println("本地回调函数被调用");
            }
        };
        Timer timer = new Timer("mcu");
        timer.schedule(task,10,1_000);
    }
    /** 模拟了一个数据发送方法，具体实现由车机大屏的MCU实现。最终交给应用层使用。 */
    public void nativeSendCanData( int canId, int[] data8 ) {
        System.out.println("McuCan：成功在本地发送报文" );
        System.out.println("id = "+ SLCTool.hex2Str(canId) +", 发送的报文为 "+ Arrays.toString(data8) );
    }
    /** 模拟了一个数据监听方法的注册，将数据监听接口传给本地方法后，由本地方法回调你写的函数 */
    public void nativeRegisterCanListener(McuCanListener mcuCanListener){
        System.out.println("McuCan：成功在本地注册回调函数");
        register = true;
        this.mcuCanListener = mcuCanListener;
    }

    /** 模拟了一个数据监听接口 */
    public interface McuCanListener {
        /** 数据监听，当有报文来时触发以下方法。解码CAN报文，进行界面的绘制等操作。*/
        void onStatus(int canId, byte[] data8);
    }
    /** 模拟了一个取消CAN注册的方法 */
    public void unregisterCan(McuCanListener mcuCanListener){
        register = false;
    }


}
