package QuickCanResolver.CanTool;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 该类是工具类，用于 int、 byte 、bit 、String 等数据类型之间相互转换
 */
public class MyByte {
    /** 正则表达式，判断是否是word格式，以字母或者下划线开头，并且只能是字母或者下划线还有数字组成*/
    public static final String isWordRegex = "\\b[a-zA-Z_]+\\w*";
    /** 此变量用于调整单词长度，为了适配之前的协议，我默认成了128。为了更好的兼容性最好单词长度小于32。*/
    static int wordLength = 128;


    /**
     * 转换的数据格式，英特尔或者摩托罗拉。<br>英特尔格式，数据的低位存放在数组最低位。<br>摩托罗拉格式，数据的高位存放在数组最高位。
     */
    public enum DataType {
        Intel,Motorola
    }
    /* 大转小 */
    /**
     * 传入一个整形数，得到一个不定长的整形数组，每一个数组元素表示一个bit<br>
     * 函数算法，位运算，int32位，从最高位开始取值,记录最高位下标，生成数组，然后依次填充数组
     * @param data 输入一个整形数
     * @param type 数据类型 "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至数组低位
     * @return bit数组，每一个表示一个bit。例如15 变成 {1,1,1,1} 。
     */
    public static byte[] intToBits(int data,DataType type){
        int a = data;
        byte[] bitArray;
        int arrayLength = 0;
        //循环，获取第一个1的位置，即获取返回数组的长度
        for(int i=30;i>=0;i--){ //下标31正负数标志位，下标30 第一个高位
            a = a <<1;
            int temp = a & 0x8000_0000;//取首位bit
            if(temp == 0x8000_0000){ //提取到第一个1时，退出循环
                arrayLength = i+1; //例如15的长度是 4
                break;
            }
        }
        bitArray = intToBits(data,type,arrayLength);
        return bitArray;
    }

    /**
     * 传入一个整形数，得到一个定长的整形数组，每一个数组元素表示一个bit<br>
     * 函数算法，位运算，int32位，从最高位开始取值,记录最高位下标，生成数组，然后依次填充数组
     * @param data 输入一个整形数
     * @param type 数据类型 "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至数组低位
     * @return bit数组，每一个表示一个bit。例如2 长度5 ，输出  [0, 1, 0, 0, 0];
     */
    public static byte[] intToBits(int data,DataType type,int bitLength){
        byte[] bitArray = new byte[bitLength];
        switch (type){
            case Intel:default:
                for(int i=0;i<bitLength;i++){
                    bitArray[i] = (byte) ((data) & 0x0000_0001); //把高位存放到高位 //英特尔格式
                    data = data >> 1;
                }
                break;
            case Motorola:
                for(int i=bitLength-1;i>=0;i--){
                    bitArray[i] = (byte) (data & 0x0000_0001); //把data低位存放到高位，最高位放到0方便显示
                    data = data >> 1 ;
                }
                break;
        }
        return bitArray;
    }

    /**
     * 把一个 int 转换成 32个bits 。和intToBits方法的区别在于，intToBits方法不定长，本方法定长32
     * @param t 要转换的int数据
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 32个bits数组
     */
    public static byte[] intTo32Bits(int t,DataType type){
        byte[] re = new byte[32];  //例如传入一个4位字节数组，那么长度就是32个bit
        switch (type){
            case Intel: default:
                for(int i=0;i<32;i++){
                    re[i] = (byte)(t & 0x0000_0001);
                    t = (t >> 1);
                }
                break;
            case Motorola:
                for (int i = 31; i >= 0; i--) {    //把int按位取出
                    re[i] = (byte)(t & 0x0000_0001);    //把最低位赋值给re[31] ;最高位放到re[0] 摩托罗拉格式
                    t = (t >> 1);  //整体右移1位bit
                }
                break;
        }
        return re;
    }
    /**
     * 把一个int数，转换成4位byte数据 ，并用int表示
     * @param data 传入的int数
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 4位byte数据 ，并用int表示
     */
    public static int[] intToBytesI(int data,DataType type){
        int[] ans = new int[4];
        switch (type) {
            case Intel: default:
                for(int i = 0;i<4;i++) {
                    ans[i] = data & 0x0000_00FF; //低位放低位，高位放高位
                    data = data >> 8;
                }
                break;
            case Motorola:
                for(int i =3;i>=0;i--) { // 把int 转换成 4 位 bytes ; 高位放低位下标 0x18FFAB03
                    ans[i] = data & 0x0000_00FF; //取 int 的最低8位放到 ans[3] ,高位放低位下标
                    data = data >> 8;
                }
                break;
        }
        return ans;
    }

