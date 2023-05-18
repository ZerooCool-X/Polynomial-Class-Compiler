import java.util.*;

public class Parser {
    static String code;
    static int[] closingBracket;
    static int[] nextSemicolon;
    static ArrayList<Integer> lineNumbers = new ArrayList<>();


    public static void parseCode(Code ret, String c) {
        preCompute(c);
        int n = code.length();

        for (int i = 0; i < n; i++) {
            if (code.charAt(i) == ' ' || code.charAt(i) == '}') {
                continue;
            }
            //imports
            if (code.startsWith("import", i)) {
                Code.Statement sta = new Code.Statement();
                sta.leftHandSide = code.substring(i + 7, nextSemicolon[i]);
                sta.line = lineNumbers.get(i);
                ret.imports.add(sta);
                i = nextSemicolon[i];
                continue;
            }
            //The class
            if (code.startsWith("public class", i)) {
                ret.line = lineNumbers.get(i);
                while (code.charAt(i) != '{') i++;
                continue;
            }

            //Methods
            boolean foundStatic = false;
            if (code.startsWith("public ", i) || code.startsWith("static ", i)) {
                if (code.startsWith("static ", i)) foundStatic = true;
                i += 7;
                if (code.startsWith("public ", i) || code.startsWith("static ", i)) {
                    if (code.startsWith("static ", i)) foundStatic = true;
                    i += 7;
                }

                if (!foundStatic) {

                    PolynomialClassCompiler.throwError("All methods and global variables must be static", lineNumbers.get(i));
                }

                if (startsWithDataType(code, i)) {
                    int j = i;
                    while (code.charAt(j) != '{' && nextSemicolon[j] != j) j++;
                    if (code.charAt(j) == '{') {
                        ret.methods.add(parseMethod(i, closingBracket[j]));
                        i = closingBracket[j];
                    } else {
                        Code.Statement sta = new Code.Statement();
                        int k = code.indexOf('=', i);
                        if (k < j && k > i) {
                            sta.rightHandSide = new Code.Expression(code.substring(k + 1, j));
                        } else {
                            k = j;
                        }
                        sta.leftHandSide = code.substring(i, k);
                        sta.line = lineNumbers.get(i);
                        ret.staticVariables.add(sta);
                        i = nextSemicolon[j];
                    }
                    continue;
                }else{
                    i--;
                }
                continue;
            }

            if(code.startsWith("class", i) )
                PolynomialClassCompiler.throwError("Can't have subclasses in this fragment of java", lineNumbers.get(i));

            PolynomialClassCompiler.throwError("All methods and global variables must be static", lineNumbers.get(i));


        }

    }

    public static Code.Method parseMethod(int s, int e) {
        Code.Method ret = new Code.Method();
        ret.line = lineNumbers.get(s);
        int j = s;
        while (code.charAt(j) != '(') j++;
        String[] signature = code.substring(s, j).split(" ");
        ret.name = signature[signature.length - 1];
        ret.returnType = signature[signature.length - 2];
        s = j + 1;
        j = closingBracket[j];
        if (j != s) {
            String[] param = code.substring(s, j).split(",");
            for (String value : param) {

                int k = value.length() - 1;
                boolean found = false;
                while (!found || (!isSymbol(value.charAt(k)))) {
                    if (!isSymbol(value.charAt(k))) {
                        found = true;
                    }
                    k--;
                }
                ret.paramTypes.add(value.substring(0, k + 1).replaceAll(" ", ""));
                ret.paramNames.add(value.substring(k + 1).replaceAll(" ", ""));
            }
        }
        s = j + 1;
        while (code.charAt(s) != '{') s++;
        ret.instructions = parseInstruction(s + 1, closingBracket[s] - 1);

        return ret;
    }

