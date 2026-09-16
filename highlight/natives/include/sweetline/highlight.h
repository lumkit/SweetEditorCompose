#ifndef SWEETLINE_HIGHLIGHT_H
#define SWEETLINE_HIGHLIGHT_H

#include <cstdint>
#include "sweetline/foundation.h"
#include "sweetline/syntax.h"

namespace NS_SWEETLINE {
  /// Each matched highlight token span
  struct TokenSpan {
    /// Range of the token span
    TextRange range;
    /// Matched text content
    U8String matched_text;
    /// Style ID matched by this token span (0 means unstyled)
    int32_t style_id;
    /// Detailed style information for the token span (only available in inline_style mode)
    InlineStyle inline_style;
    /// State when this token span was matched
    int32_t state {0};
    /// Target state to transition to
    int32_t goto_state {-1};

    bool operator==(const TokenSpan& other) const;
    bool operator!=(const TokenSpan& other) const;
    bool isReusableWith(const TokenSpan& other) const;

#ifdef SWEETLINE_DEBUG
    void dump() const;
#endif
  };

  /// Highlight token span sequence for each line
  struct LineHighlight {
    List<TokenSpan> spans;
    void pushOrMergeSpan(TokenSpan&& span);
    bool operator==(const LineHighlight& other) const;
    bool isReusableWith(const LineHighlight& other) const;
    void toJson(U8String& result) const;

#ifdef SWEETLINE_DEBUG
    void dump() const;
#endif
  };

  /// Highlight result for the entire document
  struct DocumentHighlight {
    List<LineHighlight> lines;

    void addLine(LineHighlight&& line);
    size_t spanCount() const;
    void reset();
    void toJson(U8String& result) const;

#ifdef SWEETLINE_DEBUG
    void dump() const;
#endif
  };

  /// Line range descriptor (0-based)
  struct LineRange {
    /// Start line number
    size_t start_line {0};
    /// Line count
    size_t line_count {0};
  };

  /// Highlight result slice for a specified line range
  struct DocumentHighlightSlice {
    /// Actual start line (may be clipped by boundary)
    size_t start_line {0};
    /// Total line count of the document after patching
    size_t total_line_count {0};
    /// Consecutive line highlight results
    List<LineHighlight> lines;
  };

  /// Scope state
  enum struct ScopeState : int8_t {
    /// Scope start
    START = 0,
    /// Scope end
    END,
    /// Scope content
    CONTENT
  };

  /// Line scope state for indent guide analysis
  struct LineScopeState {
    /// Nesting level of the line
    int32_t nesting_level {-1};
    /// Scope state of the line
    ScopeState scope_state {ScopeState::CONTENT};
    /// Column of the scope marker (only when scope_state is START or END)
    int32_t scope_column {0};
    /// Indentation level of the line
    int32_t indent_level {0};

    bool operator==(const LineScopeState& other) const;
  };

  /// Single indent guide line (vertical line segment)
  struct IndentGuideLine {
    /// Branch point (e.g. position of else/case)
    struct BranchPoint {
      int32_t line {0};
      int32_t column {0};

      bool operator==(const BranchPoint& other) const;
    };

    /// Column of the guide line (character column)
    int32_t column {0};
    /// Start line number
    int32_t start_line {0};
    /// End line number
    int32_t end_line {0};
    /// Nesting level (0-based)
    int32_t nesting_level {0};
    /// Associated ScopeRule ID (matching pair mode), -1 for indentation mode
    int32_t scope_rule_id {-1};
    /// Whether the guide continues from before the returned slice
    bool continues_before {false};
    /// Whether the guide continues after the returned slice
    bool continues_after {false};
    /// Branch point list (line/column positions of else/case etc.)
    List<BranchPoint> branches;

    bool operator==(const IndentGuideLine& other) const;
  };

  /// Indent guide analysis result
  struct IndentGuideResult {
    /// Actual start line of the returned slice
    size_t start_line {0};
    /// Total line count of the analyzed document
    size_t total_line_count {0};
    /// All vertical guide lines
    List<IndentGuideLine> guide_lines;
    /// Scope state for each line
    List<LineScopeState> line_states;
  };

  /// Bracket token kind
  enum struct BracketTokenKind : int8_t {
    /// Opening bracket
    OPEN = 0,
    /// Closing bracket
    CLOSE
  };

  /// Bracket match state
  enum struct BracketMatchState : int8_t {
    /// Bracket has a known partner
    MATCHED = 0,
    /// Bracket has no valid partner
    UNMATCHED,
    /// Matching state cannot be fully resolved from the requested range
    UNKNOWN
  };

  /// Single bracket token
  struct BracketToken {
    /// Range of the bracket token
    TextRange range;
    /// Nesting depth used by rainbow bracket rendering
    int32_t depth {0};
    /// Token kind
    BracketTokenKind kind {BracketTokenKind::OPEN};
    /// Match state
    BracketMatchState match_state {BracketMatchState::UNKNOWN};
    /// Range of the matched partner; valid only when match_state is MATCHED
    TextRange partner_range;
  };

