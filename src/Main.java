
public class Main {
    public static void main(String[] args) throws Exception {


        try {
            if (PolynomialClassCompiler.compile()) {
                if (Compiler.compile()) {
                    System.out.println("Compiled Successfully");
                    Compiler.execute();
                }
            }

        }catch (Exception e){
            if(Compiler.compile()){
                System.err.println("Unknown error");
                System.err.println("***********************************************");
                PolynomialClassCompiler.compile();
            }
        }



    }


}

