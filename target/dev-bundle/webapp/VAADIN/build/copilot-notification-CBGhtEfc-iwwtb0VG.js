import { d as b$1, a as $, X as Xi } from "./indexhtml-CZ8u1Gs0.js";
const a = 5e3;
let o = 1;
function m(s) {
  b$1.notifications.includes(s) && (s.dontShowAgain && s.dismissId && r(s.dismissId), b$1.removeNotification(s), $.emit("notification-dismissed", s));
}
function f(s) {
  return Xi.getDismissedNotifications().includes(s);
}
function r(s) {
  f(s) || Xi.addDismissedNotification(s);
}
function u(s) {
  return !(s.dismissId && (f(s.dismissId) || b$1.notifications.find((t) => t.dismissId === s.dismissId)));
}
function N(s) {
  u(s) && c(s);
}
function c(s) {
  const t = o;
  o += 1;
  const e = { ...s, id: t, dontShowAgain: false, animatingOut: false };
  b$1.setNotifications([...b$1.notifications, e]), !s.link && !s.dismissId && setTimeout(() => {
    m(e);
  }, s.delay ?? a), $.emit("notification-shown", s);
}
export {
  m as dismissNotification,
  N as showNotification
};
