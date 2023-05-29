import java.util.*;

public class Code {
    int line;
    ArrayList<Method> methods;
    ArrayList<Statement> imports;
    ArrayList<Statement> staticVariables;

    public Code(String s) {
        methods = new ArrayList<>();
        imports = new ArrayList<>();
        staticVariables = new ArrayList<>();
        Parser.parseCode(this, s);
        boolean found=false;
        for (int i = 0; i < methods.size(); i++) {
            Method current = methods.get(i);
            if (current.name.equals("main") && current.returnType.equals("void") && current.paramTypes.size() == 1 && current.paramTypes.get(0).equals("String[]")) {
                methods.set(i, methods.get(0));
                methods.set(0, current);
                found=true;
                break;
            }
        }
        if(!found)PolynomialClassCompiler.throwError("Code must have a main method", line);
    }

    public void check() {
        //imports
        HashSet<String> allowedImports = new HashSet<>();
        allowedImports.add("java.util.Scanner");
        for (Statement i : imports) {
            if (!allowedImports.contains(i.leftHandSide)) {
                PolynomialClassCompiler.throwError("The only allowed imports are " + allowedImports, i.line);
            }
        }

        //static variables;
        HashSet<String> allowedStaticVariables = new HashSet<>();
        allowedStaticVariables.add("Scanner sc");
        allowedStaticVariables.add("int N");


        boolean foundLim = false;
        for (Statement i : staticVariables) {
            if (!allowedStaticVariables.contains(i.leftHandSide))
                PolynomialClassCompiler.throwError("The only allowed static variables are " + allowedStaticVariables, i.line);

            if (i.leftHandSide.startsWith("Scanner ") && !i.leftHandSide.equals("Scanner sc")) {
                PolynomialClassCompiler.throwError("The Scanner must be called sc", i.line);
            } else if (i.leftHandSide.equals("int N")) {
                foundLim = true;
                if (methods.get(0).instructions.isEmpty() || !methods.get(0).instructions.get(0).getClass().equals(Code.Statement.class)
                        || !((Statement) methods.get(0).instructions.get(0)).leftHandSide.equals("N") ||
                        !((Statement) methods.get(0).instructions.get(0)).rightHandSide.equals("System.in.available()"))
                    PolynomialClassCompiler.throwError("The first instruction in the main method must be N=System.in.available();\n" +
                            "which is an initialization for the static variable N by the number of characters in the input", methods.get(0).line);

            }
        }
        if (!foundLim) {
            PolynomialClassCompiler.throwError("There must be a static int variable called N that limits the complexity of the code", line);
        }
        //Methods
        for (Method i : methods) i.check();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();

        for (Statement i : imports) ret.append(i.toString(0));
        ret.append("\n");
        for (Statement i : staticVariables) ret.append(i.toString(0));
        ret.append("\n");
        for (Method i : methods) {
            ret.append(i);
            ret.append("\n");
        }


        return ret.toString();
    }


    static class Method {
        int line;
        String name;
        String returnType;
        ArrayList<String> paramNames = new ArrayList<>();
        ArrayList<String> paramTypes = new ArrayList<>();
        ArrayList<Instruction> instructions;

        public String toString() {
            StringBuilder ret = new StringBuilder();
            ret.append(line);
            while (ret.length() != 5) ret.append(' ');


            ret.append(returnType).append(" ").append(name).append(" (");
            for (int i = 0; i < paramNames.size(); i++) {
                ret.append(paramTypes.get(i)).append(" ").append(paramNames.get(i)).append(i != paramNames.size() - 1 ? " , " : "");
            }
            ret.append("){\n");

            for (Instruction i : instructions) ret.append(i.toString(1));

            ret.append("     }\n");

            return ret.toString();
        }

