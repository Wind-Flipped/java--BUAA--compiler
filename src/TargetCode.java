import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TargetCode {
    private static HashMap<String, Val> globalVals = new HashMap<>();
    private static HashMap<String, String> operations = new HashMap<>();
    private static ArrayList<String> middleCodes = new ArrayList<>();
    // 当前符号表
    private static FuncSymbolTable funcSymbolTable = new FuncSymbolTable(null);
    // 处理到了第几行，第一行序号为 0
    private static int line = 0;
    // 块层数，全局变量处在第 0 层
    private static int layer = 0;
    // $v0
    private static int v0 = 0;
    // 实参数目
    private static int rPara = 0;
    // k register
    private static int kReg = 0;
    // s register
    private static int sReg = 0;


    public static void generateMips() {
        operations.put("+", "add");
        operations.put("-", "sub");
        operations.put("*", "mul");
        operations.put("/", "div");
        operations.put("%", "yu");
        beginData();
        beginText();
        convertContent();
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
            FileStream.mipsOutput("li $k" + kReg + "," + Integer.parseInt(name));
            kReg = 1 - kReg;
            return kr;
        }
        String sRegister = "$s" + sReg;
        sReg = 1 - sReg;
        if (name.contains("<global>")) {
            // s<global>[4]
            String[] pureName = name.split("[<>\\[\\]]");
            int addr = globalVals.get(pureName[0]).getAddress();
            if (name.contains("[")) {
                // 含数组形式
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
                        FileStream.mipsOutput("lw $t8," + funcSymbolTable.findVar(pureName[3], layer) + "($sp)");
                        FileStream.mipsOutput("sll $t8,$t8,2");
                    }
                    FileStream.mipsOutput("add $t8,$t8,$gp");
                    FileStream.mipsOutput("lw " + sRegister + "," + addr + "($t8)");
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
            if (isInteger(res)) {
                int x = Integer.parseInt(res);
                if (pureName.length > 1) {
                    // str[3]
                    if (isInteger(pureName[1])) {
                        x = Integer.parseInt(pureName[1]) * 4 + Integer.parseInt(res);
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($fp)");
                    } else {
                        if (pureName[1].charAt(0) == '@') {
                            // str[@t1]
                            FileStream.mipsOutput("sll $t8," + funcSymbolTable.findTemSymbol(pureName[1]) + ",2");
                        } else {
                            // str[i]
                            FileStream.mipsOutput("lw $t8," + funcSymbolTable.findVar(pureName[1], layer) + "($fp)");
                            FileStream.mipsOutput("sll $t8,$t8,2");
                        }
                        FileStream.mipsOutput("add $t8,$t8,$fp");
                        FileStream.mipsOutput("lw " + sRegister + "," + x + "($t8)");
                    }
                } else {
                    // 4 (bias)
                    x = Integer.parseInt(res);
                    FileStream.mipsOutput("lw " + sRegister + "," + x + "($fp)");
                }
                return sRegister;
            } else {
                // $a0
                sReg = 1 - sReg;
                return res;
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

    private static void findSymbolsStore(String name, String regStore) {
        // 把 regStore 寄存器值存入 name 标识符内
        if (name.contains("<global>")) {
            // s<global>[4]
            String[] pureName = name.split("[<>\\[\\]]");
            int addr = globalVals.get(pureName[0]).getAddress();
            if (name.contains("[")) {
                // 含数组形式
                if (isInteger(pureName[3])) {
                    int x = 4 * Integer.parseInt(pureName[3]) + addr;
                    FileStream.mipsOutput("sw " + regStore + "," + x + "($gp)");
                } else {
                    // s<global>[@t1]
                    if (pureName[3].charAt(0) == '@') {
                        // str[@t1]
                        FileStream.mipsOutput("sll $t8," + funcSymbolTable.findTemSymbol(pureName[3]) + ",2");
                    } else {
                        // str[i]
                        FileStream.mipsOutput("lw $t8," + funcSymbolTable.findVar(pureName[3], layer) + "($gp)");
                        FileStream.mipsOutput("sll $t8,$t8,2");
                    }
                    FileStream.mipsOutput("add $t8,$t8,$fp");
                    FileStream.mipsOutput("sw " + regStore + "," + addr + "($gp)");
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
            if (isInteger(res)) {
                int x = Integer.parseInt(res);
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
                            FileStream.mipsOutput("lw $t8," + funcSymbolTable.findVar(pureName[1], layer) + "($fp)");
                            FileStream.mipsOutput("sll $t8,$t8,2");
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
        }
    }

    private static void beginData() {
        FileStream.mipsOutput(".data");
        globalVals = SymbolTable.getGlobalVals();
        int addr = 0;
        for (Map.Entry<String, Val> entry : globalVals.entrySet()) {
            if (!entry.getValue().isConst()) {
                FileStream.mipsOutput(entry.getKey() + ": .space " + entry.getValue().getSize());
                entry.getValue().setAddress(addr);
                addr += entry.getValue().getSize();
            }
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

        FileStream.mipsOutput("jal main");
        FileStream.mipsOutput("main_end:");
        FileStream.mipsOutput("li $v0,10");
        FileStream.mipsOutput("syscall");
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
                push(strings[1]);
                break;
            case "#return":
                ret(strings[1]);
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
            } else {
                FileStream.mipsOutput("div " + rs + "," + rt);
                FileStream.mipsOutput("mfhi " + rd);
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
    }

    private static void funcEnd() {
        funcSymbolTable = funcSymbolTable.getPrev();
        FileStream.mipsOutput("jr $ra");
    }

    private static void para(String[] strings) {
        funcSymbolTable.addParas(strings[2]);
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
        if (v0 != 5) {
            FileStream.mipsOutput("li $v0,5");
        }
        FileStream.mipsOutput("syscall");
        v0 = 0;
        findSymbolsStore(name, "$v0");
    }

    private static void printf(String[] strings) {
        if (strings[0].equals("#printString")) {
            FileStream.mipsOutput("sw $a0,-4($sp)");
            FileStream.mipsOutput("la $a0," + strings[1]);
            if (v0 != 4) {
                FileStream.mipsOutput("li $v0,4");
                v0 = 4;
            }
            FileStream.mipsOutput("syscall");
            FileStream.mipsOutput("lw $a0,-4($sp)");
        } else {
            if (v0 != 1) {
                FileStream.mipsOutput("li $v0,1");
                v0 = 1;
            }
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
        FileStream.mipsOutput("sw $ra,-4($sp)");
        FileStream.mipsOutput("sw $a0,-8($sp)");
        FileStream.mipsOutput("sw $a1,-12($sp)");
        FileStream.mipsOutput("sw $a2,-16($sp)");
        FileStream.mipsOutput("sw $a3,-20($sp)");
        FileStream.mipsOutput("addi $fp,$fp," + funcSymbolTable.getAddr());
    }

    private static void push(String rparaName) {
        //TODO 区分地址和值传递
        String rs = findSymbolsLoad(rparaName);
        if (rPara <= 3) {
            FileStream.mipsOutput("move $a" + rPara + "," + rs);
        } else {
            FileStream.mipsOutput("lw " + rs + "," + (rPara - 4) * 4 + "($fp)");
        }
        rPara += 1;
    }

    private static void call(String funcName) {
        rPara = 0;
        FileStream.mipsOutput("sw $t0,-24($sp)");
        FileStream.mipsOutput("sw $t1,-28($sp)");
        FileStream.mipsOutput("sw $t2,-32($sp)");
        FileStream.mipsOutput("sw $t3,-36($sp)");
        FileStream.mipsOutput("sw $t4,-40($sp)");
        FileStream.mipsOutput("sw $s0,-44($sp)");
        FileStream.mipsOutput("sw $s1,-48($sp)");
        FileStream.mipsOutput("addi $sp,$sp,-48");
        FileStream.mipsOutput("jal " + funcName);
        FileStream.mipsOutput("addi $sp,$sp,48");
        FileStream.mipsOutput("lw $ra,-4($sp)");
        FileStream.mipsOutput("lw $a0,-8($sp)");
        FileStream.mipsOutput("lw $a1,-12($sp)");
        FileStream.mipsOutput("lw $a2,-16($sp)");
        FileStream.mipsOutput("lw $a3,-20($sp)");
        FileStream.mipsOutput("addi $fp,$fp," + (-funcSymbolTable.getAddr()));
        FileStream.mipsOutput("lw $t0,-24($sp)");
        FileStream.mipsOutput("lw $t1,-28($sp)");
        FileStream.mipsOutput("lw $t2,-32($sp)");
        FileStream.mipsOutput("lw $t3,-36($sp)");
        FileStream.mipsOutput("lw $t4,-40($sp)");
        FileStream.mipsOutput("lw $s0,-44($sp)");
        FileStream.mipsOutput("lw $s1,-48($sp)");
    }

    private static void ret(String name) {
        FileStream.mipsOutput("move $v0," + findSymbolsLoad(name));
        FileStream.mipsOutput("jr $ra");
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
    // 临时寄存器号
    private int reg = 0;

    public FuncSymbolTable(FuncSymbolTable prev) {
        this.prev = prev;
        symbols = new HashMap<>();
        temSymbols = new HashMap<>();
    }

    public FuncSymbolTable getPrev() {
        return prev;
    }

    public void addParas(String name) {
        String rname = name + "#1";
        if (paraNum <= 3) {
            symbols.put(rname, "$a" + paraNum);
            paraNum++;
        } else {
            symbols.put(rname, String.valueOf(addr));
            addr += 4;
        }
    }

    public void addVars(String name, int size, int layer) {
        String rname = name + "#" + layer;
        symbols.put(rname, String.valueOf(addr));
        addr += size;
    }

    public String findVar(String name, int layer) {
        return symbols.getOrDefault(name + "#" + layer, null);
    }

    // 临时变量
    public String addTemSymbol(String name) {
        temSymbols.put(name, "$t" + reg);
        String rd = "$t" + reg;
        // 总共 5 个临时寄存器
        reg = (reg + 1) % 5;
        return rd;
    }

    public int getAddr() {
        return addr;
    }

    public String findTemSymbol(String name) {
        return temSymbols.getOrDefault(name, null);
    }
}
