package io.github.lumkit.sweeteditor.highlight.internal.jni

internal fun ensureWebAbiStarted() {
    startWebAbiLoader()
}

private fun startWebAbiLoader(): Int = js(
    """
    (function () {
      if (globalThis.__slWebAbiLoaderStarted) return 0;
      globalThis.__slWebAbiLoaderStarted = 1;
      if (typeof document === 'undefined' || !document.head) return 0;
      var srcs = [
        'composeResources/io.github.lumkit.sweeteditor.highlight.generated.resources/files/sweetline_web_abi.js',
        'sweetline_web_abi.js',
        'native/web/sweetline_web_abi.js'
      ];
      var load = function (i) {
        if (i >= srcs.length) return;
        var s = document.createElement('script');
        s.src = srcs[i];
        s.async = true;
        s.onerror = function () { load(i + 1); };
        document.head.appendChild(s);
      };
      load(0);
      return 1;
    })()
    """,
)
