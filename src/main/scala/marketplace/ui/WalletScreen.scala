package marketplace.ui

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.VBox
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.geometry.Insets
import marketplace.*
import marketplace.model.*

/** Screen 4: current credit balance and this member's transaction
  * history — read-only, so no validation needed here.
  */
object WalletScreen:

  def build(
      appState: ObjectProperty[MarketplaceState],
      currentMemberId: ObjectProperty[String]
  ): VBox =

    val balanceLabel = new Label:
      styleClass = Seq("balance-label")

    val historyArea = new VBox(12):
      padding = Insets(12)

    // Transaction history grows over time, so it scrolls independently of the balance.
    val historyScroll = new ScrollPane:
      content = historyArea
      fitToWidth = true
      hbarPolicy = ScrollPane.ScrollBarPolicy.Never

    def refresh(): Unit =
      val state = appState.value
      val currentMember = state.members.find(_.id, currentMemberId.value)
      balanceLabel.text = currentMember match
        case Some(m) => s"Balance: ${m.creditBalance} credit(s)"
        case None    => "Balance: Not logged in"

      val mine = state.transactions.all.filter(t =>
        t.requesterId == currentMemberId.value || t.providerId == currentMemberId.value
      )
      historyArea.children =
        if mine.isEmpty then Seq(new Label("No transactions yet.") { styleClass = Seq("history-text") })
        else mine.map { t =>
          val historyText =
            if t.requesterId == currentMemberId.value then
              s"${t.timestamp.toLocalDate} — paid ${t.creditsPaid} credit(s) for ${t.listingTitle}"
            else
              s"${t.timestamp.toLocalDate} — received ${t.creditsPaid} credit(s) for ${t.listingTitle}"
          // ai-assisted: #8
          // why: part of the visual redesign — presents each history line as a card.
          new VBox(new Label(historyText) { styleClass = Seq("history-text") }):
            padding = Insets(12)
            styleClass = Seq("card")
        }

    refresh()
    appState.onChange { (_, _, _) => refresh() }
    currentMemberId.onChange { (_, _, _) => refresh() }

    val title = new Label("Wallet & History"):
      styleClass = Seq("screen-title")
    val historyLabel = new Label("Transaction history:"):
      styleClass = Seq("form-label")

    new VBox(12, title, balanceLabel, historyLabel, historyScroll):
      padding = Insets(16)
