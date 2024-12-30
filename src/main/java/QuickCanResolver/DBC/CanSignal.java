package QuickCanResolver.DBC;

import QuickCanResolver.CanDataEnum.*;

import java.lang.reflect.Field;
import java.util.*;

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

    @Override
    public String toString() {
        return "{信号名称:"+signalName+"}";
    }  //toString()
    public String getSignalInfo() {
        return "{\n信号名称:"+signalName+",\n多路复用:"+ groupType +" ,组号:"+groupNumber+",信号注释:"+signalComment+
                ",\n信号排列方式:"+byteOrder+",信号起始位:"+startBit+",信号长度:"+bitLength+",\n信号数据类型:"+dataType+
                ",精度:"+factor+",偏移量:"+offset+",\n物理最小值:"+signalMinValuePhys+",物理最大值:"+signalMaxValuePhys+
                ",\n单位:"+unit+",接收节点列表 : "+ getReceiveNodeListCode() +",\n}";
        ///+",\n    信号值描述: "+ this.getStrValueTable()
    }
    /**
     * 生成 接受节点列表编码,如 VCU,GW,TCU,CanIO ;<br>如果为空(即用户没有标记接收节点)，则返回 Vector__XXX;<br>
     * @return 接受节点列表编码
     */
    String getReceiveNodeListCode(){
        StringBuilder reNodeListCodeBuilder = new StringBuilder();
        if( sigReceiveNodeSet != null && sigReceiveNodeSet.size() >= 1 ) { // sigReceiveNodes != null &&   --- (length = sigReceiveNodes.length)
            // 生成节点列表，如 VCU,GW,TCU,CanIO 最后一个没有逗号
            for (String node : sigReceiveNodeSet) {
                reNodeListCodeBuilder.append(node).append(",");
            }
            reNodeListCodeBuilder.deleteCharAt(reNodeListCodeBuilder.length() - 1); // 移除最后一个字符 逗号
        } else { //receiveNodes == null 或者 length < 1 ，表示用户没有添加节点
            reNodeListCodeBuilder.append(Vector__XXX);
        }
        return reNodeListCodeBuilder.toString(); //如果没有接收节点，不会返回空null，而是会返回空字符串
    }   //getReceiveNodeListCode
    /**
     * 构造函数，采用 final 字段的形式对代码进行了优化，保证数据字段的只读，及可见性，防止意外修改。
     */
    public CanSignal(String signalName, GroupType groupType,int groupNumber, CANByteOrder byteOrder, int startBit, int bitLength, CANDataType dataType,
                     double factor, double offset, double signalMinValuePhys, double signalMaxValuePhys, String unit,Set<String> sigReceiveNodeSet) {
        this.signalName = signalName;
        this.groupType = groupType;
        this.groupNumber = groupNumber;
        this.byteOrder = byteOrder;
        this.startBit = startBit;
        this.bitLength = bitLength;
        this.dataType = dataType;
        this.factor = factor;
        this.offset = offset;
        this.signalMinValuePhys = signalMinValuePhys;
        this.signalMaxValuePhys = signalMaxValuePhys;
        this.unit = unit;
        this.sigReceiveNodeSet = sigReceiveNodeSet;
    }

    public void setTarget(Object target) {
        this.target = target;
    }
    public void setField(Field field) {
        this.field = field;
    }
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    public String getSignalComment() {
        return signalComment;
    }
    public Object getTarget() {
        return target;
    }
    public Field getField() {
        return field;
    }
    public String getSignalName() {
        return signalName;
    }
    public String getStrGroupValue() {
        return strGroupValue;
    }
    public CANByteOrder getByteOrder() {
        return byteOrder;
    }
    public int getStartBit() {
        return startBit;
    }
    public int getBitLength() {
        return bitLength;
    }
    public double getFactor() {
        return factor;
    }
    public double getOffset() {
        return offset;
    }
    public GroupType getGroupType() {
        return groupType;
    }
    public int getGroupNumber() {
        return groupNumber;
    }
    public CANDataType getDataType() {
        return dataType;
    }
    public double getSignalMinValuePhys() {
        return signalMinValuePhys;
    }
    public double getSignalMaxValuePhys() {
        return signalMaxValuePhys;
    }
    public double getIniValuePhys() {
        return iniValuePhys;
    }
    public double getIniValueHex() {
        return iniValueHex;
    }
    public boolean isValid() {
        return valid;
    }
    public String getUnit() {
        return unit;
    }
    public Set<String> getSigReceiveNodeSet() {
        return sigReceiveNodeSet;
    }
}   //  class CanSignal