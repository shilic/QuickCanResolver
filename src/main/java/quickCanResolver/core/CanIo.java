package quickCanResolver.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** 兼容层，兼容不同的 底层CAN收发实现 */
public class CanIo implements CanSendService {
    private static volatile CanIo canIo;
    public CanManagerService manager;
    private McuService mcuService;
    private CanListenService listenService;

    /** 注册底层的MCU，以及报文监听函数。 */
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
    public void unRegisterAll() {
        mcuService.nativeUnRegister(listenService);
        manager.clear();
    }
    public void unRegisterCanListener() {
        mcuService.nativeUnRegister(listenService);
    }

    @Override
    public void send(int canId, int[] data8) {
        mcuService.nativeSend(canId,data8);
    }
    @Override
    public void send(int canId) {
        mcuService.nativeSend(canId, manager.enCode_I(canId));
    }
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

    private CanIo() {
        manager = CanManagerImp.getInstance();
        System.out.println("CanIo：兼容层 CanIo 初始化完成 ");
    }
}
