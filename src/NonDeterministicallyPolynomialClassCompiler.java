import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
public class NonDeterministicallyPolynomialClassCompiler {
    static Code code;
    static String filePath;
    public static void compile(String path) throws Exception {
        filePath=path;
        PolynomialClassCompiler.filePath=path;
        String s= Files.readString(Path.of(filePath));
        code=new Code(s);
        Code.Method main= code.methods.get(0);
        Code.ForLoop initialLoops=null;
        ArrayList<Code.ForLoop>loops=new ArrayList<>();
        for(int i=0;i<main.instructions.size()-1;i++){
            if(main.instructions.get(i).getClass().equals(Code.Statement.class)){
                if(Parser.startsWithDataType(((Code.Statement)main.instructions.get(i)).leftHandSide,0)){
                    PolynomialClassCompiler.throwError("All initialization statements before the exponential loops must be \"final\"", main.instructions.get(i).line);
                }
            }else{
                PolynomialClassCompiler.throwError(" Only the initialization of variables declared as \"final\" can be used before the exponential loops", main.instructions.get(i).line);
            }
        }
        if((main.instructions.size()>=2)&&(main.instructions.get(main.instructions.size()-1).getClass().equals(Code.ForLoop.class))){
            initialLoops=(Code.ForLoop)main.instructions.get(main.instructions.size()-1);
            Code.ForLoop currentLoop=initialLoops;
            loops.add(currentLoop);
            while((currentLoop.inside.size()==1)&&(currentLoop.inside.get(0).getClass().equals(Code.ForLoop.class))){
                currentLoop=(Code.ForLoop)currentLoop.inside.get(0);
                loops.add(currentLoop);
            }
            main.instructions.remove(1);
            for(Code.Instruction i:currentLoop.inside)main.instructions.add(i);
            currentLoop.inside=new ArrayList<>();
        }
        HashMap<String,Integer>magnitudes=new HashMap<>();
        magnitudes.put("N",1);

        for(int i=0;i<loops.size();i++){
            String[] bound = loops.get(i).condition.checkBooleanExpression();
            if (bound == null || !loops.get(i).counterName.equals(bound[0])) {
                PolynomialClassCompiler.throwError("The condition of the for loop must be of the form \"(counter<polynomialInN)&&(otherBooleanStatement)&&(otherBooleanStatement)&&..\"", loops.get(i).line);
            }
            magnitudes.put(loops.get(i).counterName,Code.Expression.getVariableMagnitude(bound[1],magnitudes));
            if(magnitudes.get(loops.get(i).counterName)==3){
                PolynomialClassCompiler.throwError("The counter "+loops.get(i).counterName+" is not bounded by an exponential in N", loops.get(i).line);
            }
        }
        code.check();

//        System.out.println(code);
//        System.out.println("*******************************************");
//        if(initialLoops!=null)
//            System.out.println(initialLoops.toString(0));

//        System.err.println(magnitudes);
    }
}
