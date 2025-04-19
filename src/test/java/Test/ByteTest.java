package Test;

import QuickCanResolver.CanTool.MyByte;
import org.junit.Test;

import java.util.Arrays;

public class ByteTest {
    @Test
    public void byte1(){
        byte[] bytes = MyByte.intToBits(1, MyByte.DataType.Intel,5); // 2 长度5 ，输出  [0, 1, 0, 0, 0];
        System.out.println("bytes = "+ Arrays.toString(bytes));
        //byte[] bytes2 = MyByte.intToBits(12, MyByte.DataType.Intel);
        //System.out.println("bytes = "+ Arrays.toString(bytes2));
    }
}
