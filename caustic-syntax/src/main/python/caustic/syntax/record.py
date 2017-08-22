from __future__ import division

from caustic.syntax.codec import *


class Record(object):
    """

    """

    def __init__(self, key):
        self.key = key
        self.kind = Record(key + FieldDelimiter + 'kind')

    def __getattr__(self, name):
        return Record(branch(
            self.kind == self.Reference,
            read(self.key) + FieldDelimiter + name,
            self.key + FieldDelimiter + name
        ))

    def __setattr__(self, name, value):
        pass

    def __del__(self):
        pass

    def __delattr__(self, name):
        pass

    # String Operations.
    def __len__(self):
        # len(x) -> length(x)
        return length(read(self.key))

    def __contains__(self, y):
        # 'foo' in x -> contains(x, 'foo')
        return contains(read(self.key), text(y))

    def matches(self, y):
        # x.matches('[a-z]+') -> matches(x, '[a-z]+')
        return matches(read(self.key), text(y))

    def index(self, y):
        # x.index('foo') -> indexOf(x, 'foo')
        return indexOf(read(self.key), text(y))

    def __getitem__(self, y):
        # x[1:4] -> slice(x, 1, 4)
        if isinstance(y, slice):
            return slice(read(self.key), y.start if y.start else 0, y.stop if y.stop else len(self))
        else:
            return slice(read(self.key), y, y + 1)

    # Math Operations.
    def __neg__(self):
        return sub(Zero, read(self.key))
    def __abs__(self):
        return abs(read(self.key))
    def __ceil__(self):
        return ceil(read(self.key))
    def __floor__(self):
        return floor(read(self.key))
    def __trunc__(self):
        return branch(less(read(self.key), Zero), ceil(read(self.key)), floor(read(self.key)))
    def __round__(self):
        return round(read(self.key))
    def __add__(self, y):
        return add(read(self.key), real(y))
    def __radd__(self, x):
        return add(real(x), read(self.key))
    def __sub__(self, y):
        return sub(read(self.key), real(y))
    def __rsub__(self, x):
        return sub(real(x), read(self.key))
    def __mul__(self, y):
        return mul(read(self.key), real(y))
    def __rmul__(self, x):
        return mul(real(x), read(self.key))
    def __truediv__(self, y):
        return div(read(self.key), real(y))
    def __rtruediv__(self, x):
        return div(real(x), read(self.key))
    def __floordiv__(self, y):
        return floor(div(read(self.key), real(y)))
    def __rfloordiv__(self, x):
        return floor(div(real(x), read(self.key)))
    def __mod__(self, y):
        return mod(read(self.key), real(y))
    def __rmod__(self, x):
        return mod(real(x), read(self.key))
    def __pow__(self, y, mod=None):
        return mod(pow(read(self.key), real(y)), real(mod)) if mod else pow(read(self.key), real(y))
    def __rpow__(self, x):
        return pow(real(x), read(self.key))

    # Comparison Operations.
    def __invert__(self):
        return negate(read(self.key))
    def __eq__(self, y):
        return equal(read(self.key), y)
    def __ne__(self, y):
        return negate(equal(read(self.key), y))
    def __lt__(self, y):
        return less(read(self.key), y)
    def __le__(self, y):
        return either(less(read(self.key), y), equal(read(self.key), y))
    def __gt__(self, y):
        return negate(either(less(read(self.key), y), equal(read(self.key), y)))
    def __ge__(self, y):
        return negate(less(read(self.key), y))
    def __and__(self, y):
        return both(read(self.key), flag(y))
    def __rand__(self, x):
        return both(flag(x), read(self.key))
    def __xor__(self, y):
        return both(either(read(self.key), flag(y)), negate(both(read(self.key), flag(y))))
    def __rxor__(self, x):
        return both(either(flag(x), read(self.key)), negate(both(flag(x), read(self.key))))
    def __or__(self, y):
        return either(read(self.key), flag(y))
    def __ror__(self, x):
        return either(flag(x), read(self.key))
