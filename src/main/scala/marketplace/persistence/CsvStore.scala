package marketplace.persistence

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import marketplace.model.*
import scala.util.{Try, Using}
import scala.io.Source
import java.io.{File, PrintWriter}

/** Plain-CSV persistence for listings and transactions. Deliberately
 * simple (no external JSON library) so the whole read/write path is
 * something you can explain line-by-line in the viva. Every method
 * that touches a file returns a Try — nothing here throws past its
 * own boundary (S1-12).
 */
object CsvStore:

  private def writeLines(path: String, lines: List[String]): Try[Unit] =
    Try {
      val file = new File(path)
      Option(file.getParentFile).foreach(_.mkdirs())
      Using.resource(new PrintWriter(file)) { writer =>
        lines.foreach(writer.println)
      }
    }

  private def readLines(path: String): Try[List[String]] =
    Try {
      val file = new File(path)
      if !file.exists() then List.empty
      else Using.resource(Source.fromFile(file))(_.getLines().toList)
    }

  // --- Listings ------------------------------------------------------
  // ai-assisted: #9
  // why: stores the listing date while still accepting files saved before dates were added.
  // format: id,ownerId,category,title,description,creditCost,postedAt

  def saveListings(path: String, listings: List[SkillListing[SkillCategory]]): Try[Unit] =
    writeLines(path, listings.map(l =>
      List(l.id, l.ownerId, l.category.toString, l.title, l.description, l.creditCost.toString, l.postedAt.toString)
        .map(_.replace(",", ";")).mkString(",")
    ))

  def loadListings(path: String): Try[List[SkillListing[SkillCategory]]] =
    readLines(path).flatMap { lines =>
      Try {
        lines.flatMap { line =>
          line.split(",", -1).toList match
            case id :: owner :: cat :: title :: desc :: cost :: postedAt :: Nil =>
              for
                category <- Try(SkillCategory.valueOf(cat)).toOption
                date <- Try(java.time.LocalDateTime.parse(postedAt)).toOption
              yield SkillListing(id, owner, category, title, desc, cost.toIntOption.getOrElse(0), date)
            // Older files do not contain a posted date, so use the load time as a safe fallback.
            case id :: owner :: cat :: title :: desc :: cost :: Nil =>
              Try(SkillCategory.valueOf(cat)).toOption.map { category =>
                SkillListing(id, owner, category, title, desc, cost.toIntOption.getOrElse(0))
              }
            case _ => None // malformed row: skip rather than crash the app
        }
      }
    }

  // --- Transactions ----------------------------------------------------
  // format: id,listingId,listingTitle,providerId,requesterId,creditsPaid,timestamp

  def saveTransactions(path: String, txns: List[Transaction]): Try[Unit] =
    writeLines(path, txns.map(t =>
      List(t.id, t.listingId, t.listingTitle, t.providerId, t.requesterId, t.creditsPaid.toString, t.timestamp.toString)
        .map(_.replace(",", ";")).mkString(",")
    ))

  def loadTransactions(path: String): Try[List[Transaction]] =
    readLines(path).flatMap { lines =>
      Try {
        lines.flatMap { line =>
          line.split(",", -1).toList match
            case id :: listingId :: title :: providerId :: requesterId :: paid :: ts :: Nil =>
              for
                credits <- paid.toIntOption
                time    <- Try(java.time.LocalDateTime.parse(ts)).toOption
              yield Transaction(id, listingId, title.replace(";", ","), providerId, requesterId, credits, time)
            // Older files do not contain titles, so retain their history using the ID as a fallback.
            case id :: listingId :: providerId :: requesterId :: paid :: ts :: Nil =>
              for
                credits <- paid.toIntOption
                time    <- Try(java.time.LocalDateTime.parse(ts)).toOption
              yield Transaction(id, listingId, listingId, providerId, requesterId, credits, time)
            case _ => None
        }
      }
    }

  // --- Members --------------------------------------------------------
  // format: id,displayName,creditBalance

  def saveMembers(path: String, members: List[Member]): Try[Unit] =
    writeLines(path, members.map(m =>
      List(m.id, m.displayName, m.creditBalance.toString)
        .map(_.replace(",", ";")).mkString(",")
    ))

  def loadMembers(path: String): Try[List[Member]] =
    readLines(path).flatMap { lines =>
      Try {
        lines.flatMap { line =>
          line.split(",", -1).toList match
            case id :: name :: balance :: Nil =>
              balance.toIntOption.map { b =>
                Member(id, name.replace(";", ","), Nil, b)
              }
            case _ => None
        }
      }
    }
