env:
    @sh env.sh

build:
    @mvn package

run:
    @java -jar target/slackbot-0.1.0.jar
