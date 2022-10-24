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
        while (tag.equals("LBRACK")) {
            getToken();
            constExp();
            if (tag.equals("RBRACK")) {
                getToken();
            } else {
                error("k");
            }
            dimen++;
        }
        if (curTable != null) {
            if (!curTable.addVal(name,true,dimen)) {
                error("b");
            }
        } else {
            if (!SymbolTable.addGlobalVal(name, true, dimen)) {
                error("b");
            }
        }
        if (tag.equals("ASSIGN")) {
            getToken();
        } else {
            error("const val not assgin init value");
        }
        if (constInitVal()) {
            FileStream.output("<ConstDef>");
            return true;
        } else {
            error("const val not assgin init value");
            return false;
        }
    }

    private boolean constInitVal() {
        if (constExp()) {
            FileStream.output("<ConstInitVal>");
            return true;
        } else {
            if (curToken.equals("{")) {
                getToken();
                if (constInitVal()) {
                    while (curToken.equals(",")) {
                        getToken();
                        constInitVal();
                    }
                } else {
                    error("lack }");
                }
                while (constInitVal()) {
                    while (curToken.equals(",")) {
                        getToken();
                        constInitVal();
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
        while (tag.equals("LBRACK")) {
            getToken();
            constExp();
            if (tag.equals("RBRACK")) {
                getToken();
            } else {
                error("k");
            }
            dimen++;
        }
        if (curTable != null) {
            if (!curTable.addVal(name,false,dimen)) {
                error("b");
            }
        } else {
            if (!SymbolTable.addGlobalVal(name, false, dimen)) {
                error("b");
            }
        }
        if (tag.equals("ASSIGN")) {
            getToken();
            if (initVal()) {
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

    private boolean initVal() {
        if (exp()) {
            FileStream.output("<InitVal>");
            return true;
        } else {
            if (curToken.equals("{")) {
                getToken();
                if (initVal()) {
                    while (curToken.equals(",")) {
                        getToken();
                        initVal();
                    }
                } else {
                    error("no init value");
                }
                while (initVal()) {
                    while (curToken.equals(",")) {
                        getToken();
                        initVal();
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
            if (tag.equals("IDENFR")) {
                name = curToken;
                getToken();
                if (curToken.equals("[")) {
                    getToken();
                    dimen++;
                    if (curToken.equals("]")) {
                        getToken();
                        while (curToken.equals("[")) {
                            dimen++;
                            getToken();
                            constExp();
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
                if (!SymbolTable.addPara(name,dimen)) {
                    error("b");
                }
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
        if (lvalAssign()) {
            hasReturn = false;
            lval();
            if (curTable.isConstVal(curLval)) {
                error("h");
            }
            if (curToken.equals("=")) {
                getToken();
                if (exp()) {
                    if (curToken.equals(";")) {
                        getToken();
                    } else {
                        error("i");
                    }
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
                }
            }
            FileStream.output("<Stmt>");
            return true;
        } else if (exp() || curToken.equals(";")) {
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
            if (exp()) {
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
            return true;
        } else if (tag.equals("PRINTFTK")) {
            hasReturn = false;
            int layer = 0;
            getToken();
            if (curToken.equals("(")) {
                getToken();
                if (tag.equals("STRCON")) {
                    getToken();
                    while (curToken.equals(",")) {
                        getToken();
                        exp();
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
                FileStream.output("<Stmt>");
                return true;
            }
        }
        return false;
    }

    private boolean exp() {
        if (addExp()) {
            FileStream.output("<Exp>");
            return true;
        }
        return false;
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

    private boolean lval() {
        if (tag.equals("IDENFR")) {
            curLval = curToken;
            Val val = curTable.findVal(curLval);
            if (val == null) {
                error("c");
            } else {
                curValDimension = val.getDimension();
            }
            getToken();
            while (curToken.equals("[")) {
                curValDimension--;
                getToken();
                exp();
                if (curToken.equals("]")) {
                    getToken();
                } else {
                    error("k");
                }
            }
            FileStream.output("<LVal>");
            return true;
        }
        return false;
    }

    private boolean primaryExp() {
        if (curToken.equals("(")) {
            getToken();
            exp();
            if (curToken.equals(")")) {
                getToken();
            } else {
                error("j");
            }
            FileStream.output("<PrimaryExp>");
            return true;
        } else if (lval() || number()) {
            FileStream.output("<PrimaryExp>");
            return true;
        }
        return false;
    }

    private boolean number() {
        if (tag.equals("INTCON")) {
            getToken();
            FileStream.output("<Number>");
            return true;
        }
        return false;
    }

    private boolean unaryExp() {
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
            return true;
        } else if (primaryExp()) {
            FileStream.output("<UnaryExp>");
            return true;
        } else if (unaryOp()) {
            if (unaryExp()) {
                FileStream.output("<UnaryExp>");
                return true;
            }
        }
        return false;
    }

    private boolean unaryOp() {
        if (curToken.equals("+") || curToken.equals("-") || curToken.equals("!")) {
            getToken();
            FileStream.output("<UnaryOp>");
            return true;
        }
        return false;
    }

    private boolean funcRParams() {
        if (exp()) {
            curTable.addPartPara(null,curValDimension);
            curValDimension = 0;
            while (curToken.equals(",")) {
                getToken();
                exp();
                curTable.addPartPara(null,curValDimension);
                curValDimension = 0;
            }
            FileStream.output("<FuncRParams>");
            return true;
        }
        return false;
    }

    private boolean mulExp() {
        if (unaryExp()) {
            FileStream.output("<MulExp>");
            if (curToken.equals("*") || curToken.equals("/") || curToken.equals("%")) {
                getToken();
                mulExp();
            }

            return true;
        }
        return false;
    }

    private boolean addExp() {
        if (mulExp()) {
            FileStream.output("<AddExp>");
            if (curToken.equals("+") || curToken.equals("-")) {
                getToken();
                addExp();
            }
            return true;
        }
        return false;
    }

    private boolean relExp() {
        if (addExp()) {
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

    private boolean constExp() {
        if (addExp()) {
            FileStream.output("<ConstExp>");
            return true;
        }
        return false;
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
