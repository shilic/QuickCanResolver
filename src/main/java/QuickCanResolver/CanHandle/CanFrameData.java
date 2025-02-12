package QuickCanResolver.CanHandle;

/**
 * CAN数据类，将报文ID和数据封装成一个整体。数据是只读
 */
// 仅用于测试
public class CanFrameData {
    protected final int msgId;
    protected final byte[] bytes8;

    /**
     * 使用现有数据生成一个 CAN数据帧
     */
    public CanFrameData(int msgId, byte[] bytes8) {
        this.msgId = msgId;
        this.bytes8 = bytes8;
    }

    public int getMsgId() {
        return msgId;
    }

    public byte[] getBytes8() {
        return bytes8;
    }

}
