FROM clojure:openjdk-11-lein
MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
ENV LANG C.UTF-8
ENV APP_HOME /app
RUN apt-get update
RUN apt-get -y install tmux
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
