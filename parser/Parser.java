package parser;

import ast.*;
import lexer.Token;
import lexer.Token.TokenClass;
import lexer.Tokeniser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static ast.BaseType.INT;


public class Parser {

    private static final TokenClass[] TYPES = new TokenClass[] {
        TokenClass.INT, TokenClass.CHAR, TokenClass.VOID, TokenClass.STRUCT
    };

    private Token token;

    // use for backtracking (useful for distinguishing decls from procs when parsing a program for instance)
    private Queue<Token> buffer = new LinkedList<Token>();

    private final Tokeniser tokeniser;



    public Parser(Tokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public Program parse() {
        // get the first token
        nextToken();

        return parseProgram();
    }

    public int getErrorCount() {
        return error;
    }

    private int error = 0;
    private Token lastErrorToken;

    private void error(TokenClass... expected) {

        if (lastErrorToken == token) {
            // skip this error, same token causing trouble
            return;
        }

        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (TokenClass e : expected) {
            sb.append(sep);
            sb.append(e);
            sep = "|";
        }
        System.out.println("Parsing error: expected ("+sb+") found ("+token+") at "+token.position);

        error++;
        lastErrorToken = token;
    }

    /*
     * Look ahead the i^th element from the stream of token.
     * i should be >= 1
     */
    private Token lookAhead(int i) {
        // ensures the buffer has the element we want to look ahead
        while (buffer.size() < i)
            buffer.add(tokeniser.nextToken());
        assert buffer.size() >= i;

        int cnt=1;
        for (Token t : buffer) {
            if (cnt == i)
                return t;
            cnt++;
        }

        assert false; // should never reach this
        return null;
    }


    /*
     * Consumes the next token from the tokeniser or the buffer if not empty.
     */
    private void nextToken() {
        if (!buffer.isEmpty())
            token = buffer.remove();
        else
            token = tokeniser.nextToken();
    }

    /*
     * If the current token is equals to the expected one, then skip it, otherwise report an error.
     * Returns the expected token or null if an error occurred.
     */
    private Token expect(TokenClass... expected) {
        for (TokenClass e : expected) {
            if (e == token.tokenClass) {
                Token cur = token;
                nextToken();
                return cur;
            }
        }

        error(expected);
        return null;
    }

    /*
    * Returns true if the current token is equals to any of the expected ones.
    */
    private boolean accept(TokenClass... expected) {
        boolean result = false;
        for (TokenClass e : expected)
            result |= (e == token.tokenClass);
        return result;
    }


    private Program parseProgram() {
        parseIncludes();
        List<StructType> structs = parseStructDecls(new ArrayList<StructType>());
        List<VarDecl> varDecls = parseVarDecls(new ArrayList<VarDecl>());
        List<FunDecl> funcs = parseFunDecls(new ArrayList<FunDecl>());
        expect(TokenClass.EOF);
        return new Program(structs, varDecls, funcs);
    }

    // includes are ignored, so does not need to return an AST node
    private void parseIncludes() {
        if (accept(TokenClass.INCLUDE)) {
            nextToken();
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }

    private List<StructType> parseStructDecls(List<StructType> structDecls) {
        if (accept(TokenClass.STRUCT)) {
            // Struct declaration looks like struct IDENT "{" so look 2 tokens ahead.
            if (lookAhead(2).tokenClass != TokenClass.LBRA) {
                return structDecls; // No struct declarations present.
            }
            String structName = parseStructs();
            expect(TokenClass.LBRA);
            if (!accept(TYPES)) {
                error(token.tokenClass);
                return structDecls;
            }
            List<VarDecl> varDecls = parseVarDecls(new ArrayList<VarDecl>());
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);

            // Struct name and list of var decls is now available.
            System.out.println(structName);
            StructType structType = new StructType(structName, varDecls);
            structDecls.add(structType);
            parseStructDecls(structDecls);
        }
        return structDecls;
    }

    private String parseStructs() {
        expect(TokenClass.STRUCT);
        String structName = token.data;
        expect(TokenClass.IDENTIFIER);
        return structName;
    }

    private List<VarDecl> parseVarDecls(List<VarDecl> varDecls) {
        if (accept(TYPES)) {
            if (lookAhead(2).tokenClass == TokenClass.LPAR || lookAhead(3).tokenClass == TokenClass.LPAR || lookAhead(4).tokenClass == TokenClass.LPAR) {
                return varDecls;
            }

            Type varDeclType = parseTypes();

            String varName = token.data;
            expect(TokenClass.IDENTIFIER);
            if (accept(TokenClass.LSBR)) {
                // This is not a base type but ArrayType.
                nextToken();
                varDeclType = new ArrayType(varDeclType, Integer.parseInt(token.data));
                expect(TokenClass.INT_LITERAL);
                expect(TokenClass.RSBR);
            }
            expect(TokenClass.SC);

            VarDecl varDecl = new VarDecl(varDeclType, varName);
            varDecls.add(varDecl);
            parseVarDecls(varDecls);
        }
        return varDecls;
    }

    private Type parseTypes() {
        Type type = null;
        if (accept(TokenClass.STRUCT)) {
            String structName = parseStructs();
            type = new StructType(structName, new ArrayList<VarDecl>());
        } else {
            if (token.tokenClass == TokenClass.INT) {
                type = INT;
            } else if (token.tokenClass == TokenClass.CHAR) {
                type = BaseType.CHAR;
            } else if (token.tokenClass == TokenClass.VOID) {
                type = BaseType.VOID;
            }
            nextToken();
        }

        if (accept(TokenClass.ASTERIX)) {
            // TODO: This is a pointer to base type. Do something here.
            type = new PointerType(type);
            nextToken();
        }
        return type;
    }

    private List<FunDecl> parseFunDecls(List<FunDecl> funcDecls) {
        if (accept(TYPES)) {
            Type type = parseTypes();
            String funcName = token.data;
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);
            List<VarDecl> funcParams = parseParams(new ArrayList<VarDecl>());
            expect(TokenClass.RPAR);
            Block funDeclBlock = parseBlock();
            FunDecl funDecl = new FunDecl(type, funcName, funcParams, funDeclBlock);
            funcDecls.add(funDecl);
            parseFunDecls(funcDecls);
        }
        return funcDecls;
    }