    /**
     * 把一个int数，转换成4位byte数据 ，并用byte表示
     * @param data 传入的int数
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 4位byte数据 ，并用byte表示
     */
    public static byte[] intToBytes(int data,DataType type) {  // 把int 转换成 4 位 bytes ; 高位放低位下标 0x18FFAB03
        byte[] ans = new byte[4];
        switch (type){
            case Intel: default:
                for(int i = 0;i<4;i++){
                    ans[i] = (byte) (data & 0x0000_00FF); //低位放低位，高位放高位
                    data = data >> 8;
                }
                break;
            case Motorola:
                for(int i =3;i>=0;i--){ // 把int 转换成 4 位 bytes ; 高位放低位下标 0x18FFAB03
                    ans[i] = (byte) (data & 0x0000_00FF); //取 int 的最低8位放到 ans[3] ,高位放低位下标
                    data = data >> 8;
                }
                break;
        }
        return ans;
    }

    /**
     * 将byte转换为一个长度为8的bit数组，数组每个值代表一个bit，
     * @param mByte 传入一个字节
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 返回一个8位的数组
     */
    public static byte[] byteTo8Bits(byte mByte, DataType type) {
        byte[] array = new byte[8];
        switch (type){
            case Intel: default:
                for(int i = 0;i<8;i++){
                    array[i] = (byte)(mByte & 0b0000_0001); //bytes&0b00000001 //取最后一位放0
                    mByte = (byte) (mByte >> 1); //右移
                }
                break;
            case Motorola:
                for (int i = 7; i >= 0; i--) { //摩托罗拉格式  例如 32 = 0010 0000    0123 4567
                    array[i] = (byte)(mByte & 0b0000_0001); //bytes&0b00000001 //取最后一位放最高位7   0123 4567
                    mByte = (byte) (mByte >> 1); //右移，继续取最后一位放6，依次类推
                }
                break;
        }
        return array;
    }

    /**
     * 把 一个长度为4 的数组转换成 32位 的bit数组
     * @param bytes 长度为4的byte数组
     * @param type  要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return  32位 的bit数组
     */
    public static byte[] from4BytesTo32Bits(byte[] bytes, DataType type) {
        long t = from4bytesToLong(bytes,type);   //把4个byte组合成一个int
        byte[] re =new byte[32];  //例如传入一个4位字节数组，那么长度就是32个bit
        switch (type){
            case Intel: default:
                for (int i = 0;i<32;i++) {
                    re[i] = (byte)(t & 0x0000_0001);
                    t = (t >>> 1);
                }
                break;
            case Motorola:
                for (int i = 31; i >= 0; i--) {//低位放高位，方便查看//把int按位取出 转换int为一个长度为32的byte数组，数组每个值代表bit
                    re[i] = (byte)(t & 0x0000_0001);    //把最低位赋值给re[31] 最高位给 re[0]
                    t = (t >>> 1);  //整体右移1位bit
                }
                break;
        }
        return re;
    }

    /**
     * 将 长度是8的byte数组转换成 64个bits数组，每个表示0或者1
     * @param bytes 长度是8的byte数组
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return  64个bits数组，每个表示0或者1
     */
    public static byte[] from8BytesTo64Bits(byte[] bytes, DataType type) {
        byte[] bits = new byte[64];
        for(int i=0;i<8;i++){
            System.arraycopy(byteTo8Bits(bytes[i],type),0,bits,i*8,8); //默认摩托罗拉格式
        }
        return bits;
    }



    /*
    * 小转大
    * */

    /**
     * 输入任意长度的 bits数组，长度小于等于32，转换成一个int型数据
     * @param bits 任意长度的 bits数组
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 一个int型数据
     */
    public static int bitsToInt(byte[] bits,DataType type) {
        int re = 0;
        int len = bits.length;
        switch (type){
            case Intel: default:
                for(int i = len-1; i>=0;i--){  //数据高位存放到高位；把数据低位存放到低位
                    re = re << 1;
                    re = re | (bits[i] & 0x0000_0001); // len-1 存放到最高位
                }
                break;
            case Motorola:
                for(int i =0;i<len;i++){
                    re = re << 1;
                    re = re | (bits[i] & 0x0000_0001);  //把 bits[0]存放到  数据的最高位
                }
                break;
        }
        return re;
    }
    /**
     * 传入 8个比特的数组，转换成一个byte ，用int表示
     * @param bits 8个比特的数组，每个元素为一个bit,不是0就是1
     * @param type 转换类型 "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 一个byte数据 ,采用int表示
     */
    public static int from8bitsToByte(byte[] bits, DataType type) { //传入8个bit 变成一个byte //低位在低位，高位在高位
        int b = 0b0000_0000;
        switch (type){
            case Intel: default:
                for (int i=7;i>=0;i--){
                    b = b << 1; //左移一位 //如果放后边会出问题，最后一次循环之后，依旧会左移，导致数据丢失;所以放循环体第一句
                    b = b | bits[i]; //把最高位放进去 ; | 运算， 只要有1就是1 ; & 运算，两个都是1才是1
                }
                break;
            case Motorola:
                for(int i=0;i<8;i++){
                    b = b << 1 ;
                    b = b | bits[i]; //如果该序列为逆序，将最低位存放到 b 的最高位
                }
                break;
        }

        return b;
    }

