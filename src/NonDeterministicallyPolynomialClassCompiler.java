import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
public class NonDeterministicallyPolynomialClassCompiler {
    static Code code;
    public static boolean compile() throws Exception {
        String s= Files.readString(Path.of("D:\\computer science\\codes\\IdeaProjects\\Bacholer\\Program\\Program.java"));
        code=new Code(s);
        Code.Method main= code.methods.get(0);
        Code.ForLoop initialLoops=null;
        ArrayList<Code.ForLoop>loops=new ArrayList<>();
        if((main.instructions.size()==2)&&(main.instructions.get(1).getClass().equals(Code.ForLoop.class))){
            initialLoops=(Code.ForLoop)main.instructions.get(1);
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
        return true;
    }
}
