package marketplace

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import marketplace.model.*
import java.util.UUID

/** App-level state, held immutably: a snapshot of members, listings and
  * transactions. The UI layer keeps ONE of these inside a ScalaFX
  * ObjectProperty and replaces it wholesale on every change — this
  * class itself never mutates.
  */
case class MarketplaceState(
    members: Repository[Member],
    listings: Repository[SkillListing[SkillCategory]],
    transactions: Repository[Transaction],
    // ai-assisted: #12
    // why: gives the dormant Coordinator role somewhere to live in state; defaults
    // to empty so existing constructors (and tests) keep compiling unchanged.
    coordinators: Repository[Coordinator] = Repository.empty[Coordinator]
)

object MarketplaceService:

  /** Attempt to exchange credits for a listing. Returns Left with a
    * user-facing reason on any failure (no such listing, no such
    * member, insufficient credits, self-purchase) — the UI shows this
    * message directly rather than a stack trace (S1-18).
    */
  def requestExchange(
      state: MarketplaceState,
      requesterId: String,
      listingId: String
  ): Either[String, MarketplaceState] =
    for
      listing    <- state.listings.find(_.id, listingId).toRight(s"Listing $listingId not found")
      requester  <- state.members.find(_.id, requesterId).toRight(s"Member $requesterId not found")
      _          <- Either.cond(listing.ownerId != requesterId, (), "You can't request your own listing")
      owner      <- state.members.find(_.id, listing.ownerId).toRight(s"Listing owner ${listing.ownerId} not found")
      newBalance <- requester.afterSpending(listing.creditCost)
    yield
      val updatedRequester = requester.copy(creditBalance = newBalance)
      val updatedOwner = owner.copy(creditBalance = owner.creditBalance + listing.creditCost)
      val txn = Transaction(
        id = UUID.randomUUID().toString.take(8),
        listingId = listing.id,
        listingTitle = listing.title,
        providerId = listing.ownerId,
        requesterId = requesterId,
        creditsPaid = listing.creditCost
      )
      state.copy(
        members = state.members
          .removeWhere(member => member.id == requesterId || member.id == owner.id)
          .add(updatedRequester)
          .add(updatedOwner),
        transactions = state.transactions.add(txn)
      )

  /** Retires a listing (removing it from Browse) on a Coordinator's
    * authority. Restricted to coordinators whose moderated category
    * matches the listing, and never the coordinator's own listing —
    * same immutable Either pattern as removeListing.
    */
  // ai-assisted: #12
  // why: wires the previously-unused Coordinator role into the domain as a real capability.
  def flagListing(
      state: MarketplaceState,
      listingId: String,
      coordinatorId: String
  ): Either[String, MarketplaceState] =
    for
      listing     <- state.listings.find(_.id, listingId).toRight(s"Listing $listingId not found")
      coordinator <- state.coordinators.find(_.id, coordinatorId).toRight(s"Coordinator $coordinatorId not found")
      _           <- Either.cond(listing.ownerId != coordinatorId, (), "You can't moderate your own listing")
      _           <- Either.cond(
                       coordinator.moderatedCategory == listing.category,
                       (),
                       s"Not authorised to moderate ${listing.category} listings"
                     )
    yield state.copy(listings = state.listings.removeWhere(_.id == listingId))

  /** Removes a listing only for its owner, so one member cannot delete
    * another member's offer through the UI.
    */
  def removeListing(
      state: MarketplaceState,
      listingId: String,
      requesterId: String
  ): Either[String, MarketplaceState] =
    for
      listing <- state.listings.find(_.id, listingId).toRight(s"Listing $listingId not found")
      _ <- Either.cond(listing.ownerId == requesterId, (), "You can only delete your own listing")
    yield state.copy(listings = state.listings.removeWhere(_.id == listingId))

  /** Adds a new listing for a member. Generic-friendly: works for any
    * SkillCategory subtype T thanks to SkillListing[T <: SkillCategory].
    */
  def addListing[T <: SkillCategory](
      state: MarketplaceState,
      listing: SkillListing[T]
  ): MarketplaceState =
    state.copy(listings = state.listings.add(listing.asInstanceOf[SkillListing[SkillCategory]]))
