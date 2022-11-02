import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private int num;
    private String tag;
    private int length;
    private int pos;
    private static int line = 0; // 行号
    private String string;
    private String curToken;

    private String pureString;
    private int curFormatPattern;
    private String[] splitString;

    private boolean inString; // 字符串中
    private boolean lineNote; // 小注释
    private static boolean allNote = false; // 大注释

    private static final String Ident = "^[a-zA-Z_][\\w]*";
    private static final Pattern patternIdent = Pattern.compile(Ident);
    private static final String IntConst = "^(?<num>([1-9][0-9]*)|(0))[^0-9]?";
    private static final Pattern patternIntConst = Pattern.compile(IntConst);
    private static final String FormatString = "^\"(?<string>.*?)\"";
    private static final Pattern patternFormatString = Pattern.compile(FormatString);
    private static final String InvalidString = "[^\\x20-\\x21\\x25\\x28-\\x7E]|(\\\\[^n])|(\\\\$)|(%[^d])|(%$)";
    private static final Pattern patternInvalidString = Pattern.compile(InvalidString);
    private static final String FormatChar = "%d";
    private static final Pattern patternFormatChar = Pattern.compile(FormatChar);

    public Lexer(String s) {
        string = s;
        line++;
        num = 0;
        tag = null;
        length = s.length();
        curToken = null;
        pos = 0;
        inString = false;
        lineNote = false;
        curFormatPattern = 0;
        pureString = null;
    }

    private boolean isJudgeSign() {
        switch (string.charAt(pos)) {
            case '<':
                if (pos + 1 != length && string.charAt(pos + 1) == '=') {
                    curToken = "<=";
                    tag = "LEQ";
                    pos += 2;
                } else {
                    curToken = "<";
                    tag = "LSS";
                    pos++;
                }
                return true;
            case '>':
                if (pos + 1 != length && string.charAt(pos + 1) == '=') {
                    curToken = ">=";
                    tag = "GEQ";
                    pos += 2;
                } else {
                    curToken = ">";
                    tag = "GRE";
                    pos++;
                }
                return true;
            case '!':
                if (pos + 1 != length && string.charAt(pos + 1) == '=') {
                    curToken = "!=";
                    tag = "NEQ";
                    pos += 2;
                } else {
                    curToken = "!";
                    tag = "NOT";
                    pos++;
                }
                return true;
            case '=':
                if (pos + 1 != length && string.charAt(pos + 1) == '=') {
                    curToken = "==";
                    tag = "EQL";
                    pos += 2;
                } else {
                    curToken = "=";
                    tag = "ASSIGN";
                    pos++;
                }
                return true;
            default:
                return false;
        }
    }

    private boolean isLogicSign() {
        if (string.charAt(pos) == '|' && (pos != length && string.charAt(pos + 1) == '|')) {
            curToken = "||";
            tag = "OR";
            pos += 2;
            return true;
        } else if (string.charAt(pos) == '&' && (pos != length && string.charAt(pos + 1) == '&')) {
            curToken = "&&";
            tag = "AND";
            pos += 2;
            return true;
        }
        return false;
    }

    private boolean isBracket() {
        // 括号和分号逗号
        switch (string.charAt(pos)) {
            case '(':
                curToken = "(";
                tag = "LPARENT";
                pos++;
                return true;
            case ')':
                curToken = ")";
                tag = "RPARENT";
                pos++;
                return true;
            case '[':
                curToken = "[";
                tag = "LBRACK";
                pos++;
                return true;
            case ']':
                curToken = "]";
                tag = "RBRACK";
                pos++;
                return true;
            case '{':
                curToken = "{";
                tag = "LBRACE";
                pos++;
                return true;
            case '}':
                curToken = "}";
                tag = "RBRACE";
                pos++;
                return true;
            case ',':
                curToken = ",";
                tag = "COMMA";
                pos++;
                return true;
            case ';':
                curToken = ";";
                tag = "SEMICN";
                pos++;
                return true;
            default:
                return false;
        }
    }

    private boolean isCalculateSign() {
        switch (string.charAt(pos)) {
            case '+':
                curToken = "+";
                tag = "PLUS";
                pos++;
                return true;
            case '-':
                curToken = "-";
                tag = "MINU";
                pos++;
                return true;
            case '%':
                curToken = "%";
                tag = "MOD";
                pos++;
                return true;
            case '/' :
                if (pos + 1 != length && string.charAt(pos + 1) == '/') {
                    lineNote = true;
                    pos = length;
                    tag = "noteOn";
                } else if (pos + 1 != length && string.charAt(pos + 1) == '*') {
                    allNote = true;
                    pos += 2;
                    tag = "noteOn";
                } else {
                    curToken = "/";
                    tag = "DIV";
                    pos++;
                }
                return true;
            case '*':
                if (pos + 1 != length && string.charAt(pos + 1) == '/') {
                    allNote = false;
                    pos += 2;
                    tag = "noteOff";
                } else {
                    curToken = "*";
                    tag = "MULT";
                    pos++;
                }
                return true;
            default:
                return false;
        }
    }

    public boolean readNext() throws Exception {
        if (pos == length)
            return false;
        while (string.charAt(pos) == ' ' || string.charAt(pos) == '\n' || string.charAt(pos) == '\r' || string.charAt(pos) == '\t') {
            // 去除空格
            pos++;
            if (pos == length)
                return false;
        }
        if (allNote) {
            if (pos + 1 == length) {
                return false;
            }
            while (string.charAt(pos) != '*' || string.charAt(pos + 1) != '/') {
                pos++;
                if (pos + 1 == length) {
                    return false;
                }
            }
        }
        if (pos == length)
            return false;
        String curStr = string.substring(pos);

        Matcher matcherIdent = patternIdent.matcher(curStr);
        Matcher matcherConstInt = patternIntConst.matcher(curStr);
        Matcher matcherFormatString = patternFormatString.matcher(curStr);


        if (matcherIdent.find()) {
            curToken = matcherIdent.group();
            tag = Keyword.searchKeyWord(curToken);
            if (tag == null) {
                tag = "IDENFR";
            }
            pos += curToken.length();
            return true;
        } else if (matcherConstInt.find()) {
            curToken = matcherConstInt.group("num");
            num = Parser.str2int(curToken);
            tag = "INTCON";
            pos += curToken.length();
            return true;
        } else if (matcherFormatString.find()) {
            curToken = matcherFormatString.group();
            pureString = matcherFormatString.group("string");
            String sp = pureString.replaceAll("%d","#%d#");
            splitString = sp.split("#");
            tag = "STRCON";
            pos += curToken.length();
            isValid();
            analyzeFormatCharNum();
        } else if (isBracket() || isCalculateSign() || isJudgeSign() || isLogicSign()) {
            return true;
        } else {
            throw new Exception();
        }
        return true;
    }

    private void isValid() {
        Matcher invalidString = patternInvalidString.matcher(pureString);
        if (invalidString.find()) {
            FileStream.error(line + " a");
        }
    }

    private void analyzeFormatCharNum() {
        Matcher matcherFormatChar = patternFormatChar.matcher(pureString);
        curFormatPattern = 0;
        while (matcherFormatChar.find()) {
            curFormatPattern++;
        }
    }

    public int getCurFormatPattern() {
        return curFormatPattern;
    }

    public String[] getSplitString() {
        return splitString;
    }

    public boolean getSymbol() {
        try {
            if (!readNext()) {
                return false;
            }
            if (tag.equals("noteOn") || tag.equals("noteOff")) {
                return getSymbol();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public char watchNext() {
        if (pos + 1 >= length) {
            return '\0';
        } else {
            return string.charAt(pos);
        }
    }

    public String watchAssign() {
        int pos_n = pos;
        String tag_n = tag;
        String curToken_n = curToken;
        while (true) {
            try {
                if (!readNext()) break;
            } catch (Exception e) {
                e.printStackTrace();
            }
            switch (curToken) {
                case "=":
                    pos = pos_n;
                    tag = tag_n;
                    curToken = curToken_n;
                    return "=";
                case ";":
                    pos = pos_n;
                    tag = tag_n;
                    curToken = curToken_n;
                    return ";";
            }
        }
        pos = pos_n;
        tag = tag_n;
        curToken = curToken_n;
        return "";
    }

    public String watchBracket() {
        int pos_n = pos;
        String tag_n = tag;
        String curToken_n = curToken;
        while (true) {
            try {
                if (!readNext()) break;
            } catch (Exception e) {
                e.printStackTrace();
            }
            switch (curToken) {
                case "=":
                    pos = pos_n;
                    tag = tag_n;
                    curToken = curToken_n;
                    return "=";
                case ";":
                    pos = pos_n;
                    tag = tag_n;
                    curToken = curToken_n;
                    return ";";
                case "(":
                    pos = pos_n;
                    tag = tag_n;
                    curToken = curToken_n;
                    return "(";
                case "[":
                    pos = pos_n;
                    tag = tag_n;
                    curToken = curToken_n;
                    return "[";
            }
        }
        pos = pos_n;
        tag = tag_n;
        curToken = curToken_n;
        return "";
    }
    public String getTag() {
        return tag;
    }

    public String getCurToken() {
        return curToken;
    }

    public int getLine() {
        return line;
    }
}
