# Simple Slack "Hello" bot
*| bot | java | boilerplate | slack | maven |*

## Overview
You can find here a not very smart bot for Slack, written in Java. Just send a friendly message to it and it will use it's all graceful mind and computing power to reply you "Hello, $username".

![Image](https://i.imgur.com/oDxWnRn.png)

This is also an simple and clean boilerplate to create bots for Slack from a scratch, so just fork this repo and start your development.

It built upon Slack's [RTM API](https://api.slack.com/rtm) and [methods API](https://api.slack.com/methods) using [AHC](https://github.com/AsyncHttpClient/async-http-client) library as HTTP and WebSockets client with [GSON](https://github.com/google/gson) as JSON-coding tool.

## Building
You will need JDK 8 and Maven 3 installed to build this application.
The process is simple as:

1. Clone this repo and `cd` into.
2. [Create](https://my.slack.com/services/new/bot) new Slack bot.
3. Create your environment config with `make env` (or `sh env.sh`) and write into a new `.env` file your bot's token and user id (Press *"Copy member ID"* button in chat with bot to get it).
4. Build application with `make build` (or `mvn package`).
5. Run application with `make run` (or `java -jar target/slackbot-0.1.0.jar`)

That's it! Now you can write handy messages to your bot. Check the [RTM API](https://api.slack.com/rtm) and [methods API](https://api.slack.com/methods) to continue development.

## Deploying
### Heroku
To deploy on Heroku enter theese commands (suggesting that Herocu CLI is installed):

```bash
heroku create your-dyno-name # enter your desired name here
heroku config:set BOT_TOKEN=XXXX-XXXXXXXXXXXX-XXXXXXXXXXXXXXXXXXXXXXXX BOT_USER=XXXXXXXXX
heroku git:clone -a your-dyno-name
```
