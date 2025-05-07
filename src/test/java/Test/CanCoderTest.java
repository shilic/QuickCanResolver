package Test;

import Demo.CarDataModel;
import quickCanResolver.core.CanCoder;
import quickCanResolver.core.CanFrameData;
import quickCanResolver.core.CanManagerImp;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CanCoderTest {
    static int num = 100000;
    static final int loopTime = 100000;
    static final int msg1_Id = 0x18AB_AB01 ; // message1
    static final int msg2_Id = 0x18AB_AB02; //
    static final int msg3_Id = 0x18AB_AB03; //
    /* 1.完成DBC和数据模型的绑定。 */
    static String path1 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\Example.dbc";
    static String path2 = "E:\\storge\\very\\code\\IntelliJ_IDEA_Project\\QuickCanResolver\\src\\main\\resources\\DBC\\Example2.dbc";

    /** model = Msg1 = { <br>
     msg1_sig1 :30,<br>
     msg1_sig2 :29,<br>
     msg1_sig3 = 28,<br>
     msg1_sig4 = 20 ,<br>
     msg1_sig5: 22.200000000000003,<br>
     msg1_sig6 :10.5,<br>
     msg1_sig7 = -80.5,<br>
     msg1_sig8 = 110.0}<br>
    * */
    static byte[] data8_ = new byte[]{30, 29, 28, 20, (byte) 211, 121, (byte) 200, 100};


    CarDataModel model = new CarDataModel();
    CanManagerImp canManagerImp = CanManagerImp.getInstance();
    List<CanFrameData> canFrameDataList;
    List<CanFrameData> canFrameDataList2;
    long bindTimeCost;
    {
        /* 手动绑定存在很多问题，故也准备把这一部分优化了 */
        canManagerImp.addDbcToMap("testDbc",path1); // 绑定DBC文件
        canManagerImp.addDbcToMap("testDbc2",path2); // 绑定DBC文件

        long startTime = System.currentTimeMillis();
        canManagerImp.bindModelAndField(model.getClass(),model); // 绑定数据模型
        long endTime = System.currentTimeMillis();
        bindTimeCost = endTime - startTime;
        //System.out.println("绑定耗时 : " + bindTimeCost+" 毫秒");

        canFrameDataList = new ArrayList<>();
        CanFrameData canFrameData1 = new CanFrameData(msg1_Id,data8_);
        CanFrameData canFrameData2 = new CanFrameData(msg2_Id,data8_);
        CanFrameData canFrameData3 = new CanFrameData(msg3_Id,data8_);

        CanFrameData canFrameData4 = new CanFrameData(msg1_Id,data8_);
        CanFrameData canFrameData5 = new CanFrameData(msg2_Id,data8_);
        CanFrameData canFrameData6 = new CanFrameData(msg3_Id,data8_);

        CanFrameData canFrameData7 = new CanFrameData(msg1_Id,data8_);
        CanFrameData canFrameData8 = new CanFrameData(msg2_Id,data8_);
        CanFrameData canFrameData9 = new CanFrameData(msg3_Id,data8_);

        canFrameDataList.add(canFrameData1);
        canFrameDataList.add(canFrameData2);
        canFrameDataList.add(canFrameData3);
        canFrameDataList.add(canFrameData4);
        canFrameDataList.add(canFrameData5);
        canFrameDataList.add(canFrameData6);
        canFrameDataList.add(canFrameData7);
        canFrameDataList.add(canFrameData8);
        canFrameDataList.add(canFrameData9);

        canFrameDataList2 = new ArrayList<>();
        canFrameDataList2.add(canFrameData1);
        canFrameDataList2.add(canFrameData2);
        canFrameDataList2.add(canFrameData3);

    }


    /**
     * 测试绑定耗时
     */
    @Test
    public void bindTimeCostTest(){
        System.out.println("绑定耗时 : " + bindTimeCost+" 毫秒");
        // 耗时10毫秒左右
    }
    /**
     * 采用了并发流处理报文
     */
    @SuppressWarnings("deprecation")
    @Test
    public void concurrentTest(){
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");
        long startTime = System.currentTimeMillis();
        for (int i = 0 ;i<num ;i++){
            // 以下代码用于测试报文的  接收
            canCoder.concurrentCanToField(msg1_Id,data8_);
            //System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用了并发流处理报文，运行次数 = " + num + " , 程序运行时间: " + timeCost + " 毫秒");
    }

    /**
     * 采用单线程处理报文,并且不加锁。推荐使用这种方式。
     */
    @Test
    public void singleThreadTest(){
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");
        long startTime = System.currentTimeMillis();
        for (int i = 0 ;i<num ;i++){
            // 以下代码用于测试报文的  接收
            canCoder.deCode_B(msg1_Id,data8_);
            //System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("不采用并发流,采用单线程处理报文，并且不加锁，运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
    }
    /**
     * 采用单线程处理报文，并且加锁运行。相比于完全的单线程，只慢一点点。同样推荐使用这种方式调用
     */
    @Test
    public void syncTest(){
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");
        long startTime = System.currentTimeMillis();
        for (int i = 0 ;i<num ;i++){
            canCoder.syncDeCode_B(msg1_Id,data8_);
            //System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用单线程处理报文，并给每个报文加锁，运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
    }

    /**
     * 采用线程池处理报文，并且采用加锁方案。测试固定数据
     */
    @Test
    public void poolTest(){
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");

        long startTime = System.currentTimeMillis();

        ExecutorService executor = new ThreadPoolExecutor(4,4,3, TimeUnit.SECONDS,new LinkedBlockingDeque<>());
        // canFrameDataList 中有 9 组报文，其中有 3 种报文id ，假如每个执行1秒，则单线程执行9秒。
        // 采用线程池则执行3秒，而不是1秒。因为我给每个ID 都加了锁。
        for (CanFrameData canFrameData : canFrameDataList){
            int canId = canFrameData.getMsgId();
            byte[] data = canFrameData.getBytes8();
            ReentrantLock lock = canCoder.getLock(canId);
            executor.submit(() -> {
                lock.lock();
                try {
                    //System.out.println("Task " + SLCTool.toHexString(canId) + " is running on " + Thread.currentThread().getName());
                    canCoder.deCode_B(canId,data);
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            });
        }
        executor.shutdown();
        // while (!executor.isTerminated()) {}：不断循环检查，直到线程池中的所有任务完成。
        while (!executor.isTerminated()) {}

        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用线程池处理报文，并给每个报文加锁，运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");

    }

    /*
     * 一千 次运行：单线程 20 毫秒，线程池 100 毫秒（因为给每个ID加了锁）
     * 一万 次运行：单线程 50 毫秒，线程池 170毫秒
     * 十万 次：  单线程：300 毫秒，线程池 500 毫秒。
     * 一百万 次 单线程：1700 毫秒，线程池 3500 毫秒。

     总结：在只有3个报文时，单线程优于多线程。
     使用线程池后运行时间变大的原因可能有以下几点：
     1.线程创建和上下文切换开销
     线程池虽然减少了线程创建和销毁的开销，但线程切换仍然存在。任务量小且任务数量多时，频繁的线程切换会导致额外开销，可能超过多线程带来的性能提升。

     2.任务调度开销
     线程池需要管理任务队列和线程调度，任务量小且数量多时，调度开销可能超过任务本身的执行时间。

     3.资源竞争
     多个线程竞争共享资源（如CPU、内存、I/O）时，锁竞争和缓存失效等问题会增加额外开销，尤其在任务量小的情况下，资源竞争的影响更为明显。
     */
    /**
     * 采用线程池优化，采用加锁方案，测试随机数据。
     */
    @Test
    public void poolTest2() {
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");

        long startTime = System.currentTimeMillis();

        ExecutorService executor = new ThreadPoolExecutor(4,8,3, TimeUnit.SECONDS,new LinkedBlockingDeque<>());

        //int num = 1000 ;
        int count1 = 0,count2 = 0,count3 = 0;
        for (int i = 0 ; i < num ;i++){

            // 实际生产中，报文的比拟是随机的，而不是固定比例的，故新增测试用例。
            // 100个数据，3种报文，若比拟固定，则耗时33秒，若比拟随机，则时间不确定。核心线程池数量4，实际上只有3个在运行。
            CanFrameData canFrameData = getRandomData();
            int canId = canFrameData.getMsgId();
            byte[] data = canFrameData.getBytes8();
            switch (canId){
                case msg1_Id:
                    count1++;
                    break;
                case msg2_Id:
                    count2++;
                    break;
                case msg3_Id:
                    count3++;
                    break;
            }

            ReentrantLock lock = canCoder.getLock(canId);
            executor.submit(() -> {
                lock.lock();
                try {
                    //System.out.println("Task " + SLCTool.toHexString(canId) + " is running on " + Thread.currentThread().getName());
                    canCoder.deCode_B(canId,data);
                    //Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            });
        }
        executor.shutdown();
        // while (!executor.isTerminated()) {}：不断循环检查，直到线程池中的所有任务完成。
        while (!executor.isTerminated()) {}

        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用线程池处理报文，并给每个报文加锁，运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
        System.out.println("count1 = "+count1+" , count2 = "+count2+" , count3 = "+count3);
        // 采用线程池处理报文，并给每个报文加锁，运行次数 = 99 ,程序运行时间: 43143 毫秒
        // count1 = 41 , count2 = 29 , count3 = 29 故运行时间按照最大的一个来，而不是平均值33秒。
    }
    /**
     * 采用单线程处理报文,并且不加锁。 <br>
     * 如果不涉及 ViewModel , 推荐使用这种方式进行调用，速度最快。<br>
     */
    @Test
    public void singleThreadTest2(){
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");

        int num = 10;

        long startTime = System.currentTimeMillis();
        for (int i = 0 ; i < num ; i++){
            CanFrameData canFrameData = getRandomData();
            int canId = canFrameData.getMsgId();
            byte[] data = canFrameData.getBytes8();
            canCoder.deCode_B(canId,data);
            System.out.println("model = "+ model.getMsg1Value());
        }
        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("不采用并发流,采用单线程处理报文，并且不加锁，使用随机数据,运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
    }
    public CanFrameData getRandomData(){
        // 在3种报文中，挑一个，生成一个随机数据。
        int randomIndex = new Random().nextInt(canFrameDataList2.size());  //范围在 [0, list.size()) 的随机整数。
        return canFrameDataList2.get(randomIndex);
    }

    /**
     * 测试报文的编码.(反向操作)
     */
    @SuppressWarnings("unused")
    @Test
    public void fieldToCanITest() {
        CanCoder canCoder = canManagerImp.getCanCoder("testDbc");



        // 使用数据，产生一个初始值
        canCoder.deCode_B(msg1_Id,data8_);
        System.out.println("初始数据 model = "+ model.getMsg1Value());
        /* 初始数据：
         model = Msg1 = { <br>
         msg1_sig1 :30,<br>
         msg1_sig2 :29,<br>
         msg1_sig3 = 28,<br>
         msg1_sig4 = 20 ,<br>
         msg1_sig5: 22.200000000000003,<br>
         msg1_sig6 :10.5,<br>
         msg1_sig7 = -80.5,<br>
         msg1_sig8 = 110.0}<br>
         * */

        long startTime = System.currentTimeMillis();
        //num = 1 ;
        for (int i = 0 ; i < num  ; i++) { // num
            // 模拟在子线程中，手动修改了模型的值。
            model.updateValue();

            // 现需要根据新的模型值，产生一个新的报文。
            int[] canFrame = canCoder.enCode_I(msg1_Id);

            // 产生报文 = [11, 12, 13, 14, 217, 121, 194, 110] 正确
            // System.out.println("产生报文 = "+ Arrays.toString(canFrame));
        }

        long endTime = System.currentTimeMillis();
        long timeCost = endTime - startTime;
        System.out.println("采用单线程处理报文，并且不加锁，运行次数 = " + num + " ,程序运行时间: " + timeCost + " 毫秒");
    }

}
