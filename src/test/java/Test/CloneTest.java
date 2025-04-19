package Test;

import Demo.CarDataModel;
import org.junit.Test;

public class CloneTest {
    @SuppressWarnings("unused")
    @Test
    public void test(){
        CarDataModel model = new CarDataModel();
        CarDataModel newModel = model.clone();
    }
}
