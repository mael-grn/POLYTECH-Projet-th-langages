# Compilateur TCL

1) Groupe1 :
- BERTAUX Kevin
- BENSALEM Wail
- CAILLE Daniel

2) Groupe2 :
- GARNIER Maël
- CHABANNE Alexis
- BONNEFOND Cyprien

3) Groupe3 :
- CHAUCHET Mattéo
- GUETTOUF Yanis
- GAUDRY Ben


## Instructions pour compiler et exécuter le compilateur TCL

### Prérequis

- Java 25

### Compilation

1) Entrer le programme TCL dans le fichier _`input`_
2) Lancer le fichier _`Main.java`_
3) Le code assembleur sera généré dans le fichier _`prog.asm`_

(Les erreurs de compilation seront affichées dans la console)



## Structure du code

### Groupe 3

Tout le code lié à l'allocation de registre se trouve dans le package _`RegisterAllocator`_

- **RegisterAllocator.RegisterAllocator** :

Classe principale de l'allocation de registre (méthode minimizeRegisters gère tout)

- **RegisterAllocator.RegisterAllocatorTest** :

Classe de test pour la classe RegisterAllocator

- **RegisterAllocator.ControlGraph** :

Classe représentant le graphe de contrôle du programme assembleur

- **RegisterAllocator.ControlGraphTest** :

Classe de test pour la classe ControlGraph

- **RegisterAllocator.CFGAnalysis** :

Classe pour l'analyse de vivacité des variables sur le graphe de contrôle, et qui construit le graphe d'interférences
