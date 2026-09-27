package marketplace.model

import java.time.LocalDateTime

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

/** A skill someone is offering. Generic over the category type `T`
  * (bounded to SkillCategory) so the same class could later back a
  * differently-scoped marketplace without change — this is the
  * parametric-polymorphism requirement (S1-9).
  */
case class SkillListing[T <: SkillCategory](
    id: String,
    ownerId: String,
    category: T,
    title: String,
    description: String,
    creditCost: Int,
    // ai-assisted: #9
    // why: captures when the listing was created without changing existing constructor calls.
    postedAt: LocalDateTime = LocalDateTime.now()
)

/** Small generic in-memory repository used for both listings and
  * transactions, so lookup/add/remove logic isn't written twice.
  * Second parametric-polymorphism example (generic method `find`).
  *
  * Fully immutable by design: every operation returns a NEW Repository
  * rather than mutating in place (S1-11 requires zero `var`/mutable
  * collections in domain code). The one place state needs to change
  * over time — the running app — holds the *current* Repository inside
  * a ScalaFX ObjectProperty, which is the explicitly-permitted
  * exception for UI bindings.
  */
class Repository[T] private (private val items: List[T]):
  def all: List[T] = items
  def add(item: T): Repository[T] = Repository(item :: items)
  def find[K](key: T => K, target: K): Option[T] = items.find(item => key(item) == target)
  def removeWhere(pred: T => Boolean): Repository[T] = Repository(items.filterNot(pred))

object Repository:
  def apply[T](initial: List[T] = Nil): Repository[T] = new Repository(initial)
  def empty[T]: Repository[T] = new Repository(Nil)
