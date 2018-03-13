# Beaker
Beaker is a distributed, transactional key-value store. Beaker uses a leader-less variation of
[Generalized Paxos][1] to consistently execute transactions. Beaker permits a minority of failures 
and hence it is ```N / 2``` fault tolerant. Beaker assumes that failures are fail-stop. It
makes no assumptions about the underlying network except that messages are received in the order
they were sent. Most networking protocols, including [TCP][4], satisfy this requirement.

## Introduction
A __database__ is a transactional key-value store. Databases map keys to versioned values, called
__revisions__. Revisions are uniquely identified and totally ordered by their version. A 
__transaction__ depends on the versions of a set of keys, called its *readset*, and changes the
values of a set of keys, called its *writeset*. Transactions may be *committed* if and only if the
versions they depend on are greater than or equal to their versions in the database. Revisions 
are monotonic; if a transaction changes a key for which there exists a newer revision, the 
modification is discarded. This ensures that transactions cannot undo the effect of other 
transactions. We say that a transaction ```A``` *conflicts with* ```B``` if either reads or writes a 
key that the other writes. 

A distributed database is a collection of __beakers__. Each beaker maintains its own replica of the 
database. In order to maintain consistency across beakers, a majority of beakers must agree to
commit every transaction. Reaching agreement in a distributed system is often referred to as 
[consensus][2], and it is a relatively well studied problem in computer science. There are a variety 
of algorithms that solve this problem, most notably [Paxos][3], that have proven to be correct 
and performant. Beaker employs a variation of Paxos that has several desirable properties.
First, beakers may simultaneously commit non-conflicting transactions. Second, beakers automatically
repair replicas that have stale revisions. Third, beakers may safely commit transactions as long as
they are connected to at least a majority of their non-faulty peers.

## Consensus
Beakers reach consensus on __proposals__. A proposal is a collection of non-conflicting transactions
that are uniquely identified and totally ordered by a __ballot__ number. We say that a proposal 
```A``` *conflicts with* ```B``` if any transaction in ```A``` conflicts with a transaction in 
```B```. We say that a proposal ```A``` is *older than* ```B``` if ```A``` conflicts with ```B``` 
and ```A``` has a lower ballot than ```B```. Proposals also contain a write-only *repair* 
transaction that is used only for recovery. Proposals ```A``` and ```B``` may be *merged* by 
taking the maximum of their ballots, combining their transactions choosing the transactions in 
the newer proposal in the case of conflicts, and combining their repairs choosing 
the write with the highest version in the case of duplicates. 

The leader for a proposal ```P``` first *prepares* ```P``` on ```Q(P)```. If a beaker in ```Q(P)``` 
has not made a promise to a newer proposal, then it responds with a __promise__ not to accept any 
proposal that conflicts with the proposal it returns that has a lower ballot than the proposal it 
receives. If the beaker has already accepted older proposals, it merges them together and returns 
the result. Otherwise, it returns the proposal with a zero ballot. If the leader does not receive a 
promise from every beaker in ```Q(P)```, it retries with a higher ballot. Otherwise, it merges the 
returned promises into a single proposal ```P'```. If ```P``` does not equal ```P'```, then it
retries with ```P'```. Otherwise, the leader *gets* the latest revisions of the keys that are read 
by ```P``` from ```Q(P)```. The leader discards all transactions in ```P``` that cannot be committed 
given the latest returned revisions, and sets its repairs to the latest revisions for keys that are 
read - but not written - by ```P``` for which the beakers in ```Q(P)``` have different versions. The
leader then asks each beaker in ```Q(P)``` to *accept* the proposal. A beaker accepts a proposal if
it has not promised not to. When a beaker accepts a proposal, it discards all older accepted 
proposals and __votes__ for ```P```. We say that a proposal is *accepted* if every beaker in
```Q(P)``` votes for it. If ```P``` is accepted, then the leader requests each beaker in ```Q(P)``` 
to *learn* ```P```. Otherwise, the leader retries with ```P```.  When a beaker learns a proposal, it 
commits its transactions and repairs on its replica and *shares* the transactions it committed with 
every other beaker.

### Correctness
The proof of correctness relies on the assumption of *quorum connectivity*, beakers are always
connected to at least a majority of their peers, and the fact of *quorum intersection*, any majority
of beakers will contain at least one beaker in common.

__Consistency.__ If a proposal ```A``` is accepted, it can be committed. __Proof.__ Suppose, for the 
sake of contradiction, that ```A``` cannot be committed. Then there must exist some conflicting 
proposal ```B``` that is accepted after but learned before ```A``` on some beaker. By quorum 
intersection, ```Q(B)``` must contain at least one beaker that learned ```A```. Therefore, any 
transactions in ```B``` that conflict with changes made by ```A``` are discarded before ```B```
is accepted. 

But now transactions in ```A``` that conflict with changes in ```B``` cannot be committed.

A A
    Achanges
__Linearizability:__ If proposal ```A``` conflicts with ```B``` and ```A``` is committed before
```B``` somewhere, then ```A``` will be committed before ```B``` everywhere. __Proof.__ Suppose, for 
the sake of contradiction, that ```B``` is committed before ```A``` somewhere. Then, ```B``` must be 
learned before ```A``` somewhere but after ```A``` somewhere else. But ```A``` conflicts with ```B```, 
so ```A``` and be cannot be learned simultaneously. (Theorem 1)


