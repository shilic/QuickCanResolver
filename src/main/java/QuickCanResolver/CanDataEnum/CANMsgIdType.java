package QuickCanResolver.CanDataEnum;

/**
 * 帧ID类型  <br>标准帧 Standard 范围 0x0~0x7FF  ;<br>扩展帧 Extended 范围 0x0~0x1FFF_FFFF ;
 */
public enum CANMsgIdType {
    /** 标准帧 Standard 范围 0x0~0x7FF */
    Standard,
    /** 扩展帧 Extended 范围 0x0~0x1FFF_FFFF */
    Extended
}
