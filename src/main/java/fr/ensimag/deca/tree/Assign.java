package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.codegen.EvalExpr;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.Definition;
import fr.ensimag.deca.context.EnvironmentExp;

import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.DVal;
import fr.ensimag.ima.pseudocode.instructions.LOAD;
import fr.ensimag.ima.pseudocode.instructions.STORE;
import fr.ensimag.ima.pseudocode.instructions.BRA;
import fr.ensimag.ima.pseudocode.instructions.PUSH;
import fr.ensimag.ima.pseudocode.instructions.POP;
import fr.ensimag.ima.pseudocode.instructions.CMP;
import fr.ensimag.ima.pseudocode.instructions.BNE;
import fr.ensimag.ima.pseudocode.instructions.BEQ;

/**
 * Assignment, i.e. lvalue = expr.
 *
 * @author gl01
 * @date 01/01/2021
 */
public class Assign extends AbstractBinaryExpr {

    @Override
    public AbstractLValue getLeftOperand() {
        // The cast succeeds by construction, as the leftOperand has been set
        // as an AbstractLValue by the constructor.
        return (AbstractLValue)super.getLeftOperand();
    }

    public Assign(AbstractLValue leftOperand, AbstractExpr rightOperand) {
        super(leftOperand, rightOperand);
    }

    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {
        Type typeAssign = this.getLeftOperand().verifyExpr(compiler, localEnv, currentClass);
        this.getRightOperand().verifyRValue(compiler, localEnv, currentClass, typeAssign);
        if ((this.getLeftOperand().getType().isFloat()) && (this.getRightOperand().getType().isInt())) {
        	ConvFloat conv = new ConvFloat(this.getRightOperand());
        	this.setRightOperand(conv);
        	conv.verifyExpr(compiler, localEnv, currentClass);
        }
        this.setType(typeAssign);
        return typeAssign;
    }

    @Override
    protected void codeGenInst(DecacCompiler compiler) {
        if (this.getType().isBoolean()) {
            EvalExpr.boolInRegister(compiler, this.getRightOperand());
        } else {
            this.getRightOperand().codeGenInst(compiler);   
        }
        this.setLeftOperand(this.getLeftOperand().checkSelection());
        if (!this.getLeftOperand().isSelection()) {
            EvalExpr.mnemo(compiler, this, this.getLeftOperand().dval(compiler), Register.getR(compiler.getCurrentRegister()));
        } else {
            // Il faut gérer les registres
            DVal dVal = null;
            if (compiler.getCurrentRegister() < compiler.getCompilerOptions().getRMAX()) {
                compiler.incrCurrentRegister();
                dVal = this.getLeftOperand().dval(compiler);
				EvalExpr.mnemo(compiler, this, dVal, Register.getR(compiler.getCurrentRegister()-1));
				compiler.decrCurrentRegister();
			} else {
                // Plus assez de registres libres
                compiler.incrNbTemp();
                compiler.addInstruction(new PUSH(Register.getR(compiler.getCurrentRegister()))); // sauvegarde
                dVal = this.getLeftOperand().dval(compiler);
                compiler.addInstruction(new POP(Register.R0)); // restauration, On peut utiliser R0 car aucune méthode ne peut être appelée ensuite
                compiler.decrNbTemp();                         // Celà nous empêchait de faire un LOAD au lieu de PUSH/POP
                EvalExpr.mnemo(compiler, this, dVal, Register.R0);
            }
            compiler.addInstruction(new LOAD(dVal, Register.getR(compiler.getCurrentRegister())));
        }
    }

    @Override
    protected void boolCodeGen(DecacCompiler compiler, boolean branch, Label tag) {
        this.codeGenInst(compiler);
        compiler.addInstruction(new LOAD(this.getLeftOperand().dval(compiler), Register.R0));
        compiler.addInstruction(new CMP(0, Register.R0));
        if (branch) {
            compiler.addInstruction(new BNE(tag));
        } else {
            compiler.addInstruction(new BEQ(tag));
        }
    }

    @Override
    protected String getOperatorName() {
        return "=";
    }

}
