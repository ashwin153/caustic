import shutil
import subprocess

from pants.backend.jvm.tasks.nailgun_task import NailgunTask
from pants.base.exceptions import TaskError
from pants.base.workunit import WorkUnitLabel
from pants.java.jar.jar_dependency import JarDependency
from pants.task.simple_codegen_task import SimpleCodegenTask

from caustic.pants.targets.caustic_library import CausticLibrary


class CausticGen(SimpleCodegenTask, NailgunTask):
    """
    Generates Scala source code from Caustic *.acid files.
    """

    @classmethod
    def register_options(cls, register):
        super(CausticGen, cls).register_options(register)
        cls.register_jvm_tool(register, 'causticc', classpath_spec='//:causticc', classpath=[
            JarDependency(org='com.madavan', name='caustic-compiler_2.12', rev='2.0.3')
        ])

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
        # Generate Scala code using the Caustic compiler.
        sources = target.target_base
        main = "caustic.compiler.Causticc"
        classpath = self.tool_classpath('causticc')
        result = self.runjava(classpath=classpath, main=main, args=sources, workunit_name='compile')

        if result != 0:
            raise TaskError('Causticc ... exited non-zero ({})'.format(result))

        # Move all the generated sources to the target_workdir.
        for src in target.sources_relative_to_buildroot():
            gen = src.replace('.acid', '.scala')
            shutil.move(gen, target_workdir)