        public void check() {
            int cnt = countMethodCalls();


            if (name.equals("main")) {
                for(int i=1;i<instructions.size();i++){
                    if(instructions.get(i).getClass().equals(Statement.class)){
                        Statement initial=(Statement)instructions.get(i);
                        if(initial.rightHandSide!=null&&!Parser.isNumber(initial.rightHandSide.toString())){
                            String[] left=initial.leftHandSide.toString().split(" ");
                            String name=left[left.length-1];
                            for(int j=i+1;j<instructions.size();j++) {
                                if(instructions.get(j).getClass().equals(WhileLoop.class)&&((WhileLoop)instructions.get(j)).counterName.equals(name))
                                    PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", initial.line);
                            }
                        }
                    }
                }
                for (int i = 1; i < instructions.size(); i++) instructions.get(i).check(true, true);
            } else {
                if (paramTypes.isEmpty() || !paramTypes.get(0).equals("int") || !paramNames.get(0).equals("Lim")) {
                    PolynomialClassCompiler.throwError("The fist parameter of all recursive methods except the main method must be an\nint variable called Lim that limits the depth of the recursive calls", line);
                }
                if (instructions.isEmpty() || !instructions.get(0).getClass().equals(IfCondition.class) ||
                        !((IfCondition) instructions.get(0)).condition.equals("Lim<=0")) {
                    if(cnt!=0)
                        PolynomialClassCompiler.throwError("The fist line of all methods except main must be a base case when Lim<=0\nexample: if(Lim<=0)return 0;", line);
                } else {
                    boolean found = false;
                    for (Instruction i : ((IfCondition) instructions.get(0)).thenPart) {
                        if (i.getClass().equals(Statement.class) && ((Statement) i).leftHandSide.equals("return")) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        PolynomialClassCompiler.throwError("There must be a return statement inside the base case Lim<=0", line);

                    if (instructions.get(0).countMethodCalls() != 0)
                        PolynomialClassCompiler.throwError("Can't have method calls inside the base case Lim<=0", line);


                }


                for(int i=0;i<instructions.size();i++){
                    if(instructions.get(i).getClass().equals(Statement.class)){
                        Statement initial=(Statement)instructions.get(i);
                        if(initial.rightHandSide!=null&&!Parser.isNumber(initial.rightHandSide.toString())){
                            String[] left=initial.leftHandSide.toString().split(" ");
                            String name=left[left.length-1];
                            for(int j=i+1;j<instructions.size();j++) {
                                if(instructions.get(j).getClass().equals(WhileLoop.class)&&((WhileLoop)instructions.get(j)).counterName.equals(name))
                                    PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", initial.line);
                            }
                        }
                    }
                }
                for (Instruction i : instructions) i.check(false, cnt < 2);
            }

        }

        public int countMethodCalls() {
            int ret = 0;
            for (Instruction i : instructions) {
                if (i.getClass().equals(Statement.class))
                    ret += i.countMethodCalls();
            }
            return ret;
        }
    }

    static abstract class Instruction {
        int line;

        abstract public String toString(int depth);

        abstract public void check(boolean isMainMethod, boolean hasSingleMethodCall);

        abstract public void variableChanged(String variableName, boolean increase);

        abstract public int countMethodCalls();

        abstract public int findContinue();
    }

    static class ForLoop extends Instruction {
        ArrayList<Instruction> inside;
        String counterName;
        int counterValue;
        Expression condition;
        long inc;

        public String toString(int depth) {
            StringBuilder ret = new StringBuilder();

            ret.append(line);
            while (ret.length() != 5) ret.append(' ');

            ret.append("    ".repeat(depth));
            ret.append("for(").append(counterName).append("=").append(counterValue).append(";").append(condition).append(";").append(inc).append("){\n");

            for (Instruction i : inside) ret.append(i.toString(depth + 1));

            ret.append("    ".repeat(depth));

            ret.append("     }\n");

            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            String[] bound = condition.checkBooleanExpression();
            if (bound == null || !counterName.equals(bound[0])) {
                PolynomialClassCompiler.throwError("The condition of the for loop must be of the form \"(counter<polynomialInN)&&(otherBooleanStatement)&&(otherBooleanStatement)&&..\"", line);
            }
            boolean isLessThan = condition.isLessThan();

            int order = Expression.getExpressionMagnitude(bound[1], "N", isLessThan);
            if (order == 2) {
                if (isMainMethod || Expression.getExpressionMagnitude(bound[1], "Lim", isLessThan) == 2) {
                    PolynomialClassCompiler.throwError("The bound of the for loop must fall under on of the following cases:\n" +
                            "1. A polynomial in N \n" +
                            "2. A polynomial in Lim (If it wasn't the main method) \n" +
                            "3. Math.min() between one of the first two cases and anything (If it was an upper bound)\n" +
                            "4. Math.max() between one of the first two cases and anything (If it was a lower bound)", line);
                } else {
                    order = Expression.getExpressionMagnitude(bound[1], "Lim", isLessThan);
                }
            }
            if (countMethodCalls() > 0 && order > 0)
                PolynomialClassCompiler.throwError("Can't have method calls inside non constant for loops (constant loops are loops that are bounded by constant value\ne.g. for(int i=0;i<5;i++)", line);

            condition.checkVariableChange(line);
            condition.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);

