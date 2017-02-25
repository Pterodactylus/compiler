package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import static gen.Register.ra;
import static gen.Register.v0;

public class CodeGenerator implements ASTVisitor<Register> {

    private static final int PRINT_INTEGER = 1;
    private static final int PRINT_STRING = 4;
    private static final int READ_INTEGER = 5;
    private static final int READ_CHARACTER = 12;
    private static final int PRINT_CHARACTER = 11;
    private static final int MALLOC = 9;
    private static final int EXIT = 10;

    private static final int BYTES_FOR_SAVED_REGISTERS = 4 * Register.tmpRegs.size();
    private static final int BYTES_FOR_RA = 4;
    private static final int BYTES_FOR_OLD_FP = 4;
    private static final int BYTES_FOR_SPACE = 4;

    /*
     * Stores .text and .data segments for a MIPS program.
     * PrintWriter is only used at the very end when we write to output file
     * what is contained in two StringBuilders.
     */

    // Stores all MIPS Assembly language instructions.
    private StringBuilder textSegment;

    // Stores all MIPS Assembly language data items.
    private StringBuilder dataSegment;

    // Stores mappings for variable name -> how far relative to $fp it was allocated.
    private Map<String, Integer> map = new HashMap<>();

    private int framePtr = 0;

    // Collects all the strings
    Map<String, String> strs = new HashMap<>();

    // If, else labels.
    private int labelCount = 0;
    private int saved = 0;
    private int savedLoop = 0;
    private int stringLabelCount = 0;
    private int loops = 0;

    private int localVariablesOffset;
    int pointOfReference = 76;

    private int ifLabels = 0;
    private int elseLabels = 0;

    private int labelsInElseBlock = 0;

    private boolean m;

    private int returnCount = 0;

    /*
     * Simple register allocator.
     */

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<>();

    private List<Register> tmpRegs = new ArrayList<>();

    public CodeGenerator() {
        freeRegs.addAll(Register.tmpRegs);
        tmpRegs.addAll(Register.tmpRegs);
    }

    private class RegisterAllocationError extends Error {}

    private Register getRegister() {
        try {
            return freeRegs.pop();
        } catch (EmptyStackException ese) {
            throw new RegisterAllocationError(); // no more free registers, bad luck!
        }
    }

    private void freeRegister(Register reg) {
        freeRegs.push(reg);
    }

    private PrintWriter writer; // use this writer to output the assembly instructions


    public void emitProgram(Program program, File outputFile) throws FileNotFoundException {
        writer = new PrintWriter(outputFile);

        textSegment = new StringBuilder();
        dataSegment = new StringBuilder();

        visitProgram(program);

        // TODO: After visiting the program, write data segment followed by text segment
        // into the output file.
        writer.println(dataSegment.toString());

        // Find label main:
        writer.println(textSegment.toString());

        writer.close();
    }

    @Override
    public Register visitProgram(Program p) {

        dataSegment.append(".data" + "\n");
        dataSegment.append(".align 2\n");

        for (StructType st : p.structTypes) {
            st.accept(this);
        }

        for (VarDecl vd : p.varDecls) {
            vd.accept(this);
        }


        textSegment.append(".text" + "\n");
        textSegment.append("j main\n");

        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }


        // TODO: add malloc



