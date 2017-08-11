from caustic.syntax.codec import *
from caustic.syntax.field import Field


class Record(object):
    """

    """

    def __init__(self, name, parent):
        """

        :param name:
        :param parent:
        """
        self.key = parent.key + FieldDelimiter + name
        self.name = name
        self.parent = parent

    def __getattr__(self, name):
        return Field(name, self)

    def __setattr__(self, name, value):
        Field(name, self).set(value)

    def __del__(self):
        pass

    def __delattr__(self, name):
        pass