  /// Bracket token sequence for each line
  struct LineBracketPairs {
    List<BracketToken> tokens;
  };

  /// Bracket pair analysis result
  struct BracketPairResult {
    /// Actual start line of the returned slice
    size_t start_line {0};
    /// Total line count of the analyzed document
    size_t total_line_count {0};
    /// Consecutive bracket token results
    List<LineBracketPairs> lines;
  };

  /// Text line metadata
  struct TextLineInfo {
    /// Line index
    size_t line {0};
    /// Start highlight state of the line
    int32_t start_state {SyntaxRule::kDefaultStateId};
    /// Start character offset of the line in the entire text (not bytes), used for calculating TokenSpan index. Not needed when show_index is disabled in HighlightConfig
    size_t start_char_offset {0};
  };

  /// Single line syntax highlight analysis result
  struct LineAnalyzeResult {
    /// Highlight sequence of the current line
    LineHighlight highlight;
    /// End state after line analysis
    int32_t end_state {SyntaxRule::kDefaultStateId};
    /// Total character count analyzed in the current line (not bytes), excluding line ending
    size_t char_count {0};
  };

  /// Highlight configuration
  struct HighlightConfig {
    /// Whether the analysis result includes character index; without it, each TokenSpan only has line and column
    bool show_index {false};
    /// Whether to use inline styles, i.e. style definitions are embedded directly in syntax rule JSON, and the analysis result contains style info (foreground color, bold, etc.) instead of returning style IDs
    bool inline_style {false};
    /// Tab width, used for calculating indentation level in indent guide analysis (1 tab = tab_size spaces)
    int32_t tab_size {4};

    static HighlightConfig kDefault;
  };

  class LineHighlightAnalyzer;
  /// Plain text highlight analyzer, no incremental update support, suitable for full analysis scenarios
  class TextAnalyzer {
  public:
    TextAnalyzer(const SharedPtr<SyntaxRule>& rule, const HighlightConfig& config = HighlightConfig::kDefault);

    /// Analyze a text content and return the highlight result for the entire text
    /// @param text Full text content
    /// @return Highlight result
    SharedPtr<DocumentHighlight> analyzeText(const U8String& text);

    /// Analyze a single line of text
    /// @param text Single line text content
    /// @param line_info Metadata for the current line
    /// @param result Receives the single line analysis result
    void analyzeLine(const U8String& text, const TextLineInfo& line_info, LineAnalyzeResult& result) const;

    /// Perform indent guide analysis on a text
    /// @param text Full text content
    /// @return Indent guide analysis result
    SharedPtr<IndentGuideResult> analyzeIndentGuides(const U8String& text);

    /// Perform bracket pair analysis on a text
    /// @param text Full text content
    /// @return Bracket pair analysis result
    SharedPtr<BracketPairResult> analyzeBracketPairs(const U8String& text);

    /// Get the current highlight configuration
    const HighlightConfig& getHighlightConfig() const;
  private:
    SharedPtr<SyntaxRule> m_rule_;
    UniquePtr<LineHighlightAnalyzer> m_line_highlight_analyzer_;
    HighlightConfig m_config_;
  };

  class InternalDocumentAnalyzer;
  /// Managed document highlight analyzer with automatic patch and incremental analysis support
  class DocumentAnalyzer {
  public:
    /// Perform full highlight analysis on the managed document
    /// @return Highlight result for the entire managed document
    SharedPtr<DocumentHighlight> analyze() const;

    /// Analyze enough lines to cover the requested line range and return the corresponding slice
    /// @param visible_range The visible line range to analyze and return
    /// @return Highlight slice for the specified line range
    SharedPtr<DocumentHighlightSlice> analyzeLineRange(const LineRange& visible_range) const;

    /// Incrementally re-analyze the entire managed document based on patch content
    /// @param range The change range of the patch
    /// @param new_text The patched text
    /// @return Highlight result for the entire managed document
    SharedPtr<DocumentHighlight> analyzeIncremental(const TextRange& range, const U8String& new_text) const;

    /// Incrementally analyze based on patch content, ensuring the requested line range is available in the cache
    /// @param range The change range of the patch
    /// @param new_text The patched text
    /// @param visible_range The visible line range to return
    /// @return Highlight slice for the specified line range
    SharedPtr<DocumentHighlightSlice> analyzeIncrementalInLineRange(const TextRange& range, const U8String& new_text,
      const LineRange& visible_range) const;

    /// Get highlight slice from the current cached result without triggering new analysis
    /// @param visible_range The visible line range to return
    /// @return Highlight slice for the specified line range
    SharedPtr<DocumentHighlightSlice> getHighlightSlice(const LineRange& visible_range) const;

