package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tools.IndentPrintStream;

import fr.ensimag.ima.pseudocode.DVal;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.instructions.LOAD;

import java.io.PrintStream;
import org.apache.commons.lang.Validate;

/**
 * Expression, i.e. anything that has a value.
 *
 * @author gl01
 * @date 01/01/2021
 */
public abstract class AbstractExpr extends AbstractInst {
    /**
     * @return true if the expression does not correspond to any concrete token
     * in the source code (and should be decompiled to the empty string).
     */
    boolean isImplicit() {
        return false;
    }

    /**
     * Get the type decoration associated to this expression (i.e. the type computed by contextual verification).
     */
    public Type getType() {
        return type;
    }

    protected void setType(Type type) {
        Validate.notNull(type);
        this.type = type;
    }
    private Type type;

    @Override
    protected void checkDecoration() {
        if (getType() == null) {
            throw new DecacInternalError("Expression " + decompile() + " has no Type decoration");
        }
    }

    /**
     * Verify the expression for contextual error.
     * 
     * implements non-terminals "expr" and "lvalue" 
     *    of [SyntaxeContextuelle] in pass 3
     *
     * @param compiler  (contains the "env_types" attribute)
     * @param localEnv
     *            Environment in which the expression should be checked
     *            (corresponds to the "env_exp" attribute)
     * @param currentClass
     *            Definition of the class containing the expression
     *            (corresponds to the "class" attribute)
     *             is null in the main bloc.
     * @return the Type of the expression
     *            (corresponds to the "type" attribute)
     */
    public abstract Type verifyExpr(DecacCompiler compiler,
            EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError;

    public boolean assignCompatible(Type typeLeft, Type typeRight) throws ContextualError{
    	if (typeRight.isSubTypeOf(typeLeft, getLocation())) {
    		return true;
    	}
    	if (typeLeft.isClass()) {
    		if (typeLeft == typeRight) {
    			return true;
    		} else {
    			return false;
    		}
    	}
    	if (typeLeft.sameType(typeRight)) {
    		return true;
    	}
    	return false;
    }
    /**
     * Verify the expression in right hand-side of (implicit) assignments 
     * 
     * implements non-terminal "rvalue" of [SyntaxeContextuelle] in pass 3
     *
     * @param compiler  contains the "env_types" attribute
     * @param localEnv corresponds to the "env_exp" attribute
     * @param currentClass corresponds to the "class" attribute
     * @param expectedType corresponds to the "type1" attribute            
     * @return this with an additional ConvFloat if needed...
     */
    public AbstractExpr verifyRValue(DecacCompiler compiler,
            EnvironmentExp localEnv, ClassDefinition currentClass, 
            Type expectedType)
            throws ContextualError {
        Type type2 = verifyExpr(compiler, localEnv, currentClass);
        if (assignCompatible(expectedType, type2)){
        	if ((expectedType.isFloat()) && (type2.isInt())) {
        		ConvFloat conv = new ConvFloat(this);
        		conv.verifyExpr(compiler, localEnv, currentClass);
        		return conv;
        	}
        	return this;
        }
        throw new ContextualError("(3.28) Les types " + expectedType.toString() +
        		" et " + type2.toString() + " sont incompatibles pour l'affectation", this.getLocation());        
    }
    
    @Override
    protected void verifyInst(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass, Type returnType)
            throws ContextualError {
        verifyExpr(compiler, localEnv, currentClass);
    }

    /**
     * Verify the expression as a condition, i.e. check that the type is
     * boolean.
     *
     * @param localEnv
     *            Environment in which the condition should be checked.
     * @param currentClass
     *            Definition of the class containing the expression, or null in
     *            the main program.
     */
    void verifyCondition(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {
        Type returnedType = this.verifyExpr(compiler, localEnv, currentClass);
        if (!returnedType.isBoolean()) {
        	throw new ContextualError("(3.29) Type de l'expression : " + returnedType.toString() +
        			", attendu : 'boolean' pour une condition", this.getLocation());
        }
    }

    /**
     * Generate code to print the expression
     *
     * @param compiler
     */
    protected void codeGenPrint(DecacCompiler compiler) {
        this.codeGenInst(compiler);
        compiler.addInstruction(new LOAD(Register.getR(compiler.getCurrentRegister()), Register.R1));
    }

    @Override
    protected void codeGenInst(DecacCompiler compiler) {
        if (!type.isBoolean()) { // Pour un booleen l'appel de cette fonction est inutile (expression orpheline)
            throw new UnsupportedOperationException("not yet implemented");
        }
    }
    
     /**
     * Write assembley code for boolean expressions
     */
    protected void boolCodeGen(DecacCompiler compiler, boolean branch, Label tag) {
        throw new UnsupportedOperationException("not yet implemented");
    }
    /**
     * @return the assembly expression of the atomic expression in argument
     */
    public DVal dval(DecacCompiler compiler) {
        return Register.getR(compiler.getCurrentRegister());
    };

    /**
     * Generate code to put the result of the expression in the current register
     * @param compiler
     */
    public void codeGenExpr(DecacCompiler compiler) {
        this.codeGenInst(compiler);
    }

    public void boolCodeExpr(DecacCompiler compiler, boolean branch, Label tag) {
        this.boolCodeGen(compiler, branch, tag);
    }


    @Override
    protected void decompileInst(IndentPrintStream s) {
        decompile(s);
        s.print(";");
    }

    @Override
    protected void prettyPrintType(PrintStream s, String prefix) {
        Type t = getType();
        if (t != null) {
            s.print(prefix);
            s.print("type: ");
            s.print(t);
            s.println();
        }
    }
    
    /**
     * Regarde si l'identifiant est dans la première profondeur,
     * pour me permettre de regarder à l'intérieur d'un méthode uniquement...
     * Réécrit dans Identifier et dans Selection (et peut-être dans this)
     * Vaut false par défaut
     */
    public boolean isShallow(EnvironmentExp localEnv) {
    	return false;
    }

    /**
     * @return true if this is a Literal or an Identifier
     */
    public boolean isAtomic() {
        return false;
    }
}