    private List<VarDecl> parseParams(List<VarDecl> functionParams) {
        if (accept(TYPES)) {
            Type type = parseTypes();
            String firstParamName = token.data;
            expect(TokenClass.IDENTIFIER);
            VarDecl funcParam = new VarDecl(type, firstParamName);
            functionParams = parseSubsequentParams(new ArrayList<VarDecl>());
            functionParams.add(0, funcParam);
        }
        return functionParams;
    }

    private List<VarDecl> parseSubsequentParams(List<VarDecl> restOfParams) {
        if (accept(TokenClass.COMMA)) {
            nextToken();
            Type type = parseTypes();
            String paramName = token.data;
            expect(TokenClass.IDENTIFIER);

            VarDecl funcParam = new VarDecl(type, paramName);
            restOfParams.add(funcParam);
            parseSubsequentParams(restOfParams);
        }
        return restOfParams;
    }

    private Block parseBlock() {
        expect(TokenClass.LBRA);
        List<VarDecl> functionBlockVarDecls = parseVarDecls1(new ArrayList<VarDecl>());
        List<Stmt> functionBlockStatements = parseStmt();
        expect(TokenClass.RBRA);
        return new Block(functionBlockVarDecls, functionBlockStatements);
    }

    private List<VarDecl> parseVarDecls1(List<VarDecl> functionBlockVarDecls) {
        if (accept(TYPES)) {
            Type varDeclType = parseTypes();

            String varName = token.data;

            expect(TokenClass.IDENTIFIER);
            if (accept(TokenClass.LSBR)) {
                nextToken();
                varDeclType = new ArrayType(varDeclType, Integer.parseInt(token.data));
                expect(TokenClass.INT_LITERAL);
                expect(TokenClass.RSBR);
            }
            expect(TokenClass.SC);

            VarDecl varDecl = new VarDecl(varDeclType, varName);
            functionBlockVarDecls.add(varDecl);
            parseVarDecls1(functionBlockVarDecls);
        }
        return functionBlockVarDecls;
    }

    private List<Stmt> parseStmt() {
        List<Stmt> blockStatements = new ArrayList<>();
        while (!accept(TokenClass.RBRA) && !accept(TokenClass.EOF)) {
            Stmt stmtsResult = parseStmts();
            blockStatements.add(stmtsResult);
        }
        return blockStatements;
    }

