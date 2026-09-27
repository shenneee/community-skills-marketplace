package marketplace.ui

// ai-assisted: #12
// why: gives the seeded Coordinator role a minimal UI — retire listings in the
// moderated category, removing them from Browse.

import scalafx.Includes.handle
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.{VBox, HBox}
import scalafx.scene.control.{Label, Button, Alert, ButtonType, ScrollPane}
import scalafx.scene.control.Alert.AlertType
import scalafx.geometry.Insets
import marketplace.*
import marketplace.model.*

/** Screen 5: moderation for whoever is logged in as a Coordinator.
  * Lists the listings in the coordinator's moderated category with a
  * Retire action; anyone else sees an explanatory empty state. The
  * service still guards every call (category + ownership checks), so
  * the UI never has to trust itself.
  */
object ModerateScreen:

  def build(
      appState: ObjectProperty[MarketplaceState],
      currentMemberId: ObjectProperty[String],
      updateState: (MarketplaceState => MarketplaceState) => Unit
  ): VBox =

    val listArea = new VBox(12):
      padding = Insets(16)

    val listingsScroll = new ScrollPane:
      content = listArea
      fitToWidth = true
      hbarPolicy = ScrollPane.ScrollBarPolicy.Never

    def coordinatorFor(id: String): Option[Coordinator] =
      appState.value.coordinators.find(_.id, id)

    def listingRow(listing: SkillListing[SkillCategory]): HBox =
      val tag = new Label(s"${SkillCategory.iconFor(listing.category)} ${listing.category.toString}"):
        style = s"-fx-background-color: ${SkillCategory.colourFor(listing.category)}; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 8;"

      val retireBtn = new Button("Retire"):
        styleClass = Seq("button-secondary")
        onAction = handle {
          val confirmation = new Alert(AlertType.Confirmation, s"Retire '${listing.title}' from the marketplace?")
          confirmation.showAndWait() match
            case Some(ButtonType.OK) =>
              MarketplaceService.flagListing(appState.value, listing.id, currentMemberId.value) match
                case Right(newState) => updateState(_ => newState)
                case Left(reason)    => new Alert(AlertType.Error, reason).showAndWait()
            case _ => ()
        }

      new HBox(12,
        tag,
        new VBox(8,
          new Label(listing.title) { styleClass = Seq("card-title") },
          new Label(listing.description) { styleClass = Seq("card-description") },
          new Label(s"${listing.creditCost} credit(s)") { styleClass = Seq("card-cost") }
        ) {
          padding = Insets(8)
        },
        retireBtn
      ):
        padding = Insets(10)
        styleClass = Seq("card")

    def refresh(): Unit =
      coordinatorFor(currentMemberId.value) match
        case None =>
          listArea.children = Seq(
            new Label("Only coordinators can moderate listings.") { styleClass = Seq("empty-state") }
          )
        case Some(coordinator) =>
          val candidates = appState.value.listings.all.filter(_.category == coordinator.moderatedCategory)
          listArea.children =
            if candidates.isEmpty then
              Seq(new Label(s"No ${coordinator.moderatedCategory} listings to review.") { styleClass = Seq("empty-state") })
            else candidates.map(listingRow)

    refresh()
    appState.onChange { (_, _, _) => refresh() }
    currentMemberId.onChange { (_, _, _) => refresh() }

    val title = new Label("Moderate Listings"):
      styleClass = Seq("screen-title")

    new VBox(12, title, listingsScroll):
      padding = Insets(16)
