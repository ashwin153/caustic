# Project \# 1
A [distributed key-value store][1] based on ```caustic-beaker/```. Beaker is a distributed, 
transactional key-value store, that uses a leader-less variation of [Generalized Paxos][2] to 
consistently execute transactions. Because Beaker is leader-less, multiple Beakers may 
simultaneously propose non-conflicting transactions. Beaker is ```N/2``` fault tolerant; the system 
is strongly consistent as long as each server is connected to a majority of its peers.

Beaker uses a four-phase protocol to coordinate distributed transactions. Before jumping into a
description of the protocol, let us first define some common terminology. Each __beaker__ maintains 
a local database. This database maps keys to a version and a value, together referred to as a 
__revision__. Revisions of a key are totally ordered and uniquely identified by their version. 
A __transaction__ depends on the versions of a set of keys, called its *readset*, and changes the 
values of a set of keys, called its *writeset*. Transactions may be committed on the database if no 
greater version exists for a key in its readset. Revisions are monotonic; therefore, if a committed 
transaction changes a key for which there exists a greater version, the change is discarded. This 
property is essential for the safety of the commit protocol, for reasons that will be made clear 
later. We say that a transaction ```A``` *conflicts* with ```B``` if either reads or writes keys 
that the other writes. A __proposal__ is a collection of non-conflicting transactions. A proposal is 
uniquely identified by a monotonically increasing __ballot__ number. We say that a __proposal__ 
```A``` is *older* than ```B``` if any transaction in ```A``` conflicts with a transaction ```B``` 
and ```A``` has a lower ballot than ```B```.

1. Prepare: The proposer sends its proposal to a quorum of beakers. Each beaker replies with a
   promise not to accept an older proposal. If the beaker has already accepted an older proposal, 
   it returns it. If the beaker has already promised not to accept a newer proposal, then 
   it returns its ballot. Otherwise, it returns the original proposal.
   
   If the proposer receives a promise with a larger ballot than its own, it retries with a larger
   ballot. Otherwise, the proposer merges together the returned promises into a single proposal. If
   any two promises contain conflicting transactions, the transactions in the higher ballot promise
   are chosen. If the promised proposal matches the original, the proposer continues with the second
   phase. Otherwise, it retries with the promised proposal.
   
2. Get: The proposer reads all of its transactions' dependencies from a quorum of beakers. If any
   beaker has a later version of a key than the transaction depends on, the transaction is 
   discarded. The proposer then adds a special repair transaction to its proposal that writes the
   latest revisions of keys for which the beakers have different versions. Because transactions 
   must depend on the latest versions to be committed and transactions cannot decrease the versions 
   of a key, it is impossible for the repair transaction to overwrite simultaneous or future changes 
   made by another proposal. Therefore, the repair transaction does not need to be prepared. The
   proposer then requests the beakers to accept its updated proposal.
   
3. Accept: Replicas accept the proposal if and only if they have not promised not to. If a beaker
   accepts the proposal, it sends a vote for the proposal to all beakers.

4. Learn: Once a beaker receives a vote for a proposal from a quorum of beakers, it commits the
   proposal on its own database. Because we require each non-faulty beaker to be connected to a 
   majority of its peers, every beaker is guaranteed to learn all proposals that it proposes. Once
   the proposer learns that its proposal was accepted by a quorum or that a newer proposal was 
   accepted by a quorum it returns.


## Getting Started
Clone the reposity, and run this command from the root directory.

```./pants run caustic-cs380d/src/main/scala```

## Authors
- Ashwin Madavan (aam4379)

[1]: http://www.cs.utexas.edu/~vijay/cs380D-s18/project1.pdf
[2]: https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-2005-33.pdf