    private Stmt parseStmts() {
        if (accept(TokenClass.WHILE)) {
            nextToken();
            expect(TokenClass.LPAR);
            if (accept(TokenClass.RPAR)) {
                // Empty condition , invalid program.
                error(token.tokenClass);
                nextToken();
                return null;
            }
            Expr whileConditionExpr = parseExpr();
            expect(TokenClass.RPAR);
            if (accept(TokenClass.RBRA)) {
                // Premature end of while. Block ended.
                error(token.tokenClass);
                return null;
            }
            Stmt stmt = parseStmts();
            return new While(whileConditionExpr, stmt);
        } else if (accept(TokenClass.IF)) {
            nextToken();
            expect(TokenClass.LPAR);
            if (accept(TokenClass.RPAR)) {
                // Empty condition for while loop, invalid program.
                error(token.tokenClass);
                nextToken();
                return null;
            }
            Expr ifConditionExpr = parseExpr();
            expect(TokenClass.RPAR);
            if (accept(TokenClass.RBRA)) {
                // Premature end of while. Block ended.
                error(token.tokenClass);
                return null;
            }
            Stmt ifStatement = parseStmts();
            Stmt elseStatement = null;
            if (accept(TokenClass.ELSE)) {
                nextToken();
                if (accept(TokenClass.RBRA)) {
                    // Premature end of while. Block ended.
                    error(token.tokenClass);
                    return null;
                }
                elseStatement = parseStmts();
            }
            return new If(ifConditionExpr, ifStatement, elseStatement);
        } else if (accept(TokenClass.RETURN)) {
            nextToken();
            if (accept(TokenClass.SC)) {
                // Return with no exp.
                nextToken();
                return new Return(null);
            }
            Expr returnExpResult = parseExpr();
            expect(TokenClass.SC);

            return new Return(returnExpResult);
        } else if (accept(TokenClass.LBRA)) {
            Block blk = parseBlock();
            return blk;
        } else if (accept(TokenClass.RBRA)) {
            return null; // TODO: wrong
        } else {
            Expr exprResult = parseExpr();
            Expr assignmentExpr = null;
            if (accept(TokenClass.ASSIGN)) {
                nextToken();
                assignmentExpr = parseExpr();
            }
            expect(TokenClass.SC);
            if (assignmentExpr == null) {
                return new ExprStmt(exprResult);
            }
            return new Assign(exprResult, assignmentExpr);
        }
    }

    private Expr parseExpr() {
        Expr logicalOrExpResult = parseLogicalOrExp();
        Expr logicalOrExpOpResult = parseLogicalOrExpOp(logicalOrExpResult);
        return logicalOrExpOpResult;
    }

    private Expr parseLogicalOrExpOp(Expr lhs) {
        Expr logicalOrExpOpResult = lhs;
        while (accept(TokenClass.OR)) {
            Token current = token;
            nextToken();
            Expr rhs = parseLogicalOrExp();
            logicalOrExpOpResult = new BinOp(lhs, convertOperator(current), rhs);
            lhs = logicalOrExpOpResult;
        }
        return logicalOrExpOpResult;
    }

    private Expr parseLogicalOrExp() {
        Expr logicalAndExpResult = parseLogicalAndExp();
        Expr logicalAndExpOpResult = parseLogicalAndExpOp(logicalAndExpResult);
        return logicalAndExpOpResult;
    }

    private Expr parseLogicalAndExpOp(Expr lhs) {
        Expr logicalAndExpOpResult = lhs;
        while (accept(TokenClass.AND)) {
            Token current = token;
            nextToken();
            Expr rhs = parseLogicalAndExp();
            logicalAndExpOpResult = new BinOp(lhs, convertOperator(current), rhs);
            lhs = logicalAndExpOpResult;
        }
        return logicalAndExpOpResult;
    }

    private Expr parseLogicalAndExp() {
        Expr eqExpResult = parseEqExp();
        Expr eqExpOpResult = parseEqExpOp(eqExpResult);
        return eqExpOpResult;
    }

