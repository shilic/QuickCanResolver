package QuickCanResolver.CanHandle;

import QuickCanResolver.CanDataEnum.CANByteOrder;
import QuickCanResolver.CanTool.MyByte;
import QuickCanResolver.DBC.CanChannel;
import QuickCanResolver.DBC.CanMessage;
import QuickCanResolver.DBC.CanSignal;

import java.util.Arrays;
import java.util.Map;

/**
 * CAN收发器。<br>用于处理报文的收发,构造器传入一个DBC对象，本类就负责针对报文对该DBC进行修改。
 */
public class CanIO {
    protected CanChannel dbc;
    Map<Integer, CanMessage> msgMap;
    CanObjectMapManager manager;
    public CanIO(CanChannel dbc, CanObjectMapManager manager) {
        // 传入一个DBC对象，用于后续修改
        this.dbc = dbc ;
        this.manager = manager;
        msgMap = dbc.getIntMsgMap();
    }

    /* 以下是监听函数 ，用于解析CAN报文 */

    /**
     * 将报文中的数据,解析至绑定好的数据模型中。canData --> Dbc --> Model
     * @param canId 报文id
     * @param data8 接收的数据
     */
    public void canDataToModel(int canId, byte[] data8){
        canDataToDbc(canId, data8);
        manager.updateDbcToModel(canId);
    }
    /**
     * 将报文数据解析到DBC中
     * @param canId id
     * @param data8 8位的报文数组
     */
    public void canDataToDbc(int canId, byte[] data8){
        if (msgMap == null){
            return;
        }
        /* 接受到的8*8的CAN数据矩阵，共64个bit */
        byte[] data64; //接收到的数据
        try {
            data64 = MyByte.from8BytesTo64Bits(data8,MyByte.DataType.Intel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 拿到id之后，需要到DBC文件中查询对应的对象。然后修改这个对象
        CanMessage msg = msgMap.get(canId);
        if (msg == null){
            return;
        }
        //System.out.println("正在解析接收报文，解析报文 ID = " + MyByte.hex2Str(canId));
        // 遍历消息，取出数组值,并修改所有的消息值 。可用线程池或者并行流优化。可以考虑使用并行流（parallelStream()）来简化代码。并行流会自动处理并发执行，并且代码会更加简洁。
        msg.getSignalMap().values().parallelStream().forEach(sig -> eachSig(data64, sig));
    }
    /* 以下是发送函数 */
    /**
     * 将模型中的数据,转换成报文数组。 Model --> Dbc --> canData
     * @param canId 报文id
     */
    public int[] modelToCanData(int canId) {
        manager.updateModelToDbc(canId);
        return dbcToCanData(canId);
    }
    /**
     * 从Dbc中，获取8位byte的数据数组。<br>将64bits的数据转换成8个byte的数组。
     * @return 8位byte的数据数组
     */
    public int[] dbcToCanData(int sendId){
        if (msgMap == null){
            return new int[8];
        }
        byte[] src = instanceTo64Bits(sendId); // 第一步，获取64bit的数据
        int[] re;
        try {
            re = MyByte.from64bitsTo8BytesI(src, MyByte.DataType.Intel); // 64 转 8
        } catch (Exception  e) {
            throw new RuntimeException(e);
        }
        return re;
    }
    /**
     * 获取64bit的数据。将成员变量的数据变成64bits的数组。<br>
     * 根据id的不同，获取不同的发送报文。
     * @return 返回64bits的数据
     */
    public byte[] instanceTo64Bits(int sendId) {
        //System.out.println("正在解析发送报文，发送报文 ID = " + MyByte.hex2Str(sendId));
        byte[] sendCanData = new byte[64];
        CanMessage sendMsg = msgMap.get(sendId);
        if (sendMsg == null){
            return sendCanData; // 传一个全是0的回去
        }
        /* 将对象中的数据转换至数组中。使用流式操作优化代码,并发的方式提高程序效率。多线程修改 sendCanData，可能会出现线程安全问题。但由于修改的是数组 sendCanData 的不同下标，理论上不会出现问题。 */
        sendMsg.getSignalMap().values().parallelStream().forEach(sig ->
                instanceToBits(sendCanData,sig.currentValue , sig.getStartBit(), sig.getBitLength(), sig.getFactor() , sig.getOffset() ,sig.getByteOrder())   );
        return sendCanData;
    }

    private void eachSig(byte[] data64, CanSignal sig) {
        resolverCurrentValue(sig,data64, sig.getStartBit(), sig.getBitLength(), sig.getFactor(), sig.getOffset());
    }
    /**
     * 根据报文，计算数据的实际值。phyValue = (rawValue * factor) + offset 。<br>
     * 如果精度和偏移量有小数，那么会额外执行空值判断<br>
     * @param receiveCanData 接收到的 64 bits数组 CAN 数据
     * @param startBit 数据项在 CAN 帧的起始位
     * @param bitLength 数据项的 bit 长度
     * @param factor 精度
     * @param offset 偏移量
     */
    // 此函数存在非常大的问题需要适配，根据精度偏移量计算实际值，有可能返回 int 或是 Double 类型的数据，
    // 还有可能返回 null （只有用Double接收的时候会返回null），即0xFF时需要做提醒。适配起来太麻烦了，我疲倦了。
    protected void resolverCurrentValue(CanSignal sig,byte[] receiveCanData, int startBit , int bitLength , double factor , double offset ) {
        //System.out.println("待计算值，startBit = " + startBit +" ;  bitLength = "+bitLength+" ;  factor = " + factor + " ; offset = " + offset) ;
        int rawValue ; //总线值，未处理值
        double phyValue; //实际值
        MyByte.DataType inputType = transOrder(sig.getByteOrder());
        try {
            rawValue = MyByte.bitsToInt(Arrays.copyOfRange(receiveCanData,startBit,startBit + bitLength),inputType) ; // MyByte.DataType.Intel
            phyValue = (rawValue * factor) + offset ; //不包括8
            //Log.d("解析报文","解析报文 , 未处理值 rawValue = " + rawValue +" ; 处理值 phyValue = "+ phyValue ) ;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sig.currentValue = phyValue;
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
    } // resolverCurrentValue()
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
    public void instanceToBits(byte[] sendCanData, int instanceValue, int startBit , int bitLength, CANByteOrder instanceByteOrder ) {
        // 实例变量 instanceValue 可能为int,也可能为double , 根据精度和偏移量计算总线值 instanceValue = (rawValue * factor) + offset
        MyByte.DataType inputType = transOrder(instanceByteOrder);
        byte[] src = MyByte.intToBits(instanceValue,inputType,bitLength)  ;  //将总线值变成 0或者1 的数组
        System.arraycopy( src ,0, sendCanData , startBit , bitLength  );  //将数组赋值到目标 8*8 = 64 bits 的矩阵中
    }
    /**
     * 将实例变量（对象的属性）转变成数组，并加载到输出CAN报文中。带精度和偏移量。<br>
     * 实例变量 instanceValue 可能为int,也可能为double , 根据精度和偏移量计算总线值 instanceValue = (rawValue * factor) + offset
     * @param sendCanData  发送 CAN 报文,需要加载的目标数组
     * @param instanceValue  实例变量 （对象的属性）
     * @param startBit  起始位
     * @param bitLength 数据长度
     */
    public void instanceToBits(byte[] sendCanData, double instanceValue, int startBit , int bitLength, double factor , double offset , CANByteOrder instanceByteOrder) {
        //System.out.println("待计算值，startBit = " + startBit +" ;  bitLength = "+bitLength+" ;  factor = " + factor + " ; offset = " + offset+",instanceValue = "+instanceValue) ;
        int rawValue  = (int) ( (instanceValue - offset) / factor ); //获取总线值
        //System.out.println("计算后 ，rawValue="+rawValue);
        MyByte.DataType inputType = transOrder(instanceByteOrder);
        byte[] src = MyByte.intToBits(rawValue,inputType, bitLength ) ;  //将总线值变成 0或者1 的数组
        System.arraycopy( src ,0, sendCanData , startBit , bitLength  );  //将数组赋值到目标 8*8 = 64 bits 的矩阵中
    }
    /** 转换不同道英特尔格式 */
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
}
