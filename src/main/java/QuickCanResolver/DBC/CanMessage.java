package QuickCanResolver.DBC;

import QuickCanResolver.DBC.CanDataEnum.CANMsgIdType;
import QuickCanResolver.DBC.CanDataEnum.MsgSendType;
import QuickCanResolver.DBC.CanDataEnum.MsgType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于描述消息 Message
 */
public class CanMessage {  //编程第一步，定义数据结构
    /** 报文名称 ,非空*/
    protected final String msgName ;
    /** 报文类型 默认为普通 ; <br>
     * 可选值 Normal, Diagnosis ,NetworkManagement ;<br>
     * 其中，normal 类型又分为 标准帧 Standard 和 扩展帧 Extended ;<br>
     * 又其中， Normal 类型程序不做任何处理; Diagnosis 和 NetworkManagement 类型，程序会进行判断。<br>
     * @deprecated 弃用，暂时不打算增加对诊断报文的识别
     * */
    @Deprecated
    protected MsgType msgType = MsgType.Normal;
    /** 报文标识符 */
    protected final int msg_ID ;
    /** 报文标识符的DBC编码。<br>标准帧就等于报文标识符。<br>扩展帧等于报文标识符  + 0x8000_0000L */
    protected final long msgIDCode;
    /** 信号类型默认为 扩展帧 Extended 。<br> 可选值为 Standard 和 Extended 。<br>标准帧 Standard 范围 0x0~0x7FF ; <br>扩展帧 Extended 范围 0x0~0x1FFF_FFFF 。*/
    protected final CANMsgIdType msgIdType ;
    /** 报文发送类型 默认为周期型 。TODO 暂时不打算更新对周期型的识别 */
    protected MsgSendType msgSendType = MsgSendType.Cycle;
    /** 报文周期时间 毫秒 。 TODO 预留，暂时不打算识别 */
    protected int msgCycleTime ;
    /** 报文长度 单位: byte*/
    private final int msgLength ;  //MsgLength(Byte)
    /** 报文注释 */
    protected String msgComment = "" ;
    protected final String nodeName  ;
    /** 发送节点 , 当前报文的节点名称, 节点名称默认为 Vector__XXX*/
    protected Set<String> msgSendNodeList = new HashSet<>();
    /** 信号列表 ; 键指信号的名称, 值指的是信号 */
    protected final Map<String, CanSignal> signalMap = new ConcurrentHashMap<>();



    public int getMsg_ID() {
        return msg_ID;
    }
    public Map <String, CanSignal> getSignalMap() {
        return signalMap;
    }
    @Override
    public String toString() {
        return "CanMessage{CAN报文名称:"+msgName+"}";
    }  //toString()
    /**
     * 获取报文基本信息
     * @return 返回字符串
     */
    public String getMsgBaseInfo() {
        return  "\n--消息名称:"+msgName+",报文标识符:"
                +Integer.toString(Math.toIntExact(msg_ID),16)+",信号ID类型:"+msgIdType+
                ",报文发送类型:"+msgSendType+",报文周期(单位:毫秒):"+msgCycleTime+",报文长度(单位byte):"+ msgLength +
                ",报文注释:"+ msgComment +",报文发送节点:"+ nodeName +",消息所含信号数量:"+signalMap.size()+"\n";
    }
    /**
     * 获取报文中，一个报文所有信号的值。用于校验数据是否修改成功。
     * @return 返回字符串
     */
    public String getMsgValue(){
        StringBuilder builder = new StringBuilder();
        builder.append("报文名称：").append(msgName).append(";\n");
        signalMap.values().forEach(sig -> builder.append("信号 : ").append(sig.getSignalName()).append(" = ").append(sig.readValue()).append(";\n"));
        return builder.toString();
    }
    public CanMessage(String msgName, int msg_ID, long msgIDCode, CANMsgIdType msgIdType, int msgLength, String nodeName) {
        this.msgName = msgName;
        this.msg_ID = msg_ID;
        this.msgIDCode = msgIDCode;
        this.msgIdType = msgIdType;
        this.msgLength = msgLength;
        this.nodeName = nodeName;
    }
    /**
     * 转换 idCode至 id。公式如下 : msgIDCode = msg_ID + 0x8000_0000L ;  //仅针对扩展帧
     * @param msgIDCode 当扩展帧时，DBC文件中的id值
     * @return 返回真实id值
     */
    public static int transIdCodeToID(long msgIDCode){
        return Math.toIntExact(msgIDCode - 0x8000_0000L);
    }

    /**
     * 转换 idCode至 id。公式如下 : msgIDCode = msg_ID + 0x8000_0000L ;  仅针对扩展帧
     * @param msg_ID 真实id值
     * @return 返回 当扩展帧时，DBC文件中的id值
     */
    public static long transIdToIdCode(long msg_ID){
        return msg_ID + 0x8000_0000L ;
    }
    public String getNodeName() {
        return nodeName;
    }
    public MsgSendType getMsgSendType() {
        return msgSendType;
    }
    public int getMsgCycleTime() {
        return msgCycleTime;
    }
    public int getMsgLength() {
        return msgLength;
    }
    public String getMsgComment() {
        return msgComment;
    }
    public Set<String> getMsgSendNodeList() {
        return msgSendNodeList;
    }
    public CANMsgIdType getMsgIdType() {
        return msgIdType;
    }
    public String getMsgName() {
        return msgName;
    }
    public long getMsgIDCode(){
        return msgIDCode;
    }
}  // class  CanMessage
