package marketplace.ui

import org.scalatest.funsuite.AnyFunSuite

class NewListingScreenSpec extends AnyFunSuite:

  test("validateCost accepts positive whole numbers") {
    assert(NewListingScreen.validateCost("5") == Right(5))
    assert(NewListingScreen.validateCost(" 10 ") == Right(10))
  }

  test("validateCost rejects non-integers") {
    assert(NewListingScreen.validateCost("abc").isLeft)
    assert(NewListingScreen.validateCost("5.5").isLeft)
  }

  test("validateCost rejects non-positive numbers") {
    assert(NewListingScreen.validateCost("0").isLeft)
    assert(NewListingScreen.validateCost("-1").isLeft)
  }

  test("validateDescription accepts valid description length and trims it") {
    val shortDesc = "Learn guitar from scratch."
    assert(NewListingScreen.validateDescription(shortDesc) == Right("Learn guitar from scratch."))

    val spacesDesc = "   Some spaced text   "
    assert(NewListingScreen.validateDescription(spacesDesc) == Right("Some spaced text"))
  }

  test("validateDescription rejects descriptions exceeding 200 characters") {
    val longDesc = "a" * 201
    assert(NewListingScreen.validateDescription(longDesc).isLeft)
  }
