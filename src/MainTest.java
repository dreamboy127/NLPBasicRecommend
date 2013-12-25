import com.jerrychou.nlp.*;
public class MainTest {
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		LSIModel model=new LSIModel();
		//model.Initial("D:\\Data1");
		//model.train("D:\\Data1Save");
		model.load("D:\\Data1Save");
		model.QueryRun("D:\\QueryPreProcess");
	}

}
