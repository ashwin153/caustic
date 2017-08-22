import math

from caustic.runtime.thrift.ttypes import *


# Constants.
FieldDelimiter = '@'
ListDelimiter = ','
InternalDelimiter = '$'


# Literal Values.
def flag(x): return x if isinstance(x, Transaction) else Transaction(literal=Literal(flag=x))
def real(x): return x if isinstance(x, Transaction) else Transaction(literal=Literal(real=x))
def text(x): return x if isinstance(x, Transaction) else Transaction(literal=Literal(text=x))


# Core Expressions.
def read(k): return Transaction(expression=Expression(read=Read(k)))
def write(k, v): return Transaction(expression=Expression(write=Write(k, v)))
def load(k): return Transaction(expression=Expression(load=Load(k)))
def store(n, v): return Transaction(expression=Expression(store=Store(n, v)))
def branch(c, p, f): return Transaction(expression=Expression(branch=Branch(c, p, f)))
def cons(f, s): return Transaction(expression=Expression(cons=Cons(f, s)))
def prefetch(k): return Transaction(expression=Expression(prefetch=Prefetch(k)))
def repeat(c, b): return Transaction(expression=Expression(repeat=Repeat(c, b)))
def rollback(m): return Transaction(expression=Expression(rollback=Rollback(m)))


# String Expressions.
Empty = text("")

def contains(x, y): return Transaction(expression=Expression(contains=Contains(x, y)))
def length(x): return Transaction(expression=Expression(length=Length(x)))
def concat(x, y): return Transaction(expression=Expression(add=Add(x, y)))
def slice(x, l, h): return Transaction(expression=Expression(slice=Slice(x, l, h)))
def matches(x, r): return Transaction(expression=Expression(matches=Matches(x, r)))
def indexOf(x, y): return Transaction(expression=Expression(indexOf=IndexOf(x, y)))


# Math Expressions.
Zero = real(0)
One = real(1)
Half = real(0.5)
Two = real(2)
E = real(math.e)
Pi = real(math.pi)

def add(x, y): return Transaction(expression=Expression(add=Add(x, y)))
def sub(x, y): return Transaction(expression=Expression(sub=Sub(x, y)))
def mul(x, y): return Transaction(expression=Expression(mul=Mul(x, y)))
def div(x, y): return Transaction(expression=Expression(div=Div(x, y)))
def mod(x, y): return Transaction(expression=Expression(mod=Mod(x, y)))
def pow(x, y): return Transaction(expression=Expression(pow=Pow(x, y)))
def log(x): return Transaction(expression=Expression(log=Log(x)))
def sin(x): return Transaction(expression=Expression(sin=Sin(x)))
def cos(x): return Transaction(expression=Expression(cos=Cos(x)))
def floor(x): return Transaction(expression=Expression(floor=Floor(x)))

def abs(x): return branch(less(x, Zero), sub(Zero, x), x)
def exp(x): return pow(E, x)
def tan(x): return div(sin(x), cos(x))
def cot(x): return div(cos(x), sin(x))
def sec(x): return div(One, cos(x))
def csc(x): return div(One, sin(x))
def sinh(x): return div(sub(exp(x), exp(sub(Zero, x))), Two)
def cosh(x): return div(add(exp(x), exp(sub(Zero, x))), Two)
def tanh(x): return div(sinh(x), cosh(x))
def coth(x): return div(cosh(x), sinh(x))
def sech(x): return div(One, cosh(x))
def csch(x): return div(One, sinh(x))
def sqrt(x): return pow(x, Half)
def ceil(x): return branch(equal(x, floor(x)), x, floor(x) + One)
def round(x): return branch(less(sub(x, floor(x)), Half), floor(x), ceil(x))


# Comparison Expressions.
def both(x, y): return Transaction(expression=Expression(both=Both(x, y)))
def either(x, y): return Transaction(expression=Expression(either=Either(x, y)))
def negate(x): return Transaction(expression=Expression(negate=Negate(x)))
def equal(x, y): return Transaction(expression=Expression(equal=Equal(x, y)))
def less(x, y): return Transaction(expression=Expression(less=Less(x, y)))