    /**
     * 传入一个 大小为64的 bit数组,转换成 一个大小为8的 byte数组，用int表示
     * @param bits64 传入一个 大小为64的 bit数组
     * @param type 转换类型 "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 一个大小为8的 byte数组，用int表示
     */
    public static int[] from64bitsTo8BytesI(byte[] bits64, DataType type) {
        int[] re = new int[8];
        // 1. Arrays.copyOfRange 方法，从 bits64数组中取8个数出来，
        // 2. 再用 from8bitsToByte 方法 把这个数组变成一个 byte ,
        // 3. 最后储存到新建的数组中,循环。
        for(int index=0;index<8;index++) { // 可以使用串行或者并行的方式执行
            re[index] = from8bitsToByte(Arrays.copyOfRange(bits64,index*8,index*8+8),type); //不包含i*8+8
        }
        return re;
    }

    /**
     * 把 32个比特转换成一个 int 型整数 .
     * @param bits 传入的长度为32的比特数组，每一位元素必须是0或者1.
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return 返回值 得到一个 int型整数 ,用 long来表示，可根据需要转换成 int
     */
    public static long from32bitsToInt(byte[] bits, DataType type) {
        long re = 0x0000_0000L;
        switch (type){
            case Intel: default:
                for (int i=31;i>=0;i--){  //传入32位的 bit数组，变成int //低位在低位，高位在低位
                    re = re << 1; //左移一位 //如果放后边会出问题，最后一次循环之后，依旧会左移，导致数据丢失;所以放循环体第一句
                    re = re | bits[i]; //把最高位放进去 ; | 运算， 只要有1就是1 ; & 运算，两个都是1才是1
                }
                break;
            case Motorola:
                for(int i=0;i<32;i++){
                    re = re <<1;
                    re = re | bits[i]; //把 传入数组 的低位放到最终结果的高位，大端模式
                }
                break;
        }
        return re;
    }

    /**
     * 将java有符号byte转换成无符号byte，用int模拟
     * @param b 传入字节
     * @return 返回一个int类型
     */
    public static int byteToUnsigned(byte b) {
        return b&0x0000_00ff;
    }
    /**
     * 4位byte转int  例如  0x18 0xFE 0x01 0x1b 得到 0x18fe011b
     * @param bytes 传入长度为4的字节数组
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return int 返回由这4位数组组成的int型数据
     */
    public static int from4bytesToInt(byte[] bytes, DataType type) {
        int ans=0;
        switch (type){
            case Intel: default:
                for(int i=3;i>=0;i--){ // 假设 bytes[3]存放的是最高位数据
                    ans = ans << 8;//左移 8 位
                    ans = ans |(bytes[i]& 0x0000_00ff);
                }
                break;
            case Motorola:
                for(int i=0;i<4;i++){ //数据高位摆放到数组低位 bytes[0]存放最高位数据
                    ans = ans << 8;//左移 8 位
                    ans = ans |(bytes[i]& 0x0000_00ff);//保存 byte 值到 ans 的最低 8 位上
                }
                break;
        }
        return ans;
    }  //from4bytesToInt
    /**
     * 把4个字节组合在一起变成int,防止数据有负数，于是用long,例如  0x18 0xFE 0x01 0x1b 得到 0x18fe011b
     * @param bytes 传入一个长度为4的字节数组
     * @param type 要转换的类型  "motorola":大端模式,数据高位存放数组低位 ; "intel" :低位存放至低位
     * @return long 返回由这4位数组组成的long型数据
     */
    public static long from4bytesToLong(byte[] bytes, DataType type){
        long ans=0;
        switch (type) {
            case Intel: default:
                for(int i=3;i>=0;i--) { // 假设 bytes[3]存放的是最高位数据
                    ans = ans << 8;//左移 8 位
                    ans = ans |(bytes[i]& 0x0000_00ffL);
                }
                break;
            case Motorola:
                for(int i=0;i<4;i++) { // 数据高位摆放到数组低位 bytes[0]存放最高位数据
                    ans = ans << 8;//左移 8 位
                    ans = ans |(bytes[i]& 0x0000_00FFL);//保存 byte 值到 ans 的最低 8 位上
                }
                break;
        }
        return ans;
    }
    /**
     * 把两个字节组合在一起,例如  0xAF 0xFE 得到0xAFFE ,默认摩托罗拉类型
     * @param high 数据高位
     * @param lower 数据低位
     * @return 合成一个int
     */
    public static int from2byteToInt(byte high, byte lower){
        return ((high & 0x0000_00ff)<<8)|(lower & 0x0000_00ff);
    }


