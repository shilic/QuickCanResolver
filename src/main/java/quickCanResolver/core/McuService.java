package quickCanResolver.core;

/** 抽象底层的 CAN 收发实现  */
public interface McuService {
    /** 数据发送方法 */
    void nativeSend(int canId, byte[] data8);
    /** 数据监听方法的注册 ，传入自己的CAN监听事件*/
    void nativeRegister(CanListenService canListener);
    /** 取消注册 */
    void nativeUnRegister(CanListenService canListener);

    /**
     * 可选配置，从缓存中读取CAN报文。MCU适配器实现该方法后，需要在内部再调用原生方法获取报文，然后再返回给上层
     * @return 返回一组CAN报文
     */
    @SuppressWarnings("unused")
    default CanFrameData[] nativeReceive() {
        throw new RuntimeException("改方法暂时未实现");
    }
}
