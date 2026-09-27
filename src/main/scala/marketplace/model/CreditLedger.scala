package marketplace.model

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

/** Shared credit-balance logic. Pulled out as a trait rather than
  * duplicated inline in Member so that if a future Person subtype also
  * needs a balance (e.g. a Coordinator who occasionally trades), the
  * logic lives in one place — this is the DRY / shared-base
  * requirement (S1-13), not a copy-pasted balance check.
  */
trait CreditLedger:
  def creditBalance: Int

  /** Returns Left with a reason if the member can't afford the cost,
    * otherwise Right with the balance after paying it. Pure — callers
    * decide what to do with the result, this never throws.
    */
  def afterSpending(cost: Int): Either[String, Int] =
    if cost <= 0 then Left("Cost must be positive")
    else if cost > creditBalance then Left(s"Insufficient credits: have $creditBalance, need $cost")
    else Right(creditBalance - cost)
