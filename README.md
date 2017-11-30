# Fomo

The Fear of Missing Out on something great! This shall be the new hotness for the Fomo market api.


### Prerequisites

* Postgres 10
* sbt 0.13.16
* scala 2.12

## Getting Started
1. Start the bittrex-websocket:
```
project bittrex
reStart
```

2. Start the api
```
project api
reStart
```

3. Start the trail-stop
```
project trailingStopService
reStart
```


## Service Deployment Order
1. Bittrex Web Socket
2. Bittrex Exchange Service
3. Bittrex Trailing Stop
4. Api







