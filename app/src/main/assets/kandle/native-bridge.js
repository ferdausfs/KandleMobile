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
  NativeNotification.permission = 'granted';
  NativeNotification.requestPermission = function (callback) {
    if (typeof callback === 'function') callback('granted');
    return Promise.resolve('granted');
  };

  window.Notification = NativeNotification;
})();
