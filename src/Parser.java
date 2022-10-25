import java.util.ArrayList;

public class Parser {
    private Lexer lexer;
    private int line;
    private int formerLine;
    private String tag;
    private String curToken;
    private SymbolTable curTable; // 当前符号表
    private String curReturnType; // 当前函数返回值，若为null，则为全局环境
    private int curLayer; // 当前大括号层数
    private boolean hasReturn; // 当前是否为末尾的return
    private String curLval; // 当前lval
    private int loopLayer; // 当前循环层数
    private int curValDimension; // 当前变量维数
    private String btype; // 当前btype，目前只有int类型

    // 表达式返回值


    public Parser() {
        String s = FileStream.getNextLine();
        if (s != null) {
            lexer = new Lexer(s);
        }
        tag = null;
        curToken = null;
        line = 0;
        curTable = null;
        curReturnType = null;
        curLayer = 0;
        hasReturn = false;
        curLval = null;
        loopLayer = 0;
        curValDimension = 0;
        formerLine = 0;
    }

    private void error(String c) {
        if (c.equals("i") || c.equals("j") || c.equals("k")) {
            FileStream.error(formerLine + " " + c);
        } else {
            FileStream.error(line + " " + c);
        }
        // FileStream.output(line + tag + curToken);
    }

    public void compUnit() {
        getToken();
        while (decl()) ;
        while (funcDef()) ;
        if (mainFuncDef()) {
            FileStream.output("<CompUnit>");
        } else {
            error("main func not match");
        }
    }

    private boolean decl() {
        return constDecl() || varDecl();
    }

    private boolean bType() {
        if (tag.equals("INTTK")) {
            btype = curToken;
            getToken();
            return true;
        }
        return false;
    }

    private boolean bTypeFunc() {
        return (tag.equals("INTTK") || tag.equals("VOIDTK")) && lexer.watchBracket().equals("(");
    }

    private boolean constDecl() {
        if (tag.equals("CONSTTK") && !lexer.watchBracket().equals("(")) {
            // judge if func
            getToken();
        } else {
            return false;
        }
        bType();
        constDef();
        while (tag.equals("COMMA")) {
            getToken();
            constDef();
        }
        if (tag.equals("SEMICN")) {
            getToken();
            FileStream.output("<ConstDecl>");
            return true;
        } else {
            error("i");
            return false;
        }
    }

    private boolean constDef() {
        int dimen = 0;
        String name;
        if (tag.equals("IDENFR")) {
            name = curToken;
            getToken();
        } else {
            return false;
        }
        int dimen1 = 0;
        int dimen2 = 0;
        while (tag.equals("LBRACK")) {
            getToken();
            if (dimen == 0) {
                dimen1 = Integer.parseInt(constExp());
            } else {
                dimen2 = Integer.parseInt(constExp());
            }
            if (tag.equals("RBRACK")) {
                getToken();
            } else {
                error("k");
            }
            dimen++;
        }
        if (curTable != null) {
            if (!curTable.addVal(name,true,dimen,dimen1,dimen2)) {
                error("b");
            }
        } else {
            if (!SymbolTable.addGlobalVal(name, true, dimen,dimen1,dimen2)) {
                error("b");
            }
        }
        Val val;
        // 给后面初始化传常量
        if (curTable == null) {
            val = new SymbolTable(null).findVal(name);
        } else {
            val = curTable.findVal(name);
        }

        if (tag.equals("ASSIGN")) {
            getToken();
        } else {
            error("const val not assgin init value");
        }
        if (constInitVal(val)) {
            FileStream.output("<ConstDef>");
            return true;
        } else {
            error("const val not assgin init value");
            return false;
        }
    }

    private boolean constInitVal(Val val) {
        String constExp = constExp();
        if (constExp != null) {
            FileStream.output("<ConstInitVal>");
            val.setValue(Integer.parseInt(constExp));
            return true;
        } else {
            if (curToken.equals("{")) {
                getToken();
                if (constInitVal(val)) {
                    while (curToken.equals(",")) {
                        getToken();
                        constInitVal(val);
                    }
                } else {
                    error("lack }");
                }
                while (constInitVal(val)) {
                    while (curToken.equals(",")) {
                        getToken();
                        constInitVal(val);
                    }
                }
                if (curToken.equals("}")) {
                    getToken();
                    FileStream.output("<ConstInitVal>");
                    return true;
                } else {
                    error("lack }");
                    return false;
                }
            }
        }
        return false;
    }

