package QuickCanResolver.CanHandle;

/**
 * CAN数据类，将报文ID和数据封装成一个整体。数据是只读
 */
public class CanData {
    protected final int msgId;
    protected final byte[] data8;

    public CanData(int msgId, byte[] data8) {
        this.msgId = msgId;
        this.data8 = data8;
    }

    public int getMsgId() {
        return msgId;
    }

    public byte[] getData8() {
        return data8;
    }
}
