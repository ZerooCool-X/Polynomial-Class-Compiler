import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Scanner;

public class Compiler {
    public static boolean compile(String path,String filename) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("javac", path+"\\"+filename);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.err.println(line);
        }

        int exitCode = process.waitFor();

        Path directory = Paths.get(path);
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




    public static void execute(String path,String fileName,String in) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", fileName);
        pb.directory(new File(path));
        Process p = pb.start();

        //input, not interactive
        InputStream fileInput = new ByteArrayInputStream(in.getBytes());
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