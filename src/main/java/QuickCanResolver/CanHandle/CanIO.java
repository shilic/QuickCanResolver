package QuickCanResolver.CanHandle;

import QuickCanResolver.CanDataEnum.CANByteOrder;
import QuickCanResolver.CanTool.MyByte;
import QuickCanResolver.DBC.CanDbc;
import QuickCanResolver.DBC.CanMessage;
import QuickCanResolver.DBC.CanSignal;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * CAN收发器。<br>用于处理报文的收发,构造器传入一个DBC对象，本类就负责针对报文对该DBC进行修改。
 */
public class CanIO {
    protected CanDbc dbc;
    Map<Integer, CanMessage> msgMap;
    Map<Integer,ReentrantLock> msgWriteLockMap ;
    //ReentrantLock lock_ = new ReentrantLock();
    public CanIO(CanDbc dbc) {
        // 传入一个DBC对象，用于后续修改
        this.dbc = dbc ;
        msgMap = dbc.getIntMsgMap();
        msgWriteLockMap = new ConcurrentHashMap<>();
    }
    /**
     * 将接收到的CAN报文，解析后存入绑定好的数据模型中。<br>
     * 8 --> 64 --> signal --> field
     * @param canId 报文id
     * @param data8 8位数组的CAN报文
     */
    public void canToFieldB(int canId, byte[] data8) {
        // 拿到id之后，需要到DBC文件中查询对应的对象。然后修改这个对象
        CanMessage msg = msgMap.get(canId);
        if (msg == null){
            return;
        }
        byte[] data64 = MyByte.from8BytesTo64Bits(data8,MyByte.DataType.Intel); // 8 -->64
        // 64 --> signal ;循环，逐个将 64bits数组中的数据按位取出，并解析到信号的字段中。
        for (CanSignal mSig : msg.getSignalMap().values()) {
            bits64ToSig(data64, mSig);
        }
    }
    /**
     * 将数据模型的字段，解析后得到一个CAN报文数组。<br>
     * field --> 64 --> 8
     * @param canId 报文id
     * @return 8位数组的CAN报文
     */
    public int[] fieldToCanI(int canId) {
        CanMessage sendMsg = msgMap.get(canId);
        if (sendMsg == null){
            return new int[8]; // 传一个全是0的回去
        }
        byte[] data64 = new byte[64];
        // field --> 64 ; 循环，逐个将信号的值解析出来，并加载到数组对应的位中。
        for (CanSignal mSig : sendMsg.getSignalMap().values()){
            sigToBits64(data64,mSig.getFieldValue() , mSig.getStartBit(), mSig.getBitLength(), mSig.getFactor() , mSig.getOffset() ,mSig.getByteOrder());
        }
        return MyByte.from64bitsTo8BytesI(data64, MyByte.DataType.Intel); // 64 --> 8
    }


