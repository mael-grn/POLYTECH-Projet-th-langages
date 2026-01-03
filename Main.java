import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import Type.*;
import java.io.*;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        // Nom du fichier source (par défaut 'input')
        String fichier = "input";

        if (!new File(fichier).exists()) {
            System.err.println("Erreur : fichier '" + fichier + "' introuvable.");
            return;
        }

        try {
            // 1. Analyse Lexicale et Syntaxique
            grammarTCLLexer lexer = new grammarTCLLexer(CharStreams.fromPath(new File(fichier).toPath()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            grammarTCLParser parser = new grammarTCLParser(tokens);
            ParseTree tree = parser.main();

            // 2. Analyse Sémantique (Inférence de types)
            TyperVisitor typer = new TyperVisitor();
            typer.visit(tree);

            // 3. Affichage de la Table des Symboles Résolue
            // C'est ici qu'on voit si 'auto' a bien été transformé en 'int' ou 'bool'
            System.out.println("=== TABLE DES SYMBOLES (Types finaux) ===");
            Map<String, Type> symbolTable = typer.getSymbolTable();
            Map<UnknownType, Type> substitutions = typer.getTypes();

            if (symbolTable.isEmpty()) {
                System.out.println("Aucune variable ou fonction déclarée.");
            } else {
                for (Map.Entry<String, Type> entry : symbolTable.entrySet()) {
                    // On applique toutes les substitutions pour résoudre les 'auto'
                    Type finalType = entry.getValue().substituteAll(substitutions);
                    System.out.println("Nom: " + entry.getKey() + " => Type: " + typeVersTexte(finalType));
                }
            }

            System.out.println("\n✓ Analyse terminée avec succès.");

        } catch (Exception e) {
            System.err.println("\n✗ ERREUR DE TYPAGE : " + e.getMessage());
            // Optionnel : e.printStackTrace(); // Pour le debug précis
        }
    }

    /**
     * Méthode utilitaire pour rendre l'affichage des types plus lisible
     */
    private static String typeVersTexte(Type t) {
        if (t instanceof PrimitiveType) {
            PrimitiveType p = (PrimitiveType) t;
            return p.getType().toString().toLowerCase();
        }
        if (t instanceof ArrayType) {
            return typeVersTexte(((ArrayType) t).getTabType()) + "[]";
        }
        if (t instanceof FunctionType) {
            FunctionType f = (FunctionType) t;
            StringBuilder s = new StringBuilder("(");
            for (int i = 0; i < f.getNbArgs(); i++) {
                s.append(typeVersTexte(f.getArgsType(i)));
                if (i < f.getNbArgs() - 1) s.append(", ");
            }
            s.append(") -> ").append(typeVersTexte(f.getReturnType()));
            return s.toString();
        }
        if (t instanceof UnknownType) {
            return "auto (non résolu : " + t.toString() + ")";
        }
        return t.toString();
    }
}