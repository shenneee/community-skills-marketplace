package marketplace.model

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

/** Closed set of skill categories. Sealed so the compiler can check
 * exhaustive matches (e.g. when picking a UI colour per category) —
 * add a new case here and every `match` that forgets it won't compile.
 */
enum SkillCategory:
  case Tutoring, Repair, Childcare, Cooking, Transport, Other

object SkillCategory:
  /** Hex colour used for this category's tag in the UI. Keeping this
   * here (not scattered through UI code) is the DRY point for S1-13:
   * one place decides category -> colour.
   */
  def colourFor(cat: SkillCategory): String = cat match
    case SkillCategory.Tutoring  => "#4C6EF5"
    case SkillCategory.Repair    => "#F76707"
    case SkillCategory.Childcare => "#E64980"
    case SkillCategory.Cooking   => "#F59F00"
    case SkillCategory.Transport => "#12B886"
    case SkillCategory.Other     => "#868E96"

  // ai-assisted: #8
  // why: part of the visual redesign — makes category tags scannable without reading every word.
  /** Small emoji icon per category, used alongside the label text on
   * category tags so the browse list is scannable without reading
   * every word.
   */
  def iconFor(cat: SkillCategory): String = cat match
    case SkillCategory.Tutoring  => "📚"
    case SkillCategory.Repair    => "🔧"
    case SkillCategory.Childcare => "🧸"
    case SkillCategory.Cooking   => "🍳"
    case SkillCategory.Transport => "🚗"
    case SkillCategory.Other     => "✨"