    /*
    * 打印方法
    *  */
    /**
     * 将 double 设置为一位小数
     * @param number 你要输入的double类型数值
     * @return 返回只保留一位小数的字符串
     */
    public static String doubleToStr(double number){
        // 创建 DecimalFormat 对象，并设置保留两位小数的格式 #0.00; 一位小数 #0.0
        DecimalFormat decimalFormat = new DecimalFormat("#0.0");
        // 将 double 数转换为字符串并保留两位小数
        return decimalFormat.format(number);
    }

    /**
     * 把 double 变成 String 类型 ，保留8位小数
     * @param value double类型变量
     * @return String 类型
     */
    private static String doubleToString8(double value){
        DecimalFormat decimalFormat = new DecimalFormat("0.########");
        return decimalFormat.format(value);
    }

    /**
     * 静态函数，输入一段字符串 ，判断是否是 word类型 ， 即0-9数字，英文字母和下划线 ，即 \\w+ ，并且字符数必须小于32，并且以 英文字母和下划线 作为开头
     * @param value 输入一段字符串
     * @return 如果是word类型则返回真，如果不是则返回假
     */
    public static boolean isWord(String value){
        if (value !=null && value.length()<wordLength){
            return Pattern.matches(isWordRegex,value);  // "\\b[a-zA-Z_]+\\w*"
        }else {
            return false;
        }
    }

    public static boolean isWord(String value , int wordLength) {
        if (value !=null && value.length()<wordLength){
            return Pattern.matches(isWordRegex,value);  // "\\b[a-zA-Z_]+\\w*"
        }else {
            return false;
        }
    }
    public static String hex2_Str(byte[] data){ //传入一个字节数组，输出16进制字符串
        StringBuilder hexString = new StringBuilder();
        hexString.append(" [ ");
        for (byte datum : data) {
            hexString.append(String.format("%02X", datum)).append(","); // 将byte转换为长度为2的16进制字符串
        }
        hexString.append(" ] ");
        return hexString.toString();
    }
    public static String hex2_Str(int[] data){ //传入一个字节数组，输出16进制字符串
        StringBuilder hexString = new StringBuilder();
        hexString.append(" [ ");
        for (int datum : data) {
            hexString.append(String.format("%08X", datum)).append(","); // 将byte转换为长度为2的16进制字符串
        }
        hexString.append(" ] ");
        return hexString.toString();
    }
    public static String hex2Str(byte[] data){ //传入一个字节数组，输出16进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte datum : data) {
            hexString.append(String.format("%02X", datum)); // 将byte转换为长度为1的16进制字符串
        }
        return hexString.toString();
    }
    public static String hex2Str(byte data){ //传入一个字节，输出16进制字符串
        return String.format("%02X",data);
    }
    public static String hex2Str(byte[] data,int a){
        BigInteger big = new BigInteger(a,data); // a = 1表示正数 -1负数
        String str = big.toString(16);
        //System.out.println("data64 = " + str);
        return str;
    }
    public static String hex2Str(int data){
        return String.format("%08X",data);
    }
    /**
     * 把byte转为字符串的bit
     * @param b 传入一个字节
     * @return 返回一个数组
     */
    public static String byteToBitString(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
    }
    /**
     * 二进制字符串转byte
     * @param byteStr 传入
     * @return 返回
     */
    public static byte BinaryStr2Byte(String byteStr) {
        int re, len;
        if (null == byteStr) {
            return 0;
        }
        len = byteStr.length();
        if (len != 4 && len != 8) {
            return 0;
        }
        if (len == 8) {  // 8 bit处理
            if (byteStr.charAt(0) == '0') {  // 正数
                re = Integer.parseInt(byteStr, 2);
            } else {  // 负数
                re = Integer.parseInt(byteStr, 2) - 256;
            }
        } else {  // 4 bit处理
            re = Integer.parseInt(byteStr, 2);
        }
        return (byte) re;
    }
    public static void bytePrint(byte a){//将 byte 按位从左到右打印
        int count=0;
        for(int i=7;i>=0;i--){
            System.out.print((a>>i)&1);
            count++;
            if(count==4){//每四位为一组，用空格分开
                System.out.print(" ");
                count=0;
            }
        }
        System.out.println();
    } //bytePrint

    /**
     *  @deprecated 为了提高框架运行效率，现将校验删除，故尝试去掉报错检测。
     */
    @Deprecated
    public static class InputDataException extends RuntimeException {
        public InputDataException(String message) {
            super(message);
        }
    }
}//class MyByte
