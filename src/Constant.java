import sun.awt.windows.ThemeReader;

import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constant {
    private static ArrayList<String> middleCodes;
    private static HashMap<String,String> constantMap;
    // 处理到了第几行，第一行序号为 0
    private static int line = 0;

    private static String temp = "@t[\\d]*";
    private static final Pattern patternTemp = Pattern.compile(temp);

    public static void beginOptimization() {
        middleCodes = FileStream.getMiddleCodes();
        constantMap = new HashMap<>();
        beginConvert();
    }

    public static void beginConvert() {
        while (true) {
            String theLine = middleCodes.get(line);
            Matcher matcher = patternTemp.matcher(theLine);
            while (matcher.find()) {
                String curTemp = matcher.group();
                String tempNum = constantMap.getOrDefault(curTemp,curTemp);
                theLine = theLine.replaceAll(curTemp + "\\s",tempNum + " ");
                theLine = theLine.replaceAll(curTemp + "]",tempNum + "]");
                theLine = theLine.replaceAll(curTemp + "$",tempNum);
            }
            String[] strings = theLine.split("\\s");
            line++;
            if (strings[0].equals("#main_end")) {
                FileStream.optimizeOutput(theLine);
                break;
            }
            if (strings[0].charAt(0) == '#') {
                FileStream.optimizeOutput(theLine);
            } else {
                if (strings[0].charAt(0) == '@') {
                    if (strings.length == 5) {
                        if (constantMap.containsKey(strings[2])) {
                            strings[2] = constantMap.get(strings[2]);
                        }
                        if (constantMap.containsKey(strings[4])) {
                            strings[4] = constantMap.get(strings[4]);
                        }
                        if (strings[3].equals("*") && TargetCode.isInteger(strings[2])) {
                            theLine = strings[0] + " " + strings[1] + " " + strings[4] + " " + strings[3] + " " + strings[2];
                        }
                        if (strings[3].equals("*")) {
                            if (TargetCode.isInteger(strings[2]) && Parser.str2int(strings[2]) == 0 ||
                                    TargetCode.isInteger(strings[4]) && Parser.str2int(strings[4]) == 0) {
                                constantMap.put(strings[0], "0");
                                continue;
                            }
                        }
                        if (TargetCode.isInteger(strings[2]) && TargetCode.isInteger(strings[4])) {
                            int x1 = Parser.str2int(strings[2]);
                            int x2 = Parser.str2int(strings[4]);
                            int res = 0;
                            switch (strings[3]) {
                                case "+":
                                    res = x1 + x2;
                                    break;
                                case "-":
                                    res = x1 - x2;
                                    break;
                                case "*":
                                    res = x1 * x2;
                                    break;
                                case "/":
                                    res = x1 / x2;
                                    break;
                                case "%":
                                    res = x1 % x2;
                            }
                            constantMap.put(strings[0], String.valueOf(res));
                        } else {
                            FileStream.optimizeOutput(theLine);
                        }
                    } else {
                        FileStream.optimizeOutput(theLine);
                    }
                } else {
                    FileStream.optimizeOutput(theLine);
                }
            }
        }
    }
}