    private Expr parseEqExpOp(Expr lhs) {
        Expr eqExpOpResult = lhs;
        while (accept(TokenClass.EQ, TokenClass.NE)) {
            Token current = token;
            nextToken();
            Expr rhs = parseEqExp();
            eqExpOpResult = new BinOp(lhs, convertOperator(current), rhs);
            lhs = eqExpOpResult;
        }
        return eqExpOpResult;
    }

    private Expr parseEqExp() {
        Expr valueExpResult = parseValueExp();
        Expr logicalCompExpResult = parseLogicalCompExp(valueExpResult);
        return logicalCompExpResult;
    }

    private Expr parseLogicalCompExp(Expr lhs) {
        Expr logicalCompExpResult = lhs;
        while (accept(TokenClass.LT, TokenClass.GT, TokenClass.GE, TokenClass.LE)) {
            Token current = token;
            nextToken();
            Expr rhs = parseValueExp();
            logicalCompExpResult = new BinOp(lhs, convertOperator(current), rhs);
            lhs = logicalCompExpResult;
        }
        return logicalCompExpResult;
    }

    private Expr parseValueExp() {
        Expr termExpResult = parseTermExp();
        Expr additiveExpResult = parseAdditiveExp(termExpResult);
        return additiveExpResult;
    }

    /**
     * while (accept(PLUS, MINUS)) {
     *
     *     Token current = token; // +
     *     nextToken(); // 1
     *     Expr rhs = parseExpr();
     *     Expr e = new BinOp(lhs, Op.Operator, rhs);
     *     lhs = e;
     * }
     *
     * 1 + 1 + 1 => BinOp(BinOp(1, +, 1), + , 1)
     *
     *
     */

    private Expr parseAdditiveExp(Expr lhs) {
        Expr additiveExpResult = lhs;
        while (accept(TokenClass.PLUS, TokenClass.MINUS)) {
            Token current = token;
            nextToken();
            Expr rhs = parseTermExp();
            additiveExpResult = new BinOp(lhs, convertOperator(current), rhs);
            lhs = additiveExpResult;
        }
        return additiveExpResult;
    }

    private Op convertOperator(Token current) {
        switch (current.tokenClass) {
            case OR: // ||
                return Op.OR;
            case AND: // &&
                return Op.AND;
            case EQ: // ==
                return Op.EQ;
            case NE: // !=
                return Op.NE;
            case LT: // <
                return Op.LT;
            case GT: // >
                return Op.GT;
            case LE: // <=
                return Op.LE;
            case GE: // >=
                return Op.GE;
            case MINUS: // -
                return Op.SUB;
            case PLUS: // +
                return Op.ADD;
            case ASTERIX: // *
                return Op.MUL;
            case DIV: // /
                return Op.DIV;
            case REM: // %
                return Op.MOD;
            default:
                error(token.tokenClass);
                return null;
        }
    }

    private Expr parseTermExp() {
        Expr unaryExpResult = parseUnaryExp();
        Expr mulExpResult = parseMulExp(unaryExpResult);
        return mulExpResult;
    }

    private Expr parseMulExp(Expr lhs) {
        Expr mulExpResult = lhs;
        while (accept(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM)) {
            Token current = token;
            nextToken();
            Expr rhs = parseUnaryExp();
            mulExpResult = new BinOp(lhs, convertOperator(current), rhs);
            lhs = mulExpResult;
        }
        return mulExpResult;
    }

    private Expr parseUnaryExp() {
        if (accept(TokenClass.ASTERIX)) {
            return parseValueAtExp();
        } else if (accept(TokenClass.SIZEOF)) {
            return parseSizeofExp();
        } else if (accept(TokenClass.LPAR) && isType()) {
            return parseTypecastExp();
        } else {
            return parseFieldaccessExp();
        }
    }

    private boolean isType() {
        TokenClass look = lookAhead(1).tokenClass;
        return look == TokenClass.INT ||
                look == TokenClass.VOID ||
                look == TokenClass.CHAR ||
                look == TokenClass.STRUCT;
    }

    private Expr parseValueAtExp() {
        if (accept(TokenClass.ASTERIX)) {
            nextToken();
            Expr valueAtExpResult = parseUnaryExp();
            return new ValueAtExpr(valueAtExpResult);
        }
        error(token.tokenClass);
        return null;
    }

