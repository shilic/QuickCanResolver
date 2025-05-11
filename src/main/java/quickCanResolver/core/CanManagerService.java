package quickCanResolver.core;

public interface CanManagerService {
    /**框架最主要方法 ： 绑定 dbc <br>
     * 使用注解，直接绑定dbc和数据模型，省略手动调用的步骤。<br>
     * 已经封装好所有步骤，供外部直接调用。
     * @param clazz 数据模型Class
     */
    <T extends CanCopyable<T>> T bind(Class<T> clazz);
    <T extends CanCopyable<T>> T bind(T model);
    /**
     * 返回绑定的模型。当多个模型和多个报文进行绑定时，接收一个报文可能返回多个数据模型。
     * */
    <T extends CanCopyable<T>> T getModel(Class<T> clazz);
    /**
     * 使用新的数据，拷贝一个新的数据对象出来。<br> 用于提供给 LiveData 和 viewModel。<br>
     * @return 新的对象。
     */
    <T extends CanCopyable<T>> T createNewModel(Class<T> clazz);
    /**
     * 接收数据，解码报文。 将接收到的CAN报文，解析后存入绑定好的数据模型中
     */
    void deCode_B(int canId, byte[] data8);
    /**
     * 编码数据，发送报文。
     */
    int[] enCode_I(int canId);



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
