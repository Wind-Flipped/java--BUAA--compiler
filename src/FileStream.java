import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Scanner;

public class FileStream {
    private static final String INPUT = "testfile.txt";
    private static final String OUTPUT = "output.txt";
    private static final String ERROR = "error.txt";
    private static final String MIDDLETEXT = "20373625_刘运淇_优化前中间代码.txt";
    private static final PrintStream OUT = System.out; //保存原输出流
    private static PrintStream error_print;

    private static PrintStream middle_print;

    static {
        try {
            middle_print = new PrintStream(MIDDLETEXT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            error_print = new PrintStream(ERROR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static PrintStream output_print;

    static {
        try {
            output_print = new PrintStream(OUTPUT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Scanner sc;

    static {
        try {
            sc = new Scanner(new FileReader(INPUT));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void changeNewOutput() {
        System.setOut(output_print);//切换输出流
    }

    public static void changeOldOutput() {
        System.setOut(OUT);
    }

    public static void changeErrorOutput() {
        System.setOut(error_print);
    }

    public static void changeMidddleOutput() {
        System.setOut(middle_print);
    }

    public static void error(String str) {
        // System.out.println(str);
    }

    public static String getNextLine() {
        if (sc.hasNextLine()) {
            return sc.nextLine();
        }
        // System.setOut(OUT); //切换回原输出流
        return null;
    }

    public static void output(String str) {
//        changeNewOutput();
//        System.out.println(str);
    }

    public static void middleCodeOutput(String str) {
        System.setOut(middle_print);
        System.out.println(str);
    }
}
