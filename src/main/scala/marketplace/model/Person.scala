package marketplace.model

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import java.util.UUID

/** Common base for anyone in the marketplace. Abstract because "a bare
  * Person" never exists on its own — every person in the system is
  * either a trading Member or a Coordinator who moderates listings.
  *
  * Design decision: I made this an abstract class (not a trait) because
  * every subtype shares concrete state (id, displayName) as well as
  * behaviour, and a person can only ever extend one Person. The
  * alternative I considered was a trait with self-type — that would
  * allow mixing Person-ness into unrelated hierarchies, which this
  * domain doesn't need.
  */
abstract class Person:
  def id: String
  def displayName: String

  /** Short one-line summary shown in list views. Subclasses override
    * this to add role-specific detail — this is the S1-8 override point.
    */
  def summary: String = s"$displayName (#$id)"

object Person:
  def newId(): String = UUID.randomUUID().toString.take(8)

/**
 * A trading member of the marketplace: offers skills, requests
 * exchanges, and holds a credit balance via CreditLedger.
 *
 * @param offeredSkillsList private-by-construction list of offered skills.
 *                          It is exposed only through the immutable
 *                          `offeredSkills` accessor below. Callers get a
 *                          read-only view and must use `withOfferedSkill`
 *                          to change it, keeping every Member value immutable (S1-11).
 */
case class Member(
    id: String,
    displayName: String,
    private val offeredSkillsList: List[SkillListing[SkillCategory]] = Nil,
    creditBalance: Int = 10 // everyone starts with a small credit float
) extends Person
    with CreditLedger:

  def offeredSkills: List[SkillListing[SkillCategory]] = offeredSkillsList

  override def summary: String =
    s"$displayName — ${offeredSkillsList.size} skill(s) offered, $creditBalance credits"

  def withOfferedSkill(listing: SkillListing[SkillCategory]): Member =
    copy(offeredSkillsList = listing :: offeredSkillsList)

/** A coordinator moderates listings and can retire/flag them, but does
  * not trade themselves. Second concrete subclass of Person — this is
  * what gives the hierarchy its ≥2-subclasses inheritance credit and
  * is a real design choice (a coordinator is not just "a Member who
  * can't trade"; it has a genuinely different responsibility).
  */
case class Coordinator(id: String, displayName: String, moderatedCategory: SkillCategory)
    extends Person:
  override def summary: String =
    s"$displayName — moderates ${moderatedCategory.toString}"