            if (isLessThan) {
                if (inc <= 0)
                    PolynomialClassCompiler.throwError("The loop counter can only be incremented to grantee termination", line);
                variableChanged(counterName, true);
            } else {
                if (inc >= 0)
                    PolynomialClassCompiler.throwError("The loop counter can only be decremented to grantee termination", line);

                variableChanged(counterName, false);
            }


            for(int i=0;i<inside.size();i++){
                if(inside.get(i).getClass().equals(Statement.class)){
                    Statement initial=(Statement)inside.get(i);
                    if(initial.rightHandSide!=null&&!Parser.isNumber(initial.rightHandSide.toString())){
                        String[] left=initial.leftHandSide.toString().split(" ");
                        String name=left[left.length-1];
                        for(int j=i+1;j<inside.size();j++) {
                            if(inside.get(j).getClass().equals(WhileLoop.class)&&((WhileLoop)inside.get(j)).counterName.equals(name))
                                PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", initial.line);
                        }
                    }
                }
            }
            for (Instruction i : inside) i.check(isMainMethod, hasSingleMethodCall);


        }

        public void variableChanged(String variableName, boolean increase) {
            for (Instruction i : inside) i.variableChanged(variableName, increase);
        }

        public int countMethodCalls() {
            int ret = condition.countMethodCalls();
            for (Instruction i : inside) ret += i.countMethodCalls();
            return ret;
        }

        public int findContinue() {
            int ret = -1;
            for (Instruction i : inside) ret = Math.max(i.findContinue(), ret);
            return ret;
        }
    }

    static class WhileLoop extends Instruction {
        ArrayList<Instruction> inside;
        Expression condition;
        String counterName;

        public String toString(int depth) {
            StringBuilder ret = new StringBuilder();

            ret.append(line);
            while (ret.length() != 5) ret.append(' ');

            ret.append("    ".repeat(depth));
            ret.append("while(").append(condition).append("){\n");

            for (Instruction i : inside) ret.append(i.toString(depth + 1));

            ret.append("    ".repeat(depth));

            ret.append("     }\n");

            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            String[] bound = condition.checkBooleanExpression();
            if (bound == null) {
                PolynomialClassCompiler.throwError("The condition of the while loop must be of the form \"(counter<polynomialInN)&&(otherBooleanStatement)&&(otherBooleanStatement)&&..\"", line);
            }
            boolean isLessThan = condition.isLessThan();
            counterName = bound[0];
            int order = Expression.getExpressionMagnitude(bound[1], "N", isLessThan);
            if (order == 2) {
                if (isMainMethod || Expression.getExpressionMagnitude(bound[1], "Lim", isLessThan) == 2) {
                    PolynomialClassCompiler.throwError("The bound of the while loop must fall under on of the following cases:\n" +
                            "1. A polynomial in N \n" +
                            "2. A polynomial in Lim (If it wasn't the main method) \n" +
                            "3. Math.min() between one of the first two cases and anything (If it was an upper bound)\n" +
                            "4. Math.max() between one of the first two cases and anything (If it was a lower bound)", line);
                } else {
                    order = Expression.getExpressionMagnitude(bound[1], "Lim", isLessThan);
                }
            }
            if (countMethodCalls() > 0 && order > 0)
                PolynomialClassCompiler.throwError("Can't have method calls inside non constant while loops (constant loops are loops that are bounded by constant value\ne.g. while(i<5)", line);

            condition.checkVariableChange(line);
            condition.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);

