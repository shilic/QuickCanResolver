package QuickCanResolver.DBC;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *  单个dbc对象
 */
public class CanDbc {
    /** 节点列表 Set集合不重复*/
    Set<String> canNodeSet;
    /** 消息列表 。键记录消息ID值，值记录消息的对象 LinkedHashMap ，记录的是 newMsgIDCode，注意*/
    Map<Integer, CanMessage> intMsgMap;


    /**
     * 根据Map插入顺序获取 DBC Message对象。实际用于在添加信号sig时，获取刚插入的消息msg。并将sig添加到msg中。
     * @param index 你想要获取的索引值
     * @return 返回 CanMessage 对象 。如果查找的对象超出范围，返回 null 。
     */
    public CanMessage getMessageAtIndex(int index) {
        int size = intMsgMap.size();
        CanMessage msg = null;
        if ( index < 0 || index >= size ){ // index 范围为 0 到 size-1 (不包含size)
            return null;
        }
        int i=0;
        for (Map.Entry<Integer, CanMessage> entry : intMsgMap.entrySet()) {
            if (i == index){
                msg = entry.getValue();
                break;
            }else {
                i++;
            }
        }
        return msg;
    }

    /**
     * 根据ID获取报文，从map中查询
     * @param msgId 报文id
     * @return 返回一个报文对象
     */
    public CanMessage getMsg(int msgId){
        return intMsgMap.get(msgId);
    }

    /**
     * 根据信号名称，获取一个信号。<br>使用并行流，同时查询所有msg.
     * @param signalTag 信号标签
     * @return 返回一个信号
     */
    public CanSignal getSignal(String signalTag) {
//        final CanSignal[] signals = new CanSignal[1];
//        signals[0] = null;

//        intMsgMap.values().parallelStream().forEach(msg -> {
//            if (msg.getSignalMap().get(signalTag) != null){
//                signals[0] = msg.getSignalMap().get(signalTag);
//            }
//        });
        CanSignal mSig = null;
        // 因为缺少了 messageTag ，故这里需要多一个步骤。
        for (CanMessage mMsg : intMsgMap.values()) {
            CanSignal temp = mMsg.getSignalMap().get(signalTag); // 这里如果在map中没有查询到，仍然有可能返回一个null
            if (temp != null){
                mSig = temp;
                break;
            }
        }
        return mSig;
    }

    /**
     * 根据信号名称和报文id，获取一个信号。
     * @param signalTag 信号标签
     * @param messageTag 报文id
     * @return 返回一个信号。
     */
    public CanSignal getSignal(String signalTag,int messageTag) {
        CanMessage msg = intMsgMap.get(messageTag);
        if (msg!= null){
            return msg.getSignalMap().get(signalTag); // 这里如果在map中没有查询到，仍然有可能返回一个null
        }
        return null;
    }

    /**
     * 获取节点信息，用于调试
     * @return 返回字符串
     */
    public String getCanNodeInfo(){
        return canNodeSet.toString();
    }

    /**
     * 获取CAN通道所有报文信息。
     * @return 返回字符串
     */
    public String getChannelInfo(){
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, CanMessage> entry : intMsgMap.entrySet()){
            CanMessage msg = entry.getValue();
            builder.append(msg.getMsgBaseInfo());
            for (Map.Entry<String, CanSignal> entrySig : msg.getSignalMap().entrySet()){
                CanSignal signal = entrySig.getValue();
                builder.append(signal.getSignalInfo());
            }
        }
        return builder.toString();
    }
    protected CanDbc(){
        canNodeSet = new HashSet<>();
        intMsgMap = new LinkedHashMap<>();
    }
    public static CanDbc getEmptyDbc(){
        return new CanDbc();
    }

    public void addCanNodeSet(Set<String> data){
        canNodeSet.addAll(data);
    }

    public Map<Integer, CanMessage> getIntMsgMap() {
        return intMsgMap;
    }
}  ///class CANChannels
