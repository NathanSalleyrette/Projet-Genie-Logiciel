package fr.ensimag.deca;

import fr.ensimag.deca.context.TypeDefinition;
import fr.ensimag.deca.syntax.DecaLexer;
import fr.ensimag.deca.syntax.DecaParser;
import fr.ensimag.deca.tools.SymbolTable.Symbol;
import fr.ensimag.deca.tools.SymbolTable;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tree.AbstractProgram;
import fr.ensimag.deca.tree.LocationException;
import fr.ensimag.ima.pseudocode.AbstractLine;
import fr.ensimag.ima.pseudocode.IMAProgram;
import fr.ensimag.ima.pseudocode.Instruction;
import fr.ensimag.ima.pseudocode.Label;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import java.util.HashMap;
import java.util.Map;

/**
 * Decac compiler instance.
 *
 * This class is to be instantiated once per source file to be compiled. It
 * contains the meta-data used for compiling (source file name, compilation
 * options) and the necessary utilities for compilation (symbol tables, abstract
 * representation of target file, ...).
 *
 * It contains several objects specialized for different tasks. Delegate methods
 * are used to simplify the code of the caller (e.g. call
 * compiler.addInstruction() instead of compiler.getProgram().addInstruction()).
 *
 * @author gl01
 * @date 01/01/2021
 */
public class DecacCompiler {
    private static final Logger LOG = Logger.getLogger(DecacCompiler.class);
    
    /**
     * Portable newline character.
     */
    private static final String nl = System.getProperty("line.separator", "\n");
    
    private SymbolTable symbTb;
    
    private EnvironmentType envType;
    
    public DecacCompiler(CompilerOptions compilerOptions, File source) {
        super();
        this.compilerOptions = compilerOptions;
        this.source = source;
        this.symbTb = new SymbolTable();
        this.envType = new EnvironmentType(this.symbTb);
    }

    public SymbolTable getSymbTb() {
    	return this.symbTb;
    }
    /**
     * Source file associated with this compiler instance.
     */
    public File getSource() {
        return source;
    }

    /**
     * Compilation options (e.g. when to stop compilation, number of registers
     * to use, ...).
     */
    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#add(fr.ensimag.ima.pseudocode.AbstractLine)
     */
    public void add(AbstractLine line) {
        program.add(line);
    }

