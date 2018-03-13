package caustic.cluster

/**
 *
 */
trait Service[Client] {

  /**
   * Constructs a [[Client]] connected to the [[Address]].
   *
   * @param address
   * @return
   */
  def connect(address: Address): Client

  /**
   * Disconnects the [[Client]].
   *
   * @param client
   */
  def disconnect(client: Client): Unit

}