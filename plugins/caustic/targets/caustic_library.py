from pants.backend.jvm.targets.jvm_target import JvmTarget


class CausticLibrary(JvmTarget):
    """

    """

    def __init__(self, **kwargs):
        super(CausticLibrary, self).__init__(**kwargs)

    @classmethod
    def compute_dependency_specs(cls, kwargs=None, payload=None):
        yield 'caustic-runtime/src/main/scala:service'

