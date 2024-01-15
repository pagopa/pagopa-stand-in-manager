FROM python:3.11 as build
#-slim as build

ARG SHELL="/bin/sh"
ARG ALLURE_RELEASE=2.21.0
ARG ALLURE_REPO=https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline
ARG UID=1000
ARG GID=1000

ENV ROOT=/app

COPY ./ $ROOT

WORKDIR $ROOT

RUN apt-get update \
    && apt-get -y install libpq-dev gcc default-jdk\
    && pip install -r requirements.txt

RUN curl ${ALLURE_REPO}/${ALLURE_RELEASE}/allure-commandline-${ALLURE_RELEASE}.zip -L -o /tmp/allure-commandline.zip && \
            unzip -q /tmp/allure-commandline.zip -d /

RUN groupadd --gid ${GID} allure \
    && useradd --uid ${UID} --gid allure --shell /bin/bash --create-home allure

ENV ALLURE_HOME=/allure-$ALLURE_RELEASE
ENV ALLURE_HOME_SL=/allure
ENV PATH=$PATH:$ALLURE_HOME/bin
ENV ALLURE_RESOURCES=$ROOT/resources

FROM build as execution

ENV PSP_SUBSCRIPTION_KEY "AT_RUNTIME"
ENV ORG_SUBSCRIPTION_KEY "AT_RUNTIME"
ENV TAGS "runnable"
ENV ENV "dev"

CMD ["sh", "run_test.sh"]
