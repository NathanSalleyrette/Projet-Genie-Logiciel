package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;

import fr.ensimag.ima.pseudocode.BranchInstruction;
import fr.ensimag.ima.pseudocode.instructions.*;

/**
 *
 * @author gl01
 * @date 01/01/2021
 */
public abstract class AbstractOpCmp extends AbstractBinaryExpr {

    public AbstractOpCmp(AbstractExpr leftOperand, AbstractExpr rightOperand) {
        super(leftOperand, rightOperand);
    }

    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {
    	AbstractExpr leftOp = this.getLeftOperand();
    	AbstractExpr rightOp = this.getRightOperand();
    	Type leftType = leftOp.verifyExpr(compiler, localEnv, currentClass);
    	Type rightType = rightOp.verifyExpr(compiler, localEnv, currentClass);
    	// Au moins je suis sûr de récupérer le type Boolean comme ça
		Type returnType = compiler.getEnvType().get(compiler.getSymbTb().create("boolean")).getType();
		
		if (leftType.isInt()) {
			if (rightType.isFloat()) {
				ConvFloat conv = new ConvFloat(leftOp);
				this.setLeftOperand(conv);
				conv.verifyExpr(compiler, localEnv, currentClass);
				this.setType(returnType);
    			return returnType;
			} else if (rightType.isInt()) {
				this.setType(returnType);
    			return returnType;
			}
		} else if (leftType.isFloat()) {
			if (rightType.isInt()) {
				ConvFloat conv = new ConvFloat(rightOp);
				conv.verifyExpr(compiler, localEnv, currentClass);
				this.setRightOperand(conv);
				this.setType(returnType);
    			return returnType;
			} else if (rightType.isFloat()) {
				this.setType(returnType);
    			return returnType;
			}
		}  	
        throw new ContextualError(this.typesOp() + ". Attendu: 'int' ou 'float'", this.getLocation());
    }

    @Override
    protected void boolCodeGen(DecacCompiler compiler, boolean branch, Label tag) {
        // Evaluation des expressions et CMP
        this.codeGenInst(compiler);
        // Branchement
        BranchInstruction branchInstruction;
        switch (this.getOperatorName()) {
            case "==" :
                if (branch) branchInstruction = new BEQ(tag);
                else branchInstruction = new BNE(tag);
                break;
            case "!=" :
                if (branch) branchInstruction = new BNE(tag);
                else branchInstruction = new BEQ(tag);
                break;
            case ">" :
                if (branch) branchInstruction = new BGT(tag);
                else branchInstruction = new BLE(tag);
                break;
            case "<" :
                if (branch) branchInstruction = new BLT(tag);
                else branchInstruction = new BGE(tag);
                break;
            case ">=" :
                if (branch) branchInstruction = new BGE(tag);
                else branchInstruction = new BLT(tag);
                break;
            case "<=" :
                if (branch) branchInstruction = new BLE(tag);
                else branchInstruction = new BGT(tag);
                break;
            default :
                throw new UnsupportedOperationException("unsupported operator");
        }
        compiler.addInstruction(branchInstruction);
    }
}