    /// Incrementally re-analyze the entire managed document based on patch content
    /// @param start_index Start character index of the patch change
    /// @param end_index End character index of the patch change
    /// @param new_text The patched text
    /// @return Highlight result for the entire managed document
    SharedPtr<DocumentHighlight> analyzeIncremental(size_t start_index, size_t end_index, const U8String& new_text) const;

    /// Get the managed document held by this analyzer
    /// @return std::shared_ptr<Document>
    SharedPtr<Document> getDocument() const;

    /// Get the current highlight configuration
    /// @return HighlightConfig
    const HighlightConfig& getHighlightConfig() const;

    /// Perform indent guide analysis on the managed document
    /// @return Indent guide analysis result
    SharedPtr<IndentGuideResult> analyzeIndentGuides() const;

    /// Perform indent guide analysis for the requested line range
    /// @param visible_range The visible line range to analyze and return
    /// @return Indent guide analysis result for the specified line range
    SharedPtr<IndentGuideResult> analyzeIndentGuidesInLineRange(const LineRange& visible_range) const;

    /// Perform bracket pair analysis on the managed document
    /// @return Bracket pair analysis result
    SharedPtr<BracketPairResult> analyzeBracketPairs() const;

    /// Perform bracket pair analysis for the requested line range
    /// @param visible_range The visible line range to analyze and return
    /// @return Bracket pair analysis result for the specified line range
    SharedPtr<BracketPairResult> analyzeBracketPairsInLineRange(const LineRange& visible_range) const;
  private:
    friend class HighlightEngine;
    DocumentAnalyzer(const SharedPtr<Document>& document, const SharedPtr<SyntaxRule>& rule,
      const HighlightConfig& config = HighlightConfig::kDefault);
    UniquePtr<InternalDocumentAnalyzer> analyzer_impl_;
  };

  /// Highlight engine
  class HighlightEngine {
  public:
    explicit HighlightEngine(const HighlightConfig& config = HighlightConfig::kDefault);

    /// Define a macro
    /// @param macro_name Macro name
    void defineMacro(const U8String& macro_name);

    /// Undefine a macro
    /// @param macro_name Macro name
    void undefineMacro(const U8String& macro_name);

    /// Check if a macro is defined
    /// @param macro_name Macro name
    bool isMacroDefined(const U8String& macro_name) const;

    /// Compile syntax rule from JSON
    /// @param json JSON content of the syntax rule file
    /// @throws SyntaxCompileError Throws SyntaxCompileError on compilation error
    SharedPtr<SyntaxRule> compileSyntaxFromJson(const U8String& json);

    /// Compile syntax rule from file
    /// @param file Syntax rule definition file (JSON)
    /// @throws SyntaxCompileError Throws SyntaxCompileError on compilation error
    SharedPtr<SyntaxRule> compileSyntaxFromFile(const U8String& file);

    /// Get syntax rule by name (e.g. java)
    /// @param name Syntax rule name
    SharedPtr<SyntaxRule> getSyntaxRuleByName(const U8String& name) const;

    /// Get syntax rule by file name or path
    /// @param file_name File name or path
    SharedPtr<SyntaxRule> getSyntaxRuleByFileName(const U8String& file_name) const;

    /// Register a highlight style for name mapping
    /// @param style_name Style name
    /// @param style_id Style ID
    void registerStyleName(const U8String& style_name, int32_t style_id) const;

    /// Get the registered style name by style ID
    /// @param style_id Style ID
    const U8String& getStyleName(int32_t style_id) const;

    /// Create a text highlight analyzer by syntax rule name (no incremental analysis support, but supports single-line analysis with line state for custom incremental analysis)
    /// @param syntax_name Syntax rule name (e.g. java)
    /// @return TextAnalyzer
    SharedPtr<TextAnalyzer> createAnalyzerBySyntaxName(const U8String& syntax_name) const;

    /// Create a text highlight analyzer by file name or path (no incremental analysis support, but supports single-line analysis with line state for custom incremental analysis)
    /// @param file_name File name or path
    /// @return TextAnalyzer
    SharedPtr<TextAnalyzer> createAnalyzerByFileName(const U8String& file_name) const;

    /// Load a managed document and get an incremental highlight analyzer
    /// @param document Managed document
    /// @return Highlight result for the entire managed document
    SharedPtr<DocumentAnalyzer> loadDocument(const SharedPtr<Document>& document);

    /// Remove a previously loaded managed document
    /// @param uri URI of the managed document
    void removeDocument(const U8String& uri);
  private:
    HighlightConfig m_config_;
    HashSet<SharedPtr<SyntaxRule>> m_syntax_rules_;
    HashMap<U8String, SharedPtr<DocumentAnalyzer>> m_analyzer_map_;
    SharedPtr<StyleMapping> m_style_mapping_;
    /// Set of defined macros
    HashSet<U8String> m_macros_;
  };
}

#endif //SWEETLINE_HIGHLIGHT_H
