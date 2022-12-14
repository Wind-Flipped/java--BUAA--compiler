import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SymbolTable {
    private static final HashMap<String, Val> globalVals = new HashMap<>();
    private final SymbolTable father;
    private static final HashMap<String, Func> funcs = new HashMap<>();
    private final HashMap<String, Val> vals;
    private static final ArrayList<Val> para = new ArrayList<>(); // 函数形参
    // private final ArrayList<Val> partPara; // 函数实参

    public SymbolTable(SymbolTable father) {
        this.father = father;
        vals = new HashMap<>();
        // partPara = new ArrayList<>();
    }

    public SymbolTable getFather() {
        return father;
    }

    public static HashMap<String, Val> getGlobalVals() {
        return globalVals;
    }

    public static void clearPara() {
        para.clear();
    }

    public static boolean addPara(String name, int dimension, int dimen1, int dimen2) {
        if (para.contains(new Val(false, 0, name, dimen1, dimen2, false))) {
            return false;
        }
        para.add(new Val(false, dimension, name, dimen1, dimen2, false));
        return true;
    }

    /*
    public void addPartPara(String name, int dimension,int dimen1,int dimen2) {
        partPara.add(new Val(false, dimension, name,dimen1,dimen2,false));
    }
    */
    public boolean isFuncNameExist(String name) {
        return funcs.containsKey(name);
    }

    public String getFuncReturnType(String name) {
        return funcs.get(name).getReturnType();
    }

    public boolean funcParaNumCorrect(String name, ArrayList<Integer> curVals) {
        return funcs.get(name).getParaNum() == curVals.size();
    }

    public boolean funcParaTypeCorrect(String name, ArrayList<Integer> curVals) {
        return funcs.get(name).compareType(curVals);
    }

    /*
    public void clearPartPara() {
        partPara.clear();
    }
    */
    public static boolean addGlobalVal(String name, boolean isConst, int dimension, int dimen1, int dimen2) {
        if (globalVals.containsKey(name) || funcs.containsKey(name)) {
            return false;
        }
        globalVals.put(name, new Val(isConst, dimension, name, dimen1, dimen2, true));
        return true;
    }

    public boolean addVal(String name, boolean isConst, int dimension, int dimen1, int dimen2, int curLayer) {
        if (vals.containsKey(name) || (curLayer == 1 && para.contains(new Val(false, 0, name, dimen1, dimen2, false)))) {
            return false;
        }
        vals.put(name, new Val(isConst, dimension, name, dimen1, dimen2, false));
        return true;
    }

    public static boolean addFunc(String name, String returnType) {
        if (funcs.containsKey(name) || globalVals.containsKey(name)) {
            return false;
        }
        funcs.put(name, new Func(para, returnType, name));
        return true;
    }

    public Val findVal(String name) {
        if (vals.containsKey(name)) {
            return vals.get(name);
        }
        if (father != null) {
            return father.findVal(name);
        }
        for (int i = 0; i < para.size(); i++) {
            if (para.get(i).getName().equals(name)) {
                return para.get(i);
            }
        }
        if (globalVals.containsKey(name)) {
            // 全局变量是最后看的
            return globalVals.get(name);
        }
        return null;
    }

    public boolean isConstVal(String name) {
        Val val = findVal(name);
        if (val == null) {
            // 不让他报 修改常量 的错，而是 未定义变量 的问题。
            return false;
        }
        return val.isConst();
    }
}

class Func {
    private final String name;
    private final ArrayList<Val> parameters;
    private final String returnType;

    public Func(ArrayList<Val> parameters, String returnType, String name) {
        this.parameters = new ArrayList<>();
        this.parameters.addAll(parameters);
        this.returnType = returnType;
        this.name = name;
    }

    public int getParaNum() {
        return parameters.size();
    }

    public boolean compareType(ArrayList<Integer> rvals) {
        int size = parameters.size();
        for (int i = 0; i < size; i++) {
            if (rvals.get(i) != parameters.get(i).getDimension()) {
                return false;
            }
        }
        return true;
    }

    public String getReturnType() {
        return returnType;
    }
}

class Val {
    private String name;
    private boolean isConst;
    private ArrayList<Integer> dimension;
    private int[] values;
    private int constDimen1 = 0;
    private int constDimen2 = 0;
    private int address = 0;
    private boolean isGlobal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Val)) return false;
        Val val = (Val) o;
        return name.equals(val.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Val(boolean isConst, int dimen, String name, int dimen1, int dimen2, boolean isGlobal) {
        // 行 列 default is 1 * 1
        this.isGlobal = isGlobal;
        this.isConst = isConst;
        this.name = name;
        dimension = new ArrayList<>();
        if (dimen == 0) {
            values = new int[1];
        } else if (dimen == 1) {
            values = new int[dimen1];
        } else {
            values = new int[dimen1 * dimen2];
        }
        if (dimen >= 1) {
            dimension.add(dimen1);
        }
        if (dimen >= 2) {
            dimension.add(dimen2);
        }
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setAddress(int addr) {
        address = addr;
    }

    public int getAddress() {
        return address;
    }

    public int getSize() {
        // 所占字节大小
        return values.length * 4;
    }

    public void setValue(int value) {
        // 行 列 default is -1
        if (getDimension() == 2) {
            values[constDimen1 * dimension.get(1) + constDimen2] = value;
            constDimen2++;
            if (constDimen2 == dimension.get(1)) {
                constDimen1++;
                constDimen2 = 0;
            }
        } else if (getDimension() == 1) {
            values[constDimen1] = value;
            constDimen1++;
        } else {
            values[0] = value;
        }
    }

    public int getValue(int dimen1, int dimen2) {
        // 行 列 default is -1
        if (dimen2 == -1) {
            if (dimen1 == -1) {
                return values[0];
            } else {
                return values[dimen1];
            }
        } else {
            return values[dimen1 * dimension.get(1) + dimen2];
        }
    }

    public boolean isConst() {
        return isConst;
    }

    public int getDimension() {
        return dimension.size();
    }

    public int getCoDimen() {
        if (dimension.size() == 2) {
            return dimension.get(1);
        } else {
            return 0;
        }
    }

    public String getName() {
        return name;
    }
}
