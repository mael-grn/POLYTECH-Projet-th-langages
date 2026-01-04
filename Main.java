import Asm.Program;
import RegisterAllocator.RegisterAllocator;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class Main {
	public static void main(String[] args) {
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
            System.out.println("\nAnalyse de type terminée avec succès.");

			String inputFile = args.length > 0 ? args[0] : "input";
			CharStream stream = CharStreams.fromFileName(inputFile);

			grammarTCLLexer lexer = new grammarTCLLexer(stream);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			grammarTCLParser parser = new grammarTCLParser(tokens);
			grammarTCLParser.MainContext tree = parser.main();

			// Groupe 2: Génération de code
			CodeGenerator codegen = new CodeGenerator(new HashMap<>());
            System.out.println(tree.toStringTree(parser));
			Program prog = codegen.visit(tree);

			// Groupe 3: Allocation de registres
			RegisterAllocator ra = new RegisterAllocator(prog);
			prog = ra.minimizeRegisters();

			PrintWriter writer = new PrintWriter("prog.asm");
			writer.print(prog.toString());
			writer.close();

			System.out.println("Compilation terminée avec succès.");
		} catch (Exception e) {
            System.err.println("\n" + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println("Fichier de sortie manquant.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			System.out.println("Impossible de generer le stream de caractère.");
			throw new RuntimeException(e);
		}
	}
}
