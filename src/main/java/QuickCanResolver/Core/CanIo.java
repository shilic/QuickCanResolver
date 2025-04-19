package QuickCanResolver.Core;

/** 兼容层，兼容不同的 底层CAN收发实现 */
public class CanIo implements CanSendService {
    protected static volatile CanIo canIo;
    public CanManagerService manager;
    McuService mcuService;

    /** 注册底层的MCU，以及报文监听函数。 */
    public void register(McuService mcuService,CanListenService listenService) {
        this.mcuService = mcuService;
        mcuService.nativeRegister(listenService);
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
    }
}