    private Expr parseSizeofExp() {
        if (accept(TokenClass.SIZEOF)) {
            nextToken();
            expect(TokenClass.LPAR);
            Type sizeOfType = parseTypes();
            expect(TokenClass.RPAR);
            return new SizeOfExpr(sizeOfType);
        }
        error(token.tokenClass);
        return null;
    }

    private Expr parseTypecastExp() {
        if (accept(TokenClass.LPAR)) {
            nextToken();
            Type typecaseType = parseTypes();
            expect(TokenClass.RPAR);
            Expr typecastedExpResult = parseUnaryExp();
            return new TypecastExpr(typecaseType, typecastedExpResult);
        }
        error(token.tokenClass);
        return null;
    }

    private Expr parseFieldaccessExp() {
        Expr fieldAccessExpr = parseArrayaccessExp();
        Expr fieldAccessOpExpr = parseFieldaccessExpOp(fieldAccessExpr);
        return fieldAccessOpExpr;
    }

    private Expr parseArrayaccessExp() {
        Expr funcallExpResult = parseFuncallExp();
        Expr arrayAccessOpExpResult = parseArrayaccessExpOp(funcallExpResult);
        return arrayAccessOpExpResult;
    }

    private Expr parseFuncallExp() {
        if (accept(TokenClass.IDENTIFIER) && lookAhead(1).tokenClass == TokenClass.LPAR) {
            // This is a function call.
            Token current = token;
            nextToken();
            expect(TokenClass.LPAR);
            String functionName = current.data;
            List<Expr> functionArgs = parseArguments(new ArrayList<Expr>());
            expect(TokenClass.RPAR);
            return new FunCallExpr(functionName, functionArgs);
        } else {
            // Should be a terminal.
            return parseTerminals();
        }
    }

    private List<Expr> parseArguments(List<Expr> functionArgs) {
        if (accept(TokenClass.RPAR)) {
            return functionArgs;
        }
        functionArgs.add(parseExpr());
        parseRestOfArgs(functionArgs);
        return functionArgs;
    }

    private void parseRestOfArgs(List<Expr> functionArgs) {
        if (accept(TokenClass.COMMA)) {
            nextToken();
            functionArgs.add(parseExpr());
            parseRestOfArgs(functionArgs);
        }
    }

    private Expr parseFieldaccessExpOp(Expr field) {
        if (accept(TokenClass.DOT)) {
            Token current = token;
            nextToken();
            String fieldAccessName = current.data;
            expect(TokenClass.IDENTIFIER);
            return new FieldAccessExpr(field, fieldAccessName);
        }
        return field;
    }

    private Expr parseArrayaccessExpOp(Expr arrayName) {
        if (accept(TokenClass.LSBR)) {
            nextToken();
            Expr arrayIndex = parseExpr();
            expect(TokenClass.RSBR);
            return new ArrayAccessExpr(arrayName, arrayIndex);
        }
        return arrayName;
    }

    private Expr parseTerminals() {
        if (accept(TokenClass.LPAR)) {
            nextToken();
            Expr exp = parseExpr();
            expect(TokenClass.RPAR);
            return exp;
        } else if (accept(TokenClass.MINUS)) {
            nextToken();
            if (accept(TokenClass.IDENTIFIER)) {
                Token current = token;
                nextToken();
                String identifierName = current.data;
                return new BinOp(new IntLiteral(0), Op.SUB, new VarExpr(identifierName));
            } else if (accept(TokenClass.INT_LITERAL)) {
                Token current = token;
                nextToken();
                int intLiteral = Integer.parseInt(current.data);
                return new BinOp(new IntLiteral(0), Op.SUB, new IntLiteral(intLiteral));
            }
            error(token.tokenClass);
            return null;
        } else {
            Expr terminalExpr = extractTerminal();
            nextToken();
            return terminalExpr;
        }
    }

    private Expr extractTerminal() {
        Token current = token;
        switch (current.tokenClass) {
            case IDENTIFIER:
                return new VarExpr(current.data);
            case CHAR_LITERAL:
                return new ChrLiteral(current.data.charAt(0));
            case INT_LITERAL:
                return new IntLiteral(Integer.parseInt(current.data));
            case STRING_LITERAL:
                return new StrLiteral(current.data);
            default:
                error(token.tokenClass);
                return null;
        }
    }
}
