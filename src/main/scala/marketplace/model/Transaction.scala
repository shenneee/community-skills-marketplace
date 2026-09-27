package marketplace.model

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import java.time.LocalDateTime

/** A completed exchange: `requesterId` spent `creditsPaid` credits on
  * `listingId`, owned by `providerId`. Immutable record — once created
  * a Transaction never changes, it's just appended to history.
  */
case class Transaction(
    id: String,
    listingId: String,
    listingTitle: String,
    providerId: String,
    requesterId: String,
    creditsPaid: Int,
    timestamp: LocalDateTime = LocalDateTime.now()
):
  def describe: String =
    s"${timestamp.toLocalDate} — paid $creditsPaid credit(s) for listing $listingId"