    public static ArrayList<Code.Instruction> parseInstruction(int s, int e) {

        ArrayList<Code.Instruction> ret = new ArrayList<>();
        for (int i = s; i < e; i++) {
            if (statsWithReservedWord(code,i)!=null) {
                PolynomialClassCompiler.throwError("Can't use \""+statsWithReservedWord(code,i)+"\" in this fragment of java", lineNumbers.get(i));
            } else if (code.startsWith("for", i)) {
                // For loops
                Code.ForLoop inst = new Code.ForLoop();
                inst.line = lineNumbers.get(i);
                int start = i;
                while (code.charAt(start) != '(') start++;
                String[] params = code.substring(start + 1, closingBracket[start]).split(";");
                if (!params[0].contains("int") || !params[0].contains("=")) {
                    PolynomialClassCompiler.throwError("The counter of the for loop must be initialized in the loop", lineNumbers.get(i));
                }
                inst.counterName = params[0].split("[ =]")[1];
                if (!isNumber(params[0].split("[ =]")[2])) {
                    PolynomialClassCompiler.throwError("The counter of the loop must be initialized with a constant", lineNumbers.get(i));
                }
                inst.counterValue = Integer.parseInt(params[0].split("[ =]")[2]);
                inst.condition = new Code.Expression(params[1]);
                if (params[2].startsWith(inst.counterName + "+=")) {
                    if (!isNumber(params[2].substring(params[2].indexOf('=') + 1))) {
                        PolynomialClassCompiler.throwError("The counter of the loop must be incremented or decremented by a constant", lineNumbers.get(i));
                    }
                    inst.inc = Integer.parseInt(params[2].substring(params[2].indexOf('=') + 1));
                } else if (params[2].startsWith(inst.counterName + "-=")) {
                    if (!isNumber(params[2].substring(params[2].indexOf('=') + 1))) {
                        PolynomialClassCompiler.throwError("The counter of the loop must be incremented or decremented by a constant", lineNumbers.get(i));
                    }
                    inst.inc = -Integer.parseInt(params[2].substring(params[2].indexOf('=') + 1));
                } else if (params[2].equals(inst.counterName + "++")) {
                    inst.inc = 1;
                } else if (params[2].equals(inst.counterName + "--")) {
                    inst.inc = -1;
                } else {
                    PolynomialClassCompiler.throwError("The counter of the loop must be incremented or decremented by a constant", lineNumbers.get(i));
                }
                start = closingBracket[start] + 1;
                if (code.charAt(start) == '{') {
                    inst.inside = parseInstruction(start + 1, closingBracket[start]);
                    i = closingBracket[start];
                } else {
                    inst.inside = parseInstruction(start, getOneInstruction(s));
                    i = getOneInstruction(s);
                }
                ret.add(inst);
            }
            else if (code.startsWith("while", i)) {
                // While loop
                Code.WhileLoop inst = new Code.WhileLoop();
                inst.line = lineNumbers.get(i);

                int start = i;
                while (code.charAt(start) != '(') start++;
                inst.condition = new Code.Expression(code.substring(start + 1, closingBracket[start]));

                start = closingBracket[start] + 1;
                if (code.charAt(start) == '{') {
                    inst.inside = parseInstruction(start + 1, closingBracket[start]);
                    i = closingBracket[start];
                } else {
                    inst.inside = parseInstruction(start, getOneInstruction(s));
                    i = getOneInstruction(s);
                }
                ret.add(inst);
            }
            else if (code.startsWith("if", i)) {
                // If conditions
                Code.IfCondition inst = new Code.IfCondition();
                inst.line = lineNumbers.get(i);

                int start = i;
                int end = getOneInstruction(start);

                while (code.charAt(start) != '(') start++;
                inst.condition = new Code.Expression(code.substring(start + 1, closingBracket[start]));
                start = closingBracket[start] + 1;
                if (code.charAt(start) == '{') {
                    inst.thenPart = parseInstruction(start + 1, closingBracket[start]);
                    start = closingBracket[start] + 1;
                } else {
                    inst.thenPart = parseInstruction(start, getOneInstruction(start));
                    start = getOneInstruction(start) + 1;
                }
                if (code.startsWith("else", start)) {
                    start += 4;
                    inst.elsePart = parseInstruction(start + 1, end);
                }

                i = end;
                ret.add(inst);
            }
            else if (code.startsWith("System.out.print", i)) {

                // Print
                Code.Statement inst = new Code.Statement();
                inst.line = lineNumbers.get(i);

                int k = i;
                while (code.charAt(k) != '(') k++;
                inst.leftHandSide = code.substring(i, k);
                inst.rightHandSide = new Code.Expression(code.substring(k + 1, nextSemicolon[i] - 1));

                i = nextSemicolon[i];
                ret.add(inst);
            }
            else if ((code.startsWith("return", i) && isSymbol(code.charAt(i + 6))) ||
                    (code.startsWith("break", i) && isSymbol(code.charAt(i + 5))) ||
                    (code.startsWith("continue", i) && isSymbol(code.charAt(i + 8)))) {
                Code.Statement inst = new Code.Statement();
                inst.line = lineNumbers.get(i);
                int ind = i;
                while (!isSymbol(code.charAt(ind))) ind++;
                inst.leftHandSide = code.substring(i, ind);
                i = ind;
                if (code.charAt(i) == ' ') i++;
                if (nextSemicolon[i] > i)
                    inst.rightHandSide = new Code.Expression(code.substring(i, nextSemicolon[i] ));

                i = nextSemicolon[i];
                ret.add(inst);
            } else if (startsWithDataType(code, i) ||
                    (code.indexOf('=', i) + Integer.MAX_VALUE < code.indexOf(';', i) + Integer.MAX_VALUE
                            && code.indexOf('=', i) + Integer.MAX_VALUE < code.indexOf(':', i) + Integer.MAX_VALUE)) {
                //statement
                Code.Statement inst = new Code.Statement();
                inst.line = lineNumbers.get(i);

                int k = code.indexOf('=', i);
                int j = nextSemicolon[i];
                if (k < j && k > i) {
                    inst.rightHandSide = new Code.Expression(code.substring(k + 1, j));
                } else {
                    k = j;
                }
                inst.leftHandSide = code.substring(i, k);

                i = nextSemicolon[j];
                ret.add(inst);
            } else {
                int ind = i;
                while (!isSymbol(code.charAt(ind))) ind++;
                if (code.charAt(ind) == ':') {
                    if (!code.startsWith("for(", ind + 1) && !code.startsWith("while(", ind + 1)) {
                        PolynomialClassCompiler.throwError("Can't have labels except behind for and while loops", lineNumbers.get(i));
                    }
                    Code.Statement inst = new Code.Statement();
                    inst.line = lineNumbers.get(i);
                    inst.leftHandSide = code.substring(i, ind + 1);

                    i = ind;

                    ret.add(inst);
                } else {
                    Code.Statement inst = new Code.Statement();
                    inst.line = lineNumbers.get(i);
                    int j = nextSemicolon[i];
                    inst.leftHandSide = code.substring(i, j);

                    i = nextSemicolon[j];

                    ret.add(inst);
                }

            }


        }
        return ret;
    }


