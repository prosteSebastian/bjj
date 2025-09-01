import { e as rt } from "./indexhtml-CZ8u1Gs0.js";
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const i = (e, a, t) => (t.configurable = true, t.enumerable = true, Reflect.decorate && typeof a != "object" && Object.defineProperty(e, a, t), t);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
function p(e, a) {
  return (t, n, d) => {
    const l = (o) => o.renderRoot?.querySelector(e) ?? null;
    return i(t, n, { get() {
      return l(this);
    } });
  };
}
function y(e) {
  e.querySelectorAll(
    "vaadin-context-menu, vaadin-menu-bar, vaadin-menu-bar-submenu, vaadin-select, vaadin-combo-box, vaadin-tooltip, vaadin-dialog"
  ).forEach((a) => {
    let t = a.shadowRoot?.querySelector(
      `${a.localName}-overlay, ${a.localName}-submenu, vaadin-menu-bar-overlay`
    );
    t?.localName === "vaadin-menu-bar-submenu" && (t = t.shadowRoot.querySelector("vaadin-menu-bar-overlay")), t ? t._attachOverlay = c.bind(t) : a.$?.overlay && (a.$.overlay._attachOverlay = c.bind(a.$.overlay));
  });
}
function u() {
  return document.querySelector(`${rt}main`).shadowRoot;
}
const v = () => Array.from(u().children).filter((a) => a._hasOverlayStackMixin && !a.hasAttribute("closing")).sort((a, t) => a.__zIndex - t.__zIndex || 0), b = (e) => e === v().pop();
function c() {
  const e = this;
  e._placeholder = document.createComment("vaadin-overlay-placeholder"), e.parentNode.insertBefore(e._placeholder, e), u().appendChild(e), e.hasOwnProperty("_last") || Object.defineProperty(e, "_last", {
    // Only returns odd die sides
    get() {
      return b(this);
    }
  }), e.bringToFront(), requestAnimationFrame(() => y(e));
}
export {
  p,
  y
};
