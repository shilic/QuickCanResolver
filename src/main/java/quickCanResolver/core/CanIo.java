package quickCanResolver.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** CANIO兼容层，兼容不同的 底层CAN收发实现 */
public class CanIo implements CanSendService {
    private static volatile CanIo canIo;
    /** CAN报文编解码器的管理器，报文的解码主要由管理器在实现。 */
    public final CanManagerService manager;
    /** MCU服务，使用依赖注入的方式使用底层服务，再由适配器在外部实现该服务。 */
    private McuService mcuService;
    /** 报文监听事件，使用依赖注入的方式实现。 */
    private CanListenService listenService;

    /** 注册底层的MCU，以及报文监听函数。
     * @param clazz 传入MCU适配器，类型：McuService，需要在适配器中完成底层MCU和上层接口的对接。
     * @param listenService 监听事件
     * */
    public void register(Class<? extends McuService> clazz, CanListenService listenService) {
        try {
            // 获取无参构造函数
            Constructor<? extends McuService> constructor = clazz.getDeclaredConstructor();
            // 允许访问私有构造函数
            constructor.setAccessible(true);
            // 创建实例
            this.mcuService = constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        this.listenService = listenService ;
        mcuService.nativeRegister(listenService);
    }
    /** 取消注册所有内容，包括监听事件和绑定对象 */
    public void unRegisterAll() {
        mcuService.nativeUnRegister(listenService);
        manager.clear();
    }

    /**
     * 取消注册监听事件
     */
    public void unRegisterCanListener() {
        mcuService.nativeUnRegister(listenService);
    }
    /** 手动发送一组报文
     * @param canId canId
     * @param data8 8位数组的报文
     * */
    @Override
    public void send(int canId, byte[] data8) {
        mcuService.nativeSend(canId,data8);
    }
    /** 根据id发送报文
     * @param canId canId
     * */
    @Override
    public void send(int canId) {
        // 调用manager得到一组编码后的数据，再调用底层接口发送
        mcuService.nativeSend(canId, manager.enCode_B(canId));
    }

    /**
     * 获取实例
     * @return 返回CANIO实例
     */
    public static CanIo getInstance() {
        if (canIo == null){
            synchronized (CanIo.class){
                if (canIo == null){

                    return canIo = new CanIo();
                }
            }
        }
        return canIo;
    }

    /**
     * 私有构造
     */
    private CanIo() {
        manager = CanManagerImp.getInstance();
        System.out.println("CanIo：兼容层 CanIo 初始化完成 ");
    }
}
