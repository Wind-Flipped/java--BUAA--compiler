import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MiddleCode {
    private static int i = 0;
    // used to print str
    private static int strCount = 0;
    private static HashMap<String, String> strings = new HashMap<>();
    private static String varName = null;
    private static int varDimen1 = 0;
    private static int varDimen2 = 0;

    public static HashMap<String, String> getStrings() {
        return strings;
    }

    public static void funcDecl(String type, String name) {
        FileStream.middleCodeOutput("#func_begin " + type + " " + name);
    }

    public static void funcDeclEnd(String type, String name) {
        FileStream.middleCodeOutput("#func_end " + type + " " + name);
    }

    public static void endGlobalDecl() {
        FileStream.middleCodeOutput("#global_end");
    }

    public static void paraDecl(String type, String name, int dimen1, int dimen2) {
        // default is 0, eg. [][3]
        if (dimen1 != 0 && dimen2 != 0) {
            FileStream.middleCodeOutput("#para " + type + " " + name + " [] [" + dimen2 + "]");
        } else if (dimen1 != 0) {
            FileStream.middleCodeOutput("#para " + type + " " + name + " []");
        } else {
            FileStream.middleCodeOutput("#para " + type + " " + name);
        }
    }

    public static void rParaDecl(String name) {
        // no dimen1 , dimen2
        /*
        if (dimen1 != null && dimen2 != null) {
            FileStream.middleCodeOutput("push " + name + "[" + dimen1 + "]" + "[" + dimen2 + "]");
        } else if (dimen1 != null) {
            FileStream.middleCodeOutput("push " + name + "[" + dimen1 + "]");
        } else {
            FileStream.middleCodeOutput("push " + name);
        }
        */
        FileStream.middleCodeOutput("#push " + name);

    }

    public static String callFunc(String name,String returnType) {
        FileStream.middleCodeOutput("#call " + name);
        if (returnType.equals("int")) {
            FileStream.middleCodeOutput("@t" + i + " = @RETURN");
            i++;
            return "@t" + (i-1);
        }
        return "@RETURN";
    }

    /*
    public static void getReturnValue(String name) {
        FileStream.middleCodeOutput(name + " = @RETURN");
    }
    */

    public static void funcReturn(String name) {
        if (name != null) {
            FileStream.middleCodeOutput("#return " + name);
        } else {
            FileStream.middleCodeOutput("#return");
        }

    }

    public static void varDecl(String type, String var, int dimen1, int dimen2) {
        // 常量不需要声明
        if (dimen1 != 0 && dimen2 != 0) {
            FileStream.middleCodeOutput("#var " + type + " " + var + " [" + dimen1 + "] [" + dimen2 + "]");
        } else if (dimen1 != 0) {
            FileStream.middleCodeOutput("#var " + type + " " + var + " [" + dimen1 + "]");
        } else {
            FileStream.middleCodeOutput("#var " + type + " " + var);
        }
        varName = var;
    }

    public static void varInit(int dimen1, int dimen2, String exp) {
        if (dimen1 == 0) {
            FileStream.middleCodeOutput(varName + " = " + exp);
        } else if (dimen2 == 0) {
            FileStream.middleCodeOutput(varName + "[" + varDimen1 + "]" + " = " + exp);
            varDimen1++;
            if (varDimen1 == dimen1) {
                varDimen1 = 0;
            }
        } else {
            FileStream.middleCodeOutput(varName + "[" + (varDimen1 * dimen2 + varDimen2) + "] = " + exp);
            varDimen2++;
            if (varDimen2 == dimen2) {
                varDimen1++;
                varDimen2 = 0;
            }
            if (varDimen1 == dimen1) {
                varDimen1 = 0;
            }
        }
    }

    public static String deDimen(String name, String dimen1, String dimen2, int colDimen, int curDimen) {
        // if curDimen != 0 , addr
        if (curDimen == 2 || (curDimen == 1 && dimen1 == null)) {
            // 数组传参地址
            return name + " [0] <addr>";
        } else if (curDimen == 1) {
            if (TargetCode.isInteger(dimen1)) {
                return name + " [" + (Integer.parseInt(dimen1) * colDimen) + "] <addr>";
            } else {
                FileStream.middleCodeOutput("@t" + i + " = " + dimen1 + " * " + colDimen);
                i++;
                return name + " [@t" + (i - 1) + "] <addr>";
            }
        }
        if (dimen2 != null) {
            FileStream.middleCodeOutput("@t" + i + " = " + dimen1 + " * " + colDimen);
            i++;
            FileStream.middleCodeOutput("@t" + i + " = " + "@t" + (i - 1) + " + " + dimen2);
            i++;
            return name + "[@t" + (i - 1) + "]";
        } else if (dimen1 != null) {
            return name + "[" + dimen1 + "]";
        } else {
            return name;
        }
    }

    public static String algorithmOp(String res1, String res2, String op) {
        try {
            int n1 = Parser.str2int(res1);
            int n2 = Parser.str2int(res2);
            switch (op) {
                case "+":
                    return String.valueOf(n1 + n2);
                case "-":
                    return String.valueOf(n1 - n2);
                case "*":
                    return String.valueOf(n1 * n2);
                case "/":
                    return String.valueOf(n1 / n2);
                case "%":
                    return String.valueOf(n1 % n2);
                default://TODO 其他运算，包括关系运算等
                    return "error";
            }
        } catch (Exception e) {
            if (op == null && res2 != null) {
                // 赋值语句
                FileStream.middleCodeOutput(res1 + " = " + res2);
                return null;
            }
            if (op == null) {
                // 无事发生
                return res1;
            }
            String des = "@t" + i;
            i++;
            if (res2 != null) {
                FileStream.middleCodeOutput(des + " = " + res1 + " " + op + " " + res2);
            } else {
                if (TargetCode.isInteger(res1)) {
                    if (op.equals("+")) {
                        return res1;
                    } else if (op.equals("-")){
                        if (res1.charAt(0) == '-') {
                            return res1.substring(1);
                        } else {
                            return "-" + res1;
                        }
                    }
                }
                FileStream.middleCodeOutput(des + " = " + op + " " + res1);
            }
            return des;
        }
    }

    public static void blockBegin(int layer) {
        // 第 layer 层开始
        FileStream.middleCodeOutput("#block_begin " + layer);
    }

    public static void blockEnd(int layer) {
        // 第 layer 层结束
        FileStream.middleCodeOutput("#block_end " + layer);
    }

    public static void mainBegin() {
        FileStream.middleCodeOutput("#main_begin");
    }

    public static void mainEnd() {
        FileStream.middleCodeOutput("#main_end");
        for (Map.Entry<String, String> entry : strings.entrySet()) {
            FileStream.middleCodeOutput("#stringDefine " + entry.getKey() + " " + entry.getValue());
        }
    }

    public static void callPrapare(String name) {
        FileStream.middleCodeOutput("#call_pre " + name);
    }

    //TODO getint() & printf

    public static void getint(String lval) {
        FileStream.middleCodeOutput("#read --> " + lval);
    }

    public static void prints(String[] splitString, ArrayList<String> vals) {
        int length = splitString.length;
        int j = 0;
        for (int i = 0; i < length; i++) {
            if (splitString[i].equals("")) {
                continue;
            } else if (splitString[i].equals("%d")) {
                print(vals.get(j), true);
                j++;
            } else {
                print(splitString[i], false);
            }
        }
    }

    public static void print(String str, boolean isInt) {
        if (isInt) {
            FileStream.middleCodeOutput("#printNum " + str);
        } else {
            String str1 = "str_" + strCount;
            strCount++;
            strings.put(str1, str);
            FileStream.middleCodeOutput("#printString " + str1);
        }
    }
}
