# Beaker
Beaker is a distributed, transactional key-value store. Beaker uses a leader-less variation of
[Generalized Paxos][1] to consistently execute transactions. Beaker is ```N/2``` fault tolerant; the 
system tolerates a minority of failures. Beaker assumes that failures are fail-stop, but makes no
assumptions about the underlying network; beakers may be partitioned arbitrarily as long as they
remain connected to a majority of non-faulty peers.

## Background
A __database__ is a transactional key-value store. Databases maps keys to versioned values, called
__revisions__. Revisions are uniquely identified and totally ordered by their version. A 
__transaction__ depends on the versions of a set of keys, called its *readset*, and changes the
values of a set of keys, called its *writeset*. Transactions may be *committed* if and only if the
versions they depend on are greater than or equal to their versions in the database. Revisions 
are monotonic; if a transaction changes a key for which there exists a newer revision, the 
modification is discarded. This ensures that transactions cannot undo the effect of other 
transactions. We say that a transaction ```A``` *conflicts with* ```B``` if either reads or writes a 
key that the other writes. 

A __beaker__ is a distributed database. Each beaker maintains its own replica of the database.
Before a transaction may be committed on their replicas, a majority of beakers must agree to commit
it. Reaching agreement in a distributed system is often referred to as [consensus][2], and 
it is a relatively well studied problem in computer science. There are a variety of algorithms that 
solve this problem, most notably [Paxos][3], that have been proven to be correct and performant. 
Beaker employs a variation of Paxos with several desirable properties. First, beakers may 
simultaneously commit non-conflicting transactions. Second, beakers automatically repair replicas 
with stale revisions. Third, beakers may safely commit transactions as long as they are connected to 
at least a majority of their non-faulty peers.

## Consensus
Beakers reach consensus on __proposals__. A proposal is a collection of non-conflicting transactions
that are uniquely identified and totally ordered by a __ballot__ number. We say that a proposal 
```A``` *conflicts with* ```B``` if any transaction in ```A``` conflicts with a transaction in 
```B```. We say that a proposal ```A``` is *older than* ```B``` if ```A``` conflicts with ```B``` 
and ```A``` has a lower ballot than ```B```. Proposals also contain a write-only *repair* 
transaction that is used only for recovery. Proposals ```A``` and ```B``` may be *merged* by (1) 
taking the maximum of their ballots, (2) combining their transactions choosing the transactions in 
the newer proposal in the case of conflicts, and (3) combining their repairs choosing 
the write with the highest version in the case of duplicates. 

The leader for a proposal first *prepares* the proposal on a majority of beakers. If a beaker has 
not made a promise to a newer proposal, it responds with a __promise__. When a beaker makes a 
promise, it refuses to accept any proposal that conflicts with the proposal it returns with a lower
ballot than the proposal it makes the promise to. It a beaker has already accepted an older
proposal, it merges them together and returns the result. Otherwise, it returns the proposal with
the initial ballot. If the leader does not receive a majority of promises, it retries with a 
higher ballot. Otherwise, it merges the returned promises into a single proposal. If the proposal 
does not match the proposal it prepared, it retries with the proposal. Otherwise, the leader *gets* 
the latest versions of the keys that are read by the proposal from a majority of beakers. The leader 
discards all transactions in the proposal that cannot be committed, and sets its repairs to the 
latest revisions of keys that are read but not written by the proposal for which the beakers 
disagree on their version. The leader then sends the proposal to a majority of beakers. A beaker
*accepts* a proposal if it has not promised not to. If a beaker accepts a proposal, it discards all 
older accepted proposals and broadcasts a __vote__ for it. A beaker *learns* a proposal once a 
majority of replicas vote for it. If a beaker learns a proposal, it commits its transactions and 
repairs on its replica of the database.

### Correctness
The key idea underlying the correctness of Paxos and all its derivatives is quorum intersection; any 
two majorities cannot be disjoint. Beaker makes use of this fact extensively in its proof of 
correctness. The proof assumes that messages are received in the order they were sent. Most 
networking protocols, including [TCP][4], satisfy this requirement.

__Theorem.__ If a proposal ```A``` has been accepted by a majority, a conflicting proposal can never 
be prepared until ```A``` is learned. __Proof.__ By quorum intersection, at least one promise will
contain ```A```. Therefore, ```A``` must be prepared.

__Theorem.__ Let ```R``` denote the repairs for an accepted proposal ```A```. Any accepted proposal 
```B``` that conflicts with ```A + R``` commutes with ```A + R```. __Proof.__ Because ```A``` and 
```B``` are both accepted, ```A``` and ```B``` do not conflict. Because ```B``` conflicts with 
```A + R``` but not ```A```, ```B``` must read a key ```k``` that is read by ```A```. Because 
```B``` is accepted, it must read the latest version of ```k```. Suppose that ```B``` is 
committed first. Because ```B``` reads and does not write ```k```, ```A + R``` can still be 
committed. Suppose that ```A + R``` is committed first. Because ```A + R``` writes the latest 
version of ```k``` and ```B``` reads the latest version, ```B``` can still be committed.

__Theorem.__ If a proposal ```A``` has been accepted by a majority, its transactions can be
committed. __Proof.__ Suppose, for the sake of contradiction, that there exists a transaction that 
cannot be committed. Then, the transaction must read a key for which there exists a newer version. 
Because a conflicting proposal can never be learned before ```A``` is learned, this is only possible 
if there exists a conflicting proposal ```B``` that has been accepted, but not yet learned, by a 
majority. Because ```B``` was accepted by a majority and ```B``` conflicts with ```A```, ```A``` 
cannot be prepared until ```B``` is learned. But ```A``` was accepted, so it must have been 
prepared.

__Theorem.__ If a proposal ```A``` has been accepted by a majority, its transactions will eventually
be committed. __Proof.__ Because a conflicting proposal cannot be learned until ```A``` is learned
and the transactions in ```A``` can be committed, the transactions in ```A``` will eventually be 
committed.

__Theorem.__ If proposals ```A``` and ```B``` are learned and ```A``` is older than ```B```, then 
```A``` will be learned before ```B```. __Proof.__ If ```A``` and ```B``` are learned, they must 
have both been accepted by a majority. Because ```A``` is older than ```B```, a majority must have 
accepted ```B``` after ```A```. Otherwise, ```A``` could not have been accepted by a majority. The 
majority who accepted ```B``` after ```A``` will vote for ```A``` before they vote for ```B```. 
Because messages are received in order, ```A``` will be learned before ```B```.

[1]: https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-2005-33.pdf
[2]: https://en.wikipedia.org/wiki/Consensus_(computer_science)
[3]: https://en.wikipedia.org/wiki/Paxos_(computer_science)
[4]: https://en.wikipedia.org/wiki/Transmission_Control_Protocol