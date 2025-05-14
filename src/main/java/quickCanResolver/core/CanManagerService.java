package quickCanResolver.core;

/**
 * CAN报文编解码器的管理器接口
 */
public interface CanManagerService {
    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型 。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param clazz 数据模型Class
     */
    <T extends CanCopyable<T>> T bind(Class<T> clazz);
    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型 。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param model 数据模型
     */
    <T extends CanCopyable<T>> T bind(T model);
    /**
     * 返回绑定的初始模型。随时随地获取绑定的初始对象。
     * */
    <T extends CanCopyable<T>> T getModel(Class<T> clazz);
    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * @return 新的对象。
     */
    <T extends CanCopyable<T>> T createNewModel(Class<T> clazz);
    /**
     * 接收数据，解码报文。 将接收到的CAN报文，解析后存入绑定好的数据模型中
     * @param canId canId
     * @param data8 报文数组
     */
    void deCode_B(int canId, byte[] data8);
    /**
     * 编码数据，发送报文。
     * @param canId canId
     */
    byte[] enCode_B(int canId);

    /**
     * 使用一个新的对象，编码数据，发送报文。
     * @param canId canId
     * @param model 使用一个数据模型用来生成报文，而不是之前的模型
     * @return 返回解码后的报文
     */
    byte[] enCode_B(int canId,Object model);
    /**
     * 添加一个 DBC输入接口
     * @param dbcInputInterface DBC输入接口
     * @return 返回自身，便于链式调用
     */
    CanManagerService addDbcInputInterface(DbcInputInterface dbcInputInterface);

    /**
     * 取消注册DBC
     * @param dbcTag  对应的DBC标签
     */
    void clearDBC(String dbcTag);
    /**
     * 取消所有DBC的注册
     */
    void clearAllDbc();

    /**
     * 清理所有注册项
     */
    default void clear() {
        clearAllDbc();
    }
}
