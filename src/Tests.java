import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Tests {
    static String path="D:\\computer science\\codes\\IdeaProjects\\Bacholer\\tests";
    @Test
    public void test1() throws Exception {
        boolean isP=true;
        String in="";
        String fileName="test1.java";
        Assert.assertEquals(true,Main.compileAndRun(isP,path,fileName,in));
    }

    @Test
    public void test2() throws Exception {
        boolean isP=true;
        String in="";
        String fileName="test2.java";
        Assert.assertEquals(true,Main.compileAndRun(isP,path,fileName,in));
    }

    @Test
    public void test3() throws Exception {
        boolean isP=false;
        String in="";
        String fileName="test3.java";
        Assert.assertEquals(true,Main.compileAndRun(isP,path,fileName,in));
    }

    @Test
    public void test4() throws Exception {
        boolean isP=true;
        String in="5 6 " +
                "1 0 0 1 0 " +
                "1 0 1 " +
                "2 2 " +
                "0 3 4 " +
                "0 5 6 " +
                "1 7 8 " +
                "2 9 ";
        String fileName="test4.java";
        Assert.assertEquals(true,Main.compileAndRun(isP,path,fileName,in));
    }

    @Test
    public void test5() throws Exception {
        boolean isP=true;
        String in="";
        String fileName="test5.java";
        Assert.assertEquals(true,Main.compileAndRun(isP,path,fileName,in));
    }
}
