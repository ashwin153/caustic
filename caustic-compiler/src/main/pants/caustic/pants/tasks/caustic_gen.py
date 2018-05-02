import shutil
import subprocess

from pants.base.exceptions import TaskError
from pants.base.workunit import WorkUnitLabel
from pants.task.simple_codegen_task import SimpleCodegenTask

from caustic.pants.targets.caustic_library import CausticLibrary


class CausticGen(SimpleCodegenTask):
    """
    Generates Scala source code from Caustic *.acid files.
    """

    @classmethod
    def register_options(cls, register):
        super(CausticGen, cls).register_options(register)

    def is_gentarget(self, target):
        return isinstance(target, CausticLibrary)

    def synthetic_target_type(self, target):
        return CausticLibrary

    def synthetic_target_extra_dependencies(self, target, target_workdir):
        return self.resolve_deps([
                'caustic-library/src/main/scala',
                'caustic-runtime/src/main/scala',
                '3rdparty/jvm:spray-json',
            ])

    def execute_codegen(self, target, target_workdir):
        sources = target.target_base
        cmd = './pants run caustic-compiler/src/main/scala:bin --no-lock -- {}'.format(sources)

        # Execute the compiler on all the source files in the target.
        with self.context.new_workunit(name='caustic', labels=[WorkUnitLabel.TOOL], cmd=cmd) as work:
            result = subprocess.call(
                cmd,
                stdout=work.output('stdout'),
                stderr=work.output('stderr'),
                shell=True,
            )

            if result != 0:
                raise TaskError('Caustic Compiler ... exited non-zero ({})'.format(result))

        # Move all the generated sources to the target_workdir.
        for src in target.sources_relative_to_buildroot():
            gen = src.replace('.acid', '.scala')
            shutil.move(gen, target_workdir)
