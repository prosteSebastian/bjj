package com.example.bjj.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("cookies")
@PageTitle("Cookie Policy • BJJ Tech Map")
public class CookiePolicyView extends VerticalLayout {

  public CookiePolicyView() {
    setMaxWidth("900px");
    setWidthFull();
    setSpacing(true);
    setPadding(true);
    getStyle().set("margin","32px auto");

    add(new H1("Cookie Policy"));

    add(new Paragraph("""
      This site uses a single, strictly necessary cookie to remember which techniques you have marked as “learned”.
      We do not use analytics, advertising, or third-party tracking cookies.
    """));

    add(new Paragraph("Details:"));
    var list = new UnorderedList(
      new ListItem("Name: bjjConsent — remembers if you accepted/declined cookies."),
      new ListItem("Name: bjjLearned — stores the IDs of techniques you marked as learned."),
      new ListItem("Scope: first-party, limited to this site only."),
      new ListItem("Retention: up to 5 years for learned progress (2 years for consent)."),
      new ListItem("Optional: if you decline cookies, we keep your progress only for the current session and do not store it on your device.")
    );
    add(list);

    add(new Paragraph("""
      You can clear your progress at any time by clearing cookies for this site in your browser settings.
      If you have questions, open an issue on the project repository.
    """));
  }
}
