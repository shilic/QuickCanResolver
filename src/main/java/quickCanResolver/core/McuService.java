package quickCanResolver.core;

/** 抽象底层的 CAN 收发实现  */
public interface McuService {
    /** 数据发送方法 */
    void nativeSend(int canId, int[] data8);
    /** 数据监听方法的注册 ，传入自己的CAN监听事件*/
    void nativeRegister(CanListenService canListener);
    /** 取消注册 */
    void nativeUnRegister(CanListenService canListener);
}
