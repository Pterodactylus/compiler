package lexer;

import lexer.Token.TokenClass;

import java.io.EOFException;
import java.io.IOException;


public class Tokeniser {

    private Scanner scanner;

    private int error = 0;
    public int getErrorCount() {
	return this.error;
    }

    public Tokeniser(Scanner scanner) {
        this.scanner = scanner;
    }

    private void error(char c, int line, int col) {
        System.out.println("Lexing error: unrecognised character ("+c+") at "+line+":"+col);
	    error++;
    }


    public Token nextToken() {
        Token result;
        try {
             result = next();
        } catch (EOFException eof) {
            // end of file, nothing to worry about, just return EOF token
            return new Token(TokenClass.EOF, scanner.getLine(), scanner.getColumn());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // something went horribly wrong, abort
            System.exit(-1);
            return null;
        }
        return result;
    }

    private Token next() throws IOException {

        int line = scanner.getLine();
        int column = scanner.getColumn();

        // get the next character
        char c = scanner.next();

        // skip white spaces
        if (Character.isWhitespace(c))
            return next();

        /**
         * Simple things: delimiters, struct access, operators.
         */

        if (c == '+') {
            return new Token(TokenClass.PLUS, "+", line, column);
        }

        if (c == '-') {
            return new Token(TokenClass.MINUS, "-", line, column);
        }

        if (c == '*') {
            return new Token(TokenClass.ASTERIX, "*", line, column);
        }

        if (c == '%') {
            return new Token(TokenClass.REM, "%", line, column);
        }


        if (c == '[') {
            return new Token(TokenClass.LSBR, "[", line, column);
        }

        if (c == ']') {
            return new Token(TokenClass.RSBR, "]", line, column);
        }

        if (c == '{') {
            return new Token(TokenClass.LBRA, "{", line, column);
        }

        if (c == '}') {
            return new Token(TokenClass.RBRA, "}", line, column);
        }

        if (c == '(') {
            return new Token(TokenClass.LPAR, "(", line, column);
        }

        if (c == ')') {
            return new Token(TokenClass.RPAR, ")", line, column);
        }

        if (c == ',') {
            return new Token(TokenClass.COMMA, ",", line, column);
        }

        if (c == ';') {
            return new Token(TokenClass.SC, ";", line, column);
        }

        if (c == '.') {
            return new Token(TokenClass.DOT, ".", line, column);
        }

        /**
         * Identifiers, types and keywords.
         */

        if (Character.isLetter(c)) {
            StringBuilder builtStr = new StringBuilder();
            builtStr.append(c);
            c = scanner.peek();
            while (Character.isLetterOrDigit(c) || c == '_') {
                builtStr.append(c);
                scanner.next();
                c = scanner.peek();
            }

            String result = builtStr.toString();

            switch (result) {
                case "int":
                    return new Token(TokenClass.INT, "INT_TYPE", line, column);
                case "void":
                    return new Token(TokenClass.VOID, "VOID_TYPE", line, column);
                case "char":
                    return new Token(TokenClass.CHAR, "CHAR_TYPE", line, column);

                case "if":
                    return new Token(TokenClass.IF, "IF_KEYWORD", line, column);
                case "else":
                    return new Token(TokenClass.ELSE, "ELSE_KEYWORD", line, column);
                case "sizeof":
                    return new Token(TokenClass.SIZEOF, "SIZEOF_KEYWORD", line, column);
                case "return":
                    return new Token(TokenClass.RETURN, "RETURN_KEYWORD", line, column);
                case "while":
                    return new Token(TokenClass.WHILE, "WHILE_KEYWORD", line, column);
                case "struct":
                    return new Token(TokenClass.STRUCT, "STRUCT_KEYWORD", line, column);

                default:
                    return new Token(TokenClass.IDENTIFIER, result, line, column);
            }
        }

        /**
         * Division, single-line comment and multi-line comment.
         */

        if (c == '/') {
            c = scanner.peek();
            if (c == '/') {
                // Single-line comment detected.
                scanner.next();
                c = scanner.peek();
                while (c != '\n') {
                    scanner.next();
                    c = scanner.peek();
                }
            } else if (c == '*') {
                // Multi-line comment detected.
                char previousChar = c;
                scanner.next();
                c = scanner.peek();

                // Iterate until multi-line comment ending slash is detected.
                // Then check if last character in builtComment is '*'. I check for
                // '/' because people like to decorate their multi-line comments with
                // '*'s( me :) ) for efficiency.
                while (true) {
                    previousChar = c;
                    scanner.next();
                    c = scanner.peek();
                    if (c == '/') {
                        // End of multi-line comment detected.
                        if (previousChar == '*') {
                            scanner.next();
                            break;
                        }
                    }
                }
            } else {
                // Division operator.
                return new Token(TokenClass.DIV, "/", line, column);
            }

            // When comment is processed, return next token. Very important.
            return next();
        }

        /**
         * Include statement.
         */

        if (c == '#') {
            StringBuilder builtInclude = new StringBuilder();
            builtInclude.append(c);
            scanner.next();
            c = scanner.peek();
            while (!Character.isWhitespace(c)) {
                builtInclude.append(c);
                scanner.next();
                c = scanner.peek();
            }

            String result = builtInclude.toString();
            if (result.equals("#include")) {
                return new Token(TokenClass.INCLUDE, "#include", line, column);
            }

            return new Token(TokenClass.INVALID, line, column);
        }

        /**
         * Literals: strings, chars and integers.
         */

        if (Character.isDigit(c)) {
            StringBuilder builtInteger = new StringBuilder();
            builtInteger.append(c);
            scanner.next();
            c = scanner.peek();
            while (Character.isDigit(c)) {
                builtInteger.append(c);
                scanner.next();
                c = scanner.peek();
            }

            return new Token(TokenClass.INT_LITERAL, builtInteger.toString(), line, column);
        }

        // For strings, I don't know if double qoutes should enclose characters on a single line
        // or can span multiple. For now, I will leave it with multiple lines. It can contain '\n'
        // so it will be over multiple lines anyways?
        if (c == '"') {
            StringBuilder builtStr = new StringBuilder();
            c = scanner.peek();
            while (c != '"') {
                // Escape character detected. Handle supported escape
                // characters: \b, \t, \n, \', \", \f, \\. \r
                if (c == '\\') {
                    scanner.next();
                    c = scanner.peek();
                    if (isNonLetterEscapeChar(c)) {
                        builtStr.append(c);
                    } else if (isLetterEscapeChar(c)) {
                        builtStr.append("\\" + c);
                    } else {
                        error(c, line, column);
                        return new Token(TokenClass.INVALID, line, column);
                    }
                } else {
                    builtStr.append(c);
                }
                scanner.next();
                c = scanner.peek();
            }

            scanner.next();

            return new Token(TokenClass.STRING_LITERAL, builtStr.toString(), line, column);
        }

        if (c == '\'') {
            StringBuilder builtChar = new StringBuilder();
            c = scanner.peek();
            if (c == '\'') {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }

            if (c == '\\') {
                // Escape character detected.
                scanner.next();
                c = scanner.peek();
                if (isNonLetterEscapeChar(c)) {
                    builtChar.append(c);
                } else if (isLetterEscapeChar(c)) {
                    builtChar.append("\\" + c);
                } else {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }
            }

            builtChar.append(c);

            scanner.next();
            c = scanner.peek();

            if (c != '\'') {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }

            scanner.next();

            return new Token(TokenClass.CHAR_LITERAL, builtChar.toString(), line, column);
        }

        /**
         * Logical operators.
         */

        if (c == '&') {
            c = scanner.peek();
            if (c != '&') {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }

            scanner.next();
            return new Token(TokenClass.AND, "&&", line, column);
        }

        if (c == '|') {
            c = scanner.peek();
            if (c != '|') {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }

            scanner.next();
            return new Token(TokenClass.OR, "||", line, column);
        }

        /**
         * Assign, comparisons.
         */

        if (c == '=') {
            c = scanner.peek();
            if (c == '=') {
                // It's an equality!
                scanner.next();
                return new Token(TokenClass.EQ, "==", line, column);
            }
            return new Token(TokenClass.ASSIGN, "=", line, column);
        }

        if (c == '!') {
            c = scanner.peek();
            if (c == '=') {
                // It's a not equals!
                scanner.next();
                return new Token(TokenClass.NE, "!=", line, column);
            }

            // Otherwise, it is an error.
            error(c, line, column);
            return new Token(TokenClass.INVALID, line, column);
        }

        if (c == '<') {
            c = scanner.peek();
            if (c == '=') {
                // It's a <=
                scanner.next();
                return new Token(TokenClass.LE, "<=", line, column);
            }
            return new Token(TokenClass.LT, "<", line, column);
        }

        if (c == '>') {
            c = scanner.peek();
            if (c == '=') {
                // It's a >=
                scanner.next();
                return new Token(TokenClass.GE, ">=", line, column);
            }
            return new Token(TokenClass.GT, ">", line, column);
        }

        // if we reach this point, it means we did not recognise a valid token
        error(c, line, column);
        return new Token(TokenClass.INVALID, line, column);
    }

    private boolean isNonLetterEscapeChar(char c) {
        return c == '\\' || c == '\'' || c == '\"';
    }

    private boolean isLetterEscapeChar(char c) {
        return c == 't' || c == 'r' || c == 'n' || c == 'f' || c == 'b';
    }
}
