import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
public class Main {

    public static void main(String[] args) throws Exception {
        boolean P=true;
        String path="D:\\computer science\\codes\\IdeaProjects\\Bacholer\\program";
        String fileName="Program.java";
        String in=new String(Files.readAllBytes(Paths.get("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\program\\In.txt")));

        compileAndRun(P,path,fileName,in);
    }
    public static boolean compileAndRun(boolean isP,String path,String fileName,String in) throws Exception {
        if(isP) {
            try {
                PolynomialClassCompiler.compile(path+"\\"+fileName);
                if (Compiler.compile(path,fileName)) {
                    System.out.println("Compiled Successfully");
                    Compiler.execute(path,fileName,in);
                    return true;
                }

            } catch (Exception e) {
                if (Compiler.compile(path,fileName)) {
                    System.err.println("Unknown error");
                    System.err.println("_________________________________________________________________________________________________________________________");
                    PolynomialClassCompiler.compile(path+"\\"+fileName);
                }
            }

        }else{

            try {
                NonDeterministicallyPolynomialClassCompiler.compile(path+"\\"+fileName);
                if (Compiler.compile(path,fileName)) {
                    System.out.println("Compiled Successfully");
                    Compiler.execute(path,fileName,in);
                    return true;
                }

            } catch (Exception e) {
                if (Compiler.compile(path,fileName)) {
                    System.err.println("Unknown error");
                    System.err.println("_________________________________________________________________________________________________________________________");
                    NonDeterministicallyPolynomialClassCompiler.compile(path+"\\"+fileName);
                }

            }

        }

        return false;
    }

}

