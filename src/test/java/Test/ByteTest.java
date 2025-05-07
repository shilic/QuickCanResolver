package Test;

import quickCanResolver.tool.SLCTool;
import org.junit.Test;

import java.util.Arrays;

public class ByteTest {
    @Test
    public void byte1(){
        byte[] bytes = SLCTool.intToBits(1, SLCTool.DataType.Intel,5); // 2 长度5 ，输出  [0, 1, 0, 0, 0];
        System.out.println("bytes = "+ Arrays.toString(bytes));
        //byte[] bytes2 = SLCTool.intToBits(12, SLCTool.DataType.Intel);
        //System.out.println("bytes = "+ Arrays.toString(bytes2));
    }
}
