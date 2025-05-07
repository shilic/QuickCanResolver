package quickCanResolver.core;

/** CAN输出服务 */
public interface CanSendService {
    /** 编码CAN数据，并输出 */
    public void send(int canId, int[] data8);
    public void send(int canId);
}
