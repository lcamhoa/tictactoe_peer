# TicTacToe Peer2Peer 

It 's simple TicTacToe (3
x3) game . It's start to learn network programming in Java.

## Build 
```shell
./gradlew build  
```

## Run
```shell
java -jar build/libs/tictactoe_peer-1.0-SNAPSHOT.jar {port_number}
```

Client 1:
```shell
java -jar build/libs/tictactoe_peer-1.0-SNAPSHOT.jar 5050
```
Client 2
```shell
java -jar build/libs/tictactoe_peer-1.0-SNAPSHOT.jar 5051
```
### How to play 
Action -> Connect
![step1.png](documents%2Fscreenshots%2Fstep1.png)

Connect to opponent by {host}:{port} . Eg: localhost:5050 (from 5051)
![step2.png](documents%2Fscreenshots%2Fstep2.png)

Play and have fun
![step3.png](documents%2Fscreenshots%2Fstep3.png)

![step4.png](documents%2Fscreenshots%2Fstep4.png)