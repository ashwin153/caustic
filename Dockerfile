FROM ubuntu:latest
MAINTAINER Ashwin Madavan (ashwin.madavan@gmail.com)

####################################################################################################
#                                    Install Pants Dependencies                                    #
#              https://github.com/pantsbuild/pants/blob/master/README.md#requirements              #
####################################################################################################
RUN apt-get update && apt-get -y install curl build-essential python python-dev openjdk-8-jdk

####################################################################################################
#                                         Compile Caustic                                          #
#                    Automatically bootstraps Pants and downloads dependencies.                    #
####################################################################################################
COPY . /caustic/
RUN cd /caustic && ./pants compile ::
WORKDIR /caustic
