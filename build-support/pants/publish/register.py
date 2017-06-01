# pants/publish/register.py
from pants.build_graph.build_file_aliases import BuildFileAliases
from pants.backend.jvm.repository import Repository
from pants.backend.jvm.ossrh_publication_metadata import (Developer, License,
                                                          OSSRHPublicationMetadata, Scm)
import os

repository = Repository(
    name = 'public',
    url = 'https://oss.sonatype.org/#stagingRepositories',
    push_db_basedir = os.path.join('build-support', 'ivy', 'pushdb')
)

metadata = OSSRHPublicationMetadata(
    description = 'A library for expressing and executing transactions.',
    url = 'https://github.com/ashwin153/schema',
    licenses = [
        License(
            name = 'Apache License, Version 2.0',
            url = 'http://www.apache.org/licenses/LICENSE-2.0'
        )
    ],
    developers = [
        Developer(
            name = 'Ashwin Madavan',
            email = 'ashwin.madavan@gmail.com',
            url = 'http://madavan.me'
        )
    ],
    scm = Scm.github(
        user = 'ashwin153',
        repo = 'schema'
    )
)

def build_file_aliases():
    return BuildFileAliases(objects = {
        'public': repository,
        'describe': metadata,
    })
