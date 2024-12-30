package QuickCanResolver.CanDataEnum;

/*
* 解释LSB和MSB。
* 在英特尔格式下，LSB指数据的最低位，也指储存空间的最低位。
* 在摩托罗拉格式下，MSB指数据的最低位，LSB指数据的最高位。
* 默认 摩托罗拉格式 采用MSB，之前的代码采用MSB格式。
* */
/**
 * 1.排列格式 Intel, Motorola_MSB , Motorola_LSB 。<br>
 * 2.英特尔格式,数据的最低位存放在矩阵的最低位，数据最高位放在矩阵的最低位。<br>
 * 3.摩托罗拉格式,数据最低位存放在矩阵的高位。
 */
public enum CANByteOrder {
    /** 英特尔格式 */
    Intel,
    /** 摩托罗拉格式默认为 Motorola_MSB ;该格式与 DBC 文件(即用txt打开)的格式保持一致 ，即信号的起始位是 数据最低位 ，在矩阵中的位置是 MSB 。*/
    Motorola_MSB,
    /** 摩托罗拉格式 Motorola_LSB ;该格式与 CANdb++ 软件中的格式一致 ，即信号的起始位是 数据最高位 ，在矩阵中的位置是 LSB 。*/
    Motorola_LSB,
    /**
     * 默认摩托罗拉格式，默认为Motorola_MSB，可自适应MSB和LSB
     * @deprecated 暂时弃用该枚举，因为无法写出一个算法同时判断 MSB 和 LSB，这是不可能的。
     * */
    @Deprecated
    Motorola_Default,
    Error,
}
