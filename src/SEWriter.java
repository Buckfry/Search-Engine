import java.io.FileWriter;
import java.io.IOException;

public class SEWriter {
	private static FileWriter fw;
	public static void intialize(String path) throws IOException{
		fw = new FileWriter(path);
	}
	
	public static void write(String s) throws IOException{
		fw.write(s);
	}
	
	public static void close() throws IOException{
		fw.close();
	}
	
}
