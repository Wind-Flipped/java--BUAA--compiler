public class Compiler {
    public static void main(String[] args) {
        Keyword.updateKeywords();
        Parser parser = new Parser();
        // FileStream.changeNewOutput();
        // FileStream.changeErrorOutput();
        FileStream.changeMidddleOutput();
        parser.compUnit();
        FileStream.changeOldOutput();
        /*
        line = FileStream.getNextLine();
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\n') {
                System.out.println("有nnnn");
            } else if (line.charAt(i) == '\r') {
                System.out.println("有rrrr");
            } else {
                System.out.println(line.charAt(i));
            }
        }

        while ((line = FileStream.getNextLine()) != null) {
            lexer = new Lexer(line);
            while (lexer.getSymbol()) {
                if (!lexer.getTag().equals("noteOn") && !lexer.getTag().equals("noteOff")) {
                    FileStream.output(lexer.getTag() + ' ' + lexer.getCurToken());
                }
            }
        }
        */
    }
}
