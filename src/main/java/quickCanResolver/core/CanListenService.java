package quickCanResolver.core;

/**
 * CAN报文监听接口。实现改接口，以实现报文的监听
 */
public interface CanListenService {
    /** 监听报文的回调函数 ，监听到报文后，回调下边方法。
     * <br>注意，改方法可能会在子线程处理，请注意处理逻辑。
     * @param canId 报文id
     * @param data8 长度8的字节数组
     * */
    void listened(int canId, byte[] data8);
}
