package quickCanResolver.core;

import quickCanResolver.dbc.CanDataEnum.CANByteOrder;
import quickCanResolver.tool.SLCTool;
import quickCanResolver.dbc.CanSignal;
import quickCanResolver.dbc.CanDbc;
import quickCanResolver.dbc.CanMessage;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CAN 编解码器（执行者）。
 * <br>用于处理报文的解码编码,构造器传入一个DBC对象，本类就负责针对报文对该DBC进行修改。<br>
 */
//低耦合：此类只对 CanDbc 负责，不对其他类负责。由dbc类中的数据，解析报文，或者编码报文。
public class CanCoder {
    /** 持有一个 dbc 的 mspMap 。 用于执行报文的收发操作。*/
    protected final Map<Integer, CanMessage> msgMap;
    /** 持有一个dbc */
    protected final CanDbc dbc ;
    /** dbc 标签 */
    public final String dbcTag ;
    /** 定义锁的map，用于在写入字段的时候同步，确保相同的报文，同一时间只有一个线程在写入字段。
     * 而不同报文，则用不同的锁来保证同步。不同报文因为是不同的数据，故可以多线程同时操作。
     * */
    protected Map<Integer,ReentrantLock> msgWriteLockMap ;
    /** 构造函数，传入一个 dbc  */
    public CanCoder(CanDbc dbc){
        // 传入一个DBC对象，用于后续修改
        this.dbc = dbc;
        this.dbcTag = dbc.dbcTag;
        msgMap = dbc.getIntMsgMap();
        msgWriteLockMap = new ConcurrentHashMap<>();
    }

    /**
     * 接收数据，解码报文。
     * <br>将接收到的CAN报文，解析后存入绑定好的数据模型中。<br>
     * @param canId 报文id
     * @param data8 8位数组的CAN报文,Byte数组格式。
     */
    public void deCode_B(int canId, byte[] data8 ) {
        // 拿到id之后，需要到DBC文件中查询对应的对象。然后修改这个对象
        CanMessage msg = msgMap.get(canId);
        if (msg == null){
            return;
        }
        byte[] data64 = SLCTool.from8BytesTo64Bits(data8, SLCTool.DataType.Intel); // 8 -->64
        // 64 --> signal ;循环，逐个将 64bits数组中的数据按位取出，并解析到信号的字段中。
        for (CanSignal signal : msg.getSignalMap().values()) {
            // 解析值
            double phyValue = bits64ToValue(data64, signal, signal.getStartBit(), signal.getBitLength(), signal.getFactor(), signal.getOffset());
            // 刷新值
            signal.writeValue(phyValue) ; // 绑定的模型可能存在多个
        }
    }

    /**
     * 编码数据，发送报文。
     * <br>将数据模型的字段，解析后得到一个CAN报文数组。<br>
     * @param canId 报文id
     * @return 8位数组的CAN报文。int数组格式。
     */

    public int[] enCode_I(int canId) {
        CanMessage sendMsg = msgMap.get(canId);
        if (sendMsg == null){
            return new int[8]; // 传一个全是0的回去
        }
        byte[] data64 = new byte[64];
        // field --> 64 ; 循环，逐个将信号的值解析出来，并加载到数组对应的位中。
        for (CanSignal signal : sendMsg.getSignalMap().values()) {
            sigToBits64(data64,signal.readValue() , signal.getStartBit(), signal.getBitLength(), signal.getFactor() , signal.getOffset() ,signal.getByteOrder());
        }
        return SLCTool.from64bitsTo8BytesI(data64, SLCTool.DataType.Intel); // 64 --> 8
    }
    /**
     * 使用原有对象，编码数据，发送报文。
     * <br>将数据模型的字段，解析后得到一个CAN报文数组。<br>
     * @param canId 报文id
     * @return 8位数组的CAN报文。
     */
    public byte[] enCode_B(int canId) {
        CanMessage sendMsg = msgMap.get(canId);
        if (sendMsg == null){
            return new byte[8]; // 传一个全是0的回去
        }
        byte[] data64 = new byte[64];
        // field --> 64 ; 循环，逐个将信号的值解析出来，并加载到数组对应的位中。
        for (CanSignal signal : sendMsg.getSignalMap().values()) {
            sigToBits64(data64,signal.readValue() , signal.getStartBit(), signal.getBitLength(),
                    signal.getFactor() , signal.getOffset() ,signal.getByteOrder());
        }
        return SLCTool.from64bitsTo8Bytes(data64, SLCTool.DataType.Intel); // 64 --> 8
    }
    /**
     * 使用新的对象，编码数据，发送报文。
     * <br>将数据模型的字段，解析后得到一个CAN报文数组。<br>
     * @param canId 报文id
     * @param newObject 新的数据对象
     * @return 8位数组的CAN报文。
     */
    public byte[] enCode_B(int canId, Object newObject) {
        CanMessage sendMsg = msgMap.get(canId);
        if (sendMsg == null){
            return new byte[8]; // 传一个全是0的回去
        }
        byte[] data64 = new byte[64];
        // field --> 64 ; 循环，逐个将信号的值解析出来，并加载到数组对应的位中。
        for (CanSignal signal : sendMsg.getSignalMap().values()) {
            // 从新对象中读取值，并生成报文。
            sigToBits64(data64,signal.readValue(newObject) , signal.getStartBit(), signal.getBitLength(),
                    signal.getFactor() , signal.getOffset() ,signal.getByteOrder());
        }
        return SLCTool.from64bitsTo8Bytes(data64, SLCTool.DataType.Intel);
    }