    /**
     * @see fr.ensimag.ima.pseudocode.IMAProgram#addComment(java.lang.String)
     */
    public void addComment(String comment) {
        program.addComment(comment);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addLabel(fr.ensimag.ima.pseudocode.Label)
     */
    public void addLabel(Label label) {
        program.addLabel(label);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction)
     */
    public void addInstruction(Instruction instruction) {
        program.addInstruction(instruction);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction,
     * java.lang.String)
     */
    public void addInstruction(Instruction instruction, String comment) {
        program.addInstruction(instruction, comment);
    }

    /**
     * add the instructions treating an error
     */
    public void addError(Label label) {
        compiler.addLabel(label);
        compiler.addInstruction(new WSTR("Erreur : " + label));
        compiler.addInstruction(new WNL());
        compiler.addInstruction(new ERROR());
    }
    
    /**
     * @see 
     * fr.ensimag.ima.pseudocode.IMAProgram#display()
     */
    public String displayIMAProgram() {
        return program.display();
    }
    
    private final CompilerOptions compilerOptions;
    private final File source;
    /**
     * The main program. Every instruction generated will eventually end up here.
     */
    private final IMAProgram program = new IMAProgram();
 

    /**
     * Run the compiler (parse source file, generate code)
     *
     * @return true on error
     */
    public boolean compile() {
        String sourceFile = source.getAbsolutePath();
        String destFile = null;
        // A FAIRE: calculer le nom du fichier .ass à partir du nom du
        // A FAIRE: fichier .deca.
        PrintStream err = System.err;
        PrintStream out = System.out;
        LOG.debug("Compiling file " + sourceFile + " to assembly file " + destFile);
        try {
            return doCompile(sourceFile, destFile, out, err);
        } catch (LocationException e) {
            e.display(err);
            return true;
        } catch (DecacFatalError e) {
            err.println(e.getMessage());
            return true;
        } catch (StackOverflowError e) {
            LOG.debug("stack overflow", e);
            err.println("Stack overflow while compiling file " + sourceFile + ".");
            return true;
        } catch (Exception e) {
            LOG.fatal("Exception raised while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        } catch (AssertionError e) {
            LOG.fatal("Assertion failed while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        }
    }

    /**
     * Internal function that does the job of compiling (i.e. calling lexer,
     * verification and code generation).
     *
     * @param sourceName name of the source (deca) file
     * @param destName name of the destination (assembly) file
     * @param out stream to use for standard output (output of decac -p)
     * @param err stream to use to display compilation errors
     *
     * @return true on error
     */
    private boolean doCompile(String sourceName, String destName,
            PrintStream out, PrintStream err)
            throws DecacFatalError, LocationException {
        AbstractProgram prog = doLexingAndParsing(sourceName, err);

        if (prog == null) {
            LOG.info("Parsing failed");
            return true;
        }
        assert(prog.checkAllLocations());


        prog.verifyProgram(this);
        assert(prog.checkAllDecorations());

        addComment("start main program");
        prog.codeGenProgram(this);
        addComment("end main program");
        LOG.debug("Generated assembly code:" + nl + program.display());
        LOG.info("Output file assembly file is: " + destName);

        FileOutputStream fstream = null;
        try {
            fstream = new FileOutputStream(destName);
        } catch (FileNotFoundException e) {
            throw new DecacFatalError("Failed to open output file: " + e.getLocalizedMessage());
        }

        LOG.info("Writing assembler file ...");

        program.display(new PrintStream(fstream));
        LOG.info("Compilation of " + sourceName + " successful.");
        return false;
    }

    /**
     * Build and call the lexer and parser to build the primitive abstract
     * syntax tree.
     *
     * @param sourceName Name of the file to parse
     * @param err Stream to send error messages to
     * @return the abstract syntax tree
     * @throws DecacFatalError When an error prevented opening the source file
     * @throws DecacInternalError When an inconsistency was detected in the
     * compiler.
     * @throws LocationException When a compilation error (incorrect program)
     * occurs.
     */
    protected AbstractProgram doLexingAndParsing(String sourceName, PrintStream err)
            throws DecacFatalError, DecacInternalError {
        DecaLexer lex;
        try {
            lex = new DecaLexer(CharStreams.fromFileName(sourceName));
        } catch (IOException ex) {
            throw new DecacFatalError("Failed to open input file: " + ex.getLocalizedMessage());
        }
        lex.setDecacCompiler(this);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        DecaParser parser = new DecaParser(tokens);
        parser.setDecacCompiler(this);
        return parser.parseProgramAndManageErrors(err);
    }
    
    /**
     * Classe d'environnement des types, interne à DecacCompiler
     * pour éviter les problèmes d'accès par différents fils
     */
    public static class EnvironmentType {
    	
    	private EnvironmentType parentEnvironment;
    	
    	private SymbolTable sbtb;
    	
    	private Map<Symbol, TypeDefinition> map;
    	
    	/**
    	 * Constructeur appelé pour définir env_type_predef
    	 * par le constructeur du DecacCompiler (il est donc
    	 * déclaré privé)
    	 * 
    	 * @param table
    	 * 		Table de symboles propre au DecacCompiler, que l'on
    	 * va garnir
    	 */
    	private EnvironmentType(SymbolTable table) {
    		this.sbtb = table;
    		Symbol INT = table.create("int");
    		Symbol FLOAT = table.create("float");
    		Symbol BOOLEAN = table.create("boolean");
    		Symbol VOID = table.create("void");
    		this.map = new HashMap<Symbol, TypeDefinition>();
    		this.map.put(INT, this.get(INT));
    		this.map.put(FLOAT, this.get(FLOAT));
    		this.map.put(BOOLEAN, this.get(BOOLEAN));
    		this.map.put(VOID, this.get(VOID));
    		this.parentEnvironment = null; // Dieu que c'est laid
    		// TODO: ce qui concerne le langage objet
    	}
    	
    	public EnvironmentType(SymbolTable table, EnvironmentType parent) {
    		this.parentEnvironment = parent;
    		this.map = parent.getMap(); /* est-ce qu'une deepcopy est 
    		requise ? A-t-on besoin de conserver la table du parent ?*/
    		this.sbtb = table;
    	}
    	
    	public Map<Symbol, TypeDefinition> getMap() {
    		return this.map;
    	}
    	
    	public EnvironmentType getParentEnvironment() {
    		return this.parentEnvironment;
    	}
    	/**
         * Return the type of the symbol in the environment, or null if the
         * symbol is undefined.
         */
        public TypeDefinition get(Symbol key) {
            TypeDefinition result = map.get(key); // null if no such key

            // Search in the parent dictionary after failure in current one
            if (result == null && parentEnvironment != null) {
                return parentEnvironment.get(key);
            }
            return result;
        }
    }
}