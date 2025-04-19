package QuickCanResolver.Core;

public interface CanListenService {
//    default void listened(int canId, byte[] data8){
//
//    }
    /** 监听报文的回调函数 ，监听到报文后，回调下边方法。*/
    default void listened(int canId) {

    }
}
