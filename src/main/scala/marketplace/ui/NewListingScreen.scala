package marketplace.ui

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.VBox
import scalafx.scene.control.{Label, TextField, TextArea, Button, ComboBox, Alert}
import scalafx.scene.control.Alert.AlertType
import scalafx.geometry.Insets
import scalafx.scene.input.{KeyCode, KeyEvent}
import marketplace.*
import marketplace.model.*
import scala.util.Try

/** Screen 3: form to create a new listing. All parsing/validation goes
  * through Try/Either so a bad credit-cost entry shows a friendly
  * alert instead of a NumberFormatException crashing the app (S1-12).
  */
object NewListingScreen:

  def validateCost(raw: String): Either[String, Int] =
    Try(raw.trim.toInt).toOption
      .toRight(s"'$raw' isn't a whole number")
      .flatMap(n => Either.cond(n > 0, n, "Credit cost must be positive"))

  def validateDescription(desc: String): Either[String, String] =
    val trimmed = desc.trim
    if trimmed.length > 200 then Left("Description cannot exceed 200 characters")
    else Right(trimmed)

  def build(
      appState: ObjectProperty[MarketplaceState],
      currentMemberId: ObjectProperty[String],
      updateState: (MarketplaceState => MarketplaceState) => Unit
  ): VBox =

    val titleField = new TextField:
      promptText = "Skill title, e.g. 'Basic guitar lessons'"

    val descField = new TextArea:
      promptText = "Short description (max 200 chars)"
      prefRowCount = 3

    val costField = new TextField:
      promptText = "Credit cost, e.g. 3"

    val categoryBox = new ComboBox[SkillCategory](SkillCategory.values.toIndexedSeq):
      value = SkillCategory.Other

    def submitForm(): Unit =
      val result =
        for
          _     <- Either.cond(currentMemberId.value.nonEmpty, (), "You must be logged in to create a listing.")
          cost  <- validateCost(costField.text.value)
          title <- Either.cond(titleField.text.value.trim.nonEmpty, titleField.text.value.trim, "Title can't be empty")
          desc  <- validateDescription(descField.text.value)
        yield SkillListing(
          id = Person.newId(),
          ownerId = currentMemberId.value,
          category = categoryBox.value.value,
          title = title,
          description = desc,
          creditCost = cost
        )

      result match
        case Right(listing) =>
          updateState(state => MarketplaceService.addListing(state, listing))
          titleField.text = ""
          descField.text = ""
          costField.text = ""
          new Alert(AlertType.Information, "Listing created.").showAndWait()
        case Left(reason) =>
          new Alert(AlertType.Error, reason).showAndWait()

    val submitBtn = new Button("Create Listing"):
      styleClass = Seq("button-primary")
      onAction = handle(submitForm())

    // Setup enter-key triggers for form submission
    titleField.onAction = handle(submitForm())
    costField.onAction = handle(submitForm())
    descField.onKeyPressed = (e: KeyEvent) =>
      if e.code == KeyCode.Enter then
        e.consume()
        submitForm()
    categoryBox.onKeyPressed = (e: KeyEvent) =>
      if e.code == KeyCode.Enter && !categoryBox.showing.value then
        e.consume()
        submitForm()

    new VBox(12,
      new Label("New Listing"):
        styleClass = Seq("screen-title"),
      new Label("Category") { styleClass = Seq("form-label") }, categoryBox,
      new Label("Title") { styleClass = Seq("form-label") }, titleField,
      new Label("Description") { styleClass = Seq("form-label") }, descField,
      new Label("Credit cost") { styleClass = Seq("form-label") }, costField,
      submitBtn
    ):
      padding = Insets(16)
      maxWidth = 420
