package quickCanResolver.core;

import quickCanResolver.dbc.CanDataEnum.CANByteOrder;
import quickCanResolver.dbc.CanDataEnum.CANDataType;
import quickCanResolver.dbc.CanDataEnum.CANMsgIdType;
import quickCanResolver.dbc.CanDataEnum.GroupType;
import quickCanResolver.dbc.CanDbc;
import quickCanResolver.dbc.CanMessage;
import quickCanResolver.dbc.CanSignal;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于处理将DBC文件解析为DBC对象，也就是反向操作。注意，暂时不支持报文分组，parseSG函数中，已经被我删去相关代码。
 */
public class DbcParse {
    static final String Vector__XXX = "Vector__XXX";
    static String regex = "SG_\\s*(?<sigName>\\b[a-zA-Z_]\\w*)\\s*(?<group>[mM]\\d*)?\\s*:" +
            "\\s*(?<startBit>\\d+)\\s*[|]\\s*(?<bitLength>\\d+)@(?<ByteOrder>[10])(?<DataType>[+-])"+
            "\\s*\\((?<Factor>-?\\b\\d[\\d.]*),(?<Offset>-?\\b\\d[\\d.]*)\\)\\s*\\[(?<min>-?\\b\\d[\\d.]*)\\|(?<max>-?\\b\\d[\\d.]*)\\]\\s*" +
            "\"(?<unit>[^\"]*)\"\\s*(?<nodeSet>[\\w,]+)";

    /**
     * 由于java代码在安卓环境无法直接读取文件，所以新增一个从InputStream中读取文件并解析的方法
     * @param dbcTag 标签
     * @param inputStream 文件输入流，请在外部手动获取和关闭。
     * @return 返回一个DBC对象
     */
    public static CanDbc getDbcFromInputStream(String dbcTag, InputStream inputStream) {
        // 预加载空的数据
        CanDbc dbc = CanDbc.getEmptyDbc(dbcTag);
        Map<Integer, CanMessage> messagesMap = dbc.getIntMsgMap();
        try (
                // 因为DBC文件在windows下是GBK格式，故强制使用该格式。
                InputStreamReader isr = new InputStreamReader(inputStream,"GBK");  //fis,"GBK"  fis
                BufferedReader buffer = new BufferedReader(isr)
        ){
            // 解析一行数据
            parseBuffer(dbc,messagesMap,buffer);
        } catch (IOException e) { // IO异常，获取CanMessage时，可能会发生 ExcelSheetException 异常
            throw new RuntimeException("DbcParse: 获取DBC文件发生错误，IO异常");
        }
        //System.out.println(" CAN通道信息打印 = \n"+dbc.getChannelInfo());
        return dbc;
    }
    /**
     * 从文件中获取DBC
     * @param dbcTag 标签
     * @param filePath 文件路径
     * @return 返回一个DBC对象
     */
    public static CanDbc getDbcFromFilePath(String dbcTag, String filePath) {
        if (! isFileDbc(filePath)) {
            throw new RuntimeException("DbcParse：获取DBC文件发生错误，该文件不是DBC文件");
        }
        // 预加载空的数据
        CanDbc dbc = CanDbc.getEmptyDbc(dbcTag);
        Map<Integer, CanMessage> messagesMap = dbc.getIntMsgMap();
        try (
                FileInputStream fis = new FileInputStream(filePath);
                // 因为DBC文件在windows下是GBK格式，故强制使用该格式。
                InputStreamReader isr = new InputStreamReader(fis,"GBK");  //fis,"GBK"  fis
                BufferedReader buffer = new BufferedReader(isr)
        ){
            // 解析一行数据
            parseBuffer(dbc,messagesMap,buffer);
        } catch (IOException e) { // IO异常，获取CanMessage时，可能会发生 ExcelSheetException 异常
            throw new RuntimeException("DbcParse：获取DBC文件发生错误，IO异常");
        }
        //System.out.println(" CAN通道信息打印 = \n"+dbc.getChannelInfo());
        return dbc;
    } //getDbcFromFile