        return null;
    }

    @Override
    public Register visitBaseType(BaseType bt) {
        return null;
    }

    @Override
    public Register visitStructType(StructType st) {
        return null;
    }

    @Override
    public Register visitBlock(Block b) {

        for (VarDecl vd : b.varDeclsInBlock) {
            vd.accept(this);
        }

        textSegment.append("\n");

        for (Stmt stmt : b.stmtsInBlock) {
            stmt.accept(this);
        }

        textSegment.append("\n");

        return null;
    }

    private boolean library_function(FunDecl fd) {
        if (fd.name == "print_i") {
            textSegment.append("\tlw $a0, 84($fp)\n");
            textSegment.append("\tli $v0, 1\n");
            textSegment.append("\tsyscall\n");
            return true;
        } else if (fd.name == "print_c") {
            textSegment.append("\tlw $a0, 84($fp)\n");
            textSegment.append("\tli $v0, 11\n");
            textSegment.append("\tsyscall\n");
            return true;
        } else if (fd.name == "print_s") {
            textSegment.append("\tlw $a0, 84($fp)\n");
            textSegment.append("\tli $v0, 4\n");
            textSegment.append("\tsyscall\n");
            return true;
        } else if (fd.name == "read_i") {
            textSegment.append("\tlw $a0, 84($fp)\n");
            textSegment.append("\tli $v0, 5\n");
            textSegment.append("\tsyscall\n");
            return true;
        } else if (fd.name == "read_c") {
            textSegment.append("\tlw $a0, 84($fp)\n");
            textSegment.append("\tli $v0, 12\n");
            textSegment.append("\tsyscall\n");
            return true;
        }
        return false;
    }

    @Override
    public Register visitFunDecl(FunDecl fd) {
        textSegment.append(fd.name + ":" + "\n");

        // Function arguments are pushed on the stack. Obtain them and add an offset
        // so we can access them later.
        int fpOffset = BYTES_FOR_SAVED_REGISTERS + BYTES_FOR_OLD_FP + BYTES_FOR_RA + BYTES_FOR_SPACE; // 84
        for (VarDecl vd : fd.params) {
            vd.fpOffset = fpOffset;
            int size = vd.type.getSize();
            if (size == 1) {
                fpOffset += 4;
            } else {
                fpOffset += size;
            }
        }

        System.out.println("FUNCTION NAME: " + fd.name);
        textSegment.append("# Prologue for function: " + fd.name + "\n");
        generatePrologue();
        textSegment.append("# END of Prologue for function: " + fd.name + "\n\n");

        if (fd.name.equals("main")) {
            m = true;
        } else {
            m = false;
        }
        textSegment.append("# Entering block for function: " + fd.name + "\n");
        if (!library_function(fd)) {
            visitFunctionBlock(fd.block);
        }
        textSegment.append("# Leaving block for function: " + fd.name + "\n\n");


        textSegment.append("# Epilogue for function: " + fd.name + "\n");
        if (fd.name.equals("main")) {
            generateEnding();
        } else {
            pointOfReference = 76;
            generateEpilogue();
        }
        textSegment.append("# END of Epilogue for function: " + fd.name + "\n\n");

        return null;
    }

    private void saveRegisters() {
        textSegment.append("\t# Saving temporary registers.\n\n");
        for (Register register : tmpRegs) {
            textSegment.append("\tsw " + register.toString() + ", " + "0($sp)\n");
            textSegment.append("\taddi " + "$sp, $sp, -4\n");
        }
        textSegment.append("\n\t# Done saving temporary registers.\n\n");
    }

    private void restoreRegisters() {
        textSegment.append("\t# Restoring temporary registers.\n\n");
        for (Register register : tmpRegs) {
            textSegment.append("\t lw " + register.toString() + ", " + pointOfReference + "($fp)\n");
            pointOfReference -= 4;
        }
        textSegment.append("\n\t# Done restoring temporary registers.\n\n");
    }

    public Register visitFunctionBlock(Block b) {
        for (VarDecl vd : b.varDeclsInBlock) {
            vd.accept(this);
        }

        textSegment.append("\n");


        for (Stmt stmt : b.stmtsInBlock) {
            stmt.accept(this);
        }

        textSegment.append("\n");

//        pointOfReference = 76;
//
//        restoreRegisters();
//
//        System.out.println("M: " + m);
//
//        if (!m) {
//
//            // Second: Move $fp. Old $fp is located 4 bytes above current function $fp.
//            textSegment.append("\tlw $fp, 4($fp)");
//            textSegment.append(" # Reload previous function's $fp.\n");
//
//            // The callee then have to jump to a location where caller left. It is in $ra register.
//            textSegment.append("\t" + "jr $ra");
//            textSegment.append(" # Jump back to the caller.\n");
//        }


        // Reset local variables offset tracker.
        localVariablesOffset = 0;


        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        boolean isGlobal = vd.isGlobal;
        if (isGlobal) {
            if (vd.type instanceof ArrayType) {
                // Global array declaration. Need to compute the space for array typy and multiply by array size.
                int size = vd.type.getSize();
                dataSegment.append(vd.varName + ": .space " + size + "\n");
            } else {
                // Variable declarations marked by typechecker as global are stored in data segment.
                dataSegment.append(vd.varName + ": .word 0\n");
            }
        } else {
            // Local variable declarations.
            // These are allocated on the stack in the negative offset with regards to current $fp.
            int size = vd.type.getSize();
            if (size == 1) {
                // BaseType
                size = 4;
            }
            vd.offset = localVariablesOffset;
            textSegment.append("\taddi $sp, $sp, " + (-size));
            textSegment.append(" # Allocate stack space for local variable " + vd.varName + "\n");
            localVariablesOffset += size;
        }
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        VarDecl vd = v.vd;
        boolean isGlobal = vd.isGlobal;
        boolean isParameter = vd.isParameter;
        if (isGlobal) {
            Register result = getRegister();
            // Global variables address is loaded, then it's value.
            textSegment.append("\tla " + result.toString() + ", " + vd.varName);
            textSegment.append(" # Load address for global variable " + vd.varName + "\n");
            freeRegister(result);
            result = getRegister();
            textSegment.append("\tlw " + result.toString() + ", " + "(" + result.toString() + ")");
            textSegment.append(" # Load value of global variable.\n");
            return result;
        } else if (isParameter) {
            // Parameters offset from current $fp was stored inside associated Vardecl
            Register result = getRegister();
            textSegment.append("\tlw " + result.toString() + ", " + vd.fpOffset + "($fp)");
            textSegment.append(" # Loaded parameter " + vd.varName + " off the stack into register.\n\n");
            return result;
        } else {
            // Variable is local. It's location is stored as field offset in VarDecl and was filled in
            // when visiting function block var decls. To obtain the value, we need to lookup the offset
            // and load into register offset relative to current $fp in negative direction.
            int offset = vd.offset;
            Register result = getRegister();
            textSegment.append("\tlw " + result.toString() + ", " + (-offset) + "($fp)");
            textSegment.append(" # Loaded local variable " + vd.varName + " into register.\n\n");
            return result;
        }
    }

    @Override
    public Register visitArrayType(ArrayType at) {
        return null;
    }

    @Override
    public Register visitPointerType(PointerType pt) {
        return null;
    }

    @Override
    public Register visitIntLiteral(IntLiteral il) {
        Register result = getRegister();
        textSegment.append("\t" + "li " + result.toString() + ", " + il.value + "\n");
        return result;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral cl) {
        Register result = getRegister();
        textSegment.append("\t" + "li " + result.toString() + ", " + "'" + cl.value + "'" + "\n");
        return result;
    }

    @Override
    public Register visitStrLiteral(StrLiteral sl) {
        Register result = getRegister();
        String label = "str_label_" + stringLabelCount;
        dataSegment.append(label + ": " + ".asciiz " + "\"" + sl.value + "\"" + "\n");
        stringLabelCount++;
        strs.put(sl.value, label);
        textSegment.append("\t" + "la " + result.toString() + ", " + label + "\n");
        return result;
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fce) {
        // Function call can take immediate values, global variables
        // local variables and parameters. Quite complex.
        // All arguments to the function must be placed on the stack.

        textSegment.append("\n");

        int resetBytes = 0;
        Register result;
        // Put arguments on the stack in reverse order.
        int numArguments = fce.functionArgsExprs.size();
        for (int i = numArguments - 1; i >= 0; i--) {
            result = fce.functionArgsExprs.get(i).accept(this);
            textSegment.append("\tsw " + result.toString() + ", " + "0($sp)");
            textSegment.append(" # Store the argument on the stack.\n");

            textSegment.append("\taddi $sp, $sp, -4");
            textSegment.append(" # Prepare stack for another argument\n\n");
            resetBytes += 4;
            freeRegister(result);
            System.out.println("Number of free registers left: " + freeRegs.size());
        }

        textSegment.append("\tsw $ra, 0($sp)");
        textSegment.append(" # Save return address.\n");

        textSegment.append("\taddi $sp, $sp, -4");
        textSegment.append(" # Prepare stack to enter callee.\n");


        resetBytes += 4;

        System.out.println(resetBytes);

        textSegment.append("\tjal " + fce.functionName);
        textSegment.append(" # Jump to callee.\n");

        textSegment.append("\tlw $ra, 4($sp)");
        textSegment.append(" # Reset the $ra.\n");

        // Reset stack by popping all pushed arguments. Bytes are stored in resetBytes.
        textSegment.append("\taddi $sp, $sp, " + resetBytes);
        textSegment.append(" # Pop the arguments pushed on the stack.\n");

        // Move the result of function call from return register $vo.
        if (!fce.fd.name.startsWith("print")) {
            System.out.println("Function: " + fce.fd.name + " type: " + fce.fd.type);
            result = getRegister();
            textSegment.append("\tmove " + result.toString() + ", " + "$v0");
            textSegment.append(" # Move function call result to a register to use by the caller.\n");
            return result;
        }

        return null;
    }

    @Override
    public Register visitBinOpExpr(BinOp binOp) {
        Register lhs = binOp.lhs.accept(this);
        Register rhs = binOp.rhs.accept(this);
        Register result = getRegister();

        switch (binOp.op) {
            case ADD:
                textSegment.append("\t" + "add " + result.toString() + ", " + lhs.toString() + ", " + rhs.toString() + "\n");
                break;
            case SUB:
                textSegment.append("\t" + "sub " + result.toString() + ", " + lhs.toString() + ", " + rhs.toString() + "\n");
                break;
            case DIV:
                textSegment.append("\t" + "div " + lhs.toString() + ", " + rhs.toString() + "\n");
                textSegment.append("\t" + "mflo " + result.toString() + "\n");
                break;
            case MUL:
                textSegment.append("\t" + "mul " + result.toString() + ", " + lhs.toString() + ", " + rhs.toString() + "\n");
                break;
            case MOD:
                textSegment.append("\t" + "div " + lhs.toString() + ", " + rhs.toString() + "\n");
                textSegment.append("\t" + "mfhi " + result.toString() + "\n");
                break;
            case GE:
                textSegment.append("\t" + "blt " + lhs.toString() + ", " + rhs.toString());
                result = null;
                break;
            case LE:
                textSegment.append("\t" + "bgt " + lhs.toString() + ", " + rhs.toString());
                freeRegister(result);
                result = null;
                break;
            case GT:
                textSegment.append("\t" + "ble " + lhs.toString() + ", " + rhs.toString());
                freeRegister(result);
                result = null;
                break;
            case LT:
                textSegment.append("\t" + "bge " + lhs.toString() + ", " + rhs.toString());
                freeRegister(result);
                result = null;
                break;
            case NE:
                textSegment.append("\t" + "beq " + lhs.toString() + ", " + rhs.toString());
                freeRegister(result);
                result = null;
                break;
            case EQ:
                textSegment.append("\t" + "bne " + lhs.toString() + ", " + rhs.toString());
                freeRegister(result);
                result = null;
                break;
            default:
                result = null;
        }
        freeRegister(lhs);
        freeRegister(rhs);
        return result;
    }

    @Override
    public Register visitStmtExpr(Stmt stmt) {
        return null;
    }

    @Override
    public Register visitIfStmt(If ifStmt) {
        int saved = labelCount;
        labelCount++;
        Register condition = ifStmt.ifConditionExpr.accept(this);

        if (condition != null) {
            textSegment.append("\t" + "beq " + condition.toString() + ", " + "$0");
        }

        textSegment.append(", else_" + saved + "\n");

        ifStmt.ifStmtBlock.accept(this);


        textSegment.append("\t" + "j " + "if_continue_" + saved + "\n");
        textSegment.append("else_" + (saved) + ":\n");

        int p = saved;

        if (ifStmt.elseStmtBlock != null) {
            ifStmt.elseStmtBlock.accept(this);
        }

        textSegment.append("if_continue_" + (p) + ":\n");


        return null;
    }

    @Override
    public Register visitWhileStmt(While whileStmt) {
        int saved = loops;
        loops++;
        textSegment.append("\tb end_loop_" + saved + "\n");

        textSegment.append("loop_" + saved + ":\n");

        whileStmt.whileStmt.accept(this);


        textSegment.append("end_loop_" + saved + ":" + "\n");

        Register condition = whileStmt.whileConditionExpr.accept(this);

        if (condition != null) {
            textSegment.append("\t" + "beq " + condition.toString() + ", " + "$0");
        }

        textSegment.append(", loop_exit_" + saved + "\n");
        textSegment.append("\t j loop_" +saved + "\n");
        textSegment.append("loop_exit_" + saved + ":\n");

        return null;
    }

    @Override
    public Register visitAssignStmt(Assign assignStmt) {
        Register rhs = assignStmt.rhs.accept(this);
        if (assignStmt.lhs instanceof VarExpr) {
            // Var expression can be global, local.
            VarDecl vd = ((VarExpr) assignStmt.lhs).vd;
            boolean isGlobal = vd.isGlobal;
            boolean isParameter = vd.isParameter;
            if (isGlobal) {
                Register address = getRegister();
                textSegment.append("\tla " + address.toString() + ", " + vd.varName);
                textSegment.append(" # Load address of global variable.\n");
                textSegment.append("\tsw " + rhs.toString() + ", " + "(" + address.toString() + ")");
                textSegment.append(" # Store value to global variable.\n");
                freeRegister(address);
                freeRegister(rhs);
            } else if (isParameter) {
                int fpOffset = vd.fpOffset;
                textSegment.append("\tsw " + rhs.toString() + ", " + (fpOffset) + "($fp)");
                textSegment.append(" # Store value into function parameter " + vd.varName  + "\n");
                freeRegister(rhs);
            } else {
                // If lhs is local, then obtain the offset from current $fp.
                int offset = vd.offset;
                // Store the rhs register into the offset from $fp.
                textSegment.append("\tsw " + rhs.toString() + ", " + (-offset) + "($fp)");
                textSegment.append(" # Store register for local variable " + vd.varName + "\n");
                freeRegister(rhs);
            }
        }
        return null;
    }

    @Override
    public Register visitReturnStmt(Return returnStmt) {
        if (returnStmt.returnExpr != null) {
            Register tmp = returnStmt.returnExpr.accept(this);
            textSegment.append("\taddi $sp, $fp, 76\n");
            textSegment.append("\t" + "move " + v0.toString() + ", " + tmp.toString() + "\n");
            freeRegister(tmp);
        }

        pointOfReference = 76;
        restoreRegisters();
        // Second: Move $fp. Old $fp is located 4 bytes above current function $fp.
        textSegment.append("\tlw $fp, 4($fp)");
        textSegment.append(" # Reload previous function's $fp.\n");

        // The callee then have to jump to a location where caller left. It is in $ra register.
        textSegment.append("\t" + "jr $ra");
        textSegment.append(" # Jump back to the caller.\n");
        return null;
    }

    @Override
    public Register visitExprStmt(ExprStmt exprStmt) {
        exprStmt.exprStmt.accept(this);
        return null;
    }

    @Override
    public Register visitArrayAccessExpr(ArrayAccessExpr arrayAccessExpr) {
        return null;
    }

    @Override
    public Register visitSizeOfExpr(SizeOfExpr sizeOfExpr) {
        return null;
    }

    @Override
    public Register visitValueAtExpr(ValueAtExpr valueAtExpr) {
        return null;
    }

    @Override
    public Register visitTypecastExpr(TypecastExpr typecastExpr) {
        Register result = typecastExpr.typecastedExpr.accept(this);
        return result;
    }

    @Override
    public Register visitFieldAccessExpr(FieldAccessExpr fieldAccessExpr) {
        return null;
    }

    private void generatePrologue() {

        saveRegisters();

        // After registers are saved, store old frame pointer.
        textSegment.append("\t" + "sw $fp, 0($sp)");
        textSegment.append(" # Saving old $fp.\n");

        // Prepare current stack frame for new $fp
        textSegment.append("\t" + "addi $sp, $sp, -4");
        textSegment.append(" # Decrement $sp to prepare it for new $fp.\n");

        // Set current $fp to $sp because everything up belongs to previous function(the caller).
        textSegment.append("\t" + "move $fp, $sp");
        textSegment.append(" # Set current $fp to $sp and start enter the callee.\n");
    }


    private void generateEpilogue() {
        textSegment.append("return_" + returnCount + ":\n");
        returnCount++;
        restoreRegisters();

        // When function reached the end of block, we need to restore the stack.

        // First: pop old registers and old $fp. Simply move $sp appropriate amount.
        textSegment.append("\taddi " + "$sp, $fp, " + (BYTES_FOR_OLD_FP + BYTES_FOR_SAVED_REGISTERS));
        textSegment.append(" # Popped temporary registers and olf $fp\n");

        // Second: Move $fp. Old $fp is located 4 bytes above current function $fp.
        textSegment.append("\tlw $fp, 4($fp)");
        textSegment.append(" # Reload previous function's $fp.\n");

        // The callee then have to jump to a location where caller left. It is in $ra register.
        textSegment.append("\t" + "jr $ra");
        textSegment.append(" # Jump back to the caller.\n");
    }

    private void includeLibraryFunction(String functionName, int code) {
        textSegment.append(functionName + ":" + "\n");
        textSegment.append("\t" + "li " + v0.toString() + ", " + code + "\n");
        textSegment.append("\t" + "syscall" + "\n");
        textSegment.append("\t" + "jr " + ra.toString() + "\n");
        textSegment.append("\n");
    }

    private void generateEnding() {
        textSegment.append("\t" + "li " + v0.toString() + ", " + EXIT + "\n");
        textSegment.append("\t" + "syscall" + "\n");
        textSegment.append("\n");
    }
}