package marketplace

import org.scalatest.funsuite.AnyFunSuite
import marketplace.model.*

class MarketplaceServiceSpec extends AnyFunSuite:

  def freshState(): MarketplaceState =
    val owner     = Member("owner1", "Owner", creditBalance = 10)
    val requester = Member("req1", "Requester", creditBalance = 5)
    val listing   = SkillListing("listing1", "owner1", SkillCategory.Tutoring, "Maths help", "GCSE maths", creditCost = 3)
    MarketplaceState(
      members = Repository(List(owner, requester)),
      listings = Repository(List(listing)),
      transactions = Repository.empty
    )

  test("a valid exchange transfers credits and records a transaction") {
    val state = freshState()
    val result = MarketplaceService.requestExchange(state, "req1", "listing1")
    assert(result.isRight)
    val updated = result.getOrElse(fail("expected Right"))
    assert(updated.members.find(_.id, "req1").map(_.creditBalance).contains(2))
    assert(updated.members.find(_.id, "owner1").map(_.creditBalance).contains(13))
    assert(updated.transactions.all.size == 1)
  }

  test("only the owner can delete a listing") {
    val state = freshState()
    assert(MarketplaceService.removeListing(state, "listing1", "req1").isLeft)

    val updated = MarketplaceService.removeListing(state, "listing1", "owner1")
      .getOrElse(fail("expected Right"))
    assert(updated.listings.find(_.id, "listing1").isEmpty)
  }

  test("insufficient credits produces a Left, not an exception") {
    val state = freshState()
    val poorMember = Member("poor1", "Poor", creditBalance = 1)
    val stateWithPoorMember = state.copy(members = state.members.add(poorMember))
    val result = MarketplaceService.requestExchange(stateWithPoorMember, "poor1", "listing1")
    assert(result.isLeft)
  }

  test("a member can't request their own listing") {
    val state = freshState()
    val result = MarketplaceService.requestExchange(state, "owner1", "listing1")
    assert(result == Left("You can't request your own listing"))
  }

  test("requesting a nonexistent listing fails cleanly") {
    val state = freshState()
    val result = MarketplaceService.requestExchange(state, "req1", "nope")
    assert(result.isLeft)
  }

  test("CreditLedger.afterSpending rejects a non-positive cost") {
    val member = Member("m1", "M", creditBalance = 10)
    assert(member.afterSpending(0).isLeft)
    assert(member.afterSpending(-1).isLeft)
    assert(member.afterSpending(4) == Right(6))
  }

  // ai-assisted: #12
  // why: guards the Coordinator flagListing feature against wrong-category and non-coordinator callers.
  test("a coordinator can retire a listing in their moderated category") {
    val coordinator = Coordinator("c1", "Coordinator", SkillCategory.Tutoring)
    val state = freshState().copy(coordinators = Repository(List(coordinator)))
    val result = MarketplaceService.flagListing(state, "listing1", "c1")
    assert(result.isRight)
    assert(result.getOrElse(fail("expected Right")).listings.find(_.id, "listing1").isEmpty)
  }

  test("a coordinator cannot retire a listing outside their category") {
    val coordinator = Coordinator("c1", "Coordinator", SkillCategory.Cooking)
    val state = freshState().copy(coordinators = Repository(List(coordinator)))
    val result = MarketplaceService.flagListing(state, "listing1", "c1")
    assert(result == Left("Not authorised to moderate Tutoring listings"))
  }

  test("a non-coordinator cannot retire a listing") {
    val state = freshState()
    val result = MarketplaceService.flagListing(state, "listing1", "req1")
    assert(result.isLeft)
  }

  test("a coordinator cannot retire their own listing") {
    val coordinator = Coordinator("c1", "Coordinator", SkillCategory.Tutoring)
    val base = freshState()
    val listing = SkillListing("own1", "c1", SkillCategory.Tutoring, "Own", "owned", 2)
    val state = base.copy(
      listings = base.listings.add(listing),
      coordinators = Repository(List(coordinator))
    )
    val result = MarketplaceService.flagListing(state, "own1", "c1")
    assert(result == Left("You can't moderate your own listing"))
  }
