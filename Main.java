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

		try {
			String inputFile = args.length > 0 ? args[0] : "input";
			CharStream stream = CharStreams.fromFileName(inputFile);

			grammarTCLLexer lexer = new grammarTCLLexer(stream);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			grammarTCLParser parser = new grammarTCLParser(tokens);
			grammarTCLParser.MainContext tree = parser.main();

			CodeGenerator codegen = new CodeGenerator(new HashMap<>());
			Program prog = codegen.visit(tree);

			RegisterAllocator ra = new RegisterAllocator(prog);
			prog = ra.minimizeRegisters();

			PrintWriter writer = new PrintWriter("prog.asm");
			writer.print(prog.toString());
			writer.close();

			System.out.println("Compilation terminée avec succès.");
		} catch (FileNotFoundException e) {
			System.out.println("Fichier de sortie manquant.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			System.out.println("Impossible de generer le stream de caractère.");
			throw new RuntimeException(e);
		}
	}
}
