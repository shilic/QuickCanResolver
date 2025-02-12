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

    /**
     * 测试绑定耗时
     */
    @Test
    public void test() {
        long startTime = System.currentTimeMillis();
        // 初次绑定
        manager.bindDbc(model);
        long endTime = System.currentTimeMillis();
        bindTimeCost = endTime - startTime;
        System.out.println("初次绑定 耗时 : " + bindTimeCost +" 毫秒"); // 初次绑定耗时 : 70 毫秒


        startTime = System.currentTimeMillis();
        // 再次绑定
        manager.reBindModel(model);
        endTime = System.currentTimeMillis();
        bindTimeCost = endTime - startTime;
        System.out.println("再次绑定 耗时 : " + bindTimeCost +" 毫秒"); // 再次绑定耗时 : 0 毫秒
    }

    /**
     * 测试产生新对象的耗时
     */
    @Test
    public void createNewModelTest() {
        CarDataModel oldModel = new CarDataModel();
        manager.bindDbc(oldModel); // 初次绑定

        // 使用 CanIOHandler ，赋初始值。
        CanIOHandler canIOHandler = manager.getCanIo("testDbc");
        canIOHandler.update_B(msg1_Id,data8_);

        System.out.println("oldValue = "+ oldModel.getMsg1Value());
        System.out.println("-oldModel = " + oldModel);

        // 部分刷新数据
        byte[] newData8 = new byte[]{7, 8, 9, 10, (byte) 211, 121, (byte) 200, 100};


        // 根据新数据和旧对象，拷贝对象
        long startTime = System.currentTimeMillis();
        CarDataModel newObj = manager.createNewDataModel(msg1_Id, newData8, oldModel); // 生成一个新的对象
        long endTime = System.currentTimeMillis();

        bindTimeCost = endTime - startTime;
        System.out.println("拷贝对象 耗时 : " + bindTimeCost +" 毫秒"); // 拷贝对象 耗时 : 0 毫秒

        System.out.println("-newObj = " + newObj);
        System.out.println("newObj Value = "+ newObj.getMsg1Value());
        // old Value = Msg1 = {msg1_sig1 :30, msg1_sig2 :29, msg1_sig3 = 28, msg1_sig4 = 20 , msg1_sig5: 22.200000000000003, msg1_sig6 :10.5, msg1_sig7 = -80.5, msg1_sig8 = 110.0}
        // newObj Value = Msg1 = {msg1_sig1 :7, msg1_sig2 :8, msg1_sig3 = 9, msg1_sig4 = 10 , msg1_sig5: 22.200000000000003, msg1_sig6 :10.5, msg1_sig7 = -80.5, msg1_sig8 = 110.0}
        // oldModel = Demo1.CarDataModel@7ce6a65d
        // newObj = Demo1.CarDataModel@6043cd28 可见，确实生成了两个不一样的对象。
    }

    /**
     * 测试拷贝多个对象的耗时
     */
    @Test
    public void loopTest(){
        CarDataModel model = new CarDataModel();
        manager.bindDbc(model); // 初次绑定
        //System.out.println("model = " + model);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i< loopTime ;i++){
            CarDataModel newObj = manager.createNewDataModel(msg1_Id, data8_, model); // 生成一个新的对象
            //System.out.println("-newObj = " + newObj);
        }
        long endTime = System.currentTimeMillis();
        bindTimeCost = endTime - startTime;
        System.out.println("拷贝对象 "+ loopTime +"次， 耗时 : " + bindTimeCost +" 毫秒");
        // 一万次稳定 80 毫秒。 十万次稳定 300 毫秒
    }
    @Test
    public void updateTest(){
        byte[] newData8 = new byte[]{7, 8, 9, 10, (byte) 211, 121, (byte) 200, 100};
        CarDataModel oldModel = new CarDataModel();
        manager.bindDbc(oldModel); // 初次绑定


        manager.update_B(msg1_Id,data8_); // 赋初始值
    }

    /**
     * 测试产生报文的耗时
     */
    @Test
    public void getCanFrameTest(){
        manager.bindDbc(model); // 初次绑定

    }
}
