from pants.build_graph.build_file_aliases import BuildFileAliases
from pants.goal.task_registrar import TaskRegistrar as task

from caustic.pants.targets.caustic_library import CausticLibrary
from caustic.pants.tasks.caustic_gen import CausticGen


def build_file_aliases():
    return BuildFileAliases(
        targets={
            'caustic_library': CausticLibrary,
        },
    )


def register_goals():
    task(name='caustic', action=CausticGen).install('gen')
