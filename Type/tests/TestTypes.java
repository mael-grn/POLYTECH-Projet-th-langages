package Type.tests;
import org.antlr.v4.runtime.*;
import Type.*;

public class TestTypes {
    public static <grammarTCLParser, grammarTCLLexer, TyperVisitor> void main(String[] args) throws Exception {
        String code = """
            int main() {
                int x = 42;
                bool b = true;
                int[] t = {1, 2, 3};
                return x;
            }
            """;

        CharStream input = CharStreams.fromString(code);
        grammarTCLLexer lexer = new grammarTCLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        grammarTCLParser parser = new grammarTCLParser(tokens);

        TyperVisitor typer = new TyperVisitor();
        try {
            Type result = typer.visit(parser.main());
            System.out.println("✅ Typage réussi !");
            System.out.println("Types inférés : " + typer.getTypes());
        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}