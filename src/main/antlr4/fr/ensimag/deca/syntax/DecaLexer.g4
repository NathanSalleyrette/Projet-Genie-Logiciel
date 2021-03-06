lexer grammar DecaLexer;

options {
   language=Java;
   // Tell ANTLR to make the generated lexer class extend the
   // the named class, which is where any supporting code and
   // variables will be placed.
   superClass = AbstractDecaLexer;
}

@members {
}

// Deca lexer rules.

// Mots réservés

ASM : 'asm';
CLASS : 'class';
EXTENDS : 'extends';
ELSE : 'else';
FALSE : 'false';
IF : 'if';
INSTANCEOF : 'instanceof';
NEW : 'new';
NULL : 'null';
READINT : 'readInt';
READFLOAT : 'readFloat';
PRINT : 'print';
PRINTLN : 'println';
PRINTLNX : 'printlnx';
PRINTX : 'printx';
PROTECTED : 'protected';
RETURN : 'return';
THIS : 'this';
TRUE : 'true';
WHILE : 'while';

// Identificateurs
fragment LETTER : 'a' .. 'z' | 'A' .. 'Z';

fragment DIGIT : '0' .. '9';

IDENT : (LETTER | '$' | '_')(LETTER | DIGIT | '$' | '_')*;


// Symboles spéciaux
LT : '<';
GT : '>';
EQUALS : '=';
PLUS : '+';
MINUS : '-';
TIMES : '*';
SLASH : '/';
PERCENT : '%';
DOT : '.';
COMMA : ',';
OPARENT : '(';
CPARENT : ')';
OBRACE : '{';
CBRACE : '}';
EXCLAM : '!';
SEMI : ';';
EQEQ : '==';
NEQ : '!=';
GEQ : '>=';
LEQ : '<=';
AND : '&&';
OR : '||';

// Littéraux entiers
fragment POSITIVE_DIGIT : '1' .. '9';

INT : '0' | POSITIVE_DIGIT DIGIT*;


// Littéraux flottants
fragment NUM : DIGIT+;

fragment SIGN : ('+' | '-')? ;

fragment EXP : ('E' | 'e') SIGN NUM;

fragment DEC : NUM '.' NUM;

fragment FLOATDEC : (DEC | DEC EXP) ('F' | 'f')? ;

fragment DIGITHEX : '0' .. '9' | 'A' .. 'F' | 'a' .. 'f';

fragment NUMHEX : DIGITHEX+;

fragment FLOATHEX : ('0x' | '0X') NUMHEX '.' NUMHEX ('P' | 'p') SIGN NUM ('F' | 'f')? ;

FLOAT : FLOATDEC | FLOATHEX;


// Chaine de caractère

fragment STRING_CAR : ~('"' | '\\' | '\n');

STRING : '"' (STRING_CAR | '\\"' | '\\\\')* '"'
            { setText(getText().substring(1, getText().lastIndexOf('"'))); };

MULTI_LINE_STRING : '"' (STRING_CAR | '\n' | '\\"' | '\\\\')* '"'
            { setText(getText().substring(1, getText().lastIndexOf('"'))); };


// Commentaires
fragment COMMENT_CLASSIC : '/*' .*? '*/';

fragment COMMENT_LINE : '//' .*? ('\n' | EOF);

fragment COMMENT : COMMENT_CLASSIC | COMMENT_LINE;

// Séparateurs
fragment ESPACE  : ' ' | '\t' | '\r' | '\n';

SEPARATEUR : (ESPACE | COMMENT)
            { skip(); };


// Inclusion de fichier
fragment FILENAME : (LETTER | DIGIT | '.' | '-' | '_')+;

INCLUDE : '#include' (' ')* '"' FILENAME '"'
            { doInclude(getText()); };