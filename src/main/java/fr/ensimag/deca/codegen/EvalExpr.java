package fr.ensimag.deca.codegen;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.tree.AbstractBinaryExpr;
import fr.ensimag.deca.tree.AbstractDeclVar;
import fr.ensimag.deca.tree.AbstractExpr;
import fr.ensimag.deca.tree.AbstractInitialization;
import fr.ensimag.deca.tree.AbstractOpArith;
import fr.ensimag.deca.tree.Assign;
import fr.ensimag.deca.tree.BooleanLiteral;
import fr.ensimag.deca.tree.DeclVar;
import fr.ensimag.deca.tree.Identifier;
import fr.ensimag.deca.tree.IntLiteral;
import fr.ensimag.ima.pseudocode.BinaryInstruction;
import fr.ensimag.ima.pseudocode.BinaryInstructionDValToReg;
import fr.ensimag.ima.pseudocode.DAddr;
import fr.ensimag.ima.pseudocode.DVal;
import fr.ensimag.ima.pseudocode.GPRegister;
import fr.ensimag.ima.pseudocode.ImmediateInteger;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.RegisterOffset;
import fr.ensimag.ima.pseudocode.instructions.*;
import fr.ensimag.deca.tools.DecacInternalError;

/**
 * Class for the evaluation of any expression
 */
public class EvalExpr {

    /**
     * Write assembly instructions to put the value of the register in the stack
     */
    public static void assignVariable(DecacCompiler compiler, DAddr op) {
        compiler.addInstruction(new STORE(Register.getR(compiler.getCurrentRegister()), op));
    }

    public static void mnemo(DecacCompiler compiler, AbstractBinaryExpr op, DVal dval, GPRegister reg) {
        switch (op.getOperator()) {
            case "+" :
                compiler.addInstruction(new ADD(dval, reg));
                break;
            case "-" :
                compiler.addInstruction(new SUB(dval, reg));
                break;
            case "*" :
                int powMul = dval.powerOfTwo();
                if (powMul < 0) {
                    compiler.addInstruction(new MUL(dval, reg));
                } else {
                    for (int i = 0; i < powMul; i++) {
                        compiler.addInstruction(new SHL(reg));
                    }
                }
                break;
            case "/" :
                if (op.getType().isInt()) {
                    int powQuo = dval.powerOfTwo();
                    if (powQuo < 0) {
                        compiler.addInstruction(new QUO(dval, reg));
                        if (!compiler.getCompilerOptions().getNoCheck()) {
                            Error.instanceError(compiler, "division_par_zero");
                        }
                    } else {
                        for (int i = 0; i < powQuo; i++) {
                            compiler.addInstruction(new SHR(reg));
                        }
                    }
                } else {
                    compiler.addInstruction(new DIV(dval, reg));
                }
                break;
            case "%" :
                // Reste entier
                int powRem = dval.powerOfTwo();
                if (powRem < 0) {
                    compiler.addInstruction(new REM(dval, reg));
                    if (!compiler.getCompilerOptions().getNoCheck()) {
                        Error.instanceError(compiler, "division_par_zero");
                    }
                } else { // Reste d'une division par une puissance de 2
                    // On sauvegarde le dividende dans reg
                    compiler.addInstruction(new LOAD(reg, Register.R1));
                    // On calcule quotient * diviseur dans R1
                    for (int i = 0; i < powRem; i++) {
                        compiler.addInstruction(new SHR(Register.R1));
                    }
                    for (int i = 0; i < powRem; i++) {
                        compiler.addInstruction(new SHL(Register.R1));
                    }
                    // On soustrait dans reg : on a le reste
                    compiler.addInstruction(new SUB(Register.R1, reg));
                }
                break;
            case "=" :
                // Assign, le contexte devrait empecher l'exception
                try {
                    compiler.addInstruction(new STORE(reg, (DAddr) dval));
                } catch (ClassCastException e) {
                    throw new DecacInternalError(
                            "DVal "
                                    + dval.toString()
                                    + " is not a DAddr, you can't assign it");
                }
                break;
            case "==" :
                compiler.addInstruction(new CMP(dval, reg));
                break;
            case "!=" :
                compiler.addInstruction(new CMP(dval, reg));
                break;
            case ">" :
                compiler.addInstruction(new CMP(dval, reg));
                break;
            case "<" :
                compiler.addInstruction(new CMP(dval, reg));
                break;
            case ">=" :
                compiler.addInstruction(new CMP(dval, reg));
                break;
            case "<=" :
                compiler.addInstruction(new CMP(dval, reg));
                break;
            default :
                throw new UnsupportedOperationException("unsupported operation");
        }
        if (op.getType().isFloat() && !op.getOperator().equals("=") &&!compiler.getCompilerOptions().getNoCheck()) {
            // Gestion des erreurs liés au calcul flottant
            Error.instanceError(compiler, "debordement_flottant");
        }
    }


    /**
     * Generate code to put the boolean value of expr into the current register
     */
    public static void boolInRegister(DecacCompiler compiler, AbstractExpr expr) {
        Label faux = new Label("Assign_False." + compiler.getNbLabel());
        Label fin = new Label("Assign_Fin." + compiler.getNbLabel());
        expr.boolCodeExpr(compiler, false, faux);
        // L'expression est vrai
        compiler.addInstruction(new LOAD(1, Register.getR(compiler.getCurrentRegister())));
        compiler.addInstruction(new BRA(fin));
        // L'expression est fausse
        compiler.addLabel(faux);
        compiler.addInstruction(new LOAD(0, Register.getR(compiler.getCurrentRegister())));
        compiler.addLabel(fin);
        compiler.incrNbLabel();
    }
}