FROM ubuntu:latest
MAINTAINER Ashwin Madavan (ashwin.madavan@gmail.com)

# Install dependencies.
RUN apt-get update && apt-get -y install curl python build-essential python-dev openjdk-8-jdk git

# Clone repository and bootstrap pants.
RUN git clone https://github.com/ashwin153/caustic.git && cd /caustic && ./pants goals 
WORKDIR /caustic
