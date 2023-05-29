import java.nio.file.Files;
import java.nio.file.Path;

public class PolynomialClassCompiler {
    static Code code;
    static String filePath;
    public static void compile(String path) throws Exception {
        filePath=path;
        String s= Files.readString(Path.of(filePath));
        code=new Code(s);
        code.check();
//        System.out.println(code);
//        System.out.println(Parser.code);
    }
    public static void throwError(String error,int errorLine) {

        System.err.println("Error in line "+errorLine);
        System.err.println(filePath+":"+errorLine);
        System.err.print("->  ");
        for(int i=0;i<Parser.code.length();i++){
            if(Parser.lineNumbers.get(i)==errorLine) System.err.print(Parser.code.charAt(i));
        }
        System.err.println();
        System.err.println("_________________________________________________________________________________________________________________________");
        System.err.println();

        System.err.println(error);

//        System.out.println(code);
        System.exit(0);
    }
}
