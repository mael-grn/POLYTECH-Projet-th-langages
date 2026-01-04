import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import Type.*;
import java.io.*;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        String fichier = "input";

        if (!new File(fichier).exists()) {
            System.err.println("Erreur : fichier '" + fichier + "' introuvable.");
            return;
        }

        try {
            grammarTCLLexer lexer = new grammarTCLLexer(CharStreams.fromPath(new File(fichier).toPath()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            grammarTCLParser parser = new grammarTCLParser(tokens);
            ParseTree tree = parser.main();

            TyperVisitor typer = new TyperVisitor();
            typer.visit(tree);

            System.out.println("=== TABLE DES SYMBOLES (Types finaux) ===");
            Map<String, Type> symbolTable = typer.getSymbolTable();
            Map<UnknownType, Type> substitutions = typer.getTypes();

            if (symbolTable.isEmpty()) {
                System.out.println("Aucune variable ou fonction déclarée.");
            } else {
                for (Map.Entry<String, Type> entry : symbolTable.entrySet()) {
                    Type finalType = entry.getValue().substituteAll(substitutions);
                    System.out.println("Nom: " + entry.getKey() + " => Type: " + finalType.toString());
                }
            }
            System.out.println("\nAnalyse terminée avec succès.");

        } catch (Exception e) {
            System.err.println("\n" + e.getMessage());
        }
    }

    }