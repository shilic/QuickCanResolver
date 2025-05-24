package quickCanResolver.core;

/** CAN发送服务 */
public interface CanSendService {
    /** 手动发送一组报文
     * @param canId canId
     * @param data8 8位数组的报文
     * */
    void send(int canId, byte[] data8);
    /** 根据id发送报文
     * @param canId canId
     * */
    void send(int canId);
    void send(int canId, Object model);
}