- __Liveness:__ If a proposal is proposed, then a proposal will be learned.
- __Safety:__ If two proposals are learned, then the older proposal is learned first.
- __Non Triviality:__ If a proposal is learned, then it was proposed.

#### Appendix
__Theorem 1.__ If a proposal ```A``` has been accepted by a majority, then a conflicting proposal 
```B``` cannot be prepared until ```A``` is learned by a majority. __Proof.__ By quorum 
intersection, at least one promise will contain ```A```. Therefore, ```A``` must always be prepared 
until it is learned by a majority.




__Theorem 2.__ If a proposal ```A``` is accepted by a majority, then its transactions can be 
committed. 

__Theorem 3.__ If a proposal ```A``` is accepted by a majority, then it is eventually learned.
__Proof.__ Because a conflicting proposal ```B``` cannot be prepared until ```A``` is learned, 
by  a





__Theorem 2.__ If a proposal ```A``` has been accepted by a majority, then it is eventually learned.
__Proof.__ By Theorem 1, a conflicting proposal can never be prepared until ```A``` is learned.
Therefore, ```A``` is eventually learned.

until
after ```B``` is learned. However, ```A``` was accepted by a majority, so it must have been 
prepared.

B is learned before A is learned
B is learned after A was accept by a majority
  Accepted  Learned
    |----------|
        |
        Learned


Because ```B``` was learned and ```B``` conflicts


Then, the transaction must read a key for which there exists a newer version. This is
only possible if a conflicting proposal ```B``` was learned but not yet committed. 



Because ```A``` was learned, a conflicting proposal can never be prepared until ```A``` is learned.

Because a conflicting proposal can never be learned before ```A``` is learned, this is only possible 
if there exists a conflicting proposal ```B``` that has been accepted, but not yet learned, by a 
majority. Because ```B``` was accepted by a majority and ```B``` conflicts with ```A```, ```A``` 
cannot be prepared until ```B``` is learned. But ```A``` was accepted, so it must have been 
prepared.



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



__Theorem.__ If proposal ```A``` conflicts with ```B``` and ```A``` is learned, then ```A``` will be 
committed before ```B```. __Proof.__ Suppose, for the sake of contradiction, that
```B``` is committed before ```A```. Then, ```A``` must have been learned after ```B```. Therefore, 
a majority must have accepted ```A``` after ```B```. But ```A``` was learned before ```B```, so
```B``` cannot be accepted until after ```A``` is learned. (Theorem 1) 


Because ```A``` is learned before ```B``` and
```A``` conflicts with ```B```, ```B``` cannot be prepared until ```A``` is learned. (Theorem 1)
Therefore, ```B``` is learned after ```A```. 


Suppose, for the sake of contradiction, that
```B``` is learned before ```A```. Because ```A``` and ```B``` are learned, they may both be
committed. (Consistency) However, 


Because ```A``` and ```B``` conflict, ```A``` cannot be prepared
until ```B``` is learned. 



If ```A``` and ```B``` are learned, they must 
have both been accepted by a majority. Because ```A``` is older than ```B```, a majority must have 
accepted ```B``` after ```A```. Otherwise, ```A``` could not have been accepted by a majority. The 
majority who accepted ```B``` after ```A``` will vote for ```A``` before they vote for ```B```. 
Because messages are received in order, ```A``` will be learned before ```B```.

## Reconfiguration
Each beaker is required to be connected to a majority of non-faulty peers in order to guarantee 
correctness. However, this correctness condition is only valid when the cluster is static. In
practical systems, beakers may join or leave the cluster arbitrarily as the cluster grows or shrinks 
in size. In this section, we describe how *fresh* beakers are *bootstrapped* when they join an 
existing cluster. When a fresh beaker joins a cluster, its database is initially empty. In order to 
guarantee correctness, its database must be immediately populated with the latest revision of every
key-value pair. Otherwise, if ```N -+ 1``` fresh beakers join a cluster of size ```N``` it 
is possible for a quorum to consist entirely of fresh beakers. 

A naive solution might be for the fresh beaker to propose a read-only transaction that depends on
the initial revision of every key-value pair in the database and conflicts with every other
proposal. Then, the fresh beaker would automatically repair itself in the process of committing this
transaction. However, this is infeasible in practical systems because databases may contain
arbitrarily many key-value pairs. This approach would inevitably saturate the network because for a
database of size ```D``` such a proposal consumes ```D * (3 * N / 2 + N * N)``` in bandwidth. 
Furthermore, it prevents any proposals from being accepted in the interim.

We can improve this solution by decoupling bootstrapping and consensus. A fresh beaker joins the 
cluster as a non-voting member; it learns proposals, but does not participate in consensus. The 
fresh beaker reads the contents of the database from a quorum. It then assembles a repair 
transaction and commits it on its replica. It then joins the cluster as a voting member. This 
approach consumes just ```D * N / 2``` in bandwidth and permits concurrent proposals.

[1]: https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-2005-33.pdf
[2]: https://en.wikipedia.org/wiki/Consensus_(computer_science)
[3]: https://en.wikipedia.org/wiki/Paxos_(computer_science)
[4]: https://en.wikipedia.org/wiki/Transmission_Control_Protocol