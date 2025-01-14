# QuickCanResolver-车身CAN报文快速解析框架
*摘要：该项目用于车机控制app，核心思想是使用了注解和反射，用于快速解析数组格式的车身CAN报文至车身数据模型中，也可以快速由数据模型封装出一帧CAN报文出来。*

# 一、前情提要or基础知识具备

## 1.CAN报文是什么？

CAN总线是一种用于汽车网络的通信方式。这里附了一个连接，可以简单看一下。

[一文读懂CAN总线协议 (超详细配34张高清图)_can总线协议详解-CSDN博客](https://blog.csdn.net/qq_35057766/article/details/135580884)

CAN总线的两根信号线通常采用的是双绞线，传输的是差分信号，通过两根信号线的电压差CANH-CANL来表示总线电平。这里只关心应用层的东西，不再细说。一帧CAN报文通常含有64it的数据，也就是8个Byte的数组，还有一个报文ID用于表示这帧报文。如下图所示。

![image-20240112073255140](https://raw.githubusercontent.com/shilic/MarkDownImageRepository/main/img/aa48abfb39c952e5a31b3726c1990210.png)

我们这里只关心仲裁段的ID和数据段的64bit数据即可，通常底层会将数据处理好，只把ID和数据发过来。ID用于标记这是哪一个信号。而64bit的数据中则包含了车身的各种数据，例如车速、电机转速、空调温度等，并且他们在这64bit中占据的长度不一致，并且位置也不一致，他们一起组合在一起，构成一帧报文。如下图所示。

![image-20241230205702856](https://raw.githubusercontent.com/shilic/MarkDownImageRepository/main/img/image-20241230205702856.png)



## 2.DBC是什么？

如上图所示，横轴和纵轴都是8个，8*8共64个bit，不同的颜色标记了不同的车身数据，而应用层所需要做的事就是从这64比特的矩阵中取出数据，并解析。而描述这些数据在哪一个位置占据多少长度的文件，就叫DBC文件，全称DataBaseCan，顾名思义，就是描述CAN报文的数据库文件，以.dbc结尾。使用文本格式打开后，长下边这样。

```dbc
BU_: CCS AC

BO_ 2560107544 CCSToAC1: 8 CCS
 SG_ CCSToAC1_FactoryID : 0|8@1+ (1,0) [0|255] "" AC
 SG_ CCSToAC1_AirSw : 8|2@1+ (1,0) [0|3] "" AC
 SG_ CCSToCabin1_ColdGearReq : 10|4@1+ (1,0) [0|15] "" AC
 SG_ CCSToAC1_FanGearReq : 14|4@1+ (1,0) [0|15] "" AC
 SG_ heart : 56|8@1+ (1,0) [0|255] "" AC

BO_ 2560104484 ACToCCS1: 8 AC
 SG_ CabinToCCS1_FactoryID : 0|8@1+ (1,0) [0|255] "" CCS
 SG_ CabinToCCS1_CabinTemp : 8|8@1+ (1,-50) [-50|205] "" CCS
 SG_ CabinToCCS1_ColdGearSts : 16|4@1+ (1,0) [0|15] "" CCS
 SG_ CabinToCCS1_FanGearSts : 20|4@1+ (1,0) [0|15] "℃" CCS
 SG_ CabinToCCS1_FanMotFlt : 24|2@1+ (1,0) [0|3] "" CCS
 SG_ CabinToCCS1_ColdMotFlt : 26|2@1+ (1,0) [0|3] "" CCS
 SG_ CabinToCCS1_AirSts : 28|2@1+ (1,0) [0|3] "" CCS
 SG_ CabinToCCS1_RollCnt : 56|8@1+ (1,0) [0|255] "" CCS
```

## 3、如何解析DBC文件

上边这个就是DBC文件了，BO_表示一个消息，你也可以称之为报文，或Message；SG_表示这个消息中的一个信号，或signal；一个消息包含若干个信号，共同组成这64bit数据。

- 2560107544是十进制格式的报文ID编码；
- CCSToAC1是报文名称；
- 8指报文长度是8个Byte；
- CCS指节点名称，通常指这个消息是由哪个设备发出来的，也就是节点。

2560107544是十进制格式的报文ID编码，需要转换成16进制后，才是我们通常用到的ID的编码，这里等于 0x98982418，报文通常又分为两种，扩展帧和标准帧，扩展帧范围0到0X0x1FFF_FFFF，标准帧范围0到0x7FF。通常我们将报文ID大于0X7FF的一律当成扩展帧。而0x98982418还要减一个0X800_0000才得到最终的ID=0X18982418。

OK，我们来看下边一行，下边一行就是信号。

- CabinToCCS1_FactoryID指信号名称；
- 0|8指这个信号从矩阵中的0开始存，记8位，同理24|2指信号从数组下标的24开始记，记2位，最高位63，没有64。
- @不用管，是一个标记符号。
- 1+，其中1指英特尔格式，如果是0就是指摩托罗拉格式。英特尔格式，也就是小端排序，数据的低位存放在硬件储存空间中的低位，高位存放在高位。而摩托罗拉格式这反过来，也就是大端排序，数据的低位，需要存放在储存空间的高位，反之同理。
- 1+，其中的 + 值无符号格式，也就是unsigin，而如果是  -  就表示有符号格式，通俗易懂的讲，有符号就是允许存在负数。但是通常在汽车行业中都是采用无符号格式，使用精度和偏移量来表示负数。

- (1,-50)就表示一组精度和偏移量。我们在CAN总线中传输的是16进制的未处理值（也称原始值），需要处理过后得到物理值才拿到应用层使用。其中计算公式是

```
物理值 = 原始值 * factor（精度） + offset （偏移量）
```

比如这个时候的(1,-50)，我们拿到了未处理的十进制的车速70，那么实际车速就是70*1-50=20。ok，我们看下一条。

- [-50|205]，也就指最小值和最大值，通常会根据精度和偏移量以及信号长度计算得来，比如刚才这个信号，长度8，那么未处理值的范围也就是0到2^(8)-1，也就是0到255，对最小值和最大值分别进行精度偏移量的计算，得到范围-50到205。如果是自定义范围，那么必须在这个范围内，不可以超出范围。
- 双引号内部的指单位，后边CCS的指信号的接收节点，很容易理解。

# 二、定义数据结构

首先是DBC类，用于描述整个DBC文件，然后是消息类，一个DBC类包含若干个消息；紧接着是信号，一个消息包含若干个信号。

```java
/**
 *  单个dbc对象
 */
public class CanChannel {
    /** 节点列表 Set集合不重复*/
    Set<String> canNodeSet = new HashSet<>();
    /** 消息列表 。键记录消息ID值，值记录消息的对象 LinkedHashMap ，记录的是 newMsgIDCode，注意*/
    Map<Integer, CanMessage> intMsgMap = new LinkedHashMap<>();
}
```

DBC类使用一个LinkedHashMap来存储多个消息，键记录报文ID。然后是消息类

```java
/**
 * 用于描述消息 Message
 */
public class CanMessage {  //编程第一步，定义数据结构
    /** 报文名称 ,非空*/
    protected final String msgName ;
    @Deprecated
    protected MsgType msgType = MsgType.Normal;
    /** 报文标识符 */
    protected final int msg_ID ;
    /** 报文标识符的DBC编码。<br>标准帧就等于报文标识符。<br>扩展帧等于报文标识符  + 0x8000_0000L */
    protected final long msgIDCode;
    /** 信号类型默认为 扩展帧 Extended 。<br> 可选值为 Standard 和 Extended 。<br>标准帧 Standard 范围 0x0~0x7FF ; <br>扩展帧 Extended 范围 0x0~0x1FFF_FFFF 。*/
    protected final CANMsgIdType msgIdType ;
    /** 报文发送类型 默认为周期型 . 暂时不打算更新对周期型的识别*/
    protected MsgSendType msgSendType = MsgSendType.Cycle;
    /** 报文周期时间 毫秒 。 预留，暂时不打算识别*/
    protected int msgCycleTime ;
    /** 报文长度 单位: byte*/
    private final int msgLength ;  //MsgLength(Byte)
    /** 报文注释*/
    protected String msgComment = "" ;
    protected final String nodeName  ;
    /** 发送节点 , 当前报文的节点名称, 节点名称默认为 Vector__XXX*/
    protected Set<String> msgSendNodeList = new HashSet<>();
    /** 信号列表 ; 键指信号的名称, 值指的是信号 */
    protected Map<String, CanSignal> signalMap = new ConcurrentHashMap<>();

    public int getMsg_ID() {
        return msg_ID;
    }
    public Map <String, CanSignal> getSignalMap() {
        return signalMap;
    }
    @Override
    public String toString() {
        return " {消息名称:"+msgName+"}";
    }  //toString()
    }
```

消息类中使用一个ConcurrentHashMap来储存信号，键是信号名称。

```java
/**
 * 用于描述单个信号
 */
public class CanSignal {
    protected static final String Vector__XXX = "Vector__XXX";
    /** 信号名称*/
    protected final String signalName;
    /** 分组类型。 默认不启用多路复用 。分为默认分组(不分组)、分组标志位、组号，三个类型*/
    protected final GroupType groupType ;
    /** 多路复用实际值 M m2 ,组号。 <br>
     * 1、空白 表示默认分组，或者说不分组。 默认不分组。<br>
     * 2、大写的M 表示这个信号是分组标志位。 一个报文中只可以存在一个分组标志位。<br>
     * 3、小写的m加一个数字，表示组号。有组号，则该消息中必须存在分组标志位。*/
    protected String strGroupValue = "";
    /** 组号。用于多路复用时，区分不同分组。 */
    protected final int groupNumber ;
    /** 信号注释 */
    protected String signalComment = "";
    /** 排列格式 ，Intel 或者 Motorola 。排列方式默认为英特尔模式 Intel */
    protected final CANByteOrder byteOrder ; // 定义排列格式，并默认为英特尔格式
    /** 起始位 bit ; <br> 注意，当数据排列格式为motorola时，无论是MSB还是LSB，存入其中的起始位只能是 MSB 的位置。 */
    protected final int startBit;
    /** 信号长度 BitLength(Bit) 会用于最大值最小值的计算。*/
    protected final int bitLength;
    /** CAN数据类型,无符号和有符号，默认无符号; */
    protected final CANDataType dataType ;
    /** 精度(精度不可以为0，否则无意义) ; 物理值 = 原始值 * factor + offset */
    protected final double factor ;
    /** 偏移量 (通常为负数) ； 物理值 = 原始值 * factor + offset  */
    protected final double offset ;
    /** 物理最小值*/
    protected final double signalMinValuePhys ;
    /** 物理最大值*/
    protected final double signalMaxValuePhys ;
    /** 物理初始值 double rawValue = (sig.getInitialValuePhys()-sig.getOffset())  /  sig.getFactor(); */
    protected double iniValuePhys = 0;
    /** 总线初始值 */
    protected double iniValueHex = 0;
    /** 当前信号的值 */
    public volatile double currentValue = 0;
    /** 值是否无效 ,true表示有效，*/
    protected volatile boolean valid = true;
    /** 单位*/
    protected final String unit ;
    /** 接收节点列表 默认值 Vector__XXX*/
    final Set<String> sigReceiveNodeSet ;
    /** 用于标记该信号属于哪个数据模型 */
    Object target;
    /** 用于标记属于哪个字段 */
    Field field;
    }
```

然后就是信号，总体上除了每个对象单独记录自己的东西之外，还用了一个map来记录下一级的数据，形成树形结构，同时使用了final字段来保存固定的数据，方式数据意外修改，值得注意的是

```java
/** 用于标记该信号属于哪个数据模型 */
    Object target;
    /** 用于标记属于哪个字段 */
    Field field;
```

这两个数据等会，会用于绑定车身数据模型，而field字段正是通过反射获取的字段，讲其和DBC文件绑定到一起。

# 三、解析DBC文件

ok，既然数据是一行一行在存的，那么我用文件流一行一行读取就行了，循环读取每一行，最大程度节约内存。

这里的解析代码如下：

```java
"^(?<title>BU_:|BO_|SG_|BO_TX_BU_|CM_|BA_DEF_|BA_DEF_DEF_|BA_|VAL_)\\s+"
```

首先去掉每一行开头和结尾的空格，然后用这样一串正则表达式去匹配开头，根据匹配结果，再去调用不同的解析函数

```java
switch (lineStart){
                    case "BU_:":
                        dbc.addCanNodeSet(parseBU(line)); // 解析节点
                        break;
                    case "BO_":
                        CanMessage msg = parseBO(line); // 解析消息
                        if (msg != null){
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
```

其中，解析消息的具体代码如下，解析信号的就不贴了，大同小异，都是用了正则表达式。

```java
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
```

# 四、定义数据模型

在车载安卓app的开发当中，通常都会有一个或者多个数据模型类，用于描述车上的数据，子线程刷新该类的数据，而主线程则读取数据模型中的数据到界面中，通常是这样一个架构。

数据模型类通常如下

```java
public class Model2 {
    @SignalTag(signalName = "CabinToCCS1_FactoryID")
    int CabinToCCS1_FactoryID ;
    @SignalTag(signalName = "CabinToCCS1_CabinTemp")
    int CabinToCCS1_CabinTemp ;
    @SignalTag(signalName = "CabinToCCS1_ColdGearSts")
    int CabinToCCS1_ColdGearSts;
    @SignalTag(signalName = "CabinToCCS1_FanGearSts")
    int CabinToCCS1_FanGearSts;
    @SignalTag(signalName = "CabinToCCS1_FanMotFlt")
    int CabinToCCS1_FanMotFlt ;
    @SignalTag(signalName = "CabinToCCS1_ColdMotFlt")
    int CabinToCCS1_ColdMotFlt;
    @SignalTag(signalName = "CabinToCCS1_AirSts")
    int CabinToCCS1_AirSts;
    }
```

例如上边就依次表示了厂家代号、空调温度、空调制冷状态、鼓风机状态、鼓风机故障、压缩机故障、空调状态。

到了实际应用中，可以是车身的任何数据，这里仅仅是举例，因为之前做了个车身语音控制的项目，故这里仅对空调做举例。

你可能想问了，上边的注解

```java
    @SignalTag(signalName = "CabinToCCS1_CabinTemp")
```

是个什么意思，对，这就是本框架的核心思想。通过一行注解，将车身数据模型与DBC的信号进行一对一绑定，在解析报文和封装报文的时候，只需要一句就可以了，就像下边这样

```java
 canIO.canDataToModel(id,data8_);
```

你只需要传入报文id和原始的CAN数据即可，剩下的交给框架来解决，框架会自动解析原始CAN报文到你注解的字段中。所有操作都只需要一行，极大的简化了编程操作。

你可能会问，为什么不直接把DBC对象的数据用于安卓app界面的绑定呢？不行，因为DBC对象不是一个预定义的数据，而是动态生成的对象，在使用的时候，需要通过字符串标签和查找才能绑定数据，这太不优雅了。

使用另外的数据模型，将数据的解析和使用进行一个分离，会使代码耦合度更高，在编程的时候，只需要编写自己的数据模型类就可以了，你只负责使用它，而框架专注于解析数据。

具体的使用代码如下

```java
  int id = 0x18AB_AB01 ; // message1
        /* 1.完成DBC和数据模型的绑定。 */
        String path1 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\Example.dbc";
        Model2 model = new Model2();

        CanObjectMapManager manager = CanObjectMapManager.getInstance();
        manager.registerDBC("testDbc",path1); // 绑定DBC文件
        manager.registerData(model); // 绑定数据模型

        /* 2.获取CAN收发对象 */
        CanIO canIO = manager.getCanIo("testDbc");
// 以下代码用于测试报文的  接收
            byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
            canIO.canDataToModel(id,data8_); // 解析一个CAN报文
            System.out.println("model = "+ model.getMsg1Value()); // 打印实际数据，验证是否解析成功
```