    static int getOneInstruction(int s) {
        int e = s;
        if (code.charAt(e) == '{') return closingBracket[e];
        if (code.charAt(e) == '}') return e;
        if (code.startsWith("for", s) || code.startsWith("while", s)) {
            while (code.charAt(e) != '(') e++;
            e = closingBracket[e] + 1;
            if (code.charAt(e) == '{') {
                e = closingBracket[e];
            } else {
                e = getOneInstruction(e);
            }
        } else if (code.startsWith("if", s)) {
            while (code.charAt(e) != '(') e++;
            e = closingBracket[e] + 1;
            if (code.charAt(e) == '{') {
                e = closingBracket[e] + 1;
            } else {
                e = getOneInstruction(e) + 1;
            }
            if (code.startsWith("else", e)) {
                e += 4;
                if (code.charAt(e) == '{') {
                    e = closingBracket[e];
                } else {
                    e = getOneInstruction(e + 1);
                }
            } else {
                e--;
            }
        } else {
            e = nextSemicolon[e];
        }
        return e;
    }

    static String statsWithReservedWord(String s, int start) {
        String[] reserved = {"abstract", "assert", "case", "catch", "const", "default", "do", "enum", "extends", "final",
                "finally", "goto", "implements", "instanceof", "interface", "native", "package", "private", "protected",
                "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient","try", "volatile"};



        for(String i:reserved){
            if(s.startsWith(i,start)&&isSymbol(s.charAt(start+i.length())))return i;
        }

        return null;
    }

