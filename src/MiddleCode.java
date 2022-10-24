public class MiddleCode {
    private static int i = 0;

    public static void funcDecl(String type, String name) {
        FileStream.middleCodeOutput(type + " " + name);
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

    public static void rParaDecl(String name, String dimen1, String dimen2) {
        if (dimen1 != null && dimen2 != null) {
            FileStream.middleCodeOutput("push " + name + "[" + dimen1 + "]" + "[" + dimen2 + "]");
        } else if (dimen1 != null) {
            FileStream.middleCodeOutput("push " + name + "[" + dimen1 + "]");
        } else {
            FileStream.middleCodeOutput("push " + name);
        }
    }

    public static void callFunc(String name) {
        FileStream.middleCodeOutput("call " + name);
    }

    public static void getReturnValue(String name) {
        FileStream.middleCodeOutput(name + " = @RETURN");
    }

    public static void funcReturn(String name) {
        if (name != null) {
            FileStream.middleCodeOutput("return " + name);
        } else {
            FileStream.middleCodeOutput("return");
        }

    }

    public static void varDecl(String type, String var, String dimen1, String dimen2) {
        // 常量不需要声明
        if (dimen1 != null && dimen2 != null) {
            FileStream.middleCodeOutput("var " + type + " " + var + "[" + dimen1 + "][" + dimen2 + "]");
        } else if (dimen1 != null) {
            FileStream.middleCodeOutput("var " + type + " " + var + "[" + dimen1 + "]");
        } else {
            FileStream.middleCodeOutput("var " + type + " " + var);
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
