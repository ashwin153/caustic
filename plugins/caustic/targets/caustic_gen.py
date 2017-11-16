import os
import shutil
import subprocess

from pants.base.exceptions import TaskError
from pants.base.workunit import WorkUnitLabel
from pants.task.simple_codegen_task import SimpleCodegenTask

from caustic.targets.caustic_library import CausticLibrary


class CausticGen(SimpleCodegenTask):
    """

    """

    @classmethod
    def register_options(cls, register):
        super(CausticGen, cls).register_options(register)

    def is_gentarget(self, target):
        return isinstance(target, CausticLibrary)

    def synthetic_target_type(self, target):
        return CausticLibrary

    def synthetic_target_extra_dependencies(self, target, target_workdir):
        # Generated sources depend on spray-json for JSON serialization, and on the service package
        # of the caustic-runtime in order to connect to the runtime and execute transactions.
        return self.resolve_deps([
                'caustic-runtime/src/main/scala:service',
                '3rdparty/jvm:spray-json',
            ])

    def execute_codegen(self, target, target_workdir):
        # Execute the compiler on each source file.
        for src in target.sources_relative_to_buildroot():
            cmd = './pants run caustic-compiler/src/main/scala:bin --no-lock -- generate {}'.format(src)
            with self.context.new_workunit(name=src, labels=[WorkUnitLabel.TOOL], cmd=cmd) as work:
                result = subprocess.call(
                    cmd,
                    stdout=work.output('stdout'),
                    stderr=work.output('stderr'),
                    shell=True,
                )

                if result != 0:
                    raise TaskError('Caustic Compiler ... exited non-zero ({})'.format(result))

        # Move the generated sources from gen/ to the proper location.
        for path in os.listdir('gen'):
            shutil.move(os.path.join('gen', path), target_workdir)
        os.rmdir('gen')
