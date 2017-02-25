# Compiler

Compiler for largish subset of C programming language.

## Instructions

You must have Ant build tool installed.

```
git clone git@github.com:Pterodactylus/compiler.git
```

or HTTPS

```
git clone https://github.com/Pterodactylus/compiler.git
```

```
$ cd compiler/
```

```
$ ant build
```

Test out the possible flags available.

```
$ java -cp bin Main
```

You should see something similar:

```
$ java -cp bin Main
Usage: java Main pass inputfile outputfile
where pass is either: -lexer, -parser, -ast, -sem or -gen
```

Try out several test programs from tests folder. For example, to test out lexer,
run:

```
$ java -cp bin Main -lexer tests/b1.c tests/output.txt
```

Replace -lexer with a different flag available above. To test code generation, you would
replace -lexer with -gen