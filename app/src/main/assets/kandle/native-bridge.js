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
  Object.defineProperty(NativeNotification, 'permission', {
    get: function () {
      try {
        return typeof bridge.canNotify === 'function' && bridge.canNotify() ? 'granted' : 'denied';
      } catch (e) {
        return 'denied';
      }
    },
  });
  NativeNotification.requestPermission = function (callback) {
    try {
      if (typeof bridge.requestNotifyPermission === 'function') bridge.requestNotifyPermission();
    } catch (e) {
      /* permission dialog unavailable */
    }
    var state = NativeNotification.permission;
    if (typeof callback === 'function') callback(state);
    return Promise.resolve(state);
  };

  window.Notification = NativeNotification;
})();
