package marketplace.persistence

import org.scalatest.funsuite.AnyFunSuite
import marketplace.model.*
import java.io.File
import java.time.LocalDateTime
import java.nio.file.Files

class CsvStoreSpec extends AnyFunSuite:

  test("saving and loading members round-trip works correctly") {
    val tempFile = File.createTempFile("members_test", ".csv")
    tempFile.deleteOnExit()
    val path = tempFile.getAbsolutePath

    val members = List(
      Member("m1", "Aisyah", Nil, 12),
      Member("m2", "Ben", Nil, 8),
      Member("m3", "Charlie, the Dev", Nil, 15) // contains comma to test escaping
    )

    val saveResult = CsvStore.saveMembers(path, members)
    assert(saveResult.isSuccess)

    val loadResult = CsvStore.loadMembers(path)
    assert(loadResult.isSuccess)

    val loaded = loadResult.get
    assert(loaded.size == 3)

    val aisyah = loaded.find(_.id == "m1").get
    assert(aisyah.displayName == "Aisyah")
    assert(aisyah.creditBalance == 12)

    val ben = loaded.find(_.id == "m2").get
    assert(ben.displayName == "Ben")
    assert(ben.creditBalance == 8)

    val charlie = loaded.find(_.id == "m3").get
    assert(charlie.displayName == "Charlie, the Dev") // commas escaped to semicolon and restored
    assert(charlie.creditBalance == 15)
  }

  test("saving and loading transactions retains the listing title") {
    val tempFile = File.createTempFile("transactions_test", ".csv")
    tempFile.deleteOnExit()
    val transaction = Transaction(
      "t1", "listing1", "Maths, GCSE", "owner1", "requester1", 3,
      LocalDateTime.parse("2026-07-28T10:00:00")
    )

    assert(CsvStore.saveTransactions(tempFile.getAbsolutePath, List(transaction)).isSuccess)
    val loaded = CsvStore.loadTransactions(tempFile.getAbsolutePath).getOrElse(fail("expected transaction"))
    assert(loaded.headOption.map(_.listingTitle).contains("Maths, GCSE"))
  }

  // ai-assisted: #9
  // why: verifies new listing dates persist while older CSV rows remain readable.
  test("saving and loading listings retains the posted date") {
    val tempFile = File.createTempFile("listings_test", ".csv")
    tempFile.deleteOnExit()
    val postedAt = LocalDateTime.parse("2026-07-28T10:00:00")
    val listing = SkillListing(
      "listing1", "owner1", SkillCategory.Tutoring, "Maths help", "GCSE maths", 3, postedAt
    )

    assert(CsvStore.saveListings(tempFile.getAbsolutePath, List(listing)).isSuccess)
    val loaded = CsvStore.loadListings(tempFile.getAbsolutePath).getOrElse(fail("expected listing"))
    assert(loaded.headOption.map(_.postedAt).contains(postedAt))
  }

  test("loading an older listing row supplies a posted date") {
    val tempFile = File.createTempFile("legacy_listings_test", ".csv")
    tempFile.deleteOnExit()
    val beforeLoad = LocalDateTime.now().minusSeconds(1)
    Files.writeString(tempFile.toPath, "listing1,owner1,Tutoring,Maths help,GCSE maths,3")

    val loaded = CsvStore.loadListings(tempFile.getAbsolutePath).getOrElse(fail("expected listing"))
    assert(loaded.headOption.exists(listing => !listing.postedAt.isBefore(beforeLoad)))
  }
