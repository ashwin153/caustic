from pants.backend.jvm.targets.jvm_target import JvmTarget


class CausticLibrary(JvmTarget):
    """
    A Caustic library generated from Caustic source files.
    """

    def __init__(self, **kwargs):
        super(CausticLibrary, self).__init__(**kwargs)
