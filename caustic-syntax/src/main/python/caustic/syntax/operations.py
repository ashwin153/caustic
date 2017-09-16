import abc

from caustic.syntax.codec import *


class InfixOperations(object):
    """

    """
    __metaclass__ = abc.ABCMeta

    # String Operations.
    def __len__(self): return length(self)
    def __contains__(self, y): return contains(self, text(y))
    def matches(self, y): return matches(self, text(y))
    def index(self, y): return indexOf(self, text(y))
    def __getitem__(self, item): pass

    # Math Operations.
    def __neg__(self): return sub(Zero, self)
    def __abs__(self): return abs(self)
    def __ceil__(self): return ceil(self)
    def __floor__(self): return floor(self)
    def __trunc__(self): return trunc(self)
    def __round__(self): return round(self)
    def __add__(self, y): return add(self, y)
    def __radd__(self, x): return add(x, self)
    def __sub__(self, y): return sub(self, y)
    def __rsub__(self, x): return sub(x, self)
    def __mul__(self, y): return mul(self, y)
    def __rmul__(self, x): return mul(x, self)
    def __truediv__(self, y): return div(self, y)
    def __rtruediv__(self, x): return div(x, self)
    def __floordiv__(self, y): return floor(div(self, y))
    def __rfloordiv__(self, x): return floor(div(x, self))
    def __mod__(self, y): return mod(self, y)
    def __rmod__(self, x): return mod(x, self)
    def __pow__(self, y, mod=None):  return mod(pow(self, y), mod) if mod else pow(self, y)
    def __rpow__(self, x):  return pow(x, self)

    # Comparison Operations.
    def __invert__(self): return negate(self)
    def __eq__(self, y): return eq(self, y)
    def __ne__(self, y): return ne(self, y)
    def __lt__(self, y): return lt(self, y)
    def __le__(self, y): return le(self, y)
    def __gt__(self, y): return gt(self, y)
    def __ge__(self, y): return ge(self, y)
    def __and__(self, y): return both(self, flag(y))
    def __rand__(self, x): return both(x, self)
    def __xor__(self, y): return xor(self, y)
    def __rxor__(self, x): return xor(x, self)
    def __or__(self, y): return either(self, y)
    def __ror__(self, x): return either(x, self)