            if (isLessThan) {
                boolean found = false;
                int foundContinue = -1;
                for (Instruction i : inside) {
                    foundContinue = Math.max(i.findContinue(), foundContinue);
                    if (i.getClass().equals(Code.Statement.class)) {
                        if (((Statement) i).leftHandSide.equals(counterName + "+")) found = true;
                        if (((Statement) i).leftHandSide.equals(counterName + "++")) found = true;
                        if (found == true) {
                            if (foundContinue != -1) {
                                PolynomialClassCompiler.throwError("Can't use Continue before the first increment/decrement of the loop counter to grantee termination", foundContinue);
                            }
                            break;
                        }
                    }
                }
                if (!found) {
                    PolynomialClassCompiler.throwError("The counter of the while loop must be incremented in its body to grantee termination", line);
                }
                variableChanged(counterName, true);
            } else {
                boolean found = false;
                int foundContinue = -1;

                for (Instruction i : inside) {
                    foundContinue = Math.max(i.findContinue(), foundContinue);

                    if (i.getClass().equals(Code.Statement.class)) {
                        if (((Statement) i).leftHandSide.equals(counterName + "-")) found = true;
                        if (((Statement) i).leftHandSide.equals(counterName + "--")) found = true;
                        if (found == true) {
                            if (foundContinue != -1) {
                                PolynomialClassCompiler.throwError("Can't use Continue before the first increment/decrement of the loop counter to grantee termination", foundContinue);
                            }
                            break;
                        }
                    }
                }
                if (!found) {
                    PolynomialClassCompiler.throwError("The counter of the while loop must be decremented in its body to grantee termination", line);
                }
                variableChanged(counterName, false);
            }



            for(int i=0;i<inside.size();i++){
                if(inside.get(i).getClass().equals(Statement.class)){
                    Statement initial=(Statement)inside.get(i);
                    if(initial.rightHandSide!=null&&!Parser.isNumber(initial.rightHandSide.toString())){
                        String[] left=initial.leftHandSide.toString().split(" ");
                        String name=left[left.length-1];
                        for(int j=i+1;j<inside.size();j++) {
                            if(inside.get(j).getClass().equals(WhileLoop.class)&&((WhileLoop)inside.get(j)).counterName.equals(name))
                                PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", initial.line);
                        }
                    }
                }
            }

            for (Instruction i : inside) i.check(isMainMethod, hasSingleMethodCall);

        }

        public void variableChanged(String variableName, boolean increase) {
            for (Instruction i : inside) i.variableChanged(variableName, increase);
        }

        public int countMethodCalls() {
            int ret = condition.countMethodCalls();
            for (Instruction i : inside) ret += i.countMethodCalls();
            return ret;
        }