    private boolean varDecl() {
        if (bTypeFunc()) {
            return false;
        }
        if (bType() && varDef()) {
            while (tag.equals("COMMA")) {
                getToken();
                varDef();
            }
            if (tag.equals("SEMICN")) {
                getToken();
                FileStream.output("<VarDecl>");
                return true;
            } else {
                error("i");
                return false;
            }
        }
        return false;
    }

    private boolean varDef() {
        int dimen = 0;
        String name;
        if (tag.equals("IDENFR")) {
            name = curToken;
            getToken();
        } else {
            return false;
        }
        int dimen1 = 0;
        int dimen2 = 0;
        while (tag.equals("LBRACK")) {
            getToken();
            if (dimen == 0) {
                dimen1 = Integer.parseInt(constExp());
            } else {
                dimen2 = Integer.parseInt(constExp());
            }
            if (tag.equals("RBRACK")) {
                getToken();
            } else {
                error("k");
            }
            dimen++;
        }
        if (curTable != null) {
            if (!curTable.addVal(name,false,dimen,dimen1,dimen2)) {
                error("b");
            }
        } else {
            if (!SymbolTable.addGlobalVal(name, false, dimen,dimen1,dimen2)) {
                error("b");
            }
        }
        MiddleCode.varDecl(btype,name,dimen1,dimen2);
        if (tag.equals("ASSIGN")) {
            getToken();
            if (initVal(dimen1,dimen2)) {
                FileStream.output("<VarDef>");
                return true;
            } else {
                error("var declaration no init value");
                return false;
            }
        } else {
            FileStream.output("<VarDef>");
            return true;
        }
    }

