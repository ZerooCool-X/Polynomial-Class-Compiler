import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

public class Compiler {
    public static boolean compile() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("javac", "D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program\\Program.java");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.err.println(line);
        }

        int exitCode = process.waitFor();

        Path directory = Paths.get("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program");
        Files.walk(directory)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        if (exitCode == 0) {
            return true;
        } else {
            return false;
        }
    }




    public static void execute() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", "Program.java");
        pb.directory(new File("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program"));
        Process p = pb.start();

        //input, not interactive
        InputStream fileInput = new FileInputStream("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program\\In.txt");
        OutputStream input = p.getOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fileInput.read(buffer)) != -1) {
            input.write(buffer, 0, bytesRead);
        }
        fileInput.close();
        input.close();
        p.waitFor();

        if(p.exitValue()==0){
            //output
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            System.out.println(output.toString());

        }else{
            //error
            InputStream is = p.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
            System.err.println(error.toString());
        }

    }
    static Scanner sc=new Scanner(System.in);
}