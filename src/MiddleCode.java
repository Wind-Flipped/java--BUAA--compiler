public class MiddleCode {
    private static int i = 0;
    private static String varName = null;
    private static int varDimen1 = 0;
    private static int varDimen2 = 0;

    public static void funcDecl(String type, String name) {
        FileStream.middleCodeOutput("func " + type + " " + name);
    }

    public static void paraDecl(String type, String name, int dimen1, int dimen2) {
        // default is 0, eg. [][3]
        if (dimen1 != 0 && dimen2 != 0) {
            FileStream.middleCodeOutput("para " + type + " " + name + " [][" + dimen2 + "]");
        } else if (dimen1 != 0) {
            FileStream.middleCodeOutput("para " + type + " " + name + " []");
        } else {
            FileStream.middleCodeOutput("para " + type + " " + name);
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
        FileStream.middleCodeOutput("push " + name);

    }

    public static void callFunc(String name) {
        FileStream.middleCodeOutput("call " + name);
    }

    /*
    public static void getReturnValue(String name) {
        FileStream.middleCodeOutput(name + " = @RETURN");
    }
    */

    public static void funcReturn(String name) {
        if (name != null) {
            FileStream.middleCodeOutput("return " + name);
        } else {
            FileStream.middleCodeOutput("return");
        }

    }

    public static void varDecl(String type, String var, int dimen1, int dimen2) {
        // 常量不需要声明
        if (dimen1 != 0 && dimen2 != 0) {
            FileStream.middleCodeOutput("var " + type + " " + var + "[" + dimen1 + "][" + dimen2 + "]");
        } else if (dimen1 != 0) {
            FileStream.middleCodeOutput("var " + type + " " + var + "[" + dimen1 + "]");
        } else {
            FileStream.middleCodeOutput("var " + type + " " + var);
        }
        varName = var;
    }

    public static void varInit(int dimen1, int dimen2,String exp) {
        if (dimen1 == 0) {
            FileStream.middleCodeOutput(varName + " = " + exp);
        } else if (dimen2 == 0) {
            FileStream.middleCodeOutput(varName + "[" + varDimen1 + "]" + " = " + exp);
            varDimen1++;
            if (varDimen1 == dimen1) {
                varDimen1 = 0;
            }
        } else {
            FileStream.middleCodeOutput(varName + "[" + varDimen1 + "][" + varDimen2 + "] = " + exp);
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

    public static String deDimen(String name, String dimen1, String dimen2, int colDimen) {
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
            int n1 = Integer.parseInt(res1);
            int n2 = Integer.parseInt(res2);
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
                FileStream.middleCodeOutput(des + " = " + op + " " + res1);
            }
            return des;
        }
    }

    //TODO getint() & printf
}
