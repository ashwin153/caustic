import math

from caustic.runtime.thrift.ttypes import *


# Record kinds.
Structure = 'structure'
Attribute = 'attribute'
Reference = 'reference'


# Reserved Delimiters.
FieldDelimiter = '@'
ArrayDelimiter = ','


# Implicit Conversions.
def fmt(x):
    if x is None:
        return Empty
    elif isinstance(x, Transaction):
        return x
    elif isinstance(x, str):
        return text(x)
    elif isinstance(x, int):
        return real(x)
    elif isinstance(x, float):
        return real(x)
    elif isinstance(x, bool):
        return flag(x)
    else:
        raise TypeError('Unknown type for {}'.format(x))


# Literal Values.
def flag(x): return Transaction(literal=Literal(flag=x))
def real(x): return Transaction(literal=Literal(real=x))
def text(x): return Transaction(literal=Literal(text=x))


# Core Expressions.
def read(k): return Transaction(expression=Expression(read=Read(fmt(k))))
def write(k, v): return Transaction(expression=Expression(write=Write(fmt(k), fmt(v))))
def load(k): return Transaction(expression=Expression(load=Load(fmt(k))))
def store(n, v): return Transaction(expression=Expression(store=Store(fmt(n), fmt(v))))
def branch(c, p, f=None): return Transaction(expression=Expression(branch=Branch(fmt(c), fmt(p), fmt(f))))
def cons(a, b): return Transaction(expression=Expression(cons=Cons(fmt(a), fmt(b))))
def prefetch(k): return Transaction(expression=Expression(prefetch=Prefetch(fmt(k))))
def repeat(c, b): return Transaction(expression=Expression(repeat=Repeat(fmt(c), fmt(b))))
def rollback(m): return Transaction(expression=Expression(rollback=Rollback(fmt(m))))
def block(x, *rest): return reduce(lambda a, b: cons(a, b), rest, x)


# String Expressions.
Empty = text("")

def contains(x, y): return Transaction(expression=Expression(contains=Contains(fmt(x), fmt(y))))
def length(x): return Transaction(expression=Expression(length=Length(fmt(x))))
def concat(x, *rest): return reduce(lambda a, b: add(a, b), rest, x)
def substring(x, l, h=None): return Transaction(expression=Expression(slice=Slice(fmt(x), fmt(l), fmt(h) if h else length(fmt(x)))))
def matches(x, r): return Transaction(expression=Expression(matches=Matches(fmt(x), fmt(r))))
def indexOf(x, y): return Transaction(expression=Expression(indexOf=IndexOf(fmt(x), fmt(y))))


# Math Expressions.
Zero = real(0)
One = real(1)
Half = real(0.5)
Two = real(2)
E = real(math.e)
Pi = real(math.pi)

def add(x, y): return Transaction(expression=Expression(add=Add(fmt(x), fmt(y))))
def sub(x, y): return Transaction(expression=Expression(sub=Sub(fmt(x), fmt(y))))
def mul(x, y): return Transaction(expression=Expression(mul=Mul(fmt(x), fmt(y))))
def div(x, y): return Transaction(expression=Expression(div=Div(fmt(x), fmt(y))))
def mod(x, y): return Transaction(expression=Expression(mod=Mod(fmt(x), fmt(y))))
def pow(x, y): return Transaction(expression=Expression(pow=Pow(fmt(x), fmt(y))))
def log(x): return Transaction(expression=Expression(log=Log(fmt(x))))
def sin(x): return Transaction(expression=Expression(sin=Sin(fmt(x))))
def cos(x): return Transaction(expression=Expression(cos=Cos(fmt(x))))
def floor(x): return Transaction(expression=Expression(floor=Floor(fmt(x))))

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
def trunc(x): return branch(less(x, Zero), ceil(x), floor(x))


# Comparison Expressions.
def both(x, y): return Transaction(expression=Expression(both=Both(fmt(x), fmt(y))))
def either(x, y): return Transaction(expression=Expression(either=Either(fmt(x), fmt(y))))
def negate(x): return Transaction(expression=Expression(negate=Negate(fmt(x))))
def equal(x, y): return Transaction(expression=Expression(equal=Equal(fmt(x), fmt(y))))
def less(x, y): return Transaction(expression=Expression(less=Less(fmt(x), fmt(y))))

def eq(x, y): return equal(x, y)
def ne(x, y): return negate(equal(x, y))
def lt(x, y): return less(x, y)
def le(x, y): return either(less(x, y), equal(x, y))
def gt(x, y): return negate(either(less(x, y), equal(x, y)))
def ge(x, y): return negate(less(x, y))
def xor(x, y): return both(either(x, y), negate(both(x, y)))
