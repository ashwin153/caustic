# Project \# 1
A [distributed key-value store][1] based on
```caustic-beaker/```. Beaker is a distributed, transactional key-value store, that uses a 
leader-less variation of [Generalized Paxos][2] to consistently execute transactions. Because Beaker
is leader-less, multiple Beakers may simultaneously propose non-conflicting transactions. Beaker is 
```N/2``` fault tolerant; the system is strongly consistent as long as each server is connected 
to a majority of its peers.

## Getting Started
```./pants run caustic-cs380d/src/main/scala```

## Authors
- Ashwin Madavan (aam4379)

[1]: http://www.cs.utexas.edu/~vijay/cs380D-s18/project1.pdf
[2]: https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-2005-33.pdf