package quickCanResolver.dbc.CanDataEnum;

/*
分组类型，或者说分组模式、即多路复用。当你需要在一个报文矩阵中的同一个位置，塞入两个或以上的报文时，就需要采用多路复用，也就是分组功能。
否则会出现报文id重复错误，或者出现报文重叠错误。
请填入以下三种类型的数据：默认分组，分组标志位和组号。
1、默认分组。
请填入 ：默认、默认分组、不分组、Default、DefaultGroup、singal 或者你干脆直接空着不填(推荐默认分组直接不填)（注意：是叫你不填，并不不是让你填写0）。
默认分组就表示最基本的状态，如果该帧报文全是默认分组，则只可以表示64bit的数据。
如果该帧报文同时还存在其他分组报文，那么默认分组的报文会出现在所有的分组中，并且位置一致。
就是说，当不同分组的报文发过来的时候，默认分组的报文都存在。
2、分组标志位。
请填入：分组标志位、标志位、GroupFlag、Flag。即可被识别成标志位。
一个报文只可以有一个分组标志位。
标志位用于储存并表示当前报文的组号。虽然有了分组，信号就可以在该帧同一个位置表示多个数据，但是一次只能发送一个分组的数据，
只能同时激活一组数据(当然，默认分组会一直处于激活状态)。故标志位就是用于表示该组数据的组号。不同分组的报文发过来的时候，分组标志位也不同。
3、组号。
请填入：“组号：67(替换成你的组号)”，“Num：50(替换成你的组号)”,“32(或者你直接填写一个数字也是可以的)”。
推荐直接填写数字组号即可，只能填写大于等于0的整型数。
组号用于标注分组，实现在8*8矩阵的同一个位置放上好几个数据。
当当前报文的分组标志位是67时，只激活组号67的报文，程序按照组号67的规则来解析，以此类推。
*/
/**
 * 分组模式，分组类型，又称多路复用。 <br>
 * 可选值为 Default_Group(表示默认分组，不分组) ; <br>
 * Group_Flag(分组标志位) ; <br>
 * Group_Number(组号) ; <br>
 */
public enum GroupType {  // 多路复用 GroupType
    /** 默认分组(不分组) Signal */
    Default_Group,
    /** 分组标志位 Multiplexor */
    Group_Flag,
    /** 组号 Multiplexed */
    Group_Number
}
