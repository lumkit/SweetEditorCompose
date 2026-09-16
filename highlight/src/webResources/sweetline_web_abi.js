(function (global) {
  if (global.SweetLineWebAbi) return;

  const abi = {
    ready: false,
    error: null,
  };
  global.SweetLineWebAbi = abi;

  const script = document.currentScript;
  const scriptBase = script && script.src ? script.src.replace(/[^/]+$/, "") : "";

  function asUtf8(text) {
    if (!text) return new Uint8Array(0);
    return new TextEncoder().encode(String(text));
  }

  function writeBytes(mod, ptr, bytes) {
    for (let i = 0; i < bytes.length; i++) {
      mod.setValue(ptr + i, bytes[i], "i8");
    }
  }

  function allocCString(mod, text) {
    const src = asUtf8(text);
    const ptr = mod._malloc(src.length + 1);
    if (!ptr) throw new Error("SweetLine malloc failed");
    writeBytes(mod, ptr, src);
    mod.setValue(ptr + src.length, 0, "i8");
    return ptr;
  }

  function withCString(mod, text, fn) {
    const ptr = allocCString(mod, text);
    try {
      return fn(ptr);
    } finally {
      mod._free(ptr);
    }
  }

  function readI32(mod, ptr, index) {
    return mod.getValue(ptr + index * 4, "i32") | 0;
  }

  function highlightSliceLength(mod, ptr) {
    const stride = readI32(mod, ptr, 1);
    const lineCount = readI32(mod, ptr, 4);
    if (stride <= 0 || lineCount < 0) return 0;
    let index = 5;
    for (let line = 0; line < lineCount; line++) {
      const spanCount = readI32(mod, ptr, index++);
      if (spanCount < 0) return 0;
      index += spanCount * stride;
    }
    return index;
  }

  function indentBufferLength(mod, ptr) {
    const lineStateCount = readI32(mod, ptr, 1);
    const guideCount = readI32(mod, ptr, 2);
    if (lineStateCount < 0 || guideCount < 0) return 0;
    let index = 3;
    for (let i = 0; i < guideCount; i++) {
      const branchCount = readI32(mod, ptr, index + 4);
      if (branchCount < 0) return 0;
      index += 5 + branchCount * 2;
    }
    return index + lineStateCount * 4;
  }

  function copyInts(mod, ptr, length) {
    const out = new Int32Array(Math.max(length, 0));
    for (let i = 0; i < out.length; i++) {
      out[i] = readI32(mod, ptr, i);
    }
    return out;
  }

  function adoptSlice(mod, ptr, kind) {
    if (!ptr) return null;
    const length = kind === "indent" ? indentBufferLength(mod, ptr) : highlightSliceLength(mod, ptr);
    const copy = copyInts(mod, ptr, length);
    mod._sl_free_buffer(ptr);
    return copy;
  }

  function withI32Pair(mod, a, b, fn) {
    const ptr = mod._malloc(8);
    try {
      mod.setValue(ptr, a | 0, "i32");
      mod.setValue(ptr + 4, b | 0, "i32");
      return fn(ptr);
    } finally {
      mod._free(ptr);
    }
  }

  function withI32Quad(mod, a, b, c, d, fn) {
    const ptr = mod._malloc(16);
    try {
      mod.setValue(ptr, a | 0, "i32");
      mod.setValue(ptr + 4, b | 0, "i32");
      mod.setValue(ptr + 8, c | 0, "i32");
      mod.setValue(ptr + 12, d | 0, "i32");
      return fn(ptr);
    } finally {
      mod._free(ptr);
    }
  }

  function compileStatus(mod, fn, engine, text) {
    return withCString(mod, text, (textPtr) => {
      const msgOut = mod._malloc(4);
      try {
        const code = fn(engine, textPtr, msgOut) | 0;
        const msgPtr = mod.getValue(msgOut, "i32");
        return {
          code,
          message: msgPtr ? mod.UTF8ToString(msgPtr) : "",
        };
      } finally {
        mod._free(msgOut);
      }
    });
  }

  function install(mod) {
    abi.createEngine = (tabSize) => mod._sl_create_engine(0, 0, tabSize | 0);
    abi.freeEngine = (engine) => {
      mod._sl_free_engine(engine);
    };
    abi.registerStyleName = (engine, name, styleId) => {
      withCString(mod, name, (ptr) => mod._sl_engine_register_style_name(engine, ptr, styleId | 0));
    };
    abi.compileJson = (engine, json) => {
      const result = compileStatus(mod, mod._sl_web_compile_json, engine, json);
      abi.lastCompileMessage = result.message;
      return result.code;
    };
    abi.compileFile = (engine, path) => {
      const result = compileStatus(mod, mod._sl_web_compile_file, engine, path);
      abi.lastCompileMessage = result.message;
      return result.code;
    };
    abi.createDocument = (uri, text) =>
      withCString(mod, uri, (uriPtr) =>
        withCString(mod, text, (textPtr) => mod._sl_create_document(uriPtr, textPtr)),
      );
    abi.freeDocument = (document) => {
      mod._sl_free_document(document);
    };
    abi.loadDocument = (engine, document) => mod._sl_engine_load_document(engine, document);
    abi.removeDocument = (engine, uri) => {
      withCString(mod, uri, (ptr) => mod._sl_engine_remove_document(engine, ptr));
    };
    abi.freeDocumentAnalyzer = (analyzer) => {
      mod._sl_free_document_analyzer(analyzer);
    };
    abi.analyzeLineRange = (analyzer, startLine, lineCount) =>
      withI32Pair(mod, startLine, lineCount, (range) =>
        adoptSlice(mod, mod._sl_document_analyze_line_range(analyzer, range), "highlight"),
      );
    abi.analyzeIncrementalInLineRange = (
      analyzer,
      startLine,
      startColumn,
      endLine,
      endColumn,
      newText,
      visibleStartLine,
      visibleLineCount,
    ) =>
      withI32Quad(mod, startLine, startColumn, endLine, endColumn, (changes) =>
        withI32Pair(mod, visibleStartLine, visibleLineCount, (visible) =>
          withCString(mod, newText, (textPtr) =>
            adoptSlice(
              mod,
              mod._sl_document_analyze_incremental_in_line_range(analyzer, changes, textPtr, visible),
              "highlight",
            ),
          ),
        ),
      );
    abi.getHighlightSlice = (analyzer, startLine, lineCount) =>
      withI32Pair(mod, startLine, lineCount, (range) =>
        adoptSlice(mod, mod._sl_document_get_highlight_slice(analyzer, range), "highlight"),
      );
    abi.analyzeIndentGuidesInLineRange = (analyzer, startLine, lineCount) =>
      withI32Pair(mod, startLine, lineCount, (range) =>
        adoptSlice(mod, mod._sl_document_analyze_indent_guides_in_line_range(analyzer, range), "indent"),
      );
    abi.analyzeBracketPairsInLineRange = (analyzer, startLine, lineCount) =>
      withI32Pair(mod, startLine, lineCount, (range) =>
        adoptSlice(mod, mod._sl_document_analyze_bracket_pairs_in_line_range(analyzer, range), "highlight"),
      );
  }

  const bases = [];
  if (scriptBase) bases.push(scriptBase);
  bases.push(
    "composeResources/io.github.lumkit.sweeteditor.highlight.generated.resources/files/",
    "native/web/",
    "./",
  );

  function loadFromBase(base) {
    return import(base + "sweetline_c_abi.js").then((mod) => {
      const factory = mod && (mod.default || mod);
      if (typeof factory !== "function") {
        throw new Error("SweetLine C ABI module has no factory export");
      }
      return factory({
        locateFile: (path) => base + path,
      });
    });
  }

  (async function load() {
    let lastError = null;
    const seen = {};
    for (let i = 0; i < bases.length; i++) {
      const base = bases[i];
      if (seen[base]) continue;
      seen[base] = true;
      try {
        const mod = await loadFromBase(base);
        install(mod);
        abi.ready = true;
        return;
      } catch (error) {
        lastError = error;
      }
    }
    abi.error = String(lastError && lastError.message ? lastError.message : lastError);
    console.error("Failed to load SweetLine C ABI", lastError);
  })();
})(typeof globalThis !== "undefined" ? globalThis : window);
