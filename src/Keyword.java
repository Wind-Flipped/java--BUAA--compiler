import java.util.HashMap;

public class Keyword {
    private static final HashMap<String,String> KEYWORDS = new HashMap<>();

    //public static addKeywords()

    public static void updateKeywords() {
        KEYWORDS.put("main","MAINTK");
        KEYWORDS.put("const","CONSTTK");
        KEYWORDS.put("int","INTTK");
        KEYWORDS.put("break","BREAKTK");
        KEYWORDS.put("continue","CONTINUETK");
        KEYWORDS.put("if","IFTK");
        KEYWORDS.put("else","ELSETK");
        KEYWORDS.put("while","WHILETK");
        KEYWORDS.put("getint","GETINTTK");
        KEYWORDS.put("printf","PRINTFTK");
        KEYWORDS.put("return","RETURNTK");
        KEYWORDS.put("void","VOIDTK");
    }

    public static String searchKeyWord(String string) {
        if (KEYWORDS.containsKey(string)) {
            return KEYWORDS.get(string);
        }
        return null;
    }
}