    /**
     * 在写入字段（解码报文）的时候，加锁写入。程序默认不加锁。
     * @param canId 报文id
     * @param data8 8位数组的CAN报文
     */

    public void syncDeCode_B(int canId, byte[] data8) {
        // 拿到id之后，需要到DBC文件中查询对应的对象。然后修改这个对象
        CanMessage msg = msgMap.get(canId);
        if (msg == null) {
            return;
        }
        byte[] data64 = SLCTool.from8BytesTo64Bits(data8, SLCTool.DataType.Intel); // 8 --> 64
        // 64 --> signal ;循环，逐个将 64bits数组中的数据按位取出，并解析到信号的字段中。

        ReentrantLock lock = getLock(canId);
        lock.lock();
        try {
            for (CanSignal signal : msg.getSignalMap().values()) {
                double phyValue = bits64ToValue(data64, signal, signal.getStartBit(), signal.getBitLength(), signal.getFactor(), signal.getOffset());
                signal.writeValue(phyValue) ;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取一个锁，该锁锁住了单个报文的操作，不同报文则是不同的锁。
     */
    public ReentrantLock getLock(int canId) {
        return msgWriteLockMap.putIfAbsent(canId,new ReentrantLock());
    }

    /**
     * 64 --> signal --> field <br>
     * 根据报文，计算数据的实际值。phyValue = (rawValue * factor) + offset 。<br>
     * 如果精度和偏移量有小数，那么会额外执行空值判断<br>
     * @param data64 接收到的 64 bits数组 CAN 数据
     * @param startBit 数据项在 CAN 帧的起始位
     * @param bitLength 数据项的 bit 长度
     * @param factor 精度
     * @param offset 偏移量
     */
    /* TODO  此函数存在非常大的问题需要适配，根据精度偏移量计算实际值，有可能返回 int 或是 Double 类型的数据，
     *       还有可能返回 null （只有用Double接收的时候会返回null），即0xFF时需要做提醒。适配起来太麻烦了，我疲倦了。 */
    private double bits64ToValue(byte[] data64, CanSignal signal, int startBit , int bitLength , double factor , double offset ) {
        //System.out.println("待计算值，startBit = " + startBit +" ;  bitLength = "+bitLength+" ;  factor = " + factor + " ; offset = " + offset) ;
        int rawValue ; //总线值，未处理值
        double phyValue; //实际值
        SLCTool.DataType inputType = transOrder(signal.getByteOrder());
        rawValue = SLCTool.bitsToInt(Arrays.copyOfRange(data64,startBit,startBit + bitLength),inputType) ; // SLCTool.DataType.Intel
        phyValue = (rawValue * factor) + offset ; //不包括8
        //System.out.println("计算后 ，rawValue="+rawValue+" , phyValue="+phyValue);
        //如果准备输出 double 类型数据，首先判断 总线值不可以是 0xFF 。
        if ((!isInteger(factor)||!isInteger(offset)||! isInteger(phyValue))) { //判断是否是小数，是则下一步
            //新增代码，加入无效值判断
            if ( checkAllOnes(rawValue,bitLength) ){ // 全为1，则是0xFF，则无效化处理。
                signal.setValid(false); //设置无效值
            }
            else {
                signal.setValid(true);
            }
        }
        //另外，如果是整型的数据，仍然有可能输出0xFF表示无效。但是不能直接将0XFF视为无效值，因为有的是枚举变量。所以啊，统一协议，任重而道远啊。一旦统一了所有信号全1为无效，代码就方便多了。
        return phyValue;
    } // bits64ToValue()

    /* 一些共用方法 */
    /**
     * 判断一个 double 类型的数据是否是整型
     * @param doubleValue 输入一个 double 类型的数据
     * @return 返回是否是整型数
     */
    private static boolean isInteger(double doubleValue) {
        return doubleValue % 1 == 0;
    }
    /**
     * 输入一个int数，根据有效范围长度，检查是否全为1 。<br>
     * 例如，如果输入 15（在二进制中是 1111），并且我们计算的是 4 位二进制，那么它是全为 1。但如果考虑 5 位（比如 01111），则不是全 1。<br>
     * @param number 输入一个整型数
     * @param bitLength 该整型数的有效长度
     * @return 返回是否全为1,
     */
    private static boolean checkAllOnes(int number, int bitLength) {
        // 构造一个掩码，掩码的长度等于 bitLength，且所有位均为 1
        int mask = (1 << bitLength) - 1;

        // 比较 number 和掩码，如果 number 与掩码的按位 与结果等于掩码，说明 number 的有效位都是 1
        // 按位 与 运算。同时为1才为1，否则为0 。
        return (number & mask) == mask;
    }

    /**
     * 将实例变量（对象的属性）转变成数组，并加载到输出CAN报文中。
     * @param sendCanData  发送 CAN 报文,需要加载的目标数组
     * @param instanceValue  实例变量 （对象的属性）
     * @param startBit  起始位
     * @param bitLength 数据长度
     */

    private void sigToBits64(byte[] sendCanData, int instanceValue, int startBit , int bitLength, CANByteOrder instanceByteOrder ) {
        SLCTool.DataType inputType = transOrder(instanceByteOrder);
        byte[] src = SLCTool.intToBits(instanceValue,inputType,bitLength)  ;  //将总线值变成 0或者1 的数组
        System.arraycopy( src ,0, sendCanData , startBit , bitLength  );  //将数组赋值到目标 8*8 = 64 bits 的矩阵中
    }
    /**
     * signal --> 64 . 将实例变量（对象的属性）转变成数组，并加载到输出CAN报文中。带精度和偏移量。<br>
     * 实例变量 instanceValue 可能为int,也可能为double , 根据精度和偏移量计算总线值 instanceValue = (rawValue * factor) + offset
     * @param bits64  发送 CAN 报文,需要加载的目标数组
     * @param instanceValue  实例变量 （对象的属性）
     * @param startBit  起始位
     * @param bitLength 数据长度
     */
    private void sigToBits64(byte[] bits64, double instanceValue, int startBit , int bitLength, double factor , double offset , CANByteOrder instanceByteOrder) {
        //System.out.println("待计算值，startBit = " + startBit +" ;  bitLength = "+bitLength+" ;  factor = " + factor + " ; offset = " + offset+",instanceValue = "+instanceValue) ;
        int rawValue  = (int) ( (instanceValue - offset) / factor ); //获取总线值
        //System.out.println("计算后 ，rawValue="+rawValue);
        SLCTool.DataType inputType = transOrder(instanceByteOrder);
        byte[] src = SLCTool.intToBits(rawValue,inputType, bitLength ) ;  //将总线值变成 0或者1 的数组
        System.arraycopy( src ,0, bits64, startBit , bitLength  );  //将数组 复制 到目标 8*8 = 64 bits 的矩阵中
    }
    /** 转换不同的英特尔格式 */
    public static SLCTool.DataType transOrder(CANByteOrder instanceByteOrder) {
        if (instanceByteOrder == CANByteOrder.Intel){
            return SLCTool.DataType.Intel;
        }
        else {
            return SLCTool.DataType.Motorola;
        }
    }

    /**
     * 将正文转换成单个出口点形式
     */

    public static CANByteOrder transOrder(SLCTool.DataType instanceByteOrder) {
        if (instanceByteOrder == SLCTool.DataType.Intel){
            return CANByteOrder.Intel;
        }
        else {
            return CANByteOrder.Motorola_LSB;
        }
    }

} // CAN 编解码器（执行者）。
