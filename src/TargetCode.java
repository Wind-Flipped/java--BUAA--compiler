import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetCode {
    private static ArrayList<Val> globalVals = new ArrayList<>();
    private static HashMap<String, String> operations = new HashMap<>();
    private static ArrayList<String> middleCodes = new ArrayList<>();
    // 当前符号表
    private static FuncSymbolTable funcSymbolTable = new FuncSymbolTable(null);
    // 处理到了第几行，第一行序号为 0
    private static int line = 0;
    // 块层数，全局变量处在第 0 层
    private static int layer = 0;
    // 实参数目
    private static int rPara = 0;
    // k register
    private static int kReg = 0;
    // s register
    private static int sReg = 0;
    // func layer
    private static int funcLayer = 0;

    private static final Pattern arrayPattern = Pattern.compile("\\[(?<array>.*?)]$");

    public static void generateMips() {
        operations.put("+", "add");
        operations.put("-", "sub");
        operations.put("*", "mul");
        operations.put("/", "div");
        operations.put("%", "yu");
        operations.put(">", "sgt");
        operations.put(">=", "sge");
        operations.put("<", "slt");
        operations.put("<=", "sle");
        operations.put("==", "seq");
        operations.put("!=", "sne");
        operations.put("!", "seq");
        beginData();
        beginText();
        convertContent();
    }

    private static int getGlobalAddr(String name) {
        for (Val val : globalVals) {
            if (val.getName().equals(name)) {
                return val.getAddress();
            }
        }
        return 0;
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            System.out.println("The error is in line " + line);
            return false;
        }
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private static String findSymbolsLoad(String name) {
        // 把 name 标识符代表的值取出
        if (name.equals("@RETURN")) {
            // return value
            return "$v0";
        }
        if (isInteger(name)) {
            String kr = "$k" + kReg;
            FileStream.mipsOutput("li $k" + kReg + "," + Parser.str2int(name));
            kReg = 1 - kReg;
            return kr;
        }
        String sRegister = "$s" + sReg;
        sReg = 1 - sReg;
        String[] pure = name.split("[\\[\\]]");
        if (pure[0].contains("<global>")) {
            // s<global>[4]
            String[] pureName = name.split("[<>\\[\\]]");
            int addr = getGlobalAddr(pureName[0]);
            if (name.contains("[")) {
                // 含数组形式
                Matcher matcher = arrayPattern.matcher(name);
                if (matcher.find()) {
                    // 数组嵌套
                    String curName = matcher.group("array");
                    if (curName.contains("[")) {
                        String rs = findSymbolsLoad(curName);
                        FileStream.mipsOutput("sll $t8," + rs + ",2");
                        FileStream.mipsOutput("add $t8,$t8,$gp");
                        FileStream.mipsOutput("lw " + sRegister + "," + addr + "($t8)");
                        return sRegister;
                    }
                    if (curName.contains("<global>")) {
                        String st = curName.split("<")[0];
                        int raddr = getGlobalAddr(st);
                        FileStream.mipsOutput("lw $t8," + raddr + "($gp)");
                        FileStream.mipsOutput("sll $t8,$t8,2");
                        FileStream.mipsOutput("add $t8,$t8,$gp");
                        FileStream.mipsOutput("lw " + sRegister + "," + addr + "($t8)");
                        return sRegister;
                    }
                    if (isInteger(pureName[3])) {
                        int x = 4 * Integer.parseInt(pureName[3]) + addr;
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($gp)");
                    } else {
                        // s<global>[@t1]
                        if (pureName[3].charAt(0) == '@') {
                            // str[@t1]
                            FileStream.mipsOutput("sll $t8," + funcSymbolTable.findTemSymbol(pureName[3]) + ",2");
                        } else {
                            // str[i]
                            String rs = funcSymbolTable.findVar(pureName[3], layer);
                            if (isInteger(rs)) {
                                // 4
                                FileStream.mipsOutput("lw $t8," + rs + "($fp)");
                                FileStream.mipsOutput("sll $t8,$t8,2");
                            } else {
                                // $a0
                                int x = Integer.parseInt(rs.substring(2));
                                if (funcLayer != 0) {
                                    FileStream.mipsOutput("lw $t8," + (4 * x) + "($s7)");
                                    FileStream.mipsOutput("sll $t8,$t8,2");
                                } else {
                                    FileStream.mipsOutput("sll $t8," + rs + ",2");
                                }
                            }
                        }
                        FileStream.mipsOutput("add $t8,$t8,$gp");
                        FileStream.mipsOutput("lw " + sRegister + "," + addr + "($t8)");
                    }
                }
            } else {
                // 不含数组形式
                FileStream.mipsOutput("lw " + sRegister + "," + addr + "($gp)");
            }
            return sRegister;
        }
        String[] pureName = name.split("[\\[\\]]");
        if (name.charAt(0) != '@') {
            String res = funcSymbolTable.findVar(pureName[0], layer);
            if (res.charAt(0) == '#') {
                // address para
                Matcher matcher = arrayPattern.matcher(name);
                if (matcher.find()) {
                    String curName = matcher.group("array");
                    return findAddrParaLoad(res, curName, sRegister);
                }
            }
            if (isInteger(res)) {
                int x = Integer.parseInt(res);
                Matcher matcher = arrayPattern.matcher(name);
                if (matcher.find()) {
                    // 数组嵌套
                    String curName = matcher.group("array");
                    if (curName.contains("[")) {
                        String rs = findSymbolsLoad(curName);
                        FileStream.mipsOutput("sll $t8," + rs + ",2");
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($t8)");
                        return sRegister;
                    }
                    if (curName.contains("<global>")) {
                        String st = curName.split("<")[0];
                        int raddr = getGlobalAddr(st);
                        FileStream.mipsOutput("lw $t8," + raddr + "($gp)");
                        FileStream.mipsOutput("sll $t8,$t8,2");
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($t8)");
                        return sRegister;
                    }
                }
                if (pureName.length > 1) {
                    if (isInteger(pureName[1])) {
                        // str[3]
                        x = Integer.parseInt(pureName[1]) * 4 + Integer.parseInt(res);
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($fp)");
                    } else {
                        if (pureName[1].charAt(0) == '@') {
                            // str[@t1]
                            FileStream.mipsOutput("sll $t8," + funcSymbolTable.findTemSymbol(pureName[1]) + ",2");
                        } else {
                            // str[i]
                            String rs = funcSymbolTable.findVar(pureName[1], layer);
                            if (isInteger(rs)) {
                                // 4
                                FileStream.mipsOutput("lw $t8," + rs + "($fp)");
                                FileStream.mipsOutput("sll $t8,$t8,2");
                            } else {
                                // $a0
                                int y = Integer.parseInt(rs.substring(2));
                                if (funcLayer != 0) {
                                    FileStream.mipsOutput("lw $t8," + (4 * y) + "($s7)");
                                    FileStream.mipsOutput("sll $t8,$t8,2");
                                } else {
                                    FileStream.mipsOutput("sll $t8," + rs + ",2");
                                }
                            }
                        }
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($t8)");
                    }
                } else {
                    // 4 (bias)
                    x = Parser.str2int(res);
                    FileStream.mipsOutput("lw " + sRegister + "," + x + "($fp)");
                }
                return sRegister;
            } else {
                // $a0
                if (funcLayer == 0) {
                    // 当前不处于函数调用
                    sReg = 1 - sReg;
                    return res;
                } else {
                    int x = Integer.parseInt(res.substring(2));
                    FileStream.mipsOutput("lw " + sRegister + "," + (4 * x) + "($s7)");
                    return sRegister;
                }
            }
        } else {
            sReg = 1 - sReg;
            String rd = funcSymbolTable.findTemSymbol(name);
            if (rd == null) {
                rd = funcSymbolTable.addTemSymbol(name);
            }
            return rd;
        }
    }

    private static String findAddrParaLoad(String res, String index, String sRegister) {
        // res : #$a0 ,index = 1,i,@t1,i<global>,a[xxxx],$a0
        String reg = res.substring(1);
        if (isInteger(reg)) {
            // reg = 4 , 保存在栈中的函数参数不需要
            FileStream.mipsOutput("lw $t9," + reg + "($fp)");
            reg = "$t9";
        } else if (funcLayer != 0) {
            // 开始调用函数
            reg = reg.substring(2);
            FileStream.mipsOutput("lw $t7," + (4 * Integer.parseInt(reg)) + "($s7)");
            reg = "$t7";
        }
        // reg = $a0
        if (isInteger(index)) {
            FileStream.mipsOutput("lw " + sRegister + "," + (4 * Integer.parseInt(index)) + "(" + reg + ")");
        } else if (index.charAt(0) == '@') {
            String tem = funcSymbolTable.findTemSymbol(index);
            FileStream.mipsOutput("sll $t8," + tem + ",2");
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("lw " + sRegister + ",0($t8)");
        } else if (index.contains("[")) {
            String rs = findSymbolsLoad(index);
            FileStream.mipsOutput("sll $t8," + rs + ",2");
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("lw " + sRegister + ",0($t8)");
        } else if (index.contains("<global>")) {
            String[] pureName = index.split("[<>]");
            int addr = getGlobalAddr(pureName[0]);
            FileStream.mipsOutput("lw $t8," + addr + "($gp)");
            FileStream.mipsOutput("sll $t8,$t8,2");
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("lw " + sRegister + ",0($t8)");
        } else {
            // i
            String re = funcSymbolTable.findVar(index, layer);
            if (isInteger(re)) {
                // 4
                int x = Integer.parseInt(re);
                FileStream.mipsOutput("lw $t8," + x + "($fp)");
                FileStream.mipsOutput("sll $t8,$t8,2");
            } else {
                // $a0
                FileStream.mipsOutput("sll $t8," + re + ",2");
            }
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("lw " + sRegister + ",0($t8)");

        }
        return sRegister;
    }

    private static void findSymbolsStore(String name, String regStore) {
        // 把 regStore 寄存器值存入 name 标识符内
        String[] pure = name.split("[\\[\\]]");
        if (pure[0].contains("<global>")) {
            // s<global>[4]
            String[] pureName = name.split("[<>\\[\\]]");
            int addr = getGlobalAddr(pureName[0]);
            if (name.contains("[")) {
                // 含数组形式
                Matcher matcher = arrayPattern.matcher(name);
                if (matcher.find()) {
                    // 数组嵌套,数组内部内容
                    String curName = matcher.group("array");
                    if (curName.contains("[")) {
                        String rs = findSymbolsLoad(curName);
                        FileStream.mipsOutput("sll $t8," + rs + ",2");
                        FileStream.mipsOutput("add $t8,$t8,$gp");
                        FileStream.mipsOutput("sw " + regStore + "," + addr + "($t8)");
                        return;
                    }
                    if (curName.contains("<global>")) {
                        String st = curName.split("<")[0];
                        int raddr = getGlobalAddr(st);
                        FileStream.mipsOutput("lw $t8," + raddr + "($gp)");
                        FileStream.mipsOutput("sll $t8,$t8,2");
                        FileStream.mipsOutput("add $t8,$t8,$gp");
                        FileStream.mipsOutput("sw " + regStore + "," + addr + "($t8)");
                        return;
                    }
                    if (isInteger(pureName[3])) {
                        // s<global>[2]
                        int x = 4 * Integer.parseInt(pureName[3]) + addr;
                        FileStream.mipsOutput("sw " + regStore + "," + x + "($gp)");
                    } else {
                        // s<global>[@t1]
                        if (pureName[3].charAt(0) == '@') {
                            // s<global>[@t1]
                            FileStream.mipsOutput("sll $t8," + funcSymbolTable.findTemSymbol(pureName[3]) + ",2");
                        } else {
                            // s<global>[i]
                            String rs = funcSymbolTable.findVar(pureName[3], layer);
                            if (isInteger(rs)) {
                                // 4
                                FileStream.mipsOutput("lw $t8," + rs + "($fp)");
                                FileStream.mipsOutput("sll $t8,$t8,2");
                            } else {
                                // $a0
                                int y = Integer.parseInt(rs.substring(2));
                                if (funcLayer != 0) {
                                    FileStream.mipsOutput("lw $t8," + (4 * y) + "($s7)");
                                    FileStream.mipsOutput("sll $t8,$t8,2");
                                } else {
                                    FileStream.mipsOutput("sll $t8," + rs + ",2");
                                }
                            }
                        }
                        FileStream.mipsOutput("add $t8,$t8,$gp");
                        FileStream.mipsOutput("sw " + regStore + "," + addr + "($t8)");
                    }
                }
            } else {
                // 不含数组形式
                FileStream.mipsOutput("sw " + regStore + "," + addr + "($gp)");
            }
            return;
        }
        String[] pureName = name.split("[\\[\\]]");
        if (name.charAt(0) != '@') {
            String res = funcSymbolTable.findVar(pureName[0], layer);
            if (res.charAt(0) == '#') {
                // address para
                Matcher matcher = arrayPattern.matcher(name);
                if (matcher.find()) {
                    String curName = matcher.group("array");
                    findAddrParaStore(res, curName, regStore);
                }
                return;
            }
            if (isInteger(res)) {
                int x = Integer.parseInt(res);
                Matcher matcher = arrayPattern.matcher(name);
                if (matcher.find()) {
                    // 数组嵌套
                    String curName = matcher.group("array");
                    if (curName.contains("[")) {
                        String rs = findSymbolsLoad(curName);
                        FileStream.mipsOutput("sll $t8," + rs + ",2");
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("sw " + regStore + "," + x + "($t8)");
                        return;
                    }
                    if (curName.contains("<global>")) {
                        String st = curName.split("<")[0];
                        int raddr = getGlobalAddr(st);
                        FileStream.mipsOutput("lw $t8," + raddr + "($gp)");
                        FileStream.mipsOutput("sll $t8,$t8,2");
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("sw " + regStore + "," + x + "($t8)");
                        return;
                    }
                }
                if (pureName.length > 1) {
                    // str[3]
                    if (isInteger(pureName[1])) {
                        x = Integer.parseInt(pureName[1]) * 4 + Integer.parseInt(res);
                        FileStream.mipsOutput("sw " + regStore + "," + x + "($fp)");
                    } else {
                        if (pureName[1].charAt(0) == '@') {
                            // str[@t1]
                            FileStream.mipsOutput("sll $t8," + funcSymbolTable.findTemSymbol(pureName[1]) + ",2");
                        } else {
                            // str[i]
                            String rs = funcSymbolTable.findVar(pureName[1], layer);
                            if (isInteger(rs)) {
                                // 4
                                FileStream.mipsOutput("lw $t8," + rs + "($fp)");
                                FileStream.mipsOutput("sll $t8,$t8,2");
                            } else {
                                // $a0
                                int y = Integer.parseInt(rs.substring(2));
                                if (funcLayer != 0) {
                                    FileStream.mipsOutput("lw $t8," + (4 * y) + "($s7)");
                                    FileStream.mipsOutput("sll $t8,$t8,2");
                                } else {
                                    FileStream.mipsOutput("sll $t8," + rs + ",2");
                                }
                            }
                        }
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("sw " + regStore + "," + x + "($t8)");
                    }
                } else {
                    // 4 (bias)
                    FileStream.mipsOutput("sw " + regStore + "," + x + "($fp)");
                }
            } else {
                // $a0
                FileStream.mipsOutput("move " + res + "," + regStore);
            }
        } else {
            // @t1
            String rd = funcSymbolTable.findTemSymbol(name);
            if (rd == null) {
                rd = funcSymbolTable.addTemSymbol(name);
            }
            FileStream.mipsOutput("move " + rd + "," + regStore);
            if (rd.equals("$t6")) {
                FileStream.mipsOutput("sw $t6," + funcSymbolTable.getTemNum(name) + "($fp)");
            }
        }
    }

    private static void findAddrParaStore(String res, String index, String regStore) {
        // res : #$a0 ,index = 1,i,@t1,i<global>,a[xxxx]
        String reg = res.substring(1);
        if (isInteger(reg)) {
            // reg = 4
            FileStream.mipsOutput("lw $t9," + reg + "($fp)");
            reg = "$t9";
        }
        // reg = $a0
        if (isInteger(index)) {
            FileStream.mipsOutput("sw " + regStore + "," + (4 * Integer.parseInt(index)) + "(" + reg + ")");
        } else if (index.charAt(0) == '@') {
            String tem = funcSymbolTable.findTemSymbol(index);
            FileStream.mipsOutput("sll $t8," + tem + ",2");
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("sw " + regStore + ",0($t8)");
        } else if (index.contains("[")) {
            String rs = findSymbolsLoad(index);
            FileStream.mipsOutput("sll $t8," + rs + ",2");
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("sw " + regStore + ",0($t8)");
        } else if (index.contains("<global>")) {
            String[] pureName = index.split("[<>]");
            int addr = getGlobalAddr(pureName[0]);
            FileStream.mipsOutput("lw $t8," + addr + "($gp)");
            FileStream.mipsOutput("sll $t8,$t8,2");
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("sw " + regStore + ",0($t8)");
        } else {
            // i
            String re = funcSymbolTable.findVar(index, layer);
            if (isInteger(re)) {
                // 4
                int x = Integer.parseInt(re);
                FileStream.mipsOutput("lw $t8," + x + "($fp)");
                FileStream.mipsOutput("sll $t8,$t8,2");
            } else {
                // $a0
                FileStream.mipsOutput("sll $t8," + re + ",2");
            }
            FileStream.mipsOutput("add $t8,$t8," + reg);
            FileStream.mipsOutput("sw " + regStore + ",0($t8)");
        }
        return;
    }

    private static void beginData() {
        FileStream.mipsOutput(".data");
        globalVals = new ArrayList<>(SymbolTable.getGlobalVals().values());
        int addr = 0;
        for (Val globalVal : globalVals) {
            FileStream.mipsOutput(globalVal.getName() + ": .space " + globalVal.getSize());
            globalVal.setAddress(addr);
            addr += globalVal.getSize();
        }
        HashMap<String, String> strs = MiddleCode.getStrings();
        for (Map.Entry<String, String> entry : strs.entrySet()) {
            FileStream.mipsOutput(entry.getKey() + ": .asciiz \"" + entry.getValue() + "\"");
        }
    }

    private static void beginText() {
        FileStream.mipsOutput(".text");
        FileStream.mipsOutput("# set global variable pointer");
        FileStream.mipsOutput("li $gp,0x10010000");
        FileStream.mipsOutput("li $fp,0x10040000");
        FileStream.mipsOutput("li $s7,0x10000000");
    }

    private static void convertContent() {
        middleCodes = FileStream.getMiddleCodes();
        while (true) {
            String[] strings = middleCodes.get(line).split("\\s");
            line++;
            if (strings[0].equals("#main_end")) {
                break;
            }
            if (strings[0].charAt(0) == '#') {
                specialCmd(strings);
            } else {
                commonCmd(strings);
            }
        }
    }

    private static void specialCmd(String[] strings) {
        String cmd = strings[0];
        switch (cmd) {
            case "#main_begin":
                mainBegin();
                break;
            case "#func_begin":
                funcBegin(strings);
                break;
            case "#func_end":
                funcEnd();
                break;
            case "#para":
                para(strings);
                break;
            case "#var":
                var(strings);
                break;
            case "#block_begin":
                layer++;
                break;
            case "#block_end":
                funcSymbolTable.deleteVars(layer);
                layer--;
                break;
            case "#read":
                read(strings[2]);
                break;
            case "#printString":
            case "#printNum":
                printf(strings);
                break;
            case "#call_pre":
                callPre();
                break;
            case "#call":
                call(strings[1]);
                break;
            case "#push":
                push(strings);
                break;
            case "#return":
                ret(strings);
                break;
            case "#global_end":
                globalEnd();
                break;
            case "#setLabel":
                setLabel(strings[1]);
                break;
            case "#branch":
                branch(strings);
                break;
            case "#jump":
                jump(strings[1]);
                break;
            default:
                FileStream.mipsOutput("ERROR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    private static void commonCmd(String[] strings) {
        if (strings.length == 3) {
            // t = @t1  t = 4   t = i
            if (strings[2].equals("@RETURN")) {
                findSymbolsStore(strings[0], "$v0");
            } else {
                String rs = findSymbolsLoad(strings[2]);
                findSymbolsStore(strings[0], rs);
            }
        } else if (strings.length == 4) {
            // rd = - rt
            opration(strings[0], "$zero", strings[3], strings[2]);
            /*
            String rs = findSymbolsLoad(strings[3]);

            if (strings[2].equals("-")) {
                if (strings[0].contains("[") && !strings[0].contains("@")) {
                    FileStream.mipsOutput("sub $s2,$zero," + rs);
                    findSymbolsStore(strings[0], "$s2");
                } else {
                    String rd = findSymbolsLoad(strings[0]);
                    FileStream.mipsOutput("sub " + findSymbolsLoad(strings[0]) + ",$zero," + rs);
                }
            } else {
                if (strings[0].contains("[") && !strings[0].contains("@")) {
                    FileStream.mipsOutput("add $s2,$zero," + rs);
                    findSymbolsStore(strings[0], "$s2");
                } else {
                    String rd = findSymbolsLoad(strings[0]);
                    FileStream.mipsOutput("add " + findSymbolsLoad(strings[0]) + ",$zero," + rs);
                }
            }
            */

        } else if (strings.length == 5) {
            // rd = rs + rt
            opration(strings[0], strings[2], strings[4], strings[3]);
        } else {
            System.out.println("ERROR in strings.length!!!!!!!!!!!!!!!!");
        }
    }

    private static void opration(String name1, String name2, String name3, String op) {
        String calculate = operations.get(op);
        String rs = name2;
        if (!name2.equals("$zero")) {
            // 单目运算符
            rs = findSymbolsLoad(name2);
        }
        String rt = findSymbolsLoad(name3);
        if (name1.contains("[") && !name1.contains("@")) {
            if (!calculate.equals("yu")) {
                FileStream.mipsOutput(calculate + " $s2," + rs + "," + rt);
            } else {
                FileStream.mipsOutput("div " + rs + "," + rt);
                FileStream.mipsOutput("mfhi $s2");
            }
            findSymbolsStore(name1, "$s2");
        } else {
            String rd = findSymbolsLoad(name1);
            if (!calculate.equals("yu")) {
                FileStream.mipsOutput(calculate + " " + rd + "," + rs + "," + rt);
                if (rd.equals("$t6") || rd.equals("$t7")) {
                    FileStream.mipsOutput("sw " + rd + "," + funcSymbolTable.getTemNum(name1) + "($fp)");
                }
            } else {
                FileStream.mipsOutput("div " + rs + "," + rt);
                FileStream.mipsOutput("mfhi " + rd);
                if (rd.equals("$t6") || rd.equals("$t7")) {
                    FileStream.mipsOutput("sw " + rd + "," + funcSymbolTable.getTemNum(name1) + "($fp)");
                }
            }
        }
    }

    private static void mainBegin() {
        funcSymbolTable = new FuncSymbolTable(funcSymbolTable);
        FileStream.mipsOutput("main:");
    }

    private static void funcBegin(String[] strings) {
        funcSymbolTable = new FuncSymbolTable(funcSymbolTable);
        FileStream.mipsOutput(strings[2] + ":");
        FileStream.mipsOutput("sw $a0,0($s7)");
        FileStream.mipsOutput("sw $a1,4($s7)");
        FileStream.mipsOutput("sw $a2,8($s7)");
        FileStream.mipsOutput("sw $a3,12($s7)");

    }

    private static void funcEnd() {
        funcSymbolTable = funcSymbolTable.getPrev();
        FileStream.mipsOutput("jr $ra");
    }

    private static void para(String[] strings) {
        if (strings.length > 3) {
            // get address
            funcSymbolTable.addParas(strings[2], "#");
        } else {
            funcSymbolTable.addParas(strings[2], "");
        }

    }

    private static void var(String[] strings) {
        if (layer == 0) {
            return;
        }
        int length = strings.length;
        if (length == 3) {
            funcSymbolTable.addVars(strings[2], 4, layer);
        } else if (length == 4) {
            funcSymbolTable.addVars(strings[2], 4 * Integer.parseInt(strings[3].substring(1, strings[3].length() - 1)), layer);
        } else {
            funcSymbolTable.addVars(strings[2], 4 * Integer.parseInt(strings[3].substring(1, strings[3].length() - 1)) * Integer.parseInt(strings[4].substring(1, strings[4].length() - 1)), layer);
        }
    }

    private static void read(String name) {
        FileStream.mipsOutput("li $v0,5");
        FileStream.mipsOutput("syscall");
        findSymbolsStore(name, "$v0");
    }

    private static void printf(String[] strings) {
        if (strings[0].equals("#printString")) {
            FileStream.mipsOutput("sw $a0,-4($sp)");
            FileStream.mipsOutput("la $a0," + strings[1]);
            FileStream.mipsOutput("li $v0,4");
            FileStream.mipsOutput("syscall");
            FileStream.mipsOutput("lw $a0,-4($sp)");
        } else {
            FileStream.mipsOutput("li $v0,1");
            String a0 = findSymbolsLoad(strings[1]);
            if (!a0.equals("$a0")) {
                FileStream.mipsOutput("sw $a0,-4($sp)");
                FileStream.mipsOutput("move $a0," + a0);
            }
            FileStream.mipsOutput("syscall");
            if (!a0.equals("$a0")) {
                FileStream.mipsOutput("lw $a0,-4($sp)");
            }
        }
    }

    private static void callPre() {
        funcLayer++;
        FileStream.mipsOutput("sw $a0,-4($sp)");
        FileStream.mipsOutput("sw $a1,-8($sp)");
        FileStream.mipsOutput("sw $a2,-12($sp)");
        FileStream.mipsOutput("sw $a3,-16($sp)");
        FileStream.mipsOutput("addi $sp,$sp,-16");
    }

    private static void push(String[] strings) {
        //TODO 区分地址和值传递
        String rparaName = strings[1];
        String rs;
        rPara = Parser.str2int(strings[strings.length - 1]);
        if (strings.length == 5) {
            // addr : #push ss [0] <addr> 2
            // get [??]
            String rt = strings[2].substring(1, strings[2].length() - 1);
            rt = findSymbolsLoad(rt);
            FileStream.mipsOutput("sll $t8," + rt + ",2");
            if (strings[1].contains("<global>")) {
                String globalV = strings[1].split("<")[0];
                int addr = getGlobalAddr(globalV);
                FileStream.mipsOutput("add $t8,$t8,$gp");
                FileStream.mipsOutput("addi $t8,$t8," + addr);
            } else {
                String rp = funcSymbolTable.findVar(strings[1], layer);
                if (isInteger(rp)) {
                    // 4
                    FileStream.mipsOutput("addi $t9,$fp," + rp);
                    FileStream.mipsOutput("add $t8,$t8,$t9");
                } else {
                    // #$a0
                    int num = Integer.parseInt(rp.substring(3));
                    FileStream.mipsOutput("lw $t5," + (num * 4) + "($s7)");
                    FileStream.mipsOutput("add $t8,$t8,$t5");
                }
            }
            rs = "$t8";
        } else {
            // 非地址
            rs = findSymbolsLoad(rparaName);
            if (rs.contains("$a")) {
                // 参数寄存器
                int num = Integer.parseInt(rs.substring(2));
                FileStream.mipsOutput("lw $t5," + (num * 4) + "($s7)");
                rs = "$t5";
            }
        }
        if (rPara <= 3) {
            FileStream.mipsOutput("move $a" + rPara + "," + rs);
        } else {
            FileStream.mipsOutput("sw " + rs + "," + (funcSymbolTable.getAddr() + (rPara - 4) * 4) + "($fp)");
        }
        rPara += 1;
    }

    private static void call(String funcName) {
        rPara = 0;
        funcLayer--;
        FileStream.mipsOutput("addi $fp,$fp," + funcSymbolTable.getAddr());
        FileStream.mipsOutput("sw $t0,-4($sp)");
        FileStream.mipsOutput("sw $t1,-8($sp)");
        FileStream.mipsOutput("sw $t2,-12($sp)");
        FileStream.mipsOutput("sw $t3,-16($sp)");
        FileStream.mipsOutput("sw $t4,-20($sp)");
        FileStream.mipsOutput("sw $ra,-24($sp)");
        FileStream.mipsOutput("sw $s7,-28($sp)");
        FileStream.mipsOutput("addi $s7,$s7,16");
        FileStream.mipsOutput("addi $sp,$sp,-28");
        FileStream.mipsOutput("jal " + funcName);
        FileStream.mipsOutput("addi $sp,$sp,44");
        FileStream.mipsOutput("lw $a0,-4($sp)");
        FileStream.mipsOutput("lw $a1,-8($sp)");
        FileStream.mipsOutput("lw $a2,-12($sp)");
        FileStream.mipsOutput("lw $a3,-16($sp)");
        FileStream.mipsOutput("addi $fp,$fp," + (-funcSymbolTable.getAddr()));
        FileStream.mipsOutput("lw $t0,-20($sp)");
        FileStream.mipsOutput("lw $t1,-24($sp)");
        FileStream.mipsOutput("lw $t2,-28($sp)");
        FileStream.mipsOutput("lw $t3,-32($sp)");
        FileStream.mipsOutput("lw $t4,-36($sp)");
        FileStream.mipsOutput("lw $ra,-40($sp)");
        FileStream.mipsOutput("lw $s7,-44($sp)");
    }

    private static void ret(String[] strings) {
        if (strings.length > 1) {
            FileStream.mipsOutput("move $v0," + findSymbolsLoad(strings[1]));
        }
        FileStream.mipsOutput("jr $ra");
    }

    private static void globalEnd() {
        FileStream.mipsOutput("jal main");
        // FileStream.mipsOutput("main_end:");
        FileStream.mipsOutput("li $v0,10");
        FileStream.mipsOutput("syscall");
    }

    private static void setLabel(String label) {
        FileStream.mipsOutput(label + ":");
    }

    private static void branch(String[] strings) {
        String rs = findSymbolsLoad(strings[1]);
        if (strings[3].equals("true")) {
            FileStream.mipsOutput("bnez " + rs + "," + strings[2]);
        } else {
            FileStream.mipsOutput("beqz " + rs + "," + strings[2]);
        }
    }

    private static void jump(String label) {
        FileStream.mipsOutput("j " + label);
    }
}

class FuncSymbolTable {
    private HashMap<String, String> symbols;
    private HashMap<String, String> temSymbols;
    private FuncSymbolTable prev;
    // 地址偏移量
    private int addr = 0;
    // 参数数量大小
    private int paraNum = 0;
    // 临时寄存器是否有用
    private boolean[] tRegister;
    private int tReg = 6;

    public FuncSymbolTable(FuncSymbolTable prev) {
        this.prev = prev;
        symbols = new HashMap<>();
        temSymbols = new HashMap<>();
        tRegister = new boolean[]{true, true, true, true, true};
    }

    public FuncSymbolTable getPrev() {
        return prev;
    }

    public void addParas(String name, String isAddr) {
        // if isAddr, get "#"
        String rname = name + "#1";
        if (paraNum <= 3) {
            symbols.put(rname, isAddr + "$a" + paraNum);
            paraNum++;
        } else {
            symbols.put(rname, isAddr + addr);
            addr += 4;
        }
    }

    public void addVars(String name, int size, int layer) {
        String rname = name + "#" + layer;
        symbols.put(rname, String.valueOf(addr));
        addr += size;
    }

    public void deleteVars(int layer) {
        symbols.entrySet().removeIf(item -> item.getKey().contains("#" + layer));
    }

    public String findVar(String name, int layer) {
        int nowLayer = layer;
        while (!symbols.containsKey(name + "#" + nowLayer)) {
            nowLayer--;
        }
        return symbols.get(name + "#" + nowLayer);
        // return symbols.getOrDefault(name + "#" + layer, null);
    }

    // 临时变量
    public String addTemSymbol(String name) {
        if (tRegister[0] || tRegister[1] || tRegister[2] || tRegister[3] || tRegister[4]) {
            for (int i = 0; i < 5; i++) {
                if (tRegister[i]) {
                    temSymbols.put(name, "$t" + i);
                    tRegister[i] = false;
                    return "$t" + i;
                }
            }
        } else {
            temSymbols.put(name, String.valueOf(addr));
            addr += 4;
            return "$t6";
        }
        return null;
    }

    public void addAddr(int num) {
        addr += num;
    }

    public int getAddr() {
        return addr;
    }

    public String getTemNum(String name) {
        return temSymbols.get(name);
    }

    public String findTemSymbol(String name) {
        if (temSymbols.containsKey(name)) {
            String reg = temSymbols.get(name);
            if (!TargetCode.isInteger(reg)) {
                tRegister[Integer.parseInt(String.valueOf(reg.charAt(2)))] = true;
                return reg;
            } else {
                String treg = "$t" + tReg;
                FileStream.mipsOutput("lw " + treg + "," + reg + "($fp)");
                tReg = 13 - tReg;
                return treg;
            }
        }
        return null;
    }
}
