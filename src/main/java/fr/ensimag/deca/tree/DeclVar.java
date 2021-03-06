package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.context.TypeDefinition;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.EnvironmentExp.DoubleDefException;
import fr.ensimag.deca.tools.IndentPrintStream;
import java.io.PrintStream;
import org.apache.commons.lang.Validate;
import fr.ensimag.deca.tools.SymbolTable.Symbol;
import fr.ensimag.deca.context.VariableDefinition;

/**
 * @author gl01
 * @date 01/01/2021
 */
public class DeclVar extends AbstractDeclVar {

    @Override
    public AbstractInitialization getInitialization() {
        return initialization;
    }

    @Override
    public AbstractIdentifier getName() {
        return varName;
    }

    final private AbstractIdentifier type;
    final private AbstractIdentifier varName;
    final private AbstractInitialization initialization;

    public DeclVar(AbstractIdentifier type, AbstractIdentifier varName, AbstractInitialization initialization) {
        Validate.notNull(type);
        Validate.notNull(varName);
        Validate.notNull(initialization);
        this.type = type;
        this.varName = varName;
        this.initialization = initialization;
    }

    @Override
    protected void verifyDeclVar(DecacCompiler compiler,
            EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {
    	// On fait la vérification du type et on récupère le type au passage
    	Type varType = this.type.verifyType(compiler);
    	// On récupère le symbole du nom de la variable et on crée une ExpDefinition
    	Symbol nameSymb = this.varName.getName();
    	VariableDefinition varDef = new VariableDefinition(varType, this.getLocation());
    	try {
    		localEnv.declare(nameSymb, varDef, getLocation());
    		varName.setDefinition(varDef);
    	} catch (DoubleDefException e) {
    		throw new ContextualError("(3.17) La variable " + this.varName.getName().toString() + " est déjà déclarée", this.getLocation());
    	}
    	// On vérifie l'initialisation ensuite
    	this.initialization.verifyInitialization(compiler, varType, localEnv, currentClass);
    	if (varType.isVoid()) {
    		throw new ContextualError("(3.17) Le type de l'identificateur ne peut être 'void''", this.getLocation());
    	}
    }

    
    @Override
    public void decompile(IndentPrintStream s) {
    	this.type.decompile(s);
    	s.print(" ");
    	this.varName.decompile(s);
    	this.initialization.decompile(s);
    	s.print(";");
    }

    @Override
    protected
    void iterChildren(TreeFunction f) {
        type.iter(f);
        varName.iter(f);
        initialization.iter(f);
    }
    
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        type.prettyPrint(s, prefix, false);
        varName.prettyPrint(s, prefix, false);
        initialization.prettyPrint(s, prefix, true);
    }
}
