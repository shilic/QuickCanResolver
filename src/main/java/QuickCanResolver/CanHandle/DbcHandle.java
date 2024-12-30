package QuickCanResolver.CanHandle;

import QuickCanResolver.CanDataEnum.CANByteOrder;
import QuickCanResolver.CanDataEnum.CANDataType;
import QuickCanResolver.CanDataEnum.CANMsgIdType;
import QuickCanResolver.CanDataEnum.GroupType;
import QuickCanResolver.DBC.CanChannel;
import QuickCanResolver.DBC.CanMessage;
import QuickCanResolver.DBC.CanSignal;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于处理将DBC文件解析为DBC对象，也就是反向操作。注意，暂时不支持报文分组，parseSG函数中，已经被我删去相关代码。
 */
public class DbcHandle {
    public static CanChannel getDbcFromFile(String filePath) {
        File file = new File(filePath);
        if(!file.exists()){ //如果文件不存在，退出
            System.out.println("如果文件不存在，退出");
            return null;
        }
        if(file.isDirectory()){//如果是目录，则退出
            System.out.println("如果是目录，则退出");
            return null;
        }
        String fileName = file.getName();   //获取文件完整名称,含后缀
        //获取文件父级文件夹的连接
        //获取文件扩展名
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));   //截取最后一个.之后的字符串 .xlsx  .dbc
        //获取文件名 ， 不包含扩展名
        //获取下标0到最后一个点之前的文件名
        //Matcher dbcMatcher = Pattern.compile("dbc").matcher(fileExtension);
        if (!fileExtension.equals(".dbc")){ //不是DBC文件，退出  !fileExtension.equals("dbc")    !dbcMatcher.find()
            System.out.println("不是DBC文件，退出");
            return null;
        }
        CanChannel dbc = CanChannel.getEmptyDbc();
        Map<Integer, CanMessage> messagesMap = dbc.getIntMsgMap();
        try (
                FileInputStream fis = new FileInputStream(filePath);
                InputStreamReader isr = new InputStreamReader(fis,"GBK");  //fis,"GBK"  fis
                BufferedReader br = new BufferedReader(isr)
                ){
            String line;
            //System.out.println("开始读取DBC文件");
            /* 依次得到每行内容 每行开头的内容分为以下9种： BU_: 节点 ; BO_  消息; SG_ 信号; BO_TX_BU_ 消息传输节点;
             * CM_ 注释; BA_DEF_ 自定义属性的定义; BA_DEF_DEF_ 自定义属性的初始值;
             * BA_ 自定义属性的具体值; VAL_ 值描述
             *  */
            Pattern startPattern = Pattern.compile("^(?<title>BU_:|BO_|SG_|BO_TX_BU_|CM_|BA_DEF_|BA_DEF_DEF_|BA_|VAL_)\\s+"); // 匹配开头的正则表达式
            while ((line = br.readLine()) != null) {
                line = line.trim(); // 去除前后空格
                //System.out.println("line :   "+line);
                Matcher startMatch = startPattern.matcher(line); // 匹配一行的开头
                if (! startMatch.find()){ //没有找到则退出，执行下一次循环
                    continue;
                }
                String lineStart = startMatch.group("title");
                switch (lineStart){
                    case "BU_:":
                        dbc.addCanNodeSet(parseBU(line)); // 解析节点
                        break;
                    case "BO_":
                        CanMessage msg = parseBO(line); // 解析消息
                        if (msg != null){
                            // 在输入之前，校验id是否重复
                            //long newMsgIDCode = msg.getMsgIDCode();
                            //如果现有的集合中找不到相应id的报文，则添加新报文,就是说不存在重复的id。正确的做法是，如果重复了就弹出报错。疲倦了，不想再多写代码了，之前生成dbc的时候已经写过一次了。
                            messagesMap.putIfAbsent(msg.getMsg_ID(), msg); //消息集合添加一条消息
                        }
                        break;
                    case "SG_":
                        CanSignal sig = parseSG(line); // 解析当前信号 .可能抛出异常
                        CanMessage presentMsg = dbc.getMessageAtIndex(messagesMap.size()-1); // 获取前一个消息（已经添加到了map中）
                        if (sig != null && presentMsg != null) {
                            //添加信号到对应的消息,添加信号前，需要校验是否重复。
                            Map<String, CanSignal> signalMap = presentMsg.getSignalMap();
                            String sigName = sig.getSignalName();
                            signalMap.putIfAbsent(sigName, sig);
                        } // sig != null
                        break;
                    default:
                        break;
                } //switch (lineStart)
                //testRegex1(line);
            } // while 循环读取文件
        } catch (IOException e) { // IO异常，获取CanMessage时，可能会发生 ExcelSheetException 异常
            e.printStackTrace();
        }
        //System.out.println(" CAN通道信息打印 = \n"+dbc.getChannelInfo());
        return dbc;
    } //getDbcFromFile

    /**
     * 解析信号，暂不支持报文分组。
     * @param line 传入单行数据，例如 : SG_ emb_forceSwitch_req : 8|1@1+ (1,0) [0|1] "" CanIO    <br>
     *   SG_ test_Signal_14 m2 : 24|8@1+ (0.1,-5.55) [-5|20.5] ""  CanIO,CCS
     * @return 返回一个CAN信号 CanSignal
     */
    public static CanSignal parseSG(String line) {
        line = line.trim();
        if ( ! line.startsWith("SG_")){
            return null;
        }
        String regex = "SG_\\s*(?<sigName>\\b[a-zA-Z_]\\w*)\\s*(?<group>[mM]\\d*)?\\s*:" +
                "\\s*(?<startBit>\\d+)\\s*[|]\\s*(?<bitLength>\\d+)@(?<ByteOrder>[10])(?<DataType>[+-])"+
                "\\s*\\((?<Factor>-?\\b\\d[\\d.]*),(?<Offset>-?\\b\\d[\\d.]*)\\)\\s*\\[(?<min>-?\\b\\d[\\d.]*)\\|(?<max>-?\\b\\d[\\d.]*)\\]\\s*" +
                "\"(?<unit>[^\"]*)\"\\s*(?<nodeSet>[\\w,]+)";
        Pattern sigPattern = Pattern.compile(regex);
        Matcher sigMatch = sigPattern.matcher(line);
        CanSignal signal;
        if (sigMatch.find()){
            /* 原始数据（待解析） */
            String sigNameStr = sigMatch.group("sigName");
            String groupStr = sigMatch.group("group");
            String startBitStr = sigMatch.group("startBit");
            String bitLengthStr = sigMatch.group("bitLength");
            String ByteOrderStr = sigMatch.group("ByteOrder");
            String DataTypeStr = sigMatch.group("DataType");

            String FactorStr = sigMatch.group("Factor");
            String OffsetStr = sigMatch.group("Offset");
            String minStr = sigMatch.group("min");
            String maxStr = sigMatch.group("max");
            String unitStr = sigMatch.group("unit");
            String receiveNodeStr = sigMatch.group("nodeSet");
            /* 解析数据 */
            int groupNum = -1 ;
            GroupType groupType = GroupType.Default_Group;
            if (groupStr != null){ //表示有分组
                Matcher m = Pattern.compile("(?<M>M)|(m(?<num>\\d+))").matcher(groupStr);
                if (m.find()){
                    String groupM = m.group("M");
                    String numStr = m.group("num");
                    if (groupM != null){
                        groupType = GroupType.Group_Flag;
                        /* 这里可能抛出异常，等后期再添加对报文分组类型的识别 */
                        //throw new RuntimeException("暂不支持对CAN报文进行分组");
                    }
                    if (numStr != null){
                        groupType = GroupType.Group_Number;
                        groupNum = Integer.parseInt(numStr);
                    }
                }
            }
            int startBit = Integer.parseInt(startBitStr);/**/
            int bitLength = Integer.parseInt(bitLengthStr);/**/
            CANByteOrder byteOrder = CANByteOrder.Intel;
            if (ByteOrderStr.equals("0")){ // 英特尔格式直接省略，因为默认值就是英特尔了，减少程序判断次数。
                byteOrder = CANByteOrder.Motorola_MSB;
            }
            CANDataType dataType = CANDataType.Unsigned;
            if (DataTypeStr.equals("-")){ // 有符号类型。无符号类型同样省略
                dataType = CANDataType.Signed;
            }
            double factor = Double.parseDouble(FactorStr);
            double offset = Double.parseDouble(OffsetStr);
            double min = Double.parseDouble(minStr);
            double max = Double.parseDouble(maxStr);
            if (unitStr == null){
                unitStr = "";
            }
            Set<String> receiveNodeSet  = new HashSet<>();
            if (receiveNodeStr != null){
                Matcher nodeM = Pattern.compile("(?<node>\\b[a-zA-Z_]\\w*)").matcher(receiveNodeStr); //如  CanIO,CCS
                while (nodeM.find()){
                    String nodeItem = nodeM.group("node");
                    if (nodeItem != null){
                        receiveNodeSet.add(nodeItem);
                    }
                }
            }
            else {
                receiveNodeSet.add("Vector__XXX");
            }
            signal = new CanSignal(sigNameStr,groupType,groupNum,byteOrder,startBit,bitLength,dataType,factor,offset,min,max,unitStr,receiveNodeSet);
        }
        else {
            System.out.println("信号解析失败,原始数据line为:"+line);
            return null;
        }
        //System.out.println(" sig = "+signal.getSignalInfo());
        return signal;
    }

    /**
     * 解析消息
     * @param line 传入单行字符串，例如 : BO_ 2560107544 CCS7: 8 GW  ; BO_ 2147483921 MotorMessage: 8 Vector__XXX
     * @return 返回一个CAN消息 CanMessage
     */
    public static CanMessage parseBO(String line){
        line = line.trim();
        if ( ! line.startsWith("BO_")){
            return null;
        }
        Pattern msgPattern = Pattern.compile("BO_\\s*(?<longIdCode>\\d+)\\s*(?<msgName>\\b[a-zA-Z_]\\w*)\\s*:\\s*(?<length>\\d)\\s*(?<node>\\b[a-zA-Z_]\\w*)");
        Matcher msgMatch = msgPattern.matcher(line);
        CanMessage msg ;
        if (msgMatch.find()){
            String strIdCode = msgMatch.group("longIdCode");// 需要转换格式
            String msgName = msgMatch.group("msgName");
            String strLength = msgMatch.group("length"); // 需要转换格式
            String strNode = msgMatch.group("node");
            //System.out.println("strIdCode = "+strIdCode+" , msgName = "+msgName+" , strLength = "+strLength+" , strNode = "+strNode);
            long longIdCode = Long.parseLong(strIdCode,10); // 10进制
            CANMsgIdType msgIdType = recognizeMsgID(longIdCode); //根据id识别扩展帧和标准帧
            int msgId;
            //CANMsgIdType idType = CANMsgIdType.Extended;
            if (msgIdType == CANMsgIdType.Extended) { // id需要转换
                msgId = CanMessage.transIdCodeToID(longIdCode);
            }
            else {
                msgId = Math.toIntExact(longIdCode); // 标准帧直接添加 id = longIdCode
            }
            int msgLength = Integer.parseInt(strLength);
            if (strNode == null){
                strNode = "Vector__XXX";
            }
            msg = new CanMessage(msgName,msgId,longIdCode,msgIdType,msgLength,strNode);
        }
        else {
            System.out.println("消息解析失败");
            return null;
        }
        //System.out.println(" msg = "+msg.getMsgBaseInfo());
        return msg;
    } // parseBO

    /**
     * 根据id识别扩展帧和标准帧。<br>
     * 帧ID类型 标准帧 Standard 范围 0x0~0x7FF ; <br> 扩展帧 Extended 范围 0x0~0x1FFF_FFFF ;<br>
     * @return 返回报文ID的类型。 CANMsgIdType
     */
    public static CANMsgIdType recognizeMsgID(long msgID){
        if (msgID > 0x7FF){
            return CANMsgIdType.Extended;
        }
        else {
            return CANMsgIdType.Standard;
        }
    }

    /**
     * 解析节点
     * @param line 传入单行字符串，例如 : BU_: GW CCS CanIO VCU
     * @return 返回节点列表
     */
    public static Set<String> parseBU(String line){
        Set<String> nodeSet = new HashSet<>();
        line = line.trim();
        if ( ! line.startsWith("BU_:")){// 识别节点信息
            return null;
        }
        /* 正则表达式，用于解析文件 */
        Pattern nodePattern = Pattern.compile("(\\s+(?<node>[a-zA-Z]*))");
        Matcher nodeM = nodePattern.matcher(line);
        while (nodeM.find()) {
            String nodeTempValue = nodeM.group("node");
            nodeSet.add(nodeTempValue);
            //System.out.println("节点："+nodeTempValue);
        }
        return nodeSet;
    }
}