        public int findContinue() {
            int ret = -1;
            for (Instruction i : inside) ret = Math.max(i.findContinue(), ret);
            return ret;
        }
    }

    static class IfCondition extends Instruction {
        ArrayList<Instruction> thenPart;
        ArrayList<Instruction> elsePart;
        Expression condition;

        public String toString(int depth) {
            StringBuilder ret = new StringBuilder();

            ret.append(line);
            while (ret.length() != 5) ret.append(' ');

            ret.append("    ".repeat(depth));
            ret.append("if(").append(condition).append("){\n");

            for (Instruction i : thenPart) ret.append(i.toString(depth + 1));

            ret.append("    ".repeat(depth));
            ret.append("     }\n");
            if (elsePart != null) {
                ret.append("    ".repeat(depth));
                ret.append("     else{\n");

                for (Instruction i : elsePart) ret.append(i.toString(depth + 1));

                ret.append("    ".repeat(depth));

                ret.append("     }\n");
            }
            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            condition.checkVariableChange(line);
            condition.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);


            for(int i=0;i<thenPart.size();i++){
                if(thenPart.get(i).getClass().equals(Statement.class)){
                    Statement initial=(Statement)thenPart.get(i);
                    if(initial.rightHandSide!=null&&!Parser.isNumber(initial.rightHandSide.toString())){
                        String[] left=initial.leftHandSide.toString().split(" ");
                        String name=left[left.length-1];
                        for(int j=i+1;j<thenPart.size();j++) {
                            if(thenPart.get(j).getClass().equals(WhileLoop.class)&&((WhileLoop)thenPart.get(j)).counterName.equals(name))
                                PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", initial.line);
                        }
                    }
                }
            }

            for (Instruction i : thenPart) i.check(isMainMethod, hasSingleMethodCall);
            if (elsePart != null) {
                for(int i=0;i<elsePart.size();i++){
                    if(elsePart.get(i).getClass().equals(Statement.class)){
                        Statement initial=(Statement)elsePart.get(i);
                        if(initial.rightHandSide!=null&&!Parser.isNumber(initial.rightHandSide.toString())){
                            String[] left=initial.leftHandSide.toString().split(" ");
                            String name=left[left.length-1];
                            for(int j=i+1;j<elsePart.size();j++) {
                                if(elsePart.get(j).getClass().equals(WhileLoop.class)&&((WhileLoop)elsePart.get(j)).counterName.equals(name))
                                    PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", initial.line);
                            }
                        }
                    }
                }
                for (Instruction i : elsePart) i.check(isMainMethod, hasSingleMethodCall);
            }
        }

        public void variableChanged(String variableName, boolean increase) {
            for (Instruction i : thenPart) i.variableChanged(variableName, increase);
            if (elsePart != null)
                for (Instruction i : elsePart) i.variableChanged(variableName, increase);
        }

        public int countMethodCalls() {
            int ret = condition.countMethodCalls();
            for (Instruction i : thenPart) ret += i.countMethodCalls();
            if (elsePart != null)
                for (Instruction i : elsePart) ret += i.countMethodCalls();
            return ret;
        }

        public int findContinue() {
            int ret = -1;
            for (Instruction i : thenPart) ret = Math.max(i.findContinue(), ret);
            if (elsePart != null)
                for (Instruction i : elsePart) ret = Math.max(i.findContinue(), ret);
            return ret;
        }
    }

    static class Statement extends Instruction {
        String leftHandSide;
        Expression rightHandSide;

        public String toString(int depth) {
            StringBuilder ret = new StringBuilder();

            ret.append(line);
            while (ret.length() != 5) ret.append(' ');
            ret.append("    ".repeat(depth));
            ret.append(leftHandSide).append(" ").append(rightHandSide);
            ret.append("\n");
            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            if (leftHandSide.split(" ").length >= 2 && leftHandSide.split(" ")[leftHandSide.split(" ").length-1].equals("N")) {
                PolynomialClassCompiler.throwError("The variable name N is reserved for the complexity limiter", line);
            }
            if (leftHandSide.split(" ").length == 2 && leftHandSide.split(" ")[0].equals("Scanner") && !leftHandSide.split(" ")[1].equals("sc")) {
                PolynomialClassCompiler.throwError("Scanner objects must be called sc", line);
            }
            if (leftHandSide.split(" ").length == 2 && leftHandSide.split(" ")[1].equals("Lim")) {
                PolynomialClassCompiler.throwError("The variable name Lim is reserved for the complexity limiter", line);
            }

            if (leftHandSide.equals("N")  || leftHandSide.equals("N++")||leftHandSide.equals("N--")||
                    (leftHandSide.length()==2&&leftHandSide.startsWith("N")&&Parser.isSymbol(leftHandSide.charAt(leftHandSide.length()-1)))) {
                PolynomialClassCompiler.throwError("Changing the value of variable N is prohibited", line);
            }
            if ((leftHandSide.equals("Lim") || leftHandSide.equals("Lim+") || leftHandSide.equals("Lim++")|| leftHandSide.equals("Lim--")||
                    (leftHandSide.length()==4&&leftHandSide.startsWith("Lim")&&Parser.isSymbol(leftHandSide.charAt(leftHandSide.length()-1)))) && !isMainMethod) {
                PolynomialClassCompiler.throwError("Changing the value of variable Lim is prohibited", line);
            }

            if (rightHandSide != null) {
                rightHandSide.checkVariableChange(line);
                rightHandSide.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);
            } else if (leftHandSide.contains("(")) {
                new Expression(leftHandSide).checkMethodCalls(isMainMethod, hasSingleMethodCall, line);
            }
        }

        public void variableChanged(String variableName, boolean increase) {
            if (leftHandSide.equals(variableName) && rightHandSide != null) {
                PolynomialClassCompiler.throwError("Can't change the value of loop counter inside the loop ", line);
            }
            long change = 0;
            if (leftHandSide.equals(variableName + "++")) {
                change = 1;
            } else if (leftHandSide.equals(variableName + "+")) {
                if (rightHandSide.isNumber()) change = rightHandSide.getValue();
                else
                    PolynomialClassCompiler.throwError("The value of the loop counter can only be either incremented or decremented by a constant to grantee termination", line);
            } else if (leftHandSide.equals(variableName + "--")) {
                change = -1;
            } else if (leftHandSide.equals(variableName + "-")) {
                if (rightHandSide.isNumber()) change = -rightHandSide.getValue();
                else
                    PolynomialClassCompiler.throwError("The value of the loop counter can only be either incremented or decremented by a constant to grantee termination", line);
            } else if (leftHandSide.startsWith(variableName) && Parser.isSymbol(leftHandSide.charAt(variableName.length())) && rightHandSide != null) {
                PolynomialClassCompiler.throwError("The value of the loop counter can only be either incremented or decremented by a constant to grantee termination", line);
            } else {
                return;
            }
            if (increase) {
                if (change <= 0) {
                    PolynomialClassCompiler.throwError("The loop counter can only be incremented to grantee termination", line);
                }
            } else {
                if (change >= 0) {
                    PolynomialClassCompiler.throwError("The loop counter can only be decremented to grantee termination", line);
                }
            }
        }

        public int countMethodCalls() {
            int ret = 0;
            if (rightHandSide != null) ret += rightHandSide.countMethodCalls();
            else if (leftHandSide.contains("(")) {
                ret += new Expression(leftHandSide).countMethodCalls();
            }
            return ret;
        }

        public int findContinue() {
            return leftHandSide.equals("continue") ? line : -1;
        }

    }

    static class Expression {
        String expression;

        public Expression(String expression) {
            this.expression = expression;
        }

        public void checkVariableChange(int line) {
            if (expression.contains("++")) {
                PolynomialClassCompiler.throwError("Can't change variables value inside the expressions", line);
                return;
            }
            if (expression.contains("--")) {
                PolynomialClassCompiler.throwError("Can't change variables value inside the expressions", line);
                return;
            }
            for (int i = 1; i < expression.length() - 1; i++) {
                if (expression.charAt(i) == '=' && expression.charAt(i + 1) != '=' && expression.charAt(i - 1) != '!' && expression.charAt(i - 1) != '<' && expression.charAt(i - 1) != '>' && expression.charAt(i - 1) != '=') {
                    PolynomialClassCompiler.throwError("Can't change variables value inside the expressions", line);
                    return;
                }
            }
        }

        public boolean isLessThan() {
            return expression.indexOf('<') != -1 && (expression.indexOf('>') == -1 || (expression.indexOf('<') < expression.indexOf('>')));
        }

        public String[] checkBooleanExpression() {
            int ind = 0;
            while (ind < expression.length() && expression.charAt(ind) == '(') ind++;
            int jnd = ind;
            if (Parser.isNumber(expression.charAt(ind) + "")) {
                return null;
            }
            while (jnd < expression.length() && !Parser.isSymbol(expression.charAt(jnd))) jnd++;
            String[] ret = new String[2];
            ret[0] = expression.substring(ind, jnd);

            if (expression.startsWith(ret[0] + "<") || expression.startsWith(ret[0] + ">")) {
                if (expression.contains("&") || expression.contains("|")) {
                    return null;
                }

                ret[1] = expression.substring(jnd + (expression.charAt(jnd + 1) == '=' ? 2 : 1));
            } else if (!expression.startsWith("(" + ret[0] + "<") && !expression.startsWith("(" + ret[0] + ">")) {
                return null;
            } else {
                int cnt = 1;

                for (int i = 1; i < expression.length(); i++) {
                    if (expression.charAt(i) == '(') cnt++;
                    else if (expression.charAt(i) == ')') {
                        cnt--;
                        if (cnt == 0 && ret[1] == null)
                            ret[1] = expression.substring(jnd + (expression.charAt(jnd + 1) == '=' ? 2 : 1), i);
                    } else if (cnt == 0) {
                        if (expression.charAt(i) != '&')
                            return null;
                    }
                }
            }
            return ret;
        }

        public boolean isNumber() {
            return Parser.isNumber(expression);
        }

        public long getValue() {
            return Integer.parseInt(expression);
        }

        public int countMethodCalls() {
            int cnt = 0;
            for (int i = 1; i < expression.length(); i++) {
                if (expression.charAt(i) == '(' && !Parser.isSymbol(expression.charAt(i - 1))) {
                    int jnd = i - 1;
                    while (jnd != 0 && (!Parser.isSymbol(expression.charAt(jnd))))
                    {
                        jnd--;
                    }
                    if (expression.charAt(jnd)!='.') {
                        cnt++;
                    }
                }
            }
            return cnt;
        }

        public void checkMethodCalls(boolean isMainMethod, boolean hasSingleMethodCall, int line) {
            for (int i = 1; i < expression.length(); i++) {
                if (expression.charAt(i) == '(' && !Parser.isSymbol(expression.charAt(i - 1))) {
                    int jnd = i - 1;
                    while (jnd != 0 && (!Parser.isSymbol(expression.charAt(jnd))))
                    {
                        jnd--;
                    }
                    if (expression.charAt(jnd)=='.') {
                        continue;
                    }

                    if (isMainMethod) {
                        int ind = i;
                        int sum = 1;
                        while (sum != 0 && expression.charAt(ind) != ',') {
                            ind++;
                            if (expression.charAt(ind) == '(') sum++;
                            else if (expression.charAt(ind) == ')') sum--;
                        }
                        if (getExpressionMagnitude(expression.substring(i + 1, ind), "N", true) == 2) {
                            PolynomialClassCompiler.throwError("The first variable in the method call must fall under one of the following:\n\n" +
                                    "In the main method:\n" +
                                    "   1. A polynomial in N\n" +
                                    "   2. Math.min() between a polynomial in N and anything\n\n" +
                                    "In non main methods:\n" +
                                    "   1. Lim divided by an integer greater than 1\n" +
                                    "   2. Lim subtracted by an integer greater than 0 (If there is only one method call inside the method)", line);
                        }
                    } else {
                        int j = i - 1;
                        while (j > 0 && !Parser.isSymbol(expression.charAt(j))) j--;
                        if (expression.substring(j, i).equals("main"))
                            PolynomialClassCompiler.throwError("Can't call main method", line);
                        int ind = expression.indexOf('/', i) == -1 ? expression.indexOf('-', i) :
                                expression.indexOf('-', i) == -1 ? expression.indexOf('/', i) :
                                        Math.min(expression.indexOf('/', i), expression.indexOf('-', i));
                        if (ind == -1 || !expression.substring(i + 1, ind).equals("Lim")) {
                            PolynomialClassCompiler.throwError("The first variable in the method call must fall under one of the following:\n\n" +
                                    "In the main method:\n" +
                                    "   1. A polynomial in N\n" +
                                    "   2. Math.min() between a polynomial in N and anything\n\n" +
                                    "In non main methods:\n" +
                                    "   1. Lim divided by an integer greater than 1\n" +
                                    "   2. Lim subtracted by an integer greater than 0 (If there is only one method call inside the method)", line);
                            return;
                        }
                        i = ind;
                        ind++;

                        while (!Parser.isSymbol(expression.charAt(ind))) ind++;
                        if ((expression.charAt(ind) != ',' && expression.charAt(ind) != ')') || !Parser.isNumber(expression.substring(i + 1, ind)) || Integer.parseInt(expression.substring(i + 1, ind)) < (expression.charAt(i) == '-' ? 1 : 2)) {

                            PolynomialClassCompiler.throwError("The first variable in the method call must be Lim divided by an integer greater than 1\n" +
                                    "or subtracted by an integer greater than 0 if there is only one method call in the method\n" +
                                    "except in the main method it must be a polynomial of N", line);
                            return;
                        }

                        if (expression.charAt(i) == '-' && !hasSingleMethodCall) {
                            PolynomialClassCompiler.throwError("The first variable in the method call can be Lim subtracted by an integer greater than 0 \n" +
                                    "only if there is only one method call inside the method\n", line);
                            return;
                        }
                    }
                    int sum = 1;
                    int poi = i;
                    while (sum != 0) {
                        i++;
                        if (expression.charAt(i) == '(') sum++;
                        if (expression.charAt(i) == ')') sum--;
                        if (expression.charAt(i) == ',' && sum == 1) {
                            new Expression(expression.substring(poi + 1, i)).checkMethodCalls(isMainMethod, hasSingleMethodCall, line);
                            poi = i;
                        }
                    }
                    new Expression(expression.substring(poi + 1, i)).checkMethodCalls(isMainMethod, hasSingleMethodCall, line);

                }
            }
        }

        public static int getExpressionMagnitude(String expression, String limiter, boolean isLessThan) {
            // 0=constant 1=polynomial 2=other
            if ((isLessThan && expression.startsWith("Math.min(")) || ((!isLessThan) && expression.startsWith("Math.max("))) {
                expression = expression.substring(9).split(",")[0];
            }

            StringBuilder s = new StringBuilder();

            int order = 0;

            for (int i = 0; i < expression.length(); i++) {
                if ("+*/-".contains(expression.charAt(i) + "")) {
                    if (!Parser.isNumber(s.toString())) {
                        if (s.toString().equals(limiter)) {
                            order = Math.max(order, 1);
                        } else {
                            return 2;
                        }
                    }
                    s = new StringBuilder();
                } else if (expression.charAt(i) == '(') {
                    int cur = 1;
                    while (cur != 0) {
                        i++;
                        if (expression.charAt(i) == '(') cur++;
                        if (expression.charAt(i) == ')') cur--;
                        if (cur != 0) s.append(expression.charAt(i));
                    }
                    order = Math.max(order, getExpressionMagnitude(s.toString(), limiter, isLessThan));

                    s = new StringBuilder();
                } else {
                    s.append(expression.charAt(i));
                }
            }

            if (!Parser.isNumber(s.toString())) {
                if (s.toString().equals(limiter)) {
                    order = Math.max(order, 1);
                } else {
                    return 2;
                }
            }

            return order;
        }

        public static int getVariableMagnitude(String expression, HashMap<String, Integer> magnitudes) {
            // 0=constant 1=polynomial 2=exponential 3=other
            StringBuilder s = new StringBuilder();

            int order = 0;

            for (int i = 0; i < expression.length(); i++) {
                if ("+*/-".contains(expression.charAt(i) + "")) {
                    if (!Parser.isNumber(s.toString())) {
                        if (magnitudes.containsKey(s.toString())) {
                            order = Math.max(order, magnitudes.get(s.toString()));
                        }else{
                            return 3;
                        }
                    }
                    s = new StringBuilder();
                } else if (expression.charAt(i) == '(') {

                    if (s.toString().startsWith("Math.pow")) {
                        s = new StringBuilder();
                        int cur = 1;
                        int mag0 = -1;
                        int mag1 = -1;
                        while (cur != 0) {
                            i++;
                            if (expression.charAt(i) == '(') cur++;
                            if (expression.charAt(i) == ')') cur--;
                            if (expression.charAt(i) == ',' && cur == 1) {
                                mag0 = getVariableMagnitude(s.toString(), magnitudes);
                                s = new StringBuilder();
                            } else if (cur != 0) s.append(expression.charAt(i));
                        }
                        mag1 = getVariableMagnitude(s.toString(), magnitudes);
                        if (mag1 == 0) {
                            order = Math.max(order, mag0);
                        } else if (mag1 == 1) {
                            if (mag0 == 0) {
                                order = Math.max(order, 2);
                            } else {
                                return 3;
                            }
                        } else {
                            return 3;
                        }
                        s = new StringBuilder();
                    } else {
                        s = new StringBuilder();
                        int cur = 1;
                        while (cur != 0) {
                            i++;
                            if (expression.charAt(i) == '(') cur++;
                            if (expression.charAt(i) == ')') cur--;
                            if (cur != 0) s.append(expression.charAt(i));
                        }
                        order = Math.max(order, getVariableMagnitude(s.toString(), magnitudes));

                        s = new StringBuilder();
                    }
                } else {
                    s.append(expression.charAt(i));
                }
            }

            if (!Parser.isNumber(s.toString())) {
                if (magnitudes.containsKey(s.toString())) {
                    order = Math.max(order, magnitudes.get(s.toString()));
                }else{
                    return 3;
                }
            }

            return order;

        }
        public boolean equals(String s) {
            return s.equals(expression);
        }

        public String toString() {
            return expression;
        }
    }

}
