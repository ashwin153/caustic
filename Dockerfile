FROM ubuntu:latest
MAINTAINER Ashwin Madavan (ashwin.madavan@gmail.com)

# Install dependencies.
RUN apt-get update && apt-get -y install curl python build-essential python-dev openjdk-8-jdk

# Clone repository and bootstrap pants.
COPY . /caustic/
RUN cd /caustic && ./pants compile ::
WORKDIR /caustic
