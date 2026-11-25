/* Bridges the web Notification API to the Android notification channel. */
(function () {
  var bridge = window.AndroidNative;
  if (!bridge || typeof bridge.notify !== 'function') return;

  function NativeNotification(title, options) {
    this.title = String(title || '');
    this.body = String((options && options.body) || '');
    try {
      bridge.notify(this.title, this.body);
    } catch (e) {
      /* notification failures must never break the chart */
    }
  }

  NativeNotification.prototype.close = function () {};
  NativeNotification.permission =
    typeof bridge.canNotify === 'function' && bridge.canNotify() ? 'granted' : 'denied';
  NativeNotification.requestPermission = function (callback) {
    var state = NativeNotification.permission;
    if (typeof callback === 'function') callback(state);
    return Promise.resolve(state);
  };

  window.Notification = NativeNotification;
})();