    private boolean initVal(int dimen1,int dimen2) {
        String exp = exp();
        if (exp != null) {
            FileStream.output("<InitVal>");
            MiddleCode.varInit(dimen1,dimen2,exp);
            return true;
        } else {
            if (curToken.equals("{")) {
                getToken();
                if (initVal(dimen1,dimen2)) {
                    while (curToken.equals(",")) {
                        getToken();
                        initVal(dimen1,dimen2);
                    }
                } else {
                    error("no init value");
                }
                while (initVal(dimen1,dimen2)) {
                    while (curToken.equals(",")) {
                        getToken();
                        initVal(dimen1,dimen2);
                    }
                }
                if (curToken.equals("}")) {
                    getToken();
                    FileStream.output("<InitVal>");
                    return true;
                } else {
                    error("lack }");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean funcDef() {
        if (funcType()) {
            String name;
            if (tag.equals("IDENFR")) {
                name = curToken;
                MiddleCode.funcDecl(curReturnType,name);
                getToken();
                if (tag.equals("LPARENT")) {
                    getToken();
                    funcFParams(); // 可有可无
                    if (tag.equals("RPARENT")) {
                        getToken();
                    } else {
                        error("j");
                    }
                    if (!SymbolTable.addFunc(name,curReturnType)) {
                        error("b");
                    }
                    // func 定义完，建作用域
                    if (block()) {
                        FileStream.output("<FuncDef>");
                        curReturnType = null;
                        SymbolTable.clearPara();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean mainFuncDef() {
        /*
        if (tag.equals("INTTK")) {
            getToken();
        } else {
            error();
        }
        */
        // int 已经被分析过了
        if (tag.equals("MAINTK")) {
            if (!SymbolTable.addFunc("main","int")) {
                error("b");
            }
            getToken();
        }
        if (tag.equals("LPARENT")) {
            getToken();
        }
        if (tag.equals("RPARENT")) {
            getToken();
        } else {
            error("j");
        }
        FileStream.middleCodeOutput("main");
        if (block()) {
            FileStream.output("<MainFuncDef>");
            SymbolTable.clearPara();
            return true;
        }
        return false;
    }

    private boolean funcType() {
        if (tag.equals("VOIDTK") || tag.equals("INTTK")) {
            curReturnType = curToken;
            getToken();
            if (tag.equals("MAINTK")) {
                return false;
            }
            FileStream.output("<FuncType>");
            return true;
        }
        return false;
    }

    private boolean funcFParams() {
        if (funcFParam()) {
            while (tag.equals("COMMA")) {
                getToken();
                if (!funcFParam()) {
                    error("no funcFParam after comma");
                    break;
                }
            }
            FileStream.output("<FuncFParams>");
            return true;
        }
        return false;
    }

    private boolean funcFParam() {
        if (bType()) {
            String name;
            int dimen = 0;
            int dimen1 = 0;
            int dimen2 = 0;
            if (tag.equals("IDENFR")) {
                name = curToken;
                getToken();
                if (curToken.equals("[")) {
                    getToken();
                    dimen++;
                    dimen1 = 1;
                    if (curToken.equals("]")) {
                        getToken();
                        while (curToken.equals("[")) {
                            dimen++;
                            getToken();
                            dimen2 = Integer.parseInt(constExp());
                            if (curToken.equals("]")) {
                                getToken();
                            } else {
                                error("k");
                            }
                        }
                    } else {
                        error("k");
                    }
                }
                if (!SymbolTable.addPara(name,dimen,0,dimen2)) {
                    error("b");
                }
                MiddleCode.paraDecl(btype,name,dimen1,dimen2);
                FileStream.output("<FuncFParam>");
                return true;
            }
        }
        return false;
    }

    private boolean block() {
        if (curToken.equals("{")) {
            curTable = new SymbolTable(curTable);
            curLayer++;
            getToken();
            while (blockItem());
            if (curToken.equals("}")) {
                if (curReturnType.equals("int") && !hasReturn && curLayer == 1) {
                    error("g");
                }
                getToken();
                curLayer--;
            } else {
                error("lack }");
            }
            curTable = curTable.getFather();
            FileStream.output("<Block>");
            return true;
        }
        return false;
    }

    private boolean blockItem() {
        if (decl()) {
            hasReturn = false;
            return true;
        }
        return stmt();
    }

    private boolean stmt() {
        String exp = null;
        if (lvalAssign()) {
            hasReturn = false;
            String lval = lval();
            if (curTable.isConstVal(curLval)) {
                error("h");
            }
            if (curToken.equals("=")) {
                getToken();
                exp = exp();
                if (exp != null) {
                    if (curToken.equals(";")) {
                        getToken();
                    } else {
                        error("i");
                    }
                    MiddleCode.algorithmOp(lval,exp,null);
                } else if (tag.equals("GETINTTK")) {
                    getToken();
                    if (curToken.equals("(")) {
                        getToken();
                        if (curToken.equals(")")) {
                            getToken();
                        } else {
                            error("j");
                        }
                    }
                    if (curToken.equals(";")) {
                        getToken();
                    } else {
                        error("i");
                    }
                    MiddleCode.getint(lval);
                }
            }
            FileStream.output("<Stmt>");
            return true;
        } else if (exp() != null || curToken.equals(";")) {
            // 没有任何语义用处
            hasReturn = false;
            if (curToken.equals(";")) {
                getToken();
            } else {
                error("i");
            }
            FileStream.output("<Stmt>");
            return true;
        } else if (block()) {
            hasReturn = false;
            FileStream.output("<Stmt>");
            return true;
        } else if (tag.equals("IFTK")) {
            hasReturn = false;
            getToken();
            if (curToken.equals("(")) {
                getToken();
                cond();
                if (curToken.equals(")")) {
                    getToken();
                } else {
                    error("j");
                }
            }
            stmt();
            if (tag.equals("ELSETK")) {
                getToken();
                stmt();
            }
            FileStream.output("<Stmt>");
            return true;
        } else if (tag.equals("WHILETK")) {
            hasReturn = false;
            loopLayer++;
            getToken();
            if (curToken.equals("(")) {
                getToken();
                cond();
                if (curToken.equals(")")) {
                    getToken();
                } else {
                    error("j");
                }
            }
            stmt();
            loopLayer--;
            FileStream.output("<Stmt>");
            return true;
        } else if (tag.equals("BREAKTK") || tag.equals("CONTINUETK")) {
            if (loopLayer == 0) {
                error("m");
            }
            hasReturn = false;
            getToken();
            if (curToken.equals(";")) {
                getToken();
            } else {
                error("j");
            }
            FileStream.output("<Stmt>");
            return true;
        } else if (tag.equals("RETURNTK")) {
            getToken();
            if (curLayer == 1) {
                hasReturn = true;
            }
            exp = exp();
            if (exp != null) {
                if (curReturnType.equals("void")) {
                    error("f");
                }
            }
            if (curToken.equals(";")) {
                getToken();
            } else {
                error("i");
            }
            FileStream.output("<Stmt>");
            MiddleCode.funcReturn(exp);
            return true;
        } else if (tag.equals("PRINTFTK")) {
            hasReturn = false;
            int layer = 0;
            getToken();
            ArrayList<String> vals = new ArrayList<>();
            if (curToken.equals("(")) {
                getToken();
                String[] splitString = lexer.getSplitString();
                if (tag.equals("STRCON")) {
                    getToken();
                    while (curToken.equals(",")) {
                        getToken();
                        vals.add(exp());
                        layer++;
                    }
                }
                if (layer != lexer.getCurFormatPattern()) {
                    error("l");
                }
                if (curToken.equals(")")) {
                    getToken();
                } else {
                    error("j");
                }
                if (curToken.equals(";")) {
                    getToken();
                } else {
                    error("i");
                }
                MiddleCode.prints(splitString,vals);
                FileStream.output("<Stmt>");
                return true;
            }
        }
        return false;
    }

    private String exp() {
        String addExp = addExp();
        if (addExp!= null) {
            FileStream.output("<Exp>");
            return addExp;
        }
        return null;
    }

    private boolean cond() {
        if (lOrExp()) {
            FileStream.output("<Cond>");
            return true;
        }
        return false;
    }

    private boolean lvalAssign() {
        return tag.equals("IDENFR") && lexer.watchAssign().equals("=");
    }

    private String lval() {
        if (tag.equals("IDENFR")) {
            curLval = curToken;
            Val val;
            if (curTable == null) {
                val = new SymbolTable(null).findVal(curLval);
            } else {
                val = curTable.findVal(curLval);
            }
            // get the second dimen
            int coDimen = 0;
            if (val == null) {
                error("c");
            } else {
                coDimen = val.getCoDimen();
                curValDimension = val.getDimension();
            }
            getToken();
            String dimen1 = null;
            String dimen2 = null;
            int i = 1;
            while (curToken.equals("[")) {
                curValDimension--;
                getToken();
                if (i == 1) {
                    dimen1 = exp();
                    i++;
                } else {
                    dimen2 = exp();
                }

                if (curToken.equals("]")) {
                    getToken();
                } else {
                    error("k");
                }
            }
            FileStream.output("<LVal>");
            try {
                if (!val.isConst()) {
                    throw new Exception();
                }
                int i1;
                int i2;
                if (dimen1 == null) {
                    i1 = -1;
                } else {
                    i1 = Integer.parseInt(dimen1);
                }
                if (dimen2 == null) {
                    i2 = -1;
                } else {
                    i2 = Integer.parseInt(dimen2);
                }
                return String.valueOf(val.getValue(i1,i2));
            } catch (Exception e) {
                return MiddleCode.deDimen(curLval,dimen1,dimen2,coDimen);
            }
        }
        return null;
    }

    private String primaryExp() {
        String lval = null;
        String number = null;
        if (curToken.equals("(")) {
            getToken();
            String primaryExp = exp();
            if (curToken.equals(")")) {
                getToken();
            } else {
                error("j");
            }
            FileStream.output("<PrimaryExp>");
            return primaryExp;
        } else if ((lval = lval()) != null) {
            FileStream.output("<PrimaryExp>");
            return lval;
        } else if ((number = number()) != null) {
            FileStream.output("<PrimaryExp>");
            return number;
        }
        return null;
    }

    private String number() {
        if (tag.equals("INTCON")) {
            String num = curToken;
            getToken();
            FileStream.output("<Number>");
            return num;
        }
        return null;
    }

    private String unaryExp() {
        String primaryExp = null;
        String unaryOp = null;
        if (tag.equals("IDENFR") && lexer.watchNext() == '(') {
            // 看是函数还是变量
            String name = curToken;
            getToken();
            if (curToken.equals("(")) {
                getToken();
                funcRParams();
            }
            if (curToken.equals(")")) {
                getToken();
            } else {
                error("j");
            }
            if (curTable.isFuncNameExist(name)) {
                if (curTable.getFuncReturnType(name).equals("int")) {
                    curValDimension = 0;
                } else {
                    curValDimension = 4396;
                }
                if (curTable.funcParaNumCorrect(name)) {
                    if (!curTable.funcParaTypeCorrect(name)) {
                        error("e");
                    }
                } else {
                    error("d");
                }
            } else {
                error("c");
            }
            curTable.clearPartPara();
            FileStream.output("<UnaryExp>");
            MiddleCode.callFunc(name);
            return "@RETURN";
        } else if ((primaryExp = primaryExp()) != null) {
            FileStream.output("<UnaryExp>");
            return primaryExp;
        } else if ((unaryOp = unaryOp()) != null) {
            String unaryExp = unaryExp();
            FileStream.output("<UnaryExp>");
            return MiddleCode.algorithmOp(unaryExp,null,unaryOp);
        }
        return null;
    }

    private String unaryOp() {
        String token = curToken;
        if (curToken.equals("+") || curToken.equals("-") || curToken.equals("!")) {
            getToken();
            FileStream.output("<UnaryOp>");
            return token;
        }
        return null;
    }

    private boolean funcRParams() {
        String exp = exp();
        if (exp != null) {
            curTable.addPartPara(null,curValDimension,0,0);
            curValDimension = 0;
            MiddleCode.rParaDecl(exp);
            while (curToken.equals(",")) {
                getToken();
                exp();
                curTable.addPartPara(null,curValDimension,0,0);
                curValDimension = 0;
            }
            FileStream.output("<FuncRParams>");
            return true;
        }
        return false;
    }

    private String mulExp() {
        String unaryExp = unaryExp();
        if (unaryExp != null) {
            FileStream.output("<MulExp>");
            String token = null;
            String mul = null;
            if (curToken.equals("*") || curToken.equals("/") || curToken.equals("%")) {
                token = curToken;
                getToken();
                mul = mulExp();
            }
            return MiddleCode.algorithmOp(unaryExp,mul,token);
        }
        return null;
    }

    private String addExp() {
        String mulExp = mulExp();
        if (mulExp != null) {
            FileStream.output("<AddExp>");
            String token = null;
            String add = null;
            if (curToken.equals("+") || curToken.equals("-")) {
                token = curToken;
                getToken();
                add = addExp();
            }
            return MiddleCode.algorithmOp(mulExp,add,token);
        }
        return null;
    }

    private boolean relExp() {
        String addExp = addExp();
        //TODO something is not finished
        if (addExp != null) {
            FileStream.output("<RelExp>");
            if (curToken.equals("<") || curToken.equals(">") || curToken.equals("<=") || curToken.equals(">=")) {
                getToken();
                relExp();
            }
            return true;
        }
        return false;
    }

    private boolean eqExp() {
        if (relExp()) {
            FileStream.output("<EqExp>");
            if (curToken.equals("==") || curToken.equals("!=")) {
                getToken();
                eqExp();
            }
            return true;
        }
        return false;
    }

    private boolean lAndExp() {
        if (eqExp()) {
            FileStream.output("<LAndExp>");
            if (curToken.equals("&&")) {
                getToken();
                lAndExp();
            }
            return true;
        }
        return false;
    }

    private boolean lOrExp() {
        if (lAndExp()) {
            FileStream.output("<LOrExp>");
            if (curToken.equals("||")) {
                getToken();
                lOrExp();
            }
            return true;
        }
        return false;
    }

    private String constExp() {
        String addExp = addExp();
        if (addExp != null) {
            FileStream.output("<ConstExp>");
            return addExp;
        }
        return null;
    }

    private void getToken() {
        if (tag != null) {
            FileStream.output(tag + ' ' + curToken);
        }
        while (!lexer.getSymbol()) {
            String s;
            if ((s = FileStream.getNextLine()) != null) {
                lexer = new Lexer(s);
            } else {
                lexer = null;
                return;
            }
        }
        tag = lexer.getTag();
        curToken = lexer.getCurToken();
        formerLine = line;
        line = lexer.getLine();
    }
}