    static boolean startsWithDataType(String s, int start) {
        if (s.startsWith("int", start)) return true;
        if (s.startsWith("boolean", start)) return true;
        if (s.startsWith("String", start)) return true;
        if (s.startsWith("double", start)) return true;
        if (s.startsWith("char", start)) return true;
        if (s.startsWith("void", start)) return true;
        if (s.startsWith("Scanner", start)) return true;

        return false;
    }

    static boolean isNumber(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') continue;
            if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        }
        return true;
    }

    static boolean isSymbol(char c) {
        return !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9');
    }

    static void preCompute(String c) {
        StringBuilder s = new StringBuilder();
        lineNumbers = new ArrayList<>();
        int lineNumber = 1;

        boolean str = false;
        boolean cha = false;
        for (int i = 0; i < c.length(); i++) {
            if (str) {
                if (c.charAt(i) == '"') {
                    int count = 0;
                    while (c.charAt(i - 1 - count) == '\\') count++;

                    if (count % 2 == 0) {
                        str = false;
                        s.append('"');
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else if (cha) {
                if (c.charAt(i) == '\'') {
                    int count = 0;
                    while (c.charAt(i - 1 - count) == '\\') count++;
                    if (count % 2 == 0) {
                        cha = false;
                        s.append('\'');
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else {
                if (c.charAt(i) == '"') {
                    str = true;
                    s.append(c.charAt(i));
                } else if (c.charAt(i) == '\'') {
                    cha = true;
                    s.append(c.charAt(i));
                } else if (c.charAt(i) == ' ' || c.charAt(i) == 13) {
                    if ((i == c.length() - 1) || isSymbol(c.charAt(i + 1))) continue;
                    if ((s.length() == 0) || isSymbol(s.charAt(s.length() - 1))) continue;

                    s.append(' ');
                } else if (c.charAt(i) == '\n') {
                    lineNumber++;
                    if ((i == c.length() - 1) || isSymbol(c.charAt(i + 1))) continue;
                    if ((s.length() == 0) || isSymbol(s.charAt(s.length() - 1))) continue;

                    s.append(' ');
                } else if(c.charAt(i) == '/'&&c.charAt(i+1) == '/'){
                    while(i+1<c.length()&&c.charAt(i+1)!='\n')i++;
                    continue;

                }else{
                    s.append(c.charAt(i));

                }
            }
            lineNumbers.add(lineNumber);

        }

        code = s.toString();

        int n = code.length();
        closingBracket = new int[n];
        nextSemicolon = new int[n];
        Stack<Integer> pos = new Stack<>();
        for (int i = 0; i < n; i++) {
            if (code.charAt(i) == ';') {
                nextSemicolon[i] = i;
            }
            if (code.charAt(i) == '[' || code.charAt(i) == '(' || code.charAt(i) == '{') {
                pos.push(i);
            }
            if (code.charAt(i) == ']' || code.charAt(i) == ')' || code.charAt(i) == '}') {
                closingBracket[pos.pop()] = i;
            }


        }
        for (int i = n - 2; i > -1; i--) {
            if (nextSemicolon[i] == 0) {
                nextSemicolon[i] = nextSemicolon[i + 1];
            }
        }
    }
}
