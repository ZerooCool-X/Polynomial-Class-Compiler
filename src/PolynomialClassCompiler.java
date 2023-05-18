import java.nio.file.Files;
import java.nio.file.Path;

public class PolynomialClassCompiler {
    static Code code;
    public static boolean compile() throws Exception {
        String s= Files.readString(Path.of("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program\\Program.java"));
        code=new Code(s);
        code.check();
//        System.out.println(code);
//        System.out.println(Parser.code);

        return true;
    }
    public static void throwError(String error,int errorLine) {

        System.err.println("Error in line "+errorLine);
        System.err.println("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program\\Program.java:"+errorLine);
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