    /**
     * 解析一行数据
     * @param dbc dbc
     * @param messagesMap messagesMap
     * @param buffer 一行数据
     * @throws IOException 抛出IO异常
     */
    private static void parseBuffer(CanDbc dbc,Map<Integer, CanMessage> messagesMap,BufferedReader buffer) throws IOException {
        String line;
        /* 依次得到每行内容 每行开头的内容分为以下9种： BU_: 节点 ; BO_  消息; SG_ 信号; BO_TX_BU_ 消息传输节点;
         * CM_ 注释; BA_DEF_ 自定义属性的定义; BA_DEF_DEF_ 自定义属性的初始值;
         * BA_ 自定义属性的具体值; VAL_ 值描述
         *  */
        Pattern startPattern = Pattern.compile("^(?<title>VERSION|BU_:|BO_|SG_|BO_TX_BU_|CM_|BA_DEF_|BA_DEF_DEF_|BA_|VAL_)\\s+"); // 匹配开头的正则表达式
        while ((line = buffer.readLine()) != null) {
            line = line.trim(); // 去除前后空格
            //System.out.println("line :   "+line);
            Matcher startMatch = startPattern.matcher(line); // 匹配一行的开头
            if (! startMatch.find()){ //没有找到则退出，执行下一次循环
                continue;
            }
            String lineStart = startMatch.group("title");
            switch (lineStart){
                case "VERSION":
                    System.out.println("DbcParse：DBC文件成功打开，版本信息为："+line);
                    break;
                case "BU_:":
                    dbc.addCanNodeSet(parseBU(line)); // 解析节点
                    break;
                case "BO_":
                    CanMessage msg = parseBO(line); // 解析消息
                    //如果现有的集合中找不到相应id的报文，则添加新报文,就是说不存在重复的id。正确的做法是，如果重复了就弹出报错。疲倦了，不想再多写代码了，之前生成dbc的时候已经写过一次了。
                    messagesMap.putIfAbsent(msg.getMsg_ID(), msg); //消息集合添加一条消息
                    break;
                case "SG_":
                    CanSignal sig = parseSG(line); // 解析当前信号 .可能抛出异常
                    CanMessage presentMsg = dbc.getMessageAtIndex(messagesMap.size()-1); // 获取前一个消息（已经添加到了map中）
                    if (presentMsg != null) {
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
    } // parseBuffer


    /**
     * 检查文件路径是否是dbc
     * @param filePath 文件路径
     * @return 返回校验结果 ，或抛出异常
     */
    private static boolean isFileDbc(String filePath) {
        File file = new File(filePath);
        if(!file.exists()){ //如果文件不存在，退出
            //System.out.println("如果文件不存在，退出");
            throw new RuntimeException("DbcParse：获取DBC文件发生错误，文件不存在");
            // 由返回 null ，改为了抛出异常，可以更加明显的让外部使用者知道哪里发生了错误
        }
        if(file.isDirectory()){//如果是目录，则退出
            //System.out.println("如果是目录，则退出");
            throw new RuntimeException("DbcParse：获取DBC文件发生错误，该文件是目录，而不是文件");
        }
        String fileName = file.getName();
        //获取文件完整名称,含后缀
        //获取文件扩展名
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        //截取最后一个.之后的字符串 .xlsx  .dbc
        //获取文件名 ， 不包含扩展名
        if (!fileExtension.equals(".dbc")) {
            //System.out.println("不是DBC文件，退出");
            throw new RuntimeException("DbcParse：获取DBC文件发生错误，该文件不是DBC文件");
        }
        return true;
    }

    /**
     * 解析信号，暂不支持报文分组。
     * @param line 传入单行数据，例如 : SG_ emb_forceSwitch_req : 8|1@1+ (1,0) [0|1] "" CanIOHandler    <br>
     *   例如 ： SG_ test_Signal_14 m2 : 24|8@1+ (0.1,-5.55) [-5|20.5] ""  CanIOHandler,CCS
     * @return 返回一个CAN信号 CanSignal
     */
    public static CanSignal parseSG(String line) {
        line = line.trim();
        if ( ! line.startsWith("SG_")){
            throw new RuntimeException("DbcParse：DBC文件识别异常,该行不是信号，原始数据line为:"+line);
        }

        Matcher sigMatch =  Pattern.compile(regex).matcher(line);
        if(! sigMatch.find()){
            throw new RuntimeException("DbcParse：DBC文件识别异常,原始数据line为:"+line);
        }
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
        if (groupStr != null) { //表示有分组
            Matcher m = Pattern.compile("(?<M>M)|(m(?<num>\\d+))").matcher(groupStr);
            if (m.find()){
                String groupM = m.group("M");
                String numStr = m.group("num");
                if (groupM != null){
                    // groupType = GroupType.Group_Flag;
                    // TODO 这里可能抛出异常，等后期再添加对报文分组类型的识别
                    throw new RuntimeException("DbcParse：DBC文件识别异常，暂不支持对CAN报文进行分组");
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
            Matcher nodeM = Pattern.compile("(?<node>\\b[a-zA-Z_]\\w*)").matcher(receiveNodeStr); //如  CanIOHandler,CCS
            while (nodeM.find()){
                String nodeItem = nodeM.group("node");
                if (nodeItem != null){
                    receiveNodeSet.add(nodeItem);
                }
            }
        }
        else {
            receiveNodeSet.add(Vector__XXX);
        }
        return new CanSignal(sigNameStr,groupType,groupNum,byteOrder,startBit,bitLength,dataType,factor,offset,min,max,unitStr,receiveNodeSet);
    } // parseSG

    /**
     * 解析消息
     * @param line 传入单行字符串，例如 : BO_ 2560107544 CCS7: 8 GW  ; BO_ 2147483921 MotorMessage: 8 Vector__XXX
     * @return 返回一个CAN消息 CanMessage
     */
    public static CanMessage parseBO(String line) {
        line = line.trim();
        if ( ! line.startsWith("BO_")){
            throw new RuntimeException("DbcParse：DBC文件识别异常，该行不是CAN消息，原始line="+line);
        }
        Pattern msgPattern = Pattern.compile("BO_\\s*(?<longIdCode>\\d+)\\s*(?<msgName>\\b[a-zA-Z_]\\w*)\\s*:\\s*(?<length>\\d)\\s*(?<node>\\b[a-zA-Z_]\\w*)");
        Matcher msgMatch = msgPattern.matcher(line);
        if (! msgMatch.find()) {
            throw new RuntimeException("DbcParse：DBC文件识别异常，原始line="+line);
        }
        String strIdCode = msgMatch.group("longIdCode");// 需要转换格式
        String msgName = msgMatch.group("msgName");
        String strLength = msgMatch.group("length"); // 需要转换格式
        String strNode = msgMatch.group("node");
        //System.out.println("strIdCode = "+strIdCode+" , msgName = "+msgName+" , strLength = "+strLength+" , strNode = "+strNode);
        long longIdCode = Long.parseLong(strIdCode,10); // 10进制。因为实际存的是long格式，所以必须用long来接收
        CANMsgIdType msgIdType = recognizeMsgID(longIdCode); //根据id识别扩展帧和标准帧
        int msgId;
        if (msgIdType == CANMsgIdType.Extended) { // id需要转换
            msgId = CanMessage.transIdCodeToID(longIdCode); // 扩展帧需要将long格式 计算后 转换为 int
        }
        else {
            msgId = Math.toIntExact(longIdCode); // 标准帧直接添加 id = longIdCode
        }
        int msgLength = Integer.parseInt(strLength);
        if (strNode == null){
            strNode = Vector__XXX;
        }
        return new CanMessage(msgName,msgId,longIdCode,msgIdType,msgLength,strNode);
        //System.out.println(" msg = "+msg.getMsgBaseInfo());
    } // parseBO

    /**
     * 根据id识别扩展帧和标准帧。<br>
     * 帧ID类型 标准帧 Standard 范围 0x0~0x7FF ; <br> 扩展帧 Extended 范围 0x0~0x1FFF_FFFF ;<br>
     * @param msgID msgID
     * @return 返回报文ID的类型。 CANMsgIdType
     */
    public static CANMsgIdType recognizeMsgID(long msgID) {
        if (msgID > 0x7FF){
            return CANMsgIdType.Extended;
        }
        else {
            return CANMsgIdType.Standard;
        }
    }

    /**
     * 解析节点
     * @param line 传入单行字符串，例如 : BU_: GW CCS CanIOHandler VCU
     * @return 返回节点列表
     */
    public static Set<String> parseBU(String line) {
        Set<String> nodeSet = new HashSet<>();
        line = line.trim();
        if ( ! line.startsWith("BU_:")){// 识别节点信息
            throw new RuntimeException("DbcParse：DBC文件识别异常，该行不是节点信息，原始line="+line);
        }
        /* 正则表达式，用于解析文件 */
        Pattern nodePattern = Pattern.compile("(\\s+(?<node>[a-zA-Z_]*))");
        Matcher nodeM = nodePattern.matcher(line);
        while (nodeM.find()) {
            String nodeTempValue = nodeM.group("node");
            nodeSet.add(nodeTempValue);
            //System.out.println("节点："+nodeTempValue);
        }
        return nodeSet;
    }
    protected DbcParse() {

    }
}