    /**
     * 在写入字段的时候，加锁写入。
     * @param canId 报文id
     * @param data8 8位数组的CAN报文
     */
    public void syncCanToFieldB(int canId, byte[] data8) {
        // 拿到id之后，需要到DBC文件中查询对应的对象。然后修改这个对象
        CanMessage msg = msgMap.get(canId);
        if (msg == null) {
            return;
        }
        byte[] data64= MyByte.from8BytesTo64Bits(data8,MyByte.DataType.Intel); // 8 --> 64
        // 64 --> signal ;循环，逐个将 64bits数组中的数据按位取出，并解析到信号的字段中。

        ReentrantLock lock = getLock(canId);
        lock.lock();
        try {
            for (CanSignal mSig : msg.getSignalMap().values()) {
                bits64ToSig(data64, mSig);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取一个锁，该锁锁住了单个报文的操作，不同报文则是不同的锁。
     */
    public ReentrantLock getLock(int canId){
        ReentrantLock lock = msgWriteLockMap.get(canId);
        if (lock == null) {
            lock = new ReentrantLock();
            msgWriteLockMap.put(canId,lock);
        }
        return lock;
    }



    /**
     * 64 --> signal
     */
    private void bits64ToSig(byte[] data64, CanSignal mSig) {
        write64ToField(mSig,data64, mSig.getStartBit(), mSig.getBitLength(), mSig.getFactor(), mSig.getOffset());
    }

    /**
     * 64 --> signal <br>
     * 根据报文，计算数据的实际值。phyValue = (rawValue * factor) + offset 。<br>
     * 如果精度和偏移量有小数，那么会额外执行空值判断<br>
     * @param data64 接收到的 64 bits数组 CAN 数据
     * @param startBit 数据项在 CAN 帧的起始位
     * @param bitLength 数据项的 bit 长度
     * @param factor 精度
     * @param offset 偏移量
     * TODO  此函数存在非常大的问题需要适配，根据精度偏移量计算实际值，有可能返回 int 或是 Double 类型的数据，
     *       还有可能返回 null （只有用Double接收的时候会返回null），即0xFF时需要做提醒。适配起来太麻烦了，我疲倦了。
     */
    protected void write64ToField(CanSignal sig, byte[] data64, int startBit , int bitLength , double factor , double offset ) {
        //System.out.println("待计算值，startBit = " + startBit +" ;  bitLength = "+bitLength+" ;  factor = " + factor + " ; offset = " + offset) ;
        int rawValue ; //总线值，未处理值
        double phyValue; //实际值
        MyByte.DataType inputType = transOrder(sig.getByteOrder());
        rawValue = MyByte.bitsToInt(Arrays.copyOfRange(data64,startBit,startBit + bitLength),inputType) ; // MyByte.DataType.Intel
        phyValue = (rawValue * factor) + offset ; //不包括8
        sig.setFieldValue(phyValue) ; // 设置字段值（未处理线程同步的问题）
        //System.out.println("计算后 ，rawValue="+rawValue+" , phyValue="+phyValue);
        //如果准备输出 double 类型数据，首先判断 总线值不可以是 0xFF 。
        if ((!isInteger(factor)||!isInteger(offset)||! isInteger(phyValue))) { //判断是否是小数，是则下一步
            //新增代码，加入无效值判断
            if ( checkAllOnes(rawValue,bitLength) ){ // 全为1，则是0xFF，则无效化处理。
                sig.setValid(false); //设置无效值
            }
            else {
                sig.setValid(true);
            }
        }
        //另外，如果是整型的数据，仍然有可能输出0xFF表示无效。但是不能直接将0XFF视为无效值，因为有的是枚举变量。所以啊，统一协议，任重而道远啊。一旦统一了所有信号全1为无效，代码就方便多了。
    } // write64ToField()

    /* 一些共用方法 */
    /**
     * 判断一个 double 类型的数据是否是整型
     * @param doubleValue 输入一个 double 类型的数据
     * @return 返回是否是整型数
     */
    public static boolean isInteger(double doubleValue) {
        return doubleValue % 1 == 0;
    }
    /**
     * 输入一个int数，根据有效范围长度，检查是否全为1 。<br>
     * 例如，如果输入 15（在二进制中是 1111），并且我们计算的是 4 位二进制，那么它是全 1。但如果考虑 5 位（比如 01111），则不是全 1。<br>
     * @param number 输入一个整型数
     * @param bitLength 该整型数的有效长度
     * @return 返回是否全为1,
     */
    public static boolean checkAllOnes(int number, int bitLength) {
        // 构造一个掩码，掩码的长度等于 bitLength，且所有位均为 1
        int mask = (1 << bitLength) - 1;

        // 比较 number 和掩码，如果 number 与掩码的按位与结果等于掩码，说明 number 的有效位都是 1
        // 按位与 运算。同时为1才为1，否则为0 。
        return (number & mask) == mask;
    }

    /**
     * 将实例变量（对象的属性）转变成数组，并加载到输出CAN报文中。
     * @param sendCanData  发送 CAN 报文,需要加载的目标数组
     * @param instanceValue  实例变量 （对象的属性）
     * @param startBit  起始位
     * @param bitLength 数据长度
     */
    public void sigToBits64(byte[] sendCanData, int instanceValue, int startBit , int bitLength, CANByteOrder instanceByteOrder ) {
        // 实例变量 instanceValue 可能为int,也可能为double , 根据精度和偏移量计算总线值 instanceValue = (rawValue * factor) + offset
        MyByte.DataType inputType = transOrder(instanceByteOrder);
        byte[] src = MyByte.intToBits(instanceValue,inputType,bitLength)  ;  //将总线值变成 0或者1 的数组
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
    public void sigToBits64(byte[] bits64, double instanceValue, int startBit , int bitLength, double factor , double offset , CANByteOrder instanceByteOrder) {
        //System.out.println("待计算值，startBit = " + startBit +" ;  bitLength = "+bitLength+" ;  factor = " + factor + " ; offset = " + offset+",instanceValue = "+instanceValue) ;
        int rawValue  = (int) ( (instanceValue - offset) / factor ); //获取总线值
        //System.out.println("计算后 ，rawValue="+rawValue);
        MyByte.DataType inputType = transOrder(instanceByteOrder);
        byte[] src = MyByte.intToBits(rawValue,inputType, bitLength ) ;  //将总线值变成 0或者1 的数组
        System.arraycopy( src ,0, bits64, startBit , bitLength  );  //将数组 复制 到目标 8*8 = 64 bits 的矩阵中
    }
    /** 转换不同的英特尔格式 */
    public static MyByte.DataType transOrder(CANByteOrder instanceByteOrder){
        MyByte.DataType inputType ;
        if (instanceByteOrder == CANByteOrder.Intel){
            inputType = MyByte.DataType.Intel;
        }
        else {
            inputType = MyByte.DataType.Motorola;
        }
        return inputType;
    }

    /* 以下代码均被弃用。因为每次解析报文实际上是CPU密集型任务。并且每次的计算量很小，只有64bit的计算量。
    * 同时，由于任务大小很小，开启线程可能不会带来明显性能提升，反而可能因线程切换增加开销。
    * 同时，因为任务次数很多，故可以在最外部使用线程池来开启线程，使每个任务的大小变得更合适一些。
    *
    * 经过测试：采用了并发流来处理报文时，一千帧报文，平均耗时200毫秒。而采用单线程，一千帧，不采用并发流的平均耗时是10毫秒。
    * 可见，过早的优化确实是罪恶之源。当线程切分得太小时，反而不适合使用多线程进行处理；采用单线程反而可以提高效率。
    * 线程划分的颗粒度，还是需要实际测试了之后，才能知晓结果。
    *  */
    @Deprecated
    public void concurrentCanToField(int canId, byte[] data8){
        concurrentCanDataToDbc(canId, data8);
    }
    @Deprecated
    public int[] concurrentFieldToCan(int canId){
        return concurrentDbcToCanData(canId); // concurrentFieldToCan
    }
    /* 以下是监听函数 ，用于解析CAN报文 */
    /**
     * 将报文数据解析到DBC中
     * @param canId id
     * @param data8 8位的报文数组
     */
    @Deprecated
    public void concurrentCanDataToDbc(int canId, byte[] data8){
        if (msgMap == null){
            return;
        }
        /* 接受到的8*8的CAN数据矩阵，共64个bit */
        byte[] data64; //接收到的数据
        data64 = MyByte.from8BytesTo64Bits(data8,MyByte.DataType.Intel); // 8 -->64
        // 拿到id之后，需要到DBC文件中查询对应的对象。然后修改这个对象
        CanMessage msg = msgMap.get(canId);
        if (msg == null){
            return;
        }
        //System.out.println("正在解析接收报文，解析报文 ID = " + MyByte.hex2Str(canId));
        // 遍历消息，取出数组值,并修改所有的消息值 。可用线程池或者并行流优化。可以考虑使用并行流（parallelStream()）来简化代码。并行流会自动处理并发执行，并且代码会更加简洁。
        msg.getSignalMap().values().parallelStream().forEach(sig -> bits64ToSig(data64, sig)); // 64 --> signal
    }
    /* 以下是发送函数 */
    /**
     * 从Dbc中，获取8位byte的数据数组。<br>将64bits的数据转换成8个byte的数组。
     * @return 8位byte的数据数组
     */
    @Deprecated
    public int[] concurrentDbcToCanData(int sendId) {
        if (msgMap == null){
            return new int[8];
        }
        byte[] src = concurrentSigTo64Bits(sendId); // 第一步，signal --> 64
        int[] re;
        re = MyByte.from64bitsTo8BytesI(src, MyByte.DataType.Intel); // 64 --> 8
        return re;
    }
    /**
     * signal --> 64.获取64bit的数据。将成员变量的数据变成64bits的数组。<br>
     * 根据id的不同，获取不同的发送报文。
     * @return 返回64bits的数据
     */
    @Deprecated
    public byte[] concurrentSigTo64Bits(int sendId) {
        //System.out.println("正在解析发送报文，发送报文 ID = " + MyByte.hex2Str(sendId));
        byte[] sendCanData = new byte[64];
        CanMessage sendMsg = msgMap.get(sendId);
        if (sendMsg == null){
            return sendCanData; // 传一个全是0的回去
        }
        /* 将对象中的数据转换至数组中。使用流式操作优化代码,并发的方式提高程序效率。多线程修改 sendCanData，可能会出现线程安全问题。但由于修改的是数组 sendCanData 的不同下标，理论上不会出现问题。 */
        sendMsg.getSignalMap().values().parallelStream().forEach(sig ->
                sigToBits64(sendCanData,sig.getFieldValue() , sig.getStartBit(), sig.getBitLength(), sig.getFactor() , sig.getOffset() ,sig.getByteOrder())   );
        return sendCanData;
    }
}
