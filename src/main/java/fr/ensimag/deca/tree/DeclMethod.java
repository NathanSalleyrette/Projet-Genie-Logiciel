package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.context.ExpDefinition;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.EnvironmentExp.DoubleDefException;
import fr.ensimag.deca.context.MethodDefinition;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.Signature;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.deca.tools.IndentPrintStream;
import java.io.PrintStream;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 *
 * @author gl01
 * @date 01/01/2021
 */
public class DeclMethod extends AbstractDeclMethod  {
    
    @Override
    public AbstractIdentifier getName() {
        return methodName;
    }

    final private AbstractIdentifier type;
    final private AbstractIdentifier methodName;
    final private ListDeclParam params;
    final private MethodBody body;


    public DeclMethod(AbstractIdentifier type, AbstractIdentifier methodName, ListDeclParam params, MethodBody body) {
        Validate.notNull(type);
        Validate.notNull(methodName);
        Validate.notNull(params);
        Validate.notNull(body);
        this.type = type;
        this.methodName = methodName;
        this.params = params;
        this.body = body;
    }

    @Override
    protected void verifyDeclMethod(DecacCompiler compiler, 
    		EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {
    	// Type de la méthode
    	Type methodType = this.type.verifyType(compiler);
    	// Déclaration de sa signature
    	Signature sig = new Signature();
    	this.params.verifyListDeclParam(compiler, localEnv, currentClass);
    	// On initialise la signature si ce n'est pas déjà fait
    	if (sig.size() == 0) {
    		for(AbstractDeclParam param : params.getList()) {
    			sig.add(param.getType());
    		}
    	}
    	ExpDefinition potentialSuperDef = localEnv.get(methodName.getName());
		// On met à jour la définition de la méthode et le nombre de méthodes
		int index = 0;
		if (potentialSuperDef == null) {
			index = currentClass.incNumberOfMethods();
		} else { // Redéfinition
			index = potentialSuperDef.asMethodDefinition("(2.6) " +
				methodName.getName().toString() + " définit déjà un champ", this.getLocation()).getIndex();
		}
    	MethodDefinition methodDef = new MethodDefinition(methodType, this.getLocation(),
				sig, index);
		methodDef.setLabel(new Label("code." + currentClass.getType().toString() + "." + methodName.getName().toString()));
    	this.getName().setDefinition(methodDef);
    	try {
    		currentClass.getMembers().declare(this.getName().getName(), methodDef, getLocation());
    	} catch(DoubleDefException e) {
    		throw new ContextualError("(2.6) La méthode " + this.getName().getName().toString() +
    				" est déjà définie", this.getLocation());
    	}
    	
    	// Verification de la signature en cas de redéfinition
    	MethodDefinition superMethod;
    	if (potentialSuperDef != null) {
    		superMethod = potentialSuperDef.asMethodDefinition("(2.6) " +
    				methodName.getName().toString() + " définit déjà un champ", getLocation());
    		if ((methodType != superMethod.getType()) && 
    				(!methodType.isSubTypeOf(superMethod.getType(), getLocation()))) {
    			throw new ContextualError("(2.7) La méthode retourne le type " + methodType.toString() +
    					" mais devrait retourner le type de sa super méthode " + 
    					superMethod.getType().toString() +" ou un sous-type", this.getLocation());
    		}
    		if (this.params.size() != superMethod.getSignature().size()) {
    			throw new ContextualError("(2.7) La signature diffère de la méthode redéfinie",
    					this.getLocation());
    		} else {
    			for (int i = 0; i < this.params.size(); i++) {
    				if (superMethod.getSignature().paramNumber(i) != params.getList().get(i).getType()) {
    					throw new ContextualError("(2.7) La signature diffère de la méthode redéfinie",
    	    					this.getLocation());
    				}
    			}
    		}
    	}
    }

    public void verifyBody(DecacCompiler compiler, EnvironmentExp localEnv,
    		ClassDefinition currentClass, Type methodType) throws ContextualError {
    	this.params.verifyListDeclParam(compiler, localEnv, currentClass);
    	this.body.verifyMethodBody(compiler, localEnv, currentClass, methodType);
    }
    @Override
    public void decompile(IndentPrintStream s) {
    	this.type.decompile(s);
    	s.print(" ");
    	this.methodName.decompile(s);
    	s.print("(");
    	this.params.decompile(s);
    	s.print(")");
    	s.println("{");
        s.indent();
        body.decompile(s);
        s.unindent();
        s.println("}");

    }

    public Type getType() {
    	return this.type.getType();
    }
    
    @Override
    protected
    void iterChildren(TreeFunction f) {
        type.iter(f);
        methodName.iter(f);
    }
    
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        type.prettyPrint(s, prefix, false);
        methodName.prettyPrint(s, prefix, false);
        params.prettyPrint(s, prefix, false);
        body.prettyPrint(s, prefix, true);
    }

}
