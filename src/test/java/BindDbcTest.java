import Demo.CarDataModel;
import QuickCanResolver.CanHandle.CanIOHandler;
import QuickCanResolver.CanHandle.CanObjectMapManager;
import org.junit.Test;

public class BindDbcTest {
    static final int loopTime = 100000;
    static final int msg1_Id = 0x18AB_AB01 ; // message1
    static byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};
    CanObjectMapManager manager = CanObjectMapManager.getInstance();
    CarDataModel model = new CarDataModel();
    long bindTimeCost;


}
