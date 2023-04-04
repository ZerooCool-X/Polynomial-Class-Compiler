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
        for (int i = 0; i < methods.size(); i++) {
            Method current = methods.get(i);
            if (current.name.equals("main") && current.returnType.equals("void") && current.paramTypes.size() == 1 && current.paramTypes.get(0).equals("String[]")) {
                methods.set(i, methods.get(0));
                methods.set(0, current);
                break;
            }
        }
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
        boolean skipFirst=false;
        for (Statement i : staticVariables) {
            if(!allowedStaticVariables.contains(i.leftHandSide))
                PolynomialClassCompiler.throwError("The only allowed static variables are "+allowedStaticVariables, i.line);

            if (i.leftHandSide.startsWith("Scanner ")&&!i.leftHandSide.equals("Scanner sc")) {
                PolynomialClassCompiler.throwError("The Scanner must be called sc", i.line);
            } else if (i.leftHandSide.equals("int N")) {
                foundLim = true;
                if (i.rightHandSide == null) {
                    skipFirst=true;
                    if(methods.get(0).instructions.isEmpty() || !methods.get(0).instructions.get(0).getClass().equals(Code.Statement.class)
                            || !((Statement) methods.get(0).instructions.get(0)).leftHandSide.equals("N") ||
                            ((Statement) methods.get(0).instructions.get(0)).rightHandSide == null)
                    PolynomialClassCompiler.throwError("The first instruction in the main method must be an initialization for the static variable N if it wasn't initialized ", methods.get(0).line);
                }
            }
        }
        if (!foundLim) {
            PolynomialClassCompiler.throwError("There must be a static int variable called N that limits the complexity of the code", line);
        }
        //Methods
        for (Method i : methods) i.check(skipFirst);
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

        public void check(boolean skipFirst) {
            if (name.equals("main")) {
                for (int i = skipFirst?1:0; i < instructions.size(); i++) instructions.get(i).check(true, true);
            } else {
                if (paramTypes.isEmpty() || !paramTypes.get(0).equals("int") || !paramNames.get(0).equals("Lim")) {
                    PolynomialClassCompiler.throwError("The fist parameter of all methods except the main method must be an\nint variable called Lim that limits the depth of the recursive calls", line);
                }
                if (instructions.isEmpty() || !instructions.get(0).getClass().equals(IfCondition.class) ||
                        !((IfCondition) instructions.get(0)).condition.equals("Lim<=0")) {
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

                    if(instructions.get(0).countMethodCalls()!=0)
                        PolynomialClassCompiler.throwError("Can't have method calls inside the base case Lim<=0", line);


                }
                int cnt=countMethodCalls();
                for (Instruction i : instructions) i.check(false, cnt<2);
            }

        }

        public int countMethodCalls() {
            int ret = 0;
            for (Instruction i : instructions){
                if(i.getClass().equals(Statement.class))
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

            ret.append("    ".repeat( depth));
            ret.append("for(").append(counterName).append("=").append(counterValue).append(";").append(condition).append(";").append(inc).append("){\n");

            for (Instruction i : inside) ret.append(i.toString(depth + 1));

            ret.append("    ".repeat( depth));

            ret.append("     }\n");

            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            String[]bound=condition.checkBooleanExpression();
            if (bound==null||!counterName.equals(bound[0])) {
                PolynomialClassCompiler.throwError("The condition of the for loop must be of the form \"(counter<polynomialInN)&&(otherBooleanStatement)&&(otherBooleanStatement)&&..\"", line);
            }
            int order=Expression.getExpressionMagnitude(bound[1],"N");
            if(order==2){
                if(isMainMethod||Expression.getExpressionMagnitude(bound[1],"Lim")==2){
                    PolynomialClassCompiler.throwError("The bound of the for loop must be a polynomial in N or a polynomial in Lim if it wasn't the main method", line);
                }else{
                    order=Expression.getExpressionMagnitude(bound[1],"Lim");
                }
            }
            if (countMethodCalls() > 0&&order>0)
                PolynomialClassCompiler.throwError("Can't have method calls inside non constant for loops (constant loops are loops that are bounded by constant value\ne.g. for(int i=0;i<5;i++)", line);

            condition.checkVariableChange(line);
            condition.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);

            if (condition.isLessThan()) {
                if(inc<=0)
                    PolynomialClassCompiler.throwError("The loop counter can only be incremented to grantee termination", line);
                variableChanged(counterName, true);
            } else {
                if(inc>=0)
                    PolynomialClassCompiler.throwError("The loop counter can only be decremented to grantee termination", line);

                variableChanged(counterName, false);
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
    }

    static class WhileLoop extends Instruction {
        ArrayList<Instruction> inside;
        Expression condition;
        String counterName;

        public String toString(int depth) {
            StringBuilder ret = new StringBuilder();

            ret.append(line);
            while (ret.length() != 5) ret.append(' ');

            ret.append("    ".repeat( depth));
            ret.append("while(").append(condition).append("){\n");

            for (Instruction i : inside) ret.append(i.toString(depth + 1));

            ret.append("    ".repeat( depth));

            ret.append("     }\n");

            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            String[]bound=condition.checkBooleanExpression();
            if (bound == null) {
                PolynomialClassCompiler.throwError("The condition of the while loop must be of the form \"(counter<polynomialInN)&&(otherBooleanStatement)&&(otherBooleanStatement)&&..\"", line);
            }
            counterName = bound[0];
            int order=Expression.getExpressionMagnitude(bound[1],"N");
            if(order==2){
                if(isMainMethod||Expression.getExpressionMagnitude(bound[1],"Lim")==2){
                    PolynomialClassCompiler.throwError("The bound of the while loop must be a polynomial in N or a polynomial in Lim if it wasn't the main method", line);
                }else{
                    order=Expression.getExpressionMagnitude(bound[1],"Lim");
                }
            }
            if (countMethodCalls() > 0&&order>0)
                PolynomialClassCompiler.throwError("Can't have method calls inside non constant while loops (constant loops are loops that are bounded by constant value\ne.g. while(i<5)", line);

            condition.checkVariableChange(line);
            condition.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);

            if (condition.isLessThan()) {
                boolean found = false;
                for (Instruction i : inside) {
                    if (i.getClass().equals(Code.Statement.class)) {
                        if (((Statement) i).leftHandSide.equals(counterName + "+")) found = true;
                        if (((Statement) i).leftHandSide.equals(counterName + "++")) found = true;
                    }
                }
                if (!found) {
                    PolynomialClassCompiler.throwError("The counter of the while loop must be incremented in its body to grantee termination", line);
                }
                variableChanged(counterName, true);
            } else {
                boolean found = false;
                for (Instruction i : inside) {
                    if (i.getClass().equals(Code.Statement.class)) {
                        if (((Statement) i).leftHandSide.equals(counterName + "-")) found = true;
                        if (((Statement) i).leftHandSide.equals(counterName + "--")) found = true;
                    }
                }
                if (!found) {
                    PolynomialClassCompiler.throwError("The counter of the while loop must be decremented in its body to grantee termination", line);
                }
                variableChanged(counterName, false);
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
    }

    static class IfCondition extends Instruction {
        ArrayList<Instruction> thenPart;
        ArrayList<Instruction> elsePart;
        Expression condition;

        public String toString(int depth) {
            StringBuilder ret = new StringBuilder();

            ret.append(line);
            while (ret.length() != 5) ret.append(' ');

            ret.append("    ".repeat( depth));
            ret.append("if(").append(condition).append("){\n");

            for (Instruction i : thenPart) ret.append(i.toString(depth + 1));

            ret.append("    ".repeat( depth));
            ret.append("     }\n");
            if (elsePart != null) {
                ret.append("    ".repeat( depth));
                ret.append("     else{\n");

                for (Instruction i : elsePart) ret.append(i.toString(depth + 1));

                ret.append("    ".repeat( depth));

                ret.append("     }\n");
            }
            return ret.toString();
        }

        public void check(boolean isMainMethod, boolean hasSingleMethodCall) {
            condition.checkVariableChange(line);
            condition.checkMethodCalls(isMainMethod, hasSingleMethodCall, line);

            for (Instruction i : thenPart) i.check(isMainMethod, hasSingleMethodCall);
            if (elsePart != null)
                for (Instruction i : elsePart) i.check(isMainMethod, hasSingleMethodCall);
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
            if (leftHandSide.split(" ").length == 2 && leftHandSide.split(" ")[1].equals("N")) {
                PolynomialClassCompiler.throwError("The variable name N is reserved for the complexity limiter", line);
            }
            if (leftHandSide.split(" ").length == 2 && leftHandSide.split(" ")[0].equals("Scanner")&&!leftHandSide.split(" ")[1].equals("sc")) {
                PolynomialClassCompiler.throwError("Scanner objects must be called sc", line);
            }
            if (leftHandSide.split(" ").length == 2 && leftHandSide.split(" ")[1].equals("Lim")) {
                PolynomialClassCompiler.throwError("The variable name Lim is reserved for the complexity limiter", line);
            }

            if (leftHandSide.equals("N") || leftHandSide.equals("N+") || leftHandSide.equals("N++")) {
                PolynomialClassCompiler.throwError("Changing the value of variable N is prohibited", line);
            }
            if ((leftHandSide.equals("Lim") || leftHandSide.equals("Lim+") || leftHandSide.equals("Lim++")) && !isMainMethod) {
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
            }else if(leftHandSide.startsWith(variableName)&&Parser.isSymbol(leftHandSide.charAt(variableName.length()))&&rightHandSide!=null){
                PolynomialClassCompiler.throwError("The value of the loop counter can only be either incremented or decremented by a constant to grantee termination", line);
            }
            else{
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
                if (expression.charAt(i) == '=' && expression.charAt(i + 1) != '=' && expression.charAt(i - 1) != '<' && expression.charAt(i - 1) != '>' && expression.charAt(i - 1) != '=') {
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
            while (ind<expression.length()&&expression.charAt(ind) == '(') ind++;
            int jnd = ind;
            if (Parser.isNumber(expression.charAt(ind) + "")) {
                return null;
            }
            while (jnd<expression.length()&&!Parser.isSymbol(expression.charAt(jnd))) jnd++;
            String[]ret=new String[2];
            ret[0] = expression.substring(ind, jnd);

            if (expression.startsWith(ret[0] + "<") || expression.startsWith(ret[0] + ">")) {
                if (expression.contains("&") || expression.contains("|")) {
                    return null;
                }
                ret[1]=expression.substring(jnd+1);
            } else if (!expression.startsWith("(" + ret[0] + "<") && !expression.startsWith("(" + ret[0] + ">")) {
                return null;
            } else {
                int cnt = 1;

                for (int i = 1; i < expression.length(); i++) {
                    if (expression.charAt(i) == '(') cnt++;
                    else if (expression.charAt(i) == ')'){
                        cnt--;
                        if(cnt==0&&ret[1]==null)
                            ret[1]=expression.substring(jnd+1,i);
                    }
                    else if (cnt == 0 ) {
                        if(expression.charAt(i) != '&')
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
                    cnt++;
                    int sum = 1;
                    while (sum != 0) {
                        i++;
                        if (expression.charAt(i) == '(') sum++;
                        else if (expression.charAt(i) == ')') sum--;
                    }
                }
            }
            return cnt;
        }

        public void checkMethodCalls(boolean isMainMethod, boolean hasSingleMethodCall, int line) {
            for (int i = 1; i < expression.length(); i++) {
                if (expression.charAt(i) == '(' && !Parser.isSymbol(expression.charAt(i - 1))) {

                    if (isMainMethod) {
                        int ind = i;
                        int sum = 1;
                        while (sum != 0 && expression.charAt(ind) != ',') {
                            ind++;
                            if (expression.charAt(ind) == '(') sum++;
                            else if (expression.charAt(ind) == ')') sum--;
                        }
                        if (getExpressionMagnitude(expression.substring(i + 1, ind), "N") == 2) {
                            PolynomialClassCompiler.throwError("The first variable of the method call inside the main method must be a polynomial of N", line);
                        }
                    } else {
                        int j = i - 1;
                        while (j > 0 && !Parser.isSymbol(expression.charAt(j))) j--;
                        if (expression.substring(j, i).equals("main"))
                            PolynomialClassCompiler.throwError("Can't call main method", line);
                        int ind = expression.indexOf('/', i) == -1 ? expression.indexOf('-', i) :
                                expression.indexOf('-', i) == -1 ? expression.indexOf('/', i) :
                                        Math.min(expression.indexOf('/', i), expression.indexOf('-', i));
                        if (ind == -1 || !expression.substring(i + 1, ind).equals("Lim") ) {
                            PolynomialClassCompiler.throwError("The first variable in the method call must be Lim divided by an integer greater than 1\n" +
                                    "or subtracted by an integer greater than 0 if there is only one method call in the method\n" +
                                    "except in the main method it must be a polynomial of N", line);
                            return;
                        }
                        i = ind;
                        ind++;

                        while (!Parser.isSymbol(expression.charAt(ind))) ind++;
                        if ((expression.charAt(ind) != ',' && expression.charAt(ind) != ')') || !Parser.isNumber(expression.substring(i + 1, ind)) || Integer.parseInt(expression.substring(i + 1, ind)) <(expression.charAt(i)=='-'?1:2)) {

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
                    while (sum != 0) {
                        i++;
                        if (expression.charAt(i) == '(') sum++;
                        else if (expression.charAt(i) == ')') sum--;
                    }
                }
            }
        }

        public static int getExpressionMagnitude(String expression, String limiter) {
            // 0=constant 1=polynomial 2=other
            String[] elements = expression.split("[+*/-]");
            int order = 0;
            for (String i : elements) {
                for (char j : i.toCharArray()) {
                    if (Parser.isSymbol(j)) return 2;
                }
                if (i.equals(limiter)) {
                    order = 1;
                } else if (!Parser.isNumber(i)) {
                    return 2;
                }
            }

            return order;
        }

        public boolean equals(String s){
            return s.equals(expression);
        }

        public String toString() {
            return expression;
        }
    }

}
