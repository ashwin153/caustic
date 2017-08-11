from __future__ import division

from caustic.syntax.codec import *
from caustic.syntax.record import Record


class Field(Record):
    """

    """

    def __init__(self, name, parent):
        super(Field, self).__init__(name, parent)

    def get(self):
        return read(self.key)

    def set(self, value):
        pass

    # String Operations.
    def __len__(self): return length(self.get())
    def __contains__(self, y): return contains(self.get(), text(y))
    def matches(self, y): return matches(self.get(), text(y))
    def indexOf(self, y): return indexOf(self.get(), text(y))
    def substring(self, l, h=None): return slice(self.get(), l, h) if h else slice(self.get(), l, length(self.get()))

    # Math Operations.
    def __neg__(self): return sub(Zero, self.get())
    def __abs__(self): return abs(self.get())
    def __ceil__(self): return ceil(self.get())
    def __floor__(self): return floor(self.get())
    def __trunc__(self): return branch(less(self.get(), Zero), ceil(self.get()), floor(self.get()))
    def __round__(self): return round(self.get())
    def __add__(self, y): return add(self.get(), real(y))
    def __radd__(self, x): return add(real(x), self.get())
    def __sub__(self, y): return sub(self.get(), real(y))
    def __rsub__(self, x): return sub(real(x), self.get())
    def __mul__(self, y): return mul(self.get(), real(y))
    def __rmul__(self, x): return mul(real(x), self.get())
    def __truediv__(self, y): return div(self.get(), real(y))
    def __rtruediv__(self, x): return div(real(x), self.get())
    def __floordiv__(self, y): return floor(div(self.get(), real(y)))
    def __rfloordiv__(self, x): return floor(div(real(x), self.get()))
    def __mod__(self, y): return mod(self.get(), real(y))
    def __rmod__(self, x): return mod(real(x), self.get())
    def __pow__(self, y, modulo=None): return mod(pow(self.get(), real(y)), real(modulo)) if modulo else pow(self.get(), real(y))
    def __rpow__(self, x): return pow(real(x), self.get())

    # Comparison Operations.
    def __invert__(self): return negate(self.get())
    def __eq__(self, y): return equal(self.get(), y)
    def __ne__(self, y): return negate(equal(self.get(), y))
    def __lt__(self, y): return less(self.get(), y)
    def __le__(self, y): return either(less(self.get(), y), equal(self.get(), y))
    def __gt__(self, y): return negate(either(less(self.get(), y), equal(self.get(), y)))
    def __ge__(self, y): return negate(less(self.get(), y))
    def __and__(self, y): return both(self.get(), flag(y))
    def __rand__(self, x): return both(flag(x), self.get())
    def __xor__(self, y): return both(either(self.get(), flag(y)), negate(both(self.get(), flag(y))))
    def __rxor__(self, x): return both(either(flag(x), self.get()), negate(both(flag(x), self.get())))
    def __or__(self, y): return either(self.get(), flag(y))
    def __ror__(self, x): return either(flag(x), self.get())
