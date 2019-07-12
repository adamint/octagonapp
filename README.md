# Octagon - News API
As the argument for each program, provide a local path to a config file with the following format:

```
dbhost :: RETHINKDB_IP
```

To create a rethinkdb server with Docker for development purposes, use:
```bash
$ docker volume create database
$ docker run --name database-name -p 28015:28015 -p 29015:29015 -p 8080:8080 -v database:/data rethinkdb
```

If you are using Linux or OSX, use `localhost` as the ip. 
Windows users need to use whatever their virtual machine's ip is (likely `192.168.99.100`)

## What do I do?
For now, you have to run each program individually. 
Soon, there will be a master program that controls all others.