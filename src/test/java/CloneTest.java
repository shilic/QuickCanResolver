import Demo.CarDataModel;
import org.junit.Test;

public class CloneTest {
    @Test
    public void test(){
        CarDataModel model = new CarDataModel();
        CarDataModel newModel = model.clone();
    }
}
