package io.github.lumkit.sweeteditor.input

internal actual fun webImeBridgeInstall(
    onKey: (key: String, ctrl: Boolean, shift: Boolean, alt: Boolean, meta: Boolean, isComposing: Boolean) -> Boolean,
    onCompositionEnd: (data: String) -> Unit,
    readOnly: Boolean,
): Int = js(
    """
    (function () {
      globalThis.__seImeOnKey = onKey;
      globalThis.__seImeOnCompositionEnd = onCompositionEnd;
      globalThis.__seImeReadOnly = readOnly;
      if (globalThis.__seImeTextarea) { globalThis.__seImeToken = (globalThis.__seImeToken || 0) + 1; return globalThis.__seImeToken; }
      var ta = document.createElement('textarea');
      ta.setAttribute('autocomplete', 'off');
      ta.setAttribute('autocorrect', 'off');
      ta.setAttribute('autocapitalize', 'off');
      ta.setAttribute('spellcheck', 'false');
      ta.style.position = 'fixed';
      ta.style.top = '-9999px';
      ta.style.left = '-9999px';
      ta.style.width = '1px';
      ta.style.height = '1px';
      ta.style.opacity = '0';
      ta.style.border = '0';
      ta.style.padding = '0';
      ta.style.margin = '0';
      ta.style.fontSize = '16px';
      ta.style.zIndex = '-1';
      ta.addEventListener('keydown', function (e) {
        if (e.isComposing) return;
        var prevent = globalThis.__seImeOnKey(e.key, e.ctrlKey, e.shiftKey, e.altKey, e.metaKey, e.isComposing);
        if (prevent) { e.preventDefault(); }
      });
      ta.addEventListener('compositionend', function (e) {
        globalThis.__seImeOnCompositionEnd(e.data || '');
        e.preventDefault();
      });
      ta.addEventListener('input', function (e) { if (!e.isComposing) { ta.value = ''; } });
      document.body.appendChild(ta);
      globalThis.__seImeTextarea = ta;
      globalThis.__seImeToken = 1;
      return 1;
    })()
    """,
)

internal actual fun webImeBridgeFocus(token: Int): Unit = js(
    "(function () { if (globalThis.__seImeTextarea && !globalThis.__seImeReadOnly) { globalThis.__seImeTextarea.focus(); } })()",
)

internal actual fun webImeBridgeSetReadOnly(token: Int, readOnly: Boolean): Unit = js(
    "(globalThis.__seImeReadOnly = readOnly, (globalThis.__seImeTextarea && globalThis.__seImeReadOnly ? globalThis.__seImeTextarea.blur() : 0), 0)",
)

internal actual fun webImeBridgeDestroy(token: Int): Unit = js(
    """
    (function () {
      var ta = globalThis.__seImeTextarea;
      if (ta && ta.parentNode) { ta.parentNode.removeChild(ta); }
      globalThis.__seImeTextarea = null;
      globalThis.__seImeOnKey = null;
      globalThis.__seImeOnCompositionEnd = null;
    })()
    """,
)
