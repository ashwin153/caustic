grammar Caustic;

/**
 * All terminal symbols in the grammar.
 */
constant
    : True // true
    | False // false
    | Number // 5.0, -1
    | String  // "Hello"
    ;

name
    : Identifier ('.' Identifier)* // x, x.foo
    ;

funcall
    : Identifier '&'? '(' (expression ',')* expression? ')' // foo(1.3, false)
    ;

/**
 * An expression corresponds to a sequence of logical, relational, arithmetic operations performed
 * on some names, function calls, or constants. Expressions are ordered in descending order of
 * precedence.
 */
primaryExpression
    : name
    | funcall
    | constant
    | '(' expression ')' // (2 + 5) * 3
    ;

prefixExpression
    : (Add | Sub | Not)? primaryExpression // +2.0, -1.2, !true
    ;

multiplicativeExpression
    : prefixExpression
    | multiplicativeExpression Mul prefixExpression // 2.3 * 1
    | multiplicativeExpression Div prefixExpression // 3 / 2 (equal to 1.5)
    | multiplicativeExpression Mod prefixExpression // 4 % 2
    ;

additiveExpression
    : multiplicativeExpression
    | additiveExpression Add multiplicativeExpression // 4 - 3.1
    | additiveExpression Sub multiplicativeExpression // x.foo + 1
    ;

relationalExpression
    : additiveExpression
    | relationalExpression LessThan additiveExpression // x.foo < 8.4
    | relationalExpression GreaterThan additiveExpression // x.bar > x.foo
    | relationalExpression LessEqual additiveExpression // 3 <= 9
    | relationalExpression GreaterEqual additiveExpression // x.foo >= 1
    ;

equalityExpression
    : relationalExpression
    | equalityExpression Equal relationalExpression // 1 == 1
    | equalityExpression NotEqual relationalExpression // x.foo != x.bar
    ;

logicalAndExpression
    : equalityExpression
    | logicalAndExpression And equalityExpression // x.foo && x.bar
    ;

logicalOrExpression
    : logicalAndExpression
    | logicalOrExpression Or logicalAndExpression // x.foo || x. bar
    ;

expression
    : logicalOrExpression
    ;

/**
 * A basic block corresponds to a sequence of statements. Statements generate transactions for all
 * operations that mutate state (read, store, etc.), alter control flow (branch, repeat, etc.), or
 * cause side-effects (funcall).
 */
conditional
    : If '(' expression ')' block (Elif '(' expression ')' block)* (Else block)? // if (x) { foo() }
    ;

loop
    : While '(' expression ')' block // while (true) { foo() }
    ;

deletion
    : Del name // del x.foo
    ;

definition
    : Var Identifier Assign expression // val x = "foo"
    ;

assignment
    : name Assign expression // x.foo = y.foo
    | name MulAssign expression // x *= 2.4
    | name DivAssign expression // x /= x.bar
    | name ModAssign expression // x %= 3
    | name AddAssign expression // x += "bar"
    | name SubAssign expression // x -= 8.4
    ;

rollback
    : Rollback expression // rollback "message"
    ;

statement
    : definition
    | conditional
    | deletion
    | loop
    | assignment
    | rollback
    | funcall
    | expression
    ;

block
    : statement ';'? // if (x = 3) x += 2
    | '{' (statement ';'?)* '}' // if (x = 3) { x += 2 }
    ;

/**
 * A program corresponds to a namespaced sequence of record and service declarations. Records are
 * object schemas that are persisted in the database and services consist of function definitions.
 */
type
    : Identifier Ampersand?
    ;

parameter
    : Identifier ':' type // x: Foo&
    ;

parameters
    : (parameter ',')* parameter? // x: String, y: Integer
    ;

function
    : Def Identifier '(' parameters ')' ':' type '=' block // def foo(): Unit = 3
    ;

service
    : Service Identifier (Extends Identifier)? '{' function* '}' // service Bar { }
    ;

record
    : Record Identifier (Extends Identifier)? '{' parameters '}' // rec Foo { x: String }
    ;

declaration
    : record
    | service
    ;

module
    : Identifier ('.' Identifier)* // caustic.example
    ;

program
    : (Module module)? (Import module)* declaration*
    ;

Def          : 'def';
Del          : 'del';
Elif         : 'elif';
Else         : 'else';
Extends      : 'extends';
False        : 'false';
If           : 'if';
Import       : 'import';
Module       : 'module';
Record       : 'record';
Return       : 'return';
Rollback     : 'rollback';
Service      : 'service';
While        : 'while';
True         : 'true';
Var          : 'var';

Add          : '+';
AddAssign    : '+=';
Ampersand    : '&';
And          : '&&';
Arrow        : '=>';
Assign       : '=';
Colon        : ':';
Comma        : ',';
Div          : '/';
DivAssign    : '/=';
Equal        : '==';
GreaterEqual : '>=';
GreaterThan  : '>';
LeftBlock    : '[';
LeftBracket  : '{';
LeftParen    : '(';
LessEqual    : '<=';
LessThan     : '<';
Mod          : '%';
ModAssign    : '%=';
Mul          : '*';
MulAssign    : '*=';
Not          : '!';
NotEqual     : '!=';
Or           : '||';
Period       : '.';
Question     : '?';
RightBlock   : ']';
RightBracket : '}';
RightParen   : ')';
Semicolon    : ';';
Sub          : '-';
SubAssign    : '-=';
Underscore   : '_';

fragment
Digit
    : [0-9]
    ;

Number
    : Digit+ ('.' Digit+)?
    ;

fragment
Nondigit
    : [a-zA-Z_]
    ;

Identifier
    : Nondigit (Nondigit | Digit)*
    ;

fragment
EscapeSequence
    : '\\' ['"?abfnrtv\\]
    ;

fragment
Character
    : ~["\\\r\n]
    | EscapeSequence
    | '\\\n'
    | '\\\r\n'
    ;

String
    : '"' Character* '"'
    | '\'' Character* '\''
    ;

BlockComment
    : '/*' .*? '*/'
    -> skip
    ;

LineComment
    : '//' ~[\r\n]*
    -> skip
    ;

Whitespace
    : [ \t\r\n\f]+
    -> skip
    